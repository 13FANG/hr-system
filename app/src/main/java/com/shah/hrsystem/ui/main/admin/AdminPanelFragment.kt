package com.shah.hrsystem.ui.main.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.shah.hrsystem.databinding.FragmentAdminPanelBinding
import com.shah.hrsystem.util.Constants
import com.shah.hrsystem.viewmodel.DictionaryType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminPanelFragment : Fragment() {

    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPanelBinding.inflate(inflater, container, false)
        setupToolbar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
    }

    private fun setupToolbar() {
        binding.toolbarAdmin.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupNavigation() {
        binding.btnManageUsers.setOnClickListener {
            findNavController().navigate(AdminPanelFragmentDirections.actionAdminPanelFragmentToUserManagementFragment())
        }
        binding.btnManageDepartments.setOnClickListener {
            // Передаем тип справочника через Safe Args
            val action = AdminPanelFragmentDirections.actionAdminPanelFragmentToDictionaryListFragmentDepartments()
            // action.dictionaryType устанавливается автоматически в defaultValue="DEPARTMENTS"
            findNavController().navigate(action)
        }
        binding.btnManagePositions.setOnClickListener {
            val action = AdminPanelFragmentDirections.actionAdminPanelFragmentToDictionaryListFragmentPositions()
            findNavController().navigate(action)
        }
        binding.btnManageLanguages.setOnClickListener {
            val action = AdminPanelFragmentDirections.actionAdminPanelFragmentToDictionaryListFragmentLanguages()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}