package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.utils.getIconByName
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    initialType: String,
    transactionId: Int? = null,
    isDuplicate: Boolean = false
) {
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    // Find editing/duplicating transaction if any
    val editTransaction = remember(transactionId, transactions) {
        transactions.find { it.transaction.id == transactionId }?.transaction
    }

    var type by remember(editTransaction) {
        mutableStateOf(editTransaction?.type ?: if (initialType == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE)
    }
    var amount by remember(editTransaction) {
        mutableStateOf(editTransaction?.amount?.toString() ?: "")
    }
    var source by remember(editTransaction) {
        mutableStateOf(editTransaction?.source ?: "")
    }
    var notes by remember(editTransaction) {
        mutableStateOf(editTransaction?.notes ?: "")
    }
    var selectedCategoryId by remember(editTransaction) {
        mutableStateOf(editTransaction?.categoryId)
    }
    var paymentMethod by remember(editTransaction) {
        mutableStateOf(editTransaction?.paymentMethod ?: "Cash")
    }
    var date by remember(editTransaction) {
        mutableStateOf(editTransaction?.date ?: System.currentTimeMillis())
    }
    
    var amountError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    val filteredCategories = allCategories.filter { it.type == type && (!it.isArchived || it.id == selectedCategoryId) }
    if (selectedCategoryId == null && filteredCategories.isNotEmpty()) {
        selectedCategoryId = filteredCategories.first().id
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Focus manager for keyboard actions
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (editTransaction != null) {
                        if (isDuplicate) "Duplicate Transaction" else "Edit Transaction"
                    } else {
                        if (type == TransactionType.EXPENSE) "Add Expense" else "Add Income"
                    }
                    Text(titleText, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    FinanceIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = { navController.popBackStack() },
                        contentDescription = "Back"
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.paddingNormal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
        ) {
            // Type toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FinanceSegmentedButton(
                    text = "Expense",
                    selected = type == TransactionType.EXPENSE,
                    onClick = { type = TransactionType.EXPENSE; selectedCategoryId = null },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
                FinanceSegmentedButton(
                    text = "Income",
                    selected = type == TransactionType.INCOME,
                    onClick = { type = TransactionType.INCOME; selectedCategoryId = null },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    amount = it
                    if (amountError != null) amountError = null
                },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )

            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text(if (type == TransactionType.EXPENSE) "Payee / Store" else "Source / Employer") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Date Picker Row
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date", style = MaterialTheme.typography.titleMedium)
                    FinanceOutlinedButton(
                        text = dateFormat.format(Date(date)),
                        onClick = { showDatePicker = true },
                        shape = AppShapes.roundedCardMedium
                    )
                }
                dateError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Category Selection Row
            Column {
                Text("Category", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = AppDimens.paddingSmall))
                if (filteredCategories.isEmpty()) {
                    Text(
                        "No active categories. Create one in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredCategories) { category ->
                            CategoryItem(
                                category = category,
                                isSelected = category.id == selectedCategoryId,
                                onClick = { 
                                    selectedCategoryId = category.id
                                    categoryError = null
                                }
                            )
                        }
                    }
                }
                categoryError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Payment Method Selection Row
            Column {
                Text("Payment Method", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = AppDimens.paddingSmall))
                val paymentMethods = listOf("Cash", "Card", "Bank Transfer", "UPI", "Other")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(paymentMethods) { method ->
                        val isSelected = method == paymentMethod
                        Card(
                            modifier = Modifier
                                .clickable { paymentMethod = method },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = AppShapes.roundedCardMedium
                        ) {
                            Text(
                                text = method,
                                modifier = Modifier.padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(AppDimens.paddingNormal))

            FinanceButton(
                text = if (editTransaction != null && !isDuplicate) "Update" else "Save",
                onClick = {
                    var isValid = true

                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null) {
                        amountError = "Please enter a valid numeric amount"
                        isValid = false
                    } else if (amountValue <= 0) {
                        amountError = "Amount must be greater than zero"
                        isValid = false
                    } else {
                        amountError = null
                    }

                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = date
                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 23)
                    today.set(Calendar.MINUTE, 59)
                    today.set(Calendar.SECOND, 59)
                    
                    if (calendar.after(today)) {
                        dateError = "Date cannot be in the future"
                        isValid = false
                    } else {
                        dateError = null
                    }

                    if (selectedCategoryId == null) {
                        categoryError = "Please select a category"
                        isValid = false
                    } else {
                        categoryError = null
                    }

                    if (isValid && amountValue != null && selectedCategoryId != null) {
                        if (editTransaction != null && !isDuplicate) {
                            viewModel.updateTransaction(
                                editTransaction.copy(
                                    amount = amountValue,
                                    source = source,
                                    date = date,
                                    categoryId = selectedCategoryId!!,
                                    type = type,
                                    notes = notes,
                                    paymentMethod = paymentMethod
                                )
                            )
                        } else {
                            viewModel.addTransaction(
                                amount = amountValue,
                                source = source,
                                date = date,
                                categoryId = selectedCategoryId!!,
                                type = type,
                                notes = notes,
                                paymentMethod = paymentMethod
                            )
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppDimens.paddingLarge),
                height = AppDimens.heightButton
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                FinanceTextButton(
                    text = "OK",
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            date = it
                            dateError = null
                        }
                        showDatePicker = false
                    }
                )
            },
            dismissButton = {
                FinanceTextButton(
                    text = "Cancel",
                    onClick = { showDatePicker = false }
                )
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun CategoryItem(category: com.example.data.model.Category, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = AppShapes.roundedIconContainer
            )
            .clickable { onClick() }
            .padding(AppDimens.paddingSmall)
    ) {
        Icon(
            imageVector = getIconByName(category.iconName),
            contentDescription = category.name,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
