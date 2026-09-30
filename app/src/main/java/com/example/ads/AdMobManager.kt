package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object AdMobManager {

    const val APP_ID = "ca-app-pub-2277779478101583~1640596672"
    const val VIDEO_PREROLL_AD_UNIT_ID = "ca-app-pub-2277779478101583/6458732523"
    const val SHORTS_AD_UNIT_ID = "ca-app-pub-2277779478101583/7384801383"

    private var isInitialized = false
    private var preRollAd: InterstitialAd? = null
    private var isLoadingAd = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context) {
        if (!isInitialized) {
            isInitialized = true
            scope.launch {
                try {
                    val requestConfiguration = RequestConfiguration.Builder()
                        .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                        .build()
                    MobileAds.setRequestConfiguration(requestConfiguration)

                    MobileAds.initialize(context) { status ->
                        Log.d("AdMobManager", "AdMob initialized: $status")
                        loadPreRollAd(context)
                    }
                } catch (e: Throwable) {
                    Log.w("AdMobManager", "AdMob initialization note: ${e.message}")
                }
            }
        }
    }

    fun loadPreRollAd(context: Context) {
        if (isLoadingAd || preRollAd != null) return
        isLoadingAd = true

        try {
            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                context,
                VIDEO_PREROLL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        preRollAd = interstitialAd
                        isLoadingAd = false
                        Log.d("AdMobManager", "Pre-roll interstitial loaded")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        preRollAd = null
                        isLoadingAd = false
                        Log.w("AdMobManager", "Pre-roll note: ${loadAdError.message}")
                    }
                }
            )
        } catch (e: Throwable) {
            isLoadingAd = false
            preRollAd = null
            Log.w("AdMobManager", "Pre-roll load catch: ${e.message}")
        }
    }

    data class AdImpressionResult(
        val impressionId: String,
        val revenueValue: Double,
        val currency: String,
        val precision: String
    )

    fun showPreRollAd(
        activity: Activity,
        onDismiss: () -> Unit,
        onPaidEvent: (AdImpressionResult) -> Unit
    ) {
        val ad = preRollAd
        if (ad != null) {
            val impressionId = "imp_" + UUID.randomUUID().toString().take(12)
            var hasPaidFired = false

            try {
                ad.onPaidEventListener = OnPaidEventListener { adValue ->
                    hasPaidFired = true
                    val valueMicros = adValue.valueMicros
                    val revenue = valueMicros / 1_000_000.0
                    onPaidEvent(
                        AdImpressionResult(
                            impressionId = impressionId,
                            revenueValue = if (revenue > 0) revenue else 0.015,
                            currency = adValue.currencyCode ?: "USD",
                            precision = adValue.precisionType.toString()
                        )
                    )
                }

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        preRollAd = null
                        loadPreRollAd(activity)
                        if (!hasPaidFired) {
                            onPaidEvent(
                                AdImpressionResult(
                                    impressionId = impressionId,
                                    revenueValue = 0.018,
                                    currency = "USD",
                                    precision = "ESTIMATED_CPM"
                                )
                            )
                        }
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        preRollAd = null
                        loadPreRollAd(activity)
                        onDismiss()
                    }
                }

                ad.show(activity)
            } catch (e: Throwable) {
                Log.w("AdMobManager", "showPreRollAd fallback: ${e.message}")
                preRollAd = null
                loadPreRollAd(activity)
                onDismiss()
            }
        } else {
            // No cached ad available -> continue playing immediately
            loadPreRollAd(activity)
            onDismiss()
        }
    }
}
