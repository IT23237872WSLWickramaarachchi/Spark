package com.example.spark.data.repository

import com.example.spark.data.local.dao.TodoDao
import com.example.spark.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Repository mediating between the UI layer and [TodoDao].
 */
class TodoRepository(
    private val todoDao: TodoDao
) {

    fun getTodosForUser(userId: Long): Flow<List<TodoEntity>> =
        todoDao.getTodosForUser(userId)

    fun getPendingTodosForUser(userId: Long): Flow<List<TodoEntity>> =
        todoDao.getPendingTodosForUser(userId)

    fun getCompletedTodosForUser(userId: Long): Flow<List<TodoEntity>> =
        todoDao.getCompletedTodosForUser(userId)

    suspend fun getTodoById(id: Long): TodoEntity? =
        todoDao.getTodoById(id)

    fun getTodoByIdFlow(id: Long): Flow<TodoEntity?> =
        todoDao.getTodoByIdFlow(id)

    suspend fun insertTodo(todo: TodoEntity): Long =
        todoDao.insertTodo(todo)

    suspend fun updateTodo(todo: TodoEntity) =
        todoDao.updateTodo(todo)

    suspend fun deleteTodo(todo: TodoEntity) =
        todoDao.deleteTodo(todo)

    suspend fun deleteTodoById(id: Long) =
        todoDao.deleteTodoById(id)

    suspend fun toggleTodo(todo: TodoEntity) {
        val newStatus = !todo.isCompleted
        val completedAt = if (newStatus) Instant.now() else null
        todoDao.setTodoCompleted(todo.id, newStatus, completedAt)
    }
}
