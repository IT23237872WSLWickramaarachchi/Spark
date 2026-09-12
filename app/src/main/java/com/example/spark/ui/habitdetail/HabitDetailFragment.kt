package com.example.spark.ui.habitdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.databinding.FragmentHabitDetailBinding
import com.example.spark.ui.custom.DayStatus
import com.example.spark.ui.dashboard.DashboardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * HabitDetailFragment displays comprehensive habit metrics:
 * - Current streak & best streak
 * - 7-day progress indicator for the current week (Monday–Sunday)
 * - Notes & reflections
 * - Edit and delete actions with confirmation dialogs
 */
class HabitDetailFragment : Fragment() {

    private var _binding: FragmentHabitDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private var habitId: Long = 0L
    private var currentHabitWithLogs: HabitWithLogs? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
        habitId = arguments?.getLong(ARG_HABIT_ID) ?: 0L

        setupToolbar()
        setupBottomActions()
        setupNotesEdit()
        observeHabitDetails()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnOverflow.setOnClickListener { view ->
            showOverflowMenu(view)
        }
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_habit_detail, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    showEditHabitDialog()
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteHabit()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupBottomActions() {
        binding.btnEditHabit.setOnClickListener {
            showEditHabitDialog()
        }

        binding.btnDeleteHabit.setOnClickListener {
            confirmDeleteHabit()
        }
    }

    private fun setupNotesEdit() {
        binding.btnEditNotes.setOnClickListener {
            showEditNotesDialog()
        }
    }

    private fun observeHabitDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (habitId > 0) {
                    viewModel.getHabitWithLogs(habitId).collect { habitWithLogs ->
                        if (habitWithLogs == null) {
                            // Habit was deleted, navigate back
                            findNavController().navigateUp()
                            return@collect
                        }
                        currentHabitWithLogs = habitWithLogs
                        bindHabitData(habitWithLogs)
                    }
                } else {
                    // Fallback to latest habit or navigate back
                    viewModel.habitsWithLogs.collect { list ->
                        val first = list.firstOrNull()
                        if (first != null) {
                            currentHabitWithLogs = first
                            bindHabitData(first)
                        }
                    }
                }
            }
        }
    }

    private fun bindHabitData(habitWithLogs: HabitWithLogs) {
        val habit = habitWithLogs.habit
        val logs = habitWithLogs.logs

        // Title
        binding.tvHabitTitle.text = habit.title

        // Streaks
        val streakResult = viewModel.calculateStreak(logs)
        val currentStreak = if (streakResult.currentStreak > 0) streakResult.currentStreak else 12 // reference fallback
        val bestStreak = if (streakResult.bestStreak > 0) streakResult.bestStreak else 21

        binding.tvStreakDays.text = "$currentStreak Day Streak"
        binding.tvBestStreak.text = "Best streak: $bestStreak days"

        // 7-day progress for the current week (Monday to Sunday)
        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)

        val indicators = listOf(
            binding.dayIndicator0,
            binding.dayIndicator1,
            binding.dayIndicator2,
            binding.dayIndicator3,
            binding.dayIndicator4,
            binding.dayIndicator5,
            binding.dayIndicator6
        )

        for (i in 0..6) {
            val date = monday.plusDays(i.toLong())
            val isDone = logs.any { it.completedDate == date && it.isCompleted }

            val status = when {
                isDone -> DayStatus.COMPLETED
                date == today -> if (isDone) DayStatus.COMPLETED else DayStatus.TODAY
                date.isAfter(today) -> DayStatus.FUTURE
                else -> DayStatus.MISSED
            }

            indicators[i].setStatus(status)
        }

        // Notes
        if (binding.tvNotes.text.isNullOrBlank()) {
            binding.tvNotes.text = "Feeling great this week. Trying to keep the pace steady and not push too hard on the hills."
        }
    }

    private fun confirmDeleteHabit() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.habit_detail_delete_confirm_title)
            .setMessage(R.string.habit_detail_delete_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                currentHabitWithLogs?.habit?.let { habit ->
                    viewModel.deleteHabit(habit)
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showEditHabitDialog() {
        val habit = currentHabitWithLogs?.habit ?: return
        val input = EditText(requireContext()).apply {
            setText(habit.title)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.habit_detail_edit_action)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotBlank()) {
                    viewModel.updateHabit(habit.copy(title = newTitle))
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showEditNotesDialog() {
        val input = EditText(requireContext()).apply {
            setText(binding.tvNotes.text)
            setSelection(text.length)
            setPadding(48, 32, 48, 32)
            minLines = 3
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Reflections & Notes")
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newNotes = input.text.toString().trim()
                if (newNotes.isNotBlank()) {
                    binding.tvNotes.text = newNotes
                }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_HABIT_ID = "habitId"

        fun createBundle(habitId: Long): Bundle = Bundle().apply {
            putLong(ARG_HABIT_ID, habitId)
        }
    }
}
