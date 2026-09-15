package com.example.spark.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spark.data.local.entity.TodoEntity
import com.example.spark.data.repository.TodoRepository
import com.example.spark.di.ServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TodoFilter {
    ALL,
    PENDING,
    COMPLETED
}

data class TodoUiState(
    val todos: List<TodoEntity> = emptyList(),
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val completionPercent: Int = 0,
    val activeFilter: TodoFilter = TodoFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class TodoViewModel(
    private val todoRepository: TodoRepository = ServiceLocator.todoRepository
        ?: throw IllegalStateException("ServiceLocator.todoRepository must be initialized before TodoViewModel is created"),
    private val currentUserIdProvider: () -> Long = { ServiceLocator.sessionManager?.currentUserId ?: 1L },
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(TodoFilter.ALL)
    val activeFilter: StateFlow<TodoFilter> = _activeFilter.asStateFlow()

    private val allTodosFlow: Flow<List<TodoEntity>> =
        todoRepository.getTodosForUser(currentUserIdProvider())


    val uiState: StateFlow<TodoUiState> = combine(
        allTodosFlow,
        _activeFilter,
        _searchQuery
    ) { allTodos, filter, query ->
        val totalCount = allTodos.size
        val completedCount = allTodos.count { it.isCompleted }
        val completionPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 0

        // Filter by tab
        val filteredByTab = when (filter) {
            TodoFilter.ALL -> allTodos
            TodoFilter.PENDING -> allTodos.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> allTodos.filter { it.isCompleted }
        }

        // Filter by search query
        val finalTodos = if (query.isBlank()) {
            filteredByTab
        } else {
            val lower = query.trim().lowercase()
            filteredByTab.filter {
                it.title.lowercase().contains(lower) ||
                        (it.description?.lowercase()?.contains(lower) == true)
            }
        }

        TodoUiState(
            todos = finalTodos,
            totalCount = totalCount,
            completedCount = completedCount,
            completionPercent = completionPercent,
            activeFilter = filter,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = sharingStarted,
        initialValue = TodoUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: TodoFilter) {
        _activeFilter.value = filter
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepository.toggleTodo(todo)
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo)
        }
    }

    fun insertTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepository.insertTodo(todo)
        }
    }

    fun addTodo(title: String, description: String?, priority: String, dueDate: LocalDate?) {
        viewModelScope.launch {
            val newTodo = TodoEntity(
                userId = currentUserIdProvider(),
                title = title.trim(),
                description = description?.trim()?.ifBlank { null },
                priority = priority,
                dueDate = dueDate
            )
            todoRepository.insertTodo(newTodo)
        }
    }

    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            todoRepository.updateTodo(todo)
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TodoViewModel() as T
            }
        }
    }
}
