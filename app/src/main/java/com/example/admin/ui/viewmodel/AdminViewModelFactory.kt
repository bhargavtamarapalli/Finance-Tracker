package com.example.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AuthRepository
import com.example.data.repository.FinanceRepository
import com.example.admin.data.repository.IAdminActionsRepository

/**
 * Factory for creating [AdminViewModel] with required repository dependencies.
 */
class AdminViewModelFactory(
    private val financeRepository: FinanceRepository,
    private val actionsRepository: IAdminActionsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            return AdminViewModel(financeRepository, actionsRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
