package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.repository.ReviewTaskRepository
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    repository: ReviewTaskRepository,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val pendingTasksCount by repository.adminPendingTasksCount.collectAsStateWithLifecycle(initialValue = 0)
    val pendingWdCount by repository.adminPendingWithdrawalsCount.collectAsStateWithLifecycle(initialValue = 0)

    val submissionsLabel = if (pendingTasksCount > 0) "Submissions ($pendingTasksCount)" else "Submissions"
    val withdrawalsLabel = if (pendingWdCount > 0) "Withdrawals ($pendingWdCount)" else "Withdrawals"

    val tabs = listOf(
        "Dashboard",
        submissionsLabel,
        "Tasks/Campaigns",
        withdrawalsLabel,
        "Users",
        "Businesses",
        "Feedbacks"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Control Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("admin_portal_screen")
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminDashboardTab(repository)
                1 -> AdminSubmissionsTab(repository)
                2 -> AdminCampaignsTab(repository)
                3 -> AdminWithdrawalsTab(repository)
                4 -> AdminUsersTab(repository)
                5 -> AdminBusinessesTab(repository)
                6 -> AdminFeedbacksTab(repository)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 1: ADMIN DASHBOARD (KPIS)
// -----------------------------------------------------------------------------
@Composable
private fun AdminDashboardTab(repository: ReviewTaskRepository) {
    val totalUsers by repository.adminTotalUsers.collectAsStateWithLifecycle(initialValue = 0)
    val activeUsers by repository.adminActiveUsers.collectAsStateWithLifecycle(initialValue = 0)
    val totalBiz by repository.adminTotalBusinesses.collectAsStateWithLifecycle(initialValue = 0)
    val activeCampaigns by repository.adminActiveCampaigns.collectAsStateWithLifecycle(initialValue = 0)
    val completedTasks by repository.adminCompletedTasks.collectAsStateWithLifecycle(initialValue = 0)
    val totalRewards by repository.adminTotalRewards.collectAsStateWithLifecycle(initialValue = 0.0)
    val pendingWithdrawalsCount by repository.adminPendingWithdrawalsCount.collectAsStateWithLifecycle(initialValue = 0)
    val pendingWithdrawalsSum by repository.adminPendingWithdrawalsSum.collectAsStateWithLifecycle(initialValue = 0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Key Platform Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Total Users",
                    value = totalUsers.toString(),
                    icon = Icons.Default.People,
                    subtitle = "$activeUsers Active accounts",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Businesses",
                    value = totalBiz.toString(),
                    icon = Icons.Default.Storefront,
                    subtitle = "Enrolled stores",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Active Campaigns",
                    value = activeCampaigns.toString(),
                    icon = Icons.Default.Campaign,
                    iconTint = VibrantBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Completed Surveys",
                    value = completedTasks.toString(),
                    icon = Icons.Default.FactCheck,
                    iconTint = VibrantGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatCard(
                title = "Total Rewards Distributed",
                value = "₹${"%.2f".format(totalRewards)}",
                icon = Icons.Default.Payments,
                iconTint = VibrantGreen,
                subtitle = "Credited directly via verified feedback"
            )
        }

        item {
            val pendingTasksCount by repository.adminPendingTasksCount.collectAsStateWithLifecycle(initialValue = 0)
            StatCard(
                title = "Pending Task Proofs",
                value = pendingTasksCount.toString(),
                icon = Icons.Default.AssignmentLate,
                iconTint = VibrantAmber,
                subtitle = "$pendingTasksCount submissions awaiting admin review & reward crediting"
            )
        }

        item {
            StatCard(
                title = "Pending Withdrawals",
                value = "₹${"%.2f".format(pendingWithdrawalsSum)}",
                icon = Icons.Default.PendingActions,
                iconTint = VibrantAmber,
                subtitle = "$pendingWithdrawalsCount requests waiting for verification"
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: TASK SUBMISSIONS AUDIT & APPROVAL
// -----------------------------------------------------------------------------
@Composable
private fun AdminSubmissionsTab(repository: ReviewTaskRepository) {
    val coroutineScope = rememberCoroutineScope()
    val allTasks by repository.allTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val allCampaigns by repository.allCampaigns.collectAsStateWithLifecycle(initialValue = emptyList())
    val allFeedbacks by repository.allFeedback.collectAsStateWithLifecycle(initialValue = emptyList())

    var filterStatus by remember { mutableStateOf("PENDING") }
    var selectedTaskForReject by remember { mutableStateOf<TaskItem?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var selectedTaskForApprove by remember { mutableStateOf<TaskItem?>(null) }
    var approvalNote by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val campaignMap = remember(allCampaigns) { allCampaigns.associateBy { it.id } }
    val feedbackMap = remember(allFeedbacks) { allFeedbacks.associateBy { it.taskId } }

    val filteredTasks = remember(allTasks, filterStatus) {
        val submitted = allTasks.filter { it.status != "IN_PROGRESS" }
        when (filterStatus) {
            "PENDING" -> submitted.filter { it.status in listOf("PENDING", "SUBMITTED") }
            "APPROVED" -> submitted.filter { it.status in listOf("APPROVED", "COMPLETED") }
            "REJECTED" -> submitted.filter { it.status == "REJECTED" }
            else -> submitted
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Task Submissions Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("PENDING" to "Pending Review", "APPROVED" to "Approved", "REJECTED" to "Rejected", "ALL" to "All").forEach { (status, label) ->
                    FilterChip(
                        selected = filterStatus == status,
                        onClick = { filterStatus = status },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (filteredTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No submissions in this category", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(filteredTasks) { task ->
                val campaign = campaignMap[task.campaignId]
                val feedback = feedbackMap[task.id]

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("admin_task_submission_${task.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = campaign?.title ?: "Task Submission #${task.id.take(8)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "User: ${task.userId.take(12)}... • ID: ${task.id.take(8)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(task.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reward & Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reward: ₹${"%.2f".format(campaign?.rewardAmount ?: 0.0)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = VibrantGreen
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(task.submittedAt ?: task.completedAt ?: task.startedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Proof Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Submitted Proof:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (!task.proof.isNullOrBlank()) task.proof else "No proof text provided.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Feedback summary if available
                        if (feedback != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Feedback: ${feedback.rating}/5★ | Liked: ${feedback.whatLiked.take(40)}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!task.adminNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Admin Note: ${task.adminNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.status == "REJECTED") VibrantError else EmeraldGreenDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons
                        if (task.status in listOf("PENDING", "SUBMITTED")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedTaskForApprove = task
                                        approvalNote = "Verified and approved by admin"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                                    modifier = Modifier.weight(1f).testTag("admin_approve_btn_${task.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve & Pay", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedTaskForReject = task
                                        rejectionReason = "Proof verification failed"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VibrantError),
                                    modifier = Modifier.weight(1f).testTag("admin_reject_btn_${task.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (task.status == "REJECTED") {
                            OutlinedButton(
                                onClick = {
                                    selectedTaskForApprove = task
                                    approvalNote = "Re-verified and approved upon admin review"
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Re-Approve & Credit Reward")
                            }
                        }
                    }
                }
            }
        }
    }

    // Approve Dialog
    if (selectedTaskForApprove != null) {
        val task = selectedTaskForApprove!!
        val campaign = campaignMap[task.campaignId]
        AlertDialog(
            onDismissRequest = { if (!isProcessing) selectedTaskForApprove = null },
            title = { Text("Approve Task & Credit Reward") },
            text = {
                Column {
                    Text("This will verify the submission and immediately credit ₹${"%.2f".format(campaign?.rewardAmount ?: 0.0)} to the user's wallet balance.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = approvalNote,
                        onValueChange = { approvalNote = it },
                        label = { Text("Approval Audit Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        coroutineScope.launch {
                            repository.approveTaskSubmission(task.id, approvalNote)
                            isProcessing = false
                            selectedTaskForApprove = null
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
                ) {
                    Text("Confirm Approval")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskForApprove = null }, enabled = !isProcessing) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject Dialog
    if (selectedTaskForReject != null) {
        val task = selectedTaskForReject!!
        AlertDialog(
            onDismissRequest = { if (!isProcessing) selectedTaskForReject = null },
            title = { Text("Reject Task Submission") },
            text = {
                Column {
                    Text("Specify the reason for rejection. The user will be notified and no funds will be credited.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        coroutineScope.launch {
                            repository.rejectTaskSubmission(task.id, rejectionReason)
                            isProcessing = false
                            selectedTaskForReject = null
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantError)
                ) {
                    Text("Reject Submission")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskForReject = null }, enabled = !isProcessing) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 3: WITHDRAWAL MANAGEMENT
// -----------------------------------------------------------------------------
@Composable
private fun AdminWithdrawalsTab(repository: ReviewTaskRepository) {
    val coroutineScope = rememberCoroutineScope()
    val withdrawals by repository.allWithdrawals.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedWithdrawal by remember { mutableStateOf<Withdrawal?>(null) }
    var newStatus by remember { mutableStateOf("APPROVED") }
    var adminNote by remember { mutableStateOf("") }
    var transactionRef by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val statusOptions = listOf("SUBMITTED", "UNDER_REVIEW", "APPROVED", "PROCESSING", "SUCCESSFUL", "REJECTED")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Withdrawal Payout Requests (${withdrawals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (withdrawals.isEmpty()) {
            item {
                Text("No withdrawal requests in database.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(withdrawals) { wd ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedWithdrawal = wd
                            newStatus = wd.status
                            adminNote = wd.adminNote ?: ""
                            transactionRef = wd.transactionReference ?: ""
                        }
                        .testTag("admin_wd_${wd.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("₹${"%.2f".format(wd.amount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = VibrantGreen)
                            StatusBadge(wd.status)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("UPI: ${wd.upiId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("User ID: ${wd.userId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!wd.adminNote.isNullOrBlank()) {
                            Text("Note: ${wd.adminNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (!wd.transactionReference.isNullOrBlank()) {
                            Text("UTR Ref: ${wd.transactionReference}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap to update status / enter UTR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (selectedWithdrawal != null) {
        val wd = selectedWithdrawal!!
        AlertDialog(
            onDismissRequest = { selectedWithdrawal = null },
            title = { Text("Update Withdrawal Payout") },
            text = {
                Column {
                    Text("Amount: ₹${"%.2f".format(wd.amount)} | UPI: ${wd.upiId}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Status", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Dropdown or radio chips
                    }
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(newStatus)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        statusOptions.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    newStatus = st
                                    expanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        label = { Text("Bank UTR / Transaction Reference") },
                        placeholder = { Text("e.g. UTR128492048") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adminNote,
                        onValueChange = { adminNote = it },
                        label = { Text("Admin Audit Note") },
                        placeholder = { Text("Reason or payout remarks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        coroutineScope.launch {
                            repository.updateWithdrawalStatus(
                                withdrawalId = wd.id,
                                newStatus = newStatus,
                                adminNote = adminNote.ifBlank { null },
                                transactionRef = transactionRef.ifBlank { null },
                                adminId = "admin_001"
                            )
                            isProcessing = false
                            selectedWithdrawal = null
                        }
                    },
                    enabled = !isProcessing
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedWithdrawal = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 3: CAMPAIGN MANAGEMENT
// -----------------------------------------------------------------------------
@Composable
private fun AdminCampaignsTab(repository: ReviewTaskRepository) {
    val coroutineScope = rememberCoroutineScope()
    val campaigns by repository.allCampaigns.collectAsStateWithLifecycle(initialValue = emptyList())
    val businesses by repository.allBusinesses.collectAsStateWithLifecycle(initialValue = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf("30.00") }
    var budget by remember { mutableStateOf("3000.00") }
    var participants by remember { mutableStateOf("100") }
    var estMinutes by remember { mutableStateOf("3") }
    var selectedBizId by remember { mutableStateOf("") }

    var editingCampaign by remember { mutableStateOf<Campaign?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editDesc by remember { mutableStateOf("") }
    var editReward by remember { mutableStateOf("") }
    var editBudget by remember { mutableStateOf("") }
    var editParticipants by remember { mutableStateOf("") }
    var editEstMinutes by remember { mutableStateOf("") }

    LaunchedEffect(businesses) {
        if (selectedBizId.isEmpty() && businesses.isNotEmpty()) {
            selectedBizId = businesses.first().id
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Campaigns (${campaigns.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("admin_create_campaign_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Campaign")
                }
            }
        }

        items(campaigns) { c ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        StatusBadge(c.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(c.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reward: ₹${"%.2f".format(c.rewardAmount)}", fontWeight = FontWeight.Bold, color = VibrantGreen)
                        Text("Remaining: ₹${"%.2f".format(c.remainingBudget)} / ₹${"%.2f".format(c.totalBudget)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val next = if (c.status == "ACTIVE") "PAUSED" else "ACTIVE"
                                coroutineScope.launch { repository.updateCampaignStatus(c.id, next) }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (c.status == "ACTIVE") "Pause" else "Activate")
                        }

                        Button(
                            onClick = {
                                editingCampaign = c
                                editTitle = c.title
                                editDesc = c.description
                                editReward = c.rewardAmount.toString()
                                editBudget = c.totalBudget.toString()
                                editParticipants = c.maxParticipants.toString()
                                editEstMinutes = c.estimatedMinutes.toString()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("admin_edit_campaign_btn_${c.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Task")
                        }
                    }
                }
            }
        }
    }

    // Edit Campaign Dialog
    if (editingCampaign != null) {
        val target = editingCampaign!!
        AlertDialog(
            onDismissRequest = { editingCampaign = null },
            title = { Text("Edit Task / Campaign") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Campaign Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editReward,
                            onValueChange = { editReward = it },
                            label = { Text("Reward (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editBudget,
                            onValueChange = { editBudget = it },
                            label = { Text("Budget (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editParticipants,
                            onValueChange = { editParticipants = it },
                            label = { Text("Max Users") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editEstMinutes,
                            onValueChange = { editEstMinutes = it },
                            label = { Text("Est. Mins") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = editReward.toDoubleOrNull() ?: target.rewardAmount
                        val b = editBudget.toDoubleOrNull() ?: target.totalBudget
                        val p = editParticipants.toIntOrNull() ?: target.maxParticipants
                        val m = editEstMinutes.toIntOrNull() ?: target.estimatedMinutes

                        coroutineScope.launch {
                            repository.editCampaign(
                                campaignId = target.id,
                                title = editTitle.ifBlank { target.title },
                                description = editDesc.ifBlank { target.description },
                                rewardAmount = r,
                                totalBudget = b,
                                maxParticipants = p,
                                estimatedMinutes = m
                            )
                            editingCampaign = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCampaign = null }) { Text("Cancel") }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Feedback Campaign") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Campaign Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = reward,
                            onValueChange = { reward = it },
                            label = { Text("Reward (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = budget,
                            onValueChange = { budget = it },
                            label = { Text("Budget (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = participants,
                            onValueChange = { participants = it },
                            label = { Text("Max Users") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = estMinutes,
                            onValueChange = { estMinutes = it },
                            label = { Text("Est. Mins") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = reward.toDoubleOrNull() ?: 30.0
                        val b = budget.toDoubleOrNull() ?: 3000.0
                        val p = participants.toIntOrNull() ?: 100
                        val m = estMinutes.toIntOrNull() ?: 3

                        coroutineScope.launch {
                            repository.createCampaign(
                                businessId = selectedBizId.ifEmpty { "biz_001" },
                                title = title.ifBlank { "Store Quality Audit" },
                                description = desc.ifBlank { "Provide honest customer feedback on store hygiene and service." },
                                rewardAmount = r,
                                totalBudget = b,
                                maxParticipants = p,
                                estimatedMinutes = m
                            )
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 4: USER MANAGEMENT
// -----------------------------------------------------------------------------
@Composable
private fun AdminUsersTab(repository: ReviewTaskRepository) {
    val coroutineScope = rememberCoroutineScope()
    val profiles by repository.allProfiles.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filtered = profiles.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search users by name or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered) { user ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(user.status)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Phone: +91 ${user.phone}", style = MaterialTheme.typography.bodySmall)
                        Text("UPI: ${user.upiId.ifEmpty { "Not linked" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Role: ${user.role}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.toggleUserStatus(user.id, user.status)
                            }
                        }
                    ) {
                        Text(if (user.status == "ACTIVE") "Suspend" else "Activate")
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 5: BUSINESS MANAGEMENT
// -----------------------------------------------------------------------------
@Composable
private fun AdminBusinessesTab(repository: ReviewTaskRepository) {
    val coroutineScope = rememberCoroutineScope()
    val businesses by repository.allBusinesses.collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Registered Businesses (${businesses.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(businesses) { b ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(b.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        StatusBadge(b.verificationStatus)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Category: ${b.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("Address: ${b.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (b.verificationStatus != "APPROVED") {
                            Button(
                                onClick = { coroutineScope.launch { repository.updateBusinessStatus(b.id, "APPROVED") } }
                            ) {
                                Text("Approve")
                            }
                        }
                        if (b.verificationStatus != "SUSPENDED") {
                            OutlinedButton(
                                onClick = { coroutineScope.launch { repository.updateBusinessStatus(b.id, "SUSPENDED") } }
                            ) {
                                Text("Suspend")
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 6: FEEDBACK & TASK AUDIT
// -----------------------------------------------------------------------------
@Composable
private fun AdminFeedbacksTab(repository: ReviewTaskRepository) {
    val feedbacks by repository.allFeedback.collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Logged Customer Feedbacks (${feedbacks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (feedbacks.isEmpty()) {
            item {
                Text("No feedbacks submitted yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(feedbacks) { fb ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Overall: ${fb.rating}/5 Stars", fontWeight = FontWeight.Bold, color = VibrantAmber)
                            Text(
                                SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(Date(fb.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Service: ${fb.serviceQuality}/5 | Cleanliness: ${fb.cleanliness}/5 | Satisfaction: ${fb.productSatisfaction}/5", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (fb.whatLiked.isNotBlank()) {
                            Text("Liked: ${fb.whatLiked}", style = MaterialTheme.typography.bodySmall, color = EmeraldGreenDark)
                        }
                        if (fb.whatCanImprove.isNotBlank()) {
                            Text("Improve: ${fb.whatCanImprove}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
