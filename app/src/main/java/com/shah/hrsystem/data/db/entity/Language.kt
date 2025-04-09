package com.shah.hrsystem.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "Languages",
    indices = [Index(value = ["LanguageName"], unique = true)]
)
data class Language(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "LanguageID")
    val id: Int = 0,

    @ColumnInfo(name = "LanguageName")
    val name: String
) : Parcelable