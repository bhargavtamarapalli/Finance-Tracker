package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.Category
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TimePeriod
import com.example.ui.components.*
import com.example.ui.utils.CurrencyUtils
import com.example.ui.utils.getIconByName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Apps
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

enum class FilterType(val displayName: String) {
    ALL("All Types"),
    INCOME("Income Only"),
    EXPENSE("Expense Only")
}

enum class DateFilter(val displayName: String) {
    ACTIVE_PERIOD("Active Period"),
    ALL("All Time")
}

@Composable
fun TransactionHistoryScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    onMenuClick: () -> Unit = {}
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val selectedTimePeriod by viewModel.selectedTimePeriod.collectAsStateWithLifecycle()
    val periodLabel by viewModel.periodLabel.collectAsStateWithLifecycle()
    val activeDate by viewModel.activeDate.collectAsStateWithLifecycle()
    val isNextPeriodEnabled by viewModel.isNextPeriodEnabled.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val predictedSpending by viewModel.predictedSpending.collectAsStateWithLifecycle()
    val spendingChangePercentage by viewModel.spendingChangePercentage.collectAsStateWithLifecycle()

    TransactionHistoryContent(
        allTransactions = transactions,
        periodTransactions = periodTransactions,
        selectedTimePeriod = selectedTimePeriod,
        periodLabel = periodLabel,
        activeDate = activeDate,
        isNextPeriodEnabled = isNextPeriodEnabled,
        categories = categories,
        predictedSpending = predictedSpending,
        spendingChangePercentage = spendingChangePercentage,
        onMenuClick = onMenuClick,
        onPeriodSelected = { viewModel.setTimePeriod(it) },
        onPreviousClick = { viewModel.moveToPreviousPeriod() },
        onNextClick = { viewModel.moveToNextPeriod() },
        onDateSelected = { viewModel.setDateDirectly(it) },
        onEditTransactionClick = { item ->
            navController.navigate("add_transaction/${item.transaction.type.name}?transactionId=${item.transaction.id}")
        },
        onDuplicateTransactionClick = { item ->
            navController.navigate("add_transaction/${item.transaction.type.name}?transactionId=${item.transaction.id}&duplicate=true")
        },
        onDeleteTransactionClick = { item ->
            viewModel.deleteTransaction(item.transaction)
        },
        onAddTransactionClick = {
            navController.navigate("add_transaction/EXPENSE")
        },
        onAddCategory = { name, type, icon ->
            viewModel.addCategory(name, type, icon)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryContent(
    allTransactions: List<TransactionWithCategory>,
    periodTransactions: List<TransactionWithCategory>,
    selectedTimePeriod: TimePeriod,
    periodLabel: String,
    activeDate: Long,
    isNextPeriodEnabled: Boolean,
    categories: List<Category>,
    predictedSpending: Double,
    spendingChangePercentage: Double,
    onMenuClick: () -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onEditTransactionClick: (TransactionWithCategory) -> Unit,
    onDuplicateTransactionClick: (TransactionWithCategory) -> Unit,
    onDeleteTransactionClick: (TransactionWithCategory) -> Unit,
    onAddTransactionClick: () -> Unit,
    onAddCategory: (String, TransactionType, String) -> Unit
) {
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var headerHeightPx by remember { mutableStateOf(0f) }
    var headerOffsetPx by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember(headerHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (headerHeightPx > 0f) {
                    val oldOffset = headerOffsetPx
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    headerOffsetPx = newOffset
                    if (delta > 0 && oldOffset < 0f) {
                        val consumed = newOffset - oldOffset
                        return Offset(0f, consumed)
                    }
                }
                return Offset.Zero
            }
        }
    }
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(SortOption.DATE_DESC) }
    var selectedFilterType by remember { mutableStateOf(FilterType.ALL) }
    var selectedCategoryNames by remember { mutableStateOf(emptySet<String>()) }
    var selectedDateFilter by remember { mutableStateOf(DateFilter.ACTIVE_PERIOD) }
    var maxPriceLimit by remember { mutableStateOf(10000.0) }
    var showDatePicker by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val processedTransactions = remember(
        allTransactions, periodTransactions, searchQuery, selectedSort, selectedFilterType, selectedCategoryNames, selectedDateFilter, maxPriceLimit
    ) {
        var list = if (selectedDateFilter == DateFilter.ACTIVE_PERIOD) periodTransactions else allTransactions

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.transaction.source.contains(searchQuery, ignoreCase = true) ||
                it.transaction.notes.contains(searchQuery, ignoreCase = true) ||
                (it.category?.name?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        when (selectedFilterType) {
            FilterType.INCOME -> list = list.filter { it.transaction.type == TransactionType.INCOME }
            FilterType.EXPENSE -> list = list.filter { it.transaction.type == TransactionType.EXPENSE }
            FilterType.ALL -> {}
        }

        if (selectedCategoryNames.isNotEmpty()) {
            list = list.filter { it.category?.name != null && selectedCategoryNames.contains(it.category.name) }
        }

        if (maxPriceLimit < 10000.0) {
            list = list.filter { it.transaction.amount <= maxPriceLimit }
        }

        list = when (selectedSort) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.transaction.date }
            SortOption.DATE_ASC -> list.sortedBy { it.transaction.date }
            SortOption.AMOUNT_DESC -> list.sortedByDescending { it.transaction.amount }
            SortOption.AMOUNT_ASC -> list.sortedBy { it.transaction.amount }
        }
        list
    }

    val dynamicPredictedSpending = remember(processedTransactions) {
        val filteredCurrentMonthExpenses = processedTransactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear && it.transaction.type == TransactionType.EXPENSE
        }.sumOf { it.transaction.amount }

        val today = Calendar.getInstance()
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
        val maxDays = today.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (dayOfMonth > 0) filteredCurrentMonthExpenses * (maxDays.toDouble() / dayOfMonth) else 0.0
    }

    val dynamicSpendingChangePercentage = remember(processedTransactions, allTransactions, selectedCategoryNames, maxPriceLimit) {
        val filteredCurrentMonthExpenses = processedTransactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear && it.transaction.type == TransactionType.EXPENSE
        }.sumOf { it.transaction.amount }

        val prevMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevMonth = prevMonthCal.get(Calendar.MONTH)
        val prevYear = prevMonthCal.get(Calendar.YEAR)

        val filteredPrevMonthExpenses = allTransactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear && 
            it.transaction.type == TransactionType.EXPENSE &&
            (selectedCategoryNames.isEmpty() || (it.category?.name != null && selectedCategoryNames.contains(it.category.name))) &&
            (maxPriceLimit >= 10000.0 || it.transaction.amount <= maxPriceLimit)
        }.sumOf { it.transaction.amount }

        if (filteredPrevMonthExpenses > 0.0) {
            ((filteredCurrentMonthExpenses - filteredPrevMonthExpenses) / filteredPrevMonthExpenses) * 100.0
        } else {
            0.0
        }
    }

    val groupedTransactions = remember(processedTransactions) {
        processedTransactions.groupBy { item ->
            val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(item.transaction.date))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    top = with(LocalDensity.current) { headerHeightPx.toDp() } + 4.dp,
                    bottom = 80.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                groupedTransactions.forEach { (dateHeader, items) ->
                    item {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                items.forEachIndexed { index, item ->
                                    TransactionItem(
                                        transaction = item,
                                        modifier = Modifier.clickable { selectedTransactionForDetails = item },
                                        verticalPadding = 0.dp,
                                        shape = RoundedCornerShape(0.dp),
                                        containerColor = Color.Transparent,
                                        border = null
                                    )
                                    if (index < items.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        headerHeightPx = coordinates.size.height.toFloat()
                    }
                    .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                            .clickable { showFilterSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isAllSelected = selectedCategoryNames.isEmpty() && selectedFilterType == FilterType.ALL && maxPriceLimit >= 10000.0
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { 
                                selectedCategoryNames = emptySet()
                                selectedFilterType = FilterType.ALL
                                maxPriceLimit = 10000.0
                            },
                            label = { Text("All Categories") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.GridOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    
                    item {
                        val isIncomeSelected = selectedFilterType == FilterType.INCOME
                        FilterChip(
                            selected = isIncomeSelected,
                            onClick = { selectedFilterType = FilterType.INCOME },
                            label = { Text("Income") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isIncomeSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    item {
                        val isExpenseSelected = selectedFilterType == FilterType.EXPENSE
                        FilterChip(
                            selected = isExpenseSelected,
                            onClick = { selectedFilterType = FilterType.EXPENSE },
                            label = { Text("Expense") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isExpenseSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    items(categories.map { it.name }.distinct()) { catName ->
                        val isSelected = selectedCategoryNames.contains(catName)
                        val cat = categories.firstOrNull { it.name == catName }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryNames = if (isSelected) {
                                    selectedCategoryNames - catName
                                } else {
                                    selectedCategoryNames + catName
                                }
                            },
                            label = { Text(catName) },
                            leadingIcon = cat?.let {
                                {
                                    Icon(
                                        imageVector = getIconByName(it.iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    onClick = { showDatePicker = true },
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    if (showFilterSheet) {
        var tempFilterType by remember { mutableStateOf(selectedFilterType) }
        var tempCategoryNames by remember { mutableStateOf(selectedCategoryNames) }
        var tempMaxPriceLimit by remember { mutableStateOf(maxPriceLimit) }
        var tempSearchQuery by remember { mutableStateOf(searchQuery) }
        var tempDateFilter by remember { mutableStateOf(selectedDateFilter) }

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
        ) {
            FilterScreen(
                categories = categories,
                selectedFilterType = tempFilterType,
                onFilterTypeSelected = { tempFilterType = it },
                selectedCategoryNames = tempCategoryNames,
                onCategorySelected = { tempCategoryNames = it },
                maxPriceLimit = tempMaxPriceLimit,
                onMaxPriceLimitSelected = { tempMaxPriceLimit = it },
                searchQuery = tempSearchQuery,
                onSearchQueryChange = { tempSearchQuery = it },
                selectedDateFilter = tempDateFilter,
                onDateFilterSelected = { tempDateFilter = it },
                periodLabel = periodLabel,
                predictedSpending = dynamicPredictedSpending,
                spendingChangePercentage = dynamicSpendingChangePercentage,
                onApply = {
                    selectedFilterType = tempFilterType
                    selectedCategoryNames = tempCategoryNames
                    maxPriceLimit = tempMaxPriceLimit
                    searchQuery = tempSearchQuery
                    selectedDateFilter = tempDateFilter
                    showFilterSheet = false
                },
                onReset = {
                    tempFilterType = FilterType.ALL
                    tempCategoryNames = emptySet()
                    tempMaxPriceLimit = 10000.0
                    tempSearchQuery = ""
                    tempDateFilter = DateFilter.ACTIVE_PERIOD
                },
                onDismiss = { showFilterSheet = false },
                onSelectCustomPeriodClick = { showDatePicker = true },
                onAddCategory = onAddCategory
            )
        }
    }

    if (showDatePicker) {
        CustomPeriodPickerDialog(
            timePeriod = selectedTimePeriod,
            activeDate = activeDate,
            onDateSelected = {
                onDateSelected(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (selectedTransactionForDetails != null) {
        TransactionDetailsBottomSheet(
            transactionWithCat = selectedTransactionForDetails!!,
            onDismiss = { selectedTransactionForDetails = null },
            onEdit = { 
                onEditTransactionClick(selectedTransactionForDetails!!)
                selectedTransactionForDetails = null
            },
            onDuplicate = { 
                onDuplicateTransactionClick(selectedTransactionForDetails!!)
                selectedTransactionForDetails = null
            },
            onDelete = { 
                onDeleteTransactionClick(selectedTransactionForDetails!!)
                selectedTransactionForDetails = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterScreen(
    categories: List<Category>,
    selectedFilterType: FilterType,
    onFilterTypeSelected: (FilterType) -> Unit,
    selectedCategoryNames: Set<String>,
    onCategorySelected: (Set<String>) -> Unit,
    maxPriceLimit: Double,
    onMaxPriceLimitSelected: (Double) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedDateFilter: DateFilter,
    onDateFilterSelected: (DateFilter) -> Unit,
    periodLabel: String,
    predictedSpending: Double,
    spendingChangePercentage: Double,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onSelectCustomPeriodClick: () -> Unit,
    onAddCategory: (String, TransactionType, String) -> Unit
) {
    var showMoreCategoriesDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDateFilterDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Filter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onReset) {
                Text("RESET", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Column {
                Text("DATE RANGE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    onClick = { showDateFilterDropdown = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedDateFilter == DateFilter.ACTIVE_PERIOD) periodLabel else "All Time",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedDateFilter == DateFilter.ACTIVE_PERIOD) "CURRENT MONTH" else "ALL TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showDateFilterDropdown,
                            onDismissRequest = { showDateFilterDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Active Period (${periodLabel})") },
                                onClick = {
                                    onDateFilterSelected(DateFilter.ACTIVE_PERIOD)
                                    showDateFilterDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Choose Custom Period...") },
                                onClick = {
                                    showDateFilterDropdown = false
                                    onSelectCustomPeriodClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("All Time") },
                                onClick = {
                                    onDateFilterSelected(DateFilter.ALL)
                                    showDateFilterDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Column {
                Text("TYPE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(FilterType.ALL, FilterType.INCOME, FilterType.EXPENSE).forEach { type ->
                        val isSelected = selectedFilterType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, 
                                    CircleShape
                                )
                                .clickable { onFilterTypeSelected(type) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName.replace(" Only", "").replace(" All Types", "All"),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Column {
                val distinctCategoryNames = remember(categories) {
                    categories.map { it.name }.distinct()
                }
                val topCategories = remember(distinctCategoryNames) {
                    distinctCategoryNames.take(5)
                }
                val displayCategoryObjects = remember(topCategories, selectedCategoryNames, categories) {
                    val selectedNotTop = selectedCategoryNames.filter { it !in topCategories && it in distinctCategoryNames }
                    val displayNames = topCategories + selectedNotTop
                    categories.filter { it.name in displayNames }
                }

                // Map the name-based selection set to an ID set for CategoryChipGrid
                val selectedCategoryIds = remember(selectedCategoryNames, categories) {
                    categories.filter { selectedCategoryNames.contains(it.name) }
                        .map { it.id }
                        .toSet()
                }

                Text("CATEGORIES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Delegate to the shared CategoryChipGrid for the display categories
                    // We embed it inline inside FlowRow to keep the "More" chip in the same row
                    displayCategoryObjects.forEach { category ->
                        val isSelected = selectedCategoryNames.contains(category.name)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onCategorySelected(
                                    if (isSelected) selectedCategoryNames - category.name
                                    else selectedCategoryNames + category.name
                                )
                            },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Icon(
                                    getIconByName(category.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (distinctCategoryNames.size > 5) {
                        val isMoreSelected = selectedCategoryNames.any { it !in topCategories }
                        val remainingCategories = distinctCategoryNames.size - 5
                        FilterChip(
                            selected = isMoreSelected,
                            onClick = { showMoreCategoriesDialog = true },
                            label = { Text("More (+$remainingCategories)") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = "More",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isMoreSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }


            Column {
                val priceLabelText = if (maxPriceLimit >= 10000.0) "Up to ₹10,000+" else "Up to ${CurrencyUtils.formatRupees(maxPriceLimit)}"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MAX PRICE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = priceLabelText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = (maxPriceLimit / 10000.0).toFloat().coerceIn(0f, 1f),
                    onValueChange = {
                        onMaxPriceLimitSelected(it * 10000.0)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text("Max", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Filtered Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Predicted spending in ${SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = CurrencyUtils.formatRupees(predictedSpending),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (spendingChangePercentage != 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                val isDown = spendingChangePercentage < 0
                                Icon(
                                    imageVector = if (isDown) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isDown) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${kotlin.math.abs(spendingChangePercentage).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isDown) IncomeGreen else ExpenseRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FinanceButton(
            text = "Apply Filters",
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            icon = Icons.Default.CheckCircle
        )
    }

    if (showMoreCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showMoreCategoriesDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Category", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { showMoreCategoriesDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                var popSearchQuery by remember { mutableStateOf("") }
                val filteredPopCategories = remember(categories, popSearchQuery) {
                    categories.filter { it.name.contains(popSearchQuery, ignoreCase = true) }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = popSearchQuery,
                        onValueChange = { popSearchQuery = it },
                        placeholder = { Text("Search categories...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape
                    )
                    
                    Box(modifier = Modifier.heightIn(max = 240.dp)) {
                        if (filteredPopCategories.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No categories found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredPopCategories) { category ->
                                    val isSelected = selectedCategoryNames.contains(category.name)
                                    Card(
                                        onClick = {
                                            val nextSelection = if (isSelected) {
                                                selectedCategoryNames - category.name
                                            } else {
                                                selectedCategoryNames + category.name
                                            }
                                            onCategorySelected(nextSelection)
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(
                                            1.dp, 
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        getIconByName(category.iconName),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreCategoriesDialog = false
                                showAddCategoryDialog = true
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Add Custom Category",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var newCategoryType by remember { mutableStateOf(TransactionType.EXPENSE) }
        var selectedIconName by remember { mutableStateOf("category") }
        
        val iconsList = listOf(
            "restaurant", "shopping_cart", "home", "bolt", "work", 
            "movie", "medical_services", "school", "flight", "card_giftcard"
        )
        
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Custom Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Column {
                        Text("Type", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                                val isSelected = newCategoryType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, 
                                            CircleShape
                                        )
                                        .clickable { newCategoryType = type },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (type == TransactionType.EXPENSE) "Expense" else "Income",
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    
                    Column {
                        Text("Select Icon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(iconsList) { iconName ->
                                val isSelected = selectedIconName == iconName
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable { selectedIconName = iconName },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        getIconByName(iconName),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName, newCategoryType, selectedIconName)
                            showAddCategoryDialog = false
                            onCategorySelected(selectedCategoryNames + newCategoryName)
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
