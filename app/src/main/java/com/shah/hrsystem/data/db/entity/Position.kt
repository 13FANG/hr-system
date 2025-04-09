package com.shah.hrsystem.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "Positions",
    foreignKeys = [
        ForeignKey(
            entity = Department::class,
            parentColumns = ["DepartmentID"],
            childColumns = ["DepartmentID"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["DepartmentID"]),
        Index(value = ["PositionName", "DepartmentID"], unique = true)
    ]
)
data class Position(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "PositionID")
    val id: Int = 0,

    @ColumnInfo(name = "PositionName")
    val name: String,

    @ColumnInfo(name = "DepartmentID")
    val departmentId: Int,

    @ColumnInfo(name = "MaxAllowed", defaultValue = "1")
    val maxAllowed: Int,

    @ColumnInfo(name = "RequiresHigherEducation", defaultValue = "0")
    val requiresHigherEducation: Int,

    @ColumnInfo(name = "IsAssistant", defaultValue = "0")
    val isAssistant: Int
) : Parcelable {

    fun requiresHigherEducationBool(): Boolean = requiresHigherEducation == 1
    fun isAssistantBool(): Boolean = isAssistant == 1
}