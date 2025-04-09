package com.shah.hrsystem.util

import com.shah.hrsystem.data.db.entity.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Hilt будет управлять единственным экземпляром
class SessionManager @Inject constructor() { // Пустой конструктор для Hilt

    // Приватный MutableStateFlow для хранения текущего пользователя
    private val _currentUser = MutableStateFlow<User?>(null)
    // Публичный StateFlow для наблюдения
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /**
     * Сохраняет данные вошедшего пользователя.
     * @param user Объект User или null при выходе.
     */
    fun loginUser(user: User?) {
        _currentUser.value = user
    }

    /**
     * Очищает данные пользователя при выходе.
     */
    fun logoutUser() {
        _currentUser.value = null
    }

    /**
     * Возвращает ID текущего пользователя.
     * @return UserID или null, если пользователь не авторизован.
     */
    fun getCurrentUserId(): Int? {
        return _currentUser.value?.id
    }

    /**
     * Возвращает роль текущего пользователя.
     * @return Role (String) или null, если пользователь не авторизован.
     */
    fun getCurrentUserRole(): String? {
        return _currentUser.value?.role
    }
}