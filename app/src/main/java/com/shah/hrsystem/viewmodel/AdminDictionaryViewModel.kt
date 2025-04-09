package com.shah.hrsystem.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.data.db.entity.Position
import com.shah.hrsystem.data.repository.DictionaryRepository
import com.shah.hrsystem.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Состояния UI для управления справочниками
sealed class AdminDictionaryUiState<out T> {
    object Loading : AdminDictionaryUiState<Nothing>()
    // Для Success<T> 'out' уже подразумевается, так как T используется только в позиции 'val'
    data class Success<T>(val items: List<T>) : AdminDictionaryUiState<T>()
    data class Error(val message: String) : AdminDictionaryUiState<Nothing>()
}
// Состояния для операций CRUD
sealed class DictionaryOperationState {
    object Idle : DictionaryOperationState()
    object Processing : DictionaryOperationState()
    data class Success(val message: String) : DictionaryOperationState()
    data class Error(val message: String) : DictionaryOperationState()
}

// Типы справочников
enum class DictionaryType { DEPARTMENTS, POSITIONS, LANGUAGES }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdminDictionaryViewModel @Inject constructor(
    private val dictionaryRepository: DictionaryRepository,
    savedStateHandle: SavedStateHandle // Для получения типа справочника
) : ViewModel() {

    private val TAG = "AdminDictViewModel"

    // Получаем тип справочника из аргументов навигации
    val dictionaryType: StateFlow<DictionaryType?> = savedStateHandle.getStateFlow(Constants.ARG_DICTIONARY_TYPE, null as String?)
        .map { typeString ->
            when (typeString) {
                Constants.DICT_TYPE_DEPARTMENTS -> DictionaryType.DEPARTMENTS
                Constants.DICT_TYPE_POSITIONS -> DictionaryType.POSITIONS
                Constants.DICT_TYPE_LANGUAGES -> DictionaryType.LANGUAGES
                else -> null
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null) // Eagerly, чтобы знать тип сразу

    // StateFlow для статуса операций
    private val _operationState = MutableStateFlow<DictionaryOperationState>(DictionaryOperationState.Idle)
    val operationState: StateFlow<DictionaryOperationState> = _operationState.asStateFlow()

    // StateFlow для данных текущего справочника (используем Any и кастуем)
    private val _currentDictionaryItems = MutableStateFlow<AdminDictionaryUiState<Any>>(AdminDictionaryUiState.Loading)
    val currentDictionaryItems: StateFlow<AdminDictionaryUiState<Any>> = _currentDictionaryItems.asStateFlow()

    // Flow для отделов (нужен для выбора при создании/редактировании должностей)
    val departments: StateFlow<List<Department>> = dictionaryRepository.getAllDepartments()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Загружаем данные при изменении типа справочника
        viewModelScope.launch {
            dictionaryType.filterNotNull().flatMapLatest { type ->
                loadDictionaryData(type) // Вызываем функцию загрузки
            }.collect { state ->
                _currentDictionaryItems.value = state // Обновляем UI state
            }
        }
    }

    // Функция для загрузки данных в зависимости от типа
    private fun loadDictionaryData(type: DictionaryType): Flow<AdminDictionaryUiState<Any>> {
        return when (type) {
            DictionaryType.DEPARTMENTS -> dictionaryRepository.getAllDepartments()
                .map<List<Department>, AdminDictionaryUiState<Any>> { AdminDictionaryUiState.Success(it) } // Кастуем к Any
            DictionaryType.POSITIONS -> dictionaryRepository.getAllPositions()
                .map<List<Position>, AdminDictionaryUiState<Any>> { AdminDictionaryUiState.Success(it) }
            DictionaryType.LANGUAGES -> dictionaryRepository.getAllLanguages()
                .map<List<Language>, AdminDictionaryUiState<Any>> { AdminDictionaryUiState.Success(it) }
        }
            .onStart { emit(AdminDictionaryUiState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error loading ${type.name} dictionary", e)
                emit(AdminDictionaryUiState.Error("Ошибка загрузки справочника: ${e.message}"))
            }
    }

    // --- Функции CRUD ---

    fun addDepartment(name: String) {
        if (dictionaryType.value != DictionaryType.DEPARTMENTS) return
        _operationState.value = DictionaryOperationState.Processing
        viewModelScope.launch {
            val result = dictionaryRepository.addDepartment(name)
            handleOperationResult(result, "Отдел '$name' добавлен", "Ошибка добавления отдела")
        }
    }

    fun addPosition(position: Position) {
        if (dictionaryType.value != DictionaryType.POSITIONS) return
        _operationState.value = DictionaryOperationState.Processing
        viewModelScope.launch {
            val result = dictionaryRepository.addPosition(position)
            handleOperationResult(result, "Должность '${position.name}' добавлена", "Ошибка добавления должности")
        }
    }

    fun addLanguage(name: String) {
        if (dictionaryType.value != DictionaryType.LANGUAGES) return
        _operationState.value = DictionaryOperationState.Processing
        viewModelScope.launch {
            val result = dictionaryRepository.addLanguage(name)
            handleOperationResult(result, "Язык '$name' добавлен", "Ошибка добавления языка")
        }
    }

    fun updateDepartment(department: Department) {
        if (dictionaryType.value != DictionaryType.DEPARTMENTS) return
        _operationState.value = DictionaryOperationState.Processing
        viewModelScope.launch {
            val result = dictionaryRepository.updateDepartment(department)
            handleOperationResult(result, "Отдел '${department.name}' обновлен", "Ошибка обновления отдела")
        }
    }

    fun updatePosition(position: Position) {
        if (dictionaryType.value != DictionaryType.POSITIONS) return
        _operationState.value = DictionaryOperationState.Processing
        viewModelScope.launch {
            val result = dictionaryRepository.updatePosition(position)
            handleOperationResult(result, "Должность '${position.name}' обновлена", "Ошибка обновления должности")
        }
    }

    fun updateLanguage(language: Language) {
        if (dictionaryType.value != DictionaryType.LANGUAGES) return
        _operationState.value = DictionaryOperationState.Processing
        viewModelScope.launch {
            val result = dictionaryRepository.updateLanguage(language)
            handleOperationResult(result, "Язык '${language.name}' обновлен", "Ошибка обновления языка")
        }
    }

    fun deleteItem(item: Any) {
        _operationState.value = DictionaryOperationState.Processing
        viewModelScope.launch {
            val result: Result<Int> // Используем Result<Int> т.к. delete возвращает кол-во удаленных строк
            var itemName = ""
            var itemType = ""

            when (item) {
                is Department -> {
                    result = dictionaryRepository.deleteDepartment(item)
                    itemName = item.name
                    itemType = "Отдел"
                }
                is Position -> {
                    result = dictionaryRepository.deletePosition(item)
                    itemName = item.name
                    itemType = "Должность"
                }
                is Language -> {
                    result = dictionaryRepository.deleteLanguage(item)
                    itemName = item.name
                    itemType = "Язык"
                }
                else -> {
                    _operationState.value = DictionaryOperationState.Error("Неизвестный тип элемента для удаления")
                    return@launch
                }
            }
            handleOperationResult(result, "$itemType '$itemName' удален(а)", "Ошибка удаления")
        }
    }

    // Вспомогательная функция для обработки результата операции
    private fun handleOperationResult(result: Result<*>, successMessage: String, errorPrefix: String) {
        result.fold(
            onSuccess = { _operationState.value = DictionaryOperationState.Success(successMessage) },
            onFailure = { error ->
                // Проверяем специфичные ошибки (например, ConstraintException при удалении)
                val message = if (error is android.database.sqlite.SQLiteConstraintException && error.message?.contains("FOREIGN KEY constraint failed") == true) {
                    "$errorPrefix: Невозможно удалить, так как есть связанные записи (сотрудники)."
                } else {
                    "$errorPrefix: ${error.message}"
                }
                _operationState.value = DictionaryOperationState.Error(message)
                Log.e(TAG, errorPrefix, error)
            }
        )
    }

    // Сброс состояния операции
    fun clearOperationState() {
        _operationState.value = DictionaryOperationState.Idle
    }
}