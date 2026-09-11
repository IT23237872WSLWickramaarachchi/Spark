package com.example.spark.ui.habit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import com.example.spark.R
import com.example.spark.databinding.FragmentAddHabitBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * BottomSheetDialogFragment for creating a new habit.
 *
 * Implements FR-03: Create habit with name, category, frequency,
 * reminder time, and color theme.
 */
class AddHabitBottomSheetFragment : BottomSheetDialogFragment() {

    interface OnHabitSavedListener {
        fun onHabitSaved(
            title: String,
            category: String,
            frequency: String,
            targetDaysPerWeek: Int,
            reminderTime: LocalTime?,
            colorHex: String
        )
    }

    private var _binding: FragmentAddHabitBinding? = null
    private val binding get() = _binding!!

    var onHabitSavedListener: OnHabitSavedListener? = null

    // State
    private var selectedHour: Int = 8
    private var selectedMinute: Int = 0
    private var selectedColorHex: String = "#52559C"
    private val colorSwatches = listOf(
        "#52559C", // Primary Lilac
        "#1C8554", // Emerald Green
        "#874D5E", // Rose Coral
        "#2563EB", // Ocean Blue
        "#D97706"  // Amber Orange
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

        setupCloseButton()
        setupNameInput()
        setupReminderPicker()
        setupColorSwatches()
        setupSaveButton()
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun setupNameInput() {
        binding.etHabitName.doAfterTextChanged { text ->
            val isNotBlank = !text.isNullOrBlank()
            binding.btnSaveHabit.isEnabled = isNotBlank
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
                selectedColorHex = colorSwatches[index]
                swatchRings.forEachIndexed { rIndex, ringView ->
                    ringView.visibility = if (rIndex == index) View.VISIBLE else View.INVISIBLE
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveHabit.setOnClickListener {
            val name = binding.etHabitName.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) return@setOnClickListener

            val category = when (binding.cgCategory.checkedChipId) {
                R.id.chipMindfulness -> "Mindfulness"
                R.id.chipProductivity -> "Productivity"
                else -> "Health"
            }

            val isDaily = binding.toggleGroupFrequency.checkedButtonId == R.id.btnDaily
            val frequency = if (isDaily) "Daily" else "Weekly"
            val targetDays = if (isDaily) 7 else 3
            val reminderTime = LocalTime.of(selectedHour, selectedMinute)

            // 1. Notify via setFragmentResult
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    EXTRA_TITLE to name,
                    EXTRA_CATEGORY to category,
                    EXTRA_FREQUENCY to frequency,
                    EXTRA_TARGET_DAYS to targetDays,
                    EXTRA_REMINDER_HOUR to selectedHour,
                    EXTRA_REMINDER_MINUTE to selectedMinute,
                    EXTRA_COLOR_HEX to selectedColorHex
                )
            )

            // 2. Notify optional direct listener
            onHabitSavedListener?.onHabitSaved(
                title = name,
                category = category,
                frequency = frequency,
                targetDaysPerWeek = targetDays,
                reminderTime = reminderTime,
                colorHex = selectedColorHex
            )

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddHabitBottomSheetFragment"
        const val REQUEST_KEY = "request_add_habit"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_FREQUENCY = "extra_frequency"
        const val EXTRA_TARGET_DAYS = "extra_target_days"
        const val EXTRA_REMINDER_HOUR = "extra_reminder_hour"
        const val EXTRA_REMINDER_MINUTE = "extra_reminder_minute"
        const val EXTRA_COLOR_HEX = "extra_color_hex"

        fun newInstance(): AddHabitBottomSheetFragment = AddHabitBottomSheetFragment()
    }
}
