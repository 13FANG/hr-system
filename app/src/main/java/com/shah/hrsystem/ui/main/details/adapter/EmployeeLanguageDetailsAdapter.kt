package com.shah.hrsystem.ui.main.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.databinding.ItemEmployeeLanguageDetailBinding

// Используем Pair<Language, String> где String - это ProficiencyLevel
class LanguageDetailDiffCallback : DiffUtil.ItemCallback<Pair<Language, String>>() {
    override fun areItemsTheSame(oldItem: Pair<Language, String>, newItem: Pair<Language, String>): Boolean {
        // Сравниваем по ID языка
        return oldItem.first.id == newItem.first.id
    }

    override fun areContentsTheSame(oldItem: Pair<Language, String>, newItem: Pair<Language, String>): Boolean {
        // Сравниваем и язык, и уровень
        return oldItem == newItem
    }
}

class EmployeeLanguageDetailsAdapter : ListAdapter<Pair<Language, String>, EmployeeLanguageDetailsAdapter.LanguageDetailViewHolder>(LanguageDetailDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageDetailViewHolder {
        val binding = ItemEmployeeLanguageDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LanguageDetailViewHolder(private val binding: ItemEmployeeLanguageDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(languageDetail: Pair<Language, String>) {
            binding.tvDetailLanguageName.text = "${languageDetail.first.name}:" // Добавляем двоеточие для ясности
            binding.tvDetailLanguageLevel.text = languageDetail.second
        }
    }
}