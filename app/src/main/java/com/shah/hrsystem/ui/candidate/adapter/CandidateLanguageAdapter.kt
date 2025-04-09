package com.shah.hrsystem.ui.candidate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.databinding.ItemCandidateLanguageBinding
import com.shah.hrsystem.viewmodel.SelectedLanguageUi

// DiffUtil для эффективного обновления списка
class LanguageDiffCallback : DiffUtil.ItemCallback<SelectedLanguageUi>() {
    override fun areItemsTheSame(oldItem: SelectedLanguageUi, newItem: SelectedLanguageUi): Boolean {
        return oldItem.language.id == newItem.language.id // Сравниваем по ID языка
    }

    override fun areContentsTheSame(oldItem: SelectedLanguageUi, newItem: SelectedLanguageUi): Boolean {
        return oldItem == newItem // Сравниваем все поля data class
    }
}

// Адаптер
class CandidateLanguageAdapter(
    private val onRemoveClicked: (Language) -> Unit // Callback для удаления
) : ListAdapter<SelectedLanguageUi, CandidateLanguageAdapter.LanguageViewHolder>(LanguageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemCandidateLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding, onRemoveClicked)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ViewHolder
    class LanguageViewHolder(
        private val binding: ItemCandidateLanguageBinding,
        private val onRemoveClicked: (Language) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(selectedLanguageUi: SelectedLanguageUi) {
            binding.tvLanguageName.text = selectedLanguageUi.language.name
            binding.tvLanguageLevel.text = selectedLanguageUi.level
            binding.btnRemoveLanguage.setOnClickListener {
                onRemoveClicked(selectedLanguageUi.language)
            }
        }
    }
}