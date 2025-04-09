package com.shah.hrsystem.data.repository

import com.shah.hrsystem.data.db.dao.UserDao
import com.shah.hrsystem.data.db.entity.User
import com.shah.hrsystem.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val userDao: UserDao) {

    /**
     * Проверяет учетные данные пользователя.
     * @param login Логин пользователя.
     * @param password Пароль пользователя.
     * @return Объект User в случае успеха, null если пользователь не найден или пароль неверный.
     */
    suspend fun login(login: String, password: String): User? {
        // Выполняем операцию в IO потоке
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByLogin(login)
            // Проверяем, найден ли пользователь и совпадает ли хэш пароля
            if (user != null && PasswordHasher.verifyPassword(password, user.passwordHash)) {
                user // Возвращаем пользователя, если все верно
            } else {
                null // Возвращаем null, если пользователь не найден или пароль неверный
            }
        }
    }
}