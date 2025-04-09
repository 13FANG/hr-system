package com.shah.hrsystem.util

import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.data.db.entity.Position
import org.junit.Assert.*
import org.junit.Test

class ValidatorsTest {

    @Test
    fun isValidTariffRate_validRate_returnsTrue() {
        assertTrue(Validators.isValidTariffRate(1) ?: false)
        assertTrue(Validators.isValidTariffRate(10) ?: false)
        assertTrue(Validators.isValidTariffRate(18) ?: false)
    }

    @Test
    fun isValidTariffRate_invalidRate_returnsFalse() {
        assertFalse(Validators.isValidTariffRate(0) ?: true)
        assertFalse(Validators.isValidTariffRate(19) ?: true)
        assertFalse(Validators.isValidTariffRate(-5) ?: true)
    }

    @Test
    fun isValidTariffRate_nullRate_returnsNull() {
        assertNull(Validators.isValidTariffRate(null))
    }

    @Test
    fun isValidAge_validMaleBelowRetirement_returnsTrue() {
        // Дата рождения на 2025-04-10 дает возраст 59
        val dob = "1966-01-01"
        val gender = EmployeeConstants.GENDER_MALE // Пенс. 65
        val result = Validators.isValidAge(dob, gender)
        assertTrue(result ?: false)
    }

    @Test
    fun isValidAge_validFemaleAtRetirement_returnsFalse() {
        val dob = "1965-04-10"
        val gender = EmployeeConstants.GENDER_FEMALE // Пенс. 60
        val result = Validators.isValidAge(dob, gender)
        assertFalse(result ?: true)
    }

    @Test
    fun isValidAge_invalidGender_returnsNull() {
        val dob = "1990-01-01"
        val gender = "Другой"
        val result = Validators.isValidAge(dob, gender)
        assertNull(result)
    }
}