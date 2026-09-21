package com.websiteblocker.app.ui.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.websiteblocker.app.BuildConfig
import com.websiteblocker.app.BlockerApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AnchoredAdaptiveBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
) {
    val context = LocalContext.current
    val privacy by (context.applicationContext as BlockerApplication).adsConsent.state.collectAsStateWithLifecycle()
    if (!privacy.canShowAds) return
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthDp = configuration.screenWidthDp
    val adSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    val adHeightDp = with(density) { adSize.getHeightInPixels(context).toDp() }

    var isAdLoaded by remember { mutableStateOf(false) }

    // Reserve space to prevent layout shifting (CLS)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeightDp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    setAdUnitId(adUnitId)
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                if (adView.adSize != adSize) {
                    adView.setAdSize(adSize)
                    adView.loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { adView ->
                adView.destroy()
            },
        )
    }
}
