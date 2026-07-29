package com.aetherquorion.cleansuperai.ads.analysis

import android.util.Log
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NATIVE_CLICK_CT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NATIVE_SHOW_CT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NET_CLICK_CT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NET_SHOW_CT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_STU_NET_SHOW_ST
import com.aetherquorion.cleansuperai.ads.employment.albumTabBanner
import com.aetherquorion.cleansuperai.ads.employment.artistTabInter
import com.aetherquorion.cleansuperai.ads.employment.artistTabNative
import com.aetherquorion.cleansuperai.ads.employment.backSpecialToCy
import com.aetherquorion.cleansuperai.ads.employment.detAllInter
import com.aetherquorion.cleansuperai.ads.employment.homeTabBanner
import com.aetherquorion.cleansuperai.ads.employment.interspaceStudyKepCold
import com.aetherquorion.cleansuperai.ads.employment.interspaceStudyKepHot
import com.aetherquorion.cleansuperai.ads.employment.languageListBanner
import com.aetherquorion.cleansuperai.ads.employment.languageTranslateCy
import com.aetherquorion.cleansuperai.ads.employment.permissionNative
import com.aetherquorion.cleansuperai.ads.employment.qufengTabInter
import com.aetherquorion.cleansuperai.ads.employment.songTabBanner
import com.aetherquorion.cleansuperai.ads.employment.songTabInter
import com.aetherquorion.cleansuperai.ads.employment.zhuanTabInter
import com.tencent.mmkv.MMKV
import java.util.Calendar

object AnalysisDataUtil {
    fun kpCanLoadInterceptor(): Boolean {
        return !interspaceStudyRules(interspaceStudyKepCold()) ||
            !interspaceStudyRules(interspaceStudyKepHot())
    }

    fun interCanInterceptor(): Boolean {
        return !interspaceStudyRules(artistTabInter()) ||
            !interspaceStudyRules(detAllInter()) ||
            !interspaceStudyRules(backSpecialToCy()) ||
            !interspaceStudyRules(qufengTabInter()) ||
            !interspaceStudyRules(zhuanTabInter()) ||
            !interspaceStudyRules(songTabInter()) ||
            !interspaceStudyRules(languageTranslateCy())
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
            homeTabBanner() -> "homeBanner interspaceStudy pos :"
            artistTabNative() -> "historyBanner interspaceStudy pos :"
            languageListBanner() -> "languageListBanner interspaceStudy pos :"
            songTabBanner() -> " resBanner "
            permissionNative() -> "txtBanner interspaceStudy pos :"
            albumTabBanner() -> "voiceBanner interspaceStudy pos :"
            languageTranslateCy() -> "languageTranslateCy interspaceStudy pos :"
            qufengTabInter() -> "historyCy interspaceStudy pos :"
            backSpecialToCy() -> "backSpecialToCy interspaceStudy pos :"
            songTabInter() -> "takeTranslateCy interspaceStudy pos :"
            zhuanTabInter() -> "txtTranslateCy interspaceStudy pos :"
            detAllInter() -> "resTranslateCy interspaceStudy pos :"
            artistTabInter() -> "voiceTranslateCy interspaceStudy pos :"
            interspaceStudyKepCold() -> "interspaceStudyKepCold interspaceStudy pos :"
            interspaceStudyKepHot() -> "interspaceStudyKepHot interspaceStudy pos :"
            else -> "unknown interspaceStudy pos :"
        }
    }

    private const val TAG = "AnalysisDataUtil"
}
