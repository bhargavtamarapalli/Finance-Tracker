package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.model.Announcement
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
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

enum class TimePeriod(val displayName: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

class FinanceViewModel(
    private val repository: FinanceRepository,
    networkMonitor: com.example.ui.utils.NetworkMonitor? = null
) : ViewModel() {
    private val prefs = repository.getSettingsPreferences()

    private val _appTheme = MutableStateFlow(getSavedTheme())
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _currencyOption = MutableStateFlow(getSavedCurrency())
    val currencyOption: StateFlow<CurrencyOption> = _currencyOption.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(getSavedReminderEnabled())
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _biometricLockEnabled = MutableStateFlow(getSavedBiometricLockEnabled())
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    private val _monthlyBudgetGoal = MutableStateFlow(prefs.getFloat("monthly_budget_goal", 100000.0f).toDouble())
    val monthlyBudgetGoal: StateFlow<Double> = _monthlyBudgetGoal.asStateFlow()

    fun updateMonthlyBudgetGoal(goal: Double) {
        prefs.edit().putFloat("monthly_budget_goal", goal.toFloat()).apply()
        _monthlyBudgetGoal.value = goal
    }


    private val _networkMonitor: com.example.ui.utils.NetworkMonitor by lazy {
        networkMonitor ?: com.example.ui.utils.NetworkMonitor(repository.getContext())
    }
    val isOnline: StateFlow<Boolean> get() = _networkMonitor.isOnline


    private val _pendingSync = MutableStateFlow(getSavedPendingSync())
    val pendingSync: StateFlow<Boolean> = _pendingSync.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Admin state management ---
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val announcementListType = Types.newParameterizedType(List::class.java, Announcement::class.java)
    private val announcementAdapter = moshi.adapter<List<Announcement>>(announcementListType)

    private val _appMode = MutableStateFlow(getSavedAppMode())
    val appMode: StateFlow<String> = _appMode.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(getSavedAnnouncements())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()
    // ------------------------------

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
    private val _selectedTimePeriod = MutableStateFlow(TimePeriod.MONTH)
    val selectedTimePeriod: StateFlow<TimePeriod> = _selectedTimePeriod.asStateFlow()

    private val _activeDate = MutableStateFlow(System.currentTimeMillis())
    val activeDate: StateFlow<Long> = _activeDate.asStateFlow()

    fun setTimePeriod(period: TimePeriod) {
        _selectedTimePeriod.value = period
        // Reset active date to now when changing period to keep it intuitive
        _activeDate.value = System.currentTimeMillis()
    }

    fun setDateDirectly(dateInMillis: Long) {
        val todayMaxCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        if (dateInMillis <= todayMaxCal.timeInMillis) {
            _activeDate.value = dateInMillis
        }
    }

    fun moveToPreviousPeriod() {
        val cal = Calendar.getInstance().apply { timeInMillis = _activeDate.value }
        when (_selectedTimePeriod.value) {
            TimePeriod.DAY -> cal.add(Calendar.DATE, -1)
            TimePeriod.WEEK -> cal.add(Calendar.DATE, -7)
            TimePeriod.MONTH -> cal.add(Calendar.MONTH, -1)
            TimePeriod.YEAR -> cal.add(Calendar.YEAR, -1)
        }
        _activeDate.value = cal.timeInMillis
    }

    fun moveToNextPeriod() {
        val cal = Calendar.getInstance().apply { timeInMillis = _activeDate.value }
        val nextCal = Calendar.getInstance().apply { timeInMillis = _activeDate.value }
        val today = Calendar.getInstance()
        when (_selectedTimePeriod.value) {
            TimePeriod.DAY -> nextCal.add(Calendar.DATE, 1)
            TimePeriod.WEEK -> nextCal.add(Calendar.DATE, 7)
            TimePeriod.MONTH -> nextCal.add(Calendar.MONTH, 1)
            TimePeriod.YEAR -> nextCal.add(Calendar.YEAR, 1)
        }

        val isFuture = when (_selectedTimePeriod.value) {
            TimePeriod.DAY -> {
                val dayStart = Calendar.getInstance().apply {
                    timeInMillis = nextCal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dayStart.timeInMillis > todayStart.timeInMillis
            }
            TimePeriod.WEEK -> {
                val nextWeekStart = Calendar.getInstance().apply {
                    timeInMillis = nextCal.timeInMillis
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayWeekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                nextWeekStart.timeInMillis > todayWeekStart.timeInMillis
            }
            TimePeriod.MONTH -> {
                val nextYear = nextCal.get(Calendar.YEAR)
                val nextMonth = nextCal.get(Calendar.MONTH)
                val currentYear = today.get(Calendar.YEAR)
                val currentMonth = today.get(Calendar.MONTH)
                nextYear > currentYear || (nextYear == currentYear && nextMonth > currentMonth)
            }
            TimePeriod.YEAR -> {
                val nextYear = nextCal.get(Calendar.YEAR)
                val currentYear = today.get(Calendar.YEAR)
                nextYear > currentYear
            }
        }

        if (!isFuture) {
            _activeDate.value = nextCal.timeInMillis
        }
    }

    val isNextPeriodEnabled = kotlinx.coroutines.flow.combine(
        _selectedTimePeriod,
        _activeDate
    ) { period, activeTime ->
        val nextCal = Calendar.getInstance().apply { timeInMillis = activeTime }
        val today = Calendar.getInstance()
        when (period) {
            TimePeriod.DAY -> {
                nextCal.add(Calendar.DATE, 1)
                val dayStart = Calendar.getInstance().apply {
                    timeInMillis = nextCal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                dayStart.timeInMillis <= todayStart.timeInMillis
            }
            TimePeriod.WEEK -> {
                nextCal.add(Calendar.DATE, 7)
                val nextWeekStart = Calendar.getInstance().apply {
                    timeInMillis = nextCal.timeInMillis
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayWeekStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                nextWeekStart.timeInMillis <= todayWeekStart.timeInMillis
            }
            TimePeriod.MONTH -> {
                nextCal.add(Calendar.MONTH, 1)
                val nextYear = nextCal.get(Calendar.YEAR)
                val nextMonth = nextCal.get(Calendar.MONTH)
                val currentYear = today.get(Calendar.YEAR)
                val currentMonth = today.get(Calendar.MONTH)
                nextYear < currentYear || (nextYear == currentYear && nextMonth <= currentMonth)
            }
            TimePeriod.YEAR -> {
                nextCal.add(Calendar.YEAR, 1)
                val nextYear = nextCal.get(Calendar.YEAR)
                val currentYear = today.get(Calendar.YEAR)
                nextYear <= currentYear
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val periodRange = kotlinx.coroutines.flow.combine(
        _selectedTimePeriod,
        _activeDate
    ) { period, activeTime ->
        val startCal = Calendar.getInstance().apply { timeInMillis = activeTime }
        val endCal = Calendar.getInstance().apply { timeInMillis = activeTime }

        when (period) {
            TimePeriod.DAY -> {
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            TimePeriod.WEEK -> {
                // Set to start of week
                startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DATE, 6)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            TimePeriod.MONTH -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            TimePeriod.YEAR -> {
                startCal.set(Calendar.DAY_OF_YEAR, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)

                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.MONTH, Calendar.DECEMBER)
                endCal.set(Calendar.DAY_OF_MONTH, 31)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
        }
        startCal.timeInMillis to endCal.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L to 0L)

    val periodLabel = kotlinx.coroutines.flow.combine(
        _selectedTimePeriod,
        _activeDate
    ) { period, activeTime ->
        val cal = Calendar.getInstance().apply { timeInMillis = activeTime }
        when (period) {
            TimePeriod.DAY -> {
                val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                sdf.format(cal.time)
            }
            TimePeriod.WEEK -> {
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = activeTime
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = startCal.timeInMillis
                    add(Calendar.DATE, 6)
                }
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                val yearSdf = SimpleDateFormat("yyyy", Locale.getDefault())
                "${sdf.format(startCal.time)} - ${sdf.format(endCal.time)}, ${yearSdf.format(endCal.time)}"
            }
            TimePeriod.MONTH -> {
                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                sdf.format(cal.time)
            }
            TimePeriod.YEAR -> {
                val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
                sdf.format(cal.time)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val periodTransactions = kotlinx.coroutines.flow.combine(
        allTransactions,
        periodRange
    ) { transactions, range ->
        val (start, end) = range
        transactions.filter { it.transaction.date in start..end }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthTransactions = repository.allTransactions.map { transactions ->
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val predictedSpending = currentMonthTransactions.map { transactions ->
        val expense = transactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        // Simple prediction logic: if today is 15th, and we spent X, then predict X * (days_in_month / 15)
        val today = Calendar.getInstance()
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
        val maxDays = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (dayOfMonth > 0) expense * (maxDays.toDouble() / dayOfMonth) else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val spendingChangePercentage = repository.allTransactions.map { transactions ->
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        
        val prevMonthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonth = prevMonthCal.get(Calendar.MONTH)
        val prevYear = prevMonthCal.get(Calendar.YEAR)
        
        val currentMonthExpense = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear && it.transaction.type == TransactionType.EXPENSE
        }.sumOf { it.transaction.amount }
        
        val prevMonthExpense = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear && it.transaction.type == TransactionType.EXPENSE
        }.sumOf { it.transaction.amount }
        
        if (prevMonthExpense > 0.0) {
            ((currentMonthExpense - prevMonthExpense) / prevMonthExpense) * 100.0
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


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
        return prefs.getBoolean("pending_sync_preference", false)
    }

    private fun setPendingSync(pending: Boolean) {
        _pendingSync.value = pending
        prefs.edit().putBoolean("pending_sync_preference", pending).apply()
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
        val themeStr = prefs.getString("theme_preference", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeStr)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    private fun getSavedCurrency(): CurrencyOption {
        val currencyStr = prefs.getString("currency_preference", CurrencyOption.INR.name) ?: CurrencyOption.INR.name
        return try {
            CurrencyOption.valueOf(currencyStr)
        } catch (e: Exception) {
            CurrencyOption.INR
        }
    }

    private fun getSavedReminderEnabled(): Boolean {
        return prefs.getBoolean("reminder_preference", false)
    }

    private fun getSavedBiometricLockEnabled(): Boolean {
        return prefs.getBoolean("biometric_lock_preference", false)
    }

    fun setTheme(theme: AppTheme) {
        _appTheme.value = theme
        prefs.edit().putString("theme_preference", theme.name).apply()
    }

    fun setCurrency(currency: CurrencyOption) {
        _currencyOption.value = currency
        CurrencyUtils.selectedCurrency = currency
        prefs.edit().putString("currency_preference", currency.name).apply()
    }

    fun setReminderEnabled(enabled: Boolean, context: Context) {
        _reminderEnabled.value = enabled
        prefs.edit().putBoolean("reminder_preference", enabled).apply()
        if (enabled) {
            com.example.receiver.ReminderScheduler.scheduleDailyReminder(context)
        } else {
            com.example.receiver.ReminderScheduler.cancelDailyReminder(context)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        prefs.edit().putBoolean("biometric_lock_preference", enabled).apply()
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

    // --- Admin helper actions ---
    fun getSavedAppMode(): String {
        return prefs.getString("admin_app_mode", "Personal Finance Ledger") ?: "Personal Finance Ledger"
    }

    fun setAppMode(mode: String) {
        _appMode.value = mode
        prefs.edit().putString("admin_app_mode", mode).apply()
    }

    fun getSavedAnnouncements(): List<Announcement> {
        val json = prefs.getString("admin_announcements", null)
        if (json.isNullOrEmpty()) {
            val defaultAnnouncements = listOf(
                Announcement(
                    id = "1",
                    title = "System Privacy Guard: Enabled",
                    content = "This platform utilizes client-side AES-256 local database encryption. Administration has zero viewing visibility or logical access to your private transaction details.",
                    category = "Privacy",
                    timestamp = System.currentTimeMillis() - 172800000 // 2 days ago
                ),
                Announcement(
                    id = "2",
                    title = "Upcoming Multi-Ledger Expansion Preview",
                    content = "The Admin Console is preparing support for specialized financial structures: Chit Fund (Chitti) tools, Group associations, and Small Business ledgers.",
                    category = "System Update",
                    timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
                )
            )
            saveAnnouncementsLocally(defaultAnnouncements)
            return defaultAnnouncements
        }
        return try {
            announcementAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun publishAnnouncement(title: String, content: String, category: String) {
        val newAnn = Announcement(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            category = category,
            timestamp = System.currentTimeMillis()
        )
        val currentList = _announcements.value.toMutableList()
        currentList.add(0, newAnn)
        _announcements.value = currentList
        saveAnnouncementsLocally(currentList)
    }

    fun deleteAnnouncement(id: String) {
        val currentList = _announcements.value.filter { it.id != id }
        _announcements.value = currentList
        saveAnnouncementsLocally(currentList)
    }

    private fun saveAnnouncementsLocally(list: List<Announcement>) {
        try {
            val json = announcementAdapter.toJson(list)
            prefs.edit().putString("admin_announcements", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seedDemoTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDemoTransactionsOnly()
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
        }
    }

    private fun isTestingEnvironment(): Boolean {
        return try {
            Class.forName("org.junit.Test")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}

class FinanceViewModelFactory(
    private val repository: FinanceRepository,
    private val networkMonitor: com.example.ui.utils.NetworkMonitor? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
