package com.shah.hrsystem.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shah.hrsystem.data.db.entity.User
import com.shah.hrsystem.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Sealed class для представления состояний UI экрана входа
sealed class LoginUiState {
    object Idle : LoginUiState() // Начальное состояние или после ошибки
    object Loading : LoginUiState() // Идет процесс аутентификации
    data class Success(val user: User) : LoginUiState() // Успешная аутентификация
    data class Error(val message: String) : LoginUiState() // Ошибка аутентификации
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // StateFlow для хранения логина и пароля из полей ввода
    val loginInput = MutableStateFlow("")
    val passwordInput = MutableStateFlow("")

    // Приватный MutableStateFlow для управления состоянием UI
    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    // Публичный StateFlow для наблюдения из Fragment
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    // StateFlow для отображения ошибок валидации (пока не используется активно)
    private val _inputError = MutableStateFlow<String?>(null)
    val inputError: StateFlow<String?> = _inputError.asStateFlow()

    /**
     * Инициирует процесс входа пользователя.
     */
    fun loginUser() {
        val login = loginInput.value.trim()
        val password = passwordInput.value // Пароль не тримим

        // Простая валидация на клиенте
        if (login.isBlank() || password.isBlank()) {
            _loginUiState.value = LoginUiState.Error("Логин и пароль не могут быть пустыми")
            // Возвращаемся в Idle состояние после короткой задержки или сразу
            _loginUiState.value = LoginUiState.Idle // Можно вернуть в Idle сразу
            return
        }

        // Устанавливаем состояние загрузки
        _loginUiState.value = LoginUiState.Loading

        // Запускаем корутину для выполнения запроса
        viewModelScope.launch {
            try {
                // Вызываем метод репозитория
                val user = authRepository.login(login, password)
                if (user != null) {
                    // Успех - передаем данные пользователя
                    _loginUiState.value = LoginUiState.Success(user)
                    // Здесь можно сохранить сессию пользователя, если нужно
                } else {
                    // Ошибка - неверные данные
                    _loginUiState.value = LoginUiState.Error("Неверный логин или пароль")
                    // Можно вернуть в Idle после ошибки, чтобы пользователь мог повторить ввод
                    _loginUiState.value = LoginUiState.Idle
                }
            } catch (e: Exception) {
                // Обработка других ошибок (например, проблемы с БД)
                _loginUiState.value = LoginUiState.Error("Ошибка входа: ${e.localizedMessage ?: "Неизвестная ошибка"}")
                _loginUiState.value = LoginUiState.Idle
            }
        }
    }

    /**
     * Сбрасывает состояние ошибки ввода, если оно было.
     */
    fun clearInputError() {
        _inputError.value = null
    }

    // Опционально: сброс состояния UI в Idle, если нужно
    fun resetStateToIdle() {
        _loginUiState.value = LoginUiState.Idle
    }
}