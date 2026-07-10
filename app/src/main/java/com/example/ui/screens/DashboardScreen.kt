package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.repository.UserSession
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.utils.CurrencyUtils
import com.example.ui.utils.getIconByName
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    userSession: UserSession? = null,
    onMenuClick: () -> Unit
) {
    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle(initialValue = true)
    val pendingSync by viewModel.pendingSync.collectAsStateWithLifecycle(initialValue = false)
    val monthlyBudgetGoal by viewModel.monthlyBudgetGoal.collectAsStateWithLifecycle()

    DashboardContent(
        periodTransactions = periodTransactions,
        allTransactions = allTransactions,
        monthlyBudgetGoal = monthlyBudgetGoal,
        userSession = userSession,
        onMenuClick = onMenuClick,
        onAddTransactionClick = { type -> navController.navigate("add_transaction/$type") },
        onViewAllTransactionsClick = { navController.navigate("transactions") },
        onEditTransactionClick = { tx -> navController.navigate("add_transaction/${tx.transaction.type}?transactionId=${tx.transaction.id}") },
        onDuplicateTransactionClick = { tx -> navController.navigate("add_transaction/${tx.transaction.type}?transactionId=${tx.transaction.id}&duplicate=true") },
        onDeleteTransaction = { viewModel.deleteTransaction(it.transaction) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    periodTransactions: List<TransactionWithCategory>,
    allTransactions: List<TransactionWithCategory>,
    monthlyBudgetGoal: Double,
    userSession: UserSession? = null,
    onMenuClick: () -> Unit,
    onAddTransactionClick: (String) -> Unit,
    onViewAllTransactionsClick: () -> Unit,
    onEditTransactionClick: (TransactionWithCategory) -> Unit,
    onDuplicateTransactionClick: (TransactionWithCategory) -> Unit,
    onDeleteTransaction: (TransactionWithCategory) -> Unit
) {
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithCategory?>(null) }
    val isDark = isSystemInDarkTheme()

    val totalIncome = periodTransactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
    val totalExpense = periodTransactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
    val totalBalance = totalIncome - totalExpense
    
    val now = System.currentTimeMillis()
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
    val currentPeriodStart = now - thirtyDaysMs
    val previousPeriodStart = currentPeriodStart - thirtyDaysMs

    val currentPeriodExpense = allTransactions.filter { 
        it.transaction.type == TransactionType.EXPENSE && it.transaction.date >= currentPeriodStart 
    }.sumOf { it.transaction.amount }

    val previousPeriodExpense = allTransactions.filter {
        it.transaction.type == TransactionType.EXPENSE && it.transaction.date in previousPeriodStart until currentPeriodStart
    }.sumOf { it.transaction.amount }

    val trendPercentage = if (previousPeriodExpense > 0) {
        ((currentPeriodExpense - previousPeriodExpense) / previousPeriodExpense) * 100
    } else 0.0

    val isTrendPositive = trendPercentage <= 0 // Less expense is positive
    val trendText = "${String.format(java.util.Locale.US, "%.1f", Math.abs(trendPercentage))}% from last month"

    val savingsPercentage = if (totalIncome > 0) {
        ((totalIncome - totalExpense) / totalIncome) * 100
    } else 0.0
    val savingsText = "${String.format(java.util.Locale.US, "%.0f", savingsPercentage)}%"

    val budgetGoalProgress = if (monthlyBudgetGoal > 0) {
        (totalExpense / monthlyBudgetGoal)
    } else 0.0
    val budgetProgressValue = budgetGoalProgress.toFloat().coerceIn(0f, 1f)
    val budgetProgressText = "${String.format(java.util.Locale.US, "%.0f", budgetGoalProgress * 100)}% of ${CurrencyUtils.formatRupees(monthlyBudgetGoal)}"
    
    val budgetLeft = (monthlyBudgetGoal - totalExpense).coerceAtLeast(0.0)
    val currencyFormatter = object {
        fun format(amount: Double): String = CurrencyUtils.formatRupees(amount)
    }
    
    val budgetLeftText = currencyFormatter.format(budgetLeft)

    val topCategories = periodTransactions
        .filter { it.transaction.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { tx -> tx.transaction.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    Scaffold(
        
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = AppDimens.paddingLarge,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                    start = AppDimens.paddingNormal,
                    end = AppDimens.paddingNormal
                ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.paddingLarge)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userSession?.name?.firstOrNull()?.toString() ?: "A",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Hello, ${userSession?.name ?: "Alex"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Total Balance Card
                item {
                    TotalBalanceCard(
                        balance = currencyFormatter.format(totalBalance),
                        trendText = trendText,
                        isTrendPositive = isTrendPositive,
                        savingsText = savingsText,
                        budgetProgressText = budgetProgressText,
                        budgetProgressValue = budgetProgressValue,
                        isDark = isDark
                    )
                }

                // Top Spending Categories
                if (topCategories.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                "Top Spending Categories",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(topCategories) { (cat, amount) ->
                                    CategoryChip(
                                        name = cat?.name ?: "Other",
                                        amount = currencyFormatter.format(amount),
                                        iconName = cat?.iconName ?: "category",
                                        isDark = isDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Monthly Summary
                item {
                    Column {
                        Text(
                            "MONTHLY SUMMARY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MonthlySummaryCard(
                        income = currencyFormatter.format(totalIncome),
                        savingsText = savingsText,
                        expenses = currencyFormatter.format(totalExpense),
                        budgetLeft = budgetLeftText,
                        budgetLeftAmount = (monthlyBudgetGoal - totalExpense),
                        isDark = isDark
                    )
                    }
                }

                // Recent Activity
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "RECENT ACTIVITY",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            TextButton(onClick = onViewAllTransactionsClick) {
                                Text("View All")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                    shape = AppShapes.roundedCardMedium
                                )
                                .padding(vertical = 8.dp)
                        ) {
                            Column {
                                periodTransactions.take(3).forEachIndexed { index, tx ->
                                    com.example.ui.components.TransactionItem(transaction = tx, modifier = Modifier.clickable { selectedTransactionForDetails = tx })
                                    if (index < minOf(2, periodTransactions.size - 1)) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                                if (periodTransactions.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No recent activity",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } // Column
                                } // if isEmpty
                            } // Column
                        } // Box
                    } // Column
                } // item
            } // LazyColumn
        } // Box
    } // Scaffold body

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
                onDeleteTransaction(selectedTransactionForDetails!!)
                selectedTransactionForDetails = null
            }
        )
    }
}

@Composable
fun TotalBalanceCard(balance: String, trendText: String, isTrendPositive: Boolean, savingsText: String, budgetProgressText: String, budgetProgressValue: Float, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.15f else 0.4f),
                        Color.White.copy(alpha = if (isDark) 0.05f else 0.1f)
                    )
                ),
                shape = AppShapes.roundedCardLarge
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = AppShapes.roundedCardLarge
            )
            .padding(AppDimens.paddingLarge)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                "TOTAL BALANCE",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                balance,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isTrendPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isTrendPositive) BrightIncomeGreen else ExpenseRed, modifier = Modifier.size(20.dp))
                Text(trendText, style = MaterialTheme.typography.bodyMedium, color = if(isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "KEY INSIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SPENDING EFFICIENCY", style = MaterialTheme.typography.labelSmall, color = if(isDark) Color.White.copy(alpha=0.5f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.6f))
                        Text(savingsText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if(isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(40.dp).height(4.dp).background(Brush.horizontalGradient(listOf(IncomeGreen, ExpenseRed)), CircleShape))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BUDGET USED", style = MaterialTheme.typography.labelSmall, color = if(isDark) Color.White.copy(alpha=0.5f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.6f))
                        Text(budgetProgressText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if(isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.width(100.dp).height(6.dp).background(Color.White.copy(alpha=0.2f), CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(budgetProgressValue.coerceAtLeast(0.01f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(name: String, amount: String, iconName: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .background(
                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                shape = AppShapes.roundedCardMedium
            )
            .border(1.dp, if(isDark) Color.White.copy(alpha=0.05f) else MaterialTheme.colorScheme.outline.copy(alpha=0.1f), AppShapes.roundedCardMedium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(getIconByName(iconName), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(8.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(amount, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun MonthlySummaryCard(income: String, savingsText: String, expenses: String, budgetLeft: String, budgetLeftAmount: Double, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                shape = AppShapes.roundedCardMedium
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text("Income: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(income, style = MaterialTheme.typography.bodyMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
                Row {
                    Text("Savings: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(savingsText, style = MaterialTheme.typography.bodyMedium, color = IncomeGreen, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text("Expenses: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(expenses, style = MaterialTheme.typography.bodyMedium, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
                Row {
                    Text("Budget Left: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(budgetLeft, style = MaterialTheme.typography.bodyMedium, color = if (budgetLeftAmount >= 0) IncomeGreen else ExpenseRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}




