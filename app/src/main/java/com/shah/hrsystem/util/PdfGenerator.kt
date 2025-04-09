package com.shah.hrsystem.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Employee
import com.shah.hrsystem.data.db.entity.Language
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate

data class VacancyInfo(
    val departmentName: String,
    val positionName: String,
    val maxAllowed: Int,
    val currentlyFilled: Int,
    val availableCount: Int
)

object PdfGenerator {

    private const val TAG = "PdfGenerator"
    private const val REPORTS_SUBFOLDER_DOWNLOADS = "HRSystemReports"
    private const val FONT_PATH = "/system/fonts/Roboto-Regular.ttf"
    // Убрали кэшированную переменную: private var defaultFont: PdfFont? = null

    // --- ИЗМЕНЕНО: Убрано кэширование шрифта ---
    private fun getDefaultFont(): PdfFont? {
        return try {
            val font = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)
            Log.i(TAG, "Default font created successfully: $FONT_PATH")
            font
        } catch (e: IOException) {
            Log.e(TAG, "Error creating default font '$FONT_PATH'.", e)
            try {
                val fallbackFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)
                Log.w(TAG, "Falling back to HELVETICA font.")
                fallbackFont
            } catch (fe: IOException) {
                Log.e(TAG, "Error creating fallback font HELVETICA", fe)
                null
            }
        }
    }

    // Создание потока вывода (без изменений)
    private fun createPdfOutputStream(
        context: Context,
        resolver: ContentResolver,
        baseFileName: String
    ): Pair<OutputStream?, Any?> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pdfFileName = "${baseFileName}_$timeStamp.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + REPORTS_SUBFOLDER_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            var itemUri: Uri? = null; var outputStream: OutputStream? = null
            try {
                itemUri = resolver.insert(collection, values) ?: throw IOException("Failed to create MediaStore record")
                outputStream = resolver.openOutputStream(itemUri) ?: throw IOException("Failed to open output stream for $itemUri")
                Log.d(TAG, "MediaStore OutputStream created for $pdfFileName")
                return Pair(outputStream, itemUri)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating MediaStore entry or stream", e)
                if (itemUri != null && outputStream == null) { try { resolver.delete(itemUri, null, null) } catch (_: Exception) {} }
                return Pair(null, null)
            }
        } else {
            Log.w(TAG, "Using legacy external storage method.")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val reportDir = File(downloadsDir, REPORTS_SUBFOLDER_DOWNLOADS)
            if (!reportDir.exists() && !reportDir.mkdirs()) { Log.e(TAG, "Failed to create legacy directory"); return Pair(null, null) }
            val pdfFile = File(reportDir, pdfFileName)
            return try { Pair(FileOutputStream(pdfFile), pdfFile) } catch (e: IOException) { Log.e(TAG,"Error legacy stream", e); Pair(null, null)}
        }
    }

    // Финализация файла (без изменений)
    private fun finalizeMediaStoreFile(resolver: ContentResolver, uri: Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
            try {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                resolver.update(uri, values, null, null)
                Log.d(TAG, "MediaStore file finalized: $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing MediaStore file (IS_PENDING=0): $uri", e)
            }
        }
    }

    // Добавление заголовка (использует свежий шрифт)
    private fun addTitle(document: Document, title: String) {
        val font = getDefaultFont()
        try {
            val p = Paragraph(title).setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(16f).setMarginBottom(20f)
            if (font != null) p.setFont(font) else Log.w(TAG, "addTitle: Font is null")
            document.add(p)
            Log.d(TAG, "Added title: '$title'")
        } catch (e: Exception) { Log.e(TAG, "Error adding title: '$title'", e) }
    }

    // Добавление параграфа (использует свежий шрифт)
    private fun addParagraph(document: Document, text: String, isBold: Boolean = false, fontSize: Float = 12f) {
        val font = getDefaultFont()
        try {
            val p = Paragraph(text).setFontSize(fontSize)
            if (isBold) p.setBold()
            if (font != null) p.setFont(font) else Log.w(TAG, "addParagraph: Font is null")
            document.add(p)
            Log.d(TAG, "Added paragraph: '$text'")
        } catch (e: Exception) { Log.e(TAG, "Error adding paragraph: '$text'", e) }
    }


    // --- ИЗМЕНЕНА сигнатура и логика закрытия ---
    private fun closeDocumentAndStream(
        document: Document?,
        writer: PdfWriter?, // Добавлен PdfWriter
        pdfDocument: PdfDocument?, // Добавлен PdfDocument
        outputStream: OutputStream?, // OutputStream все еще нужен для явного закрытия
        fileUriOrPath: Any?,
        resolver: ContentResolver,
        contentGeneratedSuccessfully: Boolean // Флаг успеха генерации контента
    ): Any? {
        var finalResult: Any? = null

        // Пытаемся закрыть ресурсы iText в обратном порядке их создания
        // Document -> PdfDocument -> PdfWriter -> OutputStream (хотя OutputStream закрывается явно ниже)
        try {
            document?.close()
            Log.i(TAG, "iText Document closed for $fileUriOrPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing iText Document for $fileUriOrPath", e)
        }
        // PdfDocument и PdfWriter обычно закрываются каскадно при закрытии Document,
        // но для полной уверенности можно добавить явные вызовы в try-catch, если проблема останется.

        // Всегда пытаемся закрыть OutputStream
        try {
            outputStream?.close()
            Log.i(TAG, "OutputStream closed explicitly for $fileUriOrPath")
        } catch (e: IOException) {
            Log.e(TAG, "Error explicitly closing OutputStream for $fileUriOrPath", e)
        }

        // Обрабатываем файл/запись в зависимости от успеха генерации контента
        if (contentGeneratedSuccessfully && fileUriOrPath != null) {
            if (fileUriOrPath is Uri) {
                finalizeMediaStoreFile(resolver, fileUriOrPath)
            }
            finalResult = fileUriOrPath // Сохраняем результат
            Log.i(TAG, "Report generation successful for $fileUriOrPath")
        } else if (fileUriOrPath != null) {
            Log.w(TAG, "Content generation failed or path is null, attempting to clean up $fileUriOrPath")
            if (fileUriOrPath is Uri) {
                try { resolver.delete(fileUriOrPath, null, null) } catch (_: Exception) {}
            } else if (fileUriOrPath is File) {
                fileUriOrPath.delete()
            }
            finalResult = null // Генерация не удалась
        } else {
            Log.e(TAG, "closeDocumentAndStream: fileUriOrPath is null, cannot proceed.")
            finalResult = null
        }

        return finalResult
    }
    // --- КОНЕЦ ИЗМЕНЕННОЙ ЛОГИКИ ЗАКРЫТИЯ ---


    // --- Публичные методы генерации отчетов (ИЗМЕНЕНА СТРУКТУРА) ---

    fun generateEmployeesByDepartmentReport(
        context: Context,
        resolver: ContentResolver,
        data: Map<Department, List<Employee>>
    ): Any? {
        val (outputStream, fileUriOrPath) = createPdfOutputStream(context, resolver, "EmployeesByDepartment")
        if (outputStream == null || fileUriOrPath == null) return null

        var writer: PdfWriter? = null
        var pdfDocument: PdfDocument? = null
        var document: Document? = null
        var success = false

        try {
            writer = PdfWriter(outputStream)
            pdfDocument = PdfDocument(writer)
            document = Document(pdfDocument)
            Log.i(TAG, "[EmpByDept] Document created. Adding content...")

            addTitle(document, "Отчет: Сотрудники по подразделениям")
            addParagraph(document, "Дата создания: ${DateUtils.formatDateForDisplay(LocalDate.now())}")
            document.add(Paragraph("\n"))

            if (data.isEmpty()) {
                addParagraph(document, "Нет данных для отображения.")
            } else {
                data.entries.forEach { (department, employees) ->
                    addParagraph(document, department.name, isBold = true, fontSize = 14f)
                    if (employees.isEmpty()) {
                        addParagraph(document, "  - Нет сотрудников")
                    } else {
                        employees.forEach { emp ->
                            addParagraph(document, "  - ${emp.lastName} ${emp.firstName}")
                        }
                    }
                    document.add(Paragraph("\n"))
                }
            }
            Log.i(TAG, "[EmpByDept] Content adding finished successfully.")
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "[EmpByDept] Error during content generation", e)
            success = false
        } finally {
            // Закрываем ресурсы и возвращаем результат
            return closeDocumentAndStream(document, writer, pdfDocument, outputStream, fileUriOrPath, resolver, success)
        }
    }

    fun generateEmployeesByLanguageReport(
        context: Context,
        resolver: ContentResolver,
        data: Map<Language, List<Employee>>
    ): Any? {
        val (outputStream, fileUriOrPath) = createPdfOutputStream(context, resolver, "EmployeesByLanguage")
        if (outputStream == null || fileUriOrPath == null) return null

        var writer: PdfWriter? = null
        var pdfDocument: PdfDocument? = null
        var document: Document? = null
        var success = false

        try {
            writer = PdfWriter(outputStream)
            pdfDocument = PdfDocument(writer)
            document = Document(pdfDocument)
            Log.i(TAG, "[EmpByLang] Document created. Adding content...")

            addTitle(document, "Отчет: Сотрудники по языкам")
            addParagraph(document, "Дата создания: ${DateUtils.formatDateForDisplay(LocalDate.now())}")
            document.add(Paragraph("\n"))

            if (data.isEmpty()) {
                addParagraph(document, "Нет данных для отображения (отчет не реализован).")
            } else {
                // Эта часть пока не выполнится
                data.entries.forEach { (language, employees) ->
                    addParagraph(document, language.name, isBold = true, fontSize = 14f)
                    if (employees.isEmpty()) {
                        addParagraph(document, "  - Нет сотрудников")
                    } else {
                        employees.forEach { emp ->
                            addParagraph(document, "  - ${emp.lastName} ${emp.firstName}")
                        }
                    }
                    document.add(Paragraph("\n"))
                }
            }
            Log.i(TAG, "[EmpByLang] Content adding finished successfully.")
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "[EmpByLang] Error during content generation", e)
            success = false
        } finally {
            return closeDocumentAndStream(document, writer, pdfDocument, outputStream, fileUriOrPath, resolver, success)
        }
    }


    fun generateAgeDistributionReport(
        context: Context,
        resolver: ContentResolver,
        employees: List<Employee>
    ): Any? {
        val (outputStream, fileUriOrPath) = createPdfOutputStream(context, resolver, "AgeDistribution")
        if (outputStream == null || fileUriOrPath == null) return null

        var writer: PdfWriter? = null
        var pdfDocument: PdfDocument? = null
        var document: Document? = null
        var success = false

        try {
            writer = PdfWriter(outputStream)
            pdfDocument = PdfDocument(writer)
            document = Document(pdfDocument)
            Log.i(TAG, "[AgeDist] Document created. Adding content...")

            addTitle(document, "Отчет: Возрастной состав сотрудников")
            addParagraph(document, "Дата создания: ${DateUtils.formatDateForDisplay(LocalDate.now())}")
            document.add(Paragraph("\n"))

            if (employees.isEmpty()) {
                addParagraph(document, "Нет данных для анализа.")
            } else {
                val ageGroups = mutableMapOf( "До 25 лет" to 0, "25-34 лет" to 0, "35-44 лет" to 0, "45-54 лет" to 0, "55 лет и старше" to 0, "Возраст не определен" to 0 )
                employees.forEach { emp ->
                    val age = DateUtils.calculateAge(emp.dateOfBirth)
                    when {
                        age == null -> ageGroups["Возраст не определен"] = ageGroups.getOrDefault("Возраст не определен", 0) + 1
                        age < 25 -> ageGroups["До 25 лет"] = ageGroups.getOrDefault("До 25 лет", 0) + 1
                        age in 25..34 -> ageGroups["25-34 лет"] = ageGroups.getOrDefault("25-34 лет", 0) + 1
                        age in 35..44 -> ageGroups["35-44 лет"] = ageGroups.getOrDefault("35-44 лет", 0) + 1
                        age in 45..54 -> ageGroups["45-54 лет"] = ageGroups.getOrDefault("45-54 лет", 0) + 1
                        else -> ageGroups["55 лет и старше"] = ageGroups.getOrDefault("55 лет и старше", 0) + 1
                    }
                }
                ageGroups.forEach { (group, count) ->
                    if (count > 0) { addParagraph(document, "$group: $count чел.") }
                }
                addParagraph(document, "\nВсего сотрудников: ${employees.size} чел.", isBold = true)
            }
            Log.i(TAG, "[AgeDist] Content adding finished successfully.")
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "[AgeDist] Error during content generation", e)
            success = false
        } finally {
            return closeDocumentAndStream(document, writer, pdfDocument, outputStream, fileUriOrPath, resolver, success)
        }
    }

    fun generateAvailableVacanciesReport(
        context: Context,
        resolver: ContentResolver,
        vacancyData: List<VacancyInfo>
    ): Any? {
        val (outputStream, fileUriOrPath) = createPdfOutputStream(context, resolver, "AvailableVacancies")
        if (outputStream == null || fileUriOrPath == null) return null

        var writer: PdfWriter? = null
        var pdfDocument: PdfDocument? = null
        var document: Document? = null
        var success = false

        try {
            writer = PdfWriter(outputStream)
            pdfDocument = PdfDocument(writer)
            document = Document(pdfDocument)
            Log.i(TAG, "[Vacancies] Document created. Adding content...")

            addTitle(document, "Отчет: Доступные места (вакансии)")
            addParagraph(document, "Дата создания: ${DateUtils.formatDateForDisplay(LocalDate.now())}")
            document.add(Paragraph("\n"))

            if (vacancyData.isEmpty()) {
                addParagraph(document, "Нет информации о доступных вакансиях.")
            } else {
                val availableVacancies = vacancyData.filter { it.availableCount > 0 }
                if (availableVacancies.isEmpty()) {
                    addParagraph(document, "На данный момент все штатные единицы заняты.")
                } else {
                    addParagraph(document, "Список доступных вакансий:", isBold = true)
                    availableVacancies.forEach { vacancy ->
                        addParagraph(document, "${vacancy.departmentName} - ${vacancy.positionName}: ${vacancy.availableCount} мест (всего ${vacancy.maxAllowed}, занято ${vacancy.currentlyFilled})")
                    }
                }
            }
            Log.i(TAG, "[Vacancies] Content adding finished successfully.")
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "[Vacancies] Error during content generation", e)
            success = false
        } finally {
            return closeDocumentAndStream(document, writer, pdfDocument, outputStream, fileUriOrPath, resolver, success)
        }
    }
}