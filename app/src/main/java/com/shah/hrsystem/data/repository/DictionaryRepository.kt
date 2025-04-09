package com.shah.hrsystem.data.repository

import com.shah.hrsystem.data.db.dao.DepartmentDao
import com.shah.hrsystem.data.db.dao.LanguageDao
import com.shah.hrsystem.data.db.dao.PositionDao
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.data.db.entity.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepository @Inject constructor(
    private val departmentDao: DepartmentDao,
    private val positionDao: PositionDao,
    private val languageDao: LanguageDao
) {

    // --- Departments ---

    fun getAllDepartments(): Flow<List<Department>> = departmentDao.getAllDepartments()

    suspend fun addDepartment(name: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Проверка на пустую строку
            if (name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Department name cannot be blank."))
            }
            val department = Department(name = name.trim())
            val id = departmentDao.insert(department)
            // Если ID > 0, вставка прошла успешно
            if (id > 0) Result.success(id) else Result.failure(Exception("Failed to insert department, possibly due to duplicate name."))
        } catch (e: Exception) {
            Result.failure(e) // Возвращаем ошибку (например, нарушение unique constraint)
        }
    }

    suspend fun updateDepartment(department: Department): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (department.name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Department name cannot be blank."))
            }
            val updatedRows = departmentDao.update(department)
            if (updatedRows > 0) Result.success(updatedRows) else Result.failure(Exception("Department not found or no changes made."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDepartment(department: Department): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // В ТЗ не указана явная проверка на наличие сотрудников/должностей перед удалением отдела,
            // но ForeignKey constraint (ON DELETE CASCADE для Position, ON DELETE RESTRICT для Employee)
            // частично управляет этим. CASCADE удалит должности, RESTRICT не даст удалить, если есть сотрудники.
            // Дополнительные проверки можно добавить здесь, если требуется более явный контроль.
            val deletedRows = departmentDao.delete(department)
            if (deletedRows > 0) Result.success(deletedRows) else Result.failure(Exception("Department not found."))
        } catch (e: Exception) {
            Result.failure(e) // Может быть ConstraintException из-за RESTRICT в Employee
        }
    }

    // --- Positions ---

    fun getAllPositions(): Flow<List<Position>> = positionDao.getAllPositions()

    fun getPositionsByDepartment(departmentId: Int): Flow<List<Position>> =
        positionDao.getPositionsByDepartment(departmentId)

    suspend fun addPosition(position: Position): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (position.name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Position name cannot be blank."))
            }
            if (position.maxAllowed < 0) {
                return@withContext Result.failure(IllegalArgumentException("Max allowed must be non-negative."))
            }
            val id = positionDao.insert(position)
            if (id > 0) Result.success(id) else Result.failure(Exception("Failed to insert position, possibly due to duplicate name in department."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePosition(position: Position): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (position.name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Position name cannot be blank."))
            }
            if (position.maxAllowed < 0) {
                return@withContext Result.failure(IllegalArgumentException("Max allowed must be non-negative."))
            }
            val updatedRows = positionDao.update(position)
            if (updatedRows > 0) Result.success(updatedRows) else Result.failure(Exception("Position not found or no changes made."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePosition(position: Position): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // ForeignKey ON DELETE RESTRICT в Employee не даст удалить должность, если на ней есть сотрудники.
            val deletedRows = positionDao.delete(position)
            if (deletedRows > 0) Result.success(deletedRows) else Result.failure(Exception("Position not found."))
        } catch (e: Exception) {
            Result.failure(e) // Ожидаем ConstraintException, если есть сотрудники
        }
    }

    // --- Languages ---

    fun getAllLanguages(): Flow<List<Language>> = languageDao.getAllLanguages()

    suspend fun addLanguage(name: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Language name cannot be blank."))
            }
            val language = Language(name = name.trim())
            val id = languageDao.insert(language)
            if (id > 0) Result.success(id) else Result.failure(Exception("Failed to insert language, possibly due to duplicate name."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLanguage(language: Language): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (language.name.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Language name cannot be blank."))
            }
            val updatedRows = languageDao.update(language)
            if (updatedRows > 0) Result.success(updatedRows) else Result.failure(Exception("Language not found or no changes made."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLanguage(language: Language): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // ForeignKey ON DELETE CASCADE в EmployeeLanguage удалит связи с сотрудниками.
            val deletedRows = languageDao.delete(language)
            if (deletedRows > 0) Result.success(deletedRows) else Result.failure(Exception("Language not found."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}