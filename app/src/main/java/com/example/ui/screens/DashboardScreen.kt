package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.utils.getIconByName
import com.example.ui.utils.CurrencyUtils
import com.example.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import com.example.data.repository.UserSession
import java.text.SimpleDateFormat
import java.util.*

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
    val highestExpenseCategory = currentMonthTransactions
        .filter { it.transaction.type == TransactionType.EXPENSE }
        .groupBy { it.category?.name ?: "Other" }
        .maxByOrNull { it.value.sumOf { tx -> tx.transaction.amount } }?.key ?: "None"

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
        ) {
            // Elegant Sleek Header Block
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.paddingNormal, bottom = AppDimens.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.padding(end = AppDimens.paddingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = BrandPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        Text(
                            text = dateFormat.format(Date()).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeutralMedium,
                            fontWeight = FontWeight.Medium
                        )
                        val greetingName = if (userSession?.isGuest == true) {
                            "Guest"
                        } else {
                            userSession?.name?.split(" ")?.firstOrNull() ?: "Alex"
                        }
                        Text(
                            text = "Hello, $greetingName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeutralDark
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(AppDimens.sizeAvatar)
                            .background(AvatarBg, CircleShape)
                            .border(AppDimens.borderWidthThick, AvatarBorder, CircleShape)
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
                            color = AvatarText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppDimens.paddingExtraSmall),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
                    val pendingSync by viewModel.pendingSync.collectAsStateWithLifecycle()

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            !isOnline -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            pendingSync -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        },
                        contentColor = when {
                            !isOnline -> MaterialTheme.colorScheme.error
                            pendingSync -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(bottom = AppDimens.paddingSmall)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    !isOnline -> Icons.Default.WifiOff
                                    pendingSync -> Icons.Default.Sync
                                    else -> Icons.Default.CloudDone
                                },
                                contentDescription = "Sync Status Icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when {
                                    !isOnline -> "Offline Mode"
                                    pendingSync -> "Syncing Changes..."
                                    else -> "Data Synced to Cloud"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Sleek Gradient Summary Card
            item {
                SummaryCard(
                    title = "Total Balance",
                    amount = currencyFormatter.format(totalSavings),
                    income = currencyFormatter.format(totalIncome),
                    expense = currencyFormatter.format(totalExpense)
                )
            }

            // Quick Add Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
                ) {
                    Button(
                        onClick = { navController.navigate("add_transaction/EXPENSE") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = AppShapes.roundedCardMedium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed.copy(alpha = 0.1f),
                            contentColor = ExpenseRed
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Expense", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { navController.navigate("add_transaction/INCOME") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = AppShapes.roundedCardMedium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IncomeGreen.copy(alpha = 0.1f),
                            contentColor = IncomeGreen
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Income", fontWeight = FontWeight.Bold)
                    }
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
                            .background(SurfaceColor, AppShapes.roundedCardLarge)
                            .border(AppDimens.borderWidthThin, NeutralBorder, AppShapes.roundedCardLarge)
                            .padding(AppDimens.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(LightIncomeGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(AppDimens.sizeIconSmall)
                            )
                        }
                        Column {
                            Text(
                                "DAILY AVG",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeutralMedium
                            )
                            Text(
                                currencyFormatter.format(dailyAvg),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeutralDark
                            )
                        }
                    }

                    // Highest category card
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceColor, AppShapes.roundedCardLarge)
                            .border(AppDimens.borderWidthThin, NeutralBorder, AppShapes.roundedCardLarge)
                            .padding(AppDimens.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingMedium)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(LightExpenseRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(AppDimens.sizeIconSmall)
                            )
                        }
                        Column {
                            Text(
                                "HIGHEST",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeutralMedium
                            )
                            Text(
                                highestExpenseCategory,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeutralDark
                            )
                        }
                    }
                }
            }

            // Quick Actions Block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
                ) {
                    QuickActionButton(
                        text = "Add Income",
                        icon = Icons.Default.ArrowDownward,
                        color = IncomeGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("add_transaction/INCOME") }
                    )
                    QuickActionButton(
                        text = "Add Expense",
                        icon = Icons.Default.ArrowUpward,
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("add_transaction/EXPENSE") }
                    )
                }
            }

            // Recent Activity Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ACTIVITY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeutralMedium
                    )
                    TextButton(onClick = { navController.navigate("transactions") }) {
                        Text("View All", color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }
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
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = NeutralMedium,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
                        Text(
                            text = "No transactions found.",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeutralDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add your first one!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeutralMedium,
                            modifier = Modifier.padding(top = AppDimens.paddingSmall)
                        )
                    }
                }
            } else {
                items(
                    items = currentMonthTransactions.take(5),
                    key = { it.transaction.id }
                ) { tx ->
                    TransactionItem(
                        transaction = tx,
                        modifier = Modifier.animateItem(),
                        onClick = { selectedTransactionForDetails = tx }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(AppDimens.heightSpacerLarge)) }
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
fun SummaryCard(title: String, amount: String, income: String, expense: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientStart, GradientEnd)
                ),
                shape = AppShapes.roundedCardExtraLarge
            )
            .padding(AppDimens.paddingLarge)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
            Text(
                text = amount,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "INCOME",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        income,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF69F0AE) // Soft bright green accent for card background
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "EXPENSES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        expense,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80) // Soft bright red accent for card background
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
    Button(
        onClick = onClick,
        modifier = modifier.height(AppDimens.heightButton),
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceColor,
            contentColor = NeutralDark
        ),
        shape = AppShapes.roundedButton,
        border = androidx.compose.foundation.BorderStroke(AppDimens.borderWidthThin, NeutralBorder)
    ) {
        Icon(icon, contentDescription = text, tint = color)
        Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
        Text(text, fontWeight = FontWeight.Bold)
    }
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(AppDimens.borderWidthThin, CardBorderColor, AppShapes.roundedCardMedium),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppShapes.roundedCardMedium,
        elevation = CardDefaults.cardElevation(defaultElevation = dpZero())
    ) {
        Row(
            modifier = Modifier
                .padding(AppDimens.paddingNormal)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = AppShapes.roundedIconContainer,
                color = SurfaceVariantColor,
                modifier = Modifier.size(AppDimens.sizeIconMedium)
            ) {
                Icon(
                    imageVector = getIconByName(transaction.category?.iconName ?: "category"),
                    contentDescription = null,
                    modifier = Modifier.padding(AppDimens.paddingIconInside),
                    tint = BrandPrimary
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category?.name ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeutralDark
                )
                Text(
                    text = "${transaction.transaction.source} • ${dateFormat.format(Date(transaction.transaction.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMedium
                )
            }
            Text(
                text = "${if (isExpense) "-" else "+"}${formatter.format(transaction.transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) ExpenseRed else IncomeGreen
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
            .border(AppDimens.borderWidthThin, CardBorderColor, AppShapes.roundedCardMedium)
            .alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = AppShapes.roundedCardMedium,
        elevation = CardDefaults.cardElevation(defaultElevation = dpZero())
    ) {
        Row(
            modifier = Modifier
                .padding(AppDimens.paddingNormal)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = AppShapes.roundedIconContainer,
                color = NeutralBorder,
                modifier = Modifier.size(AppDimens.sizeIconMedium)
            ) {}
            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.5f)
                        .background(NeutralBorder, AppShapes.roundedButton)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(0.7f)
                        .background(NeutralBorder, AppShapes.roundedButton)
                )
            }
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .width(60.dp)
                    .background(NeutralBorder, AppShapes.roundedButton)
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
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
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
                    color = NeutralDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }
            
            HorizontalDivider(color = NeutralBorder)
            
            // Amount and Category Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = AppShapes.roundedIconContainer,
                    color = SurfaceVariantColor,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = getIconByName(category?.iconName ?: "category"),
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = BrandPrimary
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeutralDark
                    )
                    Text(
                        text = transaction.source,
                        style = MaterialTheme.typography.bodyLarge,
                        color = NeutralMedium
                    )
                }
                Text(
                    text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${CurrencyUtils.formatRupees(transaction.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                )
            }
            
            HorizontalDivider(color = NeutralBorder)
            
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
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = AppShapes.roundedButton
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(AppDimens.paddingExtraSmall))
                    Text("Delete")
                }
                
                // Duplicate
                Button(
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = AppShapes.roundedButton
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                    Spacer(modifier = Modifier.width(AppDimens.paddingExtraSmall))
                    Text("Duplicate")
                }
                
                // Edit
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = Color.White
                    ),
                    shape = AppShapes.roundedButton
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(AppDimens.paddingExtraSmall))
                    Text("Edit")
                }
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
            color = NeutralMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = NeutralDark,
            fontWeight = FontWeight.Bold
        )
    }
}

