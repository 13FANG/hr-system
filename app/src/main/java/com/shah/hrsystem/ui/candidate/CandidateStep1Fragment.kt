package com.shah.hrsystem.ui.candidate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.databinding.FragmentCandidateStep1Binding
import com.shah.hrsystem.util.DateUtils
import com.shah.hrsystem.util.Validators
import com.shah.hrsystem.viewmodel.CandidateUiState
import com.shah.hrsystem.viewmodel.CandidateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@AndroidEntryPoint
class CandidateStep1Fragment : Fragment() {

    private var _binding: FragmentCandidateStep1Binding? = null
    private val binding get() = _binding!!

    // Используем activityViewModels для общего CandidateViewModel
    private val viewModel: CandidateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidateStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreStateFromViewModel() // Восстанавливаем данные при возврате на экран
        setupListeners()
        observeValidationErrors() // Наблюдение за ошибками валидации из VM
    }

    // Восстановление значений полей из ViewModel
    private fun restoreStateFromViewModel() {
        binding.etLastName.setText(viewModel.lastName.value)
        binding.etFirstName.setText(viewModel.firstName.value)
        binding.etDob.setText(viewModel.dateOfBirthString.value)

        when (viewModel.gender.value) {
            EmployeeConstants.GENDER_MALE -> binding.rbMale.isChecked = true
            EmployeeConstants.GENDER_FEMALE -> binding.rbFemale.isChecked = true
            else -> binding.rgGender.clearCheck()
        }
        when (viewModel.educationLevel.value) {
            EmployeeConstants.EDU_HIGHER -> binding.rbEduHigher.isChecked = true
            EmployeeConstants.EDU_SECONDARY -> binding.rbEduSecondary.isChecked = true
            else -> binding.rgEducation.clearCheck()
        }
        binding.etTotalExperience.setText(viewModel.totalExperience.value?.toString() ?: "")
        binding.etAcademicExperience.setText(viewModel.academicExperience.value?.toString() ?: "")
    }


    private fun setupListeners() {
        // Обновление ViewModel при изменении полей
        binding.etLastName.addTextChangedListener { viewModel.updateLastName(it.toString()) }
        binding.etFirstName.addTextChangedListener { viewModel.updateFirstName(it.toString()) }
        binding.etTotalExperience.addTextChangedListener { viewModel.updateTotalExperience(it.toString().toIntOrNull()) }
        binding.etAcademicExperience.addTextChangedListener { viewModel.updateAcademicExperience(it.toString().toIntOrNull()) }

        // Выбор даты рождения
        binding.etDob.setOnClickListener { showDatePicker() }
        binding.tilDob.setEndIconOnClickListener { showDatePicker() } // На иконку календаря

        // Выбор пола
        binding.rgGender.setOnCheckedChangeListener { _, checkedId ->
            val gender = when (checkedId) {
                R.id.rb_male -> EmployeeConstants.GENDER_MALE
                R.id.rb_female -> EmployeeConstants.GENDER_FEMALE
                else -> null
            }
            viewModel.updateGender(gender ?: "")
        }

        // Выбор образования
        binding.rgEducation.setOnCheckedChangeListener { _, checkedId ->
            val education = when (checkedId) {
                R.id.rb_edu_higher -> EmployeeConstants.EDU_HIGHER
                R.id.rb_edu_secondary -> EmployeeConstants.EDU_SECONDARY
                else -> null
            }
            viewModel.updateEducation(education ?: "")
        }

        // Кнопка "Далее"
        binding.btnNextStep1.setOnClickListener {
            // Здесь можно добавить простую проверку на заполненность перед переходом,
            // но основная валидация будет перед отправкой на последнем шаге.
            if (validateStep1Fields()) {
                findNavController().navigate(R.id.action_candidateStep1Fragment_to_candidateStep2Fragment)
            }
        }
    }

    private fun showDatePicker() {
        val selectedDateMillis = viewModel.dateOfBirth.value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds() // Сегодняшняя дата по умолчанию

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date_desc))
            .setSelection(selectedDateMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // Конвертируем Long в LocalDate и обновляем ViewModel
            val selectedLocalDate = Instant.ofEpochMilli(selection).atZone(ZoneId.systemDefault()).toLocalDate()
            val formattedDate = DateUtils.formatLocalDateToIso(selectedLocalDate)?.let { DateUtils.formatIsoDateForDisplay(it) } ?: ""
            // viewModel.dateOfBirth.value = selectedLocalDate // Обновляем LocalDate
            viewModel.updateDobFromString(formattedDate) // Обновляем и строку и LocalDate
            binding.etDob.setText(formattedDate) // Обновляем поле ввода
        }

        // Показываем DatePicker, избегая ошибки state loss
        if (!parentFragmentManager.isStateSaved) {
            datePicker.show(parentFragmentManager, "DATE_PICKER_TAG")
        }
    }

    // Простая валидация полей текущего шага (для UX)
    private fun validateStep1Fields(): Boolean {
        var isValid = true
        // Сбрасываем предыдущие ошибки
        binding.tilLastName.error = null
        binding.tilFirstName.error = null
        binding.tilDob.error = null
        binding.tilTotalExperience.error = null
        binding.tilAcademicExperience.error = null

        if (binding.etLastName.text.isNullOrBlank()) {
            binding.tilLastName.error = getString(R.string.error_field_required)
            isValid = false
        }
        if (binding.etFirstName.text.isNullOrBlank()) {
            binding.tilFirstName.error = getString(R.string.error_field_required)
            isValid = false
        }
        val dob = viewModel.dateOfBirth.value
        if (dob == null) {
            binding.tilDob.error = getString(R.string.error_field_required)
            isValid = false
        } else if (!Validators.isValidDateNotInFuture(dob)!!) {
            binding.tilDob.error = getString(R.string.error_date_in_future)
            isValid = false
        }

        if (binding.rgGender.checkedRadioButtonId == -1) {
            // Можно показать Toast или другую индикацию для RadioGroup
            Snackbar.make(binding.root, R.string.error_field_required, Snackbar.LENGTH_SHORT).show()
            isValid = false
        }
        if (binding.rgEducation.checkedRadioButtonId == -1) {
            Snackbar.make(binding.root, R.string.error_field_required, Snackbar.LENGTH_SHORT).show()
            isValid = false
        }

        val totalExp = viewModel.totalExperience.value
        if (totalExp == null) {
            binding.tilTotalExperience.error = getString(R.string.error_field_required)
            isValid = false
        } else if (totalExp < 0) {
            binding.tilTotalExperience.error = getString(R.string.error_exp_negative)
            isValid = false
        }

        val academicExp = viewModel.academicExperience.value
        if (academicExp == null) {
            binding.tilAcademicExperience.error = getString(R.string.error_field_required)
            isValid = false
        } else if (academicExp < 0) {
            binding.tilAcademicExperience.error = getString(R.string.error_exp_negative)
            isValid = false
        } else if (totalExp != null && academicExp > totalExp) {
            binding.tilAcademicExperience.error = getString(R.string.error_academic_exp_greater)
            isValid = false
        }

        return isValid
    }

    // Наблюдение за общими ошибками валидации из ViewModel (которые проверяются при отправке)
    // Здесь это не так критично, но может быть полезно для отображения ошибок, не проверенных в validateStep1Fields()
    private fun observeValidationErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is CandidateUiState.ValidationError) {
                        state.errors.forEach { (field, message) ->
                            when (field) {
                                "lastName" -> binding.tilLastName.error = message
                                "firstName" -> binding.tilFirstName.error = message
                                "dateOfBirth" -> binding.tilDob.error = message
                                "totalExperience" -> binding.tilTotalExperience.error = message
                                "academicExperience" -> binding.tilAcademicExperience.error = message
                                // Ошибки для gender, education, experience обрабатываются на preview шаге
                            }
                        }
                        // Сбрасываем состояние ошибки в VM после отображения
                        // viewModel.clearError() // или пусть VM сам сбрасывает в Idle
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}