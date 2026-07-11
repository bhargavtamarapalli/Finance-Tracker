package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    userSession: UserSession? = null,
    onMenuClick: () -> Unit,
    onSignOut: () -> Unit = {},
    onSignIn: () -> Unit = {}
) {
    val periodTransactions by viewModel.periodTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
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
        onDeleteTransaction = { viewModel.deleteTransaction(it.transaction) },
        onSignOut = onSignOut,
        onSignIn = onSignIn
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
    onDeleteTransaction: (TransactionWithCategory) -> Unit,
    onSignOut: () -> Unit = {},
    onSignIn: () -> Unit = {}
) {
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithCategory?>(null) }

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

    val isTrendPositive = trendPercentage <= 0
    val trendText = "${String.format(java.util.Locale.US, "%.1f", Math.abs(trendPercentage))}% from last month"

    val savingsPercentage = if (totalIncome > 0) {
        ((totalIncome - totalExpense) / totalIncome) * 100
    } else 0.0
    val savingsText = "${String.format(java.util.Locale.US, "%.0f", savingsPercentage)}%"
    val savingsValue = (savingsPercentage / 100.0).toFloat().coerceIn(0f, 1f)

    val budgetGoalProgress = if (monthlyBudgetGoal > 0) (totalExpense / monthlyBudgetGoal) else 0.0
    val budgetProgressValue = budgetGoalProgress.toFloat().coerceIn(0f, 1f)
    val budgetProgressText = "${String.format(java.util.Locale.US, "%.0f", budgetGoalProgress * 100)}% of ${CurrencyUtils.formatRupees(monthlyBudgetGoal)}"

    val budgetLeft = (monthlyBudgetGoal - totalExpense)
    val budgetLeftText = CurrencyUtils.formatRupees(budgetLeft)

    val topCategories = periodTransactions
        .filter { it.transaction.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { tx -> tx.transaction.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    // Show up to 7 recent transactions
    val recentTransactions = allTransactions.take(7)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            var showUserMenu by remember { mutableStateOf(false) }
            val isLoggedIn = userSession?.name != null && userSession.name != "Guest User"
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Finance Manager",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (isLoggedIn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandPrimary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = userSession?.name?.split(" ")?.firstOrNull() ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    Box {
                        ProfileAvatar(
                            name = userSession?.name,
                            isGuest = userSession?.isGuest == true,
                            size = 34.dp,
                            onClick = { showUserMenu = true }
                        )
                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            if (userSession != null) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = userSession.name ?: "Guest User",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (userSession.isGuest) "Guest Session" else (userSession.email ?: ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    },
                                    onClick = { showUserMenu = false },
                                    leadingIcon = {
                                        ProfileAvatar(
                                            name = userSession.name,
                                            isGuest = userSession.isGuest,
                                            size = 32.dp
                                        )
                                    }
                                )
                                HorizontalDivider()
                                if (userSession.isGuest) {
                                    DropdownMenuItem(
                                        text = { Text("Sign In / Register") },
                                        onClick = { showUserMenu = false; onSignIn() },
                                        leadingIcon = {
                                            Icon(Icons.Default.Login, contentDescription = null)
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Sign Out", color = ExpenseRed) },
                                        onClick = { showUserMenu = false; onSignOut() },
                                        leadingIcon = {
                                            Icon(Icons.Default.Logout, contentDescription = null, tint = ExpenseRed)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransactionClick("EXPENSE") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.paddingNormal),
            contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingIconInside)
        ) {
            item {
                HeroBalanceCard(
                    balance = CurrencyUtils.formatRupees(totalBalance),
                    trendText = trendText,
                    isTrendPositive = isTrendPositive,
                    savingsText = savingsText,
                    savingsValue = savingsValue,
                    budgetProgressText = budgetProgressText,
                    budgetProgressValue = budgetProgressValue
                )
            }

            item {
                StatPillRow(
                    income = CurrencyUtils.formatRupees(totalIncome),
                    expense = CurrencyUtils.formatRupees(totalExpense),
                    savings = savingsText,
                    budgetLeft = budgetLeftText,
                    budgetLeftAmount = budgetLeft
                )
            }

            // ─── 3. Top Spending Categories ──────────────────────────────────────
            if (topCategories.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            "TOP CATEGORIES",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingIconInside)) {
                            items(topCategories) { (cat, amount) ->
                                CategoryChip(
                                    name = cat?.name ?: "Unknown",
                                    amount = CurrencyUtils.formatRupees(amount),
                                    iconName = cat?.iconName ?: "category"
                                )
                            }
                        }
                    }
                }
            }

            // ─── 4. Recent Activity (header merged into card, tighter padding) ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.roundedCardMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row — tighter vertical padding (8dp instead of 12dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "RECENT ACTIVITY",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            TextButton(
                                onClick = onViewAllTransactionsClick,
                                contentPadding = PaddingValues(horizontal = AppDimens.paddingSmall, vertical = 0.dp)
                            ) {
                                Text("View All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            thickness = 1.dp
                        )

                        if (recentTransactions.isEmpty()) {
                            EmptyStatePlaceholder(
                                message = "No transactions yet",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppDimens.paddingExtraLarge)
                            )
                        } else {
                            // Transaction rows
                            recentTransactions.forEachIndexed { index, item ->
                                TransactionItem(
                                    transaction = item,
                                    modifier = Modifier.clickable { selectedTransactionForDetails = item },
                                    verticalPadding = 0.dp,
                                    shape = when {
                                        recentTransactions.size == 1 -> RoundedCornerShape(bottomStart = AppDimens.paddingNormal, bottomEnd = AppDimens.paddingNormal)
                                        index == recentTransactions.size - 1 -> RoundedCornerShape(bottomStart = AppDimens.paddingNormal, bottomEnd = AppDimens.paddingNormal)
                                        else -> RoundedCornerShape(0.dp)
                                    }
                                )
                                if (index < recentTransactions.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                                    )
                                }
                            }
                        }
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

// ─────────────────────────────────────────────────────────────────────────────
// Hero Balance Card (greeting merged in, tightened internals)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HeroBalanceCard(
    balance: String,
    trendText: String,
    isTrendPositive: Boolean,
    savingsText: String,
    savingsValue: Float,
    budgetProgressText: String,
    budgetProgressValue: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.roundedCardLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = AppDimens.paddingNormal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Balance ──────────────────────────────────────────────────────
            Text(
                "TOTAL BALANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
            Text(
                text = balance,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isTrendPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isTrendPositive) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = trendText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTrendPositive) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))

            // ── Key Insights row ─────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "KEY INSIGHTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "SPENDING EFFICIENCY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        savingsText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
                    LinearProgressIndicator(
                        progress = { savingsValue },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        color = IncomeGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        "BUDGET USED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        budgetProgressText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
                    LinearProgressIndicator(
                        progress = { budgetProgressValue },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact Stat Pill Row (replaces Monthly Summary card)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatPillRow(
    income: String,
    expense: String,
    savings: String,
    budgetLeft: String,
    budgetLeftAmount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
    ) {
        StatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ArrowUpward,
            iconTint = IncomeGreen,
            label = "Income",
            value = income,
            valueColor = IncomeGreen
        )
        StatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ArrowDownward,
            iconTint = ExpenseRed,
            label = "Expense",
            value = expense,
            valueColor = ExpenseRed
        )
        StatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Savings,
            iconTint = MaterialTheme.colorScheme.primary,
            label = "Savings",
            value = savings,
            valueColor = if (budgetLeftAmount >= 0) IncomeGreen else ExpenseRed
        )
        StatPill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AccountBalance,
            iconTint = MaterialTheme.colorScheme.tertiary,
            label = "Left",
            value = budgetLeft,
            valueColor = if (budgetLeftAmount >= 0) IncomeGreen else ExpenseRed
        )
    }
}

@Composable
fun StatPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AppDimens.paddingSmall, vertical = AppDimens.paddingIconInside),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Category Chip (shrunk width from 160dp → 130dp)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryChip(
    name: String,
    amount: String,
    iconName: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.width(130.dp),
        border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.paddingIconInside),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getIconByName(iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
            Column {
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    amount,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monthly Summary Card (kept for backward compat / test usage)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MonthlySummaryCard(
    income: String,
    expense: String,
    savings: String,
    budgetLeft: String,
    budgetLeftAmount: Double
) {
    Card(
        shape = AppShapes.roundedCardLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("MONTHLY SUMMARY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Income: ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(income, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                    Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Expenses: ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(expense, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Savings: ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(savings, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = IncomeGreen)
                    }
                    Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Budget Left: ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(budgetLeft, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (budgetLeftAmount >= 0.0) IncomeGreen else ExpenseRed)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TotalBalanceCard kept for backward compat / tests referencing it directly
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TotalBalanceCard(
    balance: String,
    trendText: String,
    isTrendPositive: Boolean,
    savingsText: String,
    savingsValue: Float,
    budgetProgressText: String,
    budgetProgressValue: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.roundedCardLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "TOTAL BALANCE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
            Text(
                text = balance,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isTrendPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isTrendPositive) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = trendText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTrendPositive) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingExtraLarge))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("KEY INSIGHTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(AppDimens.paddingMedium))
                    Text("SPENDING EFFICIENCY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(savingsText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
                    LinearProgressIndicator(
                        progress = { savingsValue },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = IncomeGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.paddingLarge))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text("BUDGET USED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(budgetProgressText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
                    LinearProgressIndicator(
                        progress = { budgetProgressValue },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
