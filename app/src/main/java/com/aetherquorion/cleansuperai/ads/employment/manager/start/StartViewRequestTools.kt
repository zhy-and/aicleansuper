package com.aetherquorion.cleansuperai.ads.employment.manager.start

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import com.aetherquorion.cleansuperai.CleanSuperAiApp
import com.aetherquorion.cleansuperai.ads.TransLateLoadedLis
import com.aetherquorion.cleansuperai.ads.TranslateKepShowStatusLis
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil.interspaceStudyStatus
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_LAUCH_MODE
import com.aetherquorion.cleansuperai.ads.employment.interspaceStudyKepCold
import com.aetherquorion.cleansuperai.ads.employment.interspaceStudyKepHot
import com.aetherquorion.cleansuperai.ads.employment.manager.NativeTools.createFaceListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.tencent.mmkv.MMKV
import java.util.Date

class StartViewRequestTools {
    var interspaceStudyAppOpenAd: AppOpenAd? = null
    private var interspaceStudyKpIsLoading = false
    var interspaceStudyKpIsShowing = false
    private var currentId = ""
    private var kpCurrentLoTm: Long = 0

    fun requestSpecialToKp(context: Activity?, loadedLis: TransLateLoadedLis? = null) {
        context ?: return
        if (TextUtils.isEmpty(currentId)) {
            Log.e(TAG, "start id empty")
            return
        }
        Log.e(TAG, "start load kp")
        if (interspaceStudyKpIsLoading || isAdAvailable()) {
            Log.d(TAG, "AppOpenAd request skipped. loading=$interspaceStudyKpIsLoading available=${isAdAvailable()}")
            return
        }
        interspaceStudyKpIsLoading = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            currentId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    interspaceStudyAppOpenAd = ad
                    interspaceStudyKpIsLoading = false
                    kpCurrentLoTm = Date().time
                    Log.d(TAG, "AppOpenAdLoadCallback onAdLoaded.")
                    loadedLis?.kepRequestSuc()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interspaceStudyKpIsLoading = false
                    loadedLis?.kepLoadedError()
                    Log.d(TAG, "AppOpenAdLoadCallback onAdFailedToLoad: ${loadAdError.message}")
                }
            },
        )
    }

    fun addAdId(ids: String?) {
        if (TextUtils.isEmpty(ids)) {
            Log.e(TAG, "empty kep id!")
            return
        }
        currentId = ids.orEmpty()
    }

    fun interspaceStudyShowedKp(context: Activity?, showComListener: TranslateKepShowStatusLis? = null) {
        context ?: return
        val pos = if (interspaceStudyStatus) {
            Log.e(TAG, "kp  pos ------ cold")
            interspaceStudyKepCold()
        } else {
            Log.e(TAG, "kp  pos ------ hot")
            interspaceStudyKepHot()
        }
        if (AnalysisDataUtil.interspaceStudyRules(pos)) {
            Log.e(TAG, "kp pod limit ------" + AnalysisDataUtil.eventAds(pos))
            return
        }
        if (interspaceStudyKpIsShowing) {
            Log.d(TAG, "The app open ad is already showing.")
            return
        }
        if (context.isDestroyed || context.isFinishing) {
            Log.d(TAG, "The app open host is invalid.")
            return
        }
        if (!isAdAvailable()) {
            Log.d(TAG, "The app open ad is not ready yet. cache tm more than 4 h")
            interspaceStudyAppOpenAd = null
            requestSpecialToKp(context)
            return
        }

        Log.d(TAG, "Will show ad.")
        interspaceStudyAppOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            var kpClicked = false

            override fun onAdClicked() {
                super.onAdClicked()
                if (!kpClicked) {
                    kpClicked = true
                    AnalysisDataUtil.interspaceStudyClickAnys(pos)
                }
                Log.d(TAG, "AppOpenAd clicked.")
                CleanSuperAiApp.isClicked = true
            }

            override fun onAdDismissedFullScreenContent() {
                interspaceStudyAppOpenAd = null
                interspaceStudyKpIsShowing = false
                Log.d(TAG, "onAdDismissedFullScreenContent.")
                if (CleanSuperAiApp.isIntercept && MMKV.defaultMMKV().getBoolean(DATA_CONSTANT_LAUCH_MODE, true)) {
                    Log.d(TAG, "middle jump intercept .")
                    return
                }
                showComListener?.statusShowedSuc()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interspaceStudyAppOpenAd = null
                interspaceStudyKpIsShowing = false
                Log.d(TAG, "onAdFailedToShowFullScreenContent: ${adError.message}")
                showComListener?.statusShowedSuc()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "onAdShowedFullScreenContent.")
                showComListener?.statusShowedFa()
                AnalysisDataUtil.interspaceStudyShowAnys(pos)
            }
        }
        interspaceStudyAppOpenAd?.setOnPaidEventListener {
            createFaceListener(it, "Appopen", currentId, pos)
        }
        interspaceStudyKpIsShowing = true
        interspaceStudyAppOpenAd?.show(context)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - kpCurrentLoTm
        return dateDifference < 3_600_000L * numHours
    }

    private fun isAdAvailable(): Boolean {
        return interspaceStudyAppOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    companion object {
        val adClass: StartViewRequestTools by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            StartViewRequestTools()
        }
        private const val TAG = "StartViewRequestTools"
    }
}
