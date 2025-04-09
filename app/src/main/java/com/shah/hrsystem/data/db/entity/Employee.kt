package com.shah.hrsystem.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Константы для статусов и полов для избежания "магических строк"
object EmployeeConstants {
    const val STATUS_NEW = "NEW"
    const val STATUS_ACCEPTED = "ACCEPTED"
    const val GENDER_MALE = "Мужской"
    const val GENDER_FEMALE = "Женский"
    const val EDU_HIGHER = "Высшее"
    const val EDU_SECONDARY = "Не высшее"
}

@Entity(
    tableName = "Employees",
    foreignKeys = [
        ForeignKey(
            entity = Position::class,
            parentColumns = ["PositionID"],
            childColumns = ["PositionID"],
            onDelete = ForeignKey.RESTRICT, // Запретить удаление должности, если есть сотрудники
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Department::class, // Хотя DepartmentID есть в Position, дублируем для FK и запросов
            parentColumns = ["DepartmentID"],
            childColumns = ["DepartmentID"],
            onDelete = ForeignKey.RESTRICT, // Запретить удаление отдела, если есть сотрудники
            onUpdate = ForeignKey.CASCADE
        )
    ],
    // Индексы для полей, по которым будут частые запросы/фильтрация
    indices = [
        Index(value = ["PositionID"]),
        Index(value = ["DepartmentID"]),
        Index(value = ["PersonalNumber"], unique = true), // Уникальный табельный номер
        Index(value = ["Status"]), // Для быстрой фильтрации по статусу
        Index(value = ["LastName"]), // Для поиска по фамилии
    ]
    // CHECK constraint (AcademicExperience <= TotalExperience) будет проверяться в бизнес-логике
)
data class Employee(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "EmployeeID")
    val id: Int = 0,

    @ColumnInfo(name = "FirstName")
    val firstName: String,

    @ColumnInfo(name = "LastName")
    val lastName: String,

    // Даты храним в ISO 8601 формате "YYYY-MM-DD"
    @ColumnInfo(name = "DateOfBirth")
    val dateOfBirth: String,

    // Используем константы для пола
    @ColumnInfo(name = "Gender")
    val gender: String, // CHECK(Gender IN (...)) будет валидироваться

    @ColumnInfo(name = "PositionID") // Индекс есть
    val positionId: Int,

    @ColumnInfo(name = "DepartmentID") // Индекс есть
    val departmentId: Int,

    @ColumnInfo(name = "EmploymentDate") // ISO 8601 "YYYY-MM-DD", NULL для заявок
    val employmentDate: String? = null,

    @ColumnInfo(name = "TariffRate") // NULL для заявок
    val tariffRate: Int? = null,

    @ColumnInfo(name = "PersonalNumber") // Уникальный табельный номер, NULL для заявок
    val personalNumber: Int? = null,

    // Используем константы для уровня образования
    @ColumnInfo(name = "EducationLevel")
    val educationLevel: String, // CHECK(EducationLevel IN (...)) будет валидироваться

    @ColumnInfo(name = "TotalExperience", defaultValue = "0")
    val totalExperience: Int, // CHECK(TotalExperience >= 0) валидация

    @ColumnInfo(name = "AcademicExperience", defaultValue = "0")
    val academicExperience: Int, // CHECK(AcademicExperience >= 0) валидация

    // Используем константы для статуса
    @ColumnInfo(name = "Status", defaultValue = EmployeeConstants.STATUS_NEW)
    val status: String // CHECK(Status IN ('NEW', 'ACCEPTED')) валидация
)