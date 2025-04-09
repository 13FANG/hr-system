package com.shah.hrsystem.ui.main.admin.user_management.dialog

import android.app.Dialog
import android.os.Build // Импорт Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.User
import com.shah.hrsystem.data.db.entity.UserRole
import com.shah.hrsystem.databinding.DialogUserAddEditBinding
import com.shah.hrsystem.viewmodel.AdminUserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserAddEditDialog : DialogFragment() {

    private var _binding: DialogUserAddEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminUserViewModel by viewModels({ requireParentFragment() })

    private var editingUser: User? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUserAddEditBinding.inflate(LayoutInflater.from(context))

        // ИСПРАВЛЕНО: Используем безопасный способ получения Parcelable
        editingUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_USER, User::class.java)
        } else {
            @Suppress("DEPRECATION") // Подавляем предупреждение для старых API
            arguments?.getParcelable(ARG_USER)
        }


        setupViews()

        val title = if (editingUser == null) getString(R.string.dialog_add_user_title) else getString(R.string.dialog_edit_user_title)
        val positiveButtonText = if (editingUser == null) getString(R.string.add_button) else getString(R.string.save_button)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(positiveButtonText) { _, _ -> handleSave() }
            .setNegativeButton(R.string.cancel_button, null)
            .create()
    }

    private fun setupViews() {
        if (editingUser != null) {
            binding.etDialogUserLogin.setText(editingUser?.login)
            binding.tilDialogUserPassword.isVisible = false
            val roleId = if (editingUser?.role == UserRole.ADMIN) R.id.rb_dialog_role_admin else R.id.rb_dialog_role_hr
            binding.rgDialogUserRole.check(roleId)
        } else {
            binding.rgDialogUserRole.check(R.id.rb_dialog_role_hr)
        }
    }

    private fun handleSave() {
        val login = binding.etDialogUserLogin.text.toString().trim()
        val password = binding.etDialogUserPassword.text.toString()
        val selectedRoleId = binding.rgDialogUserRole.checkedRadioButtonId
        val role = if (selectedRoleId == R.id.rb_dialog_role_admin) UserRole.ADMIN else UserRole.HR

        if (login.isBlank()) {
            binding.tilDialogUserLogin.error = getString(R.string.error_field_required)
            // Можно предотвратить закрытие, но это сложнее. Пока просто показываем ошибку.
            return
        }
        if (editingUser == null && password.length < 6) {
            binding.tilDialogUserPassword.error = getString(R.string.error_password_too_short)
            return
        }

        if (editingUser == null) {
            viewModel.addUser(login, password, role)
        } else {
            viewModel.updateUser(editingUser!!, login, role)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UserAddEditDialog"
        private const val ARG_USER = "user_to_edit"
        const val REQUEST_KEY = "user_dialog_request"

        fun newInstance(user: User? = null): UserAddEditDialog {
            return UserAddEditDialog().apply {
                arguments = bundleOf(ARG_USER to user) // Убедитесь, что User реализует Parcelable
            }
        }

        fun show(fragmentManager: FragmentManager, user: User? = null) {
            newInstance(user).show(fragmentManager, TAG)
        }
    }
}