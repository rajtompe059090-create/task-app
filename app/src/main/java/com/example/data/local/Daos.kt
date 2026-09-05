package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileByIdSync(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE phone = :phone LIMIT 1")
    suspend fun getProfileByPhone(phone: String): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM profiles")
    fun getTotalUsersCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM profiles WHERE status = 'ACTIVE'")
    fun getActiveUsersCount(): Flow<Int>
}

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun getBusinessById(id: String): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessByIdSync(id: String): BusinessEntity?

    @Query("SELECT * FROM businesses ORDER BY createdAt DESC")
    fun getAllBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE verificationStatus = 'APPROVED' ORDER BY businessName ASC")
    fun getApprovedBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE verificationStatus = 'PENDING'")
    fun getPendingBusinesses(): Flow<List<BusinessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBusinesses(businesses: List<BusinessEntity>)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("UPDATE businesses SET verificationStatus = :status WHERE id = :id")
    suspend fun updateVerificationStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM businesses")
    fun getTotalBusinessesCount(): Flow<Int>
}

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    fun getCampaignById(id: String): Flow<CampaignEntity?>

    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    suspend fun getCampaignByIdSync(id: String): CampaignEntity?

    @Query("SELECT * FROM campaigns ORDER BY createdAt DESC")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE status = 'ACTIVE' ORDER BY rewardAmount DESC")
    fun getActiveCampaigns(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCampaigns(campaigns: List<CampaignEntity>)

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)

    @Query("UPDATE campaigns SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE campaigns SET title = :title, description = :description, rewardAmount = :rewardAmount, totalBudget = :totalBudget, maxParticipants = :maxParticipants, estimatedMinutes = :estimatedMinutes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCampaignDetails(
        id: String,
        title: String,
        description: String,
        rewardAmount: Double,
        totalBudget: Double,
        maxParticipants: Int,
        estimatedMinutes: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM campaigns WHERE status = 'ACTIVE'")
    fun getActiveCampaignsCount(): Flow<Int>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTasksByUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE userId = :userId AND campaignId = :campaignId LIMIT 1")
    suspend fun getTaskByUserAndCampaign(userId: String, campaignId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE userId = :userId AND campaignId = :campaignId LIMIT 1")
    fun observeTaskByUserAndCampaign(userId: String, campaignId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND status IN ('APPROVED', 'COMPLETED') AND completedAt >= :startOfDayTimestamp")
    fun getTodayCompletedTasks(userId: String, startOfDayTimestamp: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'PENDING'")
    fun getPendingTasksCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("SELECT COUNT(*) FROM tasks WHERE status IN ('APPROVED', 'COMPLETED')")
    fun getTotalCompletedTasksCount(): Flow<Int>
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedback WHERE taskId = :taskId LIMIT 1")
    fun getFeedbackByTaskId(taskId: String): Flow<FeedbackEntity?>

    @Query("SELECT * FROM feedback WHERE taskId = :taskId LIMIT 1")
    suspend fun getFeedbackByTaskIdSync(taskId: String): FeedbackEntity?

    @Query("SELECT * FROM feedback ORDER BY createdAt DESC")
    fun getAllFeedback(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    fun getWalletByUserId(userId: String): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE userId = :userId LIMIT 1")
    suspend fun getWalletByUserIdSync(userId: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("SELECT SUM(totalEarned) FROM wallets")
    fun getTotalRewardsDistributed(): Flow<Double?>
}

@Dao
interface WalletTransactionDao {
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTransactionsByUserId(userId: String): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WalletTransactionEntity)
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getWithdrawalsByUserId(userId: String): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals ORDER BY createdAt DESC")
    fun getAllWithdrawals(): Flow<List<WithdrawalEntity>>

    @Query("SELECT * FROM withdrawals WHERE id = :id LIMIT 1")
    suspend fun getWithdrawalById(id: String): WithdrawalEntity?

    @Query("SELECT * FROM withdrawals WHERE userId = :userId AND status IN ('SUBMITTED', 'UNDER_REVIEW', 'PROCESSING')")
    fun getPendingWithdrawalsByUserId(userId: String): Flow<List<WithdrawalEntity>>

    @Query("SELECT COUNT(*) FROM withdrawals WHERE status IN ('SUBMITTED', 'UNDER_REVIEW', 'PROCESSING')")
    fun getPendingWithdrawalsCount(): Flow<Int>

    @Query("SELECT SUM(amount) FROM withdrawals WHERE status IN ('SUBMITTED', 'UNDER_REVIEW', 'PROCESSING')")
    fun getPendingWithdrawalsSum(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity)

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalEntity)
}

@Dao
interface ReferralDao {
    @Query("SELECT * FROM referrals WHERE referrerId = :referrerId ORDER BY createdAt DESC")
    fun getReferralsByReferrer(referrerId: String): Flow<List<ReferralEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: ReferralEntity)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): SettingEntity?

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    fun observeSettingByKey(key: String): Flow<SettingEntity?>

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)
}
