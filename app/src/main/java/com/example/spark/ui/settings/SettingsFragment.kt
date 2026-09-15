package com.example.spark.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.data.local.entity.UserPrefsEntity
import com.example.spark.data.repository.UserRepository
import com.example.spark.databinding.FragmentSettingsBinding
import com.example.spark.databinding.ItemSettingsRowBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * SettingsFragment displaying user profile, preferences, about info,
 * and session logout action matching the Spark design tokens.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: com.example.spark.data.local.SessionManager

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

        val app = requireActivity().application as SparkApplication
        userRepository = app.userRepository
        sessionManager = app.sessionManager

        setupToolbar()
        setupAccountSection()
        setupPreferencesSection()
        setupAboutSection()
        setupLogOutButton()
        observePreferences()
    }

    private fun setupToolbar() {
        binding.ibToolbarBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.ibToolbarBell.setOnClickListener {
            // Notifications hub action
        }
    }

    private fun setupAccountSection() {
        binding.cardProfile.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.settings_profile_name), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPreferencesSection() {
        // Row 1: Notifications (SwitchMaterial)
        setupRow(
            rowBinding = binding.rowNotifications,
            iconRes = R.drawable.ic_bell,
            label = getString(R.string.settings_notifications)
        ) {
            switchRow.visibility = View.VISIBLE
            switchRow.isChecked = true
            switchRow.setOnCheckedChangeListener { _, isChecked ->
                onNotificationsToggled(isChecked)
            }
        }

        // Row 2: Dark Mode (SwitchMaterial)
        setupRow(
            rowBinding = binding.rowDarkMode,
            iconRes = R.drawable.ic_moon,
            label = getString(R.string.settings_dark_mode)
        ) {
            switchRow.visibility = View.VISIBLE
            switchRow.setOnCheckedChangeListener { _, isChecked ->
                onDarkModeToggled(isChecked)
            }
        }

        // Row 3: Reminder Time (Value + Chevron)
        setupRow(
            rowBinding = binding.rowReminderTime,
            iconRes = R.drawable.ic_clock,
            label = getString(R.string.settings_reminder_time)
        ) {
            layoutValueChevron.visibility = View.VISIBLE
            tvRowValue.text = getString(R.string.settings_reminder_time_value)
            root.setOnClickListener {
                showReminderTimePicker()
            }
        }

        // Row 4: Language (Value + Chevron)
        setupRow(
            rowBinding = binding.rowLanguage,
            iconRes = R.drawable.ic_globe,
            label = getString(R.string.settings_language)
        ) {
            layoutValueChevron.visibility = View.VISIBLE
            tvRowValue.text = getString(R.string.settings_language_value)
            root.setOnClickListener {
                onLanguageClicked()
            }
        }
    }

    private fun setupAboutSection() {
        // Row 5: Help Center (Chevron only)
        setupRow(
            rowBinding = binding.rowHelpCenter,
            iconRes = R.drawable.ic_help,
            label = getString(R.string.settings_help_center)
        ) {
            ivOnlyChevron.visibility = View.VISIBLE
            root.setOnClickListener {
                Toast.makeText(requireContext(), getString(R.string.settings_help_center), Toast.LENGTH_SHORT).show()
            }
        }

        // Row 6: Privacy Policy (Chevron only)
        setupRow(
            rowBinding = binding.rowPrivacyPolicy,
            iconRes = R.drawable.ic_privacy_tip,
            label = getString(R.string.settings_privacy_policy)
        ) {
            ivOnlyChevron.visibility = View.VISIBLE
            root.setOnClickListener {
                Toast.makeText(requireContext(), getString(R.string.settings_privacy_policy), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLogOutButton() {
        binding.btnLogOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_log_out)
                .setMessage("Are you sure you want to log out of Spark?")
                .setPositiveButton(R.string.settings_log_out) { _, _ ->
                    onLogOutConfirmed()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private inline fun setupRow(
        rowBinding: ItemSettingsRowBinding,
        @DrawableRes iconRes: Int,
        label: String,
        configure: ItemSettingsRowBinding.() -> Unit
    ) {
        rowBinding.ivRowIcon.setImageResource(iconRes)
        rowBinding.ivRowIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), R.color.primary)
        )
        rowBinding.tvRowLabel.text = label
        rowBinding.configure()
    }

    private fun observePreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userRepository.getUserPrefsFlow().collect { prefs ->
                    val notificationsEnabled = prefs?.notificationsEnabled ?: true
                    if (binding.rowNotifications.switchRow.isChecked != notificationsEnabled) {
                        binding.rowNotifications.switchRow.isChecked = notificationsEnabled
                    }

                    val darkModeEnabled = prefs?.darkModeEnabled ?: false
                    if (binding.rowDarkMode.switchRow.isChecked != darkModeEnabled) {
                        binding.rowDarkMode.switchRow.isChecked = darkModeEnabled
                    }
                }
            }
        }
    }

    private fun onNotificationsToggled(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val current = userRepository.getUserPrefs() ?: UserPrefsEntity(userId = 1L)
            userRepository.updatePrefs(current.copy(notificationsEnabled = enabled))
        }
    }

    private fun onDarkModeToggled(enabled: Boolean) {
        val targetMode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val current = userRepository.getUserPrefs() ?: UserPrefsEntity(userId = 1L)
            userRepository.updatePrefs(current.copy(darkModeEnabled = enabled))
        }
    }

    private fun showReminderTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(R.string.settings_reminder_time)
            .build()

        picker.addOnPositiveButtonClickListener {
            val formattedMinute = String.format(Locale.getDefault(), "%02d", picker.minute)
            val period = if (picker.hour >= 12) "PM" else "AM"
            val displayHour = when {
                picker.hour == 0 -> 12
                picker.hour > 12 -> picker.hour - 12
                else -> picker.hour
            }
            val timeDisplay = "$displayHour:$formattedMinute $period"
            binding.rowReminderTime.tvRowValue.text = timeDisplay

            // Persist to UserPrefsEntity
            viewLifecycleOwner.lifecycleScope.launch {
                val current = userRepository.getUserPrefs() ?: UserPrefsEntity(userId = 1L)
                userRepository.updatePrefs(current.copy(reminderTime = timeDisplay))
            }
        }

        picker.show(parentFragmentManager, "SettingsReminderTimePicker")
    }

    private fun onLanguageClicked() {
        val languages = arrayOf("English", "Spanish", "French", "German")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_language)
            .setItems(languages) { _, which ->
                val selectedLang = languages[which]
                binding.rowLanguage.tvRowValue.text = selectedLang

                // Persist to UserPrefsEntity
                viewLifecycleOwner.lifecycleScope.launch {
                    val current = userRepository.getUserPrefs() ?: UserPrefsEntity(userId = 1L)
                    userRepository.updatePrefs(current.copy(language = selectedLang))
                }
            }
            .show()
    }

    private fun onLogOutConfirmed() {
        sessionManager.clearSession()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_settings_to_welcome)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
