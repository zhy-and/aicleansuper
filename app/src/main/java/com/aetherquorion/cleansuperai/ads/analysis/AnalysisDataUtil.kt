package com.aetherquorion.cleansuperai.ads.analysis

import android.util.Log
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NATIVE_CLICK_CT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NATIVE_SHOW_CT
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
import com.tencent.mmkv.MMKV
import java.util.Calendar

object AnalysisDataUtil {
    fun kpCanLoadInterceptor(): Boolean {
        return !interspaceStudyRules(interspaceStudyKepCold()) ||
            !interspaceStudyRules(interspaceStudyKepHot())
    }

    fun interCanInterceptor(): Boolean {
        return !interspaceStudyRules(homeInterstitialAd()) ||
            !interspaceStudyRules(bottomTabSwitchInterstitialAd()) ||
            !interspaceStudyRules(swipeInterstitialAd()) ||
            !interspaceStudyRules(compressInterstitialAd()) ||
            !interspaceStudyRules(toolsInterstitialAd()) ||
            !interspaceStudyRules(detailInterstitialAd()) ||
            !interspaceStudyRules(languageTranslateCy()) ||
            !interspaceStudyRules(detAllInter()) ||
            !interspaceStudyRules(qufengTabInter()) ||
            !interspaceStudyRules(swipeDetailInterstitialAd()) ||
            !interspaceStudyRules(cleanCenterInterstitialAd()) ||
            !interspaceStudyRules(contactsInterstitialAd()) ||
            !interspaceStudyRules(largeVideoInterstitialAd()) ||
            !interspaceStudyRules(similarImagesInterstitialAd()) ||
            !interspaceStudyRules(screenshotListInterstitialAd()) ||
            !interspaceStudyRules(duplicateVideoInterstitialAd()) ||
            !interspaceStudyRules(settingInter()) ||
            !interspaceStudyRules(backSpecialToCy())
    }

    fun interspaceStudyRules(artPos: Long): Boolean {
        val configKv = MMKV.mmkvWithID("enmusic")
        val networkClickCount = configKv.getInt("$DATA_CONSTANT_STU_NET_CLICK_CT-$artPos", 0)
        val networkShowCount = configKv.getInt("$DATA_CONSTANT_STU_NET_SHOW_CT-$artPos", 0)
        val networkShow = configKv.getBoolean("$DATA_CONSTANT_STU_NET_SHOW_ST-$artPos", false)
        Log.e(TAG, eventAds(artPos) + "配置开关" + networkShow)
        if (!networkShow) {
            return true
        }

        mmkvShowTranslate().run {
            val nativeClick = getInt("$DATA_CONSTANT_STU_NATIVE_CLICK_CT-$artPos", 0)
            val nativeShow = getInt("$DATA_CONSTANT_STU_NATIVE_SHOW_CT-$artPos", 0)
            Log.e(TAG, eventAds(artPos) + "本地累积展示次数" + nativeShow)
            if (nativeShow >= networkShowCount) {
                return true
            }
            if (nativeClick >= networkClickCount) {
                return true
            }
        }
        return false
    }

    fun interspaceStudyClickAnys(krodePos: Long) {
        mmkvShowTranslate().run {
            val count = getInt("$DATA_CONSTANT_STU_NATIVE_CLICK_CT-$krodePos", 0) + 1
            Log.e(TAG, eventAds(krodePos) + " click count =====" + count)
            putInt("$DATA_CONSTANT_STU_NATIVE_CLICK_CT-$krodePos", count)
        }
    }

    fun interspaceStudyCheckHT(cacheTime: Long): Boolean {
        return System.currentTimeMillis() - cacheTime > 60 * 60 * 1000
    }

    private fun mmkvShowTranslate(): MMKV {
        val adarammkv = MMKV.mmkvWithID("pekMMKV")
        val today = Calendar.getInstance().get(Calendar.DATE)
        val lastDay = adarammkv.getInt("day", 0)
        if (today != lastDay) {
            Log.e(TAG, "clean all")
            adarammkv.clear()
        }
        adarammkv.putInt("day", today)
        return adarammkv
    }

    @JvmStatic
    var interspaceStudyInterIsShowStatus = false

    @JvmStatic
    var interspaceStudyStatus = false

    @JvmStatic
    fun markColdLaunch() {
        interspaceStudyStatus = true
    }

    @JvmStatic
    fun markHotLaunch() {
        interspaceStudyStatus = false
    }

    @JvmStatic
    fun launchPhaseLabel(): String = if (interspaceStudyStatus) "Cold" else "Hot"

    fun interspaceStudyShowAnys(position: Long) {
        mmkvShowTranslate().run {
            val showCount = getInt("$DATA_CONSTANT_STU_NATIVE_SHOW_CT-$position", 0) + 1
            Log.e(TAG, eventAds(position) + "show  count =====" + showCount)
            putInt("$DATA_CONSTANT_STU_NATIVE_SHOW_CT-$position", showCount)
        }
    }

    fun eventAds(position: Long): String {
        return when (position) {
            homeNativeAd() -> "homeNativeAd pos :"
            homeInterstitialAd() -> "homeInterstitialAd pos :"
            bottomTabSwitchInterstitialAd() -> "bottomTabSwitchInterstitialAd pos :"
            swipeNativeAd() -> "swipeNativeAd pos :"
            swipeInterstitialAd() -> "swipeInterstitialAd pos :"
            compressNativeAd() -> "compressNativeAd pos :"
            compressInterstitialAd() -> "compressInterstitialAd pos :"
            toolsNativeAd() -> "toolsNativeAd pos :"
            toolsInterstitialAd() -> "toolsInterstitialAd pos :"
            detailNativeAd() -> "detailNativeAd pos :"
            detailInterstitialAd() -> "detailInterstitialAd pos :"
            swipeDetailNativeAd() -> "swipeDetailNativeAd pos :"
            swipeDetailInterstitialAd() -> "swipeDetailInterstitialAd pos :"
            cleanCenterNativeAd() -> "cleanCenterNativeAd pos :"
            cleanCenterInterstitialAd() -> "cleanCenterInterstitialAd pos :"
            contactsNativeAd() -> "contactsNativeAd pos :"
            contactsInterstitialAd() -> "contactsInterstitialAd pos :"
            largeVideoNativeAd() -> "largeVideoNativeAd pos :"
            largeVideoInterstitialAd() -> "largeVideoInterstitialAd pos :"
            similarImagesNativeAd() -> "similarImagesNativeAd pos :"
            similarImagesInterstitialAd() -> "similarImagesInterstitialAd pos :"
            screenshotListNativeAd() -> "screenshotListNativeAd pos :"
            screenshotListInterstitialAd() -> "screenshotListInterstitialAd pos :"
            duplicateVideoNativeAd() -> "duplicateVideoNativeAd pos :"
            duplicateVideoInterstitialAd() -> "duplicateVideoInterstitialAd pos :"
            settingNative() -> "settingNative pos :"
            settingInter() -> "settingInter pos :"
            homeHf() -> "homeHf pos :"
            backSpecialToCy() -> "backSpecialToCy pos :"
            languageTranslateCy() -> "languageTranslateCy interspaceStudy pos :"
            qufengTabInter() -> "historyCy interspaceStudy pos :"
            detAllInter() -> "resTranslateCy interspaceStudy pos :"
            interspaceStudyKepCold() -> "interspaceStudyKepCold interspaceStudy pos :"
            interspaceStudyKepHot() -> "interspaceStudyKepHot interspaceStudy pos :"
            else -> "unknown interspaceStudy pos :"
        }
    }

    private const val TAG = "AnalysisDataUtil"
}
