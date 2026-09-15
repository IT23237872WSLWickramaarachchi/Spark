package com.example.spark.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spark.R
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.local.entity.UserEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.data.repository.UserRepository
import com.example.spark.di.ServiceLocator
import com.example.spark.domain.usecase.CalculateStreakUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * ViewModel for the Dashboard screen.
 *
 * Combines five Flows:
 * 1. [HabitRepository.getAllHabits]
 * 2. [HabitRepository.getTodayLogs]
 * 3. [MoodRepository.getTodayMood]
 * 4. [_searchQuery]
 * 5. [currentUserFlow]
 *
 * Emits [DashboardUiState] with completedToday, totalToday, and progressPercent computed reactively,
 * habits filtered by title/category match, and dynamic user name.
 */
class DashboardViewModel(
    private val habitRepository: HabitRepository = ServiceLocator.habitRepository!!,
    private val moodRepository: MoodRepository = ServiceLocator.moodRepository!!,
    private val userRepository: UserRepository? = ServiceLocator.userRepository,
    private val todoRepository: com.example.spark.data.repository.TodoRepository? = ServiceLocator.todoRepository,
    private val currentUserIdProvider: () -> Long = { ServiceLocator.sessionManager?.currentUserId ?: 1L },
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val streakUseCase = CalculateStreakUseCase()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val currentUserFlow: Flow<UserEntity?> =
        userRepository?.getCurrentUser(currentUserIdProvider().toString()) ?: flowOf(null)


    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Packs habit list + all logs + todos into a single emission for the outer combine.
     */
    private data class DashboardDataSnapshot(
        val habits: List<HabitEntity>,
        val allLogs: List<HabitLogEntity>,
        val todos: List<com.example.spark.data.local.entity.TodoEntity>
    )

    private val dataSnapshot: Flow<DashboardDataSnapshot> = combine(
        habitRepository.getAllHabits(),
        habitRepository.getAllLogs(),
        todoRepository?.getTodosForUser(currentUserIdProvider()) ?: flowOf(emptyList())
    ) { habits, logs, todos -> DashboardDataSnapshot(habits, logs, todos) }

    val uiState: StateFlow<DashboardUiState> = combine(
        dataSnapshot,
        moodRepository.getTodayMood(LocalDate.now()),
        _searchQuery,
        currentUserFlow
    ) { snapshot, todayMood, query, currentUser ->
        val today = LocalDate.now()
        val (habits, allLogs, todos) = snapshot

        val pendingTodos = todos.filter { !it.isCompleted }
        val topTasks = pendingTodos.take(3)
        val pendingTasksCount = pendingTodos.size

        val completedHabitIds = allLogs
            .filter { it.isCompleted && it.completedDate == today }
            .map { it.habitId.toString() }
            .toSet()

        val habitUiModels = habits.map { habit ->
            val isDone = completedHabitIds.contains(habit.id.toString())
            mapToUiModel(habit, isDone)
        }

        val totalToday = habitUiModels.size
        val completedToday = habitUiModels.count { it.isCompleted }
        val progressPercent = if (totalToday > 0) (completedToday * 100) / totalToday else 0

        val trimmedQuery = query.trim()
        val filteredHabitUiModels = if (trimmedQuery.isBlank()) {
            habitUiModels
        } else {
            habitUiModels.filter { habit ->
                habit.title.contains(trimmedQuery, ignoreCase = true) ||
                    habit.category.contains(trimmedQuery, ignoreCase = true)
            }
        }

        val hour = LocalTime.now().hour
        val greeting = when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }

        val moodLabel = todayMood?.let { entry ->
            when (entry.moodScore) {
                1 -> "Difficult today"
                2 -> "Feeling low today"
                3 -> "Feeling okay today"
                4 -> "Feeling good today"
                5 -> "Feeling radiant today"
                else -> "Mood logged today"
            }
        }

        // Real streak computation using CalculateStreakUseCase
        val streakDays = streakUseCase.execute(allLogs, today).currentStreak

        val displayName = currentUser?.name?.takeIf { it.isNotBlank() } ?: "Alex"

        DashboardUiState(
            greeting = greeting,
            userName = displayName,
            habits = filteredHabitUiModels,
            completedToday = completedToday,
            totalToday = totalToday,
            progressPercent = progressPercent,
            moodLabel = moodLabel,
            streakDays = streakDays,
            isEmpty = filteredHabitUiModels.isEmpty(),
            pendingTasksCount = pendingTasksCount,
            topTasks = topTasks
        )
    }.stateIn(
        scope = viewModelScope,
        started = sharingStarted,
        initialValue = DashboardUiState.initial()
    )

    fun toggleTask(task: com.example.spark.data.local.entity.TodoEntity) {
        viewModelScope.launch {
            todoRepository?.toggleTodo(task)
        }
    }

    val habitsWithLogs: Flow<List<HabitWithLogs>>
        get() = habitRepository.getAllHabitsWithLogs()

    /**
     * Called when a habit checkbox is toggled.
     * Upserts into Room via HabitRepository.logCompletion on IO dispatcher.
     */
    fun onHabitToggle(habitId: String, completed: Boolean) {
        viewModelScope.launch {
            habitRepository.logCompletion(habitId, LocalDate.now(), completed)
        }
    }

    // ── Supporting Actions for Dialogs and Detail Screens ───────────────

    fun toggleHabitCompletion(habitId: Long, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            habitRepository.logHabitCompletion(habitId, date)
        }
    }

    fun toggleHabitCompletion(habitId: String, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            habitRepository.logHabitCompletion(habitId, date)
        }
    }

    fun createHabit(
        title: String,
        category: String,
        frequency: String = "Daily",
        targetDaysPerWeek: Int = 7,
        reminderTime: LocalTime? = null,
        colorHex: String = "#52559c",
        userId: Long = 1L
    ) {
        viewModelScope.launch {
            val habit = HabitEntity(
                userId = userId,
                title = title,
                category = category,
                frequency = frequency,
                targetDaysPerWeek = targetDaysPerWeek,
                reminderTime = reminderTime,
                colorHex = colorHex
            )
            habitRepository.insertHabit(habit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.updateHabit(habit)
        }
    }

    fun getHabitWithLogs(habitId: Long): Flow<HabitWithLogs?> {
        return habitRepository.getHabitWithLogs(habitId)
    }

    fun calculateStreak(logs: List<HabitLogEntity>): CalculateStreakUseCase.StreakResult {
        return streakUseCase.execute(logs)
    }

    fun logMood(moodScore: Int, note: String? = null, userId: Long = 1L) {
        viewModelScope.launch {
            moodRepository.insertMoodEntry(
                MoodEntryEntity(
                    userId = userId,
                    date = LocalDate.now(),
                    moodScore = moodScore,
                    note = note?.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    private fun mapToUiModel(habit: HabitEntity, isDone: Boolean): HabitUiModel {
        // Primary: match by structured category; Secondary: match by title keywords
        val (iconRes, iconColor, bgColor) = when (habit.category.lowercase()) {
            "health" -> when {
                habit.title.contains("Water", ignoreCase = true) ||
                    habit.title.contains("Hydrat", ignoreCase = true) ->
                    Triple(R.drawable.ic_water_drop, parseColorHex("#52559C"), parseColorHex("#ECE9F1"))
                habit.title.contains("Walk", ignoreCase = true) ||
                    habit.title.contains("Run", ignoreCase = true) ||
                    habit.title.contains("Exercise", ignoreCase = true) ->
                    Triple(R.drawable.ic_walk, parseColorHex("#5D5FA6"), parseColorHex("#ECE9F1"))
                habit.title.contains("Sleep", ignoreCase = true) ->
                    Triple(R.drawable.ic_moon, parseColorHex("#1C192A"), parseColorHex("#F2A9BC33"))
                else ->
                    Triple(R.drawable.ic_water_drop, parseColorHex("#52559C"), parseColorHex("#ECE9F1"))
            }
            "mindfulness" -> when {
                habit.title.contains("Meditation", ignoreCase = true) ||
                    habit.title.contains("Meditat", ignoreCase = true) ->
                    Triple(R.drawable.ic_meditation, parseColorHex("#1C192A"), parseColorHex("#F2A9BC33"))
                else ->
                    Triple(R.drawable.ic_meditation, parseColorHex("#1C192A"), parseColorHex("#F2A9BC33"))
            }
            "productivity" -> when {
                habit.title.contains("Read", ignoreCase = true) ||
                    habit.title.contains("Page", ignoreCase = true) ||
                    habit.title.contains("Book", ignoreCase = true) ->
                    Triple(R.drawable.ic_book, parseColorHex("#6FCF97"), parseColorHex("#6FCF9733"))
                else ->
                    Triple(R.drawable.ic_book, parseColorHex("#6FCF97"), parseColorHex("#6FCF9733"))
            }
            else ->
                Triple(R.drawable.ic_streak_flame, parseColorHex("#52559C"), parseColorHex("#ECE9F1"))
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

    private fun parseColorHex(colorString: String): Int {
        val clean = if (colorString.startsWith("#")) colorString.substring(1) else colorString
        return when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> 0
        }
    }

    class Factory(
        private val habitRepository: HabitRepository,
        private val moodRepository: MoodRepository,
        private val userRepository: UserRepository? = null,
        private val currentUserIdProvider: () -> Long = { 1L }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                habitRepository = habitRepository,
                moodRepository = moodRepository,
                userRepository = userRepository,
                currentUserIdProvider = currentUserIdProvider
            ) as T
        }
    }
}
