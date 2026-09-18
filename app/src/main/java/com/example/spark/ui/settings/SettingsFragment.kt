package com.example.spark.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.spark.MainActivity
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.UserRepository
import com.example.spark.databinding.DialogProfileDetailsBinding
import com.example.spark.databinding.FragmentSettingsBinding
import com.example.spark.notifications.HabitReminderScheduler
import com.example.spark.notifications.MoodReminderScheduler
import com.example.spark.ui.auth.LoginActivity
import com.example.spark.util.DateUtils
import com.example.spark.util.PreferenceHelper
import com.example.spark.util.ProfileImageHelper
import com.example.spark.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var userRepository: UserRepository
    private lateinit var habitRepository: HabitRepository

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedProfileImage(uri)
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                requireContext(),
                "Notification permission is needed for reminders",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        sessionManager = SessionManager(context)
        preferenceHelper = PreferenceHelper(context)

        val db = AppDatabase.getDatabase(context)
        userRepository = UserRepository(db.userDao())
        habitRepository = HabitRepository(db.habitDao(), db.habitCompletionDao())

        setupTopBar()
        setupAccountSection()
        setupPreferencesSection()
        setupAboutSection()
        setupLogout()
    }

    private fun setupTopBar() {
        binding.btnSettingsBack.setOnClickListener {
            (activity as? MainActivity)?.navigateToHome()
        }

        binding.btnSettingsBell.setOnClickListener {
            Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAccountSection() {
        val userId = sessionManager.getCurrentUserId()

        // Display cached session name & image initially
        binding.tvSettingsUserName.text = sessionManager.getCurrentUserName() ?: "User"
        ProfileImageHelper.loadProfileImage(
            binding.ivSettingsAvatar,
            sessionManager.getCurrentUserProfileImageUri()
        )

        // Observe Room UserEntity in real-time
        if (userId > 0) {
            userRepository.getUserByIdLiveData(userId).observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    binding.tvSettingsUserName.text = user.fullName
                    ProfileImageHelper.loadProfileImage(
                        binding.ivSettingsAvatar,
                        user.profileImageUri
                    )
                }
            }
        }

        // Tapping avatar or account card opens image picker
        binding.ivSettingsAvatar.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.cardAccountProfile.setOnClickListener {
            showProfileDetailsDialog()
        }

        binding.tvSettingsViewProfile.setOnClickListener {
            showProfileDetailsDialog()
        }
    }

    private fun handleSelectedProfileImage(sourceUri: Uri) {
        val userId = sessionManager.getCurrentUserId()
        if (userId <= 0) return

        // Persist read permission if available
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(sourceUri, flags)
        } catch (e: Exception) {
            // Ignored if content provider doesn't support persistable permission
        }

        lifecycleScope.launch {
            // Safely copy to private app files directory so it survives app restarts
            val localUri = withContext(Dispatchers.IO) {
                ProfileImageHelper.copyUriToInternalStorage(requireContext(), sourceUri, userId)
            }

            val savedUriString = (localUri ?: sourceUri).toString()

            withContext(Dispatchers.IO) {
                userRepository.updateProfileImage(userId, savedUriString)
            }

            sessionManager.saveUserProfileImageUri(savedUriString)
            ProfileImageHelper.loadProfileImage(binding.ivSettingsAvatar, savedUriString)
            Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProfileDetailsDialog() {
        val context = requireContext()
        val userId = sessionManager.getCurrentUserId()
        val dialogBinding = DialogProfileDetailsBinding.inflate(layoutInflater)

        dialogBinding.tvProfileName.text = sessionManager.getCurrentUserName() ?: "User"
        dialogBinding.tvProfileEmail.text = sessionManager.getCurrentUserEmail() ?: ""
        ProfileImageHelper.loadProfileImage(
            dialogBinding.ivProfileAvatarLarge,
            sessionManager.getCurrentUserProfileImageUri()
        )

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnChangePhoto.setOnClickListener {
            dialog.dismiss()
            imagePickerLauncher.launch("image/*")
        }

        dialogBinding.btnCloseProfile.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupPreferencesSection() {
        // 1. Master Notifications Switch
        binding.switchNotificationsMaster.isChecked = preferenceHelper.isNotificationsMasterEnabled
        binding.switchNotificationsMaster.setOnCheckedChangeListener { _, isChecked ->
            preferenceHelper.isNotificationsMasterEnabled = isChecked

            if (isChecked) {
                // Request Android 13+ permission if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                        requestNotificationPermissionLauncher.launch(permission)
                    }
                }
                // Reschedule daily mood reminder
                MoodReminderScheduler.scheduleMoodReminder(requireContext())
                // Reschedule all active habit reminders
                lifecycleScope.launch(Dispatchers.IO) {
                    val activeHabits = habitRepository.getAllActiveReminders()
                    for (habit in activeHabits) {
                        HabitReminderScheduler.scheduleReminder(requireContext(), habit)
                    }
                }
                Toast.makeText(requireContext(), "Reminders enabled", Toast.LENGTH_SHORT).show()
            } else {
                // Cancel daily mood reminder
                MoodReminderScheduler.cancelMoodReminder(requireContext())
                // Cancel all active habit reminders
                lifecycleScope.launch(Dispatchers.IO) {
                    val activeHabits = habitRepository.getAllActiveReminders()
                    for (habit in activeHabits) {
                        HabitReminderScheduler.cancelReminder(requireContext(), habit.id)
                    }
                }
                Toast.makeText(requireContext(), "Reminders paused", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Dark Mode Switch
        binding.switchDarkMode.isChecked = preferenceHelper.isDarkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (preferenceHelper.isDarkMode != isChecked) {
                preferenceHelper.isDarkMode = isChecked
            }
        }

        // 3. Daily Mood Reminder Time
        updateMoodReminderTimeDisplay()
        binding.cardReminderTime.setOnClickListener {
            showMoodReminderTimePicker()
        }

        // 4. Language Selection
        binding.cardLanguage.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_language)
                .setMessage("Spark is currently localized in English. Additional language options will be introduced in future updates.")
                .setPositiveButton(R.string.action_done, null)
                .show()
        }
    }

    private fun updateMoodReminderTimeDisplay() {
        val timeStr = preferenceHelper.moodReminderTime
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        binding.tvReminderTimeValue.text = DateUtils.formatTime12Hour(hour, minute)
    }

    private fun showMoodReminderTimePicker() {
        val timeStr = preferenceHelper.moodReminderTime
        val parts = timeStr.split(":")
        val currentHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val currentMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val newTimeStr = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                preferenceHelper.moodReminderTime = newTimeStr
                updateMoodReminderTimeDisplay()

                if (preferenceHelper.isNotificationsMasterEnabled) {
                    MoodReminderScheduler.scheduleMoodReminder(requireContext())
                }
                Toast.makeText(requireContext(), "Mood reminder time updated", Toast.LENGTH_SHORT).show()
            },
            currentHour,
            currentMinute,
            false
        ).show()
    }

    private fun setupAboutSection() {
        binding.rowHelpCenter.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_help_center)
                .setMessage(
                    "Welcome to Spark!\n\n" +
                    "• Habits: Build positive routines by completing your daily habits.\n\n" +
                    "• Mood Check-In: Reflect on how you are feeling every day and record gentle notes.\n\n" +
                    "• Statistics: View your consistency, daily success rate, and emotional balance across weeks and months.\n\n" +
                    "• Complete Privacy: Spark is 100% offline. No data is ever sent to external cloud servers."
                )
                .setPositiveButton(R.string.action_done, null)
                .show()
        }

        binding.rowPrivacyPolicy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_privacy_policy)
                .setMessage(
                    "Spark Privacy Commitment:\n\n" +
                    "• 100% Local Storage: All habits, accounts, and mood records remain stored exclusively on your device.\n\n" +
                    "• Zero Cloud Telemetry: No user analytics, identifiers, or tracking SDKs are embedded.\n\n" +
                    "• Offline Operation: The app operates completely without an internet connection."
                )
                .setPositiveButton(R.string.action_done, null)
                .show()
        }

        binding.rowDemoData.setOnClickListener {
            showDemoOptionsDialog()
        }

        binding.tvSettingsAppVersion.setOnLongClickListener {
            showDemoOptionsDialog()
            true
        }
    }

    private fun showDemoOptionsDialog() {
        val userId = sessionManager.getCurrentUserId()
        if (userId <= 0) return

        lifecycleScope.launch {
            val (moodCount, compCount) = withContext(Dispatchers.IO) {
                com.example.spark.util.DemoDataGenerator.getDemoCounts(requireContext(), userId)
            }

            val message = if (moodCount > 0 || compCount > 0) {
                "Active Demo Data:\n• $moodCount sample mood entries\n• $compCount sample habit completions\n\nDemo records light up your statistics and insights while keeping real data safe. Clear demo data when done reviewing."
            } else {
                "No demo data currently active.\n\nGenerate 14 days of realistic sample mood and habit history to populate the Statistics charts and insights screen immediately."
            }

            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Developer / Demo Options")
                .setMessage(message)

            if (moodCount > 0 || compCount > 0) {
                builder.setPositiveButton("Clear Demo Data") { _, _ ->
                    lifecycleScope.launch {
                        val (clearedMoods, clearedComps) = withContext(Dispatchers.IO) {
                            com.example.spark.util.DemoDataGenerator.clearDemoHistory(requireContext(), userId)
                        }
                        Toast.makeText(
                            requireContext(),
                            "Cleared $clearedMoods demo moods and $clearedComps demo completions",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                builder.setNeutralButton("Regenerate") { _, _ ->
                    generateDemoData(userId)
                }
                builder.setNegativeButton("Close", null)
            } else {
                builder.setPositiveButton("Generate 14 Days") { _, _ ->
                    generateDemoData(userId)
                }
                builder.setNegativeButton("Cancel", null)
            }

            builder.show()
        }
    }

    private fun generateDemoData(userId: Long) {
        lifecycleScope.launch {
            val (moods, comps) = withContext(Dispatchers.IO) {
                com.example.spark.util.DemoDataGenerator.generateDemoHistory(requireContext(), userId)
            }
            Toast.makeText(
                requireContext(),
                "Generated $moods demo moods and $comps habit completions!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupLogout() {
        binding.cardLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_log_out)
                .setMessage("Are you sure you want to log out of Spark?")
                .setPositiveButton(R.string.settings_log_out) { _, _ ->
                    performLogout()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private fun performLogout() {
        sessionManager.clearSession()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finishAffinity()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
