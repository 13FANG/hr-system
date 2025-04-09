package com.shah.hrsystem.util

import android.util.Log
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtils {

    val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE // "yyyy-MM-dd"
    private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_DISPLAY) // "dd.MM.yyyy"
    private val TAG = "DateUtils"

    /**
     * Форматирует дату (LocalDate) в строку для отображения пользователю (dd.MM.yyyy).
     * Возвращает пустую строку при ошибке или null значении.
     */
    fun formatDateForDisplay(date: LocalDate?): String {
        return try {
            date?.format(displayDateFormatter) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date for display: $date", e)
            ""
        }
    }

    /**
     * Форматирует дату из строки ISO (yyyy-MM-dd) в строку для отображения (dd.MM.yyyy).
     * Возвращает исходную строку при ошибке.
     */
    fun formatIsoDateForDisplay(isoDateString: String?): String {
        if (isoDateString.isNullOrBlank()) return ""
        return try {
            val localDate = LocalDate.parse(isoDateString, isoDateFormatter)
            localDate.format(displayDateFormatter)
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing ISO date string for display: $isoDateString", e)
            isoDateString // Возвращаем исходную, если не смогли распарсить
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting ISO date for display: $isoDateString", e)
            isoDateString
        }
    }

    /**
     * Форматирует LocalDate в строку ISO (yyyy-MM-dd).
     * Возвращает null при ошибке.
     */
    fun formatLocalDateToIso(date: LocalDate?): String? {
        return try {
            date?.format(isoDateFormatter)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting LocalDate to ISO: $date", e)
            null
        }
    }


    /**
     * Парсит строку в формате ISO (yyyy-MM-dd) в объект LocalDate.
     * Возвращает null, если строка некорректна или пуста.
     */
    fun parseIsoDate(isoDateString: String?): LocalDate? {
        if (isoDateString.isNullOrBlank()) return null
        return try {
            LocalDate.parse(isoDateString, isoDateFormatter)
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing ISO date string: $isoDateString", e)
            null
        }
    }

    /**
     * Парсит строку в формате Display (dd.MM.yyyy) в объект LocalDate.
     * Возвращает null, если строка некорректна или пуста.
     */
    fun parseDisplayDate(displayDateString: String?): LocalDate? {
        if (displayDateString.isNullOrBlank()) return null
        return try {
            LocalDate.parse(displayDateString, displayDateFormatter)
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Error parsing display date string: $displayDateString", e)
            null
        }
    }


    /**
     * Рассчитывает возраст на текущую дату по дате рождения.
     * @param dateOfBirth Дата рождения в виде строки ISO (yyyy-MM-dd).
     * @return Полных лет или null, если дата некорректна.
     */
    fun calculateAge(dateOfBirth: String?): Int? {
        val birthLocalDate = parseIsoDate(dateOfBirth) ?: return null
        return calculateAge(birthLocalDate)
    }

    /**
     * Рассчитывает возраст на текущую дату по дате рождения.
     * @param birthLocalDate Дата рождения в виде LocalDate.
     * @return Полных лет или null, если дата некорректна.
     */
    fun calculateAge(birthLocalDate: LocalDate?): Int? {
        if (birthLocalDate == null) return null
        return try {
            val today = LocalDate.now()
            // Убедимся, что дата рождения не в будущем
            if (birthLocalDate.isAfter(today)) {
                null
            } else {
                Period.between(birthLocalDate, today).years
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating age for: $birthLocalDate", e)
            null
        }
    }

    /**
     * Получает текущую дату в формате ISO (yyyy-MM-dd).
     */
    fun getCurrentIsoDate(): String {
        return LocalDate.now().format(isoDateFormatter)
    }
}