package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.*

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["phone"], unique = true)]
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val whatsapp: String = "",
    val upiId: String = "",
    val role: String = "USER",
    val status: String = "ACTIVE",
    val referralCode: String = "",
    val referredBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Profile(
        id = id,
        name = name,
        phone = phone,
        whatsapp = whatsapp,
        upiId = upiId,
        role = role,
        status = status,
        referralCode = referralCode,
        referredBy = referredBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(p: Profile) = ProfileEntity(
            id = p.id,
            name = p.name,
            phone = p.phone,
            whatsapp = p.whatsapp,
            upiId = p.upiId,
            role = p.role,
            status = p.status,
            referralCode = p.referralCode,
            referredBy = p.referredBy,
            createdAt = p.createdAt,
            updatedAt = p.updatedAt
        )
    }
}

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val businessName: String,
    val category: String,
    val address: String,
    val googleMapsUrl: String,
    val verificationStatus: String = "APPROVED",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Business(
        id = id,
        ownerId = ownerId,
        businessName = businessName,
        category = category,
        address = address,
        googleMapsUrl = googleMapsUrl,
        verificationStatus = verificationStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(b: Business) = BusinessEntity(
            id = b.id,
            ownerId = b.ownerId,
            businessName = b.businessName,
            category = b.category,
            address = b.address,
            googleMapsUrl = b.googleMapsUrl,
            verificationStatus = b.verificationStatus,
            createdAt = b.createdAt,
            updatedAt = b.updatedAt
        )
    }
}

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val title: String,
    val description: String,
    val rewardAmount: Double,
    val totalBudget: Double,
    val remainingBudget: Double,
    val maxParticipants: Int = 50,
    val status: String = "ACTIVE",
    val estimatedMinutes: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Campaign(
        id = id,
        businessId = businessId,
        title = title,
        description = description,
        rewardAmount = rewardAmount,
        totalBudget = totalBudget,
        remainingBudget = remainingBudget,
        maxParticipants = maxParticipants,
        status = status,
        estimatedMinutes = estimatedMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(c: Campaign) = CampaignEntity(
            id = c.id,
            businessId = c.businessId,
            title = c.title,
            description = c.description,
            rewardAmount = c.rewardAmount,
            totalBudget = c.totalBudget,
            remainingBudget = c.remainingBudget,
            maxParticipants = c.maxParticipants,
            status = c.status,
            estimatedMinutes = c.estimatedMinutes,
            createdAt = c.createdAt,
            updatedAt = c.updatedAt
        )
    }
}

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["userId", "campaignId"], unique = true)]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val userId: String,
    val status: String = "IN_PROGRESS",
    val rewardAmount: Double,
    val proof: String = "",
    val adminNote: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = TaskItem(
        id = id,
        campaignId = campaignId,
        userId = userId,
        status = status,
        rewardAmount = rewardAmount,
        proof = proof,
        adminNote = adminNote,
        startedAt = startedAt,
        submittedAt = submittedAt,
        completedAt = completedAt,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(t: TaskItem) = TaskEntity(
            id = t.id,
            campaignId = t.campaignId,
            userId = t.userId,
            status = t.status,
            rewardAmount = t.rewardAmount,
            proof = t.proof,
            adminNote = t.adminNote,
            startedAt = t.startedAt,
            submittedAt = t.submittedAt,
            completedAt = t.completedAt,
            createdAt = t.createdAt
        )
    }
}

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val rating: Int,
    val serviceQuality: Int = 5,
    val cleanliness: Int = 5,
    val productSatisfaction: Int = 5,
    val answersJson: String = "",
    val comment: String = "",
    val whatLiked: String = "",
    val whatCanImprove: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = FeedbackItem(
        id = id,
        taskId = taskId,
        rating = rating,
        serviceQuality = serviceQuality,
        cleanliness = cleanliness,
        productSatisfaction = productSatisfaction,
        answersJson = answersJson,
        comment = comment,
        whatLiked = whatLiked,
        whatCanImprove = whatCanImprove,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(f: FeedbackItem) = FeedbackEntity(
            id = f.id,
            taskId = f.taskId,
            rating = f.rating,
            serviceQuality = f.serviceQuality,
            cleanliness = f.cleanliness,
            productSatisfaction = f.productSatisfaction,
            answersJson = f.answersJson,
            comment = f.comment,
            whatLiked = f.whatLiked,
            whatCanImprove = f.whatCanImprove,
            createdAt = f.createdAt
        )
    }
}

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val balance: Double = 0.0,
    val totalEarned: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Wallet(
        id = id,
        userId = userId,
        balance = balance,
        totalEarned = totalEarned,
        totalWithdrawn = totalWithdrawn,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(w: Wallet) = WalletEntity(
            id = w.id,
            userId = w.userId,
            balance = w.balance,
            totalEarned = w.totalEarned,
            totalWithdrawn = w.totalWithdrawn,
            createdAt = w.createdAt,
            updatedAt = w.updatedAt
        )
    }
}

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // CREDIT, DEBIT
    val amount: Double,
    val referenceId: String? = null,
    val status: String = "SUCCESS",
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = WalletTransaction(
        id = id,
        userId = userId,
        type = type,
        amount = amount,
        referenceId = referenceId,
        status = status,
        description = description,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(wt: WalletTransaction) = WalletTransactionEntity(
            id = wt.id,
            userId = wt.userId,
            type = wt.type,
            amount = wt.amount,
            referenceId = wt.referenceId,
            status = wt.status,
            description = wt.description,
            createdAt = wt.createdAt
        )
    }
}

@Entity(tableName = "withdrawals")
data class WithdrawalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val upiId: String,
    val status: String = "SUBMITTED",
    val adminNote: String? = null,
    val transactionReference: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val adminId: String? = null
) {
    fun toDomain() = Withdrawal(
        id = id,
        userId = userId,
        amount = amount,
        upiId = upiId,
        status = status,
        adminNote = adminNote,
        transactionReference = transactionReference,
        createdAt = createdAt,
        processedAt = processedAt,
        adminId = adminId
    )

    companion object {
        fun fromDomain(w: Withdrawal) = WithdrawalEntity(
            id = w.id,
            userId = w.userId,
            amount = w.amount,
            upiId = w.upiId,
            status = w.status,
            adminNote = w.adminNote,
            transactionReference = w.transactionReference,
            createdAt = w.createdAt,
            processedAt = w.processedAt,
            adminId = w.adminId
        )
    }
}

@Entity(tableName = "referrals")
data class ReferralEntity(
    @PrimaryKey val id: String,
    val referrerId: String,
    val referredUserId: String,
    val bonus: Double = 10.0,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Referral(
        id = id,
        referrerId = referrerId,
        referredUserId = referredUserId,
        bonus = bonus,
        status = status,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(r: Referral) = ReferralEntity(
            id = r.id,
            referrerId = r.referrerId,
            referredUserId = r.referredUserId,
            bonus = r.bonus,
            status = r.status,
            createdAt = r.createdAt
        )
    }
}

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val id: String,
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = AppSetting(
        id = id,
        key = key,
        value = value,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(s: AppSetting) = SettingEntity(
            id = s.id,
            key = s.key,
            value = s.value,
            updatedAt = s.updatedAt
        )
    }
}

fun WalletEntity.toRemoteDto(): Map<String, Any> = mapOf(
    "id" to id,
    "user_id" to userId,
    "balance" to balance,
    "total_earned" to totalEarned,
    "total_withdrawn" to totalWithdrawn
)

fun WalletEntity.toRemoteMap(): Map<String, Any> = toRemoteDto()

fun TaskEntity.toRemoteDto(): Map<String, Any> = buildMap {
    put("id", id)
    put("campaign_id", campaignId)
    put("user_id", userId)
    put("status", status)
    put("reward_amount", rewardAmount)
    put("proof", proof)
    if (adminNote != null) put("admin_note", adminNote)
}

fun CampaignEntity.toRemoteDto(): Map<String, Any> = mapOf(
    "id" to id,
    "business_id" to businessId,
    "title" to title,
    "description" to description,
    "reward_amount" to rewardAmount,
    "total_budget" to totalBudget,
    "remaining_budget" to remainingBudget,
    "max_participants" to maxParticipants,
    "status" to status
)
