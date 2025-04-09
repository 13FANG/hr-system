package com.shah.hrsystem.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


object UserRole {
    const val HR = "HR"
    const val ADMIN = "Admin"
    val ALL = listOf(HR, ADMIN)
}

@Parcelize
@Entity(
    tableName = "Users",
    indices = [Index(value = ["Login"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "UserID")
    val id: Int = 0,

    @ColumnInfo(name = "Login")
    val login: String,

    @ColumnInfo(name = "PasswordHash")
    val passwordHash: String,

    @ColumnInfo(name = "Role")
    val role: String
) : Parcelable