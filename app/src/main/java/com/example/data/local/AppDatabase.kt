package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        ProfileEntity::class,
        BusinessEntity::class,
        CampaignEntity::class,
        TaskEntity::class,
        FeedbackEntity::class,
        WalletEntity::class,
        WalletTransactionEntity::class,
        WithdrawalEntity::class,
        ReferralEntity::class,
        SettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun businessDao(): BusinessDao
    abstract fun campaignDao(): CampaignDao
    abstract fun taskDao(): TaskDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun walletDao(): WalletDao
    abstract fun walletTransactionDao(): WalletTransactionDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun referralDao(): ReferralDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reviewtask_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Prepopulate seed data in background
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedDatabase(database)
                }
            }
        }

        private suspend fun seedDatabase(db: AppDatabase) {
            val adminId = "admin_001"
            val adminProfile = ProfileEntity(
                id = adminId,
                name = "Admin Officer",
                phone = "9999999999",
                whatsapp = "9999999999",
                upiId = "reviewtask.admin@upi",
                role = "ADMIN",
                status = "ACTIVE",
                referralCode = "ADMIN99",
                createdAt = System.currentTimeMillis()
            )
            db.profileDao().insertProfile(adminProfile)
            db.walletDao().insertWallet(WalletEntity(id = "w_$adminId", userId = adminId, balance = 0.0, totalEarned = 0.0, totalWithdrawn = 0.0))

            // Seed Businesses
            val b1 = BusinessEntity(
                id = "biz_001",
                ownerId = adminId,
                businessName = "Green Spoon Bistro & Cafe",
                category = "Food & Beverages",
                address = "104 Indiranagar 100ft Rd, Bangalore",
                googleMapsUrl = "https://maps.google.com/?q=Green+Spoon+Bistro+Bangalore",
                verificationStatus = "APPROVED"
            )
            val b2 = BusinessEntity(
                id = "biz_002",
                ownerId = adminId,
                businessName = "Apex Pulse Fitness Studio",
                category = "Health & Gym",
                address = "220 Koramangala 5th Block, Bangalore",
                googleMapsUrl = "https://maps.google.com/?q=Apex+Pulse+Fitness+Bangalore",
                verificationStatus = "APPROVED"
            )
            val b3 = BusinessEntity(
                id = "biz_003",
                ownerId = adminId,
                businessName = "Urban Glow Unisex Salon",
                category = "Beauty & Wellness",
                address = "15 Jayanagar 4th Block, Bangalore",
                googleMapsUrl = "https://maps.google.com/?q=Urban+Glow+Salon+Bangalore",
                verificationStatus = "APPROVED"
            )
            val b4 = BusinessEntity(
                id = "biz_004",
                ownerId = adminId,
                businessName = "TechCraft Electronics Care",
                category = "Electronics & Repair",
                address = "88 MG Road, Bangalore",
                googleMapsUrl = "https://maps.google.com/?q=TechCraft+Bangalore",
                verificationStatus = "APPROVED"
            )
            db.businessDao().insertBusinesses(listOf(b1, b2, b3, b4))

            // Seed Campaigns
            val c1 = CampaignEntity(
                id = "camp_001",
                businessId = "biz_001",
                title = "Dining Experience & Food Quality",
                description = "Visit or order from Green Spoon. Share honest customer feedback regarding food freshness, hygiene, service speed, and dining atmosphere.",
                rewardAmount = 25.0,
                totalBudget = 2500.0,
                remainingBudget = 2475.0,
                maxParticipants = 100,
                status = "ACTIVE",
                estimatedMinutes = 3
            )
            val c2 = CampaignEntity(
                id = "camp_002",
                businessId = "biz_002",
                title = "Gym Facility & Hygiene Audit",
                description = "Evaluate workout floor cleanliness, equipment maintenance, locker sanitization, and staff responsiveness during peak hours.",
                rewardAmount = 35.0,
                totalBudget = 3500.0,
                remainingBudget = 3465.0,
                maxParticipants = 100,
                status = "ACTIVE",
                estimatedMinutes = 4
            )
            val c3 = CampaignEntity(
                id = "camp_003",
                businessId = "biz_003",
                title = "Styling & Service Satisfaction",
                description = "Provide constructive insights on salon sanitation, stylist consultation quality, appointment wait time, and pricing transparency.",
                rewardAmount = 30.0,
                totalBudget = 3000.0,
                remainingBudget = 2970.0,
                maxParticipants = 100,
                status = "ACTIVE",
                estimatedMinutes = 3
            )
            val c4 = CampaignEntity(
                id = "camp_004",
                businessId = "biz_004",
                title = "Gadget Diagnostic & Repair Transparency",
                description = "Give feedback on device intake diagnosis, technician professionalism, cost estimate clarity, and turnaround punctuality.",
                rewardAmount = 40.0,
                totalBudget = 4000.0,
                remainingBudget = 3960.0,
                maxParticipants = 100,
                status = "ACTIVE",
                estimatedMinutes = 4
            )
            db.campaignDao().insertCampaigns(listOf(c1, c2, c3, c4))

            // Seed Settings
            db.settingDao().setSetting(SettingEntity(id = UUID.randomUUID().toString(), key = "min_withdrawal_amount", value = "50.00"))
            db.settingDao().setSetting(SettingEntity(id = UUID.randomUUID().toString(), key = "referral_bonus_amount", value = "10.00"))
            db.settingDao().setSetting(SettingEntity(id = UUID.randomUUID().toString(), key = "anti_fraud_cooldown_hours", value = "24"))
        }
    }
}
