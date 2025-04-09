package com.shah.hrsystem.ui.main.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.EmployeeConstants
import com.shah.hrsystem.data.db.entity.UserRole
import com.shah.hrsystem.databinding.FragmentDashboardBinding
import com.shah.hrsystem.ui.main.dashboard.adapter.EmployeeListAdapter
import com.shah.hrsystem.util.SessionManager
import com.shah.hrsystem.viewmodel.DashboardUiState
import com.shah.hrsystem.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var employeeAdapter: EmployeeListAdapter
    private var statusAdapter: ArrayAdapter<String>? = null
    private var departmentAdapter: ArrayAdapter<String>? = null

    @Inject // Внедряем SessionManager напрямую (хотя ViewModel уже имеет доступ к роли)
    lateinit var sessionManager: SessionManager

    private val TAG = "DashboardFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        setupToolbar() // Настройка Toolbar
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupSearch()
        observeViewModel()
        // observeUserRole() // Теперь вызывается внутри observeViewModel
    }

    // Настройка Toolbar и меню
    private fun setupToolbar() {
        binding.toolbarDashboard.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_reports -> {
                    findNavController().navigate(R.id.action_dashboardFragment_to_reportsFragment)
                    true
                }
                R.id.action_admin_panel -> {
                    findNavController().navigate(R.id.action_dashboardFragment_to_adminPanelFragment)
                    true
                }
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_dashboardFragment_to_settingsFragment)
                    true
                }
                else -> false
            }
        }
        // Нет необходимости в (activity as AppCompatActivity).setSupportActionBar(binding.toolbarDashboard),
        // так как мы обрабатываем клики напрямую и не используем NavigationUI для меню здесь.
    }

    private fun setupRecyclerView() {
        employeeAdapter = EmployeeListAdapter(
            onEmployeeClicked = { employee ->
                val action = DashboardFragmentDirections.actionDashboardFragmentToEmployeeDetailsFragment(employee.id)
                findNavController().navigate(action)
            },
            getPositionName = { posId -> viewModel.positionCache.value[posId] },
            getDepartmentName = { deptId -> viewModel.departmentCache.value[deptId] }
        )
        binding.rvEmployees.apply {
            adapter = employeeAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Добавляем слушатель прокрутки для скрытия клавиатуры
            // addOnScrollListener(object : RecyclerView.OnScrollListener() { ... } ) // Если нужно
        }
    }

    private fun setupFilters() {
        // Статусы
        val statuses = listOf(
            getString(R.string.status_all),
            getString(R.string.status_new),
            getString(R.string.status_accepted)
        )
        statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statuses)
        binding.actvFilterStatus.setAdapter(statusAdapter)
        binding.actvFilterStatus.setOnItemClickListener { _, _, position, _ ->
            val selectedStatus = when (position) {
                1 -> EmployeeConstants.STATUS_NEW
                2 -> EmployeeConstants.STATUS_ACCEPTED
                else -> null // "Все статусы"
            }
            viewModel.applyStatusFilter(selectedStatus)
        }

        // Отделы (динамически из ViewModel)
        departmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.actvFilterDepartment.setAdapter(departmentAdapter)
        binding.actvFilterDepartment.setOnItemClickListener { _, _, position, _ ->
            val selectedDepartmentId = if (position == 0) {
                null // "Все отделы"
            } else {
                viewModel.departments.value.getOrNull(position - 1)?.id // -1, т.к. добавили "Все отделы"
            }
            viewModel.applyDepartmentFilter(selectedDepartmentId)
        }
        // Инициализируем списки фильтров
        binding.actvFilterStatus.setText(getString(R.string.status_all), false)
        binding.actvFilterDepartment.setText(getString(R.string.department_all), false)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener {
            viewModel.applySearchQuery(it.toString())
        }
        // Можно добавить обработку нажатия Enter для поиска, если нужно
        // binding.etSearch.setOnEditorActionListener { ... }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за состоянием списка
                viewModel.uiState.collectLatest { state ->
                    binding.progressBarDashboard.isVisible = state is DashboardUiState.Loading
                    binding.rvEmployees.isVisible = state is DashboardUiState.Success
                    binding.tvEmptyList.isVisible = state is DashboardUiState.Success && state.employees.isEmpty()

                    if (state is DashboardUiState.Success) {
                        employeeAdapter.submitList(state.employees)
                    } else if (state is DashboardUiState.Error) {
                        showSnackbar(state.message)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Наблюдаем за списком отделов для фильтра
                viewModel.departments.drop(1).distinctUntilChanged().collectLatest { departments -> // drop(1) чтобы не реагировать на initial emptyList
                    val departmentNames = mutableListOf(getString(R.string.department_all))
                    departmentNames.addAll(departments.map { it.name })
                    departmentAdapter?.clear()
                    departmentAdapter?.addAll(departmentNames)
                    departmentAdapter?.notifyDataSetChanged()
                    // Сбрасываем выбор, если список обновился (можно сделать умнее)
                    // binding.actvFilterDepartment.setText(getString(R.string.department_all), false)
                }
            }
        }

        // ИЗМЕНЕНО: Наблюдаем за ролью пользователя здесь же
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentUserRole.collectLatest { role ->
                    val menu = binding.toolbarDashboard.menu
                    val adminMenuItem = menu?.findItem(R.id.action_admin_panel)
                    if (adminMenuItem != null) {
                        val isAdmin = role == UserRole.ADMIN
                        adminMenuItem.isVisible = isAdmin
                        Log.d(TAG, "Updating admin panel visibility: Role=$role, IsAdmin=$isAdmin, Visible=${adminMenuItem.isVisible}")
                    } else {
                        Log.w(TAG, "Admin menu item not found when trying to update visibility.")
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        // Проверяем, что view еще доступен
        if (view != null && _binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        } else {
            Log.w(TAG, "Snackbar requested but view or binding is null: $message")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем адаптеры, чтобы избежать утечек контекста
        binding.rvEmployees.adapter = null
        binding.actvFilterStatus.setAdapter(null)
        binding.actvFilterDepartment.setAdapter(null)
        statusAdapter = null
        departmentAdapter = null
        _binding = null
    }
}