package com.shah.hrsystem // Тот же пакет, что и тест

import android.app.Application
import android.content.Context
import android.util.Log // <-- ДОБАВИТЬ
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

// Кастомный раннер, использующий HiltTestApplication
class CustomTestRunner : AndroidJUnitRunner() {

    // ДОБАВИТЬ: Лог при создании раннера
    init {
        Log.d("CustomTestRunner", "CustomTestRunner instance created.")
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        // ДОБАВИТЬ: Лог перед вызовом super.newApplication
        Log.d("CustomTestRunner", "newApplication called. Loading HiltTestApplication...")
        // Используем HiltTestApplication вместо вашего App класса во время тестов
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
        // ДОБАВИТЬ: Лог после вызова (может не выполниться, если super бросит исключение)
        // Log.d("CustomTestRunner", "newApplication finished.")
    }

    // ДОБАВИТЬ: Лог перед запуском тестов
    override fun onStart() {
        Log.d("CustomTestRunner", "onStart called. Starting tests...")
        super.onStart()
    }
}