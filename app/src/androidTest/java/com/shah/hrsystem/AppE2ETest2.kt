package com.shah.hrsystem

import androidx.test.ext.junit.rules.ActivityScenarioRule // Пока оставляем импорт
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log
import com.shah.hrsystem.MainActivity // Пока оставляем импорт
// import com.shah.hrsystem.R // Пока не нужен

@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class AppE2ETest2 {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // --- ВРЕМЕННО КОММЕНТИРУЕМ ActivityScenarioRule ---
    /*
    @get:Rule(order = 1)
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)
    */

    @Before
    fun init() {
        // --- ДОБАВЛЕН ЛОГ ---
        Log.d("AppE2ETest", "@Before: Attempting Hilt injection...")
        try {
            hiltRule.inject()
            Log.d("AppE2ETest", "@Before: Hilt injection SUCCESSFUL.")
        } catch (t: Throwable) {
            Log.e("AppE2ETest", "@Before: Hilt injection FAILED!", t)
            throw t // Перебрасываем, чтобы тест упал явно
        }
    }

    @Test
    fun usereditTest() { // --- ВРЕМЕННО: Переименовали тест для простоты ---
        // --- ДОБАВЛЕН ЛОГ ---
        Log.d("AppE2ETest", ">>> TEST METHOD simplestPossibleTest ENTERED <<<")

        // Временно никаких действий Espresso, только логирование
        Log.d("AppE2ETest", "Simplest test is running...")

        // Можно добавить небольшую паузу, чтобы убедиться, что лог появится
        Thread.sleep(1000)

        Log.d("AppE2ETest", "Simplest test finished.")
    }

    // --- ВРЕМЕННО КОММЕНТИРУЕМ ОРИГИНАЛЬНЫЙ ТЕСТ ---
    /*
    @Test
    fun addDepartmentAsAdmin_checkIfDisplayedInList() {
        Log.d("AppE2ETest", ">>> TEST METHOD addDepartmentAsAdmin_checkIfDisplayedInList ENTERED <<<")
        try {
            // ... остальной код теста ...
        } catch (e: Throwable) {
            Log.e("AppE2ETest", "!!! TEST FAILED WITH EXCEPTION !!!", e)
            throw e
        }
    }
    */
}