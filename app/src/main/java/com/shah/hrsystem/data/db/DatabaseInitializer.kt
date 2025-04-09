package com.shah.hrsystem.data.db

import android.util.Log
import com.shah.hrsystem.data.db.dao.*
import com.shah.hrsystem.data.db.entity.*
import com.shah.hrsystem.util.DateUtils // Убедимся, что импорт есть
import com.shah.hrsystem.util.PasswordHasher
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random // Для случайных тарифных разрядов

@Singleton
class DatabaseInitializer @Inject constructor(
    private val departmentDao: DepartmentDao,
    private val positionDao: PositionDao,
    private val languageDao: LanguageDao,
    private val userDao: UserDao,
    private val employeeDao: EmployeeDao,
    private val employeeLanguageDao: EmployeeLanguageDao
) {
    private val TAG = "DatabaseInitializer"

    suspend fun initializeData() {
        try {
            Log.i(TAG, "Starting database initialization...")
            // 1. Отделы
            val departments = listOf(
                Department(name = "Кафедра ИВТ"),
                Department(name = "Кафедра ПМ"),
                Department(name = "Кафедра КБ"),
                Department(name = "Администрация")
            )
            departments.forEach {
                try { departmentDao.insert(it) } catch (e: Exception) { Log.w(TAG, "Department likely already exists: ${it.name}") }
            }
            val actualDepartments = departmentDao.getAllDepartments().firstOrNull() ?: departments
            val ivtDeptId = actualDepartments.find { it.name == "Кафедра ИВТ" }?.id ?: 1
            val pmDeptId = actualDepartments.find { it.name == "Кафедра ПМ" }?.id ?: 2
            val kbDeptId = actualDepartments.find { it.name == "Кафедра КБ" }?.id ?: 3
            val adminDeptId = actualDepartments.find { it.name == "Администрация" }?.id ?: 4
            Log.i(TAG, "Departments initialization attempted. IDs: IVT=$ivtDeptId, PM=$pmDeptId, KB=$kbDeptId, Admin=$adminDeptId")


            // 2. Языки
            val languages = listOf(
                Language(name = "Английский"),
                Language(name = "Немецкий"),
                Language(name = "Французский"),
                Language(name = "Русский"),
                Language(name = "Китайский")
            )
            languages.forEach {
                try { languageDao.insert(it) } catch (e: Exception) { Log.w(TAG, "Language likely already exists: ${it.name}") }
            }
            val actualLanguages = languageDao.getAllLanguages().firstOrNull() ?: languages
            val langEnglish = actualLanguages.find { it.name == "Английский" }
            val langGerman = actualLanguages.find { it.name == "Немецкий" }
            val langFrench = actualLanguages.find { it.name == "Французский" } // Добавлен
            val langRussian = actualLanguages.find { it.name == "Русский" }
            val langChinese = actualLanguages.find { it.name == "Китайский" } // Добавлен
            Log.i(TAG, "Languages initialization attempted. IDs: Eng=${langEnglish?.id}, Ger=${langGerman?.id}, Fra=${langFrench?.id}, Rus=${langRussian?.id}, Chi=${langChinese?.id}")


            // 3. Должности
            val positions = listOf(
                Position(name = "Ассистент", departmentId = ivtDeptId, maxAllowed = 5, requiresHigherEducation = 1, isAssistant = 1),
                Position(name = "Старший преподаватель", departmentId = ivtDeptId, maxAllowed = 3, requiresHigherEducation = 1, isAssistant = 0),
                Position(name = "Доцент", departmentId = ivtDeptId, maxAllowed = 2, requiresHigherEducation = 1, isAssistant = 0),
                Position(name = "Профессор", departmentId = ivtDeptId, maxAllowed = 1, requiresHigherEducation = 1, isAssistant = 0),
                Position(name = "Ассистент", departmentId = pmDeptId, maxAllowed = 4, requiresHigherEducation = 1, isAssistant = 1),
                Position(name = "Старший преподаватель", departmentId = pmDeptId, maxAllowed = 4, requiresHigherEducation = 1, isAssistant = 0),
                Position(name = "Доцент", departmentId = pmDeptId, maxAllowed = 3, requiresHigherEducation = 1, isAssistant = 0),
                Position(name = "Ассистент", departmentId = kbDeptId, maxAllowed = 6, requiresHigherEducation = 1, isAssistant = 1),
                Position(name = "Доцент", departmentId = kbDeptId, maxAllowed = 2, requiresHigherEducation = 1, isAssistant = 0),
                Position(name = "Специалист HR", departmentId = adminDeptId, maxAllowed = 2, requiresHigherEducation = 0, isAssistant = 0),
                Position(name = "Администратор системы", departmentId = adminDeptId, maxAllowed = 1, requiresHigherEducation = 0, isAssistant = 0)
            )
            positions.forEach {
                try { positionDao.insert(it) } catch (e: Exception) { Log.w(TAG, "Position likely already exists: ${it.name} in dept ${it.departmentId}") }
            }
            val actualPositions = positionDao.getAllPositions().firstOrNull() ?: positions
            // Получаем ID нужных должностей
            val posDocentIvt = actualPositions.find { it.name == "Доцент" && it.departmentId == ivtDeptId }
            val posStPrepIvt = actualPositions.find { it.name == "Старший преподаватель" && it.departmentId == ivtDeptId }
            val posProfIvt = actualPositions.find { it.name == "Профессор" && it.departmentId == ivtDeptId }
            val posAssistantPm = actualPositions.find { it.name == "Ассистент" && it.departmentId == pmDeptId }
            val posStPrepPm = actualPositions.find { it.name == "Старший преподаватель" && it.departmentId == pmDeptId }
            val posDocentPm = actualPositions.find { it.name == "Доцент" && it.departmentId == pmDeptId }
            val posAssistantKb = actualPositions.find { it.name == "Ассистент" && it.departmentId == kbDeptId }
            val posDocentKb = actualPositions.find { it.name == "Доцент" && it.departmentId == kbDeptId }
            val posHrSpec = actualPositions.find { it.name == "Специалист HR" && it.departmentId == adminDeptId }
            Log.i(TAG, "Positions initialization attempted.")


            // 4. Пользователь Администратор (код без изменений)
            val adminLogin = "admin"; val adminPassword = "password123"
            if (userDao.getUserByLogin(adminLogin) == null) { userDao.insert(User(login = adminLogin, passwordHash = PasswordHasher.hashPassword(adminPassword), role = UserRole.ADMIN)); Log.i(TAG, "Admin user created") } else Log.i(TAG, "Admin user exists.")

            // 5. Пользователь HR (код без изменений)
            val hrLogin = "hr"; val hrPassword = "password456"
            if (userDao.getUserByLogin(hrLogin) == null) { userDao.insert(User(login = hrLogin, passwordHash = PasswordHasher.hashPassword(hrPassword), role = UserRole.HR)); Log.i(TAG, "HR user created") } else Log.i(TAG, "HR user exists.")

            // 6. Добавление сотрудников
            Log.i(TAG, "Attempting to add sample employees...")

            // Проверяем наличие нужных должностей и языков
            if (posDocentIvt == null || posAssistantPm == null || posHrSpec == null || langEnglish == null || langGerman == null || langRussian == null || posStPrepIvt == null || posProfIvt == null || posStPrepPm == null || posDocentPm == null || posAssistantKb == null || posDocentKb == null || langFrench == null || langChinese == null) {
                Log.e(TAG, "Could not find ALL required positions or languages to create sample employees. Skipping.")
            } else {
                // --- СУЩЕСТВУЮЩИЕ СОТРУДНИКИ ---
                val employeesToAdd = mutableListOf<Pair<Employee, List<EmployeeLanguage>>>()
                employeesToAdd.add(Pair(
                    Employee(firstName = "Иван", lastName = "Петров", dateOfBirth = "1985-05-15", gender = EmployeeConstants.GENDER_MALE, positionId = posDocentIvt.id, departmentId = posDocentIvt.departmentId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 15, academicExperience = 10, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = "2010-09-01", tariffRate = 15),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Мария", lastName = "Сидорова", dateOfBirth = "1998-11-20", gender = EmployeeConstants.GENDER_FEMALE, positionId = posAssistantPm.id, departmentId = posAssistantPm.departmentId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 3, academicExperience = 1, status = EmployeeConstants.STATUS_NEW),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.INTERMEDIATE), EmployeeLanguage(employeeId = 0, languageId = langGerman.id, proficiencyLevel = ProficiencyLevel.ELEMENTARY), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Сергей", lastName = "Кузнецов", dateOfBirth = "1992-03-01", gender = EmployeeConstants.GENDER_MALE, positionId = posHrSpec.id, departmentId = posHrSpec.departmentId, educationLevel = EmployeeConstants.EDU_SECONDARY, totalExperience = 8, academicExperience = 0, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusMonths(6)), tariffRate = 10),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))

                // --- НОВЫЕ ПРИНЯТЫЕ СОТРУДНИКИ (10) ---
                employeesToAdd.add(Pair(
                    Employee(firstName = "Елена", lastName = "Васильева", dateOfBirth = "1988-07-25", gender = EmployeeConstants.GENDER_FEMALE, positionId = posStPrepIvt.id, departmentId = ivtDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 12, academicExperience = 8, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(5)), tariffRate = 14),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.FLUENT), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Алексей", lastName = "Смирнов", dateOfBirth = "1975-02-10", gender = EmployeeConstants.GENDER_MALE, positionId = posProfIvt.id, departmentId = ivtDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 25, academicExperience = 20, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(15)), tariffRate = 18),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langGerman.id, proficiencyLevel = ProficiencyLevel.INTERMEDIATE), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Ольга", lastName = "Михайлова", dateOfBirth = "1990-12-03", gender = EmployeeConstants.GENDER_FEMALE, positionId = posStPrepPm.id, departmentId = pmDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 10, academicExperience = 5, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(3)), tariffRate = 13),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langFrench.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Дмитрий", lastName = "Федоров", dateOfBirth = "1983-09-18", gender = EmployeeConstants.GENDER_MALE, positionId = posDocentPm.id, departmentId = pmDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 16, academicExperience = 11, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(7)), tariffRate = 16),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.INTERMEDIATE), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Светлана", lastName = "Иванова", dateOfBirth = "1995-04-22", gender = EmployeeConstants.GENDER_FEMALE, positionId = posAssistantKb.id, departmentId = kbDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 5, academicExperience = 2, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(2)), tariffRate = 9),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Андрей", lastName = "Волков", dateOfBirth = "1980-01-05", gender = EmployeeConstants.GENDER_MALE, positionId = posDocentKb.id, departmentId = kbDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 20, academicExperience = 15, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(10)), tariffRate = 17),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langChinese.id, proficiencyLevel = ProficiencyLevel.ELEMENTARY), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Наталья", lastName = "Зайцева", dateOfBirth = "1993-06-30", gender = EmployeeConstants.GENDER_FEMALE, positionId = posStPrepPm.id, departmentId = pmDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 7, academicExperience = 4, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(4)), tariffRate = 12),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langGerman.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Роман", lastName = "Соколов", dateOfBirth = "1986-08-11", gender = EmployeeConstants.GENDER_MALE, positionId = posStPrepIvt.id, departmentId = ivtDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 14, academicExperience = 9, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(6)), tariffRate = 14),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.INTERMEDIATE), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Виктория", lastName = "Новикова", dateOfBirth = "1999-03-14", gender = EmployeeConstants.GENDER_FEMALE, positionId = posAssistantPm.id, departmentId = pmDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 2, academicExperience = 1, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusMonths(10)), tariffRate = 8),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Максим", lastName = "Козлов", dateOfBirth = "1989-10-01", gender = EmployeeConstants.GENDER_MALE, positionId = posDocentPm.id, departmentId = pmDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 11, academicExperience = 7, status = EmployeeConstants.STATUS_ACCEPTED, employmentDate = DateUtils.formatLocalDateToIso(LocalDate.now().minusYears(5)), tariffRate = 15),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langFrench.id, proficiencyLevel = ProficiencyLevel.INTERMEDIATE), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))

                // --- НОВЫЕ ЗАЯВКИ (3) ---
                employeesToAdd.add(Pair(
                    Employee(firstName = "Анастасия", lastName = "Лебедева", dateOfBirth = "2000-07-07", gender = EmployeeConstants.GENDER_FEMALE, positionId = posAssistantKb.id, departmentId = kbDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 1, academicExperience = 0, status = EmployeeConstants.STATUS_NEW),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.FLUENT), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Егор", lastName = "Морозов", dateOfBirth = "1997-12-25", gender = EmployeeConstants.GENDER_MALE, positionId = posAssistantKb.id, departmentId = ivtDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 4, academicExperience = 1, status = EmployeeConstants.STATUS_NEW), // Предполагаем, что есть Ассистент ИВТ
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.INTERMEDIATE), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))
                employeesToAdd.add(Pair(
                    Employee(firstName = "Полина", lastName = "Павлова", dateOfBirth = "1996-05-09", gender = EmployeeConstants.GENDER_FEMALE, positionId = posStPrepPm.id, departmentId = pmDeptId, educationLevel = EmployeeConstants.EDU_HIGHER, totalExperience = 5, academicExperience = 3, status = EmployeeConstants.STATUS_NEW),
                    listOf(EmployeeLanguage(employeeId = 0, languageId = langEnglish.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langGerman.id, proficiencyLevel = ProficiencyLevel.ADVANCED), EmployeeLanguage(employeeId = 0, languageId = langRussian.id, proficiencyLevel = ProficiencyLevel.NATIVE))
                ))


                // --- ВСТАВКА ВСЕХ СОТРУДНИКОВ ---
                var insertedCount = 0
                for ((employeeData, languageData) in employeesToAdd) {
                    val existingEmployee = employeeDao.getAllEmployeesFlow().firstOrNull()
                        ?.find { it.lastName == employeeData.lastName && it.firstName == employeeData.firstName && it.dateOfBirth == employeeData.dateOfBirth }

                    if (existingEmployee == null) {
                        try {
                            val generatedEmployeeId = employeeDao.insert(employeeData)
                            if (generatedEmployeeId > 0) {
                                insertedCount++
                                val employeeIdInt = generatedEmployeeId.toInt()
                                // Log.i(TAG, "Inserted employee ${employeeData.lastName} with ID: $employeeIdInt") // Убрал лог для краткости
                                if (languageData.isNotEmpty()) {
                                    val languagesWithId = languageData.map { it.copy(employeeId = employeeIdInt) }
                                    employeeLanguageDao.insertAll(languagesWithId)
                                }
                                if (employeeData.status == EmployeeConstants.STATUS_ACCEPTED) {
                                    val employeeToUpdate = employeeData.copy(id = employeeIdInt, personalNumber = employeeIdInt)
                                    employeeDao.update(employeeToUpdate)
                                }
                            } else {
                                Log.e(TAG, "Failed to insert employee ${employeeData.lastName}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting employee ${employeeData.lastName} or their languages", e)
                        }
                    } else {
                        // Log.w(TAG, "Employee ${employeeData.lastName} ${employeeData.firstName} likely already exists. Skipping insertion.") // Убрал лог
                    }
                }
                Log.i(TAG, "Finished adding sample employees. Attempted to insert ${employeesToAdd.size}, actually inserted: $insertedCount")
            }

            Log.i(TAG, "Database initialization finished successfully.")

        } catch (e: Exception) {
            Log.e(TAG, "Critical error during database initialization", e)
        }
    }
}