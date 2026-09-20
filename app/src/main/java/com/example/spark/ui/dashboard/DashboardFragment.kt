package com.example.spark.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spark.R
import com.example.spark.adapter.HabitAdapter
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.data.repository.UserRepository
import com.example.spark.databinding.FragmentDashboardBinding
import com.example.spark.model.HabitItemUiModel
import com.example.spark.model.MoodLevel
import com.example.spark.ui.habits.AddHabitBottomSheet
import com.example.spark.ui.habits.HabitDetailsActivity
import com.example.spark.ui.mood.MoodCheckInDialog
import com.example.spark.util.DateUtils
import com.example.spark.util.SessionManager
import com.example.spark.viewmodel.DashboardViewModel
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var habitAdapter: HabitAdapter
    private lateinit var sessionManager: SessionManager

    private var allHabitsList: List<HabitItemUiModel> = emptyList()
    private var searchQuery: String = ""
    private var filterStatus: String = "ALL"

    private val viewModel: DashboardViewModel by viewModels {
        val context = requireContext().applicationContext
        val db = AppDatabase.getDatabase(context)
        val habitRepo = HabitRepository(db.habitDao(), db.habitCompletionDao())
        val moodRepo = MoodRepository(db.moodEntryDao())
        val userId = SessionManager(context).getCurrentUserId()
        DashboardViewModel.Factory(habitRepo, moodRepo, userId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        com.example.spark.notifications.NotificationHelper.createNotificationChannel(requireContext())
        checkNotificationPermission()

        setupHeader()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        observeUserProfile()
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun setupHeader() {
        val fullName = sessionManager.getCurrentUserName().orEmpty()
        val firstName = fullName.split(" ").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "there"

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingPrefix = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        binding.tvGreeting.text = "$greetingPrefix,\n$firstName"

        binding.ivBell.setOnClickListener {
            Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
        }

        binding.cardMoodPill.setOnClickListener {
            openMoodDialog()
        }

        binding.ivMoodDismiss.setOnClickListener {
            binding.cardMoodPill.visibility = View.GONE
        }

        binding.ivAvatar.setOnClickListener {
            (activity as? com.example.spark.MainActivity)?.navigateToSettings()
        }
        updateUserAvatar()
    }

    /**
     * Observe Room UserEntity in real-time so profile image updates from Settings
     * are reflected immediately without needing to reload the fragment.
     */
    private fun observeUserProfile() {
        val userId = sessionManager.getCurrentUserId()
        if (userId > 0) {
            val db = AppDatabase.getDatabase(requireContext())
            val userRepo = UserRepository(db.userDao())
            userRepo.getUserByIdLiveData(userId).observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    com.example.spark.util.ProfileImageHelper.loadProfileImage(
                        binding.ivAvatar,
                        user.profileImageUri
                    )
                }
            }
        }
    }

    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            onToggleComplete = { item, isChecked ->
                viewModel.toggleHabit(item.habit.id)
            },
            onHabitClick = { habitId ->
                val intent = Intent(requireContext(), HabitDetailsActivity::class.java).apply {
                    putExtra(HabitDetailsActivity.EXTRA_HABIT_ID, habitId)
                }
                startActivity(intent)
            }
        )

        binding.rvTodayHabits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = habitAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        // FR-10: Subtle Search toggle
        binding.btnSearchHabits.setOnClickListener {
            if (binding.tilSearchHabits.visibility == View.VISIBLE) {
                binding.tilSearchHabits.visibility = View.GONE
                binding.etSearchHabits.setText("")
                searchQuery = ""
                binding.btnSearchHabits.setColorFilter(ContextCompat.getColor(requireContext(), R.color.outline))
                applyFilterAndSearch()
            } else {
                binding.tilSearchHabits.visibility = View.VISIBLE
                binding.etSearchHabits.requestFocus()
                binding.btnSearchHabits.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        }

        binding.etSearchHabits.doAfterTextChanged { text ->
            searchQuery = text?.toString()?.trim().orEmpty()
            applyFilterAndSearch()
        }

        // FR-10: Subtle Filter toggle
        binding.btnFilterHabits.setOnClickListener {
            if (binding.scrollFilterChips.visibility == View.VISIBLE) {
                binding.scrollFilterChips.visibility = View.GONE
                binding.btnFilterHabits.setColorFilter(ContextCompat.getColor(requireContext(), R.color.outline))
            } else {
                binding.scrollFilterChips.visibility = View.VISIBLE
                binding.btnFilterHabits.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        }

        binding.chipFilterMore.setOnClickListener {
            val categories = com.example.spark.model.HabitCategories.ALL
            val items = categories.map { it.title }.toTypedArray()
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter by Category")
                .setItems(items) { _, which ->
                    val chosen = items[which]
                    when (chosen.lowercase()) {
                        "health" -> binding.chipFilterHealth.isChecked = true
                        "fitness" -> binding.chipFilterFitness.isChecked = true
                        "mindfulness" -> binding.chipFilterMindfulness.isChecked = true
                        "productivity" -> binding.chipFilterProductivity.isChecked = true
                        "study" -> binding.chipFilterStudy.isChecked = true
                        else -> {
                            binding.chipFilterCustom.text = chosen
                            binding.chipFilterCustom.visibility = View.VISIBLE
                            binding.chipFilterCustom.isChecked = true
                            filterStatus = chosen.uppercase()
                            applyFilterAndSearch()
                        }
                    }
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipFilterAll
            filterStatus = when (checkedId) {
                R.id.chipFilterPending -> "PENDING"
                R.id.chipFilterCompleted -> "COMPLETED"
                R.id.chipFilterHealth -> "HEALTH"
                R.id.chipFilterFitness -> "FITNESS"
                R.id.chipFilterMindfulness -> "MINDFULNESS"
                R.id.chipFilterProductivity -> "PRODUCTIVITY"
                R.id.chipFilterStudy -> "STUDY"
                R.id.chipFilterCustom -> binding.chipFilterCustom.text.toString().uppercase()
                else -> "ALL"
            }
            applyFilterAndSearch()
        }
    }

    private fun applyFilterAndSearch() {
        if (allHabitsList.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.layoutEmptySearchState.visibility = View.GONE
            binding.rvTodayHabits.visibility = View.GONE
            return
        }

        binding.layoutEmptyState.visibility = View.GONE

        var filtered = allHabitsList

        // 1. Status / Category Filter
        filtered = when (filterStatus) {
            "PENDING" -> filtered.filter { !it.isCompletedToday }
            "COMPLETED" -> filtered.filter { it.isCompletedToday }
            "ALL" -> filtered
            else -> filtered.filter { it.habit.category.equals(filterStatus, ignoreCase = true) }
        }

        // 2. Search Query Filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.habit.name.contains(searchQuery, ignoreCase = true) ||
                it.habit.category.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            binding.layoutEmptySearchState.visibility = View.VISIBLE
            binding.rvTodayHabits.visibility = View.GONE
        } else {
            binding.layoutEmptySearchState.visibility = View.GONE
            binding.rvTodayHabits.visibility = View.VISIBLE
            habitAdapter.submitList(filtered)
        }
    }

    private fun observeViewModel() {
        viewModel.habitItems.observe(viewLifecycleOwner) { habits ->
            allHabitsList = habits
            val total = habits.size
            val completed = habits.count { it.isCompletedToday }
            val percentage = if (total > 0) ((completed.toDouble() / total) * 100).toInt() else 0

            binding.progressView.setProgress(percentage.toFloat(), animated = true)
            binding.tvProgressPercentage.text = "$percentage%"
            binding.tvProgressCount.text = "$completed of $total habits"

            applyFilterAndSearch()
        }

        viewModel.streakDays.observe(viewLifecycleOwner) { streak ->
            binding.tvStreakBadge.text = if (streak == 1) "1 day streak" else "$streak day streak"
        }

        viewModel.todayMood.observe(viewLifecycleOwner) { moodEntity ->
            if (moodEntity != null) {
                val mood = MoodLevel.fromLevel(moodEntity.moodLevel)
                binding.ivMoodIcon.setImageResource(mood.iconResId)

                // Show mood with time for latest check-in
                val timeDisplay = if (moodEntity.time.isNotEmpty()) {
                    try {
                        val parts = moodEntity.time.split(":")
                        val hour = parts[0].toInt()
                        val min = parts[1].toInt()
                        " • ${DateUtils.formatTime12Hour(hour, min)}"
                    } catch (e: Exception) { "" }
                } else ""
                binding.tvMoodText.text = "${mood.dashboardText}$timeDisplay"
            } else {
                binding.ivMoodIcon.setImageResource(R.drawable.ic_mood_smile)
                binding.tvMoodText.text = "How are you feeling today?"
            }
        }
    }

    fun openAddHabitBottomSheet() {
        val sheet = AddHabitBottomSheet.newInstance()
        sheet.show(parentFragmentManager, AddHabitBottomSheet.TAG)
    }

    fun openMoodDialog() {
        val dialog = MoodCheckInDialog.newInstance()
        dialog.show(parentFragmentManager, MoodCheckInDialog.TAG)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStreak()
    }

    private fun updateUserAvatar() {
        val uri = sessionManager.getCurrentUserProfileImageUri()
        com.example.spark.util.ProfileImageHelper.loadProfileImage(binding.ivAvatar, uri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): DashboardFragment = DashboardFragment()
    }
}
