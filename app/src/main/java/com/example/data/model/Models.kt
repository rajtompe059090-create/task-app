package com.example.data.model

data class Profile(
    val id: String,
    val name: String,
    val phone: String,
    val whatsapp: String = "",
    val upiId: String = "",
    val role: String = "USER", // USER, ADMIN, BUSINESS
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED
    val referralCode: String = "",
    val referredBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role.equals("ADMIN", ignoreCase = true)
    val isSuspended: Boolean get() = status.equals("SUSPENDED", ignoreCase = true)
}

data class Business(
    val id: String,
    val ownerId: String,
    val businessName: String,
    val category: String,
    val address: String,
    val googleMapsUrl: String,
    val verificationStatus: String = "APPROVED", // PENDING, APPROVED, REJECTED, SUSPENDED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Campaign(
    val id: String,
    val businessId: String,
    val title: String,
    val description: String,
    val rewardAmount: Double,
    val totalBudget: Double,
    val remainingBudget: Double,
    val maxParticipants: Int = 50,
    val status: String = "ACTIVE", // DRAFT, PENDING_APPROVAL, ACTIVE, PAUSED, COMPLETED
    val estimatedMinutes: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskItem(
    val id: String,
    val campaignId: String,
    val userId: String,
    val status: String = "IN_PROGRESS", // AVAILABLE, IN_PROGRESS, PENDING, APPROVED, REJECTED, COMPLETED
    val rewardAmount: Double,
    val proof: String = "",
    val adminNote: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class FeedbackItem(
    val id: String,
    val taskId: String,
    val rating: Int, // 1 to 5
    val serviceQuality: Int = 5,
    val cleanliness: Int = 5,
    val productSatisfaction: Int = 5,
    val answersJson: String = "",
    val comment: String = "",
    val whatLiked: String = "",
    val whatCanImprove: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Wallet(
    val id: String,
    val userId: String,
    val balance: Double = 0.0,
    val totalEarned: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class WalletTransaction(
    val id: String,
    val userId: String,
    val type: String, // CREDIT, DEBIT
    val amount: Double,
    val referenceId: String? = null,
    val status: String = "SUCCESS", // SUCCESS, PENDING, FAILED
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class Withdrawal(
    val id: String,
    val userId: String,
    val amount: Double,
    val upiId: String,
    val status: String = "SUBMITTED", // SUBMITTED, UNDER_REVIEW, APPROVED, PROCESSING, SUCCESSFUL, REJECTED
    val adminNote: String? = null,
    val transactionReference: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val adminId: String? = null
)

data class Referral(
    val id: String,
    val referrerId: String,
    val referredUserId: String,
    val bonus: Double = 10.0,
    val status: String = "PENDING", // PENDING, COMPLETED
    val createdAt: Long = System.currentTimeMillis()
)

data class AppSetting(
    val id: String,
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

// Combined UI representation for rich task cards
data class CampaignTaskDetail(
    val campaign: Campaign,
    val business: Business,
    val userTask: TaskItem? = null
)
