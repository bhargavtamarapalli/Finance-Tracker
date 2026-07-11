package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.utils.CurrencyUtils
import kotlinx.coroutines.launch

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
                    showToast(context, "Transactions exported to CSV successfully!")
                } catch (e: Exception) {
                    showToast(context, "Export failed: ${e.localizedMessage}")
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
                showToast(context, "Backup failed: No internet connection")
            } else {
                scope.launch {
                    isSyncing = true
                    viewModel.backupToFirebase(userSession?.userId ?: "")
                    isSyncing = false
                }
            }
        },
        onBackupLocal = {
            scope.launch {
                isSyncing = true
                viewModel.backupLocally()
                isSyncing = false
            }
        },
        onRestoreCloud = {
            if (!isOnline) {
                showToast(context, "Restore failed: No internet connection")
            } else {
                scope.launch {
                    isSyncing = true
                    viewModel.restoreFromFirebase(userSession?.userId ?: "")
                    isSyncing = false
                }
            }
        },
        onRestoreLocal = {
            scope.launch {
                isSyncing = true
                viewModel.restoreLocally()
                isSyncing = false
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

enum class RestoreType { LOCAL, CLOUD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    userSession: UserSession?,
    reminderEnabled: Boolean,
    biometricLockEnabled: Boolean,
    appTheme: AppTheme,
    currencyOption: CurrencyOption,
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
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userSession?.name ?: "") }
    var editEmail by remember { mutableStateOf(userSession?.email ?: "") }
    var showRestoreWarning by remember { mutableStateOf(false) }
    var restoreActionType by remember { mutableStateOf<RestoreType?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = AppDimens.paddingNormal)
        ) {
            // Profile Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.paddingIconInside),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    name = userSession?.name,
                    isGuest = userSession?.isGuest == true,
                    size = AppDimens.sizeAvatar
                )
                Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userSession?.name ?: "Guest User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (userSession?.isGuest == true) "Guest Session" else (userSession?.email ?: "guest@example.com"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                if (userSession?.isGuest == false) {
                    FinanceButton(
                        text = "Edit",
                        onClick = { showEditProfileDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = AppShapes.roundedIconContainer,
                        height = 36.dp
                    )
                }
            }

            // Upgrade Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.paddingExtraSmall),
                shape = AppShapes.roundedCardMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), Color.Transparent)
                            )
                        )
                        .padding(AppDimens.paddingNormal)
                ) {
                    Text(
                        "Upgrade to Business",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AppDimens.paddingExtraSmall))
                    Text(
                        "Unlock advanced analytics and business features. Scale your financial management.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(AppDimens.paddingMedium))
                    FinanceButton(
                        text = "Learn More",
                        onClick = { /* Learn more */ },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = AppShapes.roundedIconContainer
                    )
                }
            }
 
            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

            // Account Section
            SettingsSectionHeader("Account")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.roundedCardMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        icon = Icons.Default.CurrencyExchange,
                        title = "Currency",
                        trailingText = currencyOption.name,
                        onClick = { /* Change currency */ }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                    )
                    SettingsItem(
                        icon = Icons.Default.List,
                        title = "Manage Categories",
                        onClick = onManageCategoriesClick
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                    )
                    var isEditingBudget by remember { mutableStateOf(false) }
                    var tempBudgetInput by remember(monthlyBudgetGoal) { mutableStateOf(monthlyBudgetGoal.toInt().toString()) }
                    SettingsItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Monthly Budget Goal",
                        trailingContent = {
                            if (isEditingBudget) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                                ) {
                                    OutlinedTextField(
                                        value = tempBudgetInput,
                                        onValueChange = { tempBudgetInput = it },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.width(100.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = {
                                            val goal = tempBudgetInput.toDoubleOrNull() ?: 0.0
                                            onBudgetGoalChange(goal)
                                            isEditingBudget = false
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Save Goal",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { isEditingBudget = true }
                                ) {
                                    Text(
                                        text = CurrencyUtils.formatRupees(monthlyBudgetGoal),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Goal",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

            // Preferences Section
            SettingsSectionHeader("Preferences")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.roundedCardMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        icon = Icons.Default.Lightbulb,
                        title = "Theme",
                        trailingContent = {
                            Switch(
                                checked = appTheme == AppTheme.DARK,
                                onCheckedChange = { onThemeChange(if (it) AppTheme.DARK else AppTheme.LIGHT) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        trailingContent = {
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = onReminderToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                    )
                    SettingsItem(
                        icon = Icons.Default.Public,
                        title = "Language",
                        trailingText = "English",
                        onClick = { /* Change language */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

            // Security Section
            SettingsSectionHeader("Security")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.roundedCardMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric App Lock",
                        trailingContent = {
                            Switch(
                                checked = biometricLockEnabled,
                                onCheckedChange = onBiometricToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                    )
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Change Password",
                        onClick = { /* Change password */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

            // Data & Backup Section
            SettingsSectionHeader("Data & Backup")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.roundedCardMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        icon = Icons.Default.Save,
                        title = "Backup to Local Storage",
                        onClick = onBackupLocal
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                    )
                    SettingsItem(
                        icon = Icons.Default.Restore,
                        title = "Restore from Local Storage",
                        onClick = {
                            restoreActionType = RestoreType.LOCAL
                            showRestoreWarning = true
                        }
                    )
                    
                    if (userSession?.isGuest == false) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                        )
                        SettingsItem(
                            icon = Icons.Default.CloudUpload,
                            title = "Backup to Cloud",
                            onClick = onBackupCloud
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                        )
                        SettingsItem(
                            icon = Icons.Default.CloudDownload,
                            title = "Restore from Cloud",
                            onClick = {
                                restoreActionType = RestoreType.CLOUD
                                showRestoreWarning = true
                            }
                        )
                    }
                }
            }

            // Admin Access Section
            if (userSession?.role == "ADMIN") {
                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                SettingsSectionHeader("Administrative Access")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.roundedCardMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    SettingsItem(
                        icon = Icons.Default.SupervisorAccount,
                        title = "Admin Console",
                        onClick = onAdminConsoleClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

            // App Info Section
            SettingsSectionHeader("App Info")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.roundedCardMedium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    trailingText = "v1.1"
                )
            }
 
            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))
            
            FinanceButton(
                text = "Log Out",
                onClick = { onSignOut() },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.Logout,
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.roundedCardMedium
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showRestoreWarning) {
        AlertDialog(
            onDismissRequest = { showRestoreWarning = false },
            title = { Text("Confirm Data Restore", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to restore data? This will overwrite or merge categories and transactions. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreWarning = false
                        if (restoreActionType == RestoreType.CLOUD) {
                            onRestoreCloud()
                        } else {
                            onRestoreLocal()
                        }
                    }
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateProfile(editName, editEmail, {
                        showEditProfileDialog = false
                    }, {
                        showToast(context, it)
                    })
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = AppDimens.paddingExtraSmall)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingIconInside),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        } else if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    }
}

private fun generateCsvContent(transactions: List<com.example.data.model.TransactionWithCategory>): String {
    val sb = StringBuilder()
    sb.append("ID,Date,Amount,Source,Category,Type,Notes\n")
    transactions.forEach {
        sb.append("${it.transaction.id},${it.transaction.date},${it.transaction.amount},${it.transaction.source},${it.category?.name},${it.transaction.type},${it.transaction.notes}\n")
    }
    return sb.toString()
}

private fun showToast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
