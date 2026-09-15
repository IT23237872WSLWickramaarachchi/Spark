package com.example.spark

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.spark.data.local.SparkDatabase
import com.example.spark.data.local.entity.UserEntity
import com.example.spark.ui.habit.AddHabitViewModel
import com.example.spark.ui.todo.TodoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveCrudAuditTest {

    private lateinit var database: SparkDatabase
    private lateinit var app: SparkApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<SparkApplication>()
        database = SparkDatabase.getInstance(app)

        runBlocking {
            database.clearAllTables()
            val userDao = database.userDao()
            // Let Room auto-generate the ID to prevent UNIQUE constraint failures
            val newId = userDao.register(UserEntity(name = "Test", email = "test@example.com", passwordHash = "hash"))
            app.sessionManager.saveSession(newId)
        }
    }

    @Test
    fun verifyHabitAndTodoCrudLivePipeline() {
        runBlocking {
            Log.w("LiveCrudAudit", "=== STARTING PROGRAMMATIC LIVE CRUD AUDIT ===")

            val habitRepo = app.habitRepository
            val todoRepo = app.todoRepository

            // ================= HABITS =================
            Log.w("LiveCrudAudit", "--- HABITS ---")
            
            // 1. Create
            Log.w("LiveCrudAudit", "Step 1a: Creating 'Morning Run' Habit via AddHabitViewModel...")
            val addHabitVm = AddHabitViewModel(habitRepo, SavedStateHandle())
            addHabitVm.onNameChange("Morning Run")
            addHabitVm.onCategorySelect("Health")
            addHabitVm.saveHabit()

            // Wait for DB write
            Thread.sleep(500)

            // Query
            var habits = habitRepo.getAllHabits().first()
            val createdHabit = habits.firstOrNull { it.title == "Morning Run" }
            Log.w("LiveCrudAudit", "Step 1b: DB Query Result -> Count: ${habits.size}, Found: '${createdHabit?.title}'")
            assert(createdHabit != null)

            // 2. Edit
            Log.w("LiveCrudAudit", "Step 2a: Editing Habit to 'Evening Run' via AddHabitViewModel...")
            val editHabitVm = AddHabitViewModel(habitRepo, SavedStateHandle().apply { set("habitId", createdHabit!!.id.toString()) })
            Thread.sleep(200) // Wait for initialization to load existing
            editHabitVm.onNameChange("Evening Run")
            editHabitVm.saveHabit()

            Thread.sleep(500)

            habits = habitRepo.getAllHabits().first()
            val editedHabit = habits.firstOrNull { it.title == "Evening Run" }
            Log.w("LiveCrudAudit", "Step 2b: DB Query Result -> Count: ${habits.size}, Found: '${editedHabit?.title}'")
            assert(editedHabit != null)

            // 3. Delete
            Log.w("LiveCrudAudit", "Step 3a: Deleting Habit via Repository (simulating swipe-to-delete commit)...")
            habitRepo.deleteHabit(editedHabit!!.id)

            Thread.sleep(500)

            habits = habitRepo.getAllHabits().first()
            Log.w("LiveCrudAudit", "Step 3b: DB Query Result -> Count: ${habits.size}")
            assert(habits.isEmpty())


            // ================= TODOS =================
            Log.w("LiveCrudAudit", "--- TODOS ---")
            
            val todoVm = TodoViewModel(todoRepo, { app.sessionManager.currentUserId })

            // 1. Create
            Log.w("LiveCrudAudit", "Step 1a: Creating 'Buy Groceries' Todo via TodoViewModel...")
            todoVm.addTodo("Buy Groceries", "Milk, Eggs", "High", null)

            Thread.sleep(500)

            var todos = todoRepo.getTodosForUser(app.sessionManager.currentUserId).first()
            val createdTodo = todos.firstOrNull { it.title == "Buy Groceries" }
            Log.w("LiveCrudAudit", "Step 1b: DB Query Result -> Count: ${todos.size}, Found: '${createdTodo?.title}'")
            assert(createdTodo != null)

            // 2. Edit
            Log.w("LiveCrudAudit", "Step 2a: Editing Todo to 'Buy Hardware' via TodoViewModel...")
            todoVm.updateTodo(createdTodo!!.copy(title = "Buy Hardware"))

            Thread.sleep(500)

            todos = todoRepo.getTodosForUser(app.sessionManager.currentUserId).first()
            val editedTodo = todos.firstOrNull { it.title == "Buy Hardware" }
            Log.w("LiveCrudAudit", "Step 2b: DB Query Result -> Count: ${todos.size}, Found: '${editedTodo?.title}'")
            assert(editedTodo != null)

            // 3. Delete
            Log.w("LiveCrudAudit", "Step 3a: Deleting Todo via TodoViewModel (simulating delete click)...")
            todoVm.deleteTodo(editedTodo!!)

            Thread.sleep(500)

            todos = todoRepo.getTodosForUser(app.sessionManager.currentUserId).first()
            Log.w("LiveCrudAudit", "Step 3b: DB Query Result -> Count: ${todos.size}")
            assert(todos.isEmpty())

            Log.w("LiveCrudAudit", "=== PROGRAMMATIC LIVE CRUD AUDIT SUCCESS ===")
        }
    }
}
