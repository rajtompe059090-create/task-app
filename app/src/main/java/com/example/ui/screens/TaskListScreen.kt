package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CampaignTaskDetail
import com.example.data.repository.ReviewTaskRepository
import com.example.ui.components.BannerAdView
import com.example.ui.components.PolicyNoticeCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    repository: ReviewTaskRepository,
    onSelectTask: (campaignId: String) -> Unit
) {
    val campaignTasks by repository.activeCampaignTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(campaignTasks) {
        listOf("All") + campaignTasks.map { it.business.category }.distinct()
    }

    val filteredTasks = remember(campaignTasks, searchQuery, selectedCategory) {
        campaignTasks.filter { detail ->
            val matchesCategory = selectedCategory == "All" || detail.business.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    detail.business.businessName.contains(searchQuery, ignoreCase = true) ||
                    detail.campaign.title.contains(searchQuery, ignoreCase = true) ||
                    detail.business.address.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Tasks", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("task_list_screen"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by business, area, or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("task_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("filter_chip_${cat.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            // Policy Notice
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    PolicyNoticeCard()
                }
            }

            // Header info
            item {
                Text(
                    text = "${filteredTasks.size} Tasks Found",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching tasks found", style = MaterialTheme.typography.titleMedium)
                        Text("Try selecting another category or clearing your search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredTasks) { taskDetail ->
                    FullTaskCard(
                        taskDetail = taskDetail,
                        onStart = { onSelectTask(taskDetail.campaign.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FullTaskCard(
    taskDetail: CampaignTaskDetail,
    onStart: () -> Unit
) {
    val rawStatus = taskDetail.userTask?.status?.uppercase() ?: "AVAILABLE"
    val taskStatus = when (rawStatus) {
        "PENDING", "SUBMITTED" -> "PENDING"
        "APPROVED", "COMPLETED" -> "APPROVED"
        "REJECTED" -> "REJECTED"
        "IN_PROGRESS" -> "IN_PROGRESS"
        else -> "AVAILABLE"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onStart() }
            .testTag("full_task_card_${taskDetail.campaign.id}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = VibrantBlueContainer
                ) {
                    Text(
                        text = taskDetail.business.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnVibrantBlueContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp
                    )
                }

                StatusBadge(taskStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task Title
            Text(
                text = taskDetail.campaign.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Business Name and Address
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${taskDetail.business.businessName} • ${taskDetail.business.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task Description
            Text(
                text = taskDetail.campaign.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reward Amount
                Column {
                    Text(
                        text = "Reward Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${"%.2f".format(taskDetail.campaign.rewardAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "~${taskDetail.campaign.estimatedMinutes} mins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(14.dp))

                    // Start/Complete/Status Button
                    val buttonLabel = when (taskStatus) {
                        "PENDING" -> "Pending Review"
                        "APPROVED" -> "Approved"
                        "REJECTED" -> "Rejected"
                        "IN_PROGRESS" -> "Submit Proof"
                        else -> "Start Task"
                    }

                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("start_task_btn_${taskDetail.campaign.id}")
                    ) {
                        Text(buttonLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
