package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.ui.components.*
import com.example.ui.utils.CurrencyUtils
import com.example.ui.utils.getIconByName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount"),
    CATEGORY_ASC("Category A-Z"),
    CATEGORY_DESC("Category Z-A")
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var searchQuery by remember { mutableStateOf("") }
    
    // Bottom Sheet Filters
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(SortOption.DATE_DESC) }
    var selectedFilterType by remember { mutableStateOf(FilterType.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDateFilter by remember { mutableStateOf(DateFilter.ACTIVE_PERIOD) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Computed / Filtered Transactions list
    val processedTransactions = remember(
        transactions, periodTransactions, searchQuery, selectedSort, selectedFilterType, selectedCategory, selectedDateFilter
    ) {
        var list = if (selectedDateFilter == DateFilter.ACTIVE_PERIOD) {
            periodTransactions
        } else {
            transactions
        }

        // 1. Search Query (Notes, category or source)
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.transaction.source.contains(searchQuery, ignoreCase = true) ||
                it.transaction.notes.contains(searchQuery, ignoreCase = true) ||
                (it.category?.name?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        // 2. Filter Type
        when (selectedFilterType) {
            FilterType.INCOME -> list = list.filter { it.transaction.type == TransactionType.INCOME }
            FilterType.EXPENSE -> list = list.filter { it.transaction.type == TransactionType.EXPENSE }
            FilterType.ALL -> { /* no-op */ }
        }

        // 3. Filter Category
        if (selectedCategory != null) {
            list = list.filter { it.category?.name?.equals(selectedCategory, ignoreCase = true) == true }
        }

        // 4. Sorting
        list = when (selectedSort) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.transaction.date }
            SortOption.DATE_ASC -> list.sortedBy { it.transaction.date }
            SortOption.AMOUNT_DESC -> list.sortedByDescending { it.transaction.amount }
            SortOption.AMOUNT_ASC -> list.sortedBy { it.transaction.amount }
            SortOption.CATEGORY_ASC -> list.sortedBy { it.category?.name?.lowercase() ?: "" }
            SortOption.CATEGORY_DESC -> list.sortedByDescending { it.category?.name?.lowercase() ?: "" }
        }

        list
    }

    val groupedTransactions = remember(processedTransactions) {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

        val isSameDay = { c1: Calendar, c2: Calendar ->
            c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        val groups = processedTransactions.groupBy { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.transaction.date }
            when {
                isSameDay(cal, today) -> "Today"
                isSameDay(cal, yesterday) -> "Yesterday"
                else -> {
                    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(item.transaction.date))
                }
            }
        }

        groups.map { (dateHeader, itemsInGroup) ->
            val income = itemsInGroup.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
            val expense = itemsInGroup.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
            val netSum = income - expense
            Triple(dateHeader, netSum, itemsInGroup)
        }
    }

    val hasActiveFilters = remember(selectedFilterType, selectedCategory, selectedDateFilter) {
        selectedFilterType != FilterType.ALL || selectedCategory != null || selectedDateFilter != DateFilter.ACTIVE_PERIOD
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Transaction History", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    FinanceIconButton(
                        icon = Icons.Default.Menu,
                        onClick = onMenuClick,
                        contentDescription = "Open Navigation Drawer",
                        tint = BrandPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar & Filter Button
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall),
                placeholder = { Text("Search transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = BrandPrimary
                    ) 
                },
                trailingIcon = {
                    FinanceIconButton(
                        icon = Icons.Default.FilterList,
                        onClick = { showFilterSheet = true },
                        contentDescription = "Filter",
                        tint = if (hasActiveFilters) BrandPrimary else NeutralMedium
                    )
                },
                singleLine = true,
                shape = AppShapes.roundedCardMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            // Time Horizon Selector & Period Navigator on Date basis
            if (selectedDateFilter == DateFilter.ACTIVE_PERIOD) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall)
                ) {
                    TimePeriodSelector(
                        selectedPeriod = selectedTimePeriod,
                        onPeriodSelected = { viewModel.setTimePeriod(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                    PeriodNavigator(
                        periodLabel = periodLabel,
                        onPreviousClick = { viewModel.moveToPreviousPeriod() },
                        onNextClick = { viewModel.moveToNextPeriod() },
                        modifier = Modifier.fillMaxWidth(),
                        onLabelClick = { showDatePicker = true },
                        isNextEnabled = isNextPeriodEnabled
                    )
                }
                if (showDatePicker) {
                    CustomPeriodPickerDialog(
                        timePeriod = selectedTimePeriod,
                        activeDate = activeDate,
                        onDateSelected = { viewModel.setDateDirectly(it) },
                        onDismiss = { showDatePicker = false }
                    )
                }
            }

            // Active Filter Chips Row
            if (hasActiveFilters || selectedSort != SortOption.DATE_DESC) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimens.paddingSmall),
                    contentPadding = PaddingValues(horizontal = AppDimens.paddingNormal),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedSort != SortOption.DATE_DESC) {
                        item {
                            FilterActiveChip(
                                label = selectedSort.displayName,
                                onClear = { selectedSort = SortOption.DATE_DESC }
                            )
                        }
                    }

                    if (selectedFilterType != FilterType.ALL) {
                        item {
                            FilterActiveChip(
                                label = selectedFilterType.displayName,
                                onClear = { selectedFilterType = FilterType.ALL }
                            )
                        }
                    }

                    if (selectedCategory != null) {
                        item {
                            FilterActiveChip(
                                label = selectedCategory!!,
                                onClear = { selectedCategory = null }
                            )
                        }
                    }

                    if (selectedDateFilter != DateFilter.ACTIVE_PERIOD) {
                        item {
                            FilterActiveChip(
                                label = selectedDateFilter.displayName,
                                onClear = { selectedDateFilter = DateFilter.ACTIVE_PERIOD }
                            )
                        }
                    }

                    item {
                        FinanceTextButton(
                            text = "Reset All",
                            onClick = {
                                selectedSort = SortOption.DATE_DESC
                                selectedFilterType = FilterType.ALL
                                selectedCategory = null
                                selectedDateFilter = DateFilter.ACTIVE_PERIOD
                            }
                        )
                    }
                }
            }

            // Transaction Count Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingExtraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${processedTransactions.size} transactions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Main Transactions List
            if (processedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No transactions matches the criteria.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NeutralMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = AppDimens.paddingNormal,
                        end = AppDimens.paddingNormal,
                        bottom = AppDimens.paddingLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                ) {
                    groupedTransactions.forEach { (dateHeader, netSum, itemsInGroup) ->
                        item(key = dateHeader) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = AppDimens.paddingNormal, bottom = AppDimens.paddingExtraSmall),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (netSum != 0.0) {
                                    val isPositive = netSum > 0
                                    val formattedAmount = CurrencyUtils.formatRupees(kotlin.math.abs(netSum))
                                    Text(
                                        text = if (isPositive) "+$formattedAmount" else "-$formattedAmount",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        items(itemsInGroup, key = { it.transaction.id }) { item ->
                            TransactionHistoryRow(
                                item = item,
                                onEditClick = {
                                    navController.navigate("add_transaction/${item.transaction.type.name}?transactionId=${item.transaction.id}")
                                },
                                onDeleteClick = {
                                    viewModel.deleteTransaction(item.transaction)
                                },
                                onDuplicateClick = {
                                    navController.navigate("add_transaction/${item.transaction.type.name}?transactionId=${item.transaction.id}&duplicate=true")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Sheet Modal
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FilterBottomSheetContent(
                categories = categories,
                selectedSort = selectedSort,
                selectedFilterType = selectedFilterType,
                selectedCategory = selectedCategory,
                selectedDateFilter = selectedDateFilter,
                onSortSelected = { selectedSort = it },
                onFilterTypeSelected = { selectedFilterType = it },
                onCategorySelected = { selectedCategory = it },
                onDateFilterSelected = { selectedDateFilter = it },
                onReset = {
                    selectedSort = SortOption.DATE_DESC
                    selectedFilterType = FilterType.ALL
                    selectedCategory = null
                    selectedDateFilter = DateFilter.ACTIVE_PERIOD
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
fun FilterActiveChip(
    label: String,
    onClear: () -> Unit
) {
    Card(
        shape = AppShapes.roundedCardLarge,
        colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.15f)),
        border = null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = AppDimens.paddingSmall, end = 2.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = BrandPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            FinanceIconButton(
                icon = Icons.Default.Clear,
                onClick = onClear,
                contentDescription = "Clear Filter",
                tint = BrandPrimary,
                size = 16.dp
            )
        }
    }
}

@Composable
fun TransactionHistoryRow(
    item: TransactionWithCategory,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDuplicateClick: () -> Unit
) {
    val formatter = object {
        fun format(amount: Double): String = CurrencyUtils.formatRupees(amount)
    }
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val isExpense = item.transaction.type == TransactionType.EXPENSE
    var expanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (isDark) 0.08f else 0.4f),
                shape = AppShapes.roundedCardMedium
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        shape = AppShapes.roundedCardMedium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isExpense) ExpenseRed.copy(alpha = 0.15f) 
                        else IncomeGreen.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconByName(item.category?.iconName ?: "category"),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isExpense) ExpenseRed else IncomeGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.transaction.source.ifBlank { item.category?.name ?: "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.category?.name ?: "Unknown"} • ${timeFormat.format(Date(item.transaction.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (item.transaction.notes.isNotBlank()) {
                    Text(
                        text = item.transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}${formatter.format(item.transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isExpense) ExpenseRed else IncomeGreen,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { expanded = false; onEditClick() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { expanded = false; onDuplicateClick() },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { expanded = false; onDeleteClick() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            }
        }
    }
}

enum class FilterTab(val displayName: String) {
    SORT("Sort By"),
    TYPE("Transaction Type"),
    DATE("Time Period"),
    CATEGORY("Category")
}

@Composable
fun FilterBottomSheetContent(
    categories: List<Category>,
    selectedSort: SortOption,
    selectedFilterType: FilterType,
    selectedCategory: String?,
    selectedDateFilter: DateFilter,
    onSortSelected: (SortOption) -> Unit,
    onFilterTypeSelected: (FilterType) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onDateFilterSelected: (DateFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(FilterTab.SORT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter & Sort",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FinanceTextButton(
                text = "Reset All",
                onClick = onReset,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Split Tab Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            // Left Column: Vertical Tabs
            Column(
                modifier = Modifier
                    .width(135.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                FilterTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    val hasFilter = when (tab) {
                        FilterTab.SORT -> selectedSort != SortOption.DATE_DESC
                        FilterTab.TYPE -> selectedFilterType != FilterType.ALL
                        FilterTab.DATE -> selectedDateFilter != DateFilter.ACTIVE_PERIOD
                        FilterTab.CATEGORY -> selectedCategory != null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeTab = tab }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface 
                                else Color.Transparent
                            )
                            .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingMedium),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tab.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (hasFilter) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Vertical Divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )

            // Right Column: Vertical List of Options
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(AppDimens.paddingNormal)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                ) {
                    when (activeTab) {
                        FilterTab.SORT -> {
                            items(SortOption.values()) { option ->
                                val isSelected = selectedSort == option
                                FilterOptionRow(
                                    label = option.displayName,
                                    isSelected = isSelected,
                                    onClick = { onSortSelected(option) }
                                )
                            }
                        }
                        FilterTab.TYPE -> {
                            items(FilterType.values()) { type ->
                                val isSelected = selectedFilterType == type
                                FilterOptionRow(
                                    label = type.displayName,
                                    isSelected = isSelected,
                                    onClick = { onFilterTypeSelected(type) }
                                )
                            }
                        }
                        FilterTab.DATE -> {
                            items(DateFilter.values()) { filter ->
                                val isSelected = selectedDateFilter == filter
                                FilterOptionRow(
                                    label = filter.displayName,
                                    isSelected = isSelected,
                                    onClick = { onDateFilterSelected(filter) }
                                )
                            }
                        }
                        FilterTab.CATEGORY -> {
                            item {
                                val isAllSelected = selectedCategory == null
                                FilterOptionRow(
                                    label = "All Categories",
                                    isSelected = isAllSelected,
                                    onClick = { onCategorySelected(null) }
                                )
                            }
                            val distinctNames = categories.map { it.name }.distinct()
                            items(distinctNames) { catName ->
                                val isSelected = selectedCategory == catName
                                val categoryIcon = categories.firstOrNull { it.name == catName }?.iconName
                                FilterOptionRow(
                                    label = catName,
                                    isSelected = isSelected,
                                    iconName = categoryIcon,
                                    onClick = { onCategorySelected(catName) }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Spacer(modifier = Modifier.height(AppDimens.paddingNormal))

        // Save Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingNormal),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
        ) {
            FinanceButton(
                text = "Apply Filters",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                height = AppDimens.heightButton
            )
        }
    }
}

@Composable
fun FilterOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconName: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = AppShapes.roundedCardMedium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingMedium, vertical = AppDimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
            ) {
                if (iconName != null) {
                    Icon(
                        imageVector = getIconByName(iconName),
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
