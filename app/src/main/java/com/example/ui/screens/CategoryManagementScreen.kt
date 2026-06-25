package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                    text = { Text("Expense Categories", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Income Categories", fontWeight = FontWeight.Bold) }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No categories yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Click the + button to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(AppDimens.paddingNormal),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall)
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        CategoryRowItem(
                            category = category,
                            onEditClick = { showEditDialog = category },
                            onArchiveToggle = {
                                viewModel.updateCategory(category.copy(isArchived = !category.isArchived))
                            }
                        )
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
                viewModel.addCategory(name, type, iconName)
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
                viewModel.updateCategory(category.copy(name = name))
                showEditDialog = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryRowItem(
    category: Category,
    onEditClick: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    val alpha = if (category.isArchived) 0.5f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = AppShapes.roundedCardMedium,
        colors = CardDefaults.cardColors(
            containerColor = if (category.isArchived) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.paddingNormal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (category.isArchived) {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconByName(category.iconName),
                    contentDescription = category.name,
                    tint = if (category.isArchived) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }

            Spacer(modifier = Modifier.width(AppDimens.paddingNormal))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (category.isArchived) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    if (category.isArchived) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Archived") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    } else if (category.isDefault) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("System") }
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
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename Category",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onArchiveToggle) {
                Icon(
                    imageVector = if (category.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = if (category.isArchived) "Unarchive Category" else "Archive Category",
                    tint = if (category.isArchived) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                )
            }
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
                verticalArrangement = Arrangement.spacedBy(AppDimens.paddingSmall),
                modifier = Modifier.fillMaxWidth()
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

                Text(
                    "Choose an Icon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = AppDimens.paddingSmall)
                )

                // Render grid of icons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(AVAILABLE_ICONS) { (iconKey, label) ->
                            val isSelected = selectedIconName == iconKey
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(AppShapes.roundedCardMedium)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .clickable { selectedIconName = iconKey }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = getIconByName(iconKey),
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
            Button(
                onClick = { if (name.isNotBlank() && !isDuplicate) onSave(name, selectedIconName) },
                enabled = name.isNotBlank() && !isDuplicate
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
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
            Button(
                onClick = { if (name.isNotBlank() && !isDuplicate) onSave(name) },
                enabled = name.isNotBlank() && !isDuplicate
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
