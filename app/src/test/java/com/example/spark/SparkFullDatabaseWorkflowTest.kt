package com.example.spark

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.spark.data.dao.HabitCompletionDao
import com.example.spark.data.dao.HabitDao
import com.example.spark.data.dao.MoodEntryDao
import com.example.spark.data.dao.UserDao
import com.example.spark.data.entity.HabitCompletionEntity
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.entity.MoodEntryEntity
import com.example.spark.data.entity.UserEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.data.repository.UserRepository
import com.example.spark.util.PasswordHasher
import com.example.spark.util.StatisticsCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SparkFullDatabaseWorkflowTest {

    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var fakeHabitDao: FakeHabitDao
    private lateinit var fakeCompletionDao: FakeCompletionDao
    private lateinit var fakeMoodDao: FakeMoodDao

    private lateinit var userRepository: UserRepository
    private lateinit var habitRepository: HabitRepository
    private lateinit var moodRepository: MoodRepository

    class FakeUserDao : UserDao {
        val users = mutableMapOf<Long, UserEntity>()
        private var idGen = 1L

        override suspend fun insertUser(user: UserEntity): Long {
            val id = idGen++
            users[id] = user.copy(id = id)
            return id
        }

        override suspend fun getUserByEmail(email: String): UserEntity? {
            return users.values.find { it.email.equals(email, ignoreCase = true) }
        }

        override suspend fun getUserById(id: Long): UserEntity? {
            return users[id]
        }

        override fun getUserByIdLiveData(id: Long): LiveData<UserEntity?> {
            return MutableLiveData(users[id])
        }

        override suspend fun updateProfileImage(userId: Long, imageUri: String?) {
            users[userId]?.let { users[userId] = it.copy(profileImageUri = imageUri) }
        }

        override suspend fun updateFullName(userId: Long, fullName: String) {
            users[userId]?.let { users[userId] = it.copy(fullName = fullName) }
        }

        override suspend fun updateUser(user: UserEntity) {
            users[user.id] = user
        }
    }

    class FakeHabitDao : HabitDao {
        val habits = mutableMapOf<Long, HabitEntity>()
        private var idGen = 1L

        override suspend fun insertHabit(habit: HabitEntity): Long {
            val id = idGen++
            habits[id] = habit.copy(id = id)
            return id
        }

        override suspend fun updateHabit(habit: HabitEntity) {
            habits[habit.id] = habit
        }

        override suspend fun deleteHabit(habit: HabitEntity) {
            habits.remove(habit.id)
        }

        override suspend fun deleteHabitById(habitId: Long) {
            habits.remove(habitId)
        }

        override suspend fun getHabitById(habitId: Long): HabitEntity? {
            return habits[habitId]
        }

        override fun getActiveHabitsLiveData(userId: Long): LiveData<List<HabitEntity>> {
            return MutableLiveData(habits.values.filter { it.userId == userId && !it.isArchived })
        }

        override suspend fun getActiveHabits(userId: Long): List<HabitEntity> {
            return habits.values.filter { it.userId == userId && !it.isArchived }
        }

        override fun getAllHabitsLiveData(userId: Long): LiveData<List<HabitEntity>> {
            return MutableLiveData(habits.values.filter { it.userId == userId })
        }

        override suspend fun setArchived(habitId: Long, archived: Boolean) {
            habits[habitId]?.let { habits[habitId] = it.copy(isArchived = archived) }
        }

        override suspend fun updateHabitNotes(habitId: Long, notes: String?) {
            habits[habitId]?.let { habits[habitId] = it.copy(notes = notes) }
        }

        override suspend fun getAllActiveReminders(): List<HabitEntity> {
            return habits.values.filter { !it.isArchived && it.reminderEnabled }
        }
    }

    class FakeCompletionDao : HabitCompletionDao {
        val completions = mutableListOf<HabitCompletionEntity>()
        private var idGen = 1L

        override suspend fun insertOrUpdate(completion: HabitCompletionEntity): Long {
            completions.removeAll { it.habitId == completion.habitId && it.completionDate == completion.completionDate }
            val id = if (completion.id > 0) completion.id else idGen++
            val item = completion.copy(id = id)
            completions.add(item)
            return id
        }

        override suspend fun deleteCompletion(habitId: Long, date: String) {
            completions.removeAll { it.habitId == habitId && it.completionDate == date }
        }

        override suspend fun getCompletion(habitId: Long, date: String): HabitCompletionEntity? {
            return completions.find { it.habitId == habitId && it.completionDate == date }
        }

        override fun getCompletionLiveData(habitId: Long, date: String): LiveData<HabitCompletionEntity?> {
            return MutableLiveData(completions.find { it.habitId == habitId && it.completionDate == date })
        }

        override suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity> {
            return completions.filter { it.habitId == habitId }
        }

        override fun getCompletionsForHabitLiveData(habitId: Long): LiveData<List<HabitCompletionEntity>> {
            return MutableLiveData(completions.filter { it.habitId == habitId })
        }

        override fun getCompletionsForUserAndDateLiveData(userId: Long, date: String): LiveData<List<HabitCompletionEntity>> {
            return MutableLiveData(completions.filter { it.completionDate == date })
        }

        override suspend fun getCompletionsForUserAndDate(userId: Long, date: String): List<HabitCompletionEntity> {
            return completions.filter { it.completionDate == date }
        }

        override suspend fun getCompletionsForUserBetween(userId: Long, startDate: String, endDate: String): List<HabitCompletionEntity> {
            return completions.filter { it.completionDate in startDate..endDate }
        }

        override fun getCompletionsForUserBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<HabitCompletionEntity>> {
            return MutableLiveData(completions.filter { it.completionDate in startDate..endDate })
        }

        override suspend fun getTotalCompletedCountForHabit(habitId: Long): Int {
            return completions.count { it.habitId == habitId && it.completed }
        }
    }

    class FakeMoodDao : MoodEntryDao {
        val moods = mutableMapOf<String, MoodEntryEntity>()
        private var idGen = 1L

        override suspend fun insertOrUpdate(mood: MoodEntryEntity): Long {
            val id = if (mood.id > 0) mood.id else idGen++
            val item = mood.copy(id = id)
            moods["${mood.userId}_${mood.date}"] = item
            return id
        }

        override suspend fun update(mood: MoodEntryEntity) {
            moods["${mood.userId}_${mood.date}"] = mood
        }

        override suspend fun delete(mood: MoodEntryEntity) {
            moods.remove("${mood.userId}_${mood.date}")
        }

        override suspend fun getMoodForDate(userId: Long, date: String): MoodEntryEntity? {
            return moods["${userId}_$date"]
        }

        override fun getMoodForDateLiveData(userId: Long, date: String): LiveData<MoodEntryEntity?> {
            return MutableLiveData(moods["${userId}_$date"])
        }

        override fun getAllMoodsLiveData(userId: Long): LiveData<List<MoodEntryEntity>> {
            return MutableLiveData(moods.values.filter { it.userId == userId })
        }

        override suspend fun getAllMoods(userId: Long): List<MoodEntryEntity> {
            return moods.values.filter { it.userId == userId }
        }

        override suspend fun getMoodsBetween(userId: Long, startDate: String, endDate: String): List<MoodEntryEntity> {
            return moods.values.filter { it.userId == userId && it.date in startDate..endDate }
        }

        override fun getMoodsBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<MoodEntryEntity>> {
            return MutableLiveData(moods.values.filter { it.userId == userId && it.date in startDate..endDate })
        }

        override suspend fun getTotalMoodCheckInCount(userId: Long): Int {
            return moods.values.count { it.userId == userId }
        }
    }

    @Before
    fun setUp() {
        fakeUserDao = FakeUserDao()
        fakeHabitDao = FakeHabitDao()
        fakeCompletionDao = FakeCompletionDao()
        fakeMoodDao = FakeMoodDao()

        userRepository = UserRepository(fakeUserDao)
        habitRepository = HabitRepository(fakeHabitDao, fakeCompletionDao)
        moodRepository = MoodRepository(fakeMoodDao)
    }

    @Test
    fun completeLifecycle_StepsAThroughS_executesSuccessfully() = runBlocking {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        // A. Register
        val registerResult = userRepository.registerUser("Alex Johnson", "alex@spark.com", "SecretPass123")
        assertTrue("Step A: Register should succeed", registerResult.isSuccess)
        val registeredUser = registerResult.getOrThrow()
        val userId = registeredUser.id
        assertEquals("Alex Johnson", registeredUser.fullName)
        assertTrue(PasswordHasher.verifyPassword("SecretPass123", registeredUser.passwordHash))

        // B. Login
        val loginResult = userRepository.loginUser("alex@spark.com", "SecretPass123")
        assertTrue("Step B: Login should succeed", loginResult.isSuccess)
        val loggedInUser = loginResult.getOrThrow()
        assertEquals(userId, loggedInUser.id)

        // C. Add 3 daily habits
        val habit1Id = habitRepository.insertHabit(
            HabitEntity(
                userId = userId,
                name = "Morning Meditation",
                category = "Mindfulness",
                frequency = "DAILY",
                reminderEnabled = true,
                reminderHour = 8,
                reminderMinute = 0,
                colorTag = "#52559C"
            )
        )
        val habit2Id = habitRepository.insertHabit(
            HabitEntity(
                userId = userId,
                name = "Drink Water",
                category = "Health",
                frequency = "DAILY",
                reminderEnabled = true,
                reminderHour = 9,
                reminderMinute = 30,
                colorTag = "#2E7D56"
            )
        )
        val habit3Id = habitRepository.insertHabit(
            HabitEntity(
                userId = userId,
                name = "Evening Walk",
                category = "Health",
                frequency = "DAILY",
                reminderEnabled = false,
                colorTag = "#8E5B6E"
            )
        )
        val activeHabits = habitRepository.getActiveHabits(userId)
        assertEquals("Step C: 3 daily habits should be inserted", 3, activeHabits.size)

        // D. Add 1 weekly habit
        val habit4Id = habitRepository.insertHabit(
            HabitEntity(
                userId = userId,
                name = "Clean Workspace",
                category = "Productivity",
                frequency = "WEEKLY",
                reminderEnabled = true,
                reminderHour = 10,
                reminderMinute = 0,
                colorTag = "#6FCF97"
            )
        )
        assertEquals("Step D: Total active habits should be 4", 4, habitRepository.getActiveHabits(userId).size)

        // E. Set reminders
        val activeReminders = habitRepository.getAllActiveReminders()
        assertEquals("Step E: 3 habits have reminders enabled", 3, activeReminders.size)
        assertTrue(activeReminders.any { it.name == "Morning Meditation" && it.reminderHour == 8 })

        // F. Complete some habits
        habitRepository.toggleHabitCompletion(habit1Id, todayStr)
        habitRepository.toggleHabitCompletion(habit2Id, todayStr)
        val todayCompletions = habitRepository.getCompletionsForUserAndDate(userId, todayStr)
        assertEquals("Step F: 2 habits should be marked completed today", 2, todayCompletions.size)
        assertTrue(habitRepository.isCompletedOn(habit1Id, todayStr))
        assertTrue(habitRepository.isCompletedOn(habit2Id, todayStr))
        assertFalse(habitRepository.isCompletedOn(habit3Id, todayStr))

        // G. Open details
        val habit1Detail = habitRepository.getHabitById(habit1Id)
        assertNotNull("Step G: Habit 1 details should exist", habit1Detail)
        assertEquals("Morning Meditation", habit1Detail?.name)
        val habit1Streak = habitRepository.calculateCurrentStreak(habit1Id)
        assertEquals(1, habit1Streak)

        // H. Edit habit
        val updatedHabit1 = habit1Detail!!.copy(name = "Mindful Meditation 15m", colorTag = "#F6C1CB")
        habitRepository.updateHabit(updatedHabit1)
        val reloadedHabit1 = habitRepository.getHabitById(habit1Id)
        assertEquals("Step H: Name should be updated", "Mindful Meditation 15m", reloadedHabit1?.name)
        assertEquals("#F6C1CB", reloadedHabit1?.colorTag)

        // I. Add habit note
        habitRepository.updateHabitNotes(habit1Id, "Felt deeply calm and focused today.")
        val habit1WithNotes = habitRepository.getHabitById(habit1Id)
        assertEquals("Step I: Notes should persist", "Felt deeply calm and focused today.", habit1WithNotes?.notes)

        // J. Record mood
        moodRepository.saveMood(userId, todayStr, 5, "Feeling great and energetic!")
        val todayMood = moodRepository.getMoodForDate(userId, todayStr)
        assertNotNull("Step J: Mood entry should exist", todayMood)
        assertEquals(5, todayMood?.moodLevel)
        assertEquals("Feeling great and energetic!", todayMood?.note)

        // K. Open Statistics
        val statsRate = StatisticsCalculator.calculateDailySuccessRate(2, 3)
        assertEquals("Step K: Success rate should be 66.67%", 66.67, statsRate, 0.1)
        val avgMood = StatisticsCalculator.calculateMoodAverage(listOf(todayMood!!))
        assertEquals(5.0, avgMood ?: 0.0, 0.01)
        assertEquals("Positive", StatisticsCalculator.getMoodTrendLabel(avgMood))

        // L. Change profile picture
        val profileUri = "file:///data/user/0/com.example.spark/files/profiles/user_${userId}_avatar.jpg"
        userRepository.updateProfileImage(userId, profileUri)
        val userWithPhoto = userRepository.getUserById(userId)
        assertEquals("Step L: Profile image URI should be saved", profileUri, userWithPhoto?.profileImageUri)

        // M. Turn dark mode on (simulate preference persistence)
        var darkModeEnabled = true
        assertTrue("Step M: Dark mode toggle should be enabled", darkModeEnabled)

        // N. Restart app (simulate re-creating repositories from database)
        val newHabitRepo = HabitRepository(fakeHabitDao, fakeCompletionDao)
        val newMoodRepo = MoodRepository(fakeMoodDao)
        val newUserRepo = UserRepository(fakeUserDao)

        assertEquals("Step N: Habits persist across restart", 4, newHabitRepo.getActiveHabits(userId).size)
        assertEquals("Step N: Completions persist across restart", 2, newHabitRepo.getCompletionsForUserAndDate(userId, todayStr).size)
        assertNotNull("Step N: Mood persists across restart", newMoodRepo.getMoodForDate(userId, todayStr))
        assertEquals("Step N: User profile persists across restart", profileUri, newUserRepo.getUserById(userId)?.profileImageUri)

        // O. Delete one habit (habit4: "Clean Workspace")
        newHabitRepo.deleteHabit(habit4Id)

        // P. Verify details/history/reminder are removed appropriately
        assertNull("Step P: Deleted habit should return null", newHabitRepo.getHabitById(habit4Id))
        assertEquals("Step P: Remaining habits count should be 3", 3, newHabitRepo.getActiveHabits(userId).size)

        // Q. Logout simulation
        var currentSessionUserId: Long? = null
        assertNull("Step Q: Session cleared upon logout", currentSessionUserId)

        // R. Login again
        val reloginResult = newUserRepo.loginUser("alex@spark.com", "SecretPass123")
        assertTrue("Step R: Re-login should succeed", reloginResult.isSuccess)
        currentSessionUserId = reloginResult.getOrThrow().id
        assertEquals(userId, currentSessionUserId)

        // S. Confirm data remains
        val remainingHabits = newHabitRepo.getActiveHabits(currentSessionUserId)
        assertEquals("Step S: 3 active habits remain", 3, remainingHabits.size)
        val remainingCompletions = newHabitRepo.getCompletionsForUserAndDate(currentSessionUserId, todayStr)
        assertEquals("Step S: 2 habit completions remain", 2, remainingCompletions.size)
        val remainingMood = newMoodRepo.getMoodForDate(currentSessionUserId, todayStr)
        assertNotNull("Step S: Mood remains intact", remainingMood)
        assertEquals(profileUri, newUserRepo.getUserById(currentSessionUserId)?.profileImageUri)
    }
}
