package com.shah.hrsystem.util

import at.favre.lib.crypto.bcrypt.BCrypt

// Объект для централизованного хэширования и проверки паролей с использованием BCrypt
object PasswordHasher {

    // Стоимость хэширования (от 4 до 31). 12 - хороший баланс между скоростью и безопасностью.
    private const val BCRYPT_COST = 12

    /**
     * Хэширует переданный пароль.
     * @param password Пароль в виде строки.
     * @return Строка с BCrypt хэшем.
     */
    fun hashPassword(password: String): String {
        // Используем версию с CharSequence для безопасности (избегаем лишних String объектов)
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
    }

    /**
     * Проверяет, соответствует ли переданный пароль хэшу.
     * @param password Пароль для проверки в виде строки.
     * @param storedHash Хэш, сохраненный в базе данных.
     * @return true, если пароль совпадает с хэшем, иначе false.
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        // Используем версию с CharSequence для безопасности
        val result = BCrypt.verifyer().verify(password.toCharArray(), storedHash.toCharArray())
        return result.verified
    }
}