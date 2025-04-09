package com.shah.hrsystem

import android.app.Application
import com.shah.hrsystem.util.Constants // <-- Добавьте импорт
import com.shah.hrsystem.util.ThemeUtils // <-- Добавьте импорт
import dagger.hilt.android.HiltAndroidApp

// Аннотация Hilt для включения внедрения зависимостей во всем приложении
@HiltAndroidApp
class App : Application() {
    // onCreate вызывается при старте приложения
    override fun onCreate() {
        super.onCreate()
        // --- ДОБАВЛЕНО: Принудительное применение светлой темы при запуске ---
        ThemeUtils.applyTheme(Constants.THEME_LIGHT)
        // Мы не вызываем ThemeUtils.saveThemeSetting здесь,
        // чтобы пользовательские настройки темы сохранялись, но игнорировались при запуске.
        // ---------------------------------------------------------------------
    }
}