package com.shah.hrsystem.ui.candidate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.databinding.FragmentCandidateStep2Binding
import com.shah.hrsystem.viewmodel.CandidateUiState
import com.shah.hrsystem.viewmodel.CandidateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CandidateStep2Fragment : Fragment() {

    private var _binding: FragmentCandidateStep2Binding? = null
    private val binding get() = _binding!!

    private val viewModel: CandidateViewModel by activityViewModels()

    // Адаптеры для выпадающих списков
    private var departmentAdapter: ArrayAdapter<String>? = null
    private var positionAdapter: ArrayAdapter<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidateStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdowns()
        observeViewModel()
        setupListeners()
    }

    private fun setupDropdowns() {
        // Адаптер для отделов (изначально пустой, заполнится при получении данных)
        departmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.actvDepartment.setAdapter(departmentAdapter)

        // Адаптер для должностей (изначально пустой)
        positionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.actvPosition.setAdapter(positionAdapter)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за списком отделов
                viewModel.departments.collectLatest { departments ->
                    departmentAdapter?.clear()
                    departmentAdapter?.addAll(departments.map { it.name })
                    departmentAdapter?.notifyDataSetChanged()
                    // Восстанавливаем выбор, если он был
                    binding.actvDepartment.setText(viewModel.selectedDepartment.value?.name ?: "", false)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за отфильтрованным списком должностей
                viewModel.positions.collectLatest { positions ->
                    positionAdapter?.clear()
                    positionAdapter?.addAll(positions.map { it.name })
                    positionAdapter?.notifyDataSetChanged()
                    binding.tilPosition.isEnabled = positions.isNotEmpty() // Включаем/выключаем выбор должности
                    // Восстанавливаем выбор, если он был и список не пуст
                    if (positions.isNotEmpty()) {
                        binding.actvPosition.setText(viewModel.selectedPosition.value?.name ?: "", false)
                    } else {
                        binding.actvPosition.setText("", false) // Очищаем, если должностей нет
                    }
                    // Скрываем прогресс бар, когда должности загружены/отфильтрованы
                    binding.progressBarPositions.isVisible = false
                }
            }
        }
        // Наблюдаем за общим состоянием (показ прогресса)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // binding.progressBarPositions.isVisible = state is CandidateUiState.LoadingDictionaries // Показываем, пока грузятся все справочники
                    if (state is CandidateUiState.Error) {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        // Выбор отдела
        binding.actvDepartment.setOnItemClickListener { adapterView, _, position, _ ->
            val selectedName = adapterView.getItemAtPosition(position) as? String
            val selectedDept = viewModel.departments.value.find { it.name == selectedName }
            if (selectedDept != viewModel.selectedDepartment.value) {
                viewModel.selectDepartment(selectedDept)
                binding.actvPosition.setText("", false) // Очищаем поле должности при смене отдела
                binding.progressBarPositions.isVisible = true // Показываем прогресс на время фильтрации
            }
            binding.tilDepartment.error = null // Сбрасываем ошибку
        }

        // Выбор должности
        binding.actvPosition.setOnItemClickListener { adapterView, _, position, _ ->
            val selectedName = adapterView.getItemAtPosition(position) as? String
            val selectedPos = viewModel.positions.value.find { it.name == selectedName }
            viewModel.selectPosition(selectedPos)
            binding.tilPosition.error = null // Сбрасываем ошибку
        }

        // Кнопка "Далее"
        binding.btnNextStep2.setOnClickListener {
            // Проверка выбора отдела и должности
            var isValid = true
            if (viewModel.selectedDepartment.value == null) {
                binding.tilDepartment.error = getString(R.string.error_department_required)
                isValid = false
            }
            if (viewModel.selectedPosition.value == null) {
                binding.tilPosition.error = getString(R.string.error_position_required)
                isValid = false
            }

            if (isValid) {
                findNavController().navigate(R.id.action_candidateStep2Fragment_to_candidateStep3Fragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем адаптеры, чтобы избежать утечек контекста
        binding.actvDepartment.setAdapter(null)
        binding.actvPosition.setAdapter(null)
        departmentAdapter = null
        positionAdapter = null
        _binding = null
    }
}