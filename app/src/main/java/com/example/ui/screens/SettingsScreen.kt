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
import androidx.compose.ui.platform.testTag
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
    val activity = context as? androidx.fragment.app.FragmentActivity
    var isSyncing by remember { mutableStateOf(false) }
    val isDevMode = remember {
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 ||
        try {
            Class.forName("org.junit.Test")
            true
        } catch (e: ClassNotFoundException) {
            false
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
        onBiometricToggle = { enabled ->
            if (enabled) {
                if (activity != null) {
                    val cipher = com.example.ui.utils.BiometricHelper.getInitializedCipher(javax.crypto.Cipher.ENCRYPT_MODE)
                    val cryptoObject = if (cipher != null) androidx.biometric.BiometricPrompt.CryptoObject(cipher) else null
                    com.example.ui.utils.BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        title = "Enable Secure Lock",
                        subtitle = "Verify identity to enable biometric authentication",
                        cryptoObject = cryptoObject,
                        onSuccess = { result ->
                            val unlockedCipher = result.cryptoObject?.cipher
                            if (unlockedCipher != null && com.example.ui.utils.BiometricHelper.encryptAndSaveChallenge(context, unlockedCipher)) {
                                viewModel.setBiometricLockEnabled(true)
                            } else {
                                showToast(context, "Failed to initialize biometric cryptography keys.")
                            }
                        },
                        onError = { error ->
                            if (error != "Cancelled") {
                                showToast(context, "Biometric setup failed: $error")
                            }
                        }
                    )
                } else {
                    showToast(context, "Biometric setup requires an active activity.")
                }
            } else {
                if (activity != null) {
                    val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
                    val ivBase64 = prefs.getString("biometric_challenge_iv", null)
                    val iv = if (ivBase64 != null) android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP) else null
                    val cipher = com.example.ui.utils.BiometricHelper.getInitializedCipher(javax.crypto.Cipher.DECRYPT_MODE, iv)
                    val cryptoObject = if (cipher != null) androidx.biometric.BiometricPrompt.CryptoObject(cipher) else null
                    com.example.ui.utils.BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        title = "Disable Secure Lock",
                        subtitle = "Confirm identity to disable biometric app lock",
                        cryptoObject = cryptoObject,
                        onSuccess = { result ->
                            val unlockedCipher = result.cryptoObject?.cipher
                            if (unlockedCipher != null && com.example.ui.utils.BiometricHelper.decryptAndVerifyChallenge(context, unlockedCipher)) {
                                viewModel.setBiometricLockEnabled(false)
                            } else {
                                showToast(context, "Verification failed.")
                            }
                        },
                        onError = { error ->
                            if (error != "Cancelled") {
                                showToast(context, "Verification failed: $error")
                            }
                        }
                    )
                } else {
                    viewModel.setBiometricLockEnabled(false)
                }
            }
        },
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
            authViewModel.logout()
        },
        onSeedDemoTransactions = { viewModel.seedDemoTransactions() },
        onClearAllData = { viewModel.clearAllData() },
        isDevMode = isDevMode
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
    onSignOut: () -> Unit,
    onSeedDemoTransactions: () -> Unit,
    onClearAllData: () -> Unit,
    isDevMode: Boolean
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(userSession?.name ?: "") }
    var editEmail by remember { mutableStateOf(userSession?.email ?: "") }
    var showRestoreWarning by remember { mutableStateOf(false) }
    var restoreActionType by remember { mutableStateOf<RestoreType?>(null) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showClearAllWarning by remember { mutableStateOf(false) }

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
                    HorizontalDivider()

                    ListItem(
                        headlineContent = { Text("Email") },
                        supportingContent = { Text("Guest Account", modifier = Modifier.testTag("guest_account_text")) },
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
                        supportingContent = { Text(session.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("profile_name_text")) },
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
                        supportingContent = { Text(session.email, modifier = Modifier.testTag("profile_email_text")) },
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
                        onClick = { showCurrencyDialog = true }
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
                                modifier = Modifier.testTag("settings_reminder_switch"),
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

            // Developer Settings Section
            if (isDevMode) {
                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))
                SettingsSectionHeader("Developer Settings")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.roundedCardMedium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AppDimens.borderWidthThin, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsItem(
                            icon = Icons.Default.PlayArrow,
                            title = "Seed Demo Transactions",
                            onClick = onSeedDemoTransactions
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                        )
                        SettingsItem(
                            icon = Icons.Default.Delete,
                            title = "Clear All Data",
                            onClick = { showClearAllWarning = true }
                        )
                    }
                }
            }
 
            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))
            
            val isGuest = userSession?.isGuest == true
            FinanceButton(
                text = if (isGuest) "Sign In / Register" else "Log Out",
                onClick = { onSignOut() },
                containerColor = if (isGuest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (isGuest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                icon = if (isGuest) Icons.Default.Login else Icons.Default.Logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.paddingLarge),
                shape = AppShapes.roundedCardMedium,
                testTag = "settings_logout_button"
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

    if (showClearAllWarning) {
        AlertDialog(
            onDismissRequest = { showClearAllWarning = false },
            title = { Text("Confirm Clear All Data", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently clear all categories and transaction data? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllWarning = false
                        onClearAllData()
                    }
                ) {
                    Text("Clear Data", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)) {
                    CurrencyOption.values().forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCurrencyChange(option)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = AppDimens.paddingSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currencyOption == option,
                                onClick = {
                                    onCurrencyChange(option)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
                            Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
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
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
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

private fun escapeCsvField(value: String?): String {
    if (value == null) return ""
    var sanitized = value
    // Formula Injection prevention: prepend single quote to trigger characters
    if (sanitized.startsWith("=") || sanitized.startsWith("+") || sanitized.startsWith("-") || sanitized.startsWith("@")) {
        sanitized = "'$sanitized"
    }
    // Layout corruption prevention: escape quotes and wrap in double quotes if commas or newlines present
    if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")) {
        sanitized = "\"" + sanitized.replace("\"", "\"\"") + "\""
    }
    return sanitized
}

private fun generateCsvContent(transactions: List<com.example.data.model.TransactionWithCategory>): String {
    val sb = StringBuilder()
    sb.append("ID,Date,Amount,Source,Category,Type,Notes\n")
    transactions.forEach {
        val id = it.transaction.id
        val date = it.transaction.date
        val amount = it.transaction.amount
        val source = escapeCsvField(it.transaction.source)
        val category = escapeCsvField(it.category?.name)
        val type = it.transaction.type
        val notes = escapeCsvField(it.transaction.notes)
        sb.append("$id,$date,$amount,$source,$category,$type,$notes\n")
    }
    return sb.toString()
}

private fun showToast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
