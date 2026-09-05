package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CampaignTaskDetail
import com.example.data.model.Profile
import com.example.data.model.Wallet
import com.example.data.repository.ReviewTaskRepository
import com.example.ui.components.PolicyNoticeCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    repository: ReviewTaskRepository,
    onNavigateToTasks: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onSelectTask: (campaignId: String) -> Unit
) {
    val profile by repository.currentProfile.collectAsStateWithLifecycle(initialValue = null)
    val wallet by repository.currentWallet.collectAsStateWithLifecycle(initialValue = null)
    val todayCompleted by repository.todayCompletedTasksCount.collectAsStateWithLifecycle(initialValue = 0)
    val pendingWithdrawalSum by repository.pendingWithdrawalsSum.collectAsStateWithLifecycle(initialValue = 0.0)
    val campaignTasks by repository.activeCampaignTasks.collectAsStateWithLifecycle(initialValue = emptyList())

    val availableTasksCount = campaignTasks.count { it.userTask?.status != "COMPLETED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Header
        item {
            HomeHeader(
                profile = profile,
                wallet = wallet,
                todayCompleted = todayCompleted,
                pendingWithdrawal = pendingWithdrawalSum,
                onWalletClick = onNavigateToWallet
            )
        }

        // Quick Navigation Buttons in Vibrant Accent Clusters
        item {
            QuickNavigationRow(
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        // Policy Notice
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                PolicyNoticeCard()
            }
        }

        // Hero Banner Card
        item {
            HeroBannerCard(onExploreTasks = onNavigateToTasks)
        }

        // Available Tasks Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Available Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Real-world genuine feedback rewards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onNavigateToTasks,
                    modifier = Modifier.testTag("view_all_tasks_btn")
                ) {
                    Text(
                        text = "See All",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Available Tasks List
        val displayTasks = campaignTasks.take(4)
        if (displayTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You're all caught up!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Check back soon for new genuine customer feedback campaigns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(displayTasks) { taskDetail ->
                TaskCardCompact(
                    taskDetail = taskDetail,
                    onSelect = { onSelectTask(taskDetail.campaign.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    profile: Profile?,
    wallet: Wallet?,
    todayCompleted: Int,
    pendingWithdrawal: Double,
    onWalletClick: () -> Unit
) {
    val userName = profile?.name?.ifEmpty { "Reviewer" } ?: "Reviewer"
    val initials = userName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "RS" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_header")
    ) {
        // User Greeting & Avatar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile?.isAdmin == true) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VibrantAmberContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "ADMIN",
                            color = OnVibrantAmber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                // Rounded Avatar with border
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(VibrantBlueContainer)
                        .border(2.dp, Color.White, CircleShape)
                        .shadow(2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnVibrantBlueContainer
                    )
                }
            }
        }

        // Vibrant Blue Wallet Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onWalletClick() }
                .testTag("wallet_balance_card"),
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
                            text = "WALLET BALANCE",
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

                    // Verified badge
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0x33FFFFFF)
                    ) {
                        Text(
                            text = if (profile?.status == "ACTIVE") "Verified" else "Member",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(color = Color(0x38FFFFFF), thickness = 1.dp)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Tasks Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantBlueContainer,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "%02d".format(todayCompleted),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = "Total Earned",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantBlueContainer,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "₹${"%.0f".format(wallet?.totalEarned ?: 0.0)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Pending Payout",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantBlueContainer,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "₹${"%.0f".format(pendingWithdrawal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNavigationRow(
    onNavigateToTasks: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tasks Action (Lavender / Purple)
        QuickActionPill(
            title = "Tasks",
            icon = Icons.Default.Assignment,
            containerColor = VibrantPurpleContainer,
            contentColor = OnVibrantPurple,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToTasks
        )

        // Withdraw Action (Periwinkle / Blue)
        QuickActionPill(
            title = "Withdraw",
            icon = Icons.Default.Payments,
            containerColor = VibrantCyanContainer,
            contentColor = OnVibrantCyan,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToWallet
        )

        // Refer Action (Coral / Rose)
        QuickActionPill(
            title = "Refer",
            icon = Icons.Default.CardGiftcard,
            containerColor = VibrantCoralContainer,
            contentColor = OnVibrantCoral,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToProfile
        )
    }
}

@Composable
private fun QuickActionPill(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("quick_btn_${title.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun HeroBannerCard(onExploreTasks: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onExploreTasks() }
            .testTag("hero_banner_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "Customer Feedback Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Help Local Businesses Deliver Better",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Store managers use your honest survey answers to upgrade customer service and product quality. Earn transparent rewards directly to your wallet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TaskCardCompact(
    taskDetail: CampaignTaskDetail,
    onSelect: () -> Unit
) {
    val isCompleted = taskDetail.userTask?.status == "COMPLETED"

    val categoryIcon = when (taskDetail.business.category.lowercase()) {
        "dining", "restaurant", "cafe", "coffee" -> Icons.Default.Coffee
        "retail", "shopping", "store" -> Icons.Default.ShoppingBag
        "wellness", "fitness", "health" -> Icons.Default.Spa
        "electronics", "tech" -> Icons.Default.Devices
        else -> Icons.Default.Storefront
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onSelect() }
            .testTag("task_item_card_${taskDetail.campaign.id}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Category Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = taskDetail.business.businessName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${taskDetail.business.category} • ${taskDetail.campaign.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                if (isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge("COMPLETED")
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%.2f".format(taskDetail.campaign.rewardAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${taskDetail.campaign.estimatedMinutes} mins",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

