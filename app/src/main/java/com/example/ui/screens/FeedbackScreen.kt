package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.ReviewTaskRepository
import com.example.data.service.AdMobService
import com.example.ui.components.StarRatingInput
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    taskId: String,
    campaignId: String,
    repository: ReviewTaskRepository,
    adMobService: AdMobService? = null,
    onNavigateBack: () -> Unit,
    onSubmitSuccess: (taskId: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val campaignTasks by repository.activeCampaignTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val taskDetail = campaignTasks.find { it.userTask?.id == taskId || it.campaign.id == campaignId }
    val userTask = taskDetail?.userTask

    var overallRating by remember { mutableStateOf(5) }
    var serviceQuality by remember { mutableStateOf(5) }
    var cleanliness by remember { mutableStateOf(5) }
    var productSatisfaction by remember { mutableStateOf(5) }
    var whatLiked by remember { mutableStateOf("") }
    var whatCanImprove by remember { mutableStateOf("") }
    var proofText by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submissionNotice by remember { mutableStateOf<String?>(null) }

    val taskStatus = userTask?.status?.uppercase() ?: "IN_PROGRESS"
    val isAlreadySubmitted = taskStatus in listOf("PENDING", "SUBMITTED", "APPROVED", "COMPLETED")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Proof & Feedback", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        StatusBadge(taskStatus)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("feedback_screen")
        ) {
            // If already submitted, display status card
            if (taskStatus == "PENDING" || taskStatus == "SUBMITTED") {
                Surface(
                    color = VibrantAmberContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = OnVibrantAmber)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Submission Under Review (Pending)",
                                fontWeight = FontWeight.Bold,
                                color = OnVibrantAmber,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Your proof has been submitted. Once verified by an admin, your reward of ₹${"%.2f".format(taskDetail?.campaign?.rewardAmount ?: 0.0)} will be added to your wallet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnVibrantAmber
                            )
                        }
                    }
                }
            } else if (taskStatus in listOf("APPROVED", "COMPLETED")) {
                Surface(
                    color = VibrantGreenContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnVibrantGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Submission Approved & Rewarded",
                                fontWeight = FontWeight.Bold,
                                color = OnVibrantGreen,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Your submission was approved! The reward of ₹${"%.2f".format(taskDetail?.campaign?.rewardAmount ?: 0.0)} has been credited to your wallet balance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnVibrantGreen
                            )
                        }
                    }
                }
            } else if (taskStatus == "REJECTED") {
                Surface(
                    color = VibrantErrorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = VibrantError)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Submission Rejected",
                                fontWeight = FontWeight.Bold,
                                color = VibrantError,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                userTask?.adminNote ?: "Proof did not meet verification criteria. You may resubmit valid proof.",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibrantError
                            )
                        }
                    }
                }
            }

            // Notice banner: No forced ratings
            Surface(
                color = VibrantBlueContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = OnVibrantBlueContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Submit honest feedback along with required proof (e.g. visit notes, bill ID, or verification details).",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnVibrantBlueContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Task Proof Input
            Text(
                text = "Required Task Proof *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter your order ID, bill/receipt reference, or specific confirmation details proving your experience.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = proofText,
                onValueChange = { proofText = it; errorMessage = null },
                placeholder = { Text("e.g. Order #104928, visited on Tuesday 3pm, served by Rahul") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("task_proof_input"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isAlreadySubmitted && !isLoading
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Rating Question 1: Overall Experience
            RatingQuestionCard(
                title = "1. Overall Experience Rating",
                subtitle = "How was your overall impression with this business?",
                rating = overallRating,
                onRatingChanged = { if (!isAlreadySubmitted) overallRating = it },
                testTagPrefix = "overall"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rating Question 2: Service Quality
            RatingQuestionCard(
                title = "2. Service Quality & Staff Helpfulness",
                subtitle = "Was the staff attentive, knowledgeable, and polite?",
                rating = serviceQuality,
                onRatingChanged = { if (!isAlreadySubmitted) serviceQuality = it },
                testTagPrefix = "service"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rating Question 3: Cleanliness & Hygiene
            RatingQuestionCard(
                title = "3. Cleanliness & Atmosphere",
                subtitle = "How well-maintained and organized were the premises?",
                rating = cleanliness,
                onRatingChanged = { if (!isAlreadySubmitted) cleanliness = it },
                testTagPrefix = "cleanliness"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rating Question 4: Product / Service Satisfaction
            RatingQuestionCard(
                title = "4. Product / Service Satisfaction",
                subtitle = "Did the offering fulfill your expectations?",
                rating = productSatisfaction,
                onRatingChanged = { if (!isAlreadySubmitted) productSatisfaction = it },
                testTagPrefix = "satisfaction"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Open Question 1: What did you like?
            Text(
                text = "5. What did you like most? *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = whatLiked,
                onValueChange = { whatLiked = it; errorMessage = null },
                placeholder = { Text("Mention specific strengths, e.g. quick turnaround, friendly staff, delicious food...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("what_liked_input"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isAlreadySubmitted && !isLoading
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Open Question 2: What could be improved?
            Text(
                text = "6. What could be improved? *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = whatCanImprove,
                onValueChange = { whatCanImprove = it; errorMessage = null },
                placeholder = { Text("Suggest constructive changes, e.g. waiting time, seating, pricing clarity...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("what_improve_input"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isAlreadySubmitted && !isLoading
            )

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

            if (submissionNotice != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = VibrantGreenContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = submissionNotice ?: "",
                        color = OnVibrantGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (isAlreadySubmitted) return@Button

                    if (proofText.trim().length < 3) {
                        errorMessage = "Please enter task proof details (e.g. order ID, bill receipt, or visit reference)."
                        return@Button
                    }
                    if (whatLiked.trim().length < 4) {
                        errorMessage = "Please enter what you liked (minimum 4 characters)."
                        return@Button
                    }
                    if (whatCanImprove.trim().length < 4) {
                        errorMessage = "Please enter what could be improved (minimum 4 characters)."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = repository.submitFeedback(
                            taskId = taskId,
                            rating = overallRating,
                            serviceQuality = serviceQuality,
                            cleanliness = cleanliness,
                            productSatisfaction = productSatisfaction,
                            whatLiked = whatLiked.trim(),
                            whatCanImprove = whatCanImprove.trim(),
                            proofText = proofText.trim()
                        )
                        isLoading = false
                        result.onSuccess {
                            submissionNotice = "Task proof submitted successfully! Status is now PENDING review."
                            val activity = context as? Activity
                            if (adMobService != null && activity != null) {
                                adMobService.showInterstitial(activity) {
                                    onSubmitSuccess(taskId)
                                }
                            } else {
                                onSubmitSuccess(taskId)
                            }
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Failed to submit task proof."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_feedback_btn"),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading && !isAlreadySubmitted
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    val label = when (taskStatus) {
                        "PENDING", "SUBMITTED" -> "Submission Pending Approval"
                        "APPROVED", "COMPLETED" -> "Submission Approved"
                        "REJECTED" -> "Resubmit Proof"
                        else -> "Submit Task Proof for Review"
                    }
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun RatingQuestionCard(
    title: String,
    subtitle: String,
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    testTagPrefix: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            StarRatingInput(
                rating = rating,
                onRatingChanged = onRatingChanged,
                modifier = Modifier.testTag("rating_$testTagPrefix")
            )
        }
    }
}
