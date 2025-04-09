package com.shah.hrsystem.util

import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.data.db.entity.Position
import com.shah.hrsystem.data.db.entity.ProficiencyLevel
import java.time.LocalDate

object Validators {

    /**
     * Проверяет возраст кандидата на соответствие пенсионному возрасту.
     * @param dateOfBirth Дата рождения в формате ISO "yyyy-MM-dd".
     * @param gender Пол ("Мужской" или "Женский").
     * @return true, если возраст допустим, false - если достиг или превысил пенсионный. Null при ошибке данных.
     */
    fun isValidAge(dateOfBirth: String?, gender: String?): Boolean? {
        if (gender == null || (gender != EmployeeConstants.GENDER_MALE && gender != EmployeeConstants.GENDER_FEMALE)) {
            return null // Некорректный пол
        }
        val age = DateUtils.calculateAge(dateOfBirth) ?: return null // Некорректная дата или ошибка расчета

        val retirementAge = if (gender == EmployeeConstants.GENDER_MALE) {
            Constants.RETIREMENT_AGE_MALE
        } else {
            Constants.RETIREMENT_AGE_FEMALE
        }

        return age < retirementAge
    }

    /**
     * Проверяет соответствие уровня образования требованиям должности.
     * @param educationLevel Уровень образования кандидата ("Высшее" или "Не высшее").
     * @param position Требуемая должность.
     * @return true, если образование соответствует, иначе false. Null, если данные некорректны.
     */
    fun isEducationValidForPosition(educationLevel: String?, position: Position?): Boolean? {
        if (position == null || educationLevel == null ||
            (educationLevel != EmployeeConstants.EDU_HIGHER && educationLevel != EmployeeConstants.EDU_SECONDARY)) {
            return null // Некорректные входные данные
        }

        // Если должность требует высшего образования
        if (position.requiresHigherEducationBool()) {
            return educationLevel == EmployeeConstants.EDU_HIGHER // Кандидат должен иметь высшее
        }

        // Если должность НЕ требует высшего, подходит любой уровень
        return true
    }

    /**
     * Проверяет корректность введенного стажа.
     * @param totalExperience Общий стаж (годы).
     * @param academicExperience Академический стаж (годы).
     * @param position Должность, на которую претендует кандидат (нужна для проверки IsAssistant).
     * @return true, если стаж корректен, иначе false. Null при ошибке данных.
     */
    fun isValidExperience(
        totalExperience: Int?,
        academicExperience: Int?,
        position: Position?
    ): Boolean? {
        if (totalExperience == null || academicExperience == null || position == null ||
            totalExperience < 0 || academicExperience < 0) {
            return null // Некорректные или отрицательные значения
        }

        // 1. Академический стаж не может быть больше общего
        if (academicExperience > totalExperience) {
            return false
        }

        // 2. Для должностей НЕ ассистентов (IsAssistant = false) академ. стаж >= 3 года
        if (!position.isAssistantBool()) {
            if (academicExperience < Constants.MIN_ACADEMIC_EXP_FOR_NON_ASSISTANT) {
                return false
            }
        }

        // Все проверки пройдены
        return true
    }

    /**
     * Проверяет валидность тарифного разряда.
     * @param rate Тарифный разряд.
     * @return true, если разряд в диапазоне [1, 18], иначе false. Null, если rate is null.
     */
    fun isValidTariffRate(rate: Int?): Boolean? {
        if (rate == null) return null
        return rate in Constants.MIN_TARIFF_RATE..Constants.MAX_TARIFF_RATE
    }

    /**
     * Проверяет, является ли строка валидным уровнем владения языком.
     * @param level Строка для проверки.
     * @return true, если уровень валиден, иначе false.
     */
    fun isValidProficiencyLevel(level: String?): Boolean {
        return level != null && level in ProficiencyLevel.ALL
    }

    /**
     * Проверяет, является ли дата валидной и не находится ли она в будущем.
     * @param date Дата для проверки.
     * @return true, если дата валидна и не в будущем, иначе false. Null, если date is null.
     */
    fun isValidDateNotInFuture(date: LocalDate?): Boolean? {
        if (date == null) return null
        return !date.isAfter(LocalDate.now())
    }

}