package com.shah.hrsystem.data.repository

import android.util.Log
import com.shah.hrsystem.data.db.dao.UserDao
import com.shah.hrsystem.data.db.entity.User
import com.shah.hrsystem.data.db.entity.UserRole
import com.shah.hrsystem.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(private val userDao: UserDao) {
    private val TAG = "UserRepository"

    // Получение всех пользователей для админки
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    /**
     * Добавляет нового пользователя системы (HR или Admin).
     * @param login Логин нового пользователя.
     * @param password Пароль нового пользователя.
     * @param role Роль нового пользователя (из UserRole).
     * @return Result с ID созданного пользователя или ошибкой.
     */
    suspend fun addUser(login: String, password: String, role: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Валидация входных данных
            if (login.isBlank()) return@withContext Result.failure(IllegalArgumentException("Login cannot be blank."))
            if (password.length < 6) return@withContext Result.failure(IllegalArgumentException("Password is too short (minimum 6 characters)."))
            if (role !in UserRole.ALL) return@withContext Result.failure(IllegalArgumentException("Invalid user role specified."))

            // Хэшируем пароль
            val passwordHash = PasswordHasher.hashPassword(password)

            // Создаем объект пользователя
            val newUser = User(login = login, passwordHash = passwordHash, role = role)

            // Вставляем в базу
            val id = userDao.insert(newUser)
            if (id > 0) {
                Log.i(TAG, "User '$login' added successfully with ID: $id")
                Result.success(id)
            } else {
                // Скорее всего, сработал unique constraint на логин
                Result.failure(Exception("Failed to insert user. Login might already exist."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user '$login'", e)
            Result.failure(e) // Перехватываем другие возможные ошибки БД
        }
    }

    /**
     * Обновляет данные пользователя (логин и/или роль). Пароль меняется через resetPassword.
     * @param user Пользователь с обновленными данными (кроме пароля).
     * @return Result с количеством обновленных строк (1) или ошибкой.
     */
    suspend fun updateUser(user: User): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Валидация
            if (user.login.isBlank()) return@withContext Result.failure(IllegalArgumentException("Login cannot be blank."))
            if (user.role !in UserRole.ALL) return@withContext Result.failure(IllegalArgumentException("Invalid user role specified."))

            // Обновляем пользователя (пароль не трогаем здесь)
            val updatedRows = userDao.update(user)
            if (updatedRows > 0) {
                Log.i(TAG, "User '${user.login}' (ID: ${user.id}) updated successfully.")
                Result.success(updatedRows)
            } else {
                Result.failure(Exception("User not found or data not changed."))
            }
        } catch (e: Exception) {
            // Ловим возможную ошибку уникальности логина при смене
            Log.e(TAG, "Error updating user ID: ${user.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Удаляет пользователя из системы.
     * @param user Пользователь для удаления.
     * @return Result с количеством удаленных строк (1) или ошибкой.
     */
    suspend fun deleteUser(user: User): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Дополнительно можно добавить проверку, чтобы нельзя было удалить последнего админа, но в ТЗ этого нет.
            if (user.role == UserRole.ADMIN && userDao.getAllUsers().firstOrNull()?.count { it.role == UserRole.ADMIN } == 1) {
                // return@withContext Result.failure(IllegalStateException("Cannot delete the last administrator."))
                // Пока просто удаляем
            }

            val deletedRows = userDao.delete(user)
            if (deletedRows > 0) {
                Log.i(TAG, "User '${user.login}' (ID: ${user.id}) deleted successfully.")
                Result.success(deletedRows)
            } else {
                Result.failure(Exception("User not found."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user ID: ${user.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Сбрасывает пароль пользователя.
     * @param userId ID пользователя.
     * @param newPassword Новый пароль.
     * @return Result<Unit> в случае успеха или ошибку.
     */
    suspend fun resetPassword(userId: Int, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Валидация нового пароля
            if (newPassword.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("New password is too short (minimum 6 characters)."))
            }

            // Находим пользователя
            val user = userDao.getUserById(userId).firstOrNull() // Используем Flow для получения актуальных данных
                ?: return@withContext Result.failure(NoSuchElementException("User not found."))


            // Хэшируем новый пароль
            val newPasswordHash = PasswordHasher.hashPassword(newPassword)

            // Обновляем пользователя с новым хэшем
            val updatedUser = user.copy(passwordHash = newPasswordHash)
            val updatedRows = userDao.update(updatedUser)

            if (updatedRows > 0) {
                Log.i(TAG, "Password reset successfully for user ID: $userId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update password in database."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting password for user ID: $userId", e)
            Result.failure(e)
        }
    }
}