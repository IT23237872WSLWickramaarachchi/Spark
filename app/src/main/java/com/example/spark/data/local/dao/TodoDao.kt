package com.example.spark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spark.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for [TodoEntity].
 *
 * Provides full CRUD operations, reactive Flow queries, and completion toggle helpers.
 */
@Dao
interface TodoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Delete
    suspend fun deleteTodo(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long)

    @Query("SELECT * FROM todos WHERE userId = :userId ORDER BY isCompleted ASC, dueDate ASC, createdAt DESC")
    fun getTodosForUser(userId: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE userId = :userId AND isCompleted = 0 ORDER BY dueDate ASC, createdAt DESC")
    fun getPendingTodosForUser(userId: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC, createdAt DESC")
    fun getCompletedTodosForUser(userId: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getTodoById(id: Long): TodoEntity?

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    fun getTodoByIdFlow(id: Long): Flow<TodoEntity?>

    @Query("UPDATE todos SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun setTodoCompleted(id: Long, isCompleted: Boolean, completedAt: Instant?)
}
