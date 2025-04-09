package com.shah.hrsystem.ui.auth

import android.os.Bundle
import android.util.Log // Добавлен импорт для Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.shah.hrsystem.databinding.FragmentLoginBinding
import com.shah.hrsystem.util.SessionManager // Добавлен импорт SessionManager
import com.shah.hrsystem.viewmodel.LoginUiState
import com.shah.hrsystem.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject // Добавлен импорт Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    // ViewBinding
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!! // Гарантируем non-null доступ после onCreateView

    // Получение ViewModel через Hilt
    private val viewModel: LoginViewModel by viewModels()

    // Внедряем SessionManager
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    // Настройка слушателей кликов и изменений текста
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            // Обновляем данные в ViewModel перед вызовом loginUser
            viewModel.loginInput.value = binding.etLogin.text.toString()
            viewModel.passwordInput.value = binding.etPassword.text.toString()
            viewModel.loginUser()
        }

        binding.btnApplyJob.setOnClickListener {
            // Переход на первый шаг анкеты кандидата
            findNavController().navigate(R.id.action_loginFragment_to_candidateStep1Fragment)
        }

        // Очистка ошибок при изменении текста (опционально)
        binding.etLogin.addTextChangedListener {
            binding.tilLogin.error = null
            viewModel.clearInputError()
            // Можно также сбрасывать UI state в Idle, если нужно
            // viewModel.resetStateToIdle()
        }
        binding.etPassword.addTextChangedListener {
            binding.tilPassword.error = null
            viewModel.clearInputError()
            // viewModel.resetStateToIdle()
        }
    }

    // Наблюдение за состояниями ViewModel
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle гарантирует, что сбор данных идет только когда фрагмент активен (STARTED)
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за состоянием UI входа
                viewModel.loginUiState.collect { state ->
                    // Скрываем все элементы управления вводом и показываем прогресс при загрузке
                    val isLoading = state is LoginUiState.Loading
                    binding.progressBarLogin.isVisible = isLoading
                    binding.tilLogin.isEnabled = !isLoading
                    binding.tilPassword.isEnabled = !isLoading
                    binding.btnLogin.isEnabled = !isLoading
                    binding.btnApplyJob.isEnabled = !isLoading

                    when (state) {
                        is LoginUiState.Success -> {
                            // Успешный вход - можно показать сообщение и перейти дальше
                            Toast.makeText(context, "Вход выполнен (${state.user.role})", Toast.LENGTH_SHORT).show()

                            // --- ВАЖНЫЙ ШАГ: Обновляем SessionManager ---
                            sessionManager.loginUser(state.user)
                            Log.d("LoginFragment", "SessionManager updated with user: ${state.user.login}, role: ${state.user.role}")
                            // --------------------------------------------

                            // Переход на главный экран
                            findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                            // Сброс состояния после навигации (важно!)
                            viewModel.resetStateToIdle()
                        }
                        is LoginUiState.Error -> {
                            // Показываем ошибку пользователю
                            showErrorSnackbar(state.message)
                            // ViewModel сам вернется в Idle после ошибки
                        }
                        is LoginUiState.Loading -> {
                            // Ничего не делаем дополнительно, ProgressBar уже видим
                        }
                        is LoginUiState.Idle -> {
                            // Начальное состояние или после ошибки/успеха
                        }
                    }
                }
            }
        }

        // Наблюдение за ошибками валидации (если будете использовать)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.inputError.collect { errorMessage ->
                    // Можно показывать ошибки под конкретными полями, если ViewModel их разделяет
                    // binding.tilLogin.error = errorMessage
                    // binding.tilPassword.error = errorMessage
                    // Или просто показать Snackbar/Toast, как в LoginUiState.Error
                }
            }
        }
    }

    // Показывает Snackbar с сообщением об ошибке
    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    // Очистка ViewBinding при уничтожении View фрагмента
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}