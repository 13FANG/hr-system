package com.shah.hrsystem.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shah.hrsystem.data.db.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Запретить дубликаты логина
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User): Int

    @Delete
    suspend fun delete(user: User): Int

    // Получение пользователя по ID
    @Query("SELECT * FROM Users WHERE UserID = :id")
    fun getUserById(id: Int): Flow<User?>

    // Получение пользователя по логину (для авторизации)
    @Query("SELECT * FROM Users WHERE Login = :login LIMIT 1")
    suspend fun getUserByLogin(login: String): User? // suspend, т.к. это разовая операция

    // Получение всех пользователей для админки
    @Query("SELECT * FROM Users ORDER BY Login ASC")
    fun getAllUsers(): Flow<List<User>>
}