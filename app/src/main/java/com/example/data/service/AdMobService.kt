package com.example.data.service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.ads.AdMobConfig
import com.example.ads.AdMobManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production-ready AdMob Service interface bridging app components with [AdMobManager].
 * Automatically initializes AdMob SDK and manages ad display cycles.
 */
class AdMobService(private val context: Context) {

    companion object {
        val TEST_BANNER_AD_UNIT_ID = AdMobConfig.TEST_BANNER_AD_UNIT_ID
        val TEST_INTERSTITIAL_AD_UNIT_ID = AdMobConfig.TEST_INTERSTITIAL_AD_UNIT_ID
        val TEST_REWARDED_AD_UNIT_ID = AdMobConfig.TEST_REWARDED_AD_UNIT_ID
    }

    private val _isAdsEnabled = MutableStateFlow(true)
    val isAdsEnabled: StateFlow<Boolean> = _isAdsEnabled.asStateFlow()

    private val _adStatusMessage = MutableStateFlow(
        if (AdMobConfig.isTestMode) "AdMob Active (Google Test Ad Units)" else "AdMob Active (Production Mode)"
    )
    val adStatusMessage: StateFlow<String> = _adStatusMessage.asStateFlow()

    val isInterstitialLoaded: StateFlow<Boolean> = AdMobManager.isInterstitialLoaded
    val isRewardedLoaded: StateFlow<Boolean> = AdMobManager.isRewardedLoaded
    val isAdMobReady: StateFlow<Boolean> = AdMobManager.isAdMobReady

    init {
        // Initialize AdMob SDK and begin preloading
        AdMobManager.initialize(context)
    }

    fun toggleAds(enabled: Boolean) {
        _isAdsEnabled.value = enabled
        _adStatusMessage.value = if (enabled) {
            "AdMob Ads Enabled (Active)"
        } else {
            "AdMob Ads Temporarily Paused"
        }
        Log.d("AdMobService", "AdMob enabled state: $enabled")
    }

    /**
     * Displays an interstitial ad at natural transition points with frequency capping.
     * Always invokes [onDismissed] without delay if the ad is unavailable or cooling down.
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        if (!_isAdsEnabled.value) {
            onDismissed()
            return
        }
        AdMobManager.showInterstitialAd(activity, onDismissed)
    }

    /**
     * Displays a rewarded video ad.
     * [onRewardEarned] is strictly called ONLY when the user fully completes watching the ad.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onDismissed: () -> Unit
    ): Boolean {
        if (!_isAdsEnabled.value) {
            onDismissed()
            return false
        }
        return AdMobManager.showRewardedAd(activity, onRewardEarned, onDismissed)
    }

    /**
     * Backward-compatible test helper.
     */
    fun showTestRewardedAd(onRewarded: () -> Unit) {
        Log.d("AdMobService", "Triggered test rewarded ad callback.")
        onRewarded()
    }
}
