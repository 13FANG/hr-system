package com.shah.hrsystem.data.repository

import android.util.Log
import com.shah.hrsystem.data.db.dao.EmployeeDao
import com.shah.hrsystem.data.db.dao.EmployeeLanguageDao
import com.shah.hrsystem.data.db.dao.PositionDao
import com.shah.hrsystem.data.db.entity.Employee
import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.data.db.entity.EmployeeLanguage
import com.shah.hrsystem.data.db.entity.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmployeeRepository @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val employeeLanguageDao: EmployeeLanguageDao,
    private val positionDao: PositionDao // Для проверки MaxAllowed
) {
    private val TAG = "EmployeeRepository"

    // Получение всех сотрудников/заявок
    fun getAllEmployees(): Flow<List<Employee>> = employeeDao.getAllEmployeesFlow()

    // Получение сотрудника/заявки по ID
    fun getEmployeeById(employeeId: Int): Flow<Employee?> = employeeDao.getEmployeeById(employeeId)

    // Получение языков для сотрудника
    fun getLanguagesForEmployee(employeeId: Int): Flow<List<EmployeeLanguage>> =
        employeeLanguageDao.getLanguagesForEmployeeFlow(employeeId)

    /**
     * Сохраняет заявку от кандидата.
     * @param employee Данные сотрудника (статус должен быть NEW).
     * @param languages Список языков кандидата.
     * @return Result с ID созданной записи или ошибкой.
     */
    suspend fun saveCandidateApplication(
        employee: Employee,
        languages: List<EmployeeLanguage>
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Убедимся, что статус NEW и поля для принятого сотрудника = null
            val application = employee.copy(
                status = EmployeeConstants.STATUS_NEW,
                employmentDate = null,
                tariffRate = null,
                personalNumber = null
            )
            // Вставляем сотрудника
            val employeeId = employeeDao.insert(application)
            if (employeeId <= 0) {
                return@withContext Result.failure(Exception("Failed to insert employee application."))
            }

            // Обновляем employeeId в языках и вставляем их
            val languagesWithId = languages.map { it.copy(employeeId = employeeId.toInt()) }
            employeeLanguageDao.insertAll(languagesWithId)

            Log.i(TAG, "Candidate application saved successfully with ID: $employeeId")
            Result.success(employeeId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving candidate application", e)
            Result.failure(e)
        }
    }

    /**
     * Обновляет данные существующего сотрудника или заявки.
     * @param employee Обновленные данные сотрудника.
     * @param languages Новый список языков (старый будет перезаписан).
     * @return Result с количеством обновленных строк (1 в случае успеха) или ошибкой.
     */
    suspend fun updateEmployeeData(
        employee: Employee,
        languages: List<EmployeeLanguage>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Обновляем основные данные сотрудника
            val updatedRows = employeeDao.update(employee)
            if (updatedRows <= 0) {
                return@withContext Result.failure(Exception("Employee not found or data not changed."))
            }

            // Перезаписываем языки: сначала удаляем старые, потом вставляем новые
            employeeLanguageDao.deleteLanguagesForEmployee(employee.id)
            val languagesWithId = languages.map { it.copy(employeeId = employee.id) }
            employeeLanguageDao.insertAll(languagesWithId)

            Log.i(TAG, "Employee data updated successfully for ID: ${employee.id}")
            Result.success(updatedRows)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating employee data for ID: ${employee.id}", e)
            Result.failure(e)
        }
    }

    /**
     * Принимает заявку кандидата.
     * @param employeeId ID заявки (сотрудника со статусом NEW).
     * @param tariffRate Присваиваемый тарифный разряд.
     * @return Result с объектом обновленного Employee или ошибкой (например, если вакансия занята).
     */
    suspend fun acceptApplication(employeeId: Int, tariffRate: Int): Result<Employee> = withContext(Dispatchers.IO) {
        try {
            val application = employeeDao.getEmployeeById(employeeId).firstOrNull()
                ?: return@withContext Result.failure(NoSuchElementException("Application with ID $employeeId not found."))

            // Проверка, что это действительно заявка
            if (application.status != EmployeeConstants.STATUS_NEW) {
                return@withContext Result.failure(IllegalStateException("Record with ID $employeeId is not a new application."))
            }

            // Проверка доступности должности
            val position = positionDao.getPositionById(application.positionId).firstOrNull()
                ?: return@withContext Result.failure(NoSuchElementException("Position with ID ${application.positionId} not found."))

            val currentCount = employeeDao.countAcceptedEmployeesForPosition(application.positionId, application.departmentId)
            if (currentCount >= position.maxAllowed) {
                return@withContext Result.failure(IllegalStateException("No vacancies available for position ${position.name} in department."))
            }

            // Обновляем данные для принятого сотрудника
            val acceptedEmployee = application.copy(
                status = EmployeeConstants.STATUS_ACCEPTED,
                tariffRate = tariffRate,
                employmentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE), // Текущая дата в формате YYYY-MM-DD
                personalNumber = application.id // Табельный номер = EmployeeID
            )

            val updatedRows = employeeDao.update(acceptedEmployee)
            if (updatedRows > 0) {
                Log.i(TAG, "Application accepted successfully for ID: $employeeId")
                Result.success(acceptedEmployee)
            } else {
                Result.failure(Exception("Failed to update application status for ID: $employeeId"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting application for ID: $employeeId", e)
            Result.failure(e)
        }
    }

    /**
     * Отклоняет (удаляет) заявку кандидата.
     * @param employeeId ID заявки (сотрудника со статусом NEW).
     * @return Result с количеством удаленных строк (1 в случае успеха) или ошибкой.
     */
    suspend fun rejectApplication(employeeId: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val application = employeeDao.getEmployeeById(employeeId).firstOrNull()
                ?: return@withContext Result.failure(NoSuchElementException("Application with ID $employeeId not found."))

            // Доп. проверка, что это заявка
            if (application.status != EmployeeConstants.STATUS_NEW) {
                return@withContext Result.failure(IllegalStateException("Record with ID $employeeId is not a new application."))
            }

            // Удаляем запись (связанные языки удалятся каскадно)
            val deletedRows = employeeDao.delete(application)
            if (deletedRows > 0) {
                Log.i(TAG, "Application rejected (deleted) successfully for ID: $employeeId")
                Result.success(deletedRows)
            } else {
                Result.failure(Exception("Failed to delete application with ID: $employeeId"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting application for ID: $employeeId", e)
            Result.failure(e)
        }
    }

    /**
     * Удаляет запись о принятом сотруднике.
     * @param employeeId ID сотрудника (статус ACCEPTED).
     * @return Result с количеством удаленных строк (1 в случае успеха) или ошибкой.
     */
    suspend fun deleteAcceptedEmployee(employeeId: Int): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val employee = employeeDao.getEmployeeById(employeeId).firstOrNull()
                ?: return@withContext Result.failure(NoSuchElementException("Employee with ID $employeeId not found."))

            // Доп. проверка, что это принятый сотрудник
            if (employee.status != EmployeeConstants.STATUS_ACCEPTED) {
                return@withContext Result.failure(IllegalStateException("Record with ID $employeeId is not an accepted employee."))
            }

            // Удаляем запись (связанные языки удалятся каскадно)
            val deletedRows = employeeDao.delete(employee)
            if (deletedRows > 0) {
                Log.i(TAG, "Accepted employee deleted successfully for ID: $employeeId")
                Result.success(deletedRows)
            } else {
                Result.failure(Exception("Failed to delete employee with ID: $employeeId"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting employee for ID: $employeeId", e)
            Result.failure(e)
        }
    }
}