package com.shah.hrsystem.ui.main.details

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI // Оставляем для кнопки назад
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.*
import com.shah.hrsystem.databinding.FragmentEmployeeDetailsBinding
import com.shah.hrsystem.ui.candidate.adapter.CandidateLanguageAdapter
import com.shah.hrsystem.ui.main.details.adapter.EmployeeLanguageDetailsAdapter
import com.shah.hrsystem.util.Constants
import com.shah.hrsystem.util.DateUtils
import com.shah.hrsystem.util.SessionManager
import com.shah.hrsystem.util.Validators
import com.shah.hrsystem.viewmodel.EmployeeDetailsUiState
import com.shah.hrsystem.viewmodel.EmployeeDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

@AndroidEntryPoint
class EmployeeDetailsFragment : Fragment() {

    private var _binding: FragmentEmployeeDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmployeeDetailsViewModel by viewModels()
    private val args: EmployeeDetailsFragmentArgs by navArgs()

    private lateinit var languageDetailAdapter: EmployeeLanguageDetailsAdapter
    private lateinit var languageEditAdapter: CandidateLanguageAdapter
    private var departmentAdapter: ArrayAdapter<String>? = null
    private var positionAdapter: ArrayAdapter<String>? = null
    private var languageSelectAdapter: ArrayAdapter<String>? = null
    private var levelSelectAdapter: ArrayAdapter<String>? = null

    private var selectedLanguageToAdd: Language? = null
    private var selectedLevelToAdd: String = ProficiencyLevel.ELEMENTARY

    private var _isEditMode = false
    private val isEditMode: Boolean get() = _isEditMode

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    @Inject
    lateinit var sessionManager: SessionManager

    private val TAG = "EmployeeDetailsFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // УБИРАЕМ setHasOptionsMenu(true)
        // setHasOptionsMenu(true)

        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (_isEditMode) {
                    Log.d(TAG, "Back pressed callback handled in Edit mode")
                    setEditMode(false)
                    restoreFieldsBeforeExit()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeDetailsBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView")
        setupToolbarWithNavigation()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        setupRecyclerViews()
        setupDropdowns()
        observeViewModel()
        setupEditModeListeners()
        setupMenu() // Настраиваем меню здесь
        setEditMode(false) // Устанавливаем начальное состояние
    }

    // Настройка Toolbar с NavigationUI (только для кнопки "Назад")
    private fun setupToolbarWithNavigation() {
        val navController = findNavController()
        // Используем setOf() если у тебя только один top-level destination или NavGraph,
        // иначе настрой согласно твоему графу навигации
        val appBarConfiguration = AppBarConfiguration(navController.graph) // Или AppBarConfiguration(setOf(R.id.dashboardFragment)) если это верхний уровень
        val toolbar = binding.toolbarDetails

        // Связываем с NavController ТОЛЬКО для кнопки "Назад"
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)

        // Устанавливаем СВОЙ обработчик для кнопки "Назад" в Toolbar
        toolbar.setNavigationOnClickListener {
            if (_isEditMode) {
                Log.d(TAG, "Toolbar navigation clicked in Edit mode")
                // Показываем диалог подтверждения выхода
                showExitConfirmationDialog {
                    // Этот код выполнится, если пользователь подтвердит выход
                    setEditMode(false)
                    restoreFieldsBeforeExit()
                }
            } else {
                // Обычный выход, если не в режиме редактирования
                findNavController().popBackStack()
            }
        }
        // Заголовок будет установлен позже
        // УБИРАЕМ ДУБЛИРУЮЩИЙ ВЫЗОВ: toolbar.inflateMenu(R.menu.details_menu)
        // Меню уже инфлейтится через app:menu в XML
    }

    // ИСПРАВЛЕНО: Настройка меню через Toolbar напрямую
    private fun setupMenu() {
        binding.toolbarDetails.setOnMenuItemClickListener { menuItem ->
            Log.d(TAG, "Toolbar MenuItem Clicked: ${menuItem.title} (ID: ${menuItem.itemId})")
            when (menuItem.itemId) {
                R.id.action_edit_pencil -> {
                    Log.d(TAG, "Edit button clicked")
                    setEditMode(true)
                    true
                }
                R.id.action_save -> {
                    Log.d(TAG, "Save button clicked")
                    viewModel.saveChanges()
                    true
                }
                R.id.action_delete -> {
                    Log.d(TAG, "Delete button clicked")
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        // Устанавливаем начальную видимость меню
        updateMenuVisibility()
    }

    // ИСПРАВЛЕНО: Функция для ручного обновления видимости меню
    private fun updateMenuVisibility() {
        val menu = binding.toolbarDetails.menu
        if (menu == null) {
            Log.w(TAG, "updateMenuVisibility: Menu is null")
            return // Выходим, если меню еще не готово
        }

        val currentState = viewModel.uiState.value
        val employee = (currentState as? EmployeeDetailsUiState.Success)?.employee
        val canEditDetails = employee != null

        val editVisible = !_isEditMode && canEditDetails
        val saveVisible = _isEditMode && canEditDetails
        val deleteVisible = !_isEditMode && employee?.status == EmployeeConstants.STATUS_ACCEPTED

        menu.findItem(R.id.action_edit_pencil)?.isVisible = editVisible
        menu.findItem(R.id.action_save)?.isVisible = saveVisible
        menu.findItem(R.id.action_delete)?.isVisible = deleteVisible

        Log.d(TAG, "updateMenuVisibility: isEditMode=$_isEditMode, edit=$editVisible, save=$saveVisible, delete=$deleteVisible")
    }


    // --- Стандартная обработка меню (БОЛЬШЕ НЕ ИСПОЛЬЗУЕТСЯ) ---
    /*
    @Deprecated("Deprecated in Java")
     override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {}
    @Deprecated("Deprecated in Java")
     override fun onPrepareOptionsMenu(menu: Menu) {}
    @Deprecated("Deprecated in Java")
     override fun onOptionsItemSelected(item: MenuItem): Boolean {}
    */
    // --- Конец стандартной обработки меню ---

    private fun setupRecyclerViews() {
        languageDetailAdapter = EmployeeLanguageDetailsAdapter()
        binding.rvDetailsLanguages.apply {
            adapter = languageDetailAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        languageEditAdapter = CandidateLanguageAdapter { languageToRemove ->
            viewModel.removeEditableLanguage(languageToRemove)
        }
        binding.rvDetailsSelectedLanguages.apply {
            adapter = languageEditAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupDropdowns() {
        departmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.actvDetailsDepartment.setAdapter(departmentAdapter)
        positionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.actvDetailsPosition.setAdapter(positionAdapter)

        languageSelectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.actvDetailsLanguageSelect.setAdapter(languageSelectAdapter)
        val levels = ProficiencyLevel.ALL.map { mapLevelToResourceString(it) }
        levelSelectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, levels)
        binding.actvDetailsLevelSelect.setAdapter(levelSelectAdapter)
        binding.actvDetailsLevelSelect.setText(mapLevelToResourceString(selectedLevelToAdd), false)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    Log.d(TAG, "Observed UI State: ${state::class.java.simpleName}, EditMode: $_isEditMode")
                    binding.progressBarDetails.isVisible = state is EmployeeDetailsUiState.Loading || state is EmployeeDetailsUiState.Processing
                    val shouldShowContent = state is EmployeeDetailsUiState.Success || state is EmployeeDetailsUiState.OperationError
                    binding.contentGroup.isVisible = shouldShowContent

                    if (state is EmployeeDetailsUiState.Success) {
                        displayEmployeeData(state)
                        updateUiForViewMode()
                    }

                    when (state) {
                        is EmployeeDetailsUiState.Error -> {
                            if (_isEditMode) setEditMode(false) // Выходим из ред. при ошибке загрузки
                            showSnackbar(state.message)
                        }
                        is EmployeeDetailsUiState.OperationSuccess -> {
                            showSnackbar(state.message)
                            if (state.message.contains("удален") || state.message.contains("отклонена")) {
                                findNavController().popBackStack()
                            } else {
                                // Успешное сохранение или принятие
                                if (_isEditMode) setEditMode(false) // Выключаем режим редактирования
                                viewModel.clearOperationStatus()
                            }
                        }
                        is EmployeeDetailsUiState.OperationError -> {
                            showSnackbar(state.message)
                            viewModel.clearOperationStatus()
                            binding.progressBarDetails.isVisible = false
                            // Остаемся в режиме редактирования
                        }
                        else -> { /* Idle, Loading, Processing, Success */ }
                    }
                    // Обновляем видимость меню после обработки состояния
                    updateMenuVisibility()
                }
            }
        }

        // Наблюдения за справочниками и языками
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.departments.collectLatest { departments ->
                    departmentAdapter?.clear()
                    departmentAdapter?.addAll(departments.map { it.name })
                    departmentAdapter?.notifyDataSetChanged()
                    if (!_isEditMode) {
                        binding.actvDetailsDepartment.setText(viewModel.editableSelectedDepartment.value?.name ?: "", false)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredPositions.collectLatest { positions ->
                    positionAdapter?.clear()
                    positionAdapter?.addAll(positions.map { it.name })
                    positionAdapter?.notifyDataSetChanged()
                    if (!_isEditMode) {
                        binding.actvDetailsPosition.setText(viewModel.editableSelectedPosition.value?.name ?: "", false)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.languages.collectLatest { languages ->
                    languageSelectAdapter?.clear()
                    languageSelectAdapter?.addAll(languages.map { it.name })
                    languageSelectAdapter?.notifyDataSetChanged()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.editableLanguagesUi.collectLatest { selectedLanguages ->
                    languageEditAdapter.submitList(selectedLanguages)
                }
            }
        }
    }


    // Отображает данные в полях ввода/текста из ViewModel
    private fun displayEmployeeData(state: EmployeeDetailsUiState.Success) {
        Log.d(TAG, "displayEmployeeData called")
        val employee = state.employee
        binding.toolbarDetails.title = if (employee.status == EmployeeConstants.STATUS_NEW) {
            getString(R.string.details_title_application)
        } else {
            getString(R.string.details_title_employee)
        }

        binding.etDetailsLastName.setText(viewModel.editableLastName.value)
        binding.etDetailsFirstName.setText(viewModel.editableFirstName.value)
        binding.etDetailsDob.setText(viewModel.editableDobString.value)

        val genderId = if (viewModel.editableGender.value == EmployeeConstants.GENDER_MALE) R.id.rb_details_male else R.id.rb_details_female
        if (binding.rgDetailsGender.checkedRadioButtonId != genderId) {
            binding.rgDetailsGender.check(genderId)
        }
        val eduId = if (viewModel.editableEducation.value == EmployeeConstants.EDU_HIGHER) R.id.rb_details_edu_higher else R.id.rb_details_edu_secondary
        if (binding.rgDetailsEducation.checkedRadioButtonId != eduId) {
            binding.rgDetailsEducation.check(eduId)
        }

        binding.etDetailsTotalExp.setText(viewModel.editableTotalExp.value?.toString() ?: "")
        binding.etDetailsAcademicExp.setText(viewModel.editableAcademicExp.value?.toString() ?: "")

        binding.actvDetailsDepartment.setText(viewModel.editableSelectedDepartment.value?.name ?: "", false)
        binding.actvDetailsPosition.setText(viewModel.editableSelectedPosition.value?.name ?: "", false)
        binding.etDetailsTariffRate.setText(viewModel.editableTariffRate.value?.toString() ?: "")

        languageDetailAdapter.submitList(state.languages)
        languageEditAdapter.submitList(viewModel.editableLanguagesUi.value)
    }


    private fun setupEditModeListeners() {
        // Слушатели для обычных полей
        binding.etDetailsLastName.addTextChangedListener { if (_isEditMode) viewModel.editableLastName.value = it.toString() }
        binding.etDetailsFirstName.addTextChangedListener { if (_isEditMode) viewModel.editableFirstName.value = it.toString() }
        binding.etDetailsTotalExp.addTextChangedListener { if (_isEditMode) viewModel.editableTotalExp.value = it.toString().toIntOrNull() }
        binding.etDetailsAcademicExp.addTextChangedListener { if (_isEditMode) viewModel.editableAcademicExp.value = it.toString().toIntOrNull() }
        binding.etDetailsTariffRate.addTextChangedListener {
            if (binding.etDetailsTariffRate.isEnabled) {
                viewModel.editableTariffRate.value = it.toString().toIntOrNull()
                binding.tilDetailsTariffRate.error = null
            }
        }

        binding.etDetailsDob.setOnClickListener { if (_isEditMode) showDatePicker() }
        binding.tilDetailsDob.setEndIconOnClickListener { if (_isEditMode) showDatePicker() }

        binding.rgDetailsGender.setOnCheckedChangeListener { _, checkedId ->
            if (_isEditMode) {
                viewModel.editableGender.value = when (checkedId) {
                    R.id.rb_details_male -> EmployeeConstants.GENDER_MALE
                    R.id.rb_details_female -> EmployeeConstants.GENDER_FEMALE
                    else -> null
                }
            }
        }
        binding.rgDetailsEducation.setOnCheckedChangeListener { _, checkedId ->
            if (_isEditMode) {
                viewModel.editableEducation.value = when (checkedId) {
                    R.id.rb_details_edu_higher -> EmployeeConstants.EDU_HIGHER
                    R.id.rb_details_edu_secondary -> EmployeeConstants.EDU_SECONDARY
                    else -> null
                }
            }
        }
        binding.actvDetailsDepartment.setOnItemClickListener { _, _, position, _ ->
            if (_isEditMode) {
                val selectedDept = viewModel.departments.value.getOrNull(position)
                if (selectedDept != viewModel.editableSelectedDepartment.value) {
                    viewModel.editableSelectedDepartment.value = selectedDept
                    viewModel.editableSelectedPosition.value = null
                    binding.actvDetailsPosition.setText("", false)
                }
            }
        }
        binding.actvDetailsPosition.setOnItemClickListener { _, _, position, _ ->
            if (_isEditMode) {
                viewModel.editableSelectedPosition.value = viewModel.filteredPositions.value.getOrNull(position)
            }
        }

        // Слушатели для редактирования языков
        binding.actvDetailsLanguageSelect.setOnItemClickListener { _, _, position, _ ->
            if (_isEditMode) {
                selectedLanguageToAdd = viewModel.languages.value.getOrNull(position)
            }
        }
        binding.actvDetailsLevelSelect.setOnItemClickListener { _, _, position, _ ->
            if (_isEditMode) {
                val selectedResourceString = levelSelectAdapter?.getItem(position)
                if (selectedResourceString != null) {
                    selectedLevelToAdd = mapResourceStringToLevel(selectedResourceString)
                }
            }
        }
        binding.btnDetailsAddLanguage.setOnClickListener {
            if (_isEditMode && selectedLanguageToAdd != null) {
                val alreadyAdded = viewModel.editableLanguagesUi.value.any { it.language.id == selectedLanguageToAdd!!.id }
                if (alreadyAdded) {
                    Snackbar.make(binding.root, R.string.error_language_already_added, Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.addOrUpdateEditableLanguage(selectedLanguageToAdd!!, selectedLevelToAdd)
                    selectedLanguageToAdd = null
                    binding.actvDetailsLanguageSelect.setText("", false)
                }
            } else if (_isEditMode) {
                Snackbar.make(binding.root, R.string.error_select_language_and_level, Snackbar.LENGTH_SHORT).show()
            }
        }

        // Кнопки Принять/Отклонить
        binding.btnAccept.setOnClickListener {
            val rate = binding.etDetailsTariffRate.text.toString().toIntOrNull()
            val isValidRate = Validators.isValidTariffRate(rate)
            if (isValidRate == true) {
                binding.tilDetailsTariffRate.error = null
                viewModel.acceptApplication()
            } else {
                binding.tilDetailsTariffRate.error = getString(R.string.error_field_required) + " (${Constants.MIN_TARIFF_RATE}-${Constants.MAX_TARIFF_RATE})"
            }
        }
        binding.btnReject.setOnClickListener { showRejectConfirmationDialog() }
    }

    private fun showDatePicker() { // Код без изменений }
    }
    private fun showDeleteConfirmationDialog() { // Код без изменений }
    }
    private fun showRejectConfirmationDialog() { // Код без изменений }
    }

    // ИСПРАВЛЕНО: Управляет состоянием редактирования и обновляет UI/меню
    private fun setEditMode(enabled: Boolean) {
        Log.d(TAG, "Setting edit mode to: $enabled")
        if (_isEditMode == enabled) {
            Log.d(TAG, "Edit mode already set to $enabled, skipping.")
            return
        }
        _isEditMode = enabled
        onBackPressedCallback.isEnabled = enabled // Включаем/выключаем колбэк кнопки "Назад"
        updateUiForViewMode() // Применяем изменения к UI
        updateMenuVisibility() // Обновляем видимость кнопок меню
    }

    // Обновление UI в зависимости от режима
    private fun updateUiForViewMode() {
        if (_binding == null) return

        val enableFields = _isEditMode
        Log.d(TAG, "Updating UI for mode: ${if(enableFields) "EDIT" else "VIEW"}")

        binding.etDetailsLastName.isEnabled = enableFields
        binding.etDetailsFirstName.isEnabled = enableFields
        binding.etDetailsDob.isEnabled = enableFields
        binding.tilDetailsDob.isEndIconVisible = enableFields

        for (i in 0 until binding.rgDetailsGender.childCount) {
            binding.rgDetailsGender.getChildAt(i).isEnabled = enableFields
        }
        for (i in 0 until binding.rgDetailsEducation.childCount) {
            binding.rgDetailsEducation.getChildAt(i).isEnabled = enableFields
        }

        binding.etDetailsTotalExp.isEnabled = enableFields
        binding.etDetailsAcademicExp.isEnabled = enableFields
        binding.actvDetailsDepartment.isEnabled = enableFields
        binding.tilDetailsDepartment.isEnabled = enableFields
        binding.actvDetailsPosition.isEnabled = enableFields
        binding.tilDetailsPosition.isEnabled = enableFields

        val currentState = viewModel.uiState.value
        val employee = (currentState as? EmployeeDetailsUiState.Success)?.employee
        val isAccepted = employee?.status == EmployeeConstants.STATUS_ACCEPTED
        val isNew = employee?.status == EmployeeConstants.STATUS_NEW

        binding.tilDetailsTariffRate.isVisible = isAccepted || isNew
        binding.etDetailsTariffRate.isEnabled = _isEditMode || (!_isEditMode && isNew)

        val canEditLanguages = _isEditMode && (sessionManager.getCurrentUserRole() == UserRole.ADMIN)
        binding.languageEditGroup.isVisible = canEditLanguages
        binding.rvDetailsLanguages.isVisible = !canEditLanguages

        binding.buttonLayout.isVisible = !_isEditMode && isNew
    }

    // Вспомогательные функции для маппинга уровней
    private fun mapLevelToResourceString(levelConstant: String): String {
        return when (levelConstant) {
            ProficiencyLevel.ELEMENTARY -> getString(R.string.level_elementary)
            ProficiencyLevel.INTERMEDIATE -> getString(R.string.level_intermediate)
            ProficiencyLevel.ADVANCED -> getString(R.string.level_advanced)
            ProficiencyLevel.FLUENT -> getString(R.string.level_fluent)
            ProficiencyLevel.NATIVE -> getString(R.string.level_native)
            else -> levelConstant
        }
    }
    private fun mapResourceStringToLevel(levelResourceString: String): String {
        return when (levelResourceString) {
            getString(R.string.level_elementary) -> ProficiencyLevel.ELEMENTARY
            getString(R.string.level_intermediate) -> ProficiencyLevel.INTERMEDIATE
            getString(R.string.level_advanced) -> ProficiencyLevel.ADVANCED
            getString(R.string.level_fluent) -> ProficiencyLevel.FLUENT
            getString(R.string.level_native) -> ProficiencyLevel.NATIVE
            else -> ProficiencyLevel.ELEMENTARY
        }
    }

    private fun showSnackbar(message: String) {
        if (view != null && _binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        } else {
            Log.w(TAG, "Snackbar requested but view or binding is null: $message")
        }
    }

    // Диалог подтверждения выхода из режима редактирования (отмена изменений)
    private fun showExitConfirmationDialog(onConfirmExit: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Отменить изменения?")
            .setMessage("Вы уверены, что хотите выйти без сохранения изменений?")
            .setNegativeButton(R.string.cancel_button, null)
            .setPositiveButton("Выйти без сохранения") { _, _ ->
                onConfirmExit()
            }
            .show()
    }

    // Восстанавливает поля из ViewModel перед выходом без сохранения
    private fun restoreFieldsBeforeExit() {
        Log.d(TAG, "Restoring fields before exiting edit mode")
        viewModel.uiState.value.let { state ->
            if (state is EmployeeDetailsUiState.Success) {
                viewModel.initializeEditableFields(state.employee, state.languages, state.department, state.position)
                displayEmployeeData(state)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // НЕ СНИМАЕМ ActionBar здесь, пусть управляется Activity/NavComponent
        // (activity as? AppCompatActivity)?.setSupportActionBar(null)
        binding.rvDetailsLanguages.adapter = null
        binding.rvDetailsSelectedLanguages.adapter = null
        binding.actvDetailsDepartment.setAdapter(null)
        binding.actvDetailsPosition.setAdapter(null)
        binding.actvDetailsLanguageSelect.setAdapter(null)
        binding.actvDetailsLevelSelect.setAdapter(null)
        departmentAdapter = null
        positionAdapter = null
        languageSelectAdapter = null
        levelSelectAdapter = null
        _binding = null
    }
}