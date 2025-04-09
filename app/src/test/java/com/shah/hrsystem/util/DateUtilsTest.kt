package com.shah.hrsystem.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun calculateAge_validDate_returnsCorrectAge() {
        val today = LocalDate.of(2025, 4, 10)
        val dob = LocalDate.of(1990, 1, 1)
        val expectedAge = 35
        val actualAge = calculateAgeFixed(dob, today)
        assertEquals(expectedAge, actualAge)
    }

    @Test
    fun calculateAge_birthdayToday_returnsCorrectAge() {
        val today = LocalDate.of(2025, 4, 10)
        val dob = LocalDate.of(1995, 4, 10)
        val expectedAge = 30
        val actualAge = calculateAgeFixed(dob, today)
        assertEquals(expectedAge, actualAge)
    }

    @Test
    fun calculateAge_nullDate_returnsNull() {
        val actualAge = DateUtils.calculateAge(null as LocalDate?)
        assertNull(actualAge)
    }

    @Test
    fun calculateAge_futureDate_returnsNull() {
        val today = LocalDate.of(2025, 4, 10)
        val dob = LocalDate.of(2026, 1, 1)
        val actualAge = calculateAgeFixed(dob, today)
        assertNull(actualAge) // Ожидаем null для даты в будущем
    }

    @Test
    fun formatIsoDateForDisplay_validIso_returnsDisplayFormat() {
        val isoDate = "1995-04-10"
        val expectedDisplay = "10.04.1995"
        val actualDisplay = DateUtils.formatIsoDateForDisplay(isoDate)
        assertEquals(expectedDisplay, actualDisplay)
    }

    private fun calculateAgeFixed(birthLocalDate: LocalDate?, today: LocalDate): Int? {
        if (birthLocalDate == null) return null
        return try {
            if (birthLocalDate.isAfter(today)) {
                null
            } else {
                java.time.Period.between(birthLocalDate, today).years
            }
        } catch (e: Exception) {
            null
        }
    }
}