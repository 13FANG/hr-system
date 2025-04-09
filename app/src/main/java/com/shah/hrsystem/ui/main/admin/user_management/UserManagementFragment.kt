package com.shah.hrsystem.ui.main.admin.user_management

import android.content.Context // Импорт Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout // Импорт FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.databinding.FragmentUserManagementBinding
import com.shah.hrsystem.ui.main.admin.user_management.adapter.UserListAdapter
import com.shah.hrsystem.ui.main.admin.user_management.dialog.UserAddEditDialog
import com.shah.hrsystem.viewmodel.AdminUserUiState
import com.shah.hrsystem.viewmodel.AdminUserViewModel
import com.shah.hrsystem.viewmodel.UserOperationState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserManagementFragment : Fragment() {

    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminUserViewModel by viewModels()
    private lateinit var userAdapter: UserListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        setupToolbar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarUserMgmnt.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupRecyclerView() {
        userAdapter = UserListAdapter(
            onEditClick = { user -> UserAddEditDialog.show(childFragmentManager, user) },
            onDeleteClick = { user -> showDeleteConfirmationDialog(user) },
            onResetPasswordClick = { user -> showResetPasswordDialog(user) }
        )
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        binding.fabAddUser.setOnClickListener {
            UserAddEditDialog.show(childFragmentManager, null)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    binding.progressBarUserMgmnt.isVisible = state is AdminUserUiState.Loading
                    binding.rvUsers.isVisible = state is AdminUserUiState.Success
                    binding.tvEmptyUserList.isVisible = state is AdminUserUiState.Success && state.users.isEmpty()

                    if (state is AdminUserUiState.Success) {
                        userAdapter.submitList(state.users)
                    } else if (state is AdminUserUiState.Error) {
                        showSnackbar(state.message)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.operationState.collect { state ->
                    when (state) {
                        is UserOperationState.Success -> {
                            showSnackbar(state.message)
                            viewModel.clearOperationState()
                        }
                        is UserOperationState.Error -> {
                            showSnackbar(state.message)
                            viewModel.clearOperationState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(user: com.shah.hrsystem.data.db.entity.User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_user_message, user.login))
            .setNegativeButton(R.string.cancel_button, null)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                viewModel.deleteUser(user)
            }
            .show()
    }

    // ИСПРАВЛЕНО: Оборачиваем EditText в FrameLayout для отступов
    private fun showResetPasswordDialog(user: com.shah.hrsystem.data.db.entity.User) {
        val context = requireContext()
        val inputEditText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = getString(R.string.new_password_hint_short)
        }

        // Создаем контейнер для EditText и устанавливаем отступы
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            // Конвертируем dp в px для отступов
            val margin = (20 * resources.displayMetrics.density).toInt()
            setMargins(margin, margin / 2, margin, 0) // Добавляем отступы слева, сверху/снизу, справа
        }
        inputEditText.layoutParams = params
        container.addView(inputEditText)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_reset_password_title)
            .setMessage(getString(R.string.dialog_reset_password_message, user.login))
            .setView(container) // Устанавливаем контейнер с отступами
            .setNegativeButton(R.string.cancel_button, null)
            .setPositiveButton(R.string.reset_button) { _, _ ->
                val newPassword = inputEditText.text.toString()
                if (newPassword.length >= 6) {
                    viewModel.resetPassword(user.id, newPassword)
                } else {
                    showSnackbar(getString(R.string.error_password_too_short))
                }
            }
            .show()
    }


    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvUsers.adapter = null
        _binding = null
    }
}