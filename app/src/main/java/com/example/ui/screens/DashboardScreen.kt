package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.utils.getIconByName
import com.example.ui.utils.CurrencyUtils
import com.example.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import com.example.data.repository.UserSession
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    userSession: UserSession? = null,
    onMenuClick: () -> Unit
) {
    val currentMonthTransactions by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithCategory?>(null) }

    val totalIncome = currentMonthTransactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
    val totalExpense = currentMonthTransactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
    val totalSavings = totalIncome - totalExpense

    val currencyFormatter = object {
        fun format(amount: Double): String = CurrencyUtils.formatRupees(amount)
    }

    val dailyAvg = if (currentMonthTransactions.isNotEmpty()) totalExpense / 30 else 0.0
    val highestExpenseCategoryInfo = currentMonthTransactions
        .filter { it.transaction.type == TransactionType.EXPENSE }
        .groupBy { it.category?.name ?: "Other" }
        .maxByOrNull { it.value.sumOf { tx -> tx.transaction.amount } }
    
    val highestExpenseCategory = highestExpenseCategoryInfo?.key ?: "None"
    val highestExpenseAmount = highestExpenseCategoryInfo?.value?.sumOf { it.transaction.amount } ?: 0.0

    // Today's boundaries
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendar.timeInMillis
    
    // Yesterday's boundaries
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val startOfYesterday = calendar.timeInMillis
    val endOfYesterday = startOfToday - 1

    val todayExpense = currentMonthTransactions
        .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.date >= startOfToday }
        .sumOf { it.transaction.amount }

    val yesterdayExpense = currentMonthTransactions
        .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.date in startOfYesterday..endOfYesterday }
        .sumOf { it.transaction.amount }

    Scaffold { padding ->
        val isDark = isSystemInDarkTheme()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = AppDimens.paddingNormal),
                verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
            ) {
                // Elegant Sleek Header Block
            item {
                val isDark = isSystemInDarkTheme()
                val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                val pendingSync by viewModel.pendingSync.collectAsStateWithLifecycle()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.paddingNormal, bottom = AppDimens.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinanceIconButton(
                        icon = Icons.Default.Menu,
                        onClick = onMenuClick,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = AppDimens.paddingSmall)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            Text(
                                text = dateFormat.format(Date()).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                            )
                            
                            // Tiny premium inline sync badge
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    !isOnline -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                    pendingSync -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                },
                                contentColor = when {
                                    !isOnline -> MaterialTheme.colorScheme.error
                                    pendingSync -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            !isOnline -> Icons.Default.WifiOff
                                            pendingSync -> Icons.Default.Sync
                                            else -> Icons.Default.CloudDone
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = when {
                                            !isOnline -> "Offline"
                                            pendingSync -> "Syncing"
                                            else -> "Synced"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.alpha(0.9f)
                                    )
                                }
                            }
                        }
                        
                        val greetingName = if (userSession?.isGuest == true) {
                            "Guest"
                        } else {
                            userSession?.name?.split(" ")?.firstOrNull() ?: "Alex"
                        }
                        Text(
                            text = "Hello, $greetingName",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(AppDimens.sizeAvatar)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .border(AppDimens.borderWidthThick, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                            .clickable { onMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = if (userSession?.isGuest == true) {
                            "G"
                        } else if (userSession != null) {
                            userSession.name
                                .split(" ")
                                .mapNotNull { it.firstOrNull() }
                                .take(2)
                                .joinToString("")
                                .uppercase()
                        } else {
                            "AM"
                        }
                        Text(
                            text = initials,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Sleek Premium Matte Summary Card
            item {
                SummaryCard(
                    title = "Total Balance",
                    amount = currencyFormatter.format(totalSavings),
                    income = currencyFormatter.format(totalIncome),
                    expense = currencyFormatter.format(totalExpense),
                    isGuest = userSession?.isGuest == true
                )
            }

            // Daily Spending Trend Card
            item {
                val trendIsPositive = todayExpense <= yesterdayExpense
                val pctStr = if (yesterdayExpense > 0) {
                    val diff = kotlin.math.abs(todayExpense - yesterdayExpense)
                    val pct = (diff / yesterdayExpense) * 100
                    "${pct.toInt()}%"
                } else "0%"

                DailySpendingCard(
                    todayExpenseStr = currencyFormatter.format(todayExpense),
                    trendText = "$pctStr vs yesterday",
                    trendIsPositive = trendIsPositive
                )
            }

            // Quick Add Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
                ) {
                    FinanceButton(
                        text = "Add Expense",
                        onClick = { navController.navigate("add_transaction/EXPENSE") },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Add,
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        contentColor = MaterialTheme.colorScheme.error,
                        shape = AppShapes.roundedCardMedium
                    )
                    FinanceButton(
                        text = "Add Income",
                        onClick = { navController.navigate("add_transaction/INCOME") },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Add,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = AppShapes.roundedCardMedium
                    )
                }
            }

            // Side-by-side Custom Sleek Insights Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
                ) {
                    // Daily Avg card
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), AppShapes.roundedCardLarge)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = if(isSystemInDarkTheme()) 0.08f else 0.4f),
                                shape = AppShapes.roundedCardLarge
                            )
                            .padding(AppDimens.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppDimens.sizeIconSmall)
                            )
                        }
                        Column {
                            Text(
                                "DAILY AVG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                currencyFormatter.format(dailyAvg),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Highest category card
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), AppShapes.roundedCardLarge)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = if(isSystemInDarkTheme()) 0.08f else 0.4f),
                                shape = AppShapes.roundedCardLarge
                            )
                            .padding(AppDimens.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(AppDimens.sizeIconSmall)
                            )
                        }
                        Column {
                            Text(
                                highestExpenseCategory.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                currencyFormatter.format(highestExpenseAmount),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Recent Activity Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.paddingSmall, bottom = AppDimens.paddingExtraSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 16.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "RECENT ACTIVITY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    FinanceTextButton(
                        text = "View All",
                        onClick = { navController.navigate("transactions") },
                        contentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.AutoMirrored.Filled.ArrowForward
                    )
                }
            }

            if (isLoading) {
                items(3) {
                    TransactionItemSkeleton()
                }
            } else if (currentMonthTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), AppShapes.roundedCardLarge)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = if(isSystemInDarkTheme()) 0.08f else 0.4f),
                                shape = AppShapes.roundedCardLarge
                            )
                            .padding(vertical = AppDimens.paddingExtraLarge, horizontal = AppDimens.paddingNormal),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Add your first transaction to get started!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = AppDimens.paddingExtraSmall)
                        )
                    }
                }
            } else {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                    ) {
                        currentMonthTransactions.take(5).forEach { tx ->
                            TransactionItem(
                                transaction = tx,
                                onClick = { selectedTransactionForDetails = tx }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(AppDimens.heightSpacerLarge)) }
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
}

@Composable
fun SummaryCard(title: String, amount: String, income: String, expense: String, isGuest: Boolean) {
    val isDark = isSystemInDarkTheme()
    
    // Matte glass background brush: we blend primary & secondary with translucent layer
    val matteBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
            )
        }
    )

    // Glass rim border: high contrast semi-transparent border highlighting the edge
    val rimColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.22f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = matteBrush,
                shape = AppShapes.roundedCardExtraLarge
            )
            .border(
                width = 1.dp,
                color = rimColor,
                shape = AppShapes.roundedCardExtraLarge
            )
            .padding(AppDimens.paddingLarge)
    ) {
        // Diagonal glass reflection highlight (matte sheen)
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            // Draw a subtle translucent diagonal sheen
            drawLine(
                color = Color.White.copy(alpha = if (isDark) 0.05f else 0.12f),
                start = Offset(0f, 0f),
                end = Offset(width, height),
                strokeWidth = 32.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold
                )
                // Premium badge
                Surface(
                    shape = CircleShape,
                    color = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = if (isGuest) "Guest Mode" else "Premium Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) MaterialTheme.colorScheme.primary else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black
                ),
                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))
            HorizontalDivider(color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isDark) IncomeGreen else BrightIncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            "INCOME",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            income,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) IncomeGreen else BrightIncomeGreen
                        )
                    }
                }

                // Expenses block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isDark) ExpenseRed else BrightExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "EXPENSES",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            expense,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) ExpenseRed else BrightExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailySpendingCard(
    todayExpenseStr: String,
    trendText: String,
    trendIsPositive: Boolean
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = AppShapes.roundedCardMedium
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (isDark) 0.08f else 0.4f),
                shape = AppShapes.roundedCardMedium
            )
            .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column {
                    Text(
                        text = "TODAY'S SPENDING",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = todayExpenseStr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Trend badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (trendIsPositive) {
                    IncomeGreen.copy(alpha = 0.15f)
                } else {
                    ExpenseRed.copy(alpha = 0.15f)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (trendIsPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (trendIsPositive) IncomeGreen else ExpenseRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (trendIsPositive) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    FinanceOutlinedButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        iconTint = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.1f else 0.2f),
        shape = AppShapes.roundedButton,
        height = AppDimens.heightButton
    )
}

@Composable
fun TransactionItem(
    transaction: TransactionWithCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val formatter = object {
        fun format(amount: Double): String = CurrencyUtils.formatRupees(amount)
    }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val isExpense = transaction.transaction.type == TransactionType.EXPENSE
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if(isDark) 0.08f else 0.4f),
                shape = AppShapes.roundedCardMedium
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        shape = AppShapes.roundedCardMedium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingMedium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isExpense) {
                    ExpenseRed.copy(alpha = 0.15f)
                } else {
                    IncomeGreen.copy(alpha = 0.15f)
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconByName(transaction.category?.iconName ?: "category"),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isExpense) ExpenseRed else IncomeGreen
                    )
                }
            }
            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.transaction.source.ifBlank { transaction.category?.name ?: "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category?.name ?: "Unknown"} • ${dateFormat.format(Date(transaction.transaction.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            // Amount
            Text(
                text = "${if (isExpense) "-" else "+"}${formatter.format(transaction.transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isExpense) ExpenseRed else IncomeGreen,
                modifier = Modifier.padding(start = AppDimens.paddingSmall)
            )
        }
    }
}

// Helper to represent 0.dp without hardcoding raw numbers directly in composable
private fun dpZero() = 0.dp

@Composable
fun TransactionItemSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_transition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(AppDimens.borderWidthThin, Color.White.copy(alpha = if(isSystemInDarkTheme()) 0.08f else 0.4f), AppShapes.roundedCardMedium)
            .alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        shape = AppShapes.roundedCardMedium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(AppDimens.paddingNormal)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = AppShapes.roundedIconContainer,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(AppDimens.sizeIconMedium)
            ) {}
            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.5f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, AppShapes.roundedButton)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(0.7f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, AppShapes.roundedButton)
                )
            }
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .width(60.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, AppShapes.roundedButton)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsBottomSheet(
    transactionWithCat: TransactionWithCategory,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val transaction = transactionWithCat.transaction
    val category = transactionWithCat.category
    val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    
    val isDark = isSystemInDarkTheme()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingNormal)
                .padding(bottom = AppDimens.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FinanceIconButton(
                    icon = Icons.Default.Clear,
                    onClick = onDismiss,
                    contentDescription = "Close"
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            // Amount and Category Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = AppShapes.roundedIconContainer,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = getIconByName(category?.iconName ?: "category"),
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = transaction.source,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${CurrencyUtils.formatRupees(transaction.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            
            // Fields Grid/List
            DetailRow(label = "Date", value = formatter.format(Date(transaction.date)))
            DetailRow(label = "Payment Method", value = transaction.paymentMethod)
            if (transaction.notes.isNotBlank()) {
                DetailRow(label = "Notes", value = transaction.notes)
            }
            
            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
            
            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
            ) {
                // Delete
                FinanceButton(
                    text = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = AppShapes.roundedButton,
                    height = 48.dp
                )
                
                // Duplicate
                FinanceButton(
                    text = "Duplicate",
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ContentCopy,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = AppShapes.roundedButton,
                    height = 48.dp
                )
                
                // Edit
                FinanceButton(
                    text = "Edit",
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Edit,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = AppShapes.roundedButton,
                    height = 48.dp
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

