package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.AppTheme
import com.example.ui.utils.CurrencyOption
import kotlinx.coroutines.launch

enum class RestoreType { LOCAL, CLOUD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    authViewModel: AuthViewModel,
    onManageCategoriesClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val userSession by authViewModel.currentUserSession.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isSyncing by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf(false) }
    var restoreActionType by remember { mutableStateOf<RestoreType?>(null) }

    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setReminderEnabled(true, context)
            showToast(context, "Daily expense reminder enabled!", Toast.LENGTH_SHORT)
        } else {
            showToast(context, "Notification permission is required for reminders.", Toast.LENGTH_SHORT)
        }
    }

    // CSV Document Creator Launcher
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isSyncing = true
                try {
                    val csvContent = generateCsvContent(allTransactions)
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(csvContent.toByteArray())
                    }
                    showToast(context, "Transactions exported to CSV successfully!", Toast.LENGTH_SHORT)
                } catch (e: Exception) {
                    showToast(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG)
                } finally {
                    isSyncing = false
                }
            }
        }
    }

    if (showRestoreWarning) {
        AlertDialog(
            onDismissRequest = { showRestoreWarning = false },
            title = { Text("Confirm Data Restore", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to restore data? This will add or update categories and transactions. This action cannot be undone.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    onClick = {
                        showRestoreWarning = false
                        val type = restoreActionType
                        if (type != null) {
                            scope.launch {
                                isSyncing = true
                                if (type == RestoreType.CLOUD && !viewModel.isOnline.value) {
                                    isSyncing = false
                                    showToast(context, "Restore failed: No internet connection. Please check your network settings.", Toast.LENGTH_LONG)
                                    return@launch
                                }
                                val result = if (type == RestoreType.CLOUD) {
                                    val userId = userSession?.userId ?: ""
                                    viewModel.restoreFromFirebase(userId)
                                } else {
                                    viewModel.restoreLocally()
                                }
                                isSyncing = false
                                if (result.isSuccess) {
                                    showToast(context, "Data successfully restored!", Toast.LENGTH_SHORT)
                                } else {
                                    showToast(context, "Restore failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG)
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val currentCurrency by viewModel.currencyOption.collectAsStateWithLifecycle()

            // -------------------------------------------------------------
            // Section: Theme Settings
            // -------------------------------------------------------------
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            listOf(
                AppTheme.LIGHT to "Light",
                AppTheme.DARK to "Dark",
                AppTheme.SYSTEM to "System"
            ).forEach { (theme, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        val icon = when (theme) {
                            AppTheme.LIGHT -> Icons.Default.WbSunny
                            AppTheme.DARK -> Icons.Default.Brightness2
                            AppTheme.SYSTEM -> Icons.Default.Settings
                        }
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    trailingContent = {
                        RadioButton(
                            selected = (currentTheme == theme),
                            onClick = { viewModel.setTheme(theme) }
                        )
                    },
                    modifier = Modifier
                        .clickable { viewModel.setTheme(theme) }
                        .padding(vertical = AppDimens.paddingExtraSmall)
                )
            }
            HorizontalDivider()

            // -------------------------------------------------------------
            // Section: Currency Settings
            // -------------------------------------------------------------
            Text(
                text = "Currency",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            // Default Option: INR ₹
            ListItem(
                headlineContent = { Text("INR (₹)") },
                supportingContent = { Text("Default Currency", color = MaterialTheme.colorScheme.primary) },
                leadingContent = {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingContent = {
                    RadioButton(
                        selected = (currentCurrency == CurrencyOption.INR),
                        onClick = { viewModel.setCurrency(CurrencyOption.INR) }
                    )
                },
                modifier = Modifier
                    .clickable { viewModel.setCurrency(CurrencyOption.INR) }
                    .padding(vertical = AppDimens.paddingExtraSmall)
            )

            // Future / Multiple currencies Option Header
            Text(
                text = "Future Options (Multiple currencies)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingExtraSmall)
            )

            listOf(
                CurrencyOption.USD to "USD ($)",
                CurrencyOption.EUR to "EUR (€)",
                CurrencyOption.GBP to "GBP (£)"
            ).forEach { (currency, label) ->
                ListItem(
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = { },
                                label = { Text("Future", style = MaterialTheme.typography.labelSmall) },
                                enabled = false
                            )
                        }
                    },
                    supportingContent = { Text("Coming Soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    trailingContent = {
                        RadioButton(
                            selected = (currentCurrency == currency),
                            onClick = { viewModel.setCurrency(currency) }
                        )
                    },
                    modifier = Modifier
                        .clickable { viewModel.setCurrency(currency) }
                        .padding(vertical = AppDimens.paddingExtraSmall)
                )
            }
            HorizontalDivider()

            // -------------------------------------------------------------
            // Section: Profile Settings
            // -------------------------------------------------------------
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            userSession?.let { session ->
                ListItem(
                    headlineContent = { Text("Name") },
                    supportingContent = { Text(session.name, fontWeight = FontWeight.SemiBold) },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = "Name", tint = MaterialTheme.colorScheme.secondary)
                    }
                )
                HorizontalDivider()

                ListItem(
                    headlineContent = { Text("Email") },
                    supportingContent = { Text(if (session.isGuest) "Guest Account" else session.email) },
                    leadingContent = {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.secondary)
                    }
                )
                HorizontalDivider()
            }

            // -------------------------------------------------------------
            // Section: Notifications Settings
            // -------------------------------------------------------------
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            ListItem(
                headlineContent = { Text("Daily Expense Reminder") },
                supportingContent = { Text("Don't forget to record today's expenses.") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Notification Reminder Icon",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }

                                if (hasPermission) {
                                    viewModel.setReminderEnabled(true, context)
                                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.setReminderEnabled(false, context)
                            }
                        }
                    )
                },
                modifier = Modifier
                    .clickable {
                        val checked = !reminderEnabled
                        if (checked) {
                            val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            } else {
                                true
                            }

                            if (hasPermission) {
                                viewModel.setReminderEnabled(true, context)
                            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.setReminderEnabled(false, context)
                        }
                    }
                    .padding(vertical = AppDimens.paddingExtraSmall)
            )
            HorizontalDivider()

            // Sync Overlay indicator
            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Section 1: Categories
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            ListItem(
                headlineContent = { Text("Manage Categories") },
                supportingContent = { Text("Add, edit or remove custom categories.") },
                leadingContent = {
                    Icon(Icons.Default.Category, contentDescription = "Categories", tint = MaterialTheme.colorScheme.secondary)
                },
                modifier = Modifier
                    .clickable { onManageCategoriesClick() }
                    .padding(vertical = AppDimens.paddingExtraSmall)
            )
            HorizontalDivider()

            // Section 2: Data Management
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            // Export to CSV Option
            ListItem(
                headlineContent = { Text("Export to CSV") },
                supportingContent = { Text("Export all transaction data as a standard CSV spreadsheet.") },
                leadingContent = {
                    Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.secondary)
                },
                modifier = Modifier
                    .clickable(enabled = !isSyncing) {
                        val fileName = "finance_tracker_export_${System.currentTimeMillis()}.csv"
                        createCsvLauncher.launch(fileName)
                    }
                    .padding(vertical = AppDimens.paddingExtraSmall)
            )
            HorizontalDivider()

            // Cloud Sync Section (Firebase)
            userSession?.let { session ->
                if (!session.isGuest) {
                    ListItem(
                        headlineContent = { Text("Backup to Cloud (Firebase)") },
                        supportingContent = { Text("Securely sync and backup your data to the cloud.") },
                        leadingContent = {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Cloud Backup", tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier
                            .clickable(enabled = !isSyncing) {
                                scope.launch {
                                    isSyncing = true
                                    if (!viewModel.isOnline.value) {
                                        isSyncing = false
                                        showToast(context, "Backup failed: No internet connection. Please check your network settings.", Toast.LENGTH_LONG)
                                        return@launch
                                    }
                                    val result = viewModel.backupToFirebase(session.userId)
                                    isSyncing = false
                                    if (result.isSuccess) {
                                        showToast(context, "Backup successfully saved to cloud!", Toast.LENGTH_SHORT)
                                    } else {
                                        showToast(context, "Backup failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG)
                                    }
                                }
                            }
                            .padding(vertical = AppDimens.paddingExtraSmall)
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Restore from Cloud (Firebase)") },
                        supportingContent = { Text("Download your data from the cloud onto this device.") },
                        leadingContent = {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Cloud Restore", tint = MaterialTheme.colorScheme.secondary)
                        },
                        modifier = Modifier
                            .clickable(enabled = !isSyncing) {
                                restoreActionType = RestoreType.CLOUD
                                showRestoreWarning = true
                            }
                            .padding(vertical = AppDimens.paddingExtraSmall)
                    )
                    HorizontalDivider()
                } else {
                    // Guest Warning about Cloud Sync
                    ListItem(
                        headlineContent = { Text("Cloud Sync (Unavailable)") },
                        supportingContent = { Text("Sign in with an email account to enable secure Firebase cloud backup and sync.") },
                        leadingContent = {
                            Icon(Icons.Default.CloudOff, contentDescription = "Cloud Sync Offline", tint = MaterialTheme.colorScheme.outline)
                        },
                        modifier = Modifier.padding(vertical = AppDimens.paddingExtraSmall)
                    )
                    HorizontalDivider()
                }
            }

            // Local Backup Options
            ListItem(
                headlineContent = { Text("Backup to Local Storage") },
                supportingContent = { Text("Create a secure local copy of your financial data on this device.") },
                leadingContent = {
                    Icon(Icons.Default.Backup, contentDescription = "Local Backup", tint = MaterialTheme.colorScheme.secondary)
                },
                modifier = Modifier
                    .clickable(enabled = !isSyncing) {
                        scope.launch {
                            isSyncing = true
                            val result = viewModel.backupLocally()
                            isSyncing = false
                            if (result.isSuccess) {
                                showToast(context, "Local backup created successfully!", Toast.LENGTH_SHORT)
                            } else {
                                showToast(context, "Backup failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG)
                            }
                        }
                    }
                    .padding(vertical = AppDimens.paddingExtraSmall)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Restore from Local Storage") },
                supportingContent = { Text("Restore categories & transactions from local backup.") },
                leadingContent = {
                    Icon(Icons.Default.Restore, contentDescription = "Local Restore", tint = MaterialTheme.colorScheme.secondary)
                },
                modifier = Modifier
                    .clickable(enabled = !isSyncing) {
                        restoreActionType = RestoreType.LOCAL
                        showRestoreWarning = true
                    }
                    .padding(vertical = AppDimens.paddingExtraSmall)
            )
            HorizontalDivider()

            // About Screen Info
            Text(
                text = "App Info",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            ListItem(
                headlineContent = { Text("About") },
                supportingContent = { Text("Finance Tracker v1.1") },
                leadingContent = {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                        contentDescription = "Finance Tracker Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(com.example.ui.theme.AppShapes.roundedIconContainer)
                    )
                },
                modifier = Modifier.padding(vertical = AppDimens.paddingExtraSmall)
            )
            
            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))
            
            // Logout Button
            Button(
                onClick = { authViewModel.logout() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.paddingLarge),
                shape = AppShapes.roundedCardMedium
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun generateCsvContent(transactions: List<com.example.data.model.TransactionWithCategory>): String {
    val sb = java.lang.StringBuilder()
    sb.append("ID,Date,Amount,Source/Recipient,Category,Type,Notes,Payment Method\n")
    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    transactions.forEach { item ->
        val t = item.transaction
        val dateStr = dateFormat.format(java.util.Date(t.date))
        val categoryName = item.category?.name ?: "Uncategorized"
        val sourceEscaped = t.source.replace("\"", "\"\"")
        val notesEscaped = t.notes.replace("\"", "\"\"")
        sb.append("${t.id},")
          .append("\"$dateStr\",")
          .append("${t.amount},")
          .append("\"$sourceEscaped\",")
          .append("\"$categoryName\",")
          .append("${t.type.name},")
          .append("\"$notesEscaped\",")
          .append("\"${t.paymentMethod}\"\n")
    }
    return sb.toString()
}

private fun showToast(context: android.content.Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        Toast.makeText(context, message, duration).show()
    }
}
