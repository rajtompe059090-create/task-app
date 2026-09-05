package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WalletTransaction
import com.example.data.model.Withdrawal
import com.example.data.repository.ReviewTaskRepository
import com.example.data.service.AdMobService
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    repository: ReviewTaskRepository,
    adMobService: AdMobService? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by repository.currentProfile.collectAsStateWithLifecycle(initialValue = null)
    val wallet by repository.currentWallet.collectAsStateWithLifecycle(initialValue = null)
    val transactions by repository.currentTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val withdrawals by repository.currentWithdrawals.collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingSum by repository.pendingWithdrawalsSum.collectAsStateWithLifecycle(initialValue = 0.0)
    val minWithdrawal by repository.minWithdrawalAmount.collectAsStateWithLifecycle(initialValue = 50.0)

    var showWithdrawModal by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    var withdrawUpi by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var withdrawError by remember { mutableStateOf<String?>(null) }
    var withdrawSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isWatchingAd by remember { mutableStateOf(false) }
    var adRewardFeedback by remember { mutableStateOf<String?>(null) }

    // Prefill UPI from profile
    LaunchedEffect(profile?.upiId) {
        if (withdrawUpi.isEmpty() && !profile?.upiId.isNullOrBlank()) {
            withdrawUpi = profile?.upiId ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings & Wallet", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("wallet_screen"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Main Balance Card in Vibrant Blue 32dp container
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("wallet_main_card"),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibrantBlueContainer,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "₹${"%.2f".format(wallet?.balance ?: 0.0)}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0x33FFFFFF)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Instant UPI",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showWithdrawModal = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("request_withdrawal_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = VibrantBlue
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request Withdrawal (UPI)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Stats row (Total Earned, Total Withdrawn, Pending)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Earned",
                        value = "₹${"%.2f".format(wallet?.totalEarned ?: 0.0)}",
                        icon = Icons.Default.TrendingUp,
                        iconTint = VibrantGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Withdrawn",
                        value = "₹${"%.2f".format(wallet?.totalWithdrawn ?: 0.0)}",
                        icon = Icons.Default.CheckCircleOutline,
                        iconTint = VibrantBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    StatCard(
                        title = "Pending Withdrawals",
                        value = "₹${"%.2f".format(pendingSum)}",
                        icon = Icons.Default.HourglassTop,
                        iconTint = VibrantAmber,
                        subtitle = "Manual verification queue by admin"
                    )
                }
            }

            // Optional Rewarded Ad Bonus Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, VibrantGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("rewarded_ad_bonus_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = VibrantGreenContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = VibrantGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SPONSORED VIDEO BONUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = VibrantGreen
                                    )
                                    Text(
                                        text = "Earn +₹5.00 Instantly",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = VibrantGreenContainer
                            ) {
                                Text(
                                    text = "+₹5.00",
                                    color = VibrantGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Watch an optional sponsored video to the end to receive instant bonus credits. Rewards are granted only after full video completion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        if (adRewardFeedback != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = adRewardFeedback ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity == null || adMobService == null) {
                                    adRewardFeedback = "Ad service is not available on this screen."
                                    return@Button
                                }
                                isWatchingAd = true
                                adRewardFeedback = null

                                val showed = adMobService.showRewarded(
                                    activity = activity,
                                    onRewardEarned = { amount, type ->
                                        coroutineScope.launch {
                                            val result = repository.creditRewardedAdBonus(5.0)
                                            result.onSuccess {
                                                adRewardFeedback = "🎉 Congratulations! ₹5.00 bonus credited to your wallet balance."
                                            }.onFailure { err ->
                                                adRewardFeedback = "Could not credit reward: ${err.message}"
                                            }
                                        }
                                    },
                                    onDismissed = {
                                        isWatchingAd = false
                                    }
                                )

                                if (!showed) {
                                    adRewardFeedback = "Sponsored video is loading in background. Please tap again in 5 seconds."
                                    isWatchingAd = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("watch_rewarded_ad_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibrantGreen,
                                contentColor = Color.White
                            ),
                            enabled = !isWatchingAd
                        ) {
                            if (isWatchingAd) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Playing Video...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(Icons.Default.SmartDisplay, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Watch Video (+₹5.00 Bonus)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Active / Recent Withdrawal Requests
            if (withdrawals.isNotEmpty()) {
                item {
                    Text(
                        text = "Withdrawal Requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                items(withdrawals) { wd ->
                    WithdrawalItemCard(withdrawal = wd)
                }
            }

            // Transaction History Header
            item {
                Text(
                    text = "Ledger Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "No transactions yet. Complete genuine feedback tasks to earn rewards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(transactions) { tx ->
                    TransactionItemCard(transaction = tx)
                }
            }
        }
    }

    // Withdrawal Request Bottom Sheet / Dialog
    if (showWithdrawModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showWithdrawModal = false
                withdrawError = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("withdrawal_modal")
            ) {
                Text(
                    text = "Request UPI Withdrawal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Minimum withdrawal is ₹${"%.2f".format(minWithdrawal)}. For MVP, requests are manually checked and processed by admin into your UPI account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = withdrawAmount,
                    onValueChange = { withdrawAmount = it; withdrawError = null },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("Min. ₹${"%.0f".format(minWithdrawal)}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = withdrawUpi,
                    onValueChange = { withdrawUpi = it; withdrawError = null },
                    label = { Text("Receiving UPI ID") },
                    placeholder = { Text("e.g. mobile@paytm or name@okaxis") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_upi_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (withdrawError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = withdrawError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull()
                        if (amt == null || amt <= 0) {
                            withdrawError = "Please enter a valid amount."
                            return@Button
                        }
                        if (amt < minWithdrawal) {
                            withdrawError = "Minimum withdrawal is ₹${"%.2f".format(minWithdrawal)}."
                            return@Button
                        }
                        if (withdrawUpi.isBlank() || !withdrawUpi.contains("@")) {
                            withdrawError = "Please enter a valid UPI ID (e.g. user@bank)."
                            return@Button
                        }

                        isSubmitting = true
                        withdrawError = null
                        coroutineScope.launch {
                            val result = repository.requestWithdrawal(amt, withdrawUpi)
                            isSubmitting = false
                            result.onSuccess {
                                showWithdrawModal = false
                                withdrawAmount = ""
                            }.onFailure { err ->
                                withdrawError = err.message ?: "Failed to submit withdrawal request."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_withdrawal_modal_btn"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Submit for Admin Verification", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun WithdrawalItemCard(withdrawal: Withdrawal) {
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(withdrawal.createdAt))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("withdrawal_item_${withdrawal.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${"%.2f".format(withdrawal.amount)} to ${withdrawal.upiId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusBadge(status = withdrawal.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Requested: $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!withdrawal.adminNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Admin Note: ${withdrawal.adminNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!withdrawal.transactionReference.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ref/UTR: ${withdrawal.transactionReference}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionItemCard(transaction: WalletTransaction) {
    val isCredit = transaction.type == "CREDIT"
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.createdAt))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("tx_item_${transaction.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCredit) VibrantGreenContainer
                        else VibrantCoralContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isCredit) OnVibrantGreen else OnVibrantCoral,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}₹${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) VibrantGreen else VibrantError
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(status = transaction.status)
            }
        }
    }
}
