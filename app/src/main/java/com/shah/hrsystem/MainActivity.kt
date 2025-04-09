package com.shah.hrsystem

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.shah.hrsystem.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

// Аннотация Hilt для внедрения зависимостей в Activity
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Находим NavHostFragment и получаем его NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Настраиваем AppBarConfiguration, определяя top-level destinations
        // (экраны, на которых кнопка "назад" не отображается в ActionBar)
        // Пока только Login и Dashboard, можно будет добавить другие позже
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.loginFragment,
                R.id.dashboardFragment
                // Можно добавить R.id.adminPanelFragment, R.id.settingsFragment и т.д., если они top-level
            )
        )

        // Если вы решите использовать ActionBar, раскомментируйте эти строки
        // для его настройки с NavController (потребуется добавить Toolbar в activity_main.xml)
        // setSupportActionBar(binding.toolbar) // Предполагая, что у вас есть Toolbar с id 'toolbar'
        // setupActionBarWithNavController(navController, appBarConfiguration)

    }

    // Если вы используете ActionBar и хотите, чтобы кнопка "назад" работала
    // override fun onSupportNavigateUp(): Boolean {
    //     return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    // }
}
