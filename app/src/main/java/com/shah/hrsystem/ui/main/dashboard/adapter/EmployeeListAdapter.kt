package com.shah.hrsystem.ui.main.dashboard.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.Employee
import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.databinding.ItemEmployeeBinding

// DiffUtil Callback
class EmployeeDiffCallback : DiffUtil.ItemCallback<Employee>() {
    override fun areItemsTheSame(oldItem: Employee, newItem: Employee): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Employee, newItem: Employee): Boolean {
        return oldItem == newItem
    }
}

// Адаптер
class EmployeeListAdapter(
    private val onEmployeeClicked: (Employee) -> Unit,
    private val getPositionName: (Int) -> String?, // Функция для получения имени должности по ID
    private val getDepartmentName: (Int) -> String? // Функция для получения имени отдела по ID
) : ListAdapter<Employee, EmployeeListAdapter.EmployeeViewHolder>(EmployeeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val binding = ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmployeeViewHolder(binding, parent.context, onEmployeeClicked, getPositionName, getDepartmentName)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder
    class EmployeeViewHolder(
        private val binding: ItemEmployeeBinding,
        private val context: Context,
        private val onEmployeeClicked: (Employee) -> Unit,
        private val getPositionName: (Int) -> String?,
        private val getDepartmentName: (Int) -> String?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(employee: Employee) {
            binding.tvItemEmployeeName.text = "${employee.lastName} ${employee.firstName}"

            // Отображаем должность и отдел
            binding.tvItemEmployeePosition.text = getPositionName(employee.positionId) ?: context.getString(R.string.unknown_position)
            binding.tvItemEmployeeDepartment.text = getDepartmentName(employee.departmentId) ?: context.getString(R.string.unknown_department)

            // Настраиваем Chip статуса
            when (employee.status) {
                EmployeeConstants.STATUS_NEW -> {
                    binding.chipItemStatus.text = context.getString(R.string.status_new_chip)
                    binding.chipItemStatus.setChipBackgroundColorResource(R.color.status_new_background)
                    binding.chipItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_new_text))
                    binding.chipItemStatus.setChipIconResource(R.drawable.ic_new_application) // Опционально
                    binding.chipItemStatus.isChipIconVisible = true // Показываем иконку
                }
                EmployeeConstants.STATUS_ACCEPTED -> {
                    binding.chipItemStatus.text = context.getString(R.string.status_accepted_chip)
                    binding.chipItemStatus.setChipBackgroundColorResource(R.color.status_accepted_background)
                    binding.chipItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_accepted_text))
                    binding.chipItemStatus.setChipIconResource(R.drawable.ic_accepted) // Опционально
                    binding.chipItemStatus.isChipIconVisible = true
                }
                else -> {
                    binding.chipItemStatus.text = employee.status // Показать как есть, если статус неизвестен
                    binding.chipItemStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
                    binding.chipItemStatus.isChipIconVisible = false
                }
            }

            // Обработчик клика на элемент
            binding.root.setOnClickListener {
                onEmployeeClicked(employee)
            }
        }
    }
}