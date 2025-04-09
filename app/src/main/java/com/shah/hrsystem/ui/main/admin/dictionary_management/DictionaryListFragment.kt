package com.shah.hrsystem.ui.main.admin.dictionary_management

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.shah.hrsystem.R
import com.shah.hrsystem.data.db.entity.Department
import com.shah.hrsystem.data.db.entity.Language
import com.shah.hrsystem.data.db.entity.Position
import com.shah.hrsystem.databinding.FragmentDictionaryListBinding
import com.shah.hrsystem.ui.main.admin.dictionary_management.adapter.DictionaryAdapter
import com.shah.hrsystem.ui.main.admin.dictionary_management.dialog.DictionaryAddEditDialog
import com.shah.hrsystem.viewmodel.AdminDictionaryUiState
import com.shah.hrsystem.viewmodel.AdminDictionaryViewModel
import com.shah.hrsystem.viewmodel.DictionaryOperationState
import com.shah.hrsystem.viewmodel.DictionaryType
import com.shah.hrsystem.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DictionaryListFragment : Fragment() {

    private var _binding: FragmentDictionaryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminDictionaryViewModel by viewModels()
    private lateinit var dictionaryAdapter: DictionaryAdapter

    private val args: DictionaryListFragmentArgs by navArgs()
    private var currentDictionaryType: DictionaryType? = null

    private val TAG = "DictionaryListFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentDictionaryListBinding.inflate(inflater, container, false)
        currentDictionaryType = args.dictionaryType.toDictionaryType()
        Log.d(TAG, "Current Dictionary Type from args: $currentDictionaryType")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        setupToolbarWithNavigation()
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbarWithNavigation() {
        Log.d(TAG, "setupToolbarWithNavigation")
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val toolbar = binding.toolbarDict
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)

        toolbar.title = when (currentDictionaryType) {
            DictionaryType.DEPARTMENTS -> getString(R.string.manage_departments_title)
            DictionaryType.POSITIONS -> getString(R.string.manage_positions_title)
            DictionaryType.LANGUAGES -> getString(R.string.manage_languages_title)
            null -> "Справочник"
        }
        Log.d(TAG, "Toolbar title set to: ${toolbar.title}")
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView")
        dictionaryAdapter = DictionaryAdapter(
            getDepartmentName = { deptId ->
                viewModel.departments.value.find { it.id == deptId }?.name
            },
            onEditClick = { item ->
                currentDictionaryType?.let { type ->
                    Log.i(TAG, "onEditClick: Item: ${item::class.java.simpleName}, Type: $type")
                    try {
                        DictionaryAddEditDialog.show(childFragmentManager, type, item)
                        Log.d(TAG, "onEditClick: Dialog show called successfully.")
                    } catch (e: Exception) {
                        Log.e(TAG, "onEditClick: Error showing dialog", e)
                        showSnackbar("Ошибка при открытии диалога редактирования.")
                    }
                } ?: Log.e(TAG, "onEditClick: currentDictionaryType is null, cannot show dialog.")
            },
            onDeleteClick = { item ->
                Log.i(TAG, "onDeleteClick: Item: ${item::class.java.simpleName}")
                showDeleteConfirmationDialog(item)
            }
        )
        binding.rvDictionary.apply {
            adapter = dictionaryAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        Log.d(TAG, "setupFab")
        binding.fabAddDictItem.setOnClickListener {
            currentDictionaryType?.let { type ->
                Log.i(TAG, "FAB clicked. Showing Add dialog for type: $type")
                try {
                    DictionaryAddEditDialog.show(childFragmentManager, type, null)
                    Log.d(TAG, "FAB Add dialog show called successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "FAB Add: Error showing dialog", e)
                    showSnackbar("Ошибка при открытии диалога добавления.")
                }
            } ?: Log.e(TAG, "FAB clicked but currentDictionaryType is null")
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "observeViewModel")
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Observing currentDictionaryItems")
                viewModel.currentDictionaryItems.collectLatest { state ->
                    Log.d(TAG, "Received UI State: ${state::class.java.simpleName}")
                    binding.progressBarDict.isVisible = state is AdminDictionaryUiState.Loading
                    binding.rvDictionary.isVisible = state is AdminDictionaryUiState.Success
                    binding.tvEmptyDictList.isVisible = state is AdminDictionaryUiState.Success && state.items.isEmpty()

                    if (state is AdminDictionaryUiState.Success) {
                        Log.d(TAG, "Submitting list with ${state.items.size} items to adapter.")
                        dictionaryAdapter.submitList(state.items)
                    } else if (state is AdminDictionaryUiState.Error) {
                        showSnackbar(state.message)
                        Log.e(TAG, "UI State Error: ${state.message}")
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Observing operationState")
                viewModel.operationState.collect { state ->
                    Log.d(TAG, "Received Operation State: ${state::class.java.simpleName}")
                    when (state) {
                        is DictionaryOperationState.Success -> {
                            showSnackbar(state.message)
                            viewModel.clearOperationState()
                        }
                        is DictionaryOperationState.Error -> {
                            showSnackbar(state.message)
                            viewModel.clearOperationState()
                        }
                        else -> {}
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "Observing departments for Position name updates")
                viewModel.departments.collect {
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(item: Any) {
        Log.d(TAG, "showDeleteConfirmationDialog")
        val itemName = when (item) {
            is Department -> item.name
            is Position -> item.name
            is Language -> item.name
            else -> "этот элемент"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_dictionary_item_message, itemName))
            .setNegativeButton(R.string.cancel_button, null)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                Log.i(TAG, "Deletion confirmed for: $itemName")
                viewModel.deleteItem(item)
            }
            .show()
    }

    private fun showSnackbar(message: String) {
        if (view != null && _binding != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        } else {
            Log.w(TAG, "Snackbar requested but view or binding is null: $message")
        }
    }

    private fun String?.toDictionaryType(): DictionaryType? {
        return when(this) {
            Constants.DICT_TYPE_DEPARTMENTS -> DictionaryType.DEPARTMENTS
            Constants.DICT_TYPE_POSITIONS -> DictionaryType.POSITIONS
            Constants.DICT_TYPE_LANGUAGES -> DictionaryType.LANGUAGES
            else -> {
                Log.e(TAG, "Unknown dictionary type string from args: $this")
                null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        if ((activity as? AppCompatActivity)?.supportActionBar?.customView == binding.toolbarDict) {
            (activity as? AppCompatActivity)?.setSupportActionBar(null)
        }
        binding.rvDictionary.adapter = null
        _binding = null
    }
}