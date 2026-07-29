package com.aetherquorion.cleansuperai.ads.employment.manager.mobtools

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.aetherquorion.cleansuperai.CleanSuperAiApp
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.ads.ListenerTrans
import com.aetherquorion.cleansuperai.ads.TemTranslateIntersLis
import com.aetherquorion.cleansuperai.ads.TransLateLoadedLis
import com.aetherquorion.cleansuperai.ads.TranslateKepShowStatusLis
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil.interspaceStudyInterIsShowStatus
import com.aetherquorion.cleansuperai.ads.banner.BannerView
import com.aetherquorion.cleansuperai.ads.consent.GoogleMobileAdsConsentManager
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_AD_CACHE_TM
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_AD_DATA
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NET_BUT_ST
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NET_CLICK_CT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NET_SHOW_CT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NET_SHOW_ST
import com.aetherquorion.cleansuperai.ads.employment.backSpecialToCy
import com.aetherquorion.cleansuperai.ads.employment.bottomTabSwitchInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.cleanCenterInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.cleanCenterNativeAd
import com.aetherquorion.cleansuperai.ads.employment.compressInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.compressNativeAd
import com.aetherquorion.cleansuperai.ads.employment.contactsInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.contactsNativeAd
import com.aetherquorion.cleansuperai.ads.employment.detailInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.detailNativeAd
import com.aetherquorion.cleansuperai.ads.employment.detAllInter
import com.aetherquorion.cleansuperai.ads.employment.duplicateVideoInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.duplicateVideoNativeAd
import com.aetherquorion.cleansuperai.ads.employment.homeHf
import com.aetherquorion.cleansuperai.ads.employment.homeInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.homeNativeAd
import com.aetherquorion.cleansuperai.ads.employment.interspaceStudyKepCold
import com.aetherquorion.cleansuperai.ads.employment.interspaceStudyKepHot
import com.aetherquorion.cleansuperai.ads.employment.largeVideoInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.largeVideoNativeAd
import com.aetherquorion.cleansuperai.ads.employment.languageTranslateCy
import com.aetherquorion.cleansuperai.ads.employment.qufengTabInter
import com.aetherquorion.cleansuperai.ads.employment.screenshotListInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.screenshotListNativeAd
import com.aetherquorion.cleansuperai.ads.employment.settingInter
import com.aetherquorion.cleansuperai.ads.employment.settingNative
import com.aetherquorion.cleansuperai.ads.employment.similarImagesInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.similarImagesNativeAd
import com.aetherquorion.cleansuperai.ads.employment.swipeDetailInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.swipeDetailNativeAd
import com.aetherquorion.cleansuperai.ads.employment.swipeInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.swipeNativeAd
import com.aetherquorion.cleansuperai.ads.employment.toolsInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.toolsNativeAd
import com.aetherquorion.cleansuperai.ads.employment.manager.NativeTools.createFaceListener
import com.aetherquorion.cleansuperai.ads.employment.manager.start.StartViewRequestTools
import com.aetherquorion.cleansuperai.ads.model.AdPlaceBean
import com.aetherquorion.cleansuperai.ads.model.AdaraImportBean
import com.aetherquorion.cleansuperai.ads.model.InfoDestroyCentre
import com.aetherquorion.cleansuperai.ads.model.InfoDestroySp
import com.blankj.utilcode.util.ActivityUtils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import org.greenrobot.eventbus.EventBus

class LoadManagerTools {
    var interspaceStudyKpAdsId: String? = null
    private val bannerAdsByPosition = mutableMapOf<Long, NativeAd>()
    private val bannerAdUnitIdsByPosition = mutableMapOf<Long, String>()
    private val bannerLoadingPositions = mutableSetOf<Long>()
    var interspaceStudyAdsPos: Long = -1
    private var interspaceStudyInterCTM: Long = 0
    private var interSpecialToLoading = false
    private var interSpecialToTransAdsId: String? = null
    var hfAdID: String? = null
    var interspaceStudyInterOj: InterstitialAd? = null

    fun bannerShowButton(bannerView: BannerView?) {
        bannerView ?: return
        val button = bannerView.findViewById<TextView>(R.id.cta)
        val position = interspaceStudyAdsPos
        when (MMKV.defaultMMKV().getInt("$DATA_CONSTANT_STU_NET_BUT_ST-$position", 2)) {
            2 -> {
                button.visibility = View.VISIBLE
                button.setBackgroundResource(R.drawable.shape_bt_bg)
            }
        }
    }

    fun interspaceStudyLoadDefaultNativeAds() {
        if (!AnalysisDataUtil.interspaceStudyRules(homeNativeAd())) {
            interspaceStudyLoadTransBanner(homeNativeAd())
        }
    }

    fun getCurSpecialToNative(nativePos: Long, context: Activity): NativeAd? {
        if (context.isDestroyed || context.isFinishing) return null
        if (AnalysisDataUtil.interspaceStudyRules(nativePos)) return null
        if (interspaceStudyInterIsShowStatus) return null
        interspaceStudyAdsPos = nativePos
        bannerAdsByPosition.remove(nativePos)?.let {
            return it
        }
        return null
    }

    fun responseDatas(baseTrans: Activity?, homeToLoad: Boolean = false) {
        try {
            val byvcBeans = readConfigPlaces() ?: return
            byvcBeans.forEach { handleConfigPlace(it) }
            if (!homeToLoad && isUmeng()) {
                createKepingInit(baseTrans)
            }
            if (skipReturns(homeToLoad)) return
            if (!isUmeng()) return
            if (!AnalysisDataUtil.kpCanLoadInterceptor()) {
                interspaceStudyLoadDefaultCyAds(baseTrans)
            }
            interspaceStudyLoadDefaultNativeAds()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun middleConfigDispatcher(baseTrans: Activity?, homeToLoad: Boolean = false) {
        try {
            val byvcBeans = readConfigPlaces() ?: return
            byvcBeans.forEach { handleConfigPlace(it) }
            if (!homeToLoad && isUmeng()) {
                newKepingInit(baseTrans)
            }
            if (skipReturns(homeToLoad)) return
            if (!isUmeng()) return
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readConfigPlaces(): MutableList<AdPlaceBean>? {
        if (TextUtils.isEmpty(MMKV.defaultMMKV().getString(DATA_CONSTANT_AD_DATA, ""))) {
            MMKV.defaultMMKV().putLong(DATA_CONSTANT_AD_CACHE_TM, 0)
            return null
        }
        val bean = Gson().fromJson(
            MMKV.defaultMMKV().getString(DATA_CONSTANT_AD_DATA, ""),
            AdaraImportBean::class.java,
        ) ?: return null
        return bean.pzdyeqenjrku
            ?.hktkevvwwbw
            ?.hdvrhqxtjsr
            ?.ntpwxttl
            ?.xjcchmpevr
            ?.udjpjamfklmglw
            ?.lcpakw
    }

    private fun handleConfigPlace(bean: AdPlaceBean) {
        interspaceStudyHandlerData(bean)
        when (bean.vlthm) {
            interspaceStudyKepCold(), interspaceStudyKepHot() -> if (!TextUtils.isEmpty(bean.aaojq)) interspaceStudyKpAdsId = bean.aaojq
            homeNativeAd(), swipeNativeAd(), compressNativeAd(), toolsNativeAd(), detailNativeAd(),
            swipeDetailNativeAd(), cleanCenterNativeAd(), contactsNativeAd(), largeVideoNativeAd(),
            similarImagesNativeAd(), screenshotListNativeAd(), duplicateVideoNativeAd(), settingNative() -> {
                if (!TextUtils.isEmpty(bean.aaojq)) {
                    bannerAdUnitIdsByPosition[bean.vlthm] = bean.aaojq.orEmpty()
                }
            }
            homeInterstitialAd(), bottomTabSwitchInterstitialAd(), swipeInterstitialAd(), compressInterstitialAd(),
            toolsInterstitialAd(), detailInterstitialAd(), swipeDetailInterstitialAd(), cleanCenterInterstitialAd(),
            contactsInterstitialAd(), largeVideoInterstitialAd(), similarImagesInterstitialAd(),
            screenshotListInterstitialAd(), duplicateVideoInterstitialAd(), settingInter(), backSpecialToCy(),
            languageTranslateCy(), detAllInter(), qufengTabInter() -> {
                if (!TextUtils.isEmpty(bean.aaojq)) interSpecialToTransAdsId = bean.aaojq
            }
        }
    }

    private fun isUmeng(): Boolean {
        val context = CleanSuperAiApp.app ?: return CleanSuperAiApp.umengLoadFlag
        return CleanSuperAiApp.umengLoadFlag || GoogleMobileAdsConsentManager.getInstance(context).canRequestAds()
    }

    fun interspaceStudyLoadTransBanner(showAdPosition: Long, resultAdLoadBack: ListenerTrans? = null) {
        try {
            var position = showAdPosition
            if (bannerLoadingPositions.contains(position)) return
            if (bannerAdsByPosition.containsKey(position)) {
                resultAdLoadBack?.loadTransAdStatus(true)
                return
            }
            bannerLoadingPositions.add(position)
            val unitId = bannerAdUnitIdsByPosition[position].orEmpty()
            if (TextUtils.isEmpty(unitId)) {
                bannerLoadingPositions.remove(position)
                return
            }
            val context = CleanSuperAiApp.app ?: return
            val loader = AdLoader.Builder(context, unitId)
                .forNativeAd { nativeAd ->
                    nativeAd.setOnPaidEventListener {
                        createFaceListener(it, "Native", unitId, position)
                    }
                    bannerAdsByPosition.remove(position)?.destroy()
                    bannerAdsByPosition[position] = nativeAd
                    bannerLoadingPositions.remove(position)
                    resultAdLoadBack?.loadTransAdStatus(true)
                }
                .withAdListener(object : AdListener() {
                    var clicked = false
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        bannerLoadingPositions.remove(position)
                        resultAdLoadBack?.loadTransAdStatus(false)
                    }

                    override fun onAdClicked() {
                        if (interspaceStudyAdsPos.toInt() != -1) position = interspaceStudyAdsPos
                        if (!clicked) {
                            AnalysisDataUtil.interspaceStudyClickAnys(position)
                            clicked = true
                        }
                        CleanSuperAiApp.isClicked = true
                    }

                    override fun onAdImpression() {
                        if (interspaceStudyAdsPos.toInt() != -1) position = interspaceStudyAdsPos
                        AnalysisDataUtil.interspaceStudyShowAnys(position)
                        mainHandler.postDelayed({ interspaceStudyLoadTransBanner(position) }, 1500)
                    }
                })
                .build()
            loader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            bannerLoadingPositions.remove(showAdPosition)
            e.printStackTrace()
        }
    }

    fun createKepingInit(baseTrans: Activity?) {
        if (!AnalysisDataUtil.kpCanLoadInterceptor()) return
        StartViewRequestTools.adClass.addAdId(interspaceStudyKpAdsId)
        mainHandler.post {
            if (StartViewRequestTools.adClass.interspaceStudyAppOpenAd == null) {
                StartViewRequestTools.adClass.requestSpecialToKp(baseTrans, object : TransLateLoadedLis {
                    override fun kepRequestSuc() {
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                        interspaceStudyShowKpAds(baseTrans)
                    }

                    override fun kepLoadedError() {
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                    }
                })
            } else {
                interspaceStudyLoadDefaultCyAds(baseTrans)
                interspaceStudyShowKpAds(baseTrans)
            }
        }
    }

    fun interspaceStudyLoadDefaultCyAds(viewSic: Activity?) {
        viewSic ?: return
        if (!AnalysisDataUtil.interspaceStudyRules(backSpecialToCy())) initCurrentLoadCy(backSpecialToCy())
    }

    fun newKepingInit(baseTrans: Activity?) {
        if (!AnalysisDataUtil.kpCanLoadInterceptor()) return
        StartViewRequestTools.adClass.addAdId(interspaceStudyKpAdsId)
        mainHandler.post {
            if (StartViewRequestTools.adClass.interspaceStudyAppOpenAd == null) {
                if (AnalysisDataUtil.interspaceStudyRules(interspaceStudyKepHot())) return@post
                StartViewRequestTools.adClass.requestSpecialToKp(baseTrans, object : TransLateLoadedLis {
                    override fun kepRequestSuc() {
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                        middleShowKpAds(baseTrans)
                    }

                    override fun kepLoadedError() {
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                    }
                })
            } else {
                interspaceStudyLoadDefaultCyAds(baseTrans)
                middleShowKpAds(baseTrans)
            }
        }
    }

    fun getShowCyAds(recAds: Long, context: Activity) {
        if (AnalysisDataUtil.interspaceStudyRules(recAds)) return
        val ad = interspaceStudyInterOj
        if (ad != null) {
            if (AnalysisDataUtil.interspaceStudyCheckHT(interspaceStudyInterCTM)) {
                interspaceStudyInterOj = null
                interspaceStudyInterCTM = 0
                return
            }
            interspaceStudyInterIsShowStatus = true
            ad.show(context)
            interspaceStudyHandlerInter(ad, recAds)
            interspaceStudyInterOj = null
        } else {
            initCurrentLoadCy(recAds)
        }
    }

    fun initCurrentLoadCy(interPos: Long, lis: TemTranslateIntersLis? = null) {
        mainHandler.post {
            try {
                var adId = ""
                if (interSpecialToLoading) return@post
                if (interspaceStudyInterOj != null && !AnalysisDataUtil.interspaceStudyCheckHT(interspaceStudyInterCTM)) {
                    lis?.loadTransInterStatus(true)
                    return@post
                }
                interSpecialToLoading = true
                interSpecialToTransAdsId?.run { adId = this }
                if (TextUtils.isEmpty(adId)) {
                    interSpecialToLoading = false
                    return@post
                }
                val context = CleanSuperAiApp.app ?: return@post
                InterstitialAd.load(context, adId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        interspaceStudyInterOj = interstitialAd
                        interspaceStudyInterCTM = System.currentTimeMillis()
                        interSpecialToLoading = false
                        lis?.loadTransInterStatus(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        interSpecialToLoading = false
                        lis?.loadTransInterStatus(false)
                    }
                })
            } catch (e: Exception) {
                interSpecialToLoading = false
                e.printStackTrace()
            }
        }
    }

    private fun interspaceStudyHandlerData(bean: AdPlaceBean) {
        MMKV.mmkvWithID("enmusic").putBoolean("$DATA_CONSTANT_STU_NET_SHOW_ST-${bean.vlthm}", "0" == bean.dhdeit)
        MMKV.mmkvWithID("enmusic").putInt("$DATA_CONSTANT_STU_NET_CLICK_CT-${bean.vlthm}", bean.jxlwhu)
        MMKV.mmkvWithID("enmusic").putInt("$DATA_CONSTANT_STU_NET_SHOW_CT-${bean.vlthm}", bean.dgsup)
    }

    private fun interspaceStudyShowKpAds(activity: Activity?) {
        StartViewRequestTools.adClass.interspaceStudyShowedKp(activity, object : TranslateKepShowStatusLis {
            override fun statusShowedSuc() {
                activity ?: return
                if (!activity.isFinishing && !activity.isDestroyed) {
                    ActivityUtils.startActivity(MainActivity::class.java)
                    ActivityUtils.finishActivity(activity)
                }
            }

            override fun statusShowedFa() {
                EventBus.getDefault().post(InfoDestroySp(true))
            }
        })
    }

    private fun middleShowKpAds(activity: Activity?) {
        StartViewRequestTools.adClass.interspaceStudyShowedKp(activity, object : TranslateKepShowStatusLis {
            override fun statusShowedSuc() {
                activity ?: return
                if (!activity.isFinishing && !activity.isDestroyed) {
                    if (!ActivityUtils.isActivityExistsInStack(MainActivity::class.java)) {
                        ActivityUtils.startActivity(MainActivity::class.java)
                    }
                    activity.finish()
                }
            }

            override fun statusShowedFa() {
                EventBus.getDefault().post(InfoDestroyCentre(true))
            }
        })
    }

    private fun skipReturns(jump: Boolean): Boolean {
        if (jump) {
            mainHandler.post { interspaceStudyLoadDefaultNativeAds() }
            return true
        }
        return false
    }

    private fun interspaceStudyHandlerInter(interstitialAd: InterstitialAd?, pos: Long) {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            var clicked = false
            override fun onAdClicked() {
                if (!clicked) {
                    clicked = true
                    AnalysisDataUtil.interspaceStudyClickAnys(pos)
                }
                CleanSuperAiApp.isClicked = true
            }

            override fun onAdDismissedFullScreenContent() {
                interspaceStudyInterIsShowStatus = false
            }

            override fun onAdFailedToShowFullScreenContent(fa: AdError) {
                interspaceStudyInterIsShowStatus = false
            }

            override fun onAdImpression() {
                interspaceStudyInterIsShowStatus = true
            }

            override fun onAdShowedFullScreenContent() {
                interspaceStudyInterIsShowStatus = true
                AnalysisDataUtil.interspaceStudyShowAnys(pos)
                mainHandler.postDelayed({ initCurrentLoadCy(pos) }, 1500)
            }
        }
        interstitialAd?.setOnPaidEventListener {
            createFaceListener(it, "Interstitial", interstitialAd.adUnitId, pos)
        }
    }

    companion object {
        val adSpInstance: LoadManagerTools by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            LoadManagerTools()
        }
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
