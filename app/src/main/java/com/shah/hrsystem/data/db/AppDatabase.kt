package com.shah.hrsystem.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shah.hrsystem.data.db.dao.*
import com.shah.hrsystem.data.db.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider // Важно импортировать javax.inject.Provider

// Аннотация Database со списком всех entities и версией БД
@Database(
    entities = [
        Department::class,
        Position::class,
        Employee::class,
        Language::class,
        EmployeeLanguage::class,
        User::class
    ],
    version = 1, // Увеличивайте версию при изменении схемы
    exportSchema = true // Рекомендуется для отслеживания истории схемы
)
abstract class AppDatabase : RoomDatabase() {

    // Абстрактные методы для получения каждого DAO
    abstract fun departmentDao(): DepartmentDao
    abstract fun positionDao(): PositionDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun languageDao(): LanguageDao
    abstract fun employeeLanguageDao(): EmployeeLanguageDao
    abstract fun userDao(): UserDao

    companion object {
        // Имя файла базы данных
        private const val DATABASE_NAME = "hr_system.db"

        // Переменная для синглтона базы данных (volatile для потокобезопасности)
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Метод для получения экземпляра базы данных
        fun getInstance(
            context: Context,
            // Используем Provider для ленивой инициализации Initializer через Hilt
            databaseInitializerProvider: Provider<DatabaseInitializer>
        ): AppDatabase {
            // Если экземпляр уже есть, возвращаем его
            // Если нет, создаем новый в синхронизированном блоке
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, databaseInitializerProvider).also { INSTANCE = it }
            }
        }

        // Метод для построения базы данных
        private fun buildDatabase(
            context: Context,
            databaseInitializerProvider: Provider<DatabaseInitializer>
        ): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // Добавляем Callback для выполнения действий при создании/открытии БД
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Запускаем инициализатор в корутине при первом создании БД
                        // Получаем экземпляр инициализатора через Provider
                        val initializer = databaseInitializerProvider.get()
                        CoroutineScope(Dispatchers.IO).launch {
                            initializer.initializeData()
                        }
                    }
                    // Можно добавить onOpen для действий при каждом открытии БД
                    // override fun onOpen(db: SupportSQLiteDatabase) {
                    //    super.onOpen(db)
                    // }
                })
                // Можно добавить миграции здесь .addMigrations(...)
                .fallbackToDestructiveMigration() // Временное решение: удаляет БД при несовпадении версии
                .build()
        }
    }
}
