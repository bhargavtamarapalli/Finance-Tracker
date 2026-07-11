package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.model.Category
import com.example.data.model.TransactionType
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes
import com.example.ui.utils.getIconByName
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.components.*

val AVAILABLE_ICONS = listOf(
    "restaurant" to "Food",
    "shopping_cart" to "Cart",
    "directions_bus" to "Bus",
    "local_gas_station" to "Gas",
    "home" to "Home",
    "bolt" to "Utilities",
    "attach_money" to "Money",
    "work" to "Work",
    "store" to "Store",
    "percent" to "Percent",
    "trending_up" to "Trend",
    "keyboard_return" to "Return",
    "card_giftcard" to "Gift",
    "shopping_bag" to "Shopping Bag",
    "movie" to "Entertainment",
    "flight" to "Travel",
    "medical_services" to "Healthcare",
    "shield" to "Insurance",
    "school" to "Education",
    "credit_card" to "Card",
    "show_chart" to "Investment",
    "more_horiz" to "Misc"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: FinanceViewModel,
    navController: NavController
) {
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    CategoryManagementContent(
        allCategories = allCategories,
        isLoading = isLoading,
        onBackClick = { navController.popBackStack() },
        addCategory = { name, type, iconName -> viewModel.addCategory(name, type, iconName) },
        updateCategory = { category -> viewModel.updateCategory(category) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementContent(
    allCategories: List<Category>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    addCategory: (String, TransactionType, String) -> Unit,
    updateCategory: (Category) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Expense, 1: Income
    val type = if (selectedTab == 0) TransactionType.EXPENSE else TransactionType.INCOME

    val filteredCategories = allCategories.filter { it.type == type }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FinanceIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBackClick,
                        contentDescription = "Back"
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Expenses", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Income", fontWeight = FontWeight.Bold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredCategories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No categories found for this type.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Track which category has its context menu open
                var expandedMenuCategoryId by remember { mutableStateOf<Int?>(null) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AppDimens.paddingNormal)
                ) {
                    Box {
                        CategoryChipGrid(
                            categories = filteredCategories,
                            selectedIds = emptySet(),
                            onToggle = { /* no selection in management screen */ },
                            onLongPress = { categoryId -> expandedMenuCategoryId = categoryId },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Render a DropdownMenu anchored near the long-pressed chip
                        val targetCategory = filteredCategories.firstOrNull { it.id == expandedMenuCategoryId }
                        if (targetCategory != null) {
                            DropdownMenu(
                                expanded = expandedMenuCategoryId != null,
                                onDismissRequest = { expandedMenuCategoryId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        showEditDialog = targetCategory
                                        expandedMenuCategoryId = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (targetCategory.isArchived) "Unarchive" else "Archive"
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (targetCategory.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        updateCategory(targetCategory.copy(isArchived = !targetCategory.isArchived))
                                        expandedMenuCategoryId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            type = type,
            existingCategories = filteredCategories,
            onDismiss = { showAddDialog = false },
            onSave = { name, iconName ->
                addCategory(name, type, iconName)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { category ->
        EditCategoryDialog(
            category = category,
            existingCategories = filteredCategories.filter { it.id != category.id },
            onDismiss = { showEditDialog = null },
            onSave = { name ->
                updateCategory(category.copy(name = name))
                showEditDialog = null
            }
        )
    }
}

@Composable
fun CategoryRowItem(
    category: Category,
    onEditClick: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (category.isArchived) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (category.isArchived) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (category.isArchived) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        },
                        shape = AppShapes.roundedIconContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category.iconName),
                    contentDescription = category.name,
                    tint = if (category.isArchived) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (category.isArchived) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (category.isArchived) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Archived") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }

                    if (category.isDefault) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("System Default") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Custom") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            // Actions
            FinanceIconButton(
                icon = Icons.Default.Edit,
                onClick = onEditClick,
                contentDescription = "Rename Category",
                tint = MaterialTheme.colorScheme.primary
            )

            FinanceIconButton(
                icon = if (category.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                onClick = onArchiveToggle,
                contentDescription = if (category.isArchived) "Unarchive Category" else "Archive Category",
                tint = if (category.isArchived) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun AddCategoryDialog(
    type: TransactionType,
    existingCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf(AVAILABLE_ICONS.first().first) }
    val focusManager = LocalFocusManager.current

    val isDuplicate = remember(name) {
        existingCategories.any { it.name.trim().equals(name.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    isError = isDuplicate,
                    supportingText = {
                        if (isDuplicate) {
                            Text("Category already exists", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Box(modifier = Modifier.height(180.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                    ) {
                        items(AVAILABLE_ICONS) { (iconName, label) ->
                            val isSelected = iconName == selectedIconName
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIconName = iconName },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                shape = AppShapes.roundedCardMedium
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = getIconByName(iconName),
                                        contentDescription = label,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FinanceButton(
                text = "Create",
                onClick = { if (name.isNotBlank() && !isDuplicate) onSave(name, selectedIconName) },
                enabled = name.isNotBlank() && !isDuplicate
            )
        },
        dismissButton = {
            FinanceTextButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun EditCategoryDialog(
    category: Category,
    existingCategories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    val focusManager = LocalFocusManager.current

    val isDuplicate = remember(name) {
        existingCategories.any { it.name.trim().equals(name.trim(), ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Category", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                singleLine = true,
                isError = isDuplicate,
                supportingText = {
                    if (isDuplicate) {
                        Text("Category already exists", color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            FinanceButton(
                text = "Save",
                onClick = { if (name.isNotBlank() && !isDuplicate) onSave(name) },
                enabled = name.isNotBlank() && !isDuplicate
            )
        },
        dismissButton = {
            FinanceTextButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}
