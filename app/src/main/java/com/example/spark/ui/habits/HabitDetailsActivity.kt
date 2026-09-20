package com.example.spark.ui.habits

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.databinding.ActivityHabitDetailsBinding
import com.example.spark.notifications.HabitReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitDetailsBinding
    private lateinit var habitRepository: HabitRepository
    private var habitId: Long = -1L
    private var currentHabit: HabitEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHabitDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId <= 0L) {
            Toast.makeText(this, "Invalid habit selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)
        habitRepository = HabitRepository(db.habitDao(), db.habitCompletionDao())

        setupWindowInsets()
        setupListeners()
        loadHabitData()
    }

    private fun setupWindowInsets() {
        val baseBottomPadding = (16 * resources.displayMetrics.density).toInt()
        val baseTopPadding = (10 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottomActions) { view, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = baseBottomPadding + navInsets.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbarHabitDetails) { view, insets ->
            val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = baseTopPadding + statusInsets.top)
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnOverflow.setOnClickListener { view ->
            showOverflowMenu(view)
        }

        binding.cardNotes.setOnClickListener {
            showEditNoteDialog()
        }

        binding.btnEditHabit.setOnClickListener {
            openEditHabitSheet()
        }

        binding.btnDeleteHabit.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Edit Habit")
        popup.menu.add(0, 2, 1, "Delete Habit")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    openEditHabitSheet()
                    true
                }
                2 -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    fun loadHabitData() {
        lifecycleScope.launch {
            val habit = withContext(Dispatchers.IO) {
                habitRepository.getHabitById(habitId)
            }

            if (habit == null) {
                // If habit was deleted, close cleanly without crashing
                finish()
                return@launch
            }

            currentHabit = habit

            val currentStreak = withContext(Dispatchers.IO) {
                habitRepository.calculateCurrentStreak(habitId)
            }

            val bestStreak = withContext(Dispatchers.IO) {
                habitRepository.calculateBestStreak(habitId)
            }

            // Top Bar
            binding.tvDetailTitle.text = habit.name

            // Hero Card
            val isDark = com.example.spark.util.PreferenceHelper(this@HabitDetailsActivity).isDarkMode
            val accentColor = com.example.spark.model.HabitColors.getColorByKeyOrHex(habit.colorTag, isDark)
            binding.cardStreakHero.setCardBackgroundColor(accentColor)

            val isWeekly = habit.frequency.equals("WEEKLY", ignoreCase = true)
            val unit = if (isWeekly) "Week" else "Day"
            val units = if (isWeekly) "weeks" else "days"
            binding.tvStreakCount.text = "$currentStreak $unit Streak"
            binding.tvBestStreak.text = "Best streak: $bestStreak $units"

            // Notes
            val notesText = habit.notes?.trim()
            if (!notesText.isNullOrEmpty()) {
                binding.tvHabitNotes.text = notesText
            } else {
                binding.tvHabitNotes.text = getString(R.string.habit_detail_notes_hint)
            }

            // Load This Week row using java.time APIs
            renderThisWeekRow()
        }
    }

    private suspend fun renderThisWeekRow() {
        val completions = withContext(Dispatchers.IO) {
            habitRepository.getCompletionsForHabit(habitId)
        }

        val completedDates = completions.filter { it.completed }.map { it.completionDate }.toSet()

        val today = LocalDate.now()
        val monday = today.with(DayOfWeek.MONDAY)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val dayViews = listOf(
            Triple(binding.tvDayLetter1, binding.indicatorDay1, Pair(binding.ivCheckDay1, binding.dotDay1)),
            Triple(binding.tvDayLetter2, binding.indicatorDay2, Pair(binding.ivCheckDay2, binding.dotDay2)),
            Triple(binding.tvDayLetter3, binding.indicatorDay3, Pair(binding.ivCheckDay3, binding.dotDay3)),
            Triple(binding.tvDayLetter4, binding.indicatorDay4, Pair(binding.ivCheckDay4, binding.dotDay4)),
            Triple(binding.tvDayLetter5, binding.indicatorDay5, Pair(binding.ivCheckDay5, binding.dotDay5)),
            Triple(binding.tvDayLetter6, binding.indicatorDay6, Pair(binding.ivCheckDay6, binding.dotDay6)),
            Triple(binding.tvDayLetter7, binding.indicatorDay7, Pair(binding.ivCheckDay7, binding.dotDay7))
        )

        for (i in 0 until 7) {
            val date = monday.plusDays(i.toLong())
            val dateStr = date.format(dateFormatter)
            val isCompleted = completedDates.contains(dateStr)
            val isToday = date.isEqual(today)
            val isFuture = date.isAfter(today)

            val (_, indicator, checkAndDot) = dayViews[i]
            val (checkIcon, dotView) = checkAndDot

            when {
                isCompleted -> {
                    indicator.setBackgroundResource(R.drawable.bg_circle_success_green)
                    checkIcon.visibility = View.VISIBLE
                    dotView.visibility = View.GONE
                }
                isToday -> {
                    indicator.setBackgroundResource(R.drawable.bg_day_today_pending)
                    checkIcon.visibility = View.GONE
                    dotView.visibility = View.VISIBLE
                }
                isFuture -> {
                    indicator.setBackgroundResource(R.drawable.bg_day_future)
                    checkIcon.visibility = View.GONE
                    dotView.visibility = View.GONE
                }
                else -> { // Past missed
                    indicator.setBackgroundResource(R.drawable.bg_day_past_pending)
                    checkIcon.visibility = View.GONE
                    dotView.visibility = View.GONE
                }
            }
        }
    }

    private fun showEditNoteDialog() {
        val currentNote = currentHabit?.notes.orEmpty()
        val input = EditText(this).apply {
            setText(currentNote)
            hint = "Add reflections or progress notes here..."
            setSelection(text.length)
            setPadding(48, 36, 48, 36)
        }

        AlertDialog.Builder(this)
            .setTitle("Habit Notes")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newNote = input.text.toString().trim()
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        habitRepository.updateHabitNotes(habitId, if (newNote.isEmpty()) null else newNote)
                    }
                    loadHabitData()
                    Toast.makeText(this@HabitDetailsActivity, "Note updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEditHabitSheet() {
        val sheet = AddHabitBottomSheet.newInstance(habitId = habitId)
        sheet.setOnHabitSavedListener {
            loadHabitData()
        }
        sheet.show(supportFragmentManager, AddHabitBottomSheet.TAG)
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Habit?")
            .setMessage("Are you sure you want to delete this habit? All progress history will be removed.")
            .setPositiveButton("Delete") { _, _ ->
                deleteHabit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteHabit() {
        lifecycleScope.launch {
            // 1. Cancel scheduled reminder
            HabitReminderScheduler.cancelReminder(this@HabitDetailsActivity, habitId)

            // 2. Delete from Room database
            withContext(Dispatchers.IO) {
                habitRepository.deleteHabit(habitId)
            }

            Toast.makeText(this@HabitDetailsActivity, "Habit deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }
}
