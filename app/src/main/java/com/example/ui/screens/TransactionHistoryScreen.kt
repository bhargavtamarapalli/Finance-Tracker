package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
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
import java.util.Calendar

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
    ALL("All Time"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    var selectedSort by remember { mutableStateOf(SortOption.DATE_DESC) }
    var selectedFilterType by remember { mutableStateOf(FilterType.ALL) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDateFilter by remember { mutableStateOf(DateFilter.ALL) }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithCategory?>(null) }

    // Combined Filter and Sort Logic
    val filteredAndSortedTransactions = remember(
        transactions, searchQuery, selectedSort, selectedFilterType, selectedCategory, selectedDateFilter
    ) {
        var result = transactions

        // 1. Search Query
        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.transaction.source.contains(searchQuery, ignoreCase = true) ||
                        (it.category?.name?.contains(searchQuery, ignoreCase = true) == true) ||
                        it.transaction.notes.contains(searchQuery, ignoreCase = true)
            }
        }

        // 2. Filter by Type
        if (selectedFilterType != FilterType.ALL) {
            val isExpense = selectedFilterType == FilterType.EXPENSE
            result = result.filter {
                val typeIsExpense = it.transaction.type == TransactionType.EXPENSE
                typeIsExpense == isExpense
            }
        }

        // 3. Filter by Category
        if (selectedCategory != null) {
            result = result.filter { it.category?.name == selectedCategory }
        }

        // 4. Filter by Date
        if (selectedDateFilter != DateFilter.ALL) {
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            val cutoff = when (selectedDateFilter) {
                DateFilter.THIS_WEEK -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.timeInMillis
                }
                DateFilter.THIS_MONTH -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.timeInMillis
                }
                DateFilter.THIS_YEAR -> {
                    calendar.timeInMillis = now
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.timeInMillis
                }
                else -> 0L
            }
            result = result.filter { it.transaction.date >= cutoff }
        }

        // 5. Sort
        result = when (selectedSort) {
            SortOption.DATE_DESC -> result.sortedByDescending { it.transaction.date }
            SortOption.DATE_ASC -> result.sortedBy { it.transaction.date }
            SortOption.AMOUNT_DESC -> result.sortedByDescending { it.transaction.amount }
            SortOption.AMOUNT_ASC -> result.sortedBy { it.transaction.amount }
            SortOption.CATEGORY_ASC -> result.sortedBy { it.category?.name ?: "" }
            SortOption.CATEGORY_DESC -> result.sortedByDescending { it.category?.name ?: "" }
        }

        result
    }

    val hasActiveFilters = selectedSort != SortOption.DATE_DESC ||
            selectedFilterType != FilterType.ALL ||
            selectedCategory != null ||
            selectedDateFilter != DateFilter.ALL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Transaction History", 
                        fontWeight = FontWeight.Bold,
                        color = NeutralDark
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = BrandPrimary
                        )
                    }
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
                placeholder = { Text("Search transactions...", color = NeutralMedium) },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search",
                        tint = BrandPrimary
                    ) 
                },
                trailingIcon = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList, 
                            contentDescription = "Filter",
                            tint = if (hasActiveFilters) BrandPrimary else NeutralMedium
                        )
                    }
                },
                singleLine = true,
                shape = AppShapes.roundedCardMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = CardBorderColor,
                    focusedTextColor = NeutralDark,
                    unfocusedTextColor = NeutralDark
                )
            )

            // Dynamic Active Filters Horizontal Track
            if (hasActiveFilters) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingExtraSmall),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "Filters:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeutralMedium,
                            modifier = Modifier.padding(end = AppDimens.paddingExtraSmall)
                        )
                    }

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
                                label = selectedCategory ?: "",
                                onClear = { selectedCategory = null }
                            )
                        }
                    }

                    if (selectedDateFilter != DateFilter.ALL) {
                        item {
                            FilterActiveChip(
                                label = selectedDateFilter.displayName,
                                onClear = { selectedDateFilter = DateFilter.ALL }
                            )
                        }
                    }

                    item {
                        TextButton(
                            onClick = {
                                selectedSort = SortOption.DATE_DESC
                                selectedFilterType = FilterType.ALL
                                selectedCategory = null
                                selectedDateFilter = DateFilter.ALL
                            },
                            contentPadding = PaddingValues(horizontal = AppDimens.paddingSmall)
                        ) {
                            Text("Clear All", color = BrandPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Results List
            if (isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(AppDimens.paddingNormal),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal),
                    modifier = Modifier.weight(1f)
                ) {
                    items(5) {
                        TransactionItemSkeleton()
                    }
                }
            } else if (filteredAndSortedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NeutralBorder, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeutralMedium,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        Text(
                            text = if (transactions.isEmpty()) "No transactions found." else "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeutralDark
                        )
                        Text(
                            text = if (transactions.isEmpty()) "Add your first one!" else "Try adjusting your filters or search terms.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeutralMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(AppDimens.paddingNormal),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = filteredAndSortedTransactions,
                        key = { it.transaction.id }
                    ) { tx ->
                        TransactionItem(
                            transaction = tx,
                            modifier = Modifier.animateItem(),
                            onClick = { selectedTransactionForDetails = tx }
                        )
                    }
                }
            }
        }
    }

    if (selectedTransactionForDetails != null) {
        TransactionDetailsBottomSheet(
            transactionWithCat = selectedTransactionForDetails!!,
            onDismiss = { selectedTransactionForDetails = null },
            onEdit = {
                val tx = selectedTransactionForDetails!!
                selectedTransactionForDetails = null
                navController.navigate("add_transaction/${tx.transaction.type}?transactionId=${tx.transaction.id}")
            },
            onDuplicate = {
                val tx = selectedTransactionForDetails!!
                selectedTransactionForDetails = null
                navController.navigate("add_transaction/${tx.transaction.type}?transactionId=${tx.transaction.id}&duplicate=true")
            },
            onDelete = {
                val tx = selectedTransactionForDetails!!
                selectedTransactionForDetails = null
                viewModel.deleteTransaction(tx.transaction)
            }
        )
    }

    // Material 3 Modal Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            FilterSortSheetContent(
                categories = categories,
                selectedSort = selectedSort,
                onSortSelected = { selectedSort = it },
                selectedFilterType = selectedFilterType,
                onFilterTypeSelected = { selectedFilterType = it },
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                selectedDateFilter = selectedDateFilter,
                onDateFilterSelected = { selectedDateFilter = it },
                onDismiss = { showFilterSheet = false },
                onReset = {
                    selectedSort = SortOption.DATE_DESC
                    selectedFilterType = FilterType.ALL
                    selectedCategory = null
                    selectedDateFilter = DateFilter.ALL
                    showFilterSheet = false
                }
            )
        }
    }
}

@Composable
fun FilterActiveChip(label: String, onClear: () -> Unit) {
    Surface(
        shape = AppShapes.roundedCardMedium,
        color = SurfaceVariantColor,
        modifier = Modifier.border(AppDimens.borderWidthThin, NeutralBorder, AppShapes.roundedCardMedium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = AppDimens.paddingSmall, vertical = AppDimens.paddingExtraSmall)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = NeutralDark,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(AppDimens.paddingExtraSmall))
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear filter",
                tint = NeutralMedium,
                modifier = Modifier
                    .size(AppDimens.sizeIconSmall - AppDimens.paddingExtraSmall)
                    .clickable { onClear() }
            )
        }
    }
}

@Composable
fun FilterSortSheetContent(
    categories: List<Category>,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    selectedFilterType: FilterType,
    onFilterTypeSelected: (FilterType) -> Unit,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    selectedDateFilter: DateFilter,
    onDateFilterSelected: (DateFilter) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.paddingNormal)
            .padding(bottom = AppDimens.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter & Sort",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NeutralDark
            )
            TextButton(onClick = onReset) {
                Text("Reset All", color = BrandPrimary, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = NeutralBorder)

        // 1. Sort Section
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
            Text(
                text = "SORT BY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeutralMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SortOption.values()) { option ->
                    val isSelected = selectedSort == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSortSelected(option) },
                        label = { Text(option.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 2. Type Filter Section
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
            Text(
                text = "TRANSACTION TYPE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeutralMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
                FilterType.values().forEach { type ->
                    val isSelected = selectedFilterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterTypeSelected(type) },
                        label = { Text(type.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Date Filter Section
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
            Text(
                text = "DATE RANGE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeutralMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
                DateFilter.values().forEach { filter ->
                    val isSelected = selectedDateFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onDateFilterSelected(filter) },
                        label = { Text(filter.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 4. Category Filter Section
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
            Text(
                text = "FILTER BY CATEGORY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NeutralMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isSelected = selectedCategory == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(null) },
                        label = { Text("All Categories") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                
                // Get unique list of category names based on actual database contents
                val distinctNames = categories.map { it.name }.distinct()
                items(distinctNames) { catName ->
                    val isSelected = selectedCategory == catName
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(catName) },
                        label = { Text(catName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))

        // Save Button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.heightButton),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = AppShapes.roundedButton
        ) {
            Text("Apply Filters", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
