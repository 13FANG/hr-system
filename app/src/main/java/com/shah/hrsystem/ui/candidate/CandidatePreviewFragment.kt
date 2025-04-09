package com.shah.hrsystem.ui.candidate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.databinding.FragmentCandidatePreviewBinding
import com.shah.hrsystem.util.DateUtils
import com.shah.hrsystem.viewmodel.CandidateUiState
import com.shah.hrsystem.viewmodel.CandidateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CandidatePreviewFragment : Fragment() {

    private var _binding: FragmentCandidatePreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CandidateViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidatePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayCandidateData()
        setupListeners()
        observeViewModelState()
    }

    // Отображение данных из ViewModel
    private fun displayCandidateData() {
        binding.tvFullNameValue.text = "${viewModel.lastName.value} ${viewModel.firstName.value}"
        binding.tvDobValue.text = viewModel.dateOfBirthString.value
        binding.tvGenderPreviewValue.text = viewModel.gender.value ?: "-"
        binding.tvEducationPreviewValue.text = viewModel.educationLevel.value ?: "-"

        val totalExp = viewModel.totalExperience.value
        val academicExp = viewModel.academicExperience.value
        binding.tvExperienceValue.text = "Общий: ${totalExp ?: 0} лет, Академ.: ${academicExp ?: 0} лет"

        binding.tvDepartmentPreviewValue.text = viewModel.selectedDepartment.value?.name ?: "-"
        binding.tvPositionPreviewValue.text = viewModel.selectedPosition.value?.name ?: "-"

        val languagesText = viewModel.selectedLanguagesUi.value
            .joinToString(", ") { "${it.language.name} (${it.level})" }
            .ifEmpty { getString(R.string.no_languages_selected) }
        binding.tvLanguagesPreviewValue.text = languagesText
    }

    private fun setupListeners() {
        binding.btnSubmitApplication.setOnClickListener {
            viewModel.validateAndSaveApplication() // Вызываем метод VM для валидации и сохранения
        }
    }

    // Наблюдение за состоянием ViewModel (загрузка, успех, ошибка)
    private fun observeViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val isLoading = state is CandidateUiState.Saving
                    binding.progressBarPreview.isVisible = isLoading
                    binding.btnSubmitApplication.isEnabled = !isLoading

                    when (state) {
                        is CandidateUiState.SaveSuccess -> {
                            Snackbar.make(binding.root, R.string.submit_success_message, Snackbar.LENGTH_LONG).show()
                            // Переходим на экран входа после задержки или сразу
                            findNavController().navigate(R.id.action_candidatePreviewFragment_to_loginFragment)
                            // VM сам сбрасывает состояние и данные
                        }
                        is CandidateUiState.Error -> {
                            showErrorSnackbar(state.message)
                            viewModel.clearError() // Сбрасываем состояние ошибки в VM
                        }
                        is CandidateUiState.ValidationError -> {
                            // Показываем первую ошибку валидации
                            val firstError = state.errors.entries.firstOrNull()?.value ?: getString(R.string.submit_error_message)
                            showErrorSnackbar("${getString(R.string.validation_error_prefix)}: $firstError")
                            viewModel.clearError() // Сбрасываем состояние ошибки в VM
                            // Здесь можно было бы навигироваться назад к шагу с ошибкой, но это усложняет логику
                        }
                        else -> {
                            // Idle, LoadingDictionaries, DictionariesLoaded, Saving
                        }
                    }
                }
            }
        }
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}