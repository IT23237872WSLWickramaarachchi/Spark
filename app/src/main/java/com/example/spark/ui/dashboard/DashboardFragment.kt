package com.example.spark.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var habitAdapter: HabitAdapter
    private var hasSeededDefaults = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setupToolbarAndGreeting()
        setupMoodPill()
        setupHabitsRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbarAndGreeting() {
        binding.ivToolbarBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnNotifications.setOnClickListener {
            // Notification click placeholder
        }

        // Set contextual greeting by time of day
        val hour = LocalTime.now().hour
        val greetingPrefix = when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        binding.tvGreeting.text = "$greetingPrefix, Alex"
    }

    private fun setupMoodPill() {
        binding.btnDismissMood.setOnClickListener {
            binding.cardMoodPill.visibility = View.GONE
        }

        binding.cardMoodPill.setOnClickListener {
            // Placeholder to open mood check-in flow
        }
    }

    private fun setupHabitsRecyclerView() {
        habitAdapter = HabitAdapter(
            onToggleHabit = { habitUi ->
                viewModel.toggleHabitCompletion(habitUi.id)
            },
            onItemClick = { habitUi ->
                // Optional navigation to habit detail
            }
        )
        binding.rvHabits.adapter = habitAdapter
    }

    private fun setupFab() {
        binding.fabAddHabit.setOnClickListener {
            // FAB placeholder to add a new habit
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.habitsWithLogs.collect { list ->
                    if (list.isEmpty() && !hasSeededDefaults) {
                        hasSeededDefaults = true
                        seedDefaultHabits()
                        return@collect
                    }

                    val today = LocalDate.now()
                    val habitUiModels = list.map { habitWithLogs ->
                        mapToUiModel(habitWithLogs, today)
                    }

                    // Update Habit List & Empty State
                    habitAdapter.submitList(habitUiModels)
                    val isEmpty = habitUiModels.isEmpty()
                    binding.rvHabits.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE

                    // Update Progress Ring & Stats
                    val total = habitUiModels.size
                    val completed = habitUiModels.count { it.isCompleted }
                    val percent = if (total > 0) (completed * 100) / total else 0

                    binding.circularProgressView.setProgress(percent.toFloat(), animated = true)
                    binding.tvProgressPercent.text = "$percent%"
                    binding.tvProgressHabitsCount.text = "$completed of $total habits"

                    // Calculate Best Streak
                    var bestStreak = 7 // Default reference streak
                    if (list.isNotEmpty()) {
                        val maxStreakFromDb = list.maxOfOrNull {
                            viewModel.calculateStreak(it.logs).currentStreak
                        } ?: 0
                        if (maxStreakFromDb > 0) {
                            bestStreak = maxStreakFromDb
                        }
                    }
                    binding.tvStreakBadge.text = "$bestStreak day streak"
                }
            }
        }
    }

    private fun mapToUiModel(habitWithLogs: HabitWithLogs, today: LocalDate): HabitUiModel {
        val habit = habitWithLogs.habit
        val isDone = habitWithLogs.logs.any { it.completedDate == today && it.isCompleted }

        // Category-based icon and background palette matching the design tokens
        val (iconRes, iconColor, bgColor) = when {
            habit.title.contains("Water", ignoreCase = true) ->
                Triple(R.drawable.ic_water_drop, Color.parseColor("#52559C"), Color.parseColor("#ECE9F1"))
            habit.title.contains("Meditation", ignoreCase = true) ->
                Triple(R.drawable.ic_meditation, Color.parseColor("#1C192A"), Color.parseColor("#F2A9BC33"))
            habit.title.contains("Read", ignoreCase = true) || habit.title.contains("Page", ignoreCase = true) ->
                Triple(R.drawable.ic_book, Color.parseColor("#6FCF97"), Color.parseColor("#6FCF9733"))
            habit.title.contains("Walk", ignoreCase = true) ->
                Triple(R.drawable.ic_walk, Color.parseColor("#5D5FA6"), Color.parseColor("#ECE9F1"))
            habit.title.contains("Sleep", ignoreCase = true) ->
                Triple(R.drawable.ic_moon, Color.parseColor("#1C192A"), Color.parseColor("#F2A9BC33"))
            else ->
                Triple(R.drawable.ic_streak_flame, Color.parseColor("#52559C"), Color.parseColor("#ECE9F1"))
        }

        return HabitUiModel(
            id = habit.id,
            title = habit.title,
            subtitle = habit.frequency.ifEmpty { "Daily • 9:00 AM" },
            category = habit.category,
            iconRes = iconRes,
            iconColor = iconColor,
            iconBgColor = bgColor,
            isCompleted = isDone
        )
    }

    /**
     * Seeds initial reference habits into the database on fresh install.
     */
    private fun seedDefaultHabits() {
        viewModel.createHabit("Drink Water", "Health", "Morning • 8:00 AM")
        viewModel.createHabit("10 Min Meditation", "Mindfulness", "Morning • 9:00 AM")
        viewModel.createHabit("Read 15 Pages", "Learning", "Afternoon • 1:00 PM")
        viewModel.createHabit("Evening Walk", "Fitness", "Evening • 6:00 PM")
        viewModel.createHabit("Sleep by 11 PM", "Sleep", "Night • 11:00 PM")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
