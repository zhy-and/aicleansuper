package com.aetherquorion.cleansuperai.ads.employment.manager.mobtools

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
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
        Log.e(
            TAG,
            " ad btn visble =====" + AnalysisDataUtil.eventAds(position) +
                MMKV.defaultMMKV().getInt("$DATA_CONSTANT_STU_NET_BUT_ST-$position", 2),
        )
        when (MMKV.defaultMMKV().getInt("$DATA_CONSTANT_STU_NET_BUT_ST-$position", 2)) {
            2 -> {
                button.visibility = View.VISIBLE
                button.setBackgroundResource(R.drawable.shape_bt_bg)
            }
        }
    }

    fun interspaceStudyLoadDefaultNativeAds() {
        if (!AnalysisDataUtil.interspaceStudyRules(homeNativeAd())) {
            Log.e(TAG, "default native to load ------" + AnalysisDataUtil.eventAds(homeNativeAd()))
            interspaceStudyLoadTransBanner(homeNativeAd())
        } else {
            Log.e(TAG, "default native limit ------" + AnalysisDataUtil.eventAds(homeNativeAd()))
        }
    }

    fun getCurSpecialToNative(nativePos: Long, context: Activity): NativeAd? {
        if (context.isDestroyed || context.isFinishing) {
            Log.e(TAG, "native host invalid ------" + AnalysisDataUtil.eventAds(nativePos))
            return null
        }
        if (AnalysisDataUtil.interspaceStudyRules(nativePos)) {
            Log.e(TAG, "native  limit ------" + AnalysisDataUtil.eventAds(nativePos))
            return null
        }
        if (interspaceStudyInterIsShowStatus) {
            Log.e(TAG, "native skip, inter showing ------" + AnalysisDataUtil.eventAds(nativePos))
            return null
        }
        interspaceStudyAdsPos = nativePos
        bannerAdsByPosition.remove(nativePos)?.let {
            Log.e(TAG, "banner cache hit ------" + AnalysisDataUtil.eventAds(nativePos))
            return it
        }
        Log.e(TAG, "banner cache empty ------" + AnalysisDataUtil.eventAds(nativePos))
        return null
    }

    fun responseDatas(baseTrans: Activity?, homeToLoad: Boolean = false) {
        try {
            val byvcBeans = readConfigPlaces() ?: run {
                Log.e(TAG, "ad config empty, responseDatas return")
                return
            }
            Log.e(TAG, "ad config place count ------${byvcBeans.size}")
            byvcBeans.forEach { handleConfigPlace(it) }
            if (!homeToLoad && isUmeng()) {
                Log.e(TAG, "load um  true ----  ")
                createKepingInit(baseTrans)
            }
            if (skipReturns(homeToLoad)) return
            if (!isUmeng()) {
                Log.e(TAG, "skip  ----  ")
                return
            }
            if (!AnalysisDataUtil.kpCanLoadInterceptor()) {
                Log.e(TAG, "kp cannot load, to load inter ----")
                interspaceStudyLoadDefaultCyAds(baseTrans)
            }
            interspaceStudyLoadDefaultNativeAds()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun middleConfigDispatcher(baseTrans: Activity?, homeToLoad: Boolean = false) {
        try {
            val byvcBeans = readConfigPlaces() ?: run {
                Log.e(TAG, "ad config empty, middleConfigDispatcher return")
                return
            }
            Log.e(TAG, "middle ad config place count ------${byvcBeans.size}")
            byvcBeans.forEach { handleConfigPlace(it) }
            if (!homeToLoad && isUmeng()) {
                Log.e(TAG, "middle load um true ----")
                newKepingInit(baseTrans)
            }
            if (skipReturns(homeToLoad)) return
            if (!isUmeng()) {
                Log.e(TAG, "middle skip ----")
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readConfigPlaces(): MutableList<AdPlaceBean>? {
        if (TextUtils.isEmpty(MMKV.defaultMMKV().getString(DATA_CONSTANT_AD_DATA, ""))) {
            MMKV.defaultMMKV().putLong(DATA_CONSTANT_AD_CACHE_TM, 0)
            Log.e(TAG, "ad config cache empty")
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
            interspaceStudyKepCold(), interspaceStudyKepHot() -> if (!TextUtils.isEmpty(bean.aaojq)) {
                interspaceStudyKpAdsId = bean.aaojq
                Log.e(TAG, "config kp id ------" + AnalysisDataUtil.eventAds(bean.vlthm) + bean.aaojq)
            }
            homeNativeAd(), swipeNativeAd(), compressNativeAd(), toolsNativeAd(), detailNativeAd(),
            swipeDetailNativeAd(), cleanCenterNativeAd(), contactsNativeAd(), largeVideoNativeAd(),
            similarImagesNativeAd(), screenshotListNativeAd(), duplicateVideoNativeAd(), settingNative() -> {
                if (!TextUtils.isEmpty(bean.aaojq)) {
                    bannerAdUnitIdsByPosition[bean.vlthm] = bean.aaojq.orEmpty()
                    Log.e(TAG, "config native id ------" + AnalysisDataUtil.eventAds(bean.vlthm) + bean.aaojq)
                }
            }
            homeInterstitialAd(), bottomTabSwitchInterstitialAd(), swipeInterstitialAd(), compressInterstitialAd(),
            toolsInterstitialAd(), detailInterstitialAd(), swipeDetailInterstitialAd(), cleanCenterInterstitialAd(),
            contactsInterstitialAd(), largeVideoInterstitialAd(), similarImagesInterstitialAd(),
            screenshotListInterstitialAd(), duplicateVideoInterstitialAd(), settingInter(), backSpecialToCy(),
            languageTranslateCy(), detAllInter(), qufengTabInter() -> {
                if (!TextUtils.isEmpty(bean.aaojq)) {
                    interSpecialToTransAdsId = bean.aaojq
                    Log.e(TAG, "config inter id ------" + AnalysisDataUtil.eventAds(bean.vlthm) + bean.aaojq)
                }
            }
            homeHf() -> {
                if (!TextUtils.isEmpty(bean.aaojq)) {
                    hfAdID = bean.aaojq
                    Log.e(TAG, "config hf id ------" + AnalysisDataUtil.eventAds(bean.vlthm) + bean.aaojq)
                }
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
            if (bannerLoadingPositions.contains(position)) {
                Log.e(TAG, "banner ad is loading ------" + AnalysisDataUtil.eventAds(position))
                return
            }
            if (bannerAdsByPosition.containsKey(position)) {
                Log.e(TAG, "banner cache pools has cache,stop cac ------" + AnalysisDataUtil.eventAds(position))
                resultAdLoadBack?.loadTransAdStatus(true)
                return
            }
            bannerLoadingPositions.add(position)
            val unitId = bannerAdUnitIdsByPosition[position].orEmpty()
            if (TextUtils.isEmpty(unitId)) {
                bannerLoadingPositions.remove(position)
                Log.e(TAG, "native id error------" + AnalysisDataUtil.eventAds(position))
                return
            }
            Log.e(TAG, "----$unitId")
            val context = CleanSuperAiApp.app ?: run {
                bannerLoadingPositions.remove(position)
                Log.e(TAG, "native context null ------" + AnalysisDataUtil.eventAds(position))
                return
            }
            val loader = AdLoader.Builder(context, unitId)
                .forNativeAd { nativeAd ->
                    Log.e(TAG, "ban ad load suc${unitId}curr pos is ${AnalysisDataUtil.eventAds(position)}")
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
                        Log.e(
                            TAG,
                            "native  onAdFailedToLoad ------" + AnalysisDataUtil.eventAds(position) + adError,
                        )
                        bannerLoadingPositions.remove(position)
                        resultAdLoadBack?.loadTransAdStatus(false)
                    }

                    override fun onAdClicked() {
                        if (interspaceStudyAdsPos.toInt() != -1) position = interspaceStudyAdsPos
                        if (!clicked) {
                            AnalysisDataUtil.interspaceStudyClickAnys(position)
                            clicked = true
                        }
                        Log.e(TAG, "native clicked ------" + AnalysisDataUtil.eventAds(position))
                        CleanSuperAiApp.isClicked = true
                    }

                    override fun onAdClosed() {
                        Log.e(TAG, "native closed ------" + AnalysisDataUtil.eventAds(position))
                    }

                    override fun onAdImpression() {
                        if (interspaceStudyAdsPos.toInt() != -1) position = interspaceStudyAdsPos
                        AnalysisDataUtil.interspaceStudyShowAnys(position)
                        mainHandler.postDelayed({
                            Log.e(TAG, "native  load a new ad ------" + AnalysisDataUtil.eventAds(position))
                            interspaceStudyLoadTransBanner(position)
                        }, 1500)
                    }

                    override fun onAdOpened() {
                        Log.e(TAG, "native opened ------" + AnalysisDataUtil.eventAds(position))
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
        if (!AnalysisDataUtil.kpCanLoadInterceptor()) {
            Log.e(TAG, "kp load limit ----")
            return
        }
        StartViewRequestTools.adClass.addAdId(interspaceStudyKpAdsId)
        mainHandler.post {
            if (StartViewRequestTools.adClass.interspaceStudyAppOpenAd == null) {
                Log.e(TAG, "kp cache empty, request ----")
                StartViewRequestTools.adClass.requestSpecialToKp(baseTrans, object : TransLateLoadedLis {
                    override fun kepRequestSuc() {
                        Log.e(TAG, "kp request suc ----")
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                        interspaceStudyShowKpAds(baseTrans)
                    }

                    override fun kepLoadedError() {
                        Log.e(TAG, "kp request error ----")
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                    }
                })
            } else {
                Log.e(TAG, "kp cached has ad, show ----")
                interspaceStudyLoadDefaultCyAds(baseTrans)
                interspaceStudyShowKpAds(baseTrans)
            }
        }
    }

    fun interspaceStudyLoadDefaultCyAds(viewSic: Activity?) {
        viewSic ?: return
        if (!AnalysisDataUtil.interspaceStudyRules(backSpecialToCy())) {
            Log.e(TAG, "default inter to load ------" + AnalysisDataUtil.eventAds(backSpecialToCy()))
            initCurrentLoadCy(backSpecialToCy())
        } else {
            Log.e(TAG, "default inter limit ------" + AnalysisDataUtil.eventAds(backSpecialToCy()))
        }
    }

    fun newKepingInit(baseTrans: Activity?) {
        if (!AnalysisDataUtil.kpCanLoadInterceptor()) {
            Log.e(TAG, "middle kp load limit ----")
            return
        }
        StartViewRequestTools.adClass.addAdId(interspaceStudyKpAdsId)
        mainHandler.post {
            if (StartViewRequestTools.adClass.interspaceStudyAppOpenAd == null) {
                if (AnalysisDataUtil.interspaceStudyRules(interspaceStudyKepHot())) {
                    Log.e(TAG, "middle kp hot limit ----")
                    return@post
                }
                Log.e(TAG, "middle kp cache empty, request ----")
                StartViewRequestTools.adClass.requestSpecialToKp(baseTrans, object : TransLateLoadedLis {
                    override fun kepRequestSuc() {
                        Log.e(TAG, "middle kp request suc ----")
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                        middleShowKpAds(baseTrans)
                    }

                    override fun kepLoadedError() {
                        Log.e(TAG, "middle kp request error ----")
                        interspaceStudyLoadDefaultCyAds(baseTrans)
                    }
                })
            } else {
                Log.e(TAG, "middle kp cached has ad, show ----")
                interspaceStudyLoadDefaultCyAds(baseTrans)
                middleShowKpAds(baseTrans)
            }
        }
    }

    fun getShowCyAds(recAds: Long, context: Activity) {
        if (AnalysisDataUtil.interspaceStudyRules(recAds)) {
            Log.e(TAG, "inter flag limit ------" + AnalysisDataUtil.eventAds(recAds))
            return
        }
        val ad = interspaceStudyInterOj
        if (ad != null) {
            if (AnalysisDataUtil.interspaceStudyCheckHT(interspaceStudyInterCTM)) {
                Log.e(TAG, "inter cache timeout ------" + AnalysisDataUtil.eventAds(recAds))
                interspaceStudyInterOj = null
                interspaceStudyInterCTM = 0
                initCurrentLoadCy(recAds)
                return
            }
            interspaceStudyInterIsShowStatus = true
            interspaceStudyHandlerInter(ad, recAds)
            ad.show(context)
            Log.e(TAG, "inter showing current pos ===== " + AnalysisDataUtil.eventAds(recAds))
            interspaceStudyInterOj = null
        } else {
            Log.e(TAG, "inter cache no to load new  ===== " + AnalysisDataUtil.eventAds(recAds))
            initCurrentLoadCy(recAds, object : TemTranslateIntersLis {
                override fun loadTransInterStatus(interResult: Boolean) {
                    if (interResult) {
                        Log.e(TAG, "inter cache no to load new suc  ===== " + AnalysisDataUtil.eventAds(recAds))
                    } else {
                        Log.e(TAG, "inter cache no to load new  error  ===== " + AnalysisDataUtil.eventAds(recAds))
                    }
                }
            })
        }
    }

    fun initCurrentLoadCy(interPos: Long, lis: TemTranslateIntersLis? = null) {
        mainHandler.post {
            try {
                var adId = ""
                if (interSpecialToLoading) {
                    Log.e(TAG, "inter is loading return=====" + AnalysisDataUtil.eventAds(interPos))
                    return@post
                }
                if (interspaceStudyInterOj != null && !AnalysisDataUtil.interspaceStudyCheckHT(interspaceStudyInterCTM)) {
                    Log.e(TAG, "inter cached has ads return=====" + AnalysisDataUtil.eventAds(interPos))
                    lis?.loadTransInterStatus(true)
                    return@post
                }
                interSpecialToLoading = true
                interSpecialToTransAdsId?.run { adId = this }
                if (TextUtils.isEmpty(adId)) {
                    Log.e(TAG, "inter id empty=====" + AnalysisDataUtil.eventAds(interPos))
                    interSpecialToLoading = false
                    return@post
                }
                Log.e(TAG, "inter load start =====" + AnalysisDataUtil.eventAds(interPos) + adId)
                val context = CleanSuperAiApp.app ?: run {
                    interSpecialToLoading = false
                    Log.e(TAG, "inter context null=====" + AnalysisDataUtil.eventAds(interPos))
                    return@post
                }
                InterstitialAd.load(context, adId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.e(TAG, "inter ad load suc =====" + AnalysisDataUtil.eventAds(interPos))
                        interspaceStudyInterOj = interstitialAd
                        interspaceStudyInterCTM = System.currentTimeMillis()
                        interSpecialToLoading = false
                        lis?.loadTransInterStatus(true)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "inter ad load error =====" + AnalysisDataUtil.eventAds(interPos) + loadAdError)
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
                Log.e(TAG, "inter clicked ------" + AnalysisDataUtil.eventAds(pos))
                CleanSuperAiApp.isClicked = true
            }

            override fun onAdDismissedFullScreenContent() {
                Log.e(TAG, "inter dismissed ------" + AnalysisDataUtil.eventAds(pos))
                interspaceStudyInterIsShowStatus = false
            }

            override fun onAdFailedToShowFullScreenContent(fa: AdError) {
                Log.e(TAG, "inter failed to show ------" + AnalysisDataUtil.eventAds(pos) + fa)
                interspaceStudyInterIsShowStatus = false
            }

            override fun onAdImpression() {
                Log.e(TAG, "inter impression ------" + AnalysisDataUtil.eventAds(pos))
                interspaceStudyInterIsShowStatus = true
            }

            override fun onAdShowedFullScreenContent() {
                interspaceStudyInterIsShowStatus = true
                AnalysisDataUtil.interspaceStudyShowAnys(pos)
                Log.e(TAG, "inter showed ------" + AnalysisDataUtil.eventAds(pos))
                mainHandler.postDelayed({
                    Log.e(TAG, AnalysisDataUtil.eventAds(pos) + "load a new cy ad")
                    initCurrentLoadCy(pos)
                }, 1500)
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
        private const val TAG = "LoadManagerTools"
    }
}
