package com.shah.hrsystem.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shah.hrsystem.data.db.entity.EmployeeLanguage
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeLanguageDao {

    // Используем IGNORE, т.к. уникальность пары (EmployeeID, LanguageID) важна
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(employeeLanguage: EmployeeLanguage): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(employeeLanguages: List<EmployeeLanguage>): List<Long>

    @Update
    suspend fun update(employeeLanguage: EmployeeLanguage): Int

    @Delete
    suspend fun delete(employeeLanguage: EmployeeLanguage): Int

    @Query("DELETE FROM EmployeeLanguages WHERE EmployeeID = :employeeId")
    suspend fun deleteLanguagesForEmployee(employeeId: Int)

    @Query("SELECT * FROM EmployeeLanguages WHERE EmployeeID = :employeeId")
    fun getLanguagesForEmployeeFlow(employeeId: Int): Flow<List<EmployeeLanguage>>

    // Если нужно получить язык не через Flow
    @Query("SELECT * FROM EmployeeLanguages WHERE EmployeeID = :employeeId")
    suspend fun getLanguagesForEmployeeList(employeeId: Int): List<EmployeeLanguage>
}