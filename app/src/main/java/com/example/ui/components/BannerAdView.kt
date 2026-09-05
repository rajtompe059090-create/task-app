package com.example.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdMobConfig
import com.example.ads.AdMobManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Reusable Google AdMob Banner Ad component for Jetpack Compose.
 *
 * Features:
 * - Clean Material 3 design with clear "Sponsored / Advertisement" labeling.
 * - Non-blocking: gracefully collapses on load failure without breaking layouts or blocking tasks.
 * - Handles Android view lifecycle (resume, pause, destroy) safely.
 * - Configurable adUnitId and adSize.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.bannerAdUnitId,
    adSize: AdSize = AdSize.BANNER,
    showLabel: Boolean = true
) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current

    // In Compose preview / inspection mode, show a clean preview placeholder
    if (isInspectionMode) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("AdMob Banner Preview", style = MaterialTheme.typography.labelMedium)
            }
        }
        return
    }

    var isAdLoaded by remember { mutableStateOf(false) }
    var hasAdFailed by remember { mutableStateOf(false) }

    // If ad failed, do not occupy unnecessary space or block user interactions
    if (hasAdFailed) {
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("banner_ad_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showLabel && isAdLoaded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SPONSORED",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(adSize)
                        setAdUnitId(adUnitId)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("BannerAdView", "Banner ad loaded successfully.")
                                isAdLoaded = true
                                hasAdFailed = false
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.w("BannerAdView", "Banner ad failed to load: ${error.message} (code: ${error.code})")
                                isAdLoaded = false
                                hasAdFailed = true
                            }
                        }
                        loadAd(AdMobManager.createAdRequest())
                    }
                },
                update = { adView ->
                    // View update if needed
                },
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}
