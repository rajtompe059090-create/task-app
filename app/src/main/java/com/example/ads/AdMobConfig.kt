package com.example.ads

import com.example.BuildConfig

/**
 * Configuration and resolution of Google AdMob App ID and Ad Unit IDs.
 *
 * Defaults to the official Google AdMob test ad unit IDs for safe testing.
 * When deploying to production, real IDs can be configured via AI Studio Secrets (.env)
 * or system environment variables without changing any application code.
 */
object AdMobConfig {

    // Official Google AdMob Sample Test IDs (Android)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Frequency capping: minimum interval between interstitial ads to protect user experience
    var interstitialCooldownMillis: Long = 60_000L // 60 seconds minimum cooldown

    // User reward amount for watching rewarded video ads
    var rewardedBonusAmount: Double = 5.0

    private fun resolveConfig(buildConfigValue: String, envKey: String, fallback: String): String {
        val bc = buildConfigValue.trim()
        if (bc.isNotBlank() && !bc.contains("ca-app-pub-3940256099942544") && !bc.contains("placeholder")) {
            return bc
        }
        val env = System.getenv(envKey)?.trim()
        if (!env.isNullOrBlank() && !env.contains("placeholder")) {
            return env
        }
        return if (bc.isNotBlank()) bc else fallback
    }

    val appId: String
        get() = resolveConfig(BuildConfig.ADMOB_APP_ID, "ADMOB_APP_ID", TEST_APP_ID)

    val bannerAdUnitId: String
        get() = resolveConfig(BuildConfig.ADMOB_BANNER_AD_UNIT_ID, "ADMOB_BANNER_AD_UNIT_ID", TEST_BANNER_AD_UNIT_ID)

    val interstitialAdUnitId: String
        get() = resolveConfig(BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID, "ADMOB_INTERSTITIAL_AD_UNIT_ID", TEST_INTERSTITIAL_AD_UNIT_ID)

    val rewardedAdUnitId: String
        get() = resolveConfig(BuildConfig.ADMOB_REWARDED_AD_UNIT_ID, "ADMOB_REWARDED_AD_UNIT_ID", TEST_REWARDED_AD_UNIT_ID)

    val isTestMode: Boolean
        get() = bannerAdUnitId == TEST_BANNER_AD_UNIT_ID ||
                interstitialAdUnitId == TEST_INTERSTITIAL_AD_UNIT_ID ||
                rewardedAdUnitId == TEST_REWARDED_AD_UNIT_ID
}
