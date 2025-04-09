package com.shah.hrsystem.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shah.hrsystem.data.db.entity.*
import com.shah.hrsystem.data.repository.DictionaryRepository
import com.shah.hrsystem.data.repository.EmployeeRepository
import com.shah.hrsystem.util.Constants
import com.shah.hrsystem.util.DateUtils
import com.shah.hrsystem.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Состояния UI для флоу кандидата
sealed class CandidateUiState {
    object Idle : CandidateUiState()
    object LoadingDictionaries : CandidateUiState()
    object DictionariesLoaded : CandidateUiState()
    object Saving : CandidateUiState()
    data class SaveSuccess(val employeeId: Long) : CandidateUiState()
    data class Error(val message: String) : CandidateUiState()
    data class ValidationError(val errors: Map<String, String>) : CandidateUiState() // Поле -> Сообщение
}

// Модель для хранения данных языка в UI
data class SelectedLanguageUi(
    val language: Language,
    var level: String = ProficiencyLevel.ELEMENTARY // Уровень по умолчанию
)

@HiltViewModel
class CandidateViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    private val TAG = "CandidateViewModel"

    // --- StateFlow для UI State ---
    private val _uiState = MutableStateFlow<CandidateUiState>(CandidateUiState.Idle)
    val uiState: StateFlow<CandidateUiState> = _uiState.asStateFlow()

    // --- StateFlow для данных из справочников ---
    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions.asStateFlow() // Будут фильтроваться по отделу

    private val _allPositions = MutableStateFlow<List<Position>>(emptyList()) // Для хранения всех должностей

    private val _languages = MutableStateFlow<List<Language>>(emptyList())
    val languages: StateFlow<List<Language>> = _languages.asStateFlow()

    // --- StateFlow для данных кандидата (хранятся между шагами) ---
    val firstName = MutableStateFlow("")
    val lastName = MutableStateFlow("")
    val dateOfBirth = MutableStateFlow<LocalDate?>(null) // Храним как LocalDate
    val dateOfBirthString = MutableStateFlow("") // Строка для отображения/ввода
    val gender = MutableStateFlow<String?>(null) // EmployeeConstants.GENDER_MALE / GENDER_FEMALE
    val educationLevel = MutableStateFlow<String?>(null) // EmployeeConstants.EDU_HIGHER / EDU_SECONDARY
    val totalExperience = MutableStateFlow<Int?>(null)
    val academicExperience = MutableStateFlow<Int?>(null)
    val selectedDepartment = MutableStateFlow<Department?>(null)
    val selectedPosition = MutableStateFlow<Position?>(null)

    // Список выбранных языков с уровнями (для шага 3 и предпросмотра)
    private val _selectedLanguagesUi = MutableStateFlow<List<SelectedLanguageUi>>(emptyList())
    val selectedLanguagesUi: StateFlow<List<SelectedLanguageUi>> = _selectedLanguagesUi.asStateFlow()

    // --- Инициализация: Загрузка справочников ---
    init {
        loadDictionaries()
    }

    private fun loadDictionaries() {
        _uiState.value = CandidateUiState.LoadingDictionaries
        viewModelScope.launch {
            try {
                // Загружаем параллельно
                launch { dictionaryRepository.getAllDepartments().collect { _departments.value = it } }
                launch { dictionaryRepository.getAllPositions().collect { _allPositions.value = it } }
                launch { dictionaryRepository.getAllLanguages().collect { _languages.value = it } }
                // TODO: Дождаться завершения загрузки всех или использовать combine? Пока просто переводим в Loaded.
                // После первой успешной загрузки (или через какое-то время)
                kotlinx.coroutines.delay(500) // Имитация задержки, если collect мгновенный
                _uiState.value = CandidateUiState.DictionariesLoaded
                Log.d(TAG, "Dictionaries loaded")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dictionaries", e)
                _uiState.value = CandidateUiState.Error("Не удалось загрузить справочники: ${e.message}")
            }
        }
    }

    // --- Обновление данных кандидата ---
    fun updateFirstName(name: String) { firstName.value = name }
    fun updateLastName(name: String) { lastName.value = name }
    fun updateDobFromString(dobStr: String) {
        dateOfBirthString.value = dobStr
        dateOfBirth.value = DateUtils.parseDisplayDate(dobStr) // Парсим из dd.MM.yyyy
    }
    fun updateGender(selectedGender: String) { gender.value = selectedGender }
    fun updateEducation(level: String) { educationLevel.value = level }
    fun updateTotalExperience(exp: Int?) { totalExperience.value = exp }
    fun updateAcademicExperience(exp: Int?) { academicExperience.value = exp }

    fun selectDepartment(department: Department?) {
        selectedDepartment.value = department
        // Сбрасываем должность при смене отдела
        selectedPosition.value = null
        // Фильтруем должности для выбранного отдела
        _positions.value = if (department != null) {
            _allPositions.value.filter { it.departmentId == department.id }
        } else {
            emptyList()
        }
    }
    fun selectPosition(position: Position?) { selectedPosition.value = position }

    // --- Управление языками ---
    fun addOrUpdateLanguage(language: Language, level: String) {
        val currentList = _selectedLanguagesUi.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.language.id == language.id }
        if (Validators.isValidProficiencyLevel(level)) {
            if (existingIndex != -1) {
                // Обновляем уровень существующего
                currentList[existingIndex].level = level
            } else {
                // Добавляем новый
                currentList.add(SelectedLanguageUi(language, level))
            }
            _selectedLanguagesUi.value = currentList
        } else {
            Log.w(TAG, "Attempted to add/update language with invalid level: $level")
        }
    }

    fun removeLanguage(language: Language) {
        val currentList = _selectedLanguagesUi.value.toMutableList()
        currentList.removeAll { it.language.id == language.id }
        _selectedLanguagesUi.value = currentList
    }

    // --- Валидация и Сохранение ---
    fun validateAndSaveApplication() {
        val errors = mutableMapOf<String, String>()

        // Валидация полей
        if (firstName.value.isBlank()) errors["firstName"] = "Имя не заполнено"
        if (lastName.value.isBlank()) errors["lastName"] = "Фамилия не заполнена"
        val dob = dateOfBirth.value
        if (dob == null || !Validators.isValidDateNotInFuture(dob)!!) {
            errors["dateOfBirth"] = "Некорректная дата рождения"
        } else {
            // Проверка пенсионного возраста
            val isValidAge = Validators.isValidAge(DateUtils.formatLocalDateToIso(dob), gender.value)
            if (isValidAge == null || !isValidAge) {
                errors["dateOfBirth"] = "Возраст не соответствует требованиям (пенсионный)"
            }
        }
        if (gender.value == null) errors["gender"] = "Пол не выбран"
        if (educationLevel.value == null) errors["educationLevel"] = "Уровень образования не выбран"
        val totalExp = totalExperience.value
        val academicExp = academicExperience.value
        if (totalExp == null || totalExp < 0) errors["totalExperience"] = "Некорректный общий стаж"
        if (academicExp == null || academicExp < 0) errors["academicExperience"] = "Некорректный академ. стаж"
        if (selectedDepartment.value == null) errors["department"] = "Подразделение не выбрано"
        val position = selectedPosition.value
        if (position == null) {
            errors["position"] = "Должность не выбрана"
        } else {
            // Валидация образования для должности
            val isEduValid = Validators.isEducationValidForPosition(educationLevel.value, position)
            if (isEduValid == null || !isEduValid) {
                errors["educationLevel"] = "Уровень образования не соответствует должности"
            }
            // Валидация стажа для должности
            val isExpValid = Validators.isValidExperience(totalExp, academicExp, position)
            if (isExpValid == null || !isExpValid) {
                errors["experience"] = "Стаж не соответствует требованиям должности"
            }
        }
        // Языки не обязательны по ТЗ, поэтому не валидируем их наличие

        if (errors.isNotEmpty()) {
            _uiState.value = CandidateUiState.ValidationError(errors)
            // Сбрасываем в Idle, чтобы UI мог показать ошибки и позволить исправить
            _uiState.value = CandidateUiState.Idle
            return
        }

        // --- Если валидация прошла ---
        _uiState.value = CandidateUiState.Saving
        viewModelScope.launch {
            try {
                val employeeToSave = Employee(
                    // id будет сгенерирован базой
                    firstName = firstName.value.trim(),
                    lastName = lastName.value.trim(),
                    dateOfBirth = DateUtils.formatLocalDateToIso(dateOfBirth.value)!!, // Уже проверили на null
                    gender = gender.value!!,
                    positionId = selectedPosition.value!!.id,
                    departmentId = selectedDepartment.value!!.id,
                    educationLevel = educationLevel.value!!,
                    totalExperience = totalExperience.value!!,
                    academicExperience = academicExperience.value!!,
                    status = EmployeeConstants.STATUS_NEW // Статус по умолчанию
                    // Остальные поля (employmentDate, tariffRate, personalNumber) остаются null
                )

                val languagesToSave = _selectedLanguagesUi.value.map {
                    EmployeeLanguage(
                        // id будет сгенерирован
                        // employeeId будет установлен репозиторием
                        employeeId = 0, // Временное значение
                        languageId = it.language.id,
                        proficiencyLevel = it.level
                    )
                }

                val result = employeeRepository.saveCandidateApplication(employeeToSave, languagesToSave)

                result.fold(
                    onSuccess = { employeeId ->
                        _uiState.value = CandidateUiState.SaveSuccess(employeeId)
                        Log.i(TAG, "Application saved successfully, ID: $employeeId")
                        resetCandidateData() // Очищаем ViewModel после успешной отправки
                        _uiState.value = CandidateUiState.Idle // Возвращаемся в Idle
                    },
                    onFailure = { error ->
                        _uiState.value = CandidateUiState.Error("Ошибка сохранения: ${error.message}")
                        Log.e(TAG, "Failed to save application", error)
                        _uiState.value = CandidateUiState.Idle // Возвращаемся в Idle
                    }
                )
            } catch (e: Exception) {
                _uiState.value = CandidateUiState.Error("Критическая ошибка сохранения: ${e.message}")
                Log.e(TAG, "Critical error saving application", e)
                _uiState.value = CandidateUiState.Idle
            }
        }
    }

    // Сброс данных кандидата после успешной отправки или отмены
    fun resetCandidateData() {
        firstName.value = ""
        lastName.value = ""
        dateOfBirth.value = null
        dateOfBirthString.value = ""
        gender.value = null
        educationLevel.value = null
        totalExperience.value = null
        academicExperience.value = null
        selectDepartment(null) // Это также сбросит должность и отфильтрованные позиции
        _selectedLanguagesUi.value = emptyList()
        _uiState.value = CandidateUiState.Idle // Возвращаем в начальное состояние
    }

    // Сброс состояния ошибки
    fun clearError() {
        if (_uiState.value is CandidateUiState.Error || _uiState.value is CandidateUiState.ValidationError) {
            _uiState.value = CandidateUiState.Idle
        }
    }
}