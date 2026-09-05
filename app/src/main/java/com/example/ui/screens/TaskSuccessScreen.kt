package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.ReviewTaskRepository
import com.example.ui.theme.*

@Composable
fun TaskSuccessScreen(
    campaignId: String,
    repository: ReviewTaskRepository,
    onNavigateToHome: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val context = LocalContext.current
    val campaignTasks by repository.activeCampaignTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val taskDetail = campaignTasks.find { it.campaign.id == campaignId }
    val reward = taskDetail?.campaign?.rewardAmount ?: 25.0
    val mapsUrl = taskDetail?.business?.googleMapsUrl ?: "https://maps.google.com"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("task_success_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Success Checkmark Circle
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(VibrantGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(VibrantGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Feedback Recorded!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Thank you for sharing your genuine customer perspective. Your answers have been securely logged for store quality improvement.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Reward Credit Box
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REWARD CREDITED TO WALLET",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "+₹${"%.2f".format(reward)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = VibrantGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A ledger credit entry was automatically saved to your wallet transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Optional Google Maps Button with Explicit Disclaimer
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VibrantBlueContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Optional: Google Maps Listing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnVibrantBlueContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Leaving a Google review is completely optional, independent, and unrelated to your reward. ReviewTask does not require, verify, or pay for public Google Maps reviews.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnVibrantBlueContainer.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_maps_optional_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = VibrantBlue
                    )
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Business on Google Maps (Optional)", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigateToWallet,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("success_view_wallet_btn"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Wallet & Transactions", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onNavigateToHome,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("success_home_btn")
        ) {
            Text("Back to Home", fontWeight = FontWeight.Medium)
        }
    }
}
