package com.shah.hrsystem.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {

    private const val TAG = "ThemeUtils"

    // Получаем SharedPreferences
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Применяет выбранную тему к приложению.
     * @param themeMode Одна из констант: Constants.THEME_LIGHT, THEME_DARK, THEME_SYSTEM.
     */
    fun applyTheme(themeMode: Int) {
        val mode = when (themeMode) {
            Constants.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.THEME_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // По умолчанию - системная
        }
        try {
            AppCompatDelegate.setDefaultNightMode(mode)
            Log.d(TAG, "Applied theme mode: $mode (requested: $themeMode)")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme mode: $mode", e)
        }
    }

    /**
     * Сохраняет выбранный режим темы в SharedPreferences.
     * @param context Контекст приложения.
     * @param themeMode Одна из констант: Constants.THEME_LIGHT, THEME_DARK, THEME_SYSTEM.
     */
    fun saveThemeSetting(context: Context, themeMode: Int) {
        val editor = getPrefs(context).edit()
        editor.putInt(Constants.PREFS_KEY_THEME, themeMode)
        editor.apply()
        Log.d(TAG, "Saved theme setting: $themeMode")
    }

    /**
     * Загружает сохраненный режим темы из SharedPreferences.
     * @param context Контекст приложения.
     * @return Сохраненная константа темы или THEME_SYSTEM по умолчанию.
     */
    fun loadThemeSetting(context: Context): Int {
        return getPrefs(context).getInt(Constants.PREFS_KEY_THEME, Constants.THEME_SYSTEM) // По умолчанию системная
    }

    /**
     * Применяет сохраненную тему при старте приложения.
     * Обычно вызывается в onCreate() класса Application или MainActivity.
     * @param context Контекст приложения.
     */
    fun applySavedTheme(context: Context) {
        val savedTheme = loadThemeSetting(context)
        applyTheme(savedTheme)
        Log.d(TAG, "Applied saved theme on startup: $savedTheme")
    }
}