package com.shah.hrsystem.ui.main.admin.dictionary_management.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.data.db.entity.Position
import com.shah.hrsystem.databinding.ItemDictionaryBinding

// DiffUtil для Any (сравниваем по содержимому и ID, если возможно)
class DictionaryDiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Department && newItem is Department -> oldItem.id == newItem.id
            oldItem is Position && newItem is Position -> oldItem.id == newItem.id
            oldItem is Language && newItem is Language -> oldItem.id == newItem.id
            else -> oldItem == newItem // Фолбэк на equals
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return oldItem == newItem // Сравниваем data class'ы
    }
}

// Адаптер
class DictionaryAdapter(
    private val getDepartmentName: (Int) -> String?, // Для отображения отдела в должности
    private val onEditClick: (Any) -> Unit,
    private val onDeleteClick: (Any) -> Unit
) : ListAdapter<Any, DictionaryAdapter.DictionaryViewHolder>(DictionaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionaryViewHolder {
        val binding = ItemDictionaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DictionaryViewHolder(binding, getDepartmentName, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: DictionaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder
    class DictionaryViewHolder(
        private val binding: ItemDictionaryBinding,
        private val getDepartmentName: (Int) -> String?,
        private val onEditClick: (Any) -> Unit,
        private val onDeleteClick: (Any) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Any) {
            var itemName = ""
            var itemDetails: String? = null

            when (item) {
                is Department -> {
                    itemName = item.name
                }
                is Position -> {
                    itemName = item.name
                    val deptName = getDepartmentName(item.departmentId) ?: itemView.context.getString(R.string.unknown_department)
                    val detailsParts = mutableListOf<String>()
                    detailsParts.add("Отдел: $deptName")
                    detailsParts.add("Max: ${item.maxAllowed}")
                    if (item.requiresHigherEducationBool()) detailsParts.add("Высш. обр.")
                    if (item.isAssistantBool()) detailsParts.add("Ассистент")
                    itemDetails = detailsParts.joinToString(" / ")
                }
                is Language -> {
                    itemName = item.name
                }
                else -> {
                    itemName = item.toString() // Фолбэк
                }
            }

            binding.tvDictItemName.text = itemName
            binding.tvDictItemDetails.text = itemDetails
            binding.tvDictItemDetails.isVisible = itemDetails != null

            // Клик по элементу для редактирования
            binding.root.setOnClickListener {
                onEditClick(item)
            }

            // Клик по кнопке опций
            binding.btnMoreDictOptions.setOnClickListener { view ->
                showPopupMenu(view, item)
            }
        }

        private fun showPopupMenu(anchorView: View, item: Any) {
            val popup = PopupMenu(anchorView.context, anchorView)
            // Используем то же меню, что и для пользователей (Редактировать, Удалить)
            popup.menuInflater.inflate(R.menu.dictionary_item_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_dict_edit -> {
                        onEditClick(item)
                        true
                    }
                    R.id.action_dict_delete -> {
                        onDeleteClick(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}