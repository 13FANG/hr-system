package com.shah.hrsystem.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "Departments",
    indices = [Index(value = ["DepartmentName"], unique = true)]
)
data class Department(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "DepartmentID")
    val id: Int = 0,

    @ColumnInfo(name = "DepartmentName")
    val name: String
) : Parcelable