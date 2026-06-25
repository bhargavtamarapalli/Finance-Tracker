package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.utils.CurrencyUtils
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    onMenuClick: () -> Unit
) {
    val currentMonthTransactions by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // Switch between Expense and Income analytics
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    
    // Track selected category index for donut chart highlighting
    var selectedCategoryIndex by remember { mutableStateOf(-1) }
    
    // Reset selection when tab switches
    LaunchedEffect(selectedType) {
        selectedCategoryIndex = -1
    }

    val totalIncome = remember(currentMonthTransactions) {
        currentMonthTransactions
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
    }
    
    val totalExpense = remember(currentMonthTransactions) {
        currentMonthTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
    }
    
    val netSavings = totalIncome - totalExpense

    // Group expenses by day of month for trend
    val dailyExpenses = remember(currentMonthTransactions) {
        val calInstance = java.util.Calendar.getInstance()
        val daysInMonth = calInstance.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val currentMonth = calInstance.get(java.util.Calendar.MONTH)
        val currentYear = calInstance.get(java.util.Calendar.YEAR)

        val map = mutableMapOf<Int, Double>()
        for (day in 1..daysInMonth) {
            map[day] = 0.0
        }
        currentMonthTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .forEach { tx ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.transaction.date }
                if (cal.get(java.util.Calendar.MONTH) == currentMonth && cal.get(java.util.Calendar.YEAR) == currentYear) {
                    val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    map[day] = (map[day] ?: 0.0) + tx.transaction.amount
                }
            }
        map.toList().sortedBy { it.first }
    }

    val filteredTransactions = currentMonthTransactions.filter { it.transaction.type == selectedType }
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
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("menu_button")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
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
            // Tab Toggler Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.roundedCardMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.paddingSmall),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                    ) {
                        FilterChip(
                            selected = selectedType == TransactionType.EXPENSE,
                            onClick = { selectedType = TransactionType.EXPENSE },
                            label = { Text("Expenses", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.sizeIconSmall)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_expense")
                        )
                        FilterChip(
                            selected = selectedType == TransactionType.INCOME,
                            onClick = { selectedType = TransactionType.INCOME },
                            label = { Text("Income", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.sizeIconSmall)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_income")
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.roundedCardLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedType == TransactionType.EXPENSE) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
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
                            color = if (selectedType == TransactionType.EXPENSE) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        Text(
                            text = CurrencyUtils.formatRupees(totalAmount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedType == TransactionType.EXPENSE) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
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
                                text = "Tap categories below to filter segment",
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
                                modifier = Modifier
                                    .size(200.dp)
                                    .testTag("donut_chart")
                            )
                        }
                    }
                }

                // Breakdown list header
                item {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = AppDimens.paddingSmall)
                    )
                }

                // List items
                itemsIndexed(breakdown) { index, item ->
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
                } // Close itemsIndexed
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
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweep by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "SweepAnimation"
    )

    LaunchedEffect(portions) {
        animationPlayed = true
    }

    if (total == 0.0) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val radius = min(width, height) / 2
            val strokeWidth = radius * 0.35f
            val baseSize = Size(width - strokeWidth, height - strokeWidth)
            val baseTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            var startAngle = -90f
            portions.forEachIndexed { index, portion ->
                val sweepAngle = ((portion.second / total) * 360f).toFloat() * animateSweep
                val isSelected = index == selectedIndex
                
                // Add a micro-animation popup thickness to selected segment
                val segmentStrokeWidth = if (isSelected) strokeWidth * 1.25f else strokeWidth

                drawArc(
                    color = segmentColors[index % segmentColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = segmentStrokeWidth, cap = StrokeCap.Round),
                    size = baseSize,
                    topLeft = baseTopLeft
                )
                startAngle += sweepAngle
            }
        }

        // Center labels
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val hasSelection = selectedIndex in portions.indices
            val titleText = if (hasSelection) portions[selectedIndex].first else "Total"
            val displayAmount = if (hasSelection) portions[selectedIndex].second else total

            // Micro-animation transitions on text selection changes
            AnimatedContentText(
                text = titleText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("donut_center_title")
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
            AnimatedContentText(
                text = CurrencyUtils.formatRupees(displayAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("donut_center_amount")
            )
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), AppShapes.roundedCardMedium)
            .padding(AppDimens.paddingSmall),
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
                                .fillMaxHeight(incomeRatio)
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
                                .fillMaxHeight(expenseRatio)
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
    modifier: Modifier = Modifier
) {
    val maxAmount = dailyExpenses.maxOfOrNull { it.second } ?: 0.0
    val displayMax = if (maxAmount > 0) maxAmount else 1000.0

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
                text = "Spending Trend (This Month)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Daily cumulative spending pattern",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 8.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val pointsCount = dailyExpenses.size

                    if (pointsCount > 1 && maxAmount > 0.0) {
                        val path = Path()
                        val fillPath = Path()

                        val stepX = width / (pointsCount - 1)
                        
                        // Start drawing
                        dailyExpenses.forEachIndexed { index, pair ->
                            val x = index * stepX
                            val y = height - ((pair.second / displayMax).toFloat() * height)

                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (index - 1) * stepX
                                val prevY = height - ((dailyExpenses[index - 1].second / displayMax).toFloat() * height)
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

                        // Draw individual dots on days with non-zero spending
                        dailyExpenses.forEachIndexed { index, pair ->
                            if (pair.second > 0) {
                                val x = index * stepX
                                val y = height - ((pair.second / displayMax).toFloat() * height)
                                drawCircle(
                                    color = lineColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = Offset(x, y)
                                )
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
                Text("Day 1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Day 10", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Day 20", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Day ${dailyExpenses.size.coerceAtLeast(30)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
