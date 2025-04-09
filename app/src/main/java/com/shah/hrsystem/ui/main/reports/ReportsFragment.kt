package com.shah.hrsystem.ui.main.reports

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider // Оставляем для старых версий, если понадобится
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.databinding.FragmentReportsBinding
import com.shah.hrsystem.viewmodel.ReportGenerationState
import com.shah.hrsystem.viewmodel.ReportsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()

    private val TAG = "ReportsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        setupToolbar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarReports.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupListeners() {
        binding.btnReportByDepartment.setOnClickListener { viewModel.generateEmployeesByDepartmentReport() }
        binding.btnReportByLanguage.setOnClickListener { viewModel.generateEmployeesByLanguageReport() }
        binding.btnReportByAge.setOnClickListener { viewModel.generateAgeDistributionReport() }
        binding.btnReportVacancies.setOnClickListener { viewModel.generateAvailableVacanciesReport() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportState.collect { state ->
                    val isLoading = state is ReportGenerationState.LoadingData || state is ReportGenerationState.GeneratingPdf
                    binding.progressBarReports.isVisible = isLoading
                    // Блокируем кнопки во время генерации
                    binding.btnReportByDepartment.isEnabled = !isLoading
                    binding.btnReportByLanguage.isEnabled = !isLoading
                    binding.btnReportByAge.isEnabled = !isLoading
                    binding.btnReportVacancies.isEnabled = !isLoading

                    when (state) {
                        is ReportGenerationState.Success -> {
                            // Передаем Any? (Uri или File)
                            showSuccessSnackbar(state.fileUriOrPath)
                            viewModel.resetReportState()
                        }
                        is ReportGenerationState.Error -> {
                            showErrorSnackbar(state.message)
                            viewModel.resetReportState()
                        }
                        else -> { /* Idle, LoadingData, GeneratingPdf */ }
                    }
                }
            }
        }
    }

    // Принимает Any? и обрабатывает Uri или File
    private fun showSuccessSnackbar(fileUriOrPath: Any?) {
        if (fileUriOrPath == null) {
            showErrorSnackbar("Не удалось получить путь к файлу.")
            return
        }

        val uriToUse: Uri? = when (fileUriOrPath) {
            is Uri -> fileUriOrPath // Готовый Uri от MediaStore
            is File -> { // File для старых версий Android
                val context = requireContext()
                val authority = "${context.packageName}.provider"
                try {
                    FileProvider.getUriForFile(context, authority, fileUriOrPath)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Error getting FileProvider URI for legacy file.", e)
                    null
                }
            }
            else -> null
        }

        if (uriToUse == null) {
            showErrorSnackbar("Не удалось получить Uri для файла.")
            return
        }

        // Используем новое короткое сообщение и добавляем кнопку "Поделиться"
        Snackbar.make(binding.root, R.string.report_success_message_short, Snackbar.LENGTH_LONG)
            .setAction(R.string.report_open_button) {
                openPdfUri(uriToUse) // Используем новую функцию для Uri
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {
                    super.onShown(sb)
                    // Находим родительский Layout Snackbar'а и добавляем кнопку "Поделиться"
                    sb?.view?.findViewById<com.google.android.material.button.MaterialButton>(com.google.android.material.R.id.snackbar_action)?.let { actionButton ->
                        try {
                            // Используем context фрагмента безопасно
                            val parentLayout = actionButton.parent as? android.widget.LinearLayout
                            if (parentLayout != null) {
                                val shareButton = com.google.android.material.button.MaterialButton(requireContext(), null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                                    text = getString(R.string.report_share_button) // Новая строка "Поделиться"
                                    setTextColor(actionButton.currentTextColor) // Используем тот же цвет текста
                                    // Устанавливаем отступы для кнопки "Поделиться"
                                    val params = android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                    )
                                    val marginEndPx = (8 * resources.displayMetrics.density).toInt() // 8dp в пиксели
                                    params.marginEnd = marginEndPx
                                    layoutParams = params

                                    setOnClickListener {
                                        sharePdfUri(uriToUse) // Используем новую функцию для Uri
                                        sb.dismiss() // Закрываем Snackbar после нажатия "Поделиться"
                                    }
                                }
                                parentLayout.addView(shareButton, parentLayout.indexOfChild(actionButton)) // Добавляем перед кнопкой "Открыть"
                            } else {
                                Log.w(TAG, "Snackbar action button parent is not a LinearLayout?")
                            }
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "Fragment not attached to a context, cannot create share button.", e)
                        }
                    } ?: Log.w(TAG, "Snackbar action button not found.")
                }
            })
            .show()
    }

    private fun showErrorSnackbar(message: String) {
        if (view != null && _binding != null) { // Добавлена проверка
            Snackbar.make(binding.root, "${getString(R.string.report_error_message)}: $message", Snackbar.LENGTH_LONG).show()
        } else {
            Log.w(TAG, "Error Snackbar requested but view or binding is null: $message")
        }
    }


    // Функция для открытия PDF по Uri
    private fun openPdfUri(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                // Важно для MediaStore Uri и FileProvider Uri
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showErrorSnackbar(getString(R.string.report_error_no_app))
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF URI: $uri", e)
            showErrorSnackbar("Не удалось открыть файл.")
        }
    }

    // Функция для шаринга PDF по Uri
    private fun sharePdfUri(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Отчет из системы кадрового учета")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(intent, "Поделиться отчетом через...")
            startActivity(chooserIntent)
        } catch (e: Exception) { // Ловим общую ошибку для простоты
            Log.e(TAG, "Error sharing PDF URI: $uri", e)
            showErrorSnackbar("Не удалось поделиться файлом.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}