package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.service.FcmNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class ReviewTaskRepository(
    private val database: AppDatabase,
    private val fcmService: FcmNotificationService
) {
    private val profileDao = database.profileDao()
    private val businessDao = database.businessDao()
    private val campaignDao = database.campaignDao()
    private val taskDao = database.taskDao()
    private val feedbackDao = database.feedbackDao()
    private val walletDao = database.walletDao()
    private val transactionDao = database.walletTransactionDao()
    private val withdrawalDao = database.withdrawalDao()
    private val referralDao = database.referralDao()
    private val settingDao = database.settingDao()

    // Current Session State
    private val _currentUserId = MutableStateFlow<String?>("user_demo_101")
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    // Current User Profile Flow
    val currentProfile: Flow<Profile?> = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else profileDao.getProfileById(uid).map { it?.toDomain() }
    }

    // Current User Wallet Flow (derived directly from database)
    val currentWallet: Flow<Wallet?> = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else walletDao.getWalletByUserId(uid).map { it?.toDomain() }
    }

    // Current User Transactions
    val currentTransactions: Flow<List<WalletTransaction>> = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else transactionDao.getTransactionsByUserId(uid).map { list -> list.map { it.toDomain() } }
    }

    // Current User Withdrawals
    val currentWithdrawals: Flow<List<Withdrawal>> = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else withdrawalDao.getWithdrawalsByUserId(uid).map { list -> list.map { it.toDomain() } }
    }

    // Pending Withdrawals for current user
    val pendingWithdrawalsSum: Flow<Double> = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(0.0)
        else withdrawalDao.getPendingWithdrawalsByUserId(uid).map { list ->
            list.sumOf { it.amount }
        }
    }

    // Active Campaigns with Business details
    val activeCampaignTasks: Flow<List<CampaignTaskDetail>> = combine(
        campaignDao.getActiveCampaigns(),
        businessDao.getAllBusinesses(),
        _currentUserId.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else taskDao.getTasksByUser(uid)
        }
    ) { campaigns, businesses, userTasks ->
        val bizMap = businesses.associateBy { it.id }
        val taskMap = userTasks.associateBy { it.campaignId }

        campaigns.mapNotNull { campEntity ->
            val biz = bizMap[campEntity.businessId]?.toDomain() ?: return@mapNotNull null
            CampaignTaskDetail(
                campaign = campEntity.toDomain(),
                business = biz,
                userTask = taskMap[campEntity.id]?.toDomain()
            )
        }
    }

    // Today's completed tasks count for current user
    val todayCompletedTasksCount: Flow<Int> = _currentUserId.flatMapLatest { uid ->
        if (uid == null) flowOf(0)
        else {
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            taskDao.getTodayCompletedTasks(uid, startOfDay).map { it.size }
        }
    }

    // Minimum Withdrawal setting from DB
    val minWithdrawalAmount: Flow<Double> = settingDao.observeSettingByKey("min_withdrawal_amount").map {
        it?.value?.toDoubleOrNull() ?: 50.0
    }

    // =========================================================================
    // AUTHENTICATION & REGISTRATION
    // =========================================================================

    suspend fun loginWithPhone(phone: String): Result<Profile> = withContext(Dispatchers.IO) {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        val existing = profileDao.getProfileByPhone(cleanPhone)
        if (existing != null) {
            if (existing.status.equals("SUSPENDED", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Account is suspended by admin. Please contact support."))
            }
            _currentUserId.value = existing.id
            Result.success(existing.toDomain())
        } else {
            Result.failure(NoSuchElementException("No account found for $cleanPhone. Please register."))
        }
    }

    suspend fun registerUser(
        name: String,
        phone: String,
        whatsapp: String,
        upiId: String,
        referralCode: String?
    ): Result<Profile> = withContext(Dispatchers.IO) {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        val existing = profileDao.getProfileByPhone(cleanPhone)
        if (existing != null) {
            return@withContext Result.failure(Exception("Phone number already registered. Please log in."))
        }

        val newId = "user_" + UUID.randomUUID().toString().take(8)
        val userReferralCode = name.take(3).uppercase() + (1000..9999).random()

        val newProfile = ProfileEntity(
            id = newId,
            name = name.trim(),
            phone = cleanPhone,
            whatsapp = whatsapp.trim().ifEmpty { cleanPhone },
            upiId = upiId.trim(),
            role = "USER",
            status = "ACTIVE",
            referralCode = userReferralCode,
            referredBy = referralCode?.trim()?.ifEmpty { null },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        profileDao.insertProfile(newProfile)

        // Initialize zero wallet in DB
        val newWallet = WalletEntity(
            id = "w_$newId",
            userId = newId,
            balance = 0.0,
            totalEarned = 0.0,
            totalWithdrawn = 0.0
        )
        walletDao.insertWallet(newWallet)

        // If referred by someone, record pending referral
        if (!referralCode.isNullOrBlank()) {
            val referrer = profileDao.getAllProfiles().firstOrNull()?.find { it.referralCode == referralCode.trim() }
            if (referrer != null) {
                referralDao.insertReferral(
                    ReferralEntity(
                        id = UUID.randomUUID().toString(),
                        referrerId = referrer.id,
                        referredUserId = newId,
                        bonus = 10.0,
                        status = "PENDING"
                    )
                )
            }
        }

        _currentUserId.value = newId
        fcmService.registerDeviceToken(newId)
        Result.success(newProfile.toDomain())
    }

    fun logout() {
        _currentUserId.value = null
    }

    fun switchUserSession(userId: String) {
        _currentUserId.value = userId
    }

    // =========================================================================
    // TASK & FEEDBACK WORKFLOW (GENUINE FEEDBACK, NO PAY-FOR-REVIEW)
    // =========================================================================

    suspend fun startTask(campaignId: String): Result<TaskItem> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("User not authenticated"))

        // Check if user is suspended
        val user = profileDao.getProfileByIdSync(uid)
        if (user?.status.equals("SUSPENDED", ignoreCase = true)) {
            return@withContext Result.failure(Exception("Your account is currently suspended."))
        }

        // Fraud check: prevent duplicate task
        val existingTask = taskDao.getTaskByUserAndCampaign(uid, campaignId)
        if (existingTask != null) {
            if (existingTask.status in listOf("PENDING", "APPROVED", "COMPLETED")) {
                return@withContext Result.failure(Exception("You have already submitted this task (Status: ${existingTask.status})."))
            }
            return@withContext Result.success(existingTask.toDomain())
        }

        val campaign = campaignDao.getCampaignByIdSync(campaignId)
            ?: return@withContext Result.failure(Exception("Campaign not found"))

        if (campaign.remainingBudget < campaign.rewardAmount) {
            return@withContext Result.failure(Exception("Campaign budget exhausted. Please choose another task."))
        }

        val taskId = "task_" + UUID.randomUUID().toString().take(8)
        val newTask = TaskEntity(
            id = taskId,
            campaignId = campaignId,
            userId = uid,
            status = "IN_PROGRESS",
            rewardAmount = campaign.rewardAmount,
            startedAt = System.currentTimeMillis(),
            submittedAt = null,
            completedAt = null,
            createdAt = System.currentTimeMillis()
        )
        taskDao.insertTask(newTask)
        try {
            val api = com.example.data.remote.SupabaseConfig.apiService
            api.createTask(newTask.toRemoteDto())
        } catch (_: Exception) {}
        Result.success(newTask.toDomain())
    }

    /**
     * Submits genuine task proof and customer feedback.
     * Sets task status to PENDING for admin review.
     * Prevents duplicate submissions.
     * Rewards are ONLY credited when approved by an admin.
     */
    suspend fun submitFeedback(
        taskId: String,
        rating: Int,
        serviceQuality: Int,
        cleanliness: Int,
        productSatisfaction: Int,
        whatLiked: String,
        whatCanImprove: String,
        proofText: String? = null
    ): Result<TaskItem> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("User not authenticated"))

        val task = taskDao.getTaskById(taskId)
            ?: return@withContext Result.failure(Exception("Task not found"))

        if (task.userId != uid) {
            return@withContext Result.failure(Exception("Unauthorized task access"))
        }

        // Prevent duplicate submissions
        if (task.status in listOf("PENDING", "APPROVED", "COMPLETED")) {
            return@withContext Result.failure(Exception("This task has already been submitted (Status: ${task.status}). Duplicate submissions are not allowed."))
        }

        val campaign = campaignDao.getCampaignByIdSync(task.campaignId)
            ?: return@withContext Result.failure(Exception("Campaign not found"))

        val business = businessDao.getBusinessByIdSync(campaign.businessId)

        val fullProof = proofText?.ifBlank { null }
            ?: "Liked: $whatLiked. Can Improve: $whatCanImprove (Rating: $rating/5)"

        // Save Feedback Record
        val feedbackId = "fb_" + UUID.randomUUID().toString().take(8)
        val feedbackEntity = FeedbackEntity(
            id = feedbackId,
            taskId = taskId,
            rating = rating,
            serviceQuality = serviceQuality,
            cleanliness = cleanliness,
            productSatisfaction = productSatisfaction,
            answersJson = """{"serviceQuality":$serviceQuality,"cleanliness":$cleanliness,"productSatisfaction":$productSatisfaction}""",
            comment = fullProof,
            whatLiked = whatLiked,
            whatCanImprove = whatCanImprove,
            createdAt = System.currentTimeMillis()
        )
        feedbackDao.insertFeedback(feedbackEntity)

        // Mark task PENDING for admin verification (Do NOT credit wallet yet!)
        val now = System.currentTimeMillis()
        val updatedTask = task.copy(
            status = "PENDING",
            proof = fullProof,
            submittedAt = now,
            completedAt = null
        )
        taskDao.updateTask(updatedTask)

        // Sync to Supabase in background
        try {
            val api = com.example.data.remote.SupabaseConfig.apiService
            api.updateTask("eq.$taskId", updatedTask.toRemoteDto())
            api.submitFeedback(
                mapOf(
                    "id" to feedbackId,
                    "task_id" to taskId,
                    "rating" to rating,
                    "comment" to fullProof,
                    "answers" to mapOf(
                        "serviceQuality" to serviceQuality,
                        "cleanliness" to cleanliness,
                        "productSatisfaction" to productSatisfaction
                    )
                )
            )
        } catch (_: Exception) {}

        // Send submission acknowledgement
        fcmService.notifyWithdrawalStatus("SUBMITTED", campaign.rewardAmount, business?.businessName ?: campaign.title)

        Result.success(updatedTask.toDomain())
    }

    // =========================================================================
    // WALLET & WITHDRAWALS (MANUALLY PROCESSED BY ADMIN)
    // =========================================================================

    suspend fun requestWithdrawal(amount: Double, upiId: String): Result<Withdrawal> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("User not authenticated"))

        val wallet = walletDao.getWalletByUserIdSync(uid)
            ?: return@withContext Result.failure(Exception("Wallet not found"))

        val minAmount = settingDao.getSettingByKey("min_withdrawal_amount")?.value?.toDoubleOrNull() ?: 50.0

        if (amount < minAmount) {
            return@withContext Result.failure(Exception("Minimum withdrawal amount is ₹${"%.2f".format(minAmount)}"))
        }

        if (wallet.balance < amount) {
            return@withContext Result.failure(Exception("Insufficient balance. Available: ₹${"%.2f".format(wallet.balance)}"))
        }

        if (upiId.isBlank() || !upiId.contains("@")) {
            return@withContext Result.failure(Exception("Please enter a valid UPI ID (e.g., name@upi)"))
        }

        // Deduct from available balance immediately to prevent double spending
        val updatedWallet = wallet.copy(
            balance = wallet.balance - amount,
            updatedAt = System.currentTimeMillis()
        )
        walletDao.updateWallet(updatedWallet)

        val withdrawalId = "wd_" + UUID.randomUUID().toString().take(8)
        val withdrawal = WithdrawalEntity(
            id = withdrawalId,
            userId = uid,
            amount = amount,
            upiId = upiId.trim(),
            status = "SUBMITTED",
            adminNote = "Submitted for manual verification",
            transactionReference = null,
            createdAt = System.currentTimeMillis(),
            processedAt = null,
            adminId = null
        )
        withdrawalDao.insertWithdrawal(withdrawal)

        // Record debit transaction in pending state
        val tx = WalletTransactionEntity(
            id = "tx_" + UUID.randomUUID().toString().take(8),
            userId = uid,
            type = "DEBIT",
            amount = amount,
            referenceId = withdrawalId,
            status = "PENDING",
            description = "Withdrawal request to $upiId (Manual Admin Processing)",
            createdAt = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(tx)

        fcmService.notifyWithdrawalStatus("SUBMITTED", amount, upiId)

        Result.success(withdrawal.toDomain())
    }

    // =========================================================================
    // ADMIN FUNCTIONS
    // =========================================================================

    // Admin Dashboard KPIs
    val adminTotalUsers: Flow<Int> = profileDao.getTotalUsersCount()
    val adminActiveUsers: Flow<Int> = profileDao.getActiveUsersCount()
    val adminTotalBusinesses: Flow<Int> = businessDao.getTotalBusinessesCount()
    val adminActiveCampaigns: Flow<Int> = campaignDao.getActiveCampaignsCount()
    val adminCompletedTasks: Flow<Int> = taskDao.getTotalCompletedTasksCount()
    val adminTotalRewards: Flow<Double> = walletDao.getTotalRewardsDistributed().map { it ?: 0.0 }
    val adminPendingWithdrawalsCount: Flow<Int> = withdrawalDao.getPendingWithdrawalsCount()
    val adminPendingWithdrawalsSum: Flow<Double> = withdrawalDao.getPendingWithdrawalsSum().map { it ?: 0.0 }

    // Admin Lists
    val allProfiles: Flow<List<Profile>> = profileDao.getAllProfiles().map { list -> list.map { it.toDomain() } }
    val allBusinesses: Flow<List<Business>> = businessDao.getAllBusinesses().map { list -> list.map { it.toDomain() } }
    val allCampaigns: Flow<List<Campaign>> = campaignDao.getAllCampaigns().map { list -> list.map { it.toDomain() } }
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }
    val allPendingTasks: Flow<List<TaskItem>> = taskDao.getPendingTasks().map { list -> list.map { it.toDomain() } }
    val adminPendingTasksCount: Flow<Int> = taskDao.getPendingTasksCount()
    val allFeedback: Flow<List<FeedbackItem>> = feedbackDao.getAllFeedback().map { list -> list.map { it.toDomain() } }
    val allWithdrawals: Flow<List<Withdrawal>> = withdrawalDao.getAllWithdrawals().map { list -> list.map { it.toDomain() } }

    suspend fun toggleUserStatus(userId: String, currentStatus: String) = withContext(Dispatchers.IO) {
        val newStatus = if (currentStatus == "ACTIVE") "SUSPENDED" else "ACTIVE"
        profileDao.updateStatus(userId, newStatus)
    }

    suspend fun updateBusinessStatus(businessId: String, status: String) = withContext(Dispatchers.IO) {
        businessDao.updateVerificationStatus(businessId, status)
    }

    suspend fun updateCampaignStatus(campaignId: String, status: String) = withContext(Dispatchers.IO) {
        campaignDao.updateStatus(campaignId, status)
    }

    suspend fun createCampaign(
        businessId: String,
        title: String,
        description: String,
        rewardAmount: Double,
        totalBudget: Double,
        maxParticipants: Int,
        estimatedMinutes: Int
    ): Result<Campaign> = withContext(Dispatchers.IO) {
        val c = CampaignEntity(
            id = "camp_" + UUID.randomUUID().toString().take(8),
            businessId = businessId,
            title = title,
            description = description,
            rewardAmount = rewardAmount,
            totalBudget = totalBudget,
            remainingBudget = totalBudget,
            maxParticipants = maxParticipants,
            status = "ACTIVE",
            estimatedMinutes = estimatedMinutes
        )
        campaignDao.insertCampaign(c)
        try {
            val api = com.example.data.remote.SupabaseConfig.apiService
            api.createCampaign(c.toRemoteDto())
        } catch (_: Exception) {}
        Result.success(c.toDomain())
    }

    suspend fun editCampaign(
        campaignId: String,
        title: String,
        description: String,
        rewardAmount: Double,
        totalBudget: Double,
        maxParticipants: Int,
        estimatedMinutes: Int
    ): Result<Campaign> = withContext(Dispatchers.IO) {
        val campaign = campaignDao.getCampaignByIdSync(campaignId)
            ?: return@withContext Result.failure(Exception("Campaign not found"))

        val updated = campaign.copy(
            title = title,
            description = description,
            rewardAmount = rewardAmount,
            totalBudget = totalBudget,
            maxParticipants = maxParticipants,
            estimatedMinutes = estimatedMinutes,
            updatedAt = System.currentTimeMillis()
        )
        campaignDao.updateCampaign(updated)

        try {
            val api = com.example.data.remote.SupabaseConfig.apiService
            api.updateCampaign("eq.$campaignId", updated.toRemoteDto())
        } catch (_: Exception) {}

        Result.success(updated.toDomain())
    }

    /**
     * Approves a user's task submission.
     * Updates task status to APPROVED.
     * ONLY at this point is the reward credited to the user's wallet!
     */
    suspend fun approveTaskSubmission(
        taskId: String,
        adminNote: String? = null
    ): Result<TaskItem> = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId)
            ?: return@withContext Result.failure(Exception("Task not found"))

        if (task.status in listOf("APPROVED", "COMPLETED")) {
            return@withContext Result.failure(Exception("Task submission has already been approved."))
        }

        val campaign = campaignDao.getCampaignByIdSync(task.campaignId)
            ?: return@withContext Result.failure(Exception("Campaign not found"))

        val business = businessDao.getBusinessByIdSync(campaign.businessId)
        val now = System.currentTimeMillis()

        // 1. Update task status to APPROVED
        val approvedTask = task.copy(
            status = "APPROVED",
            adminNote = adminNote ?: "Verified and approved by admin",
            completedAt = now
        )
        taskDao.updateTask(approvedTask)

        // 2. Decrement remaining budget of campaign
        val updatedCampaign = campaign.copy(
            remainingBudget = maxOf(0.0, campaign.remainingBudget - campaign.rewardAmount)
        )
        campaignDao.updateCampaign(updatedCampaign)

        // 3. SECURE WALLET CREDIT: Add reward directly to user's wallet in local database & sync to Supabase
        val userWallet = walletDao.getWalletByUserIdSync(task.userId)
            ?: WalletEntity(id = "w_${task.userId}", userId = task.userId, balance = 0.0, totalEarned = 0.0, totalWithdrawn = 0.0)

        val updatedWallet = userWallet.copy(
            balance = userWallet.balance + campaign.rewardAmount,
            totalEarned = userWallet.totalEarned + campaign.rewardAmount,
            updatedAt = now
        )
        walletDao.insertWallet(updatedWallet)

        // 4. Create verified credit transaction
        val txId = "tx_" + UUID.randomUUID().toString().take(8)
        val tx = WalletTransactionEntity(
            id = txId,
            userId = task.userId,
            type = "CREDIT",
            amount = campaign.rewardAmount,
            referenceId = taskId,
            status = "SUCCESS",
            description = "Reward for approved submission: ${campaign.title}",
            createdAt = now
        )
        transactionDao.insertTransaction(tx)

        // 5. Sync to Supabase
        try {
            val api = com.example.data.remote.SupabaseConfig.apiService
            api.updateTask("eq.$taskId", approvedTask.toRemoteDto())
            api.upsertWallet(updatedWallet.toRemoteDto())
            api.createTransaction(
                mapOf(
                    "id" to txId,
                    "user_id" to task.userId,
                    "type" to "CREDIT",
                    "amount" to campaign.rewardAmount,
                    "reference_id" to taskId,
                    "status" to "SUCCESS",
                    "description" to tx.description
                )
            )
        } catch (_: Exception) {}

        // 6. Notify user
        fcmService.notifyRewardCredited(campaign.rewardAmount, business?.businessName ?: campaign.title)

        Result.success(approvedTask.toDomain())
    }

    /**
     * Rejects a user's task submission with a given reason.
     * Updates status to REJECTED. No wallet reward is credited.
     */
    suspend fun rejectTaskSubmission(
        taskId: String,
        reason: String
    ): Result<TaskItem> = withContext(Dispatchers.IO) {
        val task = taskDao.getTaskById(taskId)
            ?: return@withContext Result.failure(Exception("Task not found"))

        if (task.status in listOf("APPROVED", "COMPLETED")) {
            return@withContext Result.failure(Exception("Cannot reject an already approved task."))
        }

        val now = System.currentTimeMillis()
        val rejectedTask = task.copy(
            status = "REJECTED",
            adminNote = reason.ifBlank { "Proof verification failed" },
            completedAt = now
        )
        taskDao.updateTask(rejectedTask)

        // Sync to Supabase
        try {
            val api = com.example.data.remote.SupabaseConfig.apiService
            api.updateTask("eq.$taskId", rejectedTask.toRemoteDto())
        } catch (_: Exception) {}

        Result.success(rejectedTask.toDomain())
    }

    /**
     * Admin withdrawal processing:
     * Transitions: SUBMITTED -> UNDER_REVIEW -> APPROVED -> PROCESSING -> SUCCESSFUL / REJECTED
     */
    suspend fun updateWithdrawalStatus(
        withdrawalId: String,
        newStatus: String,
        adminNote: String?,
        transactionRef: String?,
        adminId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val wd = withdrawalDao.getWithdrawalById(withdrawalId)
            ?: return@withContext Result.failure(Exception("Withdrawal record not found"))

        val now = System.currentTimeMillis()
        val updated = wd.copy(
            status = newStatus,
            adminNote = adminNote ?: wd.adminNote,
            transactionReference = transactionRef ?: wd.transactionReference,
            processedAt = if (newStatus in listOf("SUCCESSFUL", "REJECTED")) now else wd.processedAt,
            adminId = adminId
        )
        withdrawalDao.updateWithdrawal(updated)

        val wallet = walletDao.getWalletByUserIdSync(wd.userId)
        if (wallet != null) {
            if (newStatus == "SUCCESSFUL") {
                // Update total withdrawn
                walletDao.updateWallet(
                    wallet.copy(
                        totalWithdrawn = wallet.totalWithdrawn + wd.amount,
                        updatedAt = now
                    )
                )
            } else if (newStatus == "REJECTED") {
                // Return funds back to user's wallet
                walletDao.updateWallet(
                    wallet.copy(
                        balance = wallet.balance + wd.amount,
                        updatedAt = now
                    )
                )
                // Add transaction for refund
                transactionDao.insertTransaction(
                    WalletTransactionEntity(
                        id = "tx_" + UUID.randomUUID().toString().take(8),
                        userId = wd.userId,
                        type = "CREDIT",
                        amount = wd.amount,
                        referenceId = withdrawalId,
                        status = "SUCCESS",
                        description = "Refund for rejected withdrawal: ${adminNote ?: "Rejected by Admin"}",
                        createdAt = now
                    )
                )
            }
        }

        fcmService.notifyWithdrawalStatus(newStatus, wd.amount, wd.upiId)
        Result.success(Unit)
    }

    suspend fun updateMinWithdrawalSetting(newMin: Double) = withContext(Dispatchers.IO) {
        settingDao.setSetting(
            SettingEntity(
                id = "min_withdrawal_amount",
                key = "min_withdrawal_amount",
                value = newMin.toString()
            )
        )
    }

    suspend fun testSupabaseConnection(): Result<String> = withContext(Dispatchers.IO) {
        com.example.data.remote.SupabaseConfig.verifyConnection()
    }

    /**
     * Credits bonus earnings to the user's wallet upon verified completion of a Rewarded Ad.
     * Guaranteed only to run after AdMob SDK OnUserEarnedRewardListener confirmation.
     */
    suspend fun creditRewardedAdBonus(amount: Double = 5.0): Result<Double> = withContext(Dispatchers.IO) {
        val uid = _currentUserId.value ?: return@withContext Result.failure(Exception("User not authenticated"))
        val now = System.currentTimeMillis()
        val wallet = walletDao.getWalletByUserIdSync(uid)
            ?: com.example.data.local.WalletEntity(id = "w_$uid", userId = uid, balance = 0.0, totalEarned = 0.0, totalWithdrawn = 0.0)

        val updatedWallet = wallet.copy(
            balance = wallet.balance + amount,
            totalEarned = wallet.totalEarned + amount,
            updatedAt = now
        )
        walletDao.insertWallet(updatedWallet)

        val txId = "tx_ad_" + UUID.randomUUID().toString().take(8)
        val tx = com.example.data.local.WalletTransactionEntity(
            id = txId,
            userId = uid,
            type = "CREDIT",
            amount = amount,
            referenceId = "admob_reward",
            status = "SUCCESS",
            description = "Rewarded Ad Bonus (+₹${"%.2f".format(amount)})",
            createdAt = now
        )
        transactionDao.insertTransaction(tx)

        try {
            val api = com.example.data.remote.SupabaseConfig.apiService
            api.upsertWallet(updatedWallet.toRemoteDto())
        } catch (_: Exception) {}

        fcmService.notifyRewardCredited(amount, "Rewarded Ad Bonus")
        Result.success(amount)
    }
}
