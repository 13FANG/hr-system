package com.shah.hrsystem.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shah.hrsystem.data.db.entity.User
import com.shah.hrsystem.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Состояния UI для управления пользователями
sealed class AdminUserUiState {
    object Loading : AdminUserUiState()
    data class Success(val users: List<User>) : AdminUserUiState()
    data class Error(val message: String) : AdminUserUiState()
}

// Состояния для операций CRUD
sealed class UserOperationState {
    object Idle : UserOperationState()
    object Processing : UserOperationState()
    data class Success(val message: String) : UserOperationState()
    data class Error(val message: String) : UserOperationState()
}

@HiltViewModel
class AdminUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "AdminUserViewModel"

    // StateFlow для списка пользователей
    private val _uiState = MutableStateFlow<AdminUserUiState>(AdminUserUiState.Loading)
    val uiState: StateFlow<AdminUserUiState> = _uiState.asStateFlow()

    // StateFlow для статуса операций (добавление, удаление и т.д.)
    private val _operationState = MutableStateFlow<UserOperationState>(UserOperationState.Idle)
    val operationState: StateFlow<UserOperationState> = _operationState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .onStart { _uiState.value = AdminUserUiState.Loading }
                .catch { e ->
                    Log.e(TAG, "Error loading users", e)
                    _uiState.value = AdminUserUiState.Error("Ошибка загрузки пользователей: ${e.message}")
                }
                .collect { users ->
                    _uiState.value = AdminUserUiState.Success(users)
                }
        }
    }

    fun addUser(login: String, password: String, role: String) {
        _operationState.value = UserOperationState.Processing
        viewModelScope.launch {
            val result = userRepository.addUser(login, password, role)
            result.fold(
                onSuccess = {
                    _operationState.value = UserOperationState.Success("Пользователь '$login' успешно добавлен.")
                    // Список обновится автоматически через Flow в loadUsers
                },
                onFailure = { error ->
                    _operationState.value = UserOperationState.Error("Ошибка добавления: ${error.message}")
                }
            )
        }
    }

    // Обновляем только логин и роль, пароль через resetPassword
    fun updateUser(user: User, newLogin: String, newRole: String) {
        // Проверяем, изменилось ли что-то
        if (user.login == newLogin && user.role == newRole) {
            _operationState.value = UserOperationState.Idle // Нет изменений
            return
        }

        _operationState.value = UserOperationState.Processing
        viewModelScope.launch {
            val updatedUser = user.copy(login = newLogin, role = newRole)
            val result = userRepository.updateUser(updatedUser)
            result.fold(
                onSuccess = {
                    _operationState.value = UserOperationState.Success("Данные пользователя '${user.login}' обновлены.")
                },
                onFailure = { error ->
                    _operationState.value = UserOperationState.Error("Ошибка обновления: ${error.message}")
                }
            )
        }
    }

    fun deleteUser(user: User) {
        _operationState.value = UserOperationState.Processing
        viewModelScope.launch {
            val result = userRepository.deleteUser(user)
            result.fold(
                onSuccess = {
                    _operationState.value = UserOperationState.Success("Пользователь '${user.login}' удален.")
                },
                onFailure = { error ->
                    _operationState.value = UserOperationState.Error("Ошибка удаления: ${error.message}")
                }
            )
        }
    }

    fun resetPassword(userId: Int, newPassword: String) {
        _operationState.value = UserOperationState.Processing
        viewModelScope.launch {
            val result = userRepository.resetPassword(userId, newPassword)
            result.fold(
                onSuccess = {
                    _operationState.value = UserOperationState.Success("Пароль для пользователя ID $userId сброшен.")
                },
                onFailure = { error ->
                    _operationState.value = UserOperationState.Error("Ошибка сброса пароля: ${error.message}")
                }
            )
        }
    }

    // Сброс состояния операции после показа сообщения
    fun clearOperationState() {
        _operationState.value = UserOperationState.Idle
    }
}