package app.downify

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Test IDs — gerçek ID'lerle değiştirilecek
private const val ADMOB_INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"
private const val ADMOB_BANNER_TEST       = "ca-app-pub-3940256099942544/6300978111"

class AdManager private constructor() {

    var isInitialized by mutableStateOf(false)
        private set

    var consentObtained by mutableStateOf(false)
        private set

    private var interstitialAd: InterstitialAd? = null
    private var downloadCount = 0

    companion object {
        val shared = AdManager()
        const val BANNER_AD_UNIT_ID  = ADMOB_BANNER_TEST
        const val INTERSTITIAL_AD_UNIT_ID = ADMOB_INTERSTITIAL_TEST
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        CoroutineScope(Dispatchers.Main).launch {
            MobileAds.initialize(context) {
                isInitialized = true
                Log.d("AdManager", "MobileAds initialized")
            }
        }
    }

    fun requestUmpConsent(activity: Activity, onComplete: () -> Unit = {}) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(activity, params,
            {
                if (consentInfo.isConsentFormAvailable) {
                    loadConsentForm(activity, consentInfo, onComplete)
                } else {
                    consentObtained = true
                    onComplete()
                }
            },
            { error ->
                Log.w("AdManager", "UMP consent info update failed: ${error.message}")
                consentObtained = true
                onComplete()
            }
        )
    }

    private fun loadConsentForm(
        activity: Activity,
        consentInfo: ConsentInformation,
        onComplete: () -> Unit
    ) {
        UserMessagingPlatform.loadConsentForm(activity,
            { form ->
                if (consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    form.show(activity) {
                        consentObtained = true
                        onComplete()
                    }
                } else {
                    consentObtained = true
                    onComplete()
                }
            },
            { error ->
                Log.w("AdManager", "Consent form load failed: ${error.message}")
                consentObtained = true
                onComplete()
            }
        )
    }

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null) return
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("AdManager", "Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.w("AdManager", "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    fun onDownloadCompleted(activity: Activity, context: Context) {
        downloadCount++
        if (downloadCount % 3 == 0) {
            showInterstitial(activity, context)
        }
    }

    private fun showInterstitial(activity: Activity, context: Context) {
        val ad = interstitialAd
        if (ad != null) {
            ad.show(activity)
            interstitialAd = null
            loadInterstitial(context)
        } else {
            loadInterstitial(context)
        }
    }
}
