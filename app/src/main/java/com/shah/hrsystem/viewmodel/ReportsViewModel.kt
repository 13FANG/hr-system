package com.shah.hrsystem.viewmodel

import android.content.Context
import android.net.Uri // Добавлен импорт Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Employee
import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.data.repository.DictionaryRepository
import com.shah.hrsystem.data.repository.EmployeeRepository
import com.shah.hrsystem.util.PdfGenerator
import com.shah.hrsystem.util.VacancyInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// Состояния UI для генерации отчетов
sealed class ReportGenerationState {
    object Idle : ReportGenerationState()
    object LoadingData : ReportGenerationState()
    object GeneratingPdf : ReportGenerationState()
    data class Success(val fileUriOrPath: Any?) : ReportGenerationState()
    data class Error(val message: String) : ReportGenerationState()
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val dictionaryRepository: DictionaryRepository,
    @ApplicationContext private val appContext: Context // Нужен для PdfGenerator
) : ViewModel() {

    private val TAG = "ReportsViewModel"

    // StateFlow для статуса генерации отчета
    private val _reportState = MutableStateFlow<ReportGenerationState>(ReportGenerationState.Idle)
    val reportState: StateFlow<ReportGenerationState> = _reportState.asStateFlow()

    // Получаем ContentResolver
    private val contentResolver = appContext.contentResolver

    // --- Функции для генерации каждого отчета ---

    fun generateEmployeesByDepartmentReport() {
        _reportState.value = ReportGenerationState.LoadingData
        viewModelScope.launch {
            try {
                // Собираем данные
                val departments = dictionaryRepository.getAllDepartments().firstOrNull() ?: emptyList()
                val employees = employeeRepository.getAllEmployees().firstOrNull() ?: emptyList()
                Log.d(TAG, "EmployeesByDepartment: Fetched ${departments.size} departments and ${employees.size} employees.") // ЛОГ 1

                if (departments.isEmpty() || employees.isEmpty()) {
                    Log.w(TAG, "EmployeesByDepartment: Data is empty, report might be empty.")
                }

                val employeesByDeptId = employees.groupBy { it.departmentId }
                val reportData = departments.associateWith { dept ->
                    employeesByDeptId[dept.id] ?: emptyList()
                }
                Log.d(TAG, "EmployeesByDepartment: Prepared reportData with ${reportData.size} entries.") // ЛОГ 2
                // Дополнительный лог для проверки содержимого reportData
                reportData.forEach { (dept, emps) ->
                    Log.d(TAG, "  Dept: ${dept.name}, Employees: ${emps.size}")
                }

                _reportState.value = ReportGenerationState.GeneratingPdf
                // Вызываем генератор PDF в IO потоке, передавая ContentResolver
                val resultUriOrPath = withContext(Dispatchers.IO) {
                    Log.d(TAG, "EmployeesByDepartment: Calling PdfGenerator...") // ЛОГ 3
                    PdfGenerator.generateEmployeesByDepartmentReport(appContext, contentResolver, reportData)
                }

                if (resultUriOrPath != null) {
                    Log.i(TAG, "EmployeesByDepartment: Report generated successfully. Path/Uri: $resultUriOrPath") // ЛОГ 4
                    _reportState.value = ReportGenerationState.Success(resultUriOrPath)
                } else {
                    Log.e(TAG, "EmployeesByDepartment: PdfGenerator returned null.") // ЛОГ 5
                    _reportState.value = ReportGenerationState.Error("Не удалось создать PDF файл отчета.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating EmployeesByDepartment report", e)
                _reportState.value = ReportGenerationState.Error("Ошибка подготовки данных: ${e.message}")
            }
        }
    }

    fun generateEmployeesByLanguageReport() {
        _reportState.value = ReportGenerationState.LoadingData
        viewModelScope.launch {
            try {
                // Сбор данных (пока заглушка)
                val languages = dictionaryRepository.getAllLanguages().firstOrNull() ?: emptyList()
                val employees = employeeRepository.getAllEmployees().firstOrNull() ?: emptyList()
                Log.d(TAG, "EmployeesByLanguage: Fetched ${languages.size} languages and ${employees.size} employees.") // ЛОГ 1

                // TODO: Реализовать получение EmployeeLanguage для всех сотрудников
                // val allEmployeeLanguages = ...
                // val employeeMap = employees.associateBy { it.id }
                // val reportData = languages.associateWith { lang -> ... }

                val reportData = emptyMap<Language, List<Employee>>() // Заглушка
                Log.d(TAG, "EmployeesByLanguage: Prepared reportData (currently empty).") // ЛОГ 2 (пока пустые данные)

                _reportState.value = ReportGenerationState.GeneratingPdf
                val resultUriOrPath = withContext(Dispatchers.IO) {
                    Log.d(TAG, "EmployeesByLanguage: Calling PdfGenerator...") // ЛОГ 3
                    PdfGenerator.generateEmployeesByLanguageReport(appContext, contentResolver, reportData)
                }
                if (resultUriOrPath != null) {
                    Log.i(TAG, "EmployeesByLanguage: Report generated successfully. Path/Uri: $resultUriOrPath") // ЛОГ 4
                    _reportState.value = ReportGenerationState.Success(resultUriOrPath)
                } else {
                    Log.e(TAG, "EmployeesByLanguage: PdfGenerator returned null.") // ЛОГ 5
                    _reportState.value = ReportGenerationState.Error("Не удалось создать PDF файл отчета (данные не готовы).")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating EmployeesByLanguage report", e)
                _reportState.value = ReportGenerationState.Error("Ошибка подготовки данных: ${e.message}")
            }
        }
    }

    fun generateAgeDistributionReport() {
        _reportState.value = ReportGenerationState.LoadingData
        viewModelScope.launch {
            try {
                val employees = employeeRepository.getAllEmployees().firstOrNull() ?: emptyList()
                Log.d(TAG, "AgeDistribution: Fetched ${employees.size} employees.") // ЛОГ 1

                if (employees.isEmpty()) {
                    Log.w(TAG, "AgeDistribution: Data is empty, report might be empty.")
                }

                _reportState.value = ReportGenerationState.GeneratingPdf
                val resultUriOrPath = withContext(Dispatchers.IO) {
                    Log.d(TAG, "AgeDistribution: Calling PdfGenerator...") // ЛОГ 3
                    PdfGenerator.generateAgeDistributionReport(appContext, contentResolver, employees)
                }

                if (resultUriOrPath != null) {
                    Log.i(TAG, "AgeDistribution: Report generated successfully. Path/Uri: $resultUriOrPath") // ЛОГ 4
                    _reportState.value = ReportGenerationState.Success(resultUriOrPath)
                } else {
                    Log.e(TAG, "AgeDistribution: PdfGenerator returned null.") // ЛОГ 5
                    _reportState.value = ReportGenerationState.Error("Не удалось создать PDF файл отчета.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating AgeDistribution report", e)
                _reportState.value = ReportGenerationState.Error("Ошибка подготовки данных: ${e.message}")
            }
        }
    }

    fun generateAvailableVacanciesReport() {
        _reportState.value = ReportGenerationState.LoadingData
        viewModelScope.launch {
            try {
                // Сбор данных
                val departments = dictionaryRepository.getAllDepartments().firstOrNull() ?: emptyList()
                val positions = dictionaryRepository.getAllPositions().firstOrNull() ?: emptyList()
                val employees = employeeRepository.getAllEmployees().firstOrNull() ?: emptyList()
                Log.d(TAG, "AvailableVacancies: Fetched ${departments.size} depts, ${positions.size} positions, ${employees.size} employees.") // ЛОГ 1

                if (departments.isEmpty() || positions.isEmpty()) {
                    Log.w(TAG, "AvailableVacancies: No departments or positions found, report might be empty.")
                }

                val filledCounts = employees.filter { it.status == EmployeeConstants.STATUS_ACCEPTED }
                    .groupingBy { Pair(it.departmentId, it.positionId) }.eachCount()
                val vacancyData = mutableListOf<VacancyInfo>()
                val departmentMap = departments.associateBy { it.id }
                positions.forEach { pos ->
                    val deptName = departmentMap[pos.departmentId]?.name ?: "Неизвестный отдел ID: ${pos.departmentId}"
                    val filled = filledCounts[Pair(pos.departmentId, pos.id)] ?: 0
                    val available = (pos.maxAllowed - filled).coerceAtLeast(0)
                    vacancyData.add(VacancyInfo(deptName, pos.name, pos.maxAllowed, filled, available))
                }
                Log.d(TAG, "AvailableVacancies: Prepared vacancyData with ${vacancyData.size} entries.") // ЛОГ 2
                // Дополнительный лог для проверки содержимого vacancyData
                vacancyData.take(5).forEach { // Логируем первые 5 для примера
                    Log.d(TAG, "  Vacancy: Dept='${it.departmentName}', Pos='${it.positionName}', Avail=${it.availableCount}")
                }


                _reportState.value = ReportGenerationState.GeneratingPdf
                val resultUriOrPath = withContext(Dispatchers.IO) {
                    Log.d(TAG, "AvailableVacancies: Calling PdfGenerator...") // ЛОГ 3
                    PdfGenerator.generateAvailableVacanciesReport(appContext, contentResolver, vacancyData)
                }

                if (resultUriOrPath != null) {
                    Log.i(TAG, "AvailableVacancies: Report generated successfully. Path/Uri: $resultUriOrPath") // ЛОГ 4
                    _reportState.value = ReportGenerationState.Success(resultUriOrPath)
                } else {
                    Log.e(TAG, "AvailableVacancies: PdfGenerator returned null.") // ЛОГ 5
                    _reportState.value = ReportGenerationState.Error("Не удалось создать PDF файл отчета.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating AvailableVacancies report", e)
                _reportState.value = ReportGenerationState.Error("Ошибка подготовки данных: ${e.message}")
            }
        }
    }

    // Сброс состояния генерации
    fun resetReportState() {
        _reportState.value = ReportGenerationState.Idle
    }
}