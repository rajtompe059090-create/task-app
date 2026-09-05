package com.example.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Firebase Cloud Messaging architecture prepared for push notifications.
 * Handles device token registration, campaign alerts, task rewards, and withdrawal updates.
 */
class FcmNotificationService(private val context: Context) {

    private val _fcmToken = MutableStateFlow<String?>("fcm_token_test_mock_device_3847")
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationPayload>>(emptyList())
    val notifications: StateFlow<List<NotificationPayload>> = _notifications.asStateFlow()

    data class NotificationPayload(
        val id: String = java.util.UUID.randomUUID().toString(),
        val title: String,
        val body: String,
        val type: String, // "TASK_REWARD", "WITHDRAWAL_STATUS", "NEW_CAMPAIGN", "SYSTEM"
        val timestamp: Long = System.currentTimeMillis()
    )

    fun registerDeviceToken(userId: String) {
        Log.d("FCM", "Registering FCM device token for user $userId: ${_fcmToken.value}")
        // In production with Firebase Messaging SDK:
        // FirebaseMessaging.getInstance().token.addOnCompleteListener { ... }
    }

    fun dispatchLocalNotification(title: String, body: String, type: String) {
        val payload = NotificationPayload(
            title = title,
            body = body,
            type = type
        )
        _notifications.value = listOf(payload) + _notifications.value
        Log.i("FCM", "Dispatched notification: $title - $body")
    }

    fun notifyRewardCredited(amount: Double, businessName: String) {
        dispatchLocalNotification(
            title = "Reward Credited!",
            body = "₹${"%.2f".format(amount)} has been added to your ReviewTask wallet for your genuine feedback on $businessName.",
            type = "TASK_REWARD"
        )
    }

    fun notifyWithdrawalStatus(status: String, amount: Double, upiId: String) {
        dispatchLocalNotification(
            title = "Withdrawal Update: $status",
            body = "Your withdrawal request of ₹${"%.2f".format(amount)} to UPI $upiId is now $status.",
            type = "WITHDRAWAL_STATUS"
        )
    }
}
