package com.shah.hrsystem.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Константы для уровней владения
object ProficiencyLevel {
    const val ELEMENTARY = "Начальный"
    const val INTERMEDIATE = "Средний"
    const val ADVANCED = "Продвинутый"
    const val FLUENT = "Свободно"
    const val NATIVE = "Родной"
    val ALL = listOf(ELEMENTARY, INTERMEDIATE, ADVANCED, FLUENT, NATIVE)
}


@Entity(
    tableName = "EmployeeLanguages",
    foreignKeys = [
        ForeignKey(
            entity = Employee::class,
            parentColumns = ["EmployeeID"],
            childColumns = ["EmployeeID"],
            onDelete = ForeignKey.CASCADE, // При удалении сотрудника удаляются и его языки
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Language::class,
            parentColumns = ["LanguageID"],
            childColumns = ["LanguageID"],
            onDelete = ForeignKey.CASCADE, // При удалении языка из справочника, удаляются записи о владении им
            onUpdate = ForeignKey.CASCADE
        )
    ],
    // Уникальность пары (Сотрудник, Язык) и индексы для поиска
    indices = [
        Index(value = ["EmployeeID", "LanguageID"], unique = true),
        Index(value = ["LanguageID"]) // Индекс по LanguageID для отчетов
    ]
)
data class EmployeeLanguage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "EmployeeLanguageID")
    val id: Int = 0,

    @ColumnInfo(name = "EmployeeID") // Индекс создан выше
    val employeeId: Int,

    @ColumnInfo(name = "LanguageID") // Индекс создан выше
    val languageId: Int,

    // Используем константы
    @ColumnInfo(name = "ProficiencyLevel")
    val proficiencyLevel: String // CHECK(ProficiencyLevel IN (...)) валидация
)