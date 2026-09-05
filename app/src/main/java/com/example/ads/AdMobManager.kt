package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production-ready AdMob Monetization Architecture.
 *
 * Handles:
 * - Thread-safe MobileAds SDK initialization.
 * - Proactive Interstitial ad preloading with exponential backoff on failure.
 * - Strict frequency capping so interstitial ads never spam the user.
 * - Non-blocking execution: tasks and navigation ALWAYS continue immediately if ads fail or are not ready.
 * - Rewarded Ads: rewards are strictly gated behind OnUserEarnedRewardListener completion.
 */
object AdMobManager {

    private const val TAG = "AdMobManager"

    private val isInitialized = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isAdMobReady = MutableStateFlow(false)
    val isAdMobReady: StateFlow<Boolean> = _isAdMobReady.asStateFlow()

    // Interstitial state
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTimestamp: Long = 0L
    private var interstitialRetryAttempts = 0

    private val _isInterstitialLoaded = MutableStateFlow(false)
    val isInterstitialLoaded: StateFlow<Boolean> = _isInterstitialLoaded.asStateFlow()

    // Rewarded state
    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false
    private var rewardedRetryAttempts = 0

    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded: StateFlow<Boolean> = _isRewardedLoaded.asStateFlow()

    /**
     * Initializes Google Mobile Ads SDK on a background thread.
     */
    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(context) { initializationStatus ->
                    Log.d(TAG, "AdMob SDK Initialized: ${initializationStatus.adapterStatusMap}")
                    _isAdMobReady.value = true

                    // Proactively warm up ad caches
                    scope.launch {
                        loadInterstitialAd(context)
                        loadRewardedAd(context)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "AdMob initialization failed gracefully: ${e.message}", e)
            }
        }
    }

    /**
     * Creates a standard AdRequest.
     */
    fun createAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    // =========================================================================
    // INTERSTITIAL ADS
    // =========================================================================

    /**
     * Preloads an Interstitial Ad with graceful error handling and retry backoff.
     */
    fun loadInterstitialAd(context: Context) {
        if (isInterstitialLoading || interstitialAd != null) return

        isInterstitialLoading = true
        val adUnitId = AdMobConfig.interstitialAdUnitId

        Log.d(TAG, "Requesting Interstitial Ad (Unit: $adUnitId)...")
        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            createAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad successfully loaded.")
                    interstitialAd = ad
                    isInterstitialLoading = false
                    interstitialRetryAttempts = 0
                    _isInterstitialLoaded.value = true
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Interstitial Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    interstitialAd = null
                    isInterstitialLoading = false
                    _isInterstitialLoaded.value = false

                    // Exponential backoff retry up to 3 times (10s, 30s, 60s)
                    if (interstitialRetryAttempts < 3) {
                        interstitialRetryAttempts++
                        val delayMs = interstitialRetryAttempts * 10_000L
                        scope.launch {
                            delay(delayMs)
                            loadInterstitialAd(context)
                        }
                    }
                }
            }
        )
    }

    /**
     * Shows an Interstitial Ad if available and frequency capping permits.
     *
     * GUARANTEE: onDismissed() will ALWAYS be executed without delay if:
     * - The ad is not yet loaded
     * - The cooldown period has not elapsed
     * - The ad fails to display
     * - The user closes the ad
     *
     * The app navigation and task progression are NEVER blocked.
     */
    fun showInterstitialAd(
        activity: Activity,
        onDismissed: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        val timeSinceLastAd = now - lastInterstitialShownTimestamp
        val isCooldownActive = timeSinceLastAd < AdMobConfig.interstitialCooldownMillis

        val ad = interstitialAd

        if (ad == null || isCooldownActive) {
            if (isCooldownActive) {
                Log.d(TAG, "Interstitial skipped due to frequency cap (${timeSinceLastAd / 1000}s / ${AdMobConfig.interstitialCooldownMillis / 1000}s).")
            } else {
                Log.d(TAG, "Interstitial not ready; proceeding smoothly.")
                loadInterstitialAd(activity.applicationContext)
            }
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed by user.")
                interstitialAd = null
                _isInterstitialLoaded.value = false
                lastInterstitialShownTimestamp = System.currentTimeMillis()
                loadInterstitialAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                interstitialAd = null
                _isInterstitialLoaded.value = false
                loadInterstitialAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad is now displaying.")
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying interstitial ad: ${e.message}", e)
            interstitialAd = null
            _isInterstitialLoaded.value = false
            onDismissed()
        }
    }

    // =========================================================================
    // REWARDED ADS
    // =========================================================================

    /**
     * Preloads a Rewarded Ad with backoff retry.
     */
    fun loadRewardedAd(context: Context) {
        if (isRewardedLoading || rewardedAd != null) return

        isRewardedLoading = true
        val adUnitId = AdMobConfig.rewardedAdUnitId

        Log.d(TAG, "Requesting Rewarded Ad (Unit: $adUnitId)...")
        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            createAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad successfully loaded.")
                    rewardedAd = ad
                    isRewardedLoading = false
                    rewardedRetryAttempts = 0
                    _isRewardedLoaded.value = true
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    rewardedAd = null
                    isRewardedLoading = false
                    _isRewardedLoaded.value = false

                    if (rewardedRetryAttempts < 3) {
                        rewardedRetryAttempts++
                        val delayMs = rewardedRetryAttempts * 10_000L
                        scope.launch {
                            delay(delayMs)
                            loadRewardedAd(context)
                        }
                    }
                }
            }
        )
    }

    /**
     * Shows a Rewarded Video Ad.
     *
     * IMPORTANT: Rewards are ONLY granted if [onRewardEarned] is invoked by the
     * AdMob SDK's OnUserEarnedRewardListener. Merely clicking or opening the ad
     * will NOT trigger a reward.
     *
     * Returns true if an ad was shown, false if ad was not ready.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (amount: Int, type: String) -> Unit,
        onDismissed: () -> Unit
    ): Boolean {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready; requesting load.")
            loadRewardedAd(activity.applicationContext)
            onDismissed()
            return false
        }

        var rewardGranted = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad closed. Reward granted = $rewardGranted")
                rewardedAd = null
                _isRewardedLoaded.value = false
                loadRewardedAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _isRewardedLoaded.value = false
                loadRewardedAd(activity.applicationContext)
                onDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad is now displaying.")
            }
        }

        try {
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User completed watching rewarded ad! Reward: ${rewardItem.amount} ${rewardItem.type}")
                rewardGranted = true
                onRewardEarned(rewardItem.amount, rewardItem.type)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying rewarded ad: ${e.message}", e)
            rewardedAd = null
            _isRewardedLoaded.value = false
            onDismissed()
            return false
        }
    }
}
