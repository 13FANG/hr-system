package com.shah.hrsystem.util

object Constants {
    // Ключи для передачи аргументов между фрагментами (если не используется Safe Args)
    const val ARG_EMPLOYEE_ID = "employeeId"
    const val ARG_DICTIONARY_TYPE = "dictionaryType"

    // Имена типов справочников (для DictionaryListFragment)
    const val DICT_TYPE_DEPARTMENTS = "DEPARTMENTS"
    const val DICT_TYPE_POSITIONS = "POSITIONS"
    const val DICT_TYPE_LANGUAGES = "LANGUAGES"

    // Формат даты, используемый в приложении (ISO 8601)
    const val DATE_FORMAT_ISO = "yyyy-MM-dd"
    const val DATE_FORMAT_DISPLAY = "dd.MM.yyyy" // Формат для отображения пользователю

    // Пенсионный возраст (согласно ТЗ)
    const val RETIREMENT_AGE_MALE = 65
    const val RETIREMENT_AGE_FEMALE = 60

    // Минимальный стаж для не-ассистентов (согласно ТЗ)
    const val MIN_ACADEMIC_EXP_FOR_NON_ASSISTANT = 3

    // Тарифный разряд (согласно ТЗ)
    const val MIN_TARIFF_RATE = 1
    const val MAX_TARIFF_RATE = 18

    // Ключи для SharedPreferences/DataStore (для настроек)
    const val PREFS_NAME = "hr_system_prefs"
    const val PREFS_KEY_THEME = "app_theme"

    // Значения для выбора темы
    const val THEME_UNDEFINED = -1 // Не используется напрямую, системная тема
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2
    const val THEME_SYSTEM = 0 // Соответствует AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}