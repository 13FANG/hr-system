package com.shah.hrsystem.ui.main.admin.dictionary_management.dialog

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup // Импортирован
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope // Используем lifecycleScope диалога
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.data.db.entity.Position
import com.shah.hrsystem.databinding.DialogDictionaryAddEditBinding
import com.shah.hrsystem.viewmodel.AdminDictionaryViewModel
import com.shah.hrsystem.viewmodel.DictionaryOperationState
import com.shah.hrsystem.viewmodel.DictionaryType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.Serializable

// Вспомогательные функции для получения Parcelable/Serializable
inline fun <reified T : Serializable> Bundle?.getSerializableCompat(key: String): T? {
    if (this == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(key) as? T
    }
}

inline fun <reified T : Parcelable> Bundle?.getParcelableCompat(key: String): T? {
    if (this == null) return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
}


@AndroidEntryPoint
class DictionaryAddEditDialog : DialogFragment() {

    private val viewModel: AdminDictionaryViewModel by viewModels({ requireParentFragment() })
    private var binding: DialogDictionaryAddEditBinding? = null

    private var dictionaryType: DictionaryType? = null
    private var editingItem: Parcelable? = null

    private var departmentAdapter: ArrayAdapter<String>? = null
    private val departmentsList = mutableListOf<Department>()
    private var selectedDepartmentForPosition: Department? = null

    private val TAG = "DictAddEditDialog"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Не используется напрямую для настройки View диалога
        return null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog START")
        binding = DialogDictionaryAddEditBinding.inflate(LayoutInflater.from(context))
        Log.d(TAG, "Binding inflated")

        dictionaryType = arguments.getSerializableCompat(ARG_TYPE)
        editingItem = arguments.getParcelableCompat(ARG_ITEM)
        Log.i(TAG, "Arguments received: Type=$dictionaryType, Item=$editingItem")

        if (dictionaryType == null) {
            Log.e(TAG, "DictionaryType is null in arguments!")
            // Возвращаем диалог с ошибкой
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error_field_required) // Пример
                .setMessage("Не удалось определить тип справочника.")
                .setPositiveButton(android.R.string.ok) { _, _ -> dismiss() }
                .create()
        }

        try {
            setupAdapters()
            setupViewsAndListeners()
            observeViewModel() // Вызываем observeViewModel здесь
        } catch (e: Exception) {
            Log.e(TAG, "Error during setup in onCreateDialog", e)
            // Возвращаем диалог с ошибкой
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ошибка инициализации")
                .setMessage("Произошла ошибка при открытии диалога: ${e.message}")
                .setPositiveButton(android.R.string.ok) { _, _ -> dismiss() }
                .create()
        }

        val title = getDialogTitle()
        val positiveButtonText = if (editingItem == null) getString(R.string.add_button) else getString(R.string.save_button)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding?.root)
            .setPositiveButton(positiveButtonText, null)
            .setNegativeButton(R.string.cancel_button, null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(Dialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                Log.d(TAG, "Positive button clicked.")
                if (handleSave()) {
                    Log.d(TAG, "handleSave returned true, dialog dismissed.")
                    // dismiss() вызывается внутри handleSave, если успешно
                } else {
                    Log.d(TAG, "handleSave returned false, dialog remains open.")
                }
            }
        }
        Log.d(TAG, "onCreateDialog END")
        return dialog
    }

    private fun setupAdapters() {
        Log.d(TAG, "setupAdapters")
        if (binding == null || dictionaryType != DictionaryType.POSITIONS) {
            Log.d(TAG, "Skipping department adapter setup.")
            return
        }
        departmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        (binding!!.tilPositionDepartment.editText as? AutoCompleteTextView)?.setAdapter(departmentAdapter)
        Log.d(TAG, "Department adapter created and set.")
    }

    private fun setupViewsAndListeners() {
        Log.d(TAG, "setupViewsAndListeners START. Type: $dictionaryType")
        if (binding == null) {
            Log.e(TAG, "setupViewsAndListeners: Binding is null!")
            return
        }

        binding!!.layoutPositionFields.isVisible = dictionaryType == DictionaryType.POSITIONS
        binding!!.etDictItemName.addTextChangedListener { binding?.tilDictItemName?.error = null }

        when (dictionaryType) {
            DictionaryType.DEPARTMENTS -> {
                binding!!.tilDictItemName.hint = getString(R.string.hint_department_name)
                val dept = editingItem as? Department
                binding!!.etDictItemName.setText(dept?.name ?: "")
                Log.d(TAG, "Setup for Department: Name='${dept?.name ?: ""}'")
            }
            DictionaryType.POSITIONS -> {
                binding!!.tilDictItemName.hint = getString(R.string.hint_position_name)
                setupPositionFieldsAndListeners()
            }
            DictionaryType.LANGUAGES -> {
                binding!!.tilDictItemName.hint = getString(R.string.hint_language_name)
                val lang = editingItem as? Language
                binding!!.etDictItemName.setText(lang?.name ?: "")
                Log.d(TAG, "Setup for Language: Name='${lang?.name ?: ""}'")
            }
            null -> { Log.e(TAG, "setupViewsAndListeners: dictionaryType is null") }
        }
        Log.d(TAG, "setupViewsAndListeners END")
    }

    private fun setupPositionFieldsAndListeners() {
        Log.d(TAG, "setupPositionFieldsAndListeners START. EditingItem: $editingItem")
        if (binding == null) return
        val pos = editingItem as? Position
        if (pos != null) {
            binding!!.etDictItemName.setText(pos.name)
            binding!!.etPositionMaxAllowed.setText(pos.maxAllowed.toString())
            binding!!.switchPositionRequiresHigherEdu.isChecked = pos.requiresHigherEducationBool()
            binding!!.switchPositionIsAssistant.isChecked = pos.isAssistantBool()
            val initialDept = viewModel.departments.value.find { it.id == pos.departmentId }
            selectedDepartmentForPosition = initialDept
            binding!!.actvPositionDepartment.setText(initialDept?.name ?: "", false)
            Log.i(TAG, "Setup editing Position: Name='${pos.name}', Max='${pos.maxAllowed}', DeptId='${pos.departmentId}', FoundDept='${initialDept?.name}'")
        } else {
            selectedDepartmentForPosition = null
            binding!!.etPositionMaxAllowed.setText("1")
            binding!!.switchPositionRequiresHigherEdu.isChecked = false
            binding!!.switchPositionIsAssistant.isChecked = false
            binding!!.actvPositionDepartment.setText("", false)
            Log.i(TAG, "Setup adding Position.")
        }

        binding!!.etPositionMaxAllowed.addTextChangedListener { binding?.tilPositionMaxAllowed?.error = null }
        (binding!!.tilPositionDepartment.editText as? AutoCompleteTextView)?.addTextChangedListener { binding?.tilPositionDepartment?.error = null }

        binding!!.actvPositionDepartment.setOnItemClickListener { adapterView, _, position, _ ->
            val selectedName = adapterView.getItemAtPosition(position) as? String
            selectedDepartmentForPosition = departmentsList.find { it.name == selectedName }
            binding!!.tilPositionDepartment.error = null
            Log.i(TAG, "Department selected via listener: ${selectedDepartmentForPosition?.name}")
        }
        Log.d(TAG, "setupPositionFieldsAndListeners END")
    }

    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel")
        // --- ИСПРАВЛЕНО: Используем lifecycleScope вместо viewLifecycleOwner.lifecycleScope ---
        if (dictionaryType == DictionaryType.POSITIONS) {
            Log.d(TAG, "Observing departments for Position dialog using lifecycleScope")
            lifecycleScope.launch { // Используем lifecycleScope диалог-фрагмента
                viewModel.departments.filterNotNull().collectLatest { depts ->
                    Log.d(TAG, "Departments observer received ${depts.size} items.")
                    departmentsList.clear()
                    departmentsList.addAll(depts)
                    val deptNames = depts.map { it.name }

                    departmentAdapter?.let { adapter ->
                        adapter.clear()
                        adapter.addAll(deptNames)
                        Log.d(TAG, "Department adapter updated with new data.")
                    } ?: Log.w(TAG, "Department adapter is null during data update.")

                    val pos = editingItem as? Position
                    if (pos != null && selectedDepartmentForPosition == null) {
                        Log.d(TAG, "Attempting to set department text after departments loaded.")
                        val initialDept = depts.find { it.id == pos.departmentId }
                        if (initialDept != null) {
                            selectedDepartmentForPosition = initialDept
                            // Обновление UI лучше делать в основном потоке и после отрисовки
                            binding?.actvPositionDepartment?.post {
                                binding?.actvPositionDepartment?.setText(initialDept.name, false)
                                Log.i(TAG, "Set department text after departments loaded: '${initialDept.name}'")
                            }
                        } else {
                            Log.w(TAG, "Department with ID ${pos.departmentId} not found in loaded list.")
                        }
                    } else if (pos != null && selectedDepartmentForPosition != null) {
                        binding?.actvPositionDepartment?.post {
                            if (binding?.actvPositionDepartment?.text.toString() != selectedDepartmentForPosition!!.name) {
                                binding?.actvPositionDepartment?.setText(selectedDepartmentForPosition!!.name, false)
                                Log.d(TAG,"Corrected department text after departments loaded: ${selectedDepartmentForPosition!!.name}")
                            }
                        }
                    }
                }
            }
        }
        lifecycleScope.launch { // Используем lifecycleScope диалог-фрагмента
            viewModel.operationState.collectLatest { state ->
                Log.d(TAG,"Received operation state: ${state::class.java.simpleName}")
                if (state is DictionaryOperationState.Error) {
                    showErrorSnackbar(state.message)
                    viewModel.clearOperationState()
                }
            }
        }
        // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
    }


    private fun getDialogTitle(): String {
        val actionResId = if (editingItem == null) R.string.dialog_add_prefix else R.string.dialog_edit_prefix
        val typeResId = when (dictionaryType) {
            DictionaryType.DEPARTMENTS -> R.string.dialog_department_suffix
            DictionaryType.POSITIONS -> R.string.dialog_position_suffix
            DictionaryType.LANGUAGES -> R.string.dialog_language_suffix
            null -> R.string.dialog_item_suffix
        }
        return getString(actionResId) + " " + getString(typeResId)
    }

    private fun handleSave(): Boolean {
        Log.d(TAG, "handleSave START")
        if (binding == null) {
            Log.e(TAG, "handleSave: Binding is null!")
            return false
        }
        val name = binding!!.etDictItemName.text.toString().trim()
        var isValid = true

        if (name.isBlank()) {
            binding!!.tilDictItemName.error = getString(R.string.error_field_required)
            isValid = false
        } else {
            binding!!.tilDictItemName.error = null
        }

        var positionToSave: Position? = null
        if (dictionaryType == DictionaryType.POSITIONS) {
            if (selectedDepartmentForPosition == null) {
                binding!!.tilPositionDepartment.error = getString(R.string.error_department_required)
                isValid = false
            } else {
                binding!!.tilPositionDepartment.error = null
            }

            val maxAllowedStr = binding!!.etPositionMaxAllowed.text.toString()
            val maxAllowed = maxAllowedStr.toIntOrNull()
            if (maxAllowed == null || maxAllowed < 0) {
                binding!!.tilPositionMaxAllowed.error = getString(R.string.error_invalid_max_allowed)
                isValid = false
            } else {
                binding!!.tilPositionMaxAllowed.error = null
            }

            if (isValid && selectedDepartmentForPosition != null && maxAllowed != null) {
                val requiresHigherEdu = binding!!.switchPositionRequiresHigherEdu.isChecked
                val isAssistant = binding!!.switchPositionIsAssistant.isChecked
                val finalDepartmentId = selectedDepartmentForPosition!!.id

                positionToSave = (editingItem as? Position)?.copy(
                    name = name,
                    departmentId = finalDepartmentId,
                    maxAllowed = maxAllowed,
                    requiresHigherEducation = if (requiresHigherEdu) 1 else 0,
                    isAssistant = if (isAssistant) 1 else 0
                ) ?: Position(
                    name = name,
                    departmentId = finalDepartmentId,
                    maxAllowed = maxAllowed,
                    requiresHigherEducation = if (requiresHigherEdu) 1 else 0,
                    isAssistant = if (isAssistant) 1 else 0
                )
                Log.d(TAG, "Position object prepared for save: $positionToSave")
            } else if (isValid) {
                isValid = false
                Log.w(TAG, "handleSave: Position validation inconsistency. isValid=$isValid, dept=$selectedDepartmentForPosition, max=$maxAllowed")
            }
        }

        if (!isValid) {
            Log.w(TAG, "handleSave: Validation failed.")
            return false
        }

        Log.i(TAG, "Validation passed. Calling ViewModel to save.")
        when (dictionaryType) {
            DictionaryType.DEPARTMENTS -> {
                val dept = editingItem as? Department
                if (dept != null) {
                    Log.d(TAG, "Calling updateDepartment: ${dept.copy(name = name)}")
                    viewModel.updateDepartment(dept.copy(name = name))
                } else {
                    Log.d(TAG, "Calling addDepartment: $name")
                    viewModel.addDepartment(name)
                }
            }
            DictionaryType.POSITIONS -> {
                if (positionToSave != null) {
                    if (editingItem == null) {
                        Log.d(TAG, "Calling addPosition: $positionToSave")
                        viewModel.addPosition(positionToSave)
                    } else {
                        Log.d(TAG, "Calling updatePosition: $positionToSave")
                        viewModel.updatePosition(positionToSave)
                    }
                } else {
                    Log.e(TAG, "handleSave: positionToSave is null after validation!")
                    showErrorSnackbar("Внутренняя ошибка сохранения должности")
                    return false
                }
            }
            DictionaryType.LANGUAGES -> {
                val lang = editingItem as? Language
                if (lang != null) {
                    Log.d(TAG, "Calling updateLanguage: ${lang.copy(name = name)}")
                    viewModel.updateLanguage(lang.copy(name = name))
                } else {
                    Log.d(TAG, "Calling addLanguage: $name")
                    viewModel.addLanguage(name)
                }
            }
            null -> { Log.e(TAG, "handleSave called with null dictionaryType"); return false }
        }
        dismiss()
        Log.d(TAG, "handleSave END - Success")
        return true
    }


    private fun showErrorSnackbar(message: String) {
        // Показываем Snackbar в родительском фрагменте, если возможно
        parentFragment?.view?.let { parentView ->
            Snackbar.make(parentView, message, Snackbar.LENGTH_LONG).show()
        } ?: Log.e(TAG, "Cannot show Snackbar, parentFragment view is null")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Отключаем адаптер, чтобы избежать утечек контекста
        (binding?.tilPositionDepartment?.editText as? AutoCompleteTextView)?.setAdapter(null)
        departmentAdapter = null
        binding = null
        Log.d(TAG, "onDestroyView")
    }

    companion object {
        const val TAG = "DictionaryAddEditDialog"
        private const val ARG_TYPE = "dictionary_type"
        private const val ARG_ITEM = "item_to_edit"

        fun newInstance(type: DictionaryType, item: Parcelable? = null): DictionaryAddEditDialog {
            Log.d(TAG, "newInstance called. Type: $type, Item: $item")
            return DictionaryAddEditDialog().apply {
                arguments = bundleOf(
                    ARG_TYPE to type,
                    ARG_ITEM to item
                )
            }
        }

        fun show(fragmentManager: FragmentManager, type: DictionaryType, item: Any? = null) {
            Log.d(TAG, "show() called. Type: $type, Item: $item")
            var parcelableItem: Parcelable? = null

            if (item != null) {
                // Проверяем, что объект является Parcelable И одним из наших типов сущностей
                if (item is Parcelable && (item is Department || item is Position || item is Language)) {
                    parcelableItem = item
                    Log.d(TAG, "show(): Item is valid Parcelable: ${item::class.java.simpleName}")
                } else {
                    Log.e(TAG, "show(): Item provided is not a valid Parcelable dictionary item: ${item::class.java.simpleName}")
                    // Можно показать ошибку пользователю или просто не открывать диалог
                    // В данном случае, просто логируем ошибку и не показываем диалог
                    return // Прерываем выполнение, чтобы избежать ошибки ClassCastException
                }
            } else {
                Log.d(TAG, "show(): Item is null, creating Add dialog.")
            }

            try {
                newInstance(type, parcelableItem).show(fragmentManager, TAG)
                Log.d(TAG,"Dialog instance created and show() called.")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dialog in companion object", e)
                // Можно показать Snackbar с ошибкой во fragmentManager.fragments.lastOrNull()?.view, если это безопасно
            }
        }
    }
}