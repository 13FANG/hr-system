package com.shah.hrsystem.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shah.hrsystem.data.db.entity.Department
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Запретить вставку с существующим именем (из-за UNIQUE индекса)
    suspend fun insert(department: Department): Long // Возвращает ID вставленной записи

    @Update
    suspend fun update(department: Department): Int // Возвращает кол-во обновленных строк

    @Delete
    suspend fun delete(department: Department): Int // Возвращает кол-во удаленных строк

    @Query("SELECT * FROM Departments WHERE DepartmentID = :id")
    fun getDepartmentById(id: Int): Flow<Department?> // Flow для автоматического обновления UI

    @Query("SELECT * FROM Departments ORDER BY DepartmentName ASC")
    fun getAllDepartments(): Flow<List<Department>> // Flow для списка
}