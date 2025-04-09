package com.shah.hrsystem.ui.main.admin.user_management.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.User
import com.shah.hrsystem.data.db.entity.UserRole
import com.shah.hrsystem.databinding.ItemUserBinding

// DiffUtil Callback
class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}

// Адаптер
class UserListAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit,
    private val onResetPasswordClick: (User) -> Unit
) : ListAdapter<User, UserListAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding, onEditClick, onDeleteClick, onResetPasswordClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder
    class UserViewHolder(
        private val binding: ItemUserBinding,
        private val onEditClick: (User) -> Unit,
        private val onDeleteClick: (User) -> Unit,
        private val onResetPasswordClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvUserLogin.text = user.login
            binding.tvUserRole.text = user.role

            // Устанавливаем иконку роли
            val roleIconRes = when (user.role) {
                UserRole.ADMIN -> R.drawable.ic_admin_panel
                UserRole.HR -> R.drawable.ic_manage_users // Используем иконку управления пользователями для HR
                else -> R.drawable.ic_person // Запасная иконка
            }
            binding.ivUserRoleIcon.setImageResource(roleIconRes)

            // Настраиваем PopupMenu для кнопки опций
            binding.btnMoreUserOptions.setOnClickListener { view ->
                showPopupMenu(view, user)
            }
            // Можно добавить клик по всему элементу для редактирования
            binding.root.setOnClickListener { onEditClick(user) }
        }

        private fun showPopupMenu(anchorView: View, user: User) {
            val popup = PopupMenu(anchorView.context, anchorView)
            popup.menuInflater.inflate(R.menu.user_item_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_user_edit -> {
                        onEditClick(user)
                        true
                    }
                    R.id.action_user_delete -> {
                        onDeleteClick(user)
                        true
                    }
                    R.id.action_user_reset_password -> {
                        onResetPasswordClick(user)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}