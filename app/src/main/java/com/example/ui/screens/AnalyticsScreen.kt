package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.*
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.utils.CurrencyUtils
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.components.*
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.atan2
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    onMenuClick: () -> Unit
) {
    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val selectedTimePeriod by viewModel.selectedTimePeriod.collectAsStateWithLifecycle()
    val periodLabel by viewModel.periodLabel.collectAsStateWithLifecycle()
    val activeDate by viewModel.activeDate.collectAsStateWithLifecycle()
    val isNextPeriodEnabled by viewModel.isNextPeriodEnabled.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Switch between Expense and Income analytics
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    
    // Track selected category index for donut chart highlighting
    var selectedCategoryIndex by remember { mutableStateOf(-1) }
    
    // Toggle state for category breakdown list visibility
    var listVisible by remember { mutableStateOf(true) }
    
    // Reset selection and visibility when tab switches
    LaunchedEffect(selectedType) {
        selectedCategoryIndex = -1
        listVisible = true
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

    // Group expenses by period trends
    val dailyExpenses = remember(periodTransactions, selectedTimePeriod) {
        val calInstance = java.util.Calendar.getInstance()
        val map = mutableMapOf<Int, Double>()
        
        when (selectedTimePeriod) {
            com.example.ui.viewmodel.TimePeriod.DAY -> {
                for (hour in 0..23) {
                    map[hour] = 0.0
                }
                periodTransactions
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val hour = calInstance.get(java.util.Calendar.HOUR_OF_DAY)
                        map[hour] = (map[hour] ?: 0.0) + tx.transaction.amount
                    }
            }
            com.example.ui.viewmodel.TimePeriod.WEEK -> {
                for (day in 1..7) {
                    map[day] = 0.0
                }
                periodTransactions
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val day = calInstance.get(java.util.Calendar.DAY_OF_WEEK)
                        map[day] = (map[day] ?: 0.0) + tx.transaction.amount
                    }
            }
            com.example.ui.viewmodel.TimePeriod.MONTH -> {
                val daysInMonth = calInstance.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                for (day in 1..daysInMonth) {
                    map[day] = 0.0
                }
                periodTransactions
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val day = calInstance.get(java.util.Calendar.DAY_OF_MONTH)
                        map[day] = (map[day] ?: 0.0) + tx.transaction.amount
                    }
            }
            com.example.ui.viewmodel.TimePeriod.YEAR -> {
                for (month in 0..11) {
                    map[month] = 0.0
                }
                periodTransactions
                    .filter { it.transaction.type == TransactionType.EXPENSE }
                    .forEach { tx ->
                        calInstance.timeInMillis = tx.transaction.date
                        val month = calInstance.get(java.util.Calendar.MONTH)
                        map[month] = (map[month] ?: 0.0) + tx.transaction.amount
                    }
            }
        }
        map.toList().sortedBy { it.first }
    }

    val filteredTransactions = periodTransactions.filter { it.transaction.type == selectedType }
    val totalAmount = filteredTransactions.sumOf { it.transaction.amount }
    
    // Group by category name
    val breakdown = filteredTransactions
        .groupBy { it.category?.name ?: "Other" }
        .mapValues { it.value.sumOf { tx -> tx.transaction.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Segment colors reusing centralized theme tokens
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FinanceIconButton(
                        icon = Icons.Default.Menu,
                        onClick = onMenuClick,
                        contentDescription = "Menu",
                        modifier = Modifier.testTag("menu_button")
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("analytics_scroll_container"),
            contentPadding = PaddingValues(AppDimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
        ) {
            // Tab Toggler
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expenses Tab Option
                    val isExpenseSelected = selectedType == TransactionType.EXPENSE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (isExpenseSelected) ExpenseRed.copy(alpha = 0.15f) else Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(
                                width = if (isExpenseSelected) 1.5.dp else 0.dp,
                                color = if (isExpenseSelected) ExpenseRed.copy(alpha = 0.3f) else Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable { selectedType = TransactionType.EXPENSE }
                            .testTag("chip_expense"),
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

                    // Income Tab Option
                    val isIncomeSelected = selectedType == TransactionType.INCOME
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                color = if (isIncomeSelected) IncomeGreen.copy(alpha = 0.15f) else Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .border(
                                width = if (isIncomeSelected) 1.5.dp else 0.dp,
                                color = if (isIncomeSelected) IncomeGreen.copy(alpha = 0.3f) else Color.Transparent,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .clickable { selectedType = TransactionType.INCOME }
                            .testTag("chip_income"),
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

            // Time Horizon Selector
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimens.paddingSmall)
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
                    if (showDatePicker) {
                        CustomPeriodPickerDialog(
                            timePeriod = selectedTimePeriod,
                            activeDate = activeDate,
                            onDateSelected = { viewModel.setDateDirectly(it) },
                            onDismiss = { showDatePicker = false }
                        )
                    }
                }
            }

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
                                .background(NeutralBorder, androidx.compose.foundation.shape.CircleShape),
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
                            text = "No analytics data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeutralDark
                        )
                        Text(
                            text = "Add some transactions to see your stats.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeutralMedium
                        )
                    }
                }
            } 
            
            if (!isLoading && filteredTransactions.isNotEmpty()) {
                // Overview Total Amount Card
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.roundedCardLarge,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.paddingLarge),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (selectedType == TransactionType.EXPENSE) "Monthly Expenses" else "Monthly Income",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                            Text(
                                text = CurrencyUtils.formatRupees(totalAmount),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedType == TransactionType.EXPENSE) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.testTag("total_amount_text")
                            )
                        }
                    }
                }

            // Monthly Summary Card
            item {
                MonthlySummaryRow(
                    income = totalIncome,
                    expense = totalExpense,
                    savings = netSavings,
                    modifier = Modifier.testTag("monthly_summary_card")
                )
            }

            // Income vs Expense Comparison Chart
            item {
                IncomeExpenseComparisonChart(
                    income = totalIncome,
                    expense = totalExpense,
                    modifier = Modifier.testTag("income_expense_comparison_chart")
                )
            }

            // Spending Trend Chart
            item {
                SpendingTrendChart(
                    dailyExpenses = dailyExpenses,
                    timePeriod = selectedTimePeriod,
                    modifier = Modifier.testTag("spending_trend_chart")
                )
            }

            if (breakdown.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimens.paddingExtraLarge),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.paddingLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                            Text(
                                text = "No transactions found for this month",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Interactive Donut Chart Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { listVisible = !listVisible }
                            .testTag("donut_chart_card"),
                        shape = AppShapes.roundedCardLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.paddingLarge),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Interactive Visual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Tap chart or the indicator below to toggle details",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))

                            DonutChart(
                                portions = breakdown,
                                total = totalAmount,
                                segmentColors = segmentColors,
                                selectedIndex = selectedCategoryIndex,
                                onSliceClick = { index ->
                                    selectedCategoryIndex = index
                                    listVisible = true
                                },
                                onSelectionCycle = {
                                    selectedCategoryIndex = if (breakdown.isEmpty()) -1 else (selectedCategoryIndex + 1) % breakdown.size
                                    listVisible = true
                                },
                                modifier = Modifier
                                    .height(340.dp)
                                    .fillMaxWidth()
                                    .testTag("donut_chart")
                            )

                            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = AppDimens.paddingSmall)
                            ) {
                                Icon(
                                    imageVector = if (listVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (listVisible) "Hide Detailed Breakdown" else "View Detailed Breakdown",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Breakdown list header and items, shown with high-end spring expand/collapse animations
                item {
                    AnimatedVisibility(
                        visible = listVisible,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppDimens.paddingSmall),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                        ) {
                            Text(
                                text = "Category Breakdown",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = AppDimens.paddingSmall)
                            )

                            breakdown.forEachIndexed { index, item ->
                                val (categoryName, amount) = item
                                val percentage = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f
                                val isSelected = index == selectedCategoryIndex
                                
                                CategoryBreakdownRow(
                                    categoryName = categoryName,
                                    amount = amount,
                                    percentage = percentage,
                                    barColor = segmentColors[index % segmentColors.size],
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedCategoryIndex = if (isSelected) -1 else index
                                    },
                                    modifier = Modifier.testTag("category_row_$categoryName")
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = AppDimens.paddingSmall, vertical = AppDimens.paddingSmall),
                                        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                                    ) {
                                        val categoryTransactions = remember(periodTransactions, categoryName, selectedType) {
                                            periodTransactions.filter { 
                                                it.category?.name == categoryName && it.transaction.type == selectedType 
                                            }.sortedByDescending { it.transaction.date }
                                        }

                                        if (categoryTransactions.isEmpty()) {
                                            Text(
                                                text = "No individual transactions found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = AppDimens.paddingSmall)
                                            )
                                        } else {
                                            Text(
                                                text = "Transactions under $categoryName:",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                            )
                                            categoryTransactions.forEach { transaction ->
                                                TransactionItem(
                                                    transaction = transaction,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // Close inner else
        } // Close outer if
        } // Close LazyColumn
    } // Close Scaffold lambda
} // Close AnalyticsScreen

@Composable
fun DonutChart(
    portions: List<Pair<String, Double>>,
    total: Double,
    segmentColors: List<Color>,
    selectedIndex: Int,
    onSelectionCycle: () -> Unit,
    modifier: Modifier = Modifier,
    onSliceClick: (Int) -> Unit = {}
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "SweepAnimation"
    )

    // State list for individual slice displacement animation (exploding pie chart slices)
    val displacements = portions.mapIndexed { index, _ ->
        val isSelected = index == selectedIndex
        animateFloatAsState(
            targetValue = if (isSelected) 12f else 0f, // 12 dp displacement outwards
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "displacement_$index"
        )
    }

    // State list for animating the outer pointing label text sizes when selected
    val labelTextSizes = portions.mapIndexed { index, _ ->
        val isSelected = index == selectedIndex
        animateFloatAsState(
            targetValue = if (isSelected) 15f else 9.5f, // Enlarge to 15 dp when selected, default is 9.5 dp
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "labelTextSize_$index"
        )
    }

    LaunchedEffect(portions) {
        animationPlayed = true
    }

    if (total == 0.0) return

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(portions, selectedIndex) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val dist = sqrt(dx * dx + dy * dy)
                            
                            val radius = min(size.width, size.height) / 2
                            val arcRadius = radius * 0.65f
                            
                            // Check if the tap is within the active area of the pie chart
                            if (dist <= arcRadius * 1.25f) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < -90f) {
                                    angle += 360f
                                }
                                
                                var startAngle = -90f
                                var clickedIndex = -1
                                portions.forEachIndexed { idx, portion ->
                                    val sweepAngle = ((portion.second / total) * 360f).toFloat()
                                    val endAngle = startAngle + sweepAngle
                                    if (angle >= startAngle && angle < endAngle) {
                                        clickedIndex = idx
                                    }
                                    startAngle = endAngle
                                }
                                
                                if (clickedIndex != -1) {
                                    onSliceClick(clickedIndex)
                                } else {
                                    onSelectionCycle()
                                }
                            } else {
                                onSelectionCycle()
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val radius = min(width, height) / 2
                
                // Leave 35% space for pointing lines and outer text labels
                val arcRadius = radius * 0.65f
                val baseSize = Size(arcRadius * 2f, arcRadius * 2f)
                val baseTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

                // Step 1: Draw the 3D depth extrusion layer
                var startAngle = -90f
                portions.forEachIndexed { index, portion ->
                    val sweepAngle = ((portion.second / total) * 360f).toFloat() * animateSweep
                    
                    // Displacement calculation for currently rendering segment
                    val middleAngle = startAngle + sweepAngle / 2f
                    val angleRad = Math.toRadians(middleAngle.toDouble())
                    val dispPx = displacements.getOrNull(index)?.value?.dp?.toPx() ?: 0f
                    val dx = dispPx * cos(angleRad).toFloat()
                    val dy = dispPx * sin(angleRad).toFloat()

                    // Construct a darker extrusion color for visual physical depth
                    val baseColor = segmentColors[index % segmentColors.size]
                    val darkColor = Color(
                        red = (baseColor.red * 0.5f).coerceIn(0f, 1f),
                        green = (baseColor.green * 0.5f).coerceIn(0f, 1f),
                        blue = (baseColor.blue * 0.5f).coerceIn(0f, 1f),
                        alpha = baseColor.alpha
                    )

                    // Render volumetric depth by drawing stacked offset shadow sectors with exploded offset
                    for (offsetY in 1..8) {
                        drawArc(
                            color = darkColor.copy(alpha = 0.12f * offsetY),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = baseSize,
                            topLeft = Offset(baseTopLeft.x + dx, baseTopLeft.y + dy + offsetY * 2f)
                        )
                    }
                    startAngle += sweepAngle
                }

                // Step 2: Draw the main premium top segment layer
                startAngle = -90f
                portions.forEachIndexed { index, portion ->
                    val sweepAngle = ((portion.second / total) * 360f).toFloat() * animateSweep

                    // Displacement calculation for currently rendering segment
                    val middleAngle = startAngle + sweepAngle / 2f
                    val angleRad = Math.toRadians(middleAngle.toDouble())
                    val dispPx = displacements.getOrNull(index)?.value?.dp?.toPx() ?: 0f
                    val dx = dispPx * cos(angleRad).toFloat()
                    val dy = dispPx * sin(angleRad).toFloat()

                    drawArc(
                        color = segmentColors[index % segmentColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = baseSize,
                        topLeft = Offset(baseTopLeft.x + dx, baseTopLeft.y + dy)
                    )
                    startAngle += sweepAngle
                }

                // Step 3: Draw pointing lines and absolute rupee values outside segments
                startAngle = -90f
                portions.forEachIndexed { index, portion ->
                    val sweepAngle = ((portion.second / total) * 360f).toFloat() * animateSweep
                    
                    // Only draw a label if the portion is at least 4% of total to avoid cluttering
                    if (portion.second / total >= 0.04 && sweepAngle > 0f) {
                        val middleAngle = startAngle + sweepAngle / 2f
                        val angleRad = Math.toRadians(middleAngle.toDouble())
                        val dispPx = displacements.getOrNull(index)?.value?.dp?.toPx() ?: 0f
                        val dx = dispPx * cos(angleRad).toFloat()
                        val dy = dispPx * sin(angleRad).toFloat()
                        
                        // Outer arc boundary point (shifted by slice displacement)
                        val startX = center.x + dx + (arcRadius * 0.9f) * cos(angleRad).toFloat()
                        val startY = center.y + dy + (arcRadius * 0.9f) * sin(angleRad).toFloat()
                        
                        // Outward indicator pointer end-point (also shifted for perfect continuity)
                        val endX = center.x + dx + (arcRadius * 1.22f) * cos(angleRad).toFloat()
                        val endY = center.y + dy + (arcRadius * 1.22f) * sin(angleRad).toFloat()
                        
                        val isSelected = index == selectedIndex
                        
                        // Drawing elegant pointing line
                        drawLine(
                            color = segmentColors[index % segmentColors.size].copy(alpha = if (isSelected) 1f else 0.5f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isSelected) 2.2.dp.toPx() else 1.dp.toPx()
                        )
                        
                        // Draw a tiny color-coded anchor dot
                        drawCircle(
                            color = segmentColors[index % segmentColors.size],
                            radius = if (isSelected) 4.dp.toPx() else 2.5.dp.toPx(),
                            center = Offset(endX, endY)
                        )
                        
                        // Format the value in Rupees: e.g. "₹5.5k" or "₹350"
                        val amountText = if (portion.second >= 1000) {
                            "₹${String.format(Locale.getDefault(), "%.1f", portion.second / 1000.0)}k"
                        } else {
                            "₹${portion.second.toInt()}"
                        }.replace(".0k", "k")
                        
                        // Text placement and alignment (also shifted)
                        val textX = center.x + dx + (arcRadius * 1.34f) * cos(angleRad).toFloat()
                        val textY = center.y + dy + (arcRadius * 1.34f) * sin(angleRad).toFloat()
                        
                        val animatedSize = labelTextSizes.getOrNull(index)?.value?.dp?.toPx() ?: 9.5.dp.toPx()
                        val paint = android.graphics.Paint().apply {
                            color = if (isSelected) segmentColors[index % segmentColors.size].toArgb() else textColor
                            textSize = animatedSize
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            textAlign = when {
                                cos(angleRad) > 0.1 -> android.graphics.Paint.Align.LEFT
                                cos(angleRad) < -0.1 -> android.graphics.Paint.Align.RIGHT
                                else -> android.graphics.Paint.Align.CENTER
                            }
                        }
                        val baselineY = textY + (paint.textSize / 3f)
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            amountText,
                            textX,
                            baselineY,
                            paint
                        )
                    }
                    startAngle += sweepAngle
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected slice details below the filled pie chart
        val hasSelection = selectedIndex in portions.indices
        val titleText = if (hasSelection) portions[selectedIndex].first else "All Categories"
        val displayAmount = if (hasSelection) portions[selectedIndex].second else total
        val percentage = if (hasSelection) (portions[selectedIndex].second / total * 100) else 100.0

        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val dotColor = if (hasSelection) segmentColors[selectedIndex % segmentColors.size] else MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                    )
                    AnimatedContent(
                        targetState = titleText to hasSelection,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                             scaleIn(initialScale = 0.9f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)) + 
                                          scaleOut(targetScale = 0.9f, animationSpec = tween(90)))
                        },
                        label = "TitleEnlarger"
                    ) { (title, isSel) ->
                        Text(
                            text = title,
                            style = if (isSel) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("donut_center_title")
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    AnimatedContent(
                        targetState = displayAmount to hasSelection,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                             scaleIn(initialScale = 0.85f, animationSpec = tween(220, delayMillis = 90)))
                            .togetherWith(fadeOut(animationSpec = tween(90)) + 
                                          scaleOut(targetScale = 0.85f, animationSpec = tween(90)))
                        },
                        label = "AmountEnlarger"
                    ) { (amount, isSel) ->
                        Text(
                            text = CurrencyUtils.formatRupees(amount),
                            style = if (isSel) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("donut_center_amount")
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                        style = if (hasSelection) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                        color = if (hasSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (hasSelection) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedContentText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
    modifier: Modifier = Modifier
) {
    // Simple micro fade and scale animation for updating labels
    var textState by remember { mutableStateOf(text) }
    var scaleTrigger by remember { mutableStateOf(1f) }

    LaunchedEffect(text) {
        if (text != textState) {
            scaleTrigger = 0.95f
            delay(50)
            textState = text
            scaleTrigger = 1.05f
            delay(50)
            scaleTrigger = 1.0f
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = scaleTrigger,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "LabelScaleAnimation"
    )

    Text(
        text = textState,
        style = style,
        color = color,
        fontWeight = fontWeight,
        modifier = modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
    )
}

@Composable
fun CategoryBreakdownRow(
    categoryName: String,
    amount: Double,
    percentage: Float,
    barColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationTrigger by remember { mutableStateOf(false) }
    
    // Animate progress bar fill smoothly upon entry or selection changes
    LaunchedEffect(percentage) {
        animationTrigger = false
        delay(30)
        animationTrigger = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTrigger) percentage else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ProgressAnimation"
    )

    // Scale animation of selected row
    val rowScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "RowScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clickable(onClick = onClick),
        shape = AppShapes.roundedCardMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = AppDimens.borderWidthThick,
                color = barColor
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingNormal)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small color indicator circle
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = barColor
                    ) {}
                    Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = CurrencyUtils.formatRupees(amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
            
            // Percentage indicator & progress track
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MonthlySummaryRow(
    income: Double,
    expense: Double,
    savings: Double,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.roundedCardLarge,
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingNormal)
        ) {
            Text(
                text = "Monthly Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
            ) {
                // Income Column
                SummaryItem(
                    title = "Income",
                    amount = income,
                    amountColor = IncomeGreen, // Green
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                // Expense Column
                SummaryItem(
                    title = "Expense",
                    amount = expense,
                    amountColor = MaterialTheme.colorScheme.error, // Red/Pink
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    modifier = Modifier.weight(1f)
                )
                // Savings Column
                SummaryItem(
                    title = "Savings",
                    amount = savings,
                    amountColor = if (savings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    icon = Icons.Default.PieChart,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    title: String,
    amount: Double,
    amountColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = AppDimens.paddingSmall, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = amountColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = CurrencyUtils.formatRupees(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor,
            maxLines = 1
        )
    }
}

@Composable
fun IncomeExpenseComparisonChart(
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val maxAmount = maxOf(income, expense, 1000.0)
    val incomeRatio = (income / maxAmount).toFloat().coerceIn(0.05f, 1f)
    val expenseRatio = (expense / maxAmount).toFloat().coerceIn(0.05f, 1f)

    var animTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(income, expense) {
        animTrigger = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "bar_growth"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.roundedCardLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingNormal)
        ) {
            Text(
                text = "Income vs Expense Chart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Grid background lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            thickness = 1.dp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppDimens.paddingLarge),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Income Bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = CurrencyUtils.formatRupees(income),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(incomeRatio * animatedProgress)
                                .width(48.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            IncomeGreen.copy(alpha = 0.5f),
                                            IncomeGreen
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Expense Bar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = CurrencyUtils.formatRupees(expense),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(expenseRatio * animatedProgress)
                                .width(48.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            ExpenseRed.copy(alpha = 0.5f),
                                            ExpenseRed
                                        )
                                    ),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingTrendChart(
    dailyExpenses: List<Pair<Int, Double>>,
    modifier: Modifier = Modifier,
    timePeriod: com.example.ui.viewmodel.TimePeriod = com.example.ui.viewmodel.TimePeriod.MONTH
) {
    val maxAmount = dailyExpenses.maxOfOrNull { it.second } ?: 0.0
    val displayMax = if (maxAmount > 0) maxAmount else 1000.0
    val totalSpend = dailyExpenses.sumOf { it.second }
    val averageSpend = if (dailyExpenses.isNotEmpty()) totalSpend / dailyExpenses.size else 0.0
    
    var activeIndex by remember { mutableStateOf<Int?>(null) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    var animateChart by remember { mutableStateOf(false) }
    LaunchedEffect(dailyExpenses) {
        animateChart = true
    }
    val progress by animateFloatAsState(
        targetValue = if (animateChart) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "trend_anim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.roundedCardLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingNormal)
        ) {
            val titleText = when (timePeriod) {
                com.example.ui.viewmodel.TimePeriod.DAY -> "Spending Trend (Today)"
                com.example.ui.viewmodel.TimePeriod.WEEK -> "Spending Trend (This Week)"
                com.example.ui.viewmodel.TimePeriod.MONTH -> "Spending Trend (This Month)"
                com.example.ui.viewmodel.TimePeriod.YEAR -> "Spending Trend (This Year)"
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Dynamic subtitle showing values on hover/drag or average spend
            val subtitleText = if (activeIndex != null && activeIndex!! in dailyExpenses.indices) {
                val point = dailyExpenses[activeIndex!!]
                val label = when (timePeriod) {
                    com.example.ui.viewmodel.TimePeriod.DAY -> "Hour ${point.first}:00"
                    com.example.ui.viewmodel.TimePeriod.WEEK -> {
                        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        if (point.first in 1..7) dayNames[point.first - 1] else "Day ${point.first}"
                    }
                    com.example.ui.viewmodel.TimePeriod.MONTH -> "Day ${point.first}"
                    com.example.ui.viewmodel.TimePeriod.YEAR -> {
                        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        if (point.first in 0..11) monthNames[point.first] else "Month ${point.first + 1}"
                    }
                }
                "$label • ${CurrencyUtils.formatRupees(point.second)}"
            } else {
                val periodName = when (timePeriod) {
                    com.example.ui.viewmodel.TimePeriod.DAY -> "hourly"
                    com.example.ui.viewmodel.TimePeriod.WEEK -> "daily"
                    com.example.ui.viewmodel.TimePeriod.MONTH -> "daily"
                    com.example.ui.viewmodel.TimePeriod.YEAR -> "monthly"
                }
                "Avg spend: ${CurrencyUtils.formatRupees(averageSpend)}/$periodName • Hold & drag to inspect"
            }
            
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (activeIndex != null) FontWeight.Bold else FontWeight.Normal,
                color = if (activeIndex != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val lineColor = MaterialTheme.colorScheme.primary
                val gradientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                val baselineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                val tooltipBgColor = MaterialTheme.colorScheme.surfaceVariant
                val avgLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                val avgTextColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f).toArgb()

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp, bottom = 16.dp, start = 12.dp, end = 12.dp)
                        .pointerInput(dailyExpenses) {
                            if (dailyExpenses.size > 1) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val step = size.width / (dailyExpenses.size - 1)
                                    var idx = (down.position.x / step).roundToInt().coerceIn(0, dailyExpenses.size - 1)
                                    activeIndex = idx
                                    
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val anyDown = event.changes.any { it.pressed }
                                        if (!anyDown) {
                                            activeIndex = null
                                            break
                                        }
                                        val first = event.changes.firstOrNull { it.pressed }
                                        if (first != null) {
                                            idx = (first.position.x / step).roundToInt().coerceIn(0, dailyExpenses.size - 1)
                                            activeIndex = idx
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = dailyExpenses.size

                    // Draw Average Daily Spend Reference Line
                    if (pointsCount > 1 && averageSpend > 0.0) {
                        val rawAvgY = (averageSpend / displayMax).toFloat() * height
                        val avgY = height - (rawAvgY * progress)
                        
                        drawLine(
                            color = avgLineColor,
                            start = Offset(0f, avgY),
                            end = Offset(width, avgY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                        
                        // Small label for average line
                        val avgText = "Avg"
                        val avgPaint = android.graphics.Paint().apply {
                            color = avgTextColor
                            textSize = 8.dp.toPx()
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            avgText,
                            4.dp.toPx(),
                            avgY - 4.dp.toPx(),
                            avgPaint
                        )
                    }

                    if (pointsCount > 1 && maxAmount > 0.0) {
                        val path = Path()
                        val fillPath = Path()

                        val stepX = width / (pointsCount - 1)
                        
                        // Start drawing
                        dailyExpenses.forEachIndexed { index, pair ->
                            val x = index * stepX
                            val rawY = (pair.second / displayMax).toFloat() * height
                            val y = height - (rawY * progress)

                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (index - 1) * stepX
                                val prevRawY = (dailyExpenses[index - 1].second / displayMax).toFloat() * height
                                val prevY = height - (prevRawY * progress)
                                val controlX1 = prevX + (stepX / 2f)
                                val controlY1 = prevY
                                val controlX2 = prevX + (stepX / 2f)
                                val controlY2 = y
                                
                                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                            }

                            if (index == pointsCount - 1) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }
                        }

                        // Draw filled gradient under path
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(gradientColor, Color.Transparent)
                            )
                        )

                        // Draw the line
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw interactive highlight guide line, pulsing dot, and float tooltip card
                        if (activeIndex != null && activeIndex!! in dailyExpenses.indices) {
                            val idx = activeIndex!!
                            val pair = dailyExpenses[idx]
                            val hX = idx * stepX
                            val hRawY = (pair.second / displayMax).toFloat() * height
                            val hY = height - (hRawY * progress)
                            
                            // 1. Vertical guideline
                            drawLine(
                                color = lineColor.copy(alpha = 0.35f),
                                start = Offset(hX, 0f),
                                end = Offset(hX, height),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            
                            // 2. Highlighting Pulsing dot
                            drawCircle(
                                color = lineColor,
                                radius = 7.dp.toPx(),
                                center = Offset(hX, hY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = Offset(hX, hY)
                            )
                            
                            // 3. Floating Tooltip above highlight dot
                            val tooltipText = "₹${pair.second.toInt()}"
                            val paint = android.graphics.Paint().apply {
                                color = textColor
                                textSize = 9.dp.toPx()
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            
                            val textWidth = paint.measureText(tooltipText)
                            val textHeight = paint.textSize
                            val rectLeft = (hX - (textWidth / 2f) - 6.dp.toPx()).coerceAtLeast(0f).coerceAtMost(width - textWidth - 12.dp.toPx())
                            val rectRight = rectLeft + textWidth + 12.dp.toPx()
                            val rectTop = (hY - textHeight - 12.dp.toPx()).coerceAtLeast(0f)
                            val rectBottom = rectTop + textHeight + 6.dp.toPx()
                            
                            drawRoundRect(
                                color = tooltipBgColor,
                                topLeft = Offset(rectLeft, rectTop),
                                size = Size(rectRight - rectLeft, rectBottom - rectTop),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            
                            drawContext.canvas.nativeCanvas.drawText(
                                tooltipText,
                                rectLeft + (rectRight - rectLeft) / 2f,
                                rectTop + textHeight + 2.dp.toPx(),
                                paint
                            )
                        } else {
                            // Draw normal small dots on days with non-zero spending when not interacting
                            dailyExpenses.forEachIndexed { index, pair ->
                                if (pair.second > 0) {
                                    val x = index * stepX
                                    val rawY = (pair.second / displayMax).toFloat() * height
                                    val y = height - (rawY * progress)
                                    drawCircle(
                                        color = lineColor,
                                        radius = 3.5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 1.5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }
                            }
                        }
                    } else {
                        // If empty, draw a beautiful horizontal baseline
                        drawLine(
                            color = baselineColor,
                            start = Offset(0f, height),
                            end = Offset(width, height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            // Days timeline label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                when (timePeriod) {
                    com.example.ui.viewmodel.TimePeriod.DAY -> {
                        Text("0:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("8:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("16:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("23:00", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.example.ui.viewmodel.TimePeriod.WEEK -> {
                        Text("Mon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Wed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Fri", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Sun", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.example.ui.viewmodel.TimePeriod.MONTH -> {
                        Text("Day 1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Day 10", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Day 20", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Day ${dailyExpenses.size.coerceAtLeast(30)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.example.ui.viewmodel.TimePeriod.YEAR -> {
                        Text("Jan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Apr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Jul", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Dec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
