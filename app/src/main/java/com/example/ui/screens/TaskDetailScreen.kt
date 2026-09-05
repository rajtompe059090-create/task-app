package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.repository.ReviewTaskRepository
import com.example.ui.components.PolicyNoticeCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    campaignId: String,
    repository: ReviewTaskRepository,
    onNavigateBack: () -> Unit,
    onProceedToFeedback: (taskId: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val campaignTasks by repository.activeCampaignTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val taskDetail = campaignTasks.find { it.campaign.id == campaignId }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Instructions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (taskDetail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val rawStatus = taskDetail.userTask?.status?.uppercase() ?: "AVAILABLE"
        val taskStatus = when (rawStatus) {
            "PENDING", "SUBMITTED" -> "PENDING"
            "APPROVED", "COMPLETED" -> "APPROVED"
            "REJECTED" -> "REJECTED"
            "IN_PROGRESS" -> "IN_PROGRESS"
            else -> "AVAILABLE"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("task_detail_screen")
        ) {
            // Status Alert Banner
            when (taskStatus) {
                "PENDING" -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VibrantAmberContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = OnVibrantAmber)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Task Status: Pending Review", fontWeight = FontWeight.Bold, color = OnVibrantAmber, style = MaterialTheme.typography.titleSmall)
                                Text("Your proof has been submitted and is awaiting admin approval. Reward will be added to your wallet once verified.", color = OnVibrantAmber, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                "APPROVED" -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VibrantGreenContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnVibrantGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Task Status: Approved & Rewarded", fontWeight = FontWeight.Bold, color = OnVibrantGreen, style = MaterialTheme.typography.titleSmall)
                                Text("This submission was verified and approved by admin. ₹${"%.2f".format(taskDetail.campaign.rewardAmount)} was added to your wallet!", color = OnVibrantGreen, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                "REJECTED" -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = VibrantErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = VibrantError)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Task Status: Rejected", fontWeight = FontWeight.Bold, color = VibrantError, style = MaterialTheme.typography.titleSmall)
                                Text(taskDetail.userTask?.adminNote ?: "Proof did not meet verification criteria.", color = VibrantError, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Business & Task Header Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
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

                    Text(
                        text = taskDetail.campaign.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = taskDetail.business.businessName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = taskDetail.business.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Task Reward", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${"%.2f".format(taskDetail.campaign.rewardAmount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Est. Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${taskDetail.campaign.estimatedMinutes} Minutes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Policy Notice
            PolicyNoticeCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Task Description & Instructions
            Text(
                text = "Task Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = taskDetail.campaign.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Genuine Feedback Guidelines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            InstructionBullet(number = 1, text = "Answer honestly based on your real experience with this business.")
            InstructionBullet(number = 2, text = "Rate service quality, cleanliness, and overall satisfaction accurately.")
            InstructionBullet(number = 3, text = "Highlight what you loved, and constructively point out what can be improved.")
            InstructionBullet(number = 4, text = "Do not post fake or biased reviews. ReviewTask prohibits paid manipulation.")

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (taskDetail.userTask != null) {
                        onProceedToFeedback(taskDetail.userTask.id)
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = repository.startTask(campaignId)
                        isLoading = false
                        result.onSuccess { startedTask ->
                            onProceedToFeedback(startedTask.id)
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Could not start task."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("task_detail_start_btn"),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    val label = when (taskStatus) {
                        "PENDING" -> "View Submitted Proof (Pending Review)"
                        "APPROVED" -> "View Approved Task Proof"
                        "REJECTED" -> "Review & Resubmit Proof"
                        "IN_PROGRESS" -> "Complete & Submit Proof"
                        else -> "Start Task & Submit Proof"
                    }
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionBullet(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(VibrantBlueContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = OnVibrantBlueContainer
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
