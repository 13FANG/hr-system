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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.data.db.entity.ProficiencyLevel
import com.shah.hrsystem.databinding.FragmentCandidateStep3Binding
import com.shah.hrsystem.ui.candidate.adapter.CandidateLanguageAdapter
import com.shah.hrsystem.viewmodel.CandidateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CandidateStep3Fragment : Fragment() {

    private var _binding: FragmentCandidateStep3Binding? = null
    private val binding get() = _binding!!

    private val viewModel: CandidateViewModel by activityViewModels()
    private lateinit var languageListAdapter: CandidateLanguageAdapter

    // Адаптеры для выпадающих списков
    private var languageSelectAdapter: ArrayAdapter<String>? = null
    private var levelSelectAdapter: ArrayAdapter<String>? = null

    // Для хранения текущего выбора в AutoCompleteTextViews
    private var selectedLanguageToAdd: Language? = null
    private var selectedLevelToAdd: String = ProficiencyLevel.ELEMENTARY // Уровень по умолчанию

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidateStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupDropdowns()
        observeViewModel()
        setupListeners()
    }

    private fun setupRecyclerView() {
        languageListAdapter = CandidateLanguageAdapter { languageToRemove ->
            viewModel.removeLanguage(languageToRemove)
        }
        binding.rvSelectedLanguages.apply {
            adapter = languageListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupDropdowns() {
        // Адаптер для выбора языка
        languageSelectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.actvLanguageSelect.setAdapter(languageSelectAdapter)

        // Адаптер для выбора уровня
        val levels = ProficiencyLevel.ALL.map { mapLevelToResourceString(it) } // Получаем строки уровней из ресурсов
        levelSelectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, levels)
        binding.actvLevelSelect.setAdapter(levelSelectAdapter)
        // Устанавливаем уровень по умолчанию в поле ввода
        binding.actvLevelSelect.setText(mapLevelToResourceString(selectedLevelToAdd), false)
    }

    // Вспомогательная функция для получения строки уровня из ресурсов
    private fun mapLevelToResourceString(levelConstant: String): String {
        return when (levelConstant) {
            ProficiencyLevel.ELEMENTARY -> getString(R.string.level_elementary)
            ProficiencyLevel.INTERMEDIATE -> getString(R.string.level_intermediate)
            ProficiencyLevel.ADVANCED -> getString(R.string.level_advanced)
            ProficiencyLevel.FLUENT -> getString(R.string.level_fluent)
            ProficiencyLevel.NATIVE -> getString(R.string.level_native)
            else -> levelConstant // Возвращаем как есть, если нет соответствия
        }
    }
    // Вспомогательная функция для получения константы уровня из строки ресурса
    private fun mapResourceStringToLevel(levelResourceString: String): String {
        return when (levelResourceString) {
            getString(R.string.level_elementary) -> ProficiencyLevel.ELEMENTARY
            getString(R.string.level_intermediate) -> ProficiencyLevel.INTERMEDIATE
            getString(R.string.level_advanced) -> ProficiencyLevel.ADVANCED
            getString(R.string.level_fluent) -> ProficiencyLevel.FLUENT
            getString(R.string.level_native) -> ProficiencyLevel.NATIVE
            else -> ProficiencyLevel.ELEMENTARY // По умолчанию
        }
    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за списком доступных языков
                viewModel.languages.collectLatest { languages ->
                    languageSelectAdapter?.clear()
                    languageSelectAdapter?.addAll(languages.map { it.name })
                    languageSelectAdapter?.notifyDataSetChanged()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за списком выбранных языков
                viewModel.selectedLanguagesUi.collectLatest { selectedLanguages ->
                    languageListAdapter.submitList(selectedLanguages)
                    // Показываем/скрываем текст "Языки не выбраны"
                    binding.tvNoLanguagesSelected.isVisible = selectedLanguages.isEmpty()
                    binding.rvSelectedLanguages.isVisible = selectedLanguages.isNotEmpty()
                }
            }
        }
    }

    private fun setupListeners() {
        // Выбор языка для добавления
        binding.actvLanguageSelect.setOnItemClickListener { adapterView, _, position, _ ->
            val selectedName = adapterView.getItemAtPosition(position) as? String
            selectedLanguageToAdd = viewModel.languages.value.find { it.name == selectedName }
            binding.tilLanguageSelect.error = null // Сбрасываем ошибку
        }

        // Выбор уровня для добавления
        binding.actvLevelSelect.setOnItemClickListener { adapterView, _, position, _ ->
            val selectedResourceString = adapterView.getItemAtPosition(position) as? String
            if (selectedResourceString != null) {
                selectedLevelToAdd = mapResourceStringToLevel(selectedResourceString)
            }
            binding.tilLevelSelect.error = null // Сбрасываем ошибку
        }

        // Кнопка "Добавить язык"
        binding.btnAddLanguage.setOnClickListener {
            if (selectedLanguageToAdd == null) {
                binding.tilLanguageSelect.error = getString(R.string.error_select_language_and_level)
                return@setOnClickListener
            }
            // Проверка, не добавлен ли уже этот язык
            val alreadyAdded = viewModel.selectedLanguagesUi.value.any { it.language.id == selectedLanguageToAdd!!.id }
            if (alreadyAdded) {
                Snackbar.make(binding.root, R.string.error_language_already_added, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Добавляем язык в ViewModel
            viewModel.addOrUpdateLanguage(selectedLanguageToAdd!!, selectedLevelToAdd)

            // Очищаем поля выбора (опционально)
            selectedLanguageToAdd = null
            binding.actvLanguageSelect.setText("", false)
            // Уровень можно оставить или сбросить на дефолтный
            // selectedLevelToAdd = ProficiencyLevel.ELEMENTARY
            // binding.actvLevelSelect.setText(mapLevelToResourceString(selectedLevelToAdd), false)
        }

        // Кнопка "Далее"
        binding.btnNextStep3.setOnClickListener {
            // Валидация не требуется на этом шаге по ТЗ (языки не обязательны)
            findNavController().navigate(R.id.action_candidateStep3Fragment_to_candidatePreviewFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvSelectedLanguages.adapter = null // Очистка адаптера RecyclerView
        binding.actvLanguageSelect.setAdapter(null)
        binding.actvLevelSelect.setAdapter(null)
        languageSelectAdapter = null
        levelSelectAdapter = null
        _binding = null
    }
}