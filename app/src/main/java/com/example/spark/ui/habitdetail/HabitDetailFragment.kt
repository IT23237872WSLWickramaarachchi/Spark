package com.example.spark.ui.habitdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.databinding.FragmentHabitDetailBinding
import com.example.spark.ui.habit.AddHabitBottomSheetFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * HabitDetailFragment displays comprehensive habit metrics:
 * - Current streak & best streak
 * - 7-day progress indicator for the current week (Monday–Sunday)
 * - Notes & reflections
 * - Edit and delete actions
 */
class HabitDetailFragment : Fragment() {

    private var _binding: FragmentHabitDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HabitDetailViewModel

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

        val app = requireActivity().application as SparkApplication
        viewModel = ViewModelProvider(
            this,
            HabitDetailViewModel.Factory(app.habitRepository, this, arguments)
        )[HabitDetailViewModel::class.java]

        setupToolbar()
        setupBottomActions()
        setupNotesEdit()
        observeViewModel()
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
                    navigateToEditHabit()
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
            navigateToEditHabit()
        }

        binding.btnDeleteHabit.setOnClickListener {
            confirmDeleteHabit()
        }
    }

    private fun navigateToEditHabit() {
        val habitId = viewModel.habitId
        val bundle = bundleOf(AddHabitBottomSheetFragment.ARG_HABIT_ID to habitId)
        try {
            findNavController().navigate(R.id.action_habitDetail_to_addHabit, bundle)
        } catch (e: Exception) {
            AddHabitBottomSheetFragment.newInstance(habitId).show(childFragmentManager, AddHabitBottomSheetFragment.TAG)
        }
    }

    private fun setupNotesEdit() {
        binding.btnEditNotes.setOnClickListener {
            showEditNotesDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        bindHabitData(state)
                    }
                }
                launch {
                    viewModel.habitDeleted.collect {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun bindHabitData(state: HabitDetailUiState) {
        if (state.title.isBlank()) return

        // Title
        binding.tvHabitTitle.text = state.title

        // Streaks
        binding.tvStreakDays.text = "${state.currentStreak} Day Streak"
        binding.tvBestStreak.text = "Best streak: ${state.bestStreak} days"

        // 7-day progress for the current week (Monday to Sunday)
        val indicators = listOf(
            binding.dayIndicator0,
            binding.dayIndicator1,
            binding.dayIndicator2,
            binding.dayIndicator3,
            binding.dayIndicator4,
            binding.dayIndicator5,
            binding.dayIndicator6
        )

        state.weeklyStatuses.forEachIndexed { index, status ->
            if (index < indicators.size) {
                indicators[index].setStatus(status)
            }
        }

        // Notes
        binding.tvNotes.text = state.notes.ifBlank {
            "Tap edit to add your reflections and notes."
        }
    }

    private fun confirmDeleteHabit() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.habit_detail_delete_confirm_title)
            .setMessage(R.string.habit_detail_delete_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteHabit()
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
                    viewModel.updateNotes(newNotes)
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
            putString(ARG_HABIT_ID, habitId.toString())
        }
    }
}
