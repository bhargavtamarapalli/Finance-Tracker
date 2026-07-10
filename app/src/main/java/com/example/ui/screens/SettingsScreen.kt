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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.utils.CurrencyUtils

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.data.repository.UserSession
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
    onAdminConsoleClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val userSession by authViewModel.currentUserSession.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val currencyOption by viewModel.currencyOption.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val monthlyBudgetGoal by viewModel.monthlyBudgetGoal.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }

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

    SettingsContent(
        userSession = userSession,
        reminderEnabled = reminderEnabled,
        biometricLockEnabled = biometricLockEnabled,
        appTheme = appTheme,
        currencyOption = currencyOption,
        appMode = appMode,
        isSyncing = isSyncing,
        monthlyBudgetGoal = monthlyBudgetGoal,
        onBudgetGoalChange = { goal -> viewModel.updateMonthlyBudgetGoal(goal) },
        onReminderToggle = { enabled -> viewModel.setReminderEnabled(enabled, context) },
        onBiometricToggle = { enabled -> viewModel.setBiometricLockEnabled(enabled) },
        onThemeChange = { theme -> viewModel.setTheme(theme) },
        onCurrencyChange = { currency -> viewModel.setCurrency(currency) },
        onManageCategoriesClick = onManageCategoriesClick,
        onAdminConsoleClick = onAdminConsoleClick,
        onMenuClick = onMenuClick,
        onExportCsv = { fileName -> createCsvLauncher.launch(fileName) },
        onBackupCloud = {
            if (!isOnline) {
                showToast(context, "Backup failed: No internet connection. Please check your network settings.", Toast.LENGTH_LONG)
            } else {
                scope.launch {
                    isSyncing = true
                    val result = viewModel.backupToFirebase(userSession?.userId ?: "")
                    isSyncing = false
                    if (result.isSuccess) {
                        showToast(context, "Backup successfully saved to cloud!", Toast.LENGTH_SHORT)
                    } else {
                        showToast(context, "Backup failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG)
                    }
                }
            }
        },
        onBackupLocal = {
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
        },
        onRestoreCloud = {
            if (!isOnline) {
                showToast(context, "Restore failed: No internet connection. Please check your network settings.", Toast.LENGTH_LONG)
            } else {
                scope.launch {
                    isSyncing = true
                    val result = viewModel.restoreFromFirebase(userSession?.userId ?: "")
                    isSyncing = false
                    if (result.isSuccess) {
                        showToast(context, "Data successfully restored!", Toast.LENGTH_SHORT)
                    } else {
                        showToast(context, "Restore failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG)
                    }
                }
            }
        },
        onRestoreLocal = {
            scope.launch {
                isSyncing = true
                val result = viewModel.restoreLocally()
                isSyncing = false
                if (result.isSuccess) {
                    showToast(context, "Data successfully restored!", Toast.LENGTH_SHORT)
                } else {
                    showToast(context, "Restore failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG)
                }
            }
        },
        onUpdateProfile = { name, email, onSuccess, onError ->
            authViewModel.updateProfile(name, email, onSuccess, onError)
        },
        onSignOut = {
            scope.launch {
                authViewModel.logout()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    userSession: UserSession?,
    reminderEnabled: Boolean,
    biometricLockEnabled: Boolean,
    appTheme: AppTheme,
    currencyOption: CurrencyOption,
    appMode: String,
    isSyncing: Boolean,
    monthlyBudgetGoal: Double,
    onBudgetGoalChange: (Double) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onCurrencyChange: (CurrencyOption) -> Unit,
    onManageCategoriesClick: () -> Unit,
    onAdminConsoleClick: () -> Unit,
    onMenuClick: () -> Unit,
    onExportCsv: (String) -> Unit,
    onBackupCloud: () -> Unit,
    onBackupLocal: () -> Unit,
    onRestoreCloud: () -> Unit,
    onRestoreLocal: () -> Unit,
    onUpdateProfile: (name: String, email: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onReminderToggle(true)
            showToast(context, "Daily expense reminder enabled!", Toast.LENGTH_SHORT)
        } else {
            showToast(context, "Notification permission is required for reminders.", Toast.LENGTH_SHORT)
        }
    }
    var showRestoreWarning by remember { mutableStateOf(false) }
    var restoreActionType by remember { mutableStateOf<RestoreType?>(null) }
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }





    if (showRestoreWarning) {
        AlertDialog(
            onDismissRequest = { showRestoreWarning = false },
            title = { Text("Confirm Data Restore", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to restore data? This will add or update categories and transactions. This action cannot be undone.") },
            confirmButton = {
                FinanceButton(
                    text = "Restore",
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = {
                        showRestoreWarning = false
                        val type = restoreActionType
                        if (type != null) {
                            if (type == RestoreType.CLOUD) {
                                onRestoreCloud()
                            } else {
                                onRestoreLocal()
                            }
                        }
                    }
                )
            },
            dismissButton = {
                FinanceTextButton(
                    text = "Cancel",
                    onClick = { showRestoreWarning = false }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FinanceIconButton(
                        icon = Icons.Default.Menu,
                        onClick = onMenuClick,
                        contentDescription = "Menu"
                    )
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
            val currentTheme = appTheme
            val currentCurrency = currencyOption

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
                            onClick = { onThemeChange(theme) }
                        )
                    },
                    modifier = Modifier
                        .clickable { onThemeChange(theme) }
                        .padding(vertical = AppDimens.paddingExtraSmall)
                )
            }
            HorizontalDivider()

            // -------------------------------------------------------------
            // Section: Currency Settings
            // -------------------------------------------------------------
            // Budget Goal Section
            Text(
                text = "Budget",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )
            
            var isEditingBudget by remember { mutableStateOf(false) }
            var tempBudgetInput by remember(monthlyBudgetGoal) { mutableStateOf(monthlyBudgetGoal.toInt().toString()) }
            
            ListItem(
                headlineContent = { Text("Monthly Budget Goal") },
                supportingContent = { 
                    if (isEditingBudget) {
                        OutlinedTextField(
                            value = tempBudgetInput,
                            onValueChange = { tempBudgetInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    } else {
                        Text(CurrencyUtils.formatRupees(monthlyBudgetGoal))
                    }
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    if (isEditingBudget) {
                        Row {
                            IconButton(onClick = { isEditingBudget = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                            IconButton(onClick = { 
                                val newGoal = tempBudgetInput.toDoubleOrNull()
                                if (newGoal != null && newGoal > 0) {
                                    onBudgetGoalChange(newGoal)
                                }
                                isEditingBudget = false 
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    } else {
                        IconButton(onClick = { isEditingBudget = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Budget")
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
            )
            HorizontalDivider()

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
                        onClick = { onCurrencyChange(CurrencyOption.INR) }
                    )
                },
                modifier = Modifier
                    .clickable { onCurrencyChange(CurrencyOption.INR) }
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
                            onClick = { onCurrencyChange(currency) }
                        )
                    },
                    modifier = Modifier
                        .clickable { onCurrencyChange(currency) }
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
                if (session.isGuest) {
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
                        supportingContent = { Text("Guest Account") },
                        leadingContent = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.secondary)
                        }
                    )
                    HorizontalDivider()

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = AppShapes.roundedCardMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.paddingNormal),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Using Guest Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sign in or register to customize your profile, enable secure cloud sync, and protect your wealth details.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FinanceButton(
                                text = "Sign In / Register",
                                onClick = { onSignOut() },
                                icon = Icons.Default.Login,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    HorizontalDivider()
                } else {
                    ListItem(
                        headlineContent = { Text("Name") },
                        supportingContent = { Text(session.name, fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Icon(Icons.Default.Person, contentDescription = "Name", tint = MaterialTheme.colorScheme.secondary)
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                editName = session.name
                                editEmail = session.email
                                showEditProfileDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Email") },
                        supportingContent = { Text(session.email) },
                        leadingContent = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.secondary)
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                editName = session.name
                                editEmail = session.email
                                showEditProfileDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Email", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FinanceTextButton(
                            text = "Edit Profile Details",
                            onClick = {
                                editName = session.name
                                editEmail = session.email
                                showEditProfileDialog = true
                            },
                            icon = Icons.Default.Edit
                        )
                    }
                    HorizontalDivider()
                }
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
                                    onReminderToggle(true)
                                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                onReminderToggle(false)
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
                                onReminderToggle(true)
                            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            onReminderToggle(false)
                        }
                    }
                    .padding(vertical = AppDimens.paddingExtraSmall)
            )
            HorizontalDivider()

            // -------------------------------------------------------------
            // Section: Security Settings
            // -------------------------------------------------------------
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
            )

            
            val isBiometricAvailable = remember(context) { com.example.ui.utils.BiometricHelper.isBiometricAvailable(context) }

            ListItem(
                headlineContent = { Text("Biometric App Lock") },
                supportingContent = { Text(if (isBiometricAvailable) "Require fingerprint or face unlock to open the app." else "Biometrics not available or not set up on this device.") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Lock Icon",
                        tint = if (isBiometricAvailable) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                },
                trailingContent = {
                    Switch(
                        checked = biometricLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !isBiometricAvailable) {
                                showToast(context, "Biometric authentication is not set up on this device.", Toast.LENGTH_LONG)
                            } else {
                                onBiometricToggle(checked)
                            }
                        },
                        enabled = isBiometricAvailable
                    )
                },
                modifier = Modifier
                    .clickable(enabled = isBiometricAvailable) {
                        onBiometricToggle(!biometricLockEnabled)
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
                        onExportCsv(fileName)
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
                                onBackupCloud()
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
                        onBackupLocal()
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
            HorizontalDivider()

            // Admin Access Section
            if (userSession?.role == "ADMIN") {
                Text(
                    text = "Administrative Access",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = AppDimens.paddingLarge, vertical = AppDimens.paddingSmall)
                )

                ListItem(
                    headlineContent = { Text("Admin Console") },
                    supportingContent = { Text("Configure app focus modes, publish updates feed, and view diagnostics.") },
                    leadingContent = {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = "Admin Console", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Navigate to Admin")
                    },
                    modifier = Modifier
                        .clickable { onAdminConsoleClick() }
                        .padding(vertical = AppDimens.paddingExtraSmall)
                )
                HorizontalDivider()
            }
            
            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))
            
            // Logout Button
            FinanceButton(
                text = "Log Out",
                onClick = { onSignOut() },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.Logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.paddingLarge),
                shape = AppShapes.roundedCardMedium
            )
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FinanceButton(
                    text = "Save Changes",
                    onClick = {
                        if (editName.isBlank()) {
                            showToast(context, "Name cannot be blank", Toast.LENGTH_SHORT)
                            return@FinanceButton
                        }
                        onUpdateProfile(
                            editName,
                            editEmail,
                            {
                                showToast(context, "Profile updated successfully!", Toast.LENGTH_SHORT)
                                showEditProfileDialog = false
                            },
                            { error ->
                                showToast(context, "Failed to update: $error", Toast.LENGTH_LONG)
                                showEditProfileDialog = false
                            }
                        )
                    }
                )
            },
            dismissButton = {
                FinanceTextButton(
                    text = "Cancel",
                    onClick = { showEditProfileDialog = false }
                )
            }
        )
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
