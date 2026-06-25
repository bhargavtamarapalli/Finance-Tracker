package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.repository.FinanceRepository
import com.example.ui.utils.CurrencyOption
import com.example.ui.utils.CurrencyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {
    private val prefs = repository.getSettingsPreferences()

    private val _appTheme = MutableStateFlow(getSavedTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _currencyOption = MutableStateFlow(getSavedCurrency())
    val currencyOption: StateFlow<CurrencyOption> = _currencyOption.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(getSavedReminderEnabled())
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val networkMonitor = com.example.ui.utils.NetworkMonitor(repository.getContext())
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private val _pendingSync = MutableStateFlow(getSavedPendingSync())
    val pendingSync: StateFlow<Boolean> = _pendingSync.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentUserId: String? = null

    val allTransactions = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary stats
    val currentMonthTransactions = repository.allTransactions.map { transactions ->
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        CurrencyUtils.selectedCurrency = getSavedCurrency()
        viewModelScope.launch {
            repository.seedDataIfNeeded()
            _isLoading.value = false
        }
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online && pendingSync.value) {
                    triggerAutoSync()
                }
            }
        }
    }

    private fun getSavedPendingSync(): Boolean {
        return prefs?.getBoolean("pending_sync_preference", false) ?: false
    }

    private fun setPendingSync(pending: Boolean) {
        _pendingSync.value = pending
        prefs?.edit()?.putBoolean("pending_sync_preference", pending)?.apply()
    }

    fun updateSession(userId: String?, isGuest: Boolean) {
        if (!isGuest && userId != null && !userId.startsWith("guest")) {
            currentUserId = userId
            if (pendingSync.value && isOnline.value) {
                triggerAutoSync()
            }
        } else {
            currentUserId = null
        }
    }

    fun triggerAutoSync() {
        val userId = currentUserId
        if (userId != null && !userId.startsWith("guest") && isOnline.value) {
            viewModelScope.launch {
                _isSyncing.value = true
                val result = repository.backupToFirebase(userId)
                _isSyncing.value = false
                if (result.isSuccess) {
                    setPendingSync(false)
                }
            }
        }
    }

    private fun getSavedTheme(): AppTheme {
        val themeStr = prefs?.getString("theme_preference", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeStr)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    private fun getSavedCurrency(): CurrencyOption {
        val currencyStr = prefs?.getString("currency_preference", CurrencyOption.INR.name) ?: CurrencyOption.INR.name
        return try {
            CurrencyOption.valueOf(currencyStr)
        } catch (e: Exception) {
            CurrencyOption.INR
        }
    }

    private fun getSavedReminderEnabled(): Boolean {
        return prefs?.getBoolean("reminder_preference", false) ?: false
    }

    fun setTheme(theme: AppTheme) {
        _appTheme.value = theme
        prefs?.edit()?.putString("theme_preference", theme.name)?.apply()
    }

    fun setCurrency(currency: CurrencyOption) {
        _currencyOption.value = currency
        CurrencyUtils.selectedCurrency = currency
        prefs?.edit()?.putString("currency_preference", currency.name)?.apply()
    }

    fun setReminderEnabled(enabled: Boolean, context: Context) {
        _reminderEnabled.value = enabled
        prefs?.edit()?.putBoolean("reminder_preference", enabled)?.apply()
        if (enabled) {
            com.example.receiver.ReminderScheduler.scheduleDailyReminder(context)
        } else {
            com.example.receiver.ReminderScheduler.cancelDailyReminder(context)
        }
    }

    fun addTransaction(amount: Double, source: String, date: Long, categoryId: Int, type: TransactionType, notes: String, paymentMethod: String = "Cash") {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(amount = amount, source = source, date = date, categoryId = categoryId, type = type, notes = notes, paymentMethod = paymentMethod)
            )
            setPendingSync(true)
            triggerAutoSync()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            setPendingSync(true)
            triggerAutoSync()
        }
    }
    
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            setPendingSync(true)
            triggerAutoSync()
        }
    }

    fun addCategory(name: String, type: TransactionType, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(
                Category(name = name, type = type, iconName = iconName)
            )
            setPendingSync(true)
            triggerAutoSync()
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
            setPendingSync(true)
            triggerAutoSync()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            setPendingSync(true)
            triggerAutoSync()
        }
    }

    suspend fun backupToFirebase(userId: String): Result<Unit> {
        return repository.backupToFirebase(userId)
    }

    suspend fun restoreFromFirebase(userId: String): Result<Unit> {
        return repository.restoreFromFirebase(userId)
    }

    suspend fun backupLocally(): Result<Unit> {
        return repository.backupLocally()
    }

    suspend fun restoreLocally(): Result<Unit> {
        return repository.restoreLocally()
    }
}

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
