package com.shah.hrsystem.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shah.hrsystem.data.db.entity.*
import com.shah.hrsystem.data.repository.DictionaryRepository
import com.shah.hrsystem.data.repository.EmployeeRepository
import com.shah.hrsystem.util.Constants
import com.shah.hrsystem.util.DateUtils
import com.shah.hrsystem.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Состояния UI для экрана деталей
sealed class EmployeeDetailsUiState {
    object Loading : EmployeeDetailsUiState()
    data class Success(
        val employee: Employee,
        val position: Position?, // Должность для отображения имени
        val department: Department?, // Отдел для отображения имени
        val languages: List<Pair<Language, String>> // Язык и уровень
    ) : EmployeeDetailsUiState()
    data class Error(val message: String) : EmployeeDetailsUiState()
    // Состояния для операций
    object Processing : EmployeeDetailsUiState() // Принятие/Отклонение/Удаление/Сохранение
    data class OperationSuccess(val message: String) : EmployeeDetailsUiState()
    data class OperationError(val message: String) : EmployeeDetailsUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EmployeeDetailsViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    private val dictionaryRepository: DictionaryRepository,
    savedStateHandle: SavedStateHandle // Для получения employeeId из аргументов навигации
) : ViewModel() {

    private val TAG = "EmployeeDetailsViewModel"
    // Получаем ID сотрудника из аргументов навигации
    val employeeId: StateFlow<Int?> = savedStateHandle.getStateFlow(Constants.ARG_EMPLOYEE_ID, null)

    // Приватный MutableStateFlow для основного состояния UI
    private val _uiState = MutableStateFlow<EmployeeDetailsUiState>(EmployeeDetailsUiState.Loading)
    val uiState: StateFlow<EmployeeDetailsUiState> = _uiState.asStateFlow()

    // --- Поля для редактирования (используем StateFlow для двусторонней привязки, если нужно) ---
    // Лучше обновлять _uiState.Success с измененными данными перед сохранением
    val editableFirstName = MutableStateFlow("")
    val editableLastName = MutableStateFlow("")
    val editableDobString = MutableStateFlow("") // dd.MM.yyyy
    val editableGender = MutableStateFlow<String?>(null)
    val editableEducation = MutableStateFlow<String?>(null)
    val editableTotalExp = MutableStateFlow<Int?>(null)
    val editableAcademicExp = MutableStateFlow<Int?>(null)
    val editableSelectedDepartment = MutableStateFlow<Department?>(null)
    val editableSelectedPosition = MutableStateFlow<Position?>(null)
    val editableLanguagesUi = MutableStateFlow<List<SelectedLanguageUi>>(emptyList()) // Как в CandidateVM
    val editableTariffRate = MutableStateFlow<Int?>(null)

    // --- Справочники для выбора при редактировании ---
    val departments: StateFlow<List<Department>> = dictionaryRepository.getAllDepartments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPositions: StateFlow<List<Position>> = dictionaryRepository.getAllPositions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val filteredPositions = MutableStateFlow<List<Position>>(emptyList()) // Позиции для выбранного отдела
    val languages: StateFlow<List<Language>> = dictionaryRepository.getAllLanguages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Инициализация: Загрузка данных при изменении employeeId ---
    init {
        viewModelScope.launch {
            employeeId.filterNotNull().flatMapLatest { id ->
                // Комбинируем потоки данных для сотрудника, его языков и справочников
                combine(
                    employeeRepository.getEmployeeById(id),
                    employeeRepository.getLanguagesForEmployee(id),
                    dictionaryRepository.getAllPositions(), // Понадобятся для имен
                    dictionaryRepository.getAllDepartments(), // Понадобятся для имен
                    dictionaryRepository.getAllLanguages() // Понадобятся для имен языков
                ) { employee, empLangs, positions, departments, allLangs ->
                    // Обрабатываем данные и формируем Success состояние
                    if (employee == null) {
                        EmployeeDetailsUiState.Error("Сотрудник с ID $id не найден")
                    } else {
                        val position = positions.find { it.id == employee.positionId }
                        val department = departments.find { it.id == employee.departmentId }
                        val languagesWithNames = empLangs.mapNotNull { empLang ->
                            allLangs.find { it.id == empLang.languageId }
                                ?.let { lang -> Pair(lang, empLang.proficiencyLevel) }
                        }
                        // Инициализируем редактируемые поля
                        initializeEditableFields(employee, languagesWithNames, department, position)
                        EmployeeDetailsUiState.Success(employee, position, department, languagesWithNames)
                    }
                }
            }
                .onStart { _uiState.value = EmployeeDetailsUiState.Loading }
                .catch { e ->
                    Log.e(TAG, "Error loading employee details", e)
                    _uiState.value = EmployeeDetailsUiState.Error("Ошибка загрузки данных: ${e.message}")
                }
                .collect { state ->
                    _uiState.value = state
                    // Если загружен отдел, фильтруем должности
                    if (state is EmployeeDetailsUiState.Success) {
                        filterPositionsForDepartment(state.department)
                    }
                }
        }

        // Следим за изменением выбранного отдела при редактировании
        viewModelScope.launch {
            editableSelectedDepartment.collect { department ->
                filterPositionsForDepartment(department)
            }
        }
    }

    // Инициализация полей для редактирования
    internal fun initializeEditableFields(
        employee: Employee,
        languages: List<Pair<Language, String>>,
        department: Department?,
        position: Position?
    ) {
        editableFirstName.value = employee.firstName
        editableLastName.value = employee.lastName
        editableDobString.value = DateUtils.formatIsoDateForDisplay(employee.dateOfBirth)
        editableGender.value = employee.gender
        editableEducation.value = employee.educationLevel
        editableTotalExp.value = employee.totalExperience
        editableAcademicExp.value = employee.academicExperience
        editableSelectedDepartment.value = department
        editableSelectedPosition.value = position
        editableLanguagesUi.value = languages.map { SelectedLanguageUi(it.first, it.second) }
        editableTariffRate.value = employee.tariffRate
    }

    // Фильтрация должностей при выборе отдела в режиме редактирования
    private fun filterPositionsForDepartment(department: Department?) {
        filteredPositions.value = if (department != null) {
            allPositions.value.filter { it.departmentId == department.id }
        } else {
            emptyList()
        }
        // Сбросить выбранную позицию, если она не принадлежит новому отделу
        if (editableSelectedPosition.value?.departmentId != department?.id) {
            editableSelectedPosition.value = null
        }
    }

    // --- Функции управления языками при редактировании ---
    fun addOrUpdateEditableLanguage(language: Language, level: String) {
        val currentList = editableLanguagesUi.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.language.id == language.id }
        if (Validators.isValidProficiencyLevel(level)) {
            if (existingIndex != -1) {
                currentList[existingIndex].level = level
            } else {
                currentList.add(SelectedLanguageUi(language, level))
            }
            editableLanguagesUi.value = currentList
        }
    }
    fun removeEditableLanguage(language: Language) {
        val currentList = editableLanguagesUi.value.toMutableList()
        currentList.removeAll { it.language.id == language.id }
        editableLanguagesUi.value = currentList
    }


    // --- Функции для выполнения операций ---

    fun saveChanges() {
        val currentEmployeeId = employeeId.value ?: return // Нужен ID
        val currentState = _uiState.value
        if (currentState !is EmployeeDetailsUiState.Success) return // Нужны текущие данные

        // Валидация редактируемых полей (аналогично CandidateViewModel)
        val errors = mutableMapOf<String, String>()
        val firstName = editableFirstName.value
        val lastName = editableLastName.value
        val dobString = editableDobString.value
        val gender = editableGender.value
        val education = editableEducation.value
        val totalExp = editableTotalExp.value
        val academicExp = editableAcademicExp.value
        val department = editableSelectedDepartment.value
        val position = editableSelectedPosition.value
        val dob = DateUtils.parseDisplayDate(dobString)

        if (firstName.isBlank()) errors["firstName"] = "Имя не заполнено"
        if (lastName.isBlank()) errors["lastName"] = "Фамилия не заполнена"
        if (dob == null || !Validators.isValidDateNotInFuture(dob)!!) {
            errors["dateOfBirth"] = "Некорректная дата рождения"
        } else {
            val isValidAge = Validators.isValidAge(DateUtils.formatLocalDateToIso(dob), gender)
            if (isValidAge == null || !isValidAge) {
                errors["dateOfBirth"] = "Возраст не соответствует требованиям (пенсионный)"
            }
        }
        if (gender == null) errors["gender"] = "Пол не выбран"
        if (education == null) errors["educationLevel"] = "Уровень образования не выбран"
        if (totalExp == null || totalExp < 0) errors["totalExperience"] = "Некорректный общий стаж"
        if (academicExp == null || academicExp < 0) errors["academicExperience"] = "Некорректный академ. стаж"
        if (department == null) errors["department"] = "Подразделение не выбрано"
        if (position == null) {
            errors["position"] = "Должность не выбрана"
        } else {
            val isEduValid = Validators.isEducationValidForPosition(education, position)
            if (isEduValid == null || !isEduValid) {
                errors["educationLevel"] = "Уровень образования не соответствует должности"
            }
            val isExpValid = Validators.isValidExperience(totalExp, academicExp, position)
            if (isExpValid == null || !isExpValid) {
                errors["experience"] = "Стаж не соответствует требованиям должности"
            }
        }
        // Валидация тарифного разряда (если сотрудник принят)
        if (currentState.employee.status == EmployeeConstants.STATUS_ACCEPTED) {
            val rate = editableTariffRate.value
            val isRateValid = Validators.isValidTariffRate(rate)
            if (isRateValid == null || !isRateValid) {
                errors["tariffRate"] = "Некорректный тарифный разряд (${Constants.MIN_TARIFF_RATE}-${Constants.MAX_TARIFF_RATE})"
            }
        }


        if (errors.isNotEmpty()) {
            // Можно показать ошибки пользователю через _uiState или отдельный Flow
            _uiState.value = EmployeeDetailsUiState.OperationError("Ошибка валидации: ${errors.values.first()}")
            // Возвращаем предыдущее Success состояние, чтобы не сбрасывать поля
            _uiState.value = currentState
            return
        }

        // --- Если валидация прошла ---
        _uiState.value = EmployeeDetailsUiState.Processing
        viewModelScope.launch {
            val updatedEmployee = currentState.employee.copy( // Берем текущего и меняем поля
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                dateOfBirth = DateUtils.formatLocalDateToIso(dob)!!,
                gender = gender!!,
                positionId = position!!.id,
                departmentId = department!!.id,
                educationLevel = education!!,
                totalExperience = totalExp!!,
                academicExperience = academicExp!!,
                // Тарифный разряд обновляем только если он применим (сотрудник принят)
                tariffRate = if (currentState.employee.status == EmployeeConstants.STATUS_ACCEPTED) editableTariffRate.value else currentState.employee.tariffRate
            )
            val updatedLanguages = editableLanguagesUi.value.map {
                EmployeeLanguage(employeeId = currentEmployeeId, languageId = it.language.id, proficiencyLevel = it.level)
            }

            val result = employeeRepository.updateEmployeeData(updatedEmployee, updatedLanguages)
            result.fold(
                onSuccess = {
                    _uiState.value = EmployeeDetailsUiState.OperationSuccess("Данные успешно обновлены")
                    // Состояние Success обновится автоматически через flow combine
                },
                onFailure = { error ->
                    _uiState.value = EmployeeDetailsUiState.OperationError("Ошибка обновления: ${error.message}")
                    _uiState.value = currentState // Возврат к данным до ошибки
                }
            )
        }
    }

    fun acceptApplication() {
        val currentEmployeeId = employeeId.value ?: return
        val currentState = _uiState.value
        if (currentState !is EmployeeDetailsUiState.Success || currentState.employee.status != EmployeeConstants.STATUS_NEW) return
        val rate = editableTariffRate.value // Берем введенный разряд

        val isRateValid = Validators.isValidTariffRate(rate)
        if (isRateValid == null || !isRateValid) {
            _uiState.value = EmployeeDetailsUiState.OperationError("Введите корректный тарифный разряд (${Constants.MIN_TARIFF_RATE}-${Constants.MAX_TARIFF_RATE})")
            _uiState.value = currentState // Возврат к данным до ошибки
            return
        }

        _uiState.value = EmployeeDetailsUiState.Processing
        viewModelScope.launch {
            val result = employeeRepository.acceptApplication(currentEmployeeId, rate!!)
            result.fold(
                onSuccess = {
                    _uiState.value = EmployeeDetailsUiState.OperationSuccess("Заявка успешно принята")
                    // Данные обновятся через flow
                },
                onFailure = { error ->
                    _uiState.value = EmployeeDetailsUiState.OperationError("Ошибка принятия: ${error.message}")
                    _uiState.value = currentState
                }
            )
        }
    }

    fun rejectApplication() {
        val currentEmployeeId = employeeId.value ?: return
        val currentState = _uiState.value
        if (currentState !is EmployeeDetailsUiState.Success || currentState.employee.status != EmployeeConstants.STATUS_NEW) return

        _uiState.value = EmployeeDetailsUiState.Processing
        viewModelScope.launch {
            val result = employeeRepository.rejectApplication(currentEmployeeId)
            result.fold(
                onSuccess = {
                    // Успешное удаление - нужно навигироваться назад
                    _uiState.value = EmployeeDetailsUiState.OperationSuccess("Заявка отклонена (удалена)")
                    // Fragment должен обработать это состояние и выполнить findNavController().popBackStack()
                },
                onFailure = { error ->
                    _uiState.value = EmployeeDetailsUiState.OperationError("Ошибка отклонения: ${error.message}")
                    _uiState.value = currentState
                }
            )
        }
    }

    fun deleteAcceptedEmployee() {
        val currentEmployeeId = employeeId.value ?: return
        val currentState = _uiState.value
        if (currentState !is EmployeeDetailsUiState.Success || currentState.employee.status != EmployeeConstants.STATUS_ACCEPTED) return

        _uiState.value = EmployeeDetailsUiState.Processing
        viewModelScope.launch {
            val result = employeeRepository.deleteAcceptedEmployee(currentEmployeeId)
            result.fold(
                onSuccess = {
                    _uiState.value = EmployeeDetailsUiState.OperationSuccess("Сотрудник удален")
                    // Fragment должен обработать это состояние и выполнить findNavController().popBackStack()
                },
                onFailure = { error ->
                    _uiState.value = EmployeeDetailsUiState.OperationError("Ошибка удаления: ${error.message}")
                    _uiState.value = currentState
                }
            )
        }
    }

    // Сброс состояния операции (например, после показа Snackbar)
    fun clearOperationStatus() {
        val currentState = _uiState.value
        if (currentState is EmployeeDetailsUiState.OperationSuccess || currentState is EmployeeDetailsUiState.OperationError) {
            // Пытаемся найти предыдущее Success состояние или ставим Loading
            // Это сложно, лучше перестроить логику или использовать SingleLiveEvent/SharedFlow для событий
            // Пока просто вернем в Loading, чтобы данные перезагрузились
            _uiState.value = EmployeeDetailsUiState.Loading
            // Перезапускаем загрузку данных
            viewModelScope.launch { employeeId.value?.let { /* триггер перезагрузки, если необходимо */ } }
        }
    }
}