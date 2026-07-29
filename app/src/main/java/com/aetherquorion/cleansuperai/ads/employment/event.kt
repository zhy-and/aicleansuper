package com.aetherquorion.cleansuperai.ads.employment

import android.util.Log
import java.util.Locale

const val DATA_CONSTANT_STU_NET_CLICK_CT = "enmusic_network_click_count"
const val DATA_CONSTANT_STU_NET_SHOW_CT = "enmusic_network_show_count"
const val DATA_CONSTANT_STU_NET_BUT_ST = "enmusic_network_button_status"
const val DATA_CONSTANT_STU_NET_SHOW_ST = "enmusic_network_is_show"
const val DATA_CONSTANT_STU_NATIVE_CLICK_CT = "enmusic_native_click_count"
const val DATA_CONSTANT_STU_NATIVE_SHOW_CT = "enmusic_native_show_count"
const val DATA_CONSTANT_AD_CACHE_TM = "txt_translatekey_ads_cm"
const val DATA_CONSTANT_APP_CACHE_TM = "txt_translatekey_app_cm"
const val DATA_CONSTANT_REFRESH = "txt_translatekey_refresh"
const val DATA_CONSTANT_REFER_INFO = "txt_translatekey_re_infos"
const val DATA_CONSTANT_AD_DATA = "txt_translatekey_ads_data"
const val GLOBAL_CONSTANT_FB_SYS = "adara_config_tms"
const val GLOBAL_CONSTANT_LINES = "adara_lines"
const val GLOBAL_CONSTANT_COEFS = "adara_xs"
const val DATA_CONSTANT_FO = "txt_translatekey_fir_ops"
const val DATA_CONSTANT_LAUCH_MODE = "lauch_mode"
const val DATA_CONSTANT_NATIVE_CONTENT = "native_content"
const val VALUE_TOTAL_LINES = "lines_one"
const val VALUE_TOTAL_LINES_TWO = "lines_two"
const val MMKV_NOTIFICATION_TITLE = "notification_title"
const val MMKV_NOTIFICATION_CONTENT = "notification_content"

fun checkRules(): Boolean {
    val currentCountry = Locale.getDefault().country.lowercase()
    for (country in countryCode) {
        if (country == currentCountry) {
            Log.e("AdEvent", "currentCountry true")
            return true
        }
    }
    Log.e("AdEvent", "currentCountry false")
    return false
}

private val countryCode = arrayOf(
    "ie", "ee", "at", "bg", "be", "pl", "dk", "de", "fr", "fi", "nl", "cz", "hr",
    "lv", "lt", "lu", "ro", "mt", "pt", "se", "cy", "sk", "si", "es", "gr", "hu",
    "it", "gb", "ch",
)

fun homeNativeAd(): Long = 2082349918957473792L
fun homeInterstitialAd(): Long = 2082349918982377472L
fun bottomTabSwitchInterstitialAd(): Long = 2082349918982377472L
fun swipeNativeAd(): Long = 2082391084614291456L
fun swipeInterstitialAd(): Long = 2082349918982377472L
fun compressNativeAd(): Long = 2082391666962731008L
fun compressInterstitialAd(): Long = 2082349918982377472L
fun toolsNativeAd(): Long = 2082392148386516992L
fun toolsInterstitialAd(): Long = 2082349918982377472L
fun detailNativeAd(): Long = 2082393970164240384L
fun detailInterstitialAd(): Long = 2082393097262206976L
fun swipeDetailNativeAd(): Long = 2082390435267219456L
fun swipeDetailInterstitialAd(): Long = 2082390901087801344L
fun cleanCenterNativeAd(): Long = 2082389902339346432L
fun cleanCenterInterstitialAd(): Long = 2082389844048220160L
fun contactsNativeAd(): Long = 2082389190466473984L
fun contactsInterstitialAd(): Long = 2082389234749673472L
fun largeVideoNativeAd(): Long = 2082385451591405568L
fun largeVideoInterstitialAd(): Long = 2082385401165123584L
fun similarImagesNativeAd(): Long = 2082385062063378432L
fun similarImagesInterstitialAd(): Long = 2082383245530501120L
fun screenshotListNativeAd(): Long = 2082383245556715520L
fun screenshotListInterstitialAd(): Long = 2082383898780897280L
fun duplicateVideoNativeAd(): Long = 2082383245584109568L
fun duplicateVideoInterstitialAd(): Long = 2082384158663643136L

fun settingNative(): Long = 2082394287342358528L
fun songTabInter(): Long = bottomTabSwitchInterstitialAd()
fun playlistTabInter(): Long = swipeInterstitialAd()
fun artistTabInter(): Long = toolsInterstitialAd()
fun qufengTabInter(): Long = 2057060635469156352
fun albumTabInter(): Long = compressInterstitialAd()
fun searchInter(): Long = 2057061016458891264
fun settingInter(): Long = 2082394225404284928L
fun scanInter(): Long = 2057060772590522368
fun detAllInter(): Long = 2057060707335278592
fun homeHf(): Long = 2056709590448869376
fun backSpecialToCy(): Long = 2082394380329816064L
fun languageTranslateCy(): Long = 1896824357792911360
fun interspaceStudyKepCold(): Long = 2082349918929031168L
fun interspaceStudyKepHot(): Long = 2082349919008329728L
