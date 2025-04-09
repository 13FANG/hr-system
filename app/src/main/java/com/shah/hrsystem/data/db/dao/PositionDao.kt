package com.shah.hrsystem.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shah.hrsystem.data.db.entity.Position
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Запретить дубликаты (Имя, Отдел)
    suspend fun insert(position: Position): Long

    @Update
    suspend fun update(position: Position): Int

    @Delete
    suspend fun delete(position: Position): Int

    @Query("SELECT * FROM Positions WHERE PositionID = :id")
    fun getPositionById(id: Int): Flow<Position?>

    @Query("SELECT * FROM Positions ORDER BY PositionName ASC")
    fun getAllPositions(): Flow<List<Position>>

    // Получить должности для конкретного отдела
    @Query("SELECT * FROM Positions WHERE DepartmentID = :departmentId ORDER BY PositionName ASC")
    fun getPositionsByDepartment(departmentId: Int): Flow<List<Position>>
}