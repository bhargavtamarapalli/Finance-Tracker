package com.example.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.ui.components.AdminSectionHeader
import com.example.ui.components.FinanceButton
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Announcements tab displaying publishing forms and feed listings.
 * Reorganises the original announcement publish mechanics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnnouncementsTab(
    announcements: List<Announcement>,
    onPublish: (title: String, content: String, category: String) -> Unit,
    onDelete: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var annTitle by remember { mutableStateOf("") }
    var annContent by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("System Update") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    
    val categories = listOf("System Update", "Chitti Info", "Business Tip", "Privacy", "General")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_announcements_tab"),
        contentPadding = PaddingValues(AppDimens.paddingLarge),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Section: Publish Form
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminSectionHeader(title = "Publish System Updates & Feed", icon = Icons.Default.Campaign)
                Text(
                    text = "Compose updates, news, tips, or group schedules to publish on user dashboards instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = AppShapes.roundedCardMedium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(AppDimens.paddingLarge),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = annTitle,
                            onValueChange = { annTitle = it },
                            label = { Text("Update Title") },
                            placeholder = { Text("e.g. Schedule for Monthly Chitti Auction") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("announcement_title_input")
                        )

                        // Category Dropdown Selection
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Feed Category") },
                                trailingIcon = {
                                    IconButton(onClick = { showCategoryMenu = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCategoryMenu = true }
                            )
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedCategory = cat
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = annContent,
                            onValueChange = { annContent = it },
                            label = { Text("Update Content") },
                            placeholder = { Text("Provide details about upcoming schedules, financial news, or general announcements...") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("announcement_content_input")
                        )

                        FinanceButton(
                            text = "Publish to User Feed",
                            onClick = {
                                if (annTitle.isBlank() || annContent.isBlank()) {
                                    Toast.makeText(context, "Title and Content cannot be blank", Toast.LENGTH_SHORT).show()
                                    return@FinanceButton
                                }
                                onPublish(annTitle, annContent, selectedCategory)
                                annTitle = ""
                                annContent = ""
                                Toast.makeText(context, "Announcement published successfully!", Toast.LENGTH_SHORT).show()
                            },
                            icon = Icons.Default.Campaign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_announcement_button")
                        )
                    }
                }
            }
        }

        // Section: Active Announcements list
        item {
            AdminSectionHeader(
                title = "Live Active Feed List (${announcements.size})",
                icon = Icons.Default.Campaign
            )
        }

        if (announcements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No updates currently published.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(announcements, key = { it.id }) { ann ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = AppShapes.roundedCardMedium,
                    modifier = Modifier.fillMaxWidth().testTag("announcement_card_${ann.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(AppDimens.paddingLarge),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(ann.category) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            IconButton(
                                onClick = {
                                    onDelete(ann.id)
                                    Toast.makeText(context, "Announcement deleted", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("delete_announcement_${ann.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete update",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            text = ann.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ann.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Published " + SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ann.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
