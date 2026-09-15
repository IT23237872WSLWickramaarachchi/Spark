package com.example.spark.ui.habit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.databinding.FragmentAddHabitBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * BottomSheetDialogFragment for creating or editing a habit.
 *
 * Implements FR-03: Create/Edit habit with name, category, frequency,
 * reminder time, and color theme.
 */
class AddHabitBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddHabitBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddHabitViewModel
    private var isInitialFormPopulated: Boolean = false

    // State
    private var selectedHour: Int = 8
    private var selectedMinute: Int = 0
    private val colorSwatches = listOf(
        "#52559C", // Primary Purple
        "#874D5E", // Secondary Mauve/Pink
        "#006A3F", // Tertiary Green
        "#7ADAA1", // Tertiary Fixed Dim Light Green
        "#FFD9E1"  // Secondary Fixed Light Pink
    )

    override fun getTheme(): Int = R.style.Widget_Spark_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as SparkApplication
        viewModel = ViewModelProvider(
            this,
            AddHabitViewModel.Factory(
                habitRepository = app.habitRepository,
                defaultArgs = arguments,
                currentUserIdProvider = { app.sessionManager.currentUserId }
            )
        )[AddHabitViewModel::class.java]

        setupCloseButton()
        setupNameInput()
        setupCategoryChips()
        setupFrequencyToggle()
        setupReminderPicker()
        setupColorSwatches()
        setupSaveButton()
        observeViewModel()
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupNameInput() {
        binding.etHabitName.doAfterTextChanged { text ->
            viewModel.onNameChange(text?.toString().orEmpty())
        }
    }

    private fun setupCategoryChips() {
        binding.cgCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipMindfulness -> "Mindfulness"
                R.id.chipProductivity -> "Productivity"
                else -> "Health"
            }
            viewModel.onCategorySelect(category)
        }
    }

    private fun setupFrequencyToggle() {
        binding.toggleGroupFrequency.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val frequency = if (checkedId == R.id.btnDaily) "Daily" else "Weekly"
                viewModel.onFrequencySelect(frequency)
            }
        }
    }

    private fun setupReminderPicker() {
        updateReminderTimeUi()
        binding.cardReminderTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText(getString(R.string.add_habit_reminder_time_label))
                .build()

            picker.addOnPositiveButtonClickListener {
                selectedHour = picker.hour
                selectedMinute = picker.minute
                val formattedTime = String.format(Locale.ROOT, "%02d:%02d", selectedHour, selectedMinute)
                viewModel.onReminderTimeSelect(formattedTime)
                updateReminderTimeUi()
            }

            picker.show(childFragmentManager, "time_picker")
        }
    }

    private fun updateReminderTimeUi() {
        val time = LocalTime.of(selectedHour, selectedMinute)
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        binding.tvReminderTime.text = time.format(formatter)
    }

    private fun setupColorSwatches() {
        val swatchRings = listOf(
            binding.ring1,
            binding.ring2,
            binding.ring3,
            binding.ring4,
            binding.ring5
        )
        val swatches = listOf(
            binding.swatch1,
            binding.swatch2,
            binding.swatch3,
            binding.swatch4,
            binding.swatch5
        )

        swatches.forEachIndexed { index, swatchView ->
            swatchView.setOnClickListener {
                val colorHex = colorSwatches[index]
                viewModel.onColorSelect(colorHex)
                swatchRings.forEachIndexed { rIndex, ringView ->
                    ringView.visibility = if (rIndex == index) View.VISIBLE else View.INVISIBLE
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveHabit.setOnClickListener {
            viewModel.saveHabit()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.formState.collect { state ->
                        binding.btnSaveHabit.isEnabled = state.isSaveEnabled

                        // Pre-populate form when opening in Edit mode
                        if (!viewModel.habitId.isNullOrBlank() && !isInitialFormPopulated && state.name.isNotBlank()) {
                            isInitialFormPopulated = true
                            binding.tvHeaderTitle.text = "Edit Habit"
                            binding.btnSaveHabit.text = "Update Habit"

                            if (binding.etHabitName.text.toString() != state.name) {
                                binding.etHabitName.setText(state.name)
                                binding.etHabitName.setSelection(state.name.length)
                            }

                            val chipId = when (state.category) {
                                "Mindfulness" -> R.id.chipMindfulness
                                "Productivity" -> R.id.chipProductivity
                                else -> R.id.chipHealth
                            }
                            if (binding.cgCategory.checkedChipId != chipId) {
                                binding.cgCategory.check(chipId)
                            }

                            val freqBtnId = if (state.frequency.equals("Weekly", ignoreCase = true)) {
                                R.id.btnWeekly
                            } else {
                                R.id.btnDaily
                            }
                            if (binding.toggleGroupFrequency.checkedButtonId != freqBtnId) {
                                binding.toggleGroupFrequency.check(freqBtnId)
                            }

                            val timeParts = state.reminderTime.split(":")
                            if (timeParts.size == 2) {
                                selectedHour = timeParts[0].toIntOrNull() ?: 8
                                selectedMinute = timeParts[1].toIntOrNull() ?: 0
                                updateReminderTimeUi()
                            }

                            val colorIdx = colorSwatches.indexOfFirst { it.equals(state.colorTag, ignoreCase = true) }
                            if (colorIdx >= 0) {
                                val swatchRings = listOf(
                                    binding.ring1,
                                    binding.ring2,
                                    binding.ring3,
                                    binding.ring4,
                                    binding.ring5
                                )
                                swatchRings.forEachIndexed { rIndex, ringView ->
                                    ringView.visibility = if (rIndex == colorIdx) View.VISIBLE else View.INVISIBLE
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.habitSaved.collect {
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddHabitBottomSheetFragment"
        const val ARG_HABIT_ID = "habitId"

        fun newInstance(habitId: String? = null): AddHabitBottomSheetFragment {
            return AddHabitBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    if (habitId != null) {
                        putString(ARG_HABIT_ID, habitId)
                    }
                }
            }
        }
    }
}
