package com.shah.hrsystem.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shah.hrsystem.data.db.entity.Employee
import com.shah.hrsystem.data.db.entity.EmployeeLanguage
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    // Используем REPLACE для простоты обновления, но ABORT может быть безопаснее, если PersonalNumber уникален
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: Employee): Long

    @Update
    suspend fun update(employee: Employee): Int

    @Delete
    suspend fun delete(employee: Employee): Int

    @Query("SELECT * FROM Employees WHERE EmployeeID = :id")
    fun getEmployeeById(id: Int): Flow<Employee?>

    // Получение всех сотрудников/заявок (можно добавить фильтры)
    @Query("SELECT * FROM Employees ORDER BY LastName ASC, FirstName ASC")
    fun getAllEmployeesFlow(): Flow<List<Employee>> // Flow для реактивного обновления

    // Получение сотрудников/заявок по статусу
    @Query("SELECT * FROM Employees WHERE Status = :status ORDER BY LastName ASC, FirstName ASC")
    fun getEmployeesByStatus(status: String): Flow<List<Employee>>

    // Подсчет принятых сотрудников на конкретной должности в отделе (для проверки MaxAllowed)
    @Query("""
        SELECT COUNT(*) FROM Employees
        WHERE PositionID = :positionId AND DepartmentID = :departmentId AND Status = 'ACCEPTED'
    """)
    suspend fun countAcceptedEmployeesForPosition(positionId: Int, departmentId: Int): Int

    // Пример транзакции для вставки сотрудника и его языков
    // (Хотя языки добавляются позже по ТЗ, пример полезен)
    @Transaction
    suspend fun insertEmployeeWithLanguages(employee: Employee, languages: List<EmployeeLanguage>) {
        val employeeId = insert(employee)
        // Устанавливаем правильный ID сотрудника для языков, если он был 0 (автогенерация)
        val languagesWithEmployeeId = languages.map { lang ->
            // Если ID сотрудника в языке не совпадает с ID вставленного сотрудника
            // (или был 0 и теперь ID сгенерирован), обновляем его.
            // Это нужно, если EmployeeLanguage создается до вставки Employee.
            if (lang.employeeId != employeeId.toInt() && employeeId != -1L) {
                lang.copy(employeeId = employeeId.toInt())
            } else {
                lang
            }
        }
        insertEmployeeLanguagesInternal(languagesWithEmployeeId) // Вызов внутреннего метода для вставки языков
    }

    // Вспомогательный метод для вставки языков (не должен быть public, но Room требует)
    // Используем IGNORE, если язык уже существует для этого сотрудника
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmployeeLanguagesInternal(languages: List<EmployeeLanguage>)


    // Удаление языков сотрудника (вызывается перед обновлением списка языков или при удалении сотрудника)
    @Query("DELETE FROM EmployeeLanguages WHERE EmployeeID = :employeeId")
    suspend fun deleteLanguagesForEmployee(employeeId: Int)

    // Получение языков для сотрудника
    @Query("SELECT * FROM EmployeeLanguages WHERE EmployeeID = :employeeId")
    fun getLanguagesForEmployee(employeeId: Int): Flow<List<EmployeeLanguage>>

}