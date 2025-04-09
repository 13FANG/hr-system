package com.shah.hrsystem.data.repository

import android.util.Log
import com.shah.hrsystem.data.db.dao.UserDao // Оставляем импорт, если в будущем понадобится
import com.shah.hrsystem.util.PasswordHasher // Оставляем импорт, если в будущем понадобится
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(private val userDao: UserDao) { // Оставляем userDao, вдруг понадобится
    private val TAG = "SettingsRepository"
    // Здесь можно будет добавить другие методы для настроек в будущем,
    // например, сохранение/загрузка других параметров из SharedPreferences/DataStore
}