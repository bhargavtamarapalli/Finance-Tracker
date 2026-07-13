package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.ui.theme.*
import com.example.ui.utils.CurrencyUtils
import com.example.ui.utils.getIconByName
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TimePeriod
import com.example.ui.components.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

enum class DetailChartType(val displayName: String) {
    CATEGORY("Category Breakdown"),
    COMPARISON("Income vs Expense"),
    TREND("Spending Trend")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnalyticsDetailScreen(
    viewModel: FinanceViewModel,
    initialChartType: String,
    onBackClick: () -> Unit
) {
    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val selectedTimePeriod by viewModel.selectedTimePeriod.collectAsStateWithLifecycle()
    val periodLabel by viewModel.periodLabel.collectAsStateWithLifecycle()
    val activeDate by viewModel.activeDate.collectAsStateWithLifecycle()
    val isNextPeriodEnabled by viewModel.isNextPeriodEnabled.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var activeChart by remember {
        mutableStateOf(
            try {
                DetailChartType.valueOf(initialChartType)
            } catch (e: Exception) {
                DetailChartType.CATEGORY
            }
        )
    }

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryIndex by remember { mutableStateOf(-1) }
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // Swipe gesture threshold (px) — only register intentional horizontal swipes
    val swipeThreshold = 80f
    var swipeOffsetX by remember { mutableStateOf(0f) }

    // Reset slice index and category details when switching views
    LaunchedEffect(activeChart, selectedType) {
        selectedCategoryIndex = -1
        expandedCategories = emptySet()
    }

    val totalIncome = remember(periodTransactions) {
        periodTransactions
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
    }
    
    val totalExpense = remember(periodTransactions) {
        periodTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
    }
    
    val netSavings = totalIncome - totalExpense

    val filteredTransactions = remember(periodTransactions, selectedType, activeChart) {
        if (activeChart == DetailChartType.COMPARISON) {
            periodTransactions
        } else {
            periodTransactions.filter { it.transaction.type == selectedType }
        }
    }

    val totalAmount = remember(filteredTransactions, selectedType) {
        filteredTransactions
            .filter { it.transaction.type == selectedType }
            .sumOf { it.transaction.amount }
    }

    // Grouping by category — stable list (index = pie slice color index)
    val breakdown = remember(filteredTransactions, selectedType) {
        filteredTransactions
            .filter { it.transaction.type == selectedType }
            .groupBy { it.category?.name ?: "Other" }
            .mapValues { it.value.sumOf { tx -> tx.transaction.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Display list: selected item floats to top; rest follow natural order.
    // Each entry carries its originalIndex for stable color/slice mapping.
    val displayBreakdown: List<Pair<Int, Pair<String, Double>>> = remember(breakdown, selectedCategoryIndex) {
        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= breakdown.size) {
            breakdown.mapIndexed { idx, item -> idx to item }
        } else {
            val selected = selectedCategoryIndex to breakdown[selectedCategoryIndex]
            val rest = breakdown.mapIndexed { idx, item -> idx to item }.filter { it.first != selectedCategoryIndex }
            listOf(selected) + rest
        }
    }

    // When a category is selected: auto-expand it and scroll the list to the top
    LaunchedEffect(selectedCategoryIndex) {
        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < breakdown.size) {
            val catName = breakdown[selectedCategoryIndex].first
            expandedCategories = setOf(catName)   // expand selected, collapse all others
            coroutineScope.launch {
                listState.animateScrollToItem(index = 0)
            }
        } else {
            // deselect: collapse everything
            expandedCategories = emptySet()
        }
    }

    // Segment colors for category donut chart
    val segmentColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.outline
    )

    // Calculate trend points (daily/weekly/monthly/yearly depending on horizon)
    val dailyExpenses = remember(periodTransactions, selectedTimePeriod, selectedType) {
        val calInstance = Calendar.getInstance()
        val map = mutableMapOf<Int, Double>()
        
        when (selectedTimePeriod) {
            TimePeriod.DAY -> {
                for (hour in 0..23) map[hour] = 0.0
                periodTransactions
                    .filter { it.transaction.type == selectedType }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val hour = calInstance.get(Calendar.HOUR_OF_DAY)
                        map[hour] = (map[hour] ?: 0.0) + tx.transaction.amount
                    }
            }
            TimePeriod.WEEK -> {
                for (day in 1..7) map[day] = 0.0
                periodTransactions
                    .filter { it.transaction.type == selectedType }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val day = calInstance.get(Calendar.DAY_OF_WEEK)
                        map[day] = (map[day] ?: 0.0) + tx.transaction.amount
                    }
            }
            TimePeriod.MONTH -> {
                val daysInMonth = calInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
                for (day in 1..daysInMonth) map[day] = 0.0
                periodTransactions
                    .filter { it.transaction.type == selectedType }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val day = calInstance.get(Calendar.DAY_OF_MONTH)
                        map[day] = (map[day] ?: 0.0) + tx.transaction.amount
                    }
            }
            TimePeriod.YEAR -> {
                for (month in 0..11) map[month] = 0.0
                periodTransactions
                    .filter { it.transaction.type == selectedType }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val month = calInstance.get(Calendar.MONTH)
                        map[month] = (map[month] ?: 0.0) + tx.transaction.amount
                    }
            }
        }
        map.toList().sortedBy { it.first }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.transaction.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toList().sortedByDescending { it.first }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(activeChart.displayName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("detail_scroll_container")
                .pointerInput(activeChart) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeOffsetX = 0f },
                        onDragEnd = {
                            val charts = DetailChartType.values()
                            val currentIdx = charts.indexOf(activeChart)
                            when {
                                // Swipe right → go to previous chart
                                swipeOffsetX > swipeThreshold -> {
                                    val prevIdx = (currentIdx - 1 + charts.size) % charts.size
                                    activeChart = charts[prevIdx]
                                }
                                // Swipe left → go to next chart
                                swipeOffsetX < -swipeThreshold -> {
                                    val nextIdx = (currentIdx + 1) % charts.size
                                    activeChart = charts[nextIdx]
                                }
                            }
                            swipeOffsetX = 0f
                        },
                        onDragCancel = { swipeOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            swipeOffsetX += dragAmount
                        }
                    )
                },
            contentPadding = PaddingValues(horizontal = AppDimens.paddingNormal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stickyHeader {
                var showPeriodDropdown by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Chart Switcher Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailChartType.values().forEach { chartType ->
                            val isSelected = activeChart == chartType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { activeChart = chartType }
                                    .testTag("tab_${chartType.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chartType.displayName.replace(" Breakdown", "").replace("Spending ", ""),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Income / Expense Capsule Toggler (Hide for comparison screen because it renders both)
                    if (activeChart != DetailChartType.COMPARISON) {
                        val isExpenseSelected = selectedType == TransactionType.EXPENSE
                        val isIncomeSelected = selectedType == TransactionType.INCOME
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                                .padding(3.dp)
                        ) {
                            // Expenses Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        color = if (isExpenseSelected) ExpenseRed.copy(alpha = 0.15f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedType = TransactionType.EXPENSE }
                                    .testTag("detail_tab_expense"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isExpenseSelected) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Expenses",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isExpenseSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isExpenseSelected) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Income Tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        color = if (isIncomeSelected) IncomeGreen.copy(alpha = 0.15f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedType = TransactionType.INCOME }
                                    .testTag("detail_tab_income"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = if (isIncomeSelected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Income",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isIncomeSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isIncomeSelected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Combined Date and Period Dropdown Navigation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { showDatePicker = true },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = periodLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Box {
                                Surface(
                                    onClick = { showPeriodDropdown = true },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (selectedTimePeriod) {
                                                com.example.ui.viewmodel.TimePeriod.DAY -> "Day"
                                                com.example.ui.viewmodel.TimePeriod.WEEK -> "Week"
                                                com.example.ui.viewmodel.TimePeriod.MONTH -> "Month"
                                                com.example.ui.viewmodel.TimePeriod.YEAR -> "Year"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showPeriodDropdown,
                                    onDismissRequest = { showPeriodDropdown = false }
                                ) {
                                    com.example.ui.viewmodel.TimePeriod.values().forEach { period ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = when (period) {
                                                        com.example.ui.viewmodel.TimePeriod.DAY -> "Day"
                                                        com.example.ui.viewmodel.TimePeriod.WEEK -> "Week"
                                                        com.example.ui.viewmodel.TimePeriod.MONTH -> "Month"
                                                        com.example.ui.viewmodel.TimePeriod.YEAR -> "Year"
                                                    }
                                                )
                                            },
                                            onClick = {
                                                viewModel.setTimePeriod(period)
                                                showPeriodDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.moveToPreviousPeriod() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Period",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.moveToNextPeriod() },
                                enabled = isNextPeriodEnabled,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isNextPeriodEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Period",
                                    tint = if (isNextPeriodEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (showDatePicker) {
                    CustomPeriodPickerDialog(
                        timePeriod = selectedTimePeriod,
                        activeDate = activeDate,
                        onDateSelected = { 
                            viewModel.setDateDirectly(it)
                            showDatePicker = false
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }
            }

            // Loading state
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimens.paddingExtraLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(NeutralBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = NeutralMedium,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        Text(
                            text = "No analytics data available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeutralDark
                        )
                    }
                }
            } else {
                // Focus Chart rendering
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.roundedCardLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (activeChart) {
                                DetailChartType.CATEGORY -> {
                                    Text(
                                        text = if (selectedType == TransactionType.EXPENSE) "Category Expenses" else "Category Income",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    DonutChart(
                                        portions = breakdown,
                                        total = totalAmount,
                                        segmentColors = segmentColors,
                                        selectedIndex = selectedCategoryIndex,
                                        onSliceClick = { selectedCategoryIndex = it },
                                        onSelectionCycle = {
                                            selectedCategoryIndex = if (breakdown.isEmpty()) -1 else (selectedCategoryIndex + 1) % breakdown.size
                                        },
                                        modifier = Modifier
                                            .height(260.dp)
                                            .fillMaxWidth()
                                    )
                                }
                                DetailChartType.COMPARISON -> {
                                    Text(
                                        text = "Income vs Expense Comparison",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IncomeExpenseComparisonChart(
                                        income = totalIncome,
                                        expense = totalExpense,
                                        modifier = Modifier.height(260.dp).fillMaxWidth()
                                    )
                                }
                                DetailChartType.TREND -> {
                                    Text(
                                        text = if (selectedType == TransactionType.EXPENSE) "Expense Spending Trend" else "Income Earning Trend",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SpendingTrendChart(
                                        dailyExpenses = dailyExpenses,
                                        timePeriod = selectedTimePeriod,
                                        modifier = Modifier.height(260.dp).fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Data details header
                item {
                    Text(
                        text = "Data Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Detailed data list breakdown
                if (activeChart == DetailChartType.CATEGORY) {
                    itemsIndexed(
                        items = displayBreakdown,
                        key = { _, entry -> entry.second.first }  // stable key = category name
                    ) { _, entry ->
                        val (originalIndex, item) = entry
                        val (catName, amount) = item
                        val percentage = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f
                        val percentageText = if (totalAmount > 0) (amount / totalAmount * 100) else 0.0
                        val isSelected = originalIndex == selectedCategoryIndex
                        val isExpanded = expandedCategories.contains(catName)
                        val catTransactions = filteredTransactions
                            .filter { (it.category?.name ?: "Other") == catName }
                            .sortedByDescending { it.transaction.date }
                        val catColor = segmentColors[originalIndex % segmentColors.size]

                        val rowScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.02f else 1.0f,
                            animationSpec = tween(durationMillis = 350),
                            label = "CategoryRowScale_$catName"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(
                                    fadeInSpec = tween(durationMillis = 400),
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    fadeOutSpec = tween(durationMillis = 250)
                                )
                                .graphicsLayer {
                                    scaleX = rowScale
                                    scaleY = rowScale
                                }
                                .clickable {
                                    selectedCategoryIndex = if (isSelected) -1 else originalIndex
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    catColor.copy(alpha = 0.10f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) catColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // ─── Header Row ───────────────────────────────────
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Pie-chart color dot
                                        Box(
                                            modifier = Modifier
                                                .size(if (isSelected) 14.dp else 10.dp)
                                                .background(catColor, CircleShape)
                                        )
                                        // Category icon
                                        val iconVector = remember(catName) {
                                            val matchedIcon = catTransactions.firstOrNull()?.category?.iconName
                                            getIconByName(matchedIcon ?: "question_mark")
                                        }
                                        Surface(
                                            modifier = Modifier.size(if (isSelected) 40.dp else 36.dp),
                                            shape = CircleShape,
                                            color = catColor.copy(alpha = 0.15f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(if (isSelected) 22.dp else 18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = catName,
                                                style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${catTransactions.size} transaction${if (catTransactions.size != 1) "s" else ""} · ${String.format(Locale.getDefault(), "%.1f", percentageText)}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) catColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = CurrencyUtils.formatRupees(amount),
                                            style = if (isSelected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) catColor else (if (selectedType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen)
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // ─── Progress bar ────────────────────────────────
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = catColor,
                                    trackColor = catColor.copy(alpha = 0.15f),
                                )

                                // ─── Inline transaction list (animated) ──────────
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, bottom = 4.dp)
                                    ) {
                                        HorizontalDivider(
                                            color = catColor.copy(alpha = 0.25f),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (catTransactions.isEmpty()) {
                                            Text(
                                                text = "No transactions found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Contributions to $catName",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = catColor,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                            catTransactions.forEach { tx ->
                                                TransactionItem(
                                                    transaction = tx,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Group comparison or trend list by date
                    groupedTransactions.forEach { (dateMillis, txs) ->
                        item {
                            val headerDate = remember(dateMillis) {
                                val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                                sdf.format(Date(dateMillis))
                            }
                            Text(
                                text = headerDate,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        items(txs) { tx ->
                            TransactionItem(
                                transaction = tx,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
