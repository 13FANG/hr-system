package com.shah.hrsystem.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Employee
import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.data.db.entity.UserRole // Добавим импорт UserRole
import com.shah.hrsystem.data.repository.DictionaryRepository
import com.shah.hrsystem.data.repository.EmployeeRepository
import com.shah.hrsystem.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.shah.hrsystem.data.db.entity.Position

// Состояния UI для дашборда
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val employees: List<Employee>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val TAG = "DashboardViewModel"

    // --- StateFlow для фильтров ---
    val statusFilter = MutableStateFlow<String?>(null)
    val departmentFilter = MutableStateFlow<Int?>(null)
    val searchQuery = MutableStateFlow("")

    // --- StateFlow для UI State и данных ---
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val departments: StateFlow<List<Department>> = dictionaryRepository.getAllDepartments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow для роли текущего пользователя
    val currentUserRole: StateFlow<String?> = sessionManager.currentUser.map { user ->
        Log.d(TAG, "SessionManager emitted user: ${user?.login}, role: ${user?.role}")
        user?.role
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Переменная для хранения последней обработанной роли
    var lastKnownRoleInternal: String? = null
        private set // Сеттер приватный

    // --- Кэш для имен справочников ---
    val departmentCache = MutableStateFlow<Map<Int, String>>(emptyMap())
    val positionCache = MutableStateFlow<Map<Int, String>>(emptyMap())

    init {
        // Загружаем справочники для кэша
        viewModelScope.launch {
            launch {
                dictionaryRepository.getAllDepartments().collect { list ->
                    departmentCache.value = list.associateBy({ it.id }, { it.name })
                }
            }
            launch {
                dictionaryRepository.getAllPositions().collect { list ->
                    positionCache.value = list.associateBy({ it.id }, { it.name })
                }
            }
        }

        // Основной поток данных для списка сотрудников
        viewModelScope.launch {
            combine(
                employeeRepository.getAllEmployees(),
                statusFilter,
                departmentFilter,
                searchQuery
            ) { employees, status, departmentId, query ->
                employees.filter { emp ->
                    val statusMatch = status == null || emp.status == status
                    val departmentMatch = departmentId == null || emp.departmentId == departmentId
                    val queryMatch = query.isBlank() ||
                            emp.firstName.contains(query, ignoreCase = true) ||
                            emp.lastName.contains(query, ignoreCase = true) ||
                            "${emp.lastName} ${emp.firstName}".contains(query, ignoreCase = true) ||
                            "${emp.firstName} ${emp.lastName}".contains(query, ignoreCase = true)
                    statusMatch && departmentMatch && queryMatch
                }
            }
                .onStart { _uiState.value = DashboardUiState.Loading }
                .catch { e ->
                    Log.e(TAG, "Error loading or filtering employees", e)
                    _uiState.value = DashboardUiState.Error("Ошибка загрузки списка: ${e.message}")
                }
                .collect { filteredEmployees ->
                    _uiState.value = DashboardUiState.Success(filteredEmployees)
                }
        }

        // Отдельный сборщик для обновления lastKnownRoleInternal
        viewModelScope.launch {
            currentUserRole.collect { role ->
                Log.d(TAG, "Updating lastKnownRoleInternal to: $role")
                lastKnownRoleInternal = role
            }
        }
    }

    // --- Функции для обновления фильтров из UI ---
    fun applyStatusFilter(status: String?) { statusFilter.value = status }
    fun applyDepartmentFilter(departmentId: Int?) { departmentFilter.value = departmentId }
    fun applySearchQuery(query: String) { searchQuery.value = query }
    fun refreshData() { _uiState.value = DashboardUiState.Loading }
}