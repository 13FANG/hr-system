package com.shah.hrsystem.viewmodel

import android.content.Context // Добавлен импорт Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import com.shah.hrsystem.data.repository.SettingsRepository // Убран, если не нужен для другого
import com.shah.hrsystem.util.Constants
import com.shah.hrsystem.util.SessionManager
import com.shah.hrsystem.util.ThemeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // private val settingsRepository: SettingsRepository, // Убрано
    // private val sessionManager: SessionManager, // Оставляем, если нужен UserID/Role для чего-то еще в настройках
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val TAG = "SettingsViewModel"

    // --- StateFlow для текущей темы ---
    private val _currentTheme = MutableStateFlow(ThemeUtils.loadThemeSetting(appContext))
    val currentTheme: StateFlow<Int> = _currentTheme.asStateFlow()

    /**
     * Применяет и сохраняет выбранную тему.
     * @param themeMode Константа темы из Constants (THEME_LIGHT, THEME_DARK, THEME_SYSTEM).
     */
    fun applyAndSaveTheme(themeMode: Int) {
        try {
            ThemeUtils.applyTheme(themeMode)
            ThemeUtils.saveThemeSetting(appContext, themeMode)
            _currentTheme.value = themeMode // Обновляем состояние
            Log.i(TAG, "Theme changed and saved: $themeMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying/saving theme", e)
            // Можно показать ошибку пользователю, если нужно
        }
    }
}