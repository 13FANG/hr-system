package com.shah.hrsystem.ui.main.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity // Для Toolbar
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.databinding.FragmentSettingsBinding
import com.shah.hrsystem.util.Constants
import com.shah.hrsystem.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        setupToolbar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        // Устанавливаем Toolbar как ActionBar для этого фрагмента
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbarSettings)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowHomeEnabled(true)
        // Обработчик для кнопки Назад в Toolbar
        binding.toolbarSettings.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupListeners() {
        // Смена темы
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.rb_theme_light -> Constants.THEME_LIGHT
                R.id.rb_theme_dark -> Constants.THEME_DARK
                R.id.rb_theme_system -> Constants.THEME_SYSTEM
                else -> Constants.THEME_SYSTEM
            }
            if (themeMode != viewModel.currentTheme.value) {
                viewModel.applyAndSaveTheme(themeMode)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за текущей темой для установки RadioButton
                viewModel.currentTheme.collectLatest { themeMode ->
                    val checkedId = when (themeMode) {
                        Constants.THEME_LIGHT -> R.id.rb_theme_light
                        Constants.THEME_DARK -> R.id.rb_theme_dark
                        else -> R.id.rb_theme_system
                    }
                    // Устанавливаем checked, только если он отличается, чтобы не вызвать listener зря
                    if (binding.rgTheme.checkedRadioButtonId != checkedId) {
                        binding.rgTheme.check(checkedId)
                    }
                }
            }
        }
        // Наблюдение за passwordChangeState убрано
    }

    // Функции clearPasswordError и showSnackbar больше не нужны здесь (или showSnackbar можно оставить)
    private fun showSnackbar(message: String) {
        if (view != null && _binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Снимаем ActionBar
        (activity as? AppCompatActivity)?.setSupportActionBar(null)
        _binding = null
    }
}