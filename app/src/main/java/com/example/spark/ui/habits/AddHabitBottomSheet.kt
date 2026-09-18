package com.example.spark.ui.habits

import android.app.TimePickerDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.databinding.BottomSheetAddHabitBinding
import com.example.spark.model.HabitCategories
import com.example.spark.model.HabitColors
import com.example.spark.notifications.HabitReminderScheduler
import com.example.spark.util.DateUtils
import com.example.spark.util.PreferenceHelper
import com.example.spark.util.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddHabitBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddHabitBinding? = null
    private val binding get() = _binding!!

    private var habitIdToEdit: Long = -1L
    private var existingHabit: HabitEntity? = null

    private var selectedCategory: String = "Health"
    private var selectedFrequency: String = "DAILY"
    private var reminderHour: Int = 8
    private var reminderMinute: Int = 0
    private var reminderEnabled: Boolean = true
    private var selectedColorTag: String = "purple"

    private var onHabitSavedListener: (() -> Unit)? = null

    fun setOnHabitSavedListener(listener: () -> Unit) {
        this.onHabitSavedListener = listener
    }

    override fun getTheme(): Int = R.style.Theme_Spark_BottomSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habitIdToEdit = arguments?.getLong(ARG_HABIT_ID, -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupColorSwatches()
        updateFrequencyUi(selectedFrequency)
        updateReminderTimeUi()
        selectCategory(selectedCategory)

        if (habitIdToEdit > 0L) {
            binding.tvSheetTitle.text = "Edit Habit"
            binding.btnSaveHabit.text = "Update Habit"
            loadExistingHabitData()
        }
    }

    private fun setupListeners() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        binding.chipHealth.setOnClickListener { selectCategory("Health") }
        binding.chipFitness.setOnClickListener { selectCategory("Fitness") }
        binding.chipMindfulness.setOnClickListener { selectCategory("Mindfulness") }
        binding.chipProductivity.setOnClickListener { selectCategory("Productivity") }
        binding.chipStudy.setOnClickListener { selectCategory("Study") }
        binding.chipCustomCategory.setOnClickListener { selectCategory(binding.chipCustomCategory.text.toString()) }

        binding.chipMoreCategory.setOnClickListener {
            showFullCategorySelectionDialog()
        }

        binding.btnFreqDaily.setOnClickListener {
            selectedFrequency = "DAILY"
            updateFrequencyUi("DAILY")
        }

        binding.btnFreqWeekly.setOnClickListener {
            selectedFrequency = "WEEKLY"
            updateFrequencyUi("WEEKLY")
        }

        binding.cardReminderTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnSaveHabit.setOnClickListener {
            validateAndSaveHabit()
        }
    }

    private fun selectCategory(category: String) {
        selectedCategory = category
        when (category.lowercase()) {
            "health" -> binding.chipHealth.isChecked = true
            "fitness" -> binding.chipFitness.isChecked = true
            "mindfulness" -> binding.chipMindfulness.isChecked = true
            "productivity" -> binding.chipProductivity.isChecked = true
            "study" -> binding.chipStudy.isChecked = true
            else -> {
                binding.chipCustomCategory.text = category
                binding.chipCustomCategory.visibility = View.VISIBLE
                binding.chipCustomCategory.isChecked = true
            }
        }
    }

    private fun showFullCategorySelectionDialog() {
        val categories = HabitCategories.ALL
        val items = categories.map { it.title }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Category")
            .setItems(items) { _, which ->
                val chosenCategory = items[which]
                selectCategory(chosenCategory)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun setupColorSwatches() {
        val context = requireContext()
        val isDark = PreferenceHelper(context).isDarkMode
        binding.layoutColorTags.removeAllViews()

        val density = resources.displayMetrics.density
        val frameSize = (44 * density).toInt()
        val circleSize = (32 * density).toInt()
        val marginEnd = (8 * density).toInt()

        HabitColors.ALL.forEach { colorOption ->
            val frame = FrameLayout(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(frameSize, frameSize).apply {
                    this.marginEnd = marginEnd
                }
                isClickable = true
                isFocusable = true
                tag = colorOption.key
            }

            val circle = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(circleSize, circleSize, Gravity.CENTER)
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(colorOption.getColorInt(isDark))
                }
                background = bg
            }
            frame.addView(circle)

            // Checkmark overlay
            val checkIv = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams((18 * density).toInt(), (18 * density).toInt(), Gravity.CENTER)
                setImageResource(R.drawable.ic_check)
                setColorFilter(ContextCompat.getColor(context, R.color.white))
                visibility = View.GONE
            }
            frame.addView(checkIv)

            frame.setOnClickListener {
                selectColorTag(colorOption.key)
            }

            binding.layoutColorTags.addView(frame)
        }

        updateColorTagUi(selectedColorTag)
    }

    private fun selectColorTag(colorKeyOrHex: String) {
        selectedColorTag = HabitColors.getKeyForValue(colorKeyOrHex)
        updateColorTagUi(selectedColorTag)
    }

    private fun updateColorTagUi(selectedTag: String) {
        val normalizedKey = HabitColors.getKeyForValue(selectedTag)
        for (i in 0 until binding.layoutColorTags.childCount) {
            val frame = binding.layoutColorTags.getChildAt(i) as? FrameLayout ?: continue
            val key = frame.tag as? String ?: ""
            val checkIv = frame.getChildAt(1) as? ImageView

            if (key.equals(normalizedKey, ignoreCase = true)) {
                frame.setBackgroundResource(R.drawable.bg_color_tag_selected_ring)
                checkIv?.visibility = View.VISIBLE
            } else {
                frame.background = null
                checkIv?.visibility = View.GONE
            }
        }
    }

    private fun updateFrequencyUi(frequency: String) {
        if (frequency == "DAILY") {
            binding.btnFreqDaily.setBackgroundResource(R.drawable.bg_segmented_active)
            binding.btnFreqDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnFreqDaily.elevation = 2f

            binding.btnFreqWeekly.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnFreqWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            binding.btnFreqWeekly.elevation = 0f
        } else {
            binding.btnFreqWeekly.setBackgroundResource(R.drawable.bg_segmented_active)
            binding.btnFreqWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            binding.btnFreqWeekly.elevation = 2f

            binding.btnFreqDaily.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            binding.btnFreqDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            binding.btnFreqDaily.elevation = 0f
        }
    }

    private fun showTimePicker() {
        val picker = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                reminderHour = hourOfDay
                reminderMinute = minute
                reminderEnabled = true
                updateReminderTimeUi()
            },
            reminderHour,
            reminderMinute,
            false
        )
        picker.show()
    }

    private fun updateReminderTimeUi() {
        binding.tvReminderTime.text = DateUtils.formatTime12Hour(reminderHour, reminderMinute)
    }

    private fun loadExistingHabitData() {
        val context = context ?: return
        val db = AppDatabase.getDatabase(context)
        val repository = HabitRepository(db.habitDao(), db.habitCompletionDao())

        lifecycleScope.launch {
            val habit = withContext(Dispatchers.IO) {
                repository.getHabitById(habitIdToEdit)
            }

            if (habit != null) {
                existingHabit = habit
                binding.etHabitName.setText(habit.name)

                selectCategory(habit.category)

                selectedFrequency = habit.frequency
                updateFrequencyUi(habit.frequency)

                reminderEnabled = habit.reminderEnabled
                reminderHour = habit.reminderHour
                reminderMinute = habit.reminderMinute
                updateReminderTimeUi()

                selectedColorTag = habit.colorTag
                updateColorTagUi(habit.colorTag)
            }
        }
    }

    private fun validateAndSaveHabit() {
        val name = binding.etHabitName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.tilHabitName.error = "Please enter a habit name"
            return
        }
        if (name.length > 50) {
            binding.tilHabitName.error = "Habit name cannot exceed 50 characters"
            return
        }
        binding.tilHabitName.error = null

        val context = context ?: return
        val sessionManager = SessionManager(context)
        val userId = sessionManager.getCurrentUserId()
        if (userId <= 0) {
            Toast.makeText(context, "User session not found", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        binding.btnSaveHabit.isEnabled = false

        val db = AppDatabase.getDatabase(context)
        val repository = HabitRepository(db.habitDao(), db.habitCompletionDao())

        lifecycleScope.launch {
            val habitToSave = if (existingHabit != null) {
                existingHabit!!.copy(
                    name = name,
                    category = selectedCategory,
                    frequency = selectedFrequency,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    colorTag = selectedColorTag
                )
            } else {
                HabitEntity(
                    userId = userId,
                    name = name,
                    category = selectedCategory,
                    frequency = selectedFrequency,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    colorTag = selectedColorTag
                )
            }

            val savedId = withContext(Dispatchers.IO) {
                if (existingHabit != null) {
                    repository.updateHabit(habitToSave)
                    habitToSave.id
                } else {
                    repository.insertHabit(habitToSave)
                }
            }

            val finalHabit = habitToSave.copy(id = savedId)

            if (reminderEnabled) {
                HabitReminderScheduler.scheduleReminder(context, finalHabit)
            } else {
                HabitReminderScheduler.cancelReminder(context, savedId)
            }

            val msg = if (existingHabit != null) "Habit updated!" else "Habit created!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onHabitSavedListener?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddHabitBottomSheet"
        const val ARG_HABIT_ID = "arg_habit_id"

        fun newInstance(habitId: Long = -1L): AddHabitBottomSheet {
            val sheet = AddHabitBottomSheet()
            if (habitId > 0L) {
                val args = Bundle().apply {
                    putLong(ARG_HABIT_ID, habitId)
                }
                sheet.arguments = args
            }
            return sheet
        }
    }
}
