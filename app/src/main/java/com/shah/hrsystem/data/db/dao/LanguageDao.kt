package com.shah.hrsystem.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shah.hrsystem.data.db.entity.Language
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Запретить дубликаты имен
    suspend fun insert(language: Language): Long

    @Update
    suspend fun update(language: Language): Int

    @Delete
    suspend fun delete(language: Language): Int

    @Query("SELECT * FROM Languages WHERE LanguageID = :id")
    fun getLanguageById(id: Int): Flow<Language?>

    @Query("SELECT * FROM Languages ORDER BY LanguageName ASC")
    fun getAllLanguages(): Flow<List<Language>>
}