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

fun homeTabBanner(): Long = 2056709529737105408
fun settingNative(): Long = 2057059661990989824
fun permissionNative(): Long = 2057059860664946688
fun songTabBanner(): Long = 2057058885453484032
fun albumTabBanner(): Long = 2057059251504418816
fun artistTabNative(): Long = 2057059351160295424
fun playlistTabBanner(): Long = 2057059093740523520
fun qufengTabBanner(): Long = 2057059475369758720
fun searchBanner(): Long = 2057059767532785664
fun allDetBanner(): Long = 2057059564318887936
fun zhuanTabInter(): Long = 2056709465545510912
fun songTabInter(): Long = 2057060312322412544
fun playlistTabInter(): Long = 2057060386819149824
fun artistTabInter(): Long = 2057060560213905408
fun qufengTabInter(): Long = 2057060635469156352
fun albumTabInter(): Long = 2057060460884529152
fun searchInter(): Long = 2057061016458891264
fun settingInter(): Long = 2057060772590522368
fun scanInter(): Long = 2057060772590522368
fun detAllInter(): Long = 2057060707335278592
fun homeHf(): Long = 2056709590448869376
fun backSpecialToCy(): Long = 2058842773889355776
fun languageTranslateCy(): Long = 1896824357792911360
fun interspaceStudyKepCold(): Long = 2056709331480219648
fun interspaceStudyKepHot(): Long = 2056709270598455296
fun languageListBanner(): Long = 0
