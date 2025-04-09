package com.shah.hrsystem.di

import android.content.Context
import androidx.room.Room
import com.shah.hrsystem.data.db.AppDatabase
import com.shah.hrsystem.data.db.DatabaseInitializer
import com.shah.hrsystem.data.db.dao.*
import com.shah.hrsystem.data.repository.*
import com.shah.hrsystem.util.SessionManager // Добавлен импорт SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Предоставляем экземпляр AppDatabase как синглтон
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext appContext: Context,
        databaseInitializerProvider: Provider<DatabaseInitializer>
    ): AppDatabase {
        return AppDatabase.getInstance(appContext, databaseInitializerProvider)
    }

    // Предоставляем DatabaseInitializer
    // ИСПРАВЛЕНО: Добавлены employeeDao и employeeLanguageDao в параметры и вызов конструктора
    @Provides
    @Singleton
    fun provideDatabaseInitializer(
        departmentDao: DepartmentDao,
        positionDao: PositionDao,
        languageDao: LanguageDao,
        userDao: UserDao,
        employeeDao: EmployeeDao, // <-- Добавлено
        employeeLanguageDao: EmployeeLanguageDao // <-- Добавлено
    ): DatabaseInitializer {
        // Hilt сам создаст экземпляр, внедрив все DAO
        return DatabaseInitializer(
            departmentDao,
            positionDao,
            languageDao,
            userDao,
            employeeDao, // <-- Добавлено
            employeeLanguageDao // <-- Добавлено
        )
    }

    // Предоставляем SessionManager как синглтон
    @Provides
    @Singleton
    fun provideSessionManager(): SessionManager {
        return SessionManager()
    }

    // --- Предоставление DAO ---

    @Provides
    @Singleton
    fun provideDepartmentDao(db: AppDatabase): DepartmentDao = db.departmentDao()

    @Provides
    @Singleton
    fun providePositionDao(db: AppDatabase): PositionDao = db.positionDao()

    @Provides
    @Singleton
    fun provideEmployeeDao(db: AppDatabase): EmployeeDao = db.employeeDao()

    @Provides
    @Singleton
    fun provideLanguageDao(db: AppDatabase): LanguageDao = db.languageDao()

    @Provides
    @Singleton
    fun provideEmployeeLanguageDao(db: AppDatabase): EmployeeLanguageDao = db.employeeLanguageDao()

    @Provides
    @Singleton
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()


    // --- Предоставление Репозиториев ---

    @Provides
    @Singleton
    fun provideAuthRepository(userDao: UserDao): AuthRepository {
        return AuthRepository(userDao)
    }

    @Provides
    @Singleton
    fun provideDictionaryRepository(
        departmentDao: DepartmentDao,
        positionDao: PositionDao,
        languageDao: LanguageDao
    ): DictionaryRepository {
        return DictionaryRepository(departmentDao, positionDao, languageDao)
    }

    @Provides
    @Singleton
    fun provideEmployeeRepository(
        employeeDao: EmployeeDao,
        employeeLanguageDao: EmployeeLanguageDao,
        positionDao: PositionDao
    ): EmployeeRepository {
        return EmployeeRepository(employeeDao, employeeLanguageDao, positionDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(userDao: UserDao): SettingsRepository {
        return SettingsRepository(userDao)
    }

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepository(userDao)
    }
}