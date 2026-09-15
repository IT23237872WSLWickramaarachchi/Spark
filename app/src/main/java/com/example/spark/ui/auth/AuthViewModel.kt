package com.example.spark.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spark.data.local.PasswordHasher
import com.example.spark.data.local.SessionManager
import com.example.spark.data.local.entity.UserEntity
import com.example.spark.data.local.entity.UserPrefsEntity
import com.example.spark.data.repository.UserRepository
import com.example.spark.di.ServiceLocator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for authentication (Login & Register) business logic,
 * input validation, password hashing, database queries, and session persistence.
 */
class AuthViewModel(
    private val userRepository: UserRepository = ServiceLocator.userRepository
        ?: throw IllegalStateException("UserRepository must be initialized"),
    private val sessionManager: SessionManager = ServiceLocator.sessionManager
        ?: throw IllegalStateException("SessionManager must be initialized")
) : ViewModel() {

    sealed class AuthUiEvent {
        object Success : AuthUiEvent()
        data class EmailError(val message: String) : AuthUiEvent()
        data class PasswordError(val message: String) : AuthUiEvent()
        data class ConfirmPasswordError(val message: String) : AuthUiEvent()
        data class NameError(val message: String) : AuthUiEvent()
        data class GeneralError(val message: String) : AuthUiEvent()
        object Loading : AuthUiEvent()
    }

    private val _uiEvent = MutableSharedFlow<AuthUiEvent>()
    val uiEvent: SharedFlow<AuthUiEvent> = _uiEvent.asSharedFlow()

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }

    fun login(emailInput: String, passwordInput: String) {
        val email = emailInput.trim()
        var hasError = false

        if (email.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.EmailError("Email Address is required")) }
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.EmailError("Enter a valid email address")) }
            hasError = true
        }

        if (passwordInput.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.PasswordError("Password is required")) }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiEvent.emit(AuthUiEvent.Loading)
            try {
                val hashedPassword = PasswordHasher.hash(passwordInput)
                val user = userRepository.login(email, hashedPassword)
                if (user != null) {
                    sessionManager.saveSession(user.id)
                    _uiEvent.emit(AuthUiEvent.Success)
                } else {
                    _uiEvent.emit(AuthUiEvent.PasswordError("Invalid email or password"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(AuthUiEvent.GeneralError(e.message ?: "Authentication failed"))
            }
        }
    }

    fun register(nameInput: String, emailInput: String, passwordInput: String, confirmPasswordInput: String) {
        val name = nameInput.trim()
        val email = emailInput.trim()
        var hasError = false

        if (name.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.NameError("Full Name is required")) }
            hasError = true
        }

        if (email.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.EmailError("Email Address is required")) }
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.EmailError("Enter a valid email address")) }
            hasError = true
        }

        if (passwordInput.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.PasswordError("Password is required")) }
            hasError = true
        } else if (passwordInput.length < MIN_PASSWORD_LENGTH) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.PasswordError("Minimum $MIN_PASSWORD_LENGTH characters required")) }
            hasError = true
        }

        if (confirmPasswordInput.isEmpty()) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.ConfirmPasswordError("Please confirm your password")) }
            hasError = true
        } else if (confirmPasswordInput != passwordInput) {
            viewModelScope.launch { _uiEvent.emit(AuthUiEvent.ConfirmPasswordError("Passwords do not match")) }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiEvent.emit(AuthUiEvent.Loading)
            try {
                val existingUser = userRepository.getUserByEmail(email)
                if (existingUser != null) {
                    _uiEvent.emit(AuthUiEvent.EmailError("An account with this email already exists"))
                    return@launch
                }

                val hashedPassword = PasswordHasher.hash(passwordInput)
                val newUser = UserEntity(
                    name = name,
                    email = email,
                    passwordHash = hashedPassword
                )
                val newId = userRepository.register(newUser)
                userRepository.updatePrefs(UserPrefsEntity(userId = newId))
                sessionManager.saveSession(newId)

                _uiEvent.emit(AuthUiEvent.Success)
            } catch (e: Exception) {
                _uiEvent.emit(AuthUiEvent.GeneralError(e.message ?: "Registration failed"))
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel() as T
        }
    }
}
