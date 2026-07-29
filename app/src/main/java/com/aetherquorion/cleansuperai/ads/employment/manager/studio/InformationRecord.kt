package com.aetherquorion.cleansuperai.ads.employment.manager.studio

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import android.util.Log
import com.aetherquorion.cleansuperai.BuildConfig
import com.aetherquorion.cleansuperai.CleanSuperAiApp
import com.aetherquorion.cleansuperai.MainActivity
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_AD_CACHE_TM
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_AD_DATA
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_APP_CACHE_TM
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_FO
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_LAUCH_MODE
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_NATIVE_CONTENT
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_REFER_INFO
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_REFRESH
import com.aetherquorion.cleansuperai.ads.employment.GLOBAL_CONSTANT_COEFS
import com.aetherquorion.cleansuperai.ads.employment.GLOBAL_CONSTANT_FB_SYS
import com.aetherquorion.cleansuperai.ads.employment.GLOBAL_CONSTANT_LINES
import com.aetherquorion.cleansuperai.ads.employment.MMKV_NOTIFICATION_CONTENT
import com.aetherquorion.cleansuperai.ads.employment.MMKV_NOTIFICATION_TITLE
import com.aetherquorion.cleansuperai.ads.employment.VALUE_TOTAL_LINES
import com.aetherquorion.cleansuperai.ads.employment.VALUE_TOTAL_LINES_TWO
import com.aetherquorion.cleansuperai.ads.employment.manager.NativeTools.getCountryInfo
import com.aetherquorion.cleansuperai.ads.employment.manager.NativeTools.getDevAndId
import com.aetherquorion.cleansuperai.ads.employment.manager.mobtools.LoadManagerTools
import com.aetherquorion.cleansuperai.ads.model.AdaraImportBean
import com.aetherquorion.cleansuperai.ads.model.AdaraInfoBean
import com.aetherquorion.cleansuperai.ads.model.UploadLogInfoBean
import com.aetherquorion.cleansuperai.network.OKHttpInterceptor
import com.aetherquorion.cleansuperai.network.OKRequestManager
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.JsonUtils
import com.google.android.gms.ads.AdValue
import com.google.gson.Gson
import com.tencent.mmkv.MMKV

object InformationRecord {
    fun upPekVas(adsId: String, type: String, value: AdValue, fbIpValue: Double, pos: Long) {
        try {
            OKRequestManager.get().upValues(
                paramsPerly(adsId, type, value, fbIpValue, pos),
                valueUrl(),
                object : OKHttpInterceptor.OKHTTPRequestListener {
                    override fun okError(message: String) {
                        Log.e(TAG, message)
                    }

                    override fun okGetInfos(configInfo: String) {
                        Log.e(TAG, configInfo)
                    }
                },
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun lauchUpload() {
        try {
            OKRequestManager.get().upValues(
                lauchArgsParams(),
                valueUrl(),
                object : OKHttpInterceptor.OKHTTPRequestListener {
                    override fun okError(message: String) {
                        Log.e(TAG, message)
                    }

                    override fun okGetInfos(configInfo: String) {
                        Log.e(TAG, configInfo)
                    }
                },
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun installValues() {
        try {
            if (MMKV.defaultMMKV().getBoolean("is_install", false)) return
            OKRequestManager.get().upValues(
                installParams(),
                valueUrl(),
                object : OKHttpInterceptor.OKHTTPRequestListener {
                    override fun okError(message: String) = Unit

                    override fun okGetInfos(configInfo: String) {
                        try {
                            Gson().fromJson(configInfo, UploadLogInfoBean::class.java)?.run {
                                val payload = pzdyeqenjrku
                                    ?.hktkevvwwbw
                                    ?.hdvrhqxtjsr
                                    ?.ntpwxttl
                                    ?.xjcchmpevr
                                    ?.udjpjamfklmglw
                                if (payload?.evj == 200) {
                                    MMKV.defaultMMKV().putBoolean("is_install", true)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun paramsPerly(
        adsId: String,
        adsType: String,
        value: AdValue,
        fbIpValue: Double,
        pos: Long,
    ): HashMap<String, String> {
        val params = HashMap<String, String>()
        val context = CleanSuperAiApp.app ?: return params
        try {
            params["nijua"] = context.getString(com.aetherquorion.cleansuperai.R.string.app_name)
            params["xpcxd"] = "Android"
            params["qyeogv"] = Build.MODEL
            params["ltxbs"] = "10001"
            if (!TextUtils.isEmpty(MMKV.defaultMMKV().getString("gaid", ""))) {
                params["olbcoi"] = MMKV.defaultMMKV().getString("gaid", "") ?: ""
            }
            params["hemxkr"] = System.currentTimeMillis().toString()
            params["ddkvjn"] = currentActivityName()
            params["rbmr"] = JsonUtils.formatJson(adArgsJson(value, adsId, adsType, fbIpValue, pos))
            params["yzl"] = MMKV.defaultMMKV().getString(DATA_CONSTANT_REFER_INFO, "").orEmpty()
            params["mhesy"] = configId()
            params["jri"] = getDevAndId(context)
            params["uvjfze"] = Build.VERSION.RELEASE
            params["xmv"] = Build.MANUFACTURER
            params["qyyh"] = appVersionName()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    private fun lauchArgsParams(): HashMap<String, String> {
        val params = installParams()
        params["ltxbs"] = "10005"
        params["hemxkr"] = System.currentTimeMillis().toString()
        params["ddkvjn"] = currentActivityName()
        params["rbmr"] = JsonUtils.formatJson(lauchJson())
        return params
    }

    private fun installParams(): HashMap<String, String> {
        val params = HashMap<String, String>()
        val context = CleanSuperAiApp.app ?: return params
        try {
            params["nijua"] = context.getString(com.aetherquorion.cleansuperai.R.string.app_name)
            params["xpcxd"] = "Android"
            params["qyeogv"] = Build.MODEL
            params["ltxbs"] = "10003"
            if (!TextUtils.isEmpty(MMKV.defaultMMKV().getString("gaid", ""))) {
                params["olbcoi"] = MMKV.defaultMMKV().getString("gaid", "") ?: ""
            }
            params["yzl"] = MMKV.defaultMMKV().getString(DATA_CONSTANT_REFER_INFO, "").orEmpty()
            params["mhesy"] = configId()
            params["jri"] = getDevAndId(context)
            params["uvjfze"] = Build.VERSION.RELEASE
            params["xmv"] = Build.MANUFACTURER
            params["qyyh"] = appVersionName()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    private fun adArgsJson(value: AdValue, adId: String, adsType: String, fbIpValue: Double, pos: Long): String {
        val catPa = HashMap<String, String>()
        try {
            val context = CleanSuperAiApp.app
            catPa["abi"] = adId.replace("/", "-")
            catPa["hlylfl"] = "Show"
            catPa["rqdmgb"] = pos.toString()
            catPa["pxamc"] = AnalysisDataUtil.launchPhaseLabel()
            catPa["fktgg"] = adsType
            catPa["koolnd"] = value.valueMicros.toString()
            catPa["xdhgv"] = value.currencyCode
            catPa["itz"] = value.precisionType.toString()
            catPa["setg"] = getCountryInfo(context).lowercase()
            catPa["enFirstIns"] = getInstallSys().toString()
            catPa["enUploadValue"] = fbIpValue.toString()
            catPa["enFirstOp"] = MMKV.defaultMMKV().getLong(DATA_CONSTANT_FO, 0).toString()
            catPa["enPast"] = ((System.currentTimeMillis() - getInstallSys()) / 1000).toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return catPa.toString()
    }

    private fun lauchJson(): String {
        return hashMapOf("pxamc" to AnalysisDataUtil.launchPhaseLabel()).toString()
    }

    @JvmStatic
    fun configInfo(): String {
        return "https://dcau.cleansuperai.com"
    }

    @JvmStatic
    fun configKy(): String {
        return "BJUW46G1LHUQLLKU"
    }

    @JvmStatic
    fun configId(): String {
        return "76713380"
    }

    fun getInstallSys(): Long {
        return try {
            val context = CleanSuperAiApp.app ?: return 0
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            0
        }
    }

    fun addConfigInfoLoad(view: Activity, flag: Boolean = false) {
        if (System.currentTimeMillis() - MMKV.defaultMMKV().getLong(DATA_CONSTANT_AD_CACHE_TM, 0) > 60 * 60 * 1000) {
            OKRequestManager.get().loadNormalRequestInfo(HashMap(), configUrl(), configLoadListener(view, flag, false))
        } else {
            if (!flag) LoadManagerTools.adSpInstance.responseDatas(view)
        }
        checkAppConfig()
    }

    fun newMiddleConfigInfoLoad(view: Activity, flag: Boolean = false) {
        if (System.currentTimeMillis() - MMKV.defaultMMKV().getLong(DATA_CONSTANT_AD_CACHE_TM, 0) > 60 * 60 * 1000) {
            OKRequestManager.get().loadNormalRequestInfo(HashMap(), configUrl(), configLoadListener(view, flag, true))
        } else {
            if (TextUtils.isEmpty(LoadManagerTools.adSpInstance.interspaceStudyKpAdsId)) {
                LoadManagerTools.adSpInstance.middleConfigDispatcher(view)
            } else {
                LoadManagerTools.adSpInstance.newKepingInit(view)
            }
        }
        checkAppConfig()
    }

    fun referAdaraTranslateLoads(view: Activity, flag: Boolean = false) {
        if (
            System.currentTimeMillis() - MMKV.defaultMMKV().getLong(DATA_CONSTANT_AD_CACHE_TM, 0) > 60 * 60 * 1000 ||
            CleanSuperAiApp.statusCall
        ) {
            CleanSuperAiApp.statusCall = false
            OKRequestManager.get().flagRequestInfo(HashMap(), configUrl(), configLoadListener(view, flag, false, true))
        } else {
            if (!flag) LoadManagerTools.adSpInstance.responseDatas(view)
        }
        checkAppConfig()
    }

    private fun configLoadListener(
        view: Activity,
        flag: Boolean,
        middle: Boolean,
        refer: Boolean = false,
    ): OKHttpInterceptor.OKHTTPRequestListener {
        return object : OKHttpInterceptor.OKHTTPRequestListener {
            override fun okError(message: String) = Unit

            override fun okGetInfos(configInfo: String) {
                try {
                    Gson().fromJson(configInfo, AdaraImportBean::class.java)?.run {
                        val payload = pzdyeqenjrku
                            ?.hktkevvwwbw
                            ?.hdvrhqxtjsr
                            ?.ntpwxttl
                            ?.xjcchmpevr
                            ?.udjpjamfklmglw
                        if (payload?.lcpakw?.isNotEmpty() == true && payload.beepiw == 200) {
                            if (refer) dataMMKV()
                            MMKV.defaultMMKV().putString(DATA_CONSTANT_AD_DATA, configInfo)
                            MMKV.defaultMMKV().putLong(DATA_CONSTANT_AD_CACHE_TM, System.currentTimeMillis())
                            if (middle) {
                                LoadManagerTools.adSpInstance.middleConfigDispatcher(view, flag)
                            } else {
                                LoadManagerTools.adSpInstance.responseDatas(view, flag)
                            }
                        } else if (!flag) {
                            interspaceStudylateToMain(view)
                        }
                    }
                } catch (e: Exception) {
                    MMKV.defaultMMKV().putLong(DATA_CONSTANT_AD_CACHE_TM, 0)
                    e.printStackTrace()
                }
            }
        }
    }

    private fun dataMMKV() {
        MMKV.mmkvWithID("enmusic").clear()
        MMKV.defaultMMKV().putString(DATA_CONSTANT_AD_DATA, "")
        MMKV.defaultMMKV().putLong(DATA_CONSTANT_AD_CACHE_TM, 0)
    }

    private fun checkAppConfig() {
        if (System.currentTimeMillis() - MMKV.defaultMMKV().getLong(DATA_CONSTANT_APP_CACHE_TM, 0) <= 60 * 60 * 1000) {
            installValues()
            return
        }
        OKRequestManager.get().loadNormalRequestInfo(
            HashMap(),
            proUrl(),
            object : OKHttpInterceptor.OKHTTPRequestListener {
                override fun okError(message: String) {
                    Log.e(TAG, message)
                }

                override fun okGetInfos(configInfo: String) {
                    try {
                        Gson().fromJson(configInfo, AdaraInfoBean::class.java)?.run {
                            val payload = pzdyeqenjrku
                                ?.hktkevvwwbw
                                ?.hdvrhqxtjsr
                                ?.ntpwxttl
                                ?.xjcchmpevr
                                ?.udjpjamfklmglw
                            if (payload?.ljg == 200) {
                                payload.pupyzt?.apply {
                                    MMKV.defaultMMKV().putInt(DATA_CONSTANT_REFRESH, xnr)
                                    MMKV.defaultMMKV().putLong(DATA_CONSTANT_APP_CACHE_TM, System.currentTimeMillis())
                                    yhanof?.forEach {
                                        when (it.walj) {
                                            "standard_line" -> MMKV.defaultMMKV().putString(GLOBAL_CONSTANT_LINES, it.hqcz)
                                            "upload_coef" -> MMKV.defaultMMKV().putString(GLOBAL_CONSTANT_COEFS, it.hqcz)
                                            "window_period_time" -> MMKV.defaultMMKV().putString(GLOBAL_CONSTANT_FB_SYS, it.hqcz)
                                            "start_mode" -> MMKV.defaultMMKV().putBoolean(DATA_CONSTANT_LAUCH_MODE, it.hqcz == "2")
                                            "native_content" -> MMKV.defaultMMKV().putString(DATA_CONSTANT_NATIVE_CONTENT, it.hqcz)
                                            "total_line_first" -> MMKV.defaultMMKV().putString(VALUE_TOTAL_LINES, it.hqcz)
                                            "total_line_second" -> MMKV.defaultMMKV().putString(VALUE_TOTAL_LINES_TWO, it.hqcz)
                                            "notification_title" -> MMKV.defaultMMKV().putString(MMKV_NOTIFICATION_TITLE, it.hqcz)
                                            "notification_content" -> MMKV.defaultMMKV().putString(MMKV_NOTIFICATION_CONTENT, it.hqcz)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        MMKV.defaultMMKV().putLong(DATA_CONSTANT_APP_CACHE_TM, 0)
                        e.printStackTrace()
                    }
                }
            },
        )
        installValues()
    }

    fun interspaceStudylateToMain(activity: Activity) {
        if (!activity.isFinishing && !activity.isDestroyed) {
            activity.startActivity(Intent(activity, MainActivity::class.java))
            activity.finish()
        }
    }

    private fun configUrl(): String {
        val context = CleanSuperAiApp.app
        return "${configInfo()}/vuiyyf/qno/cfqwzw/build1/hyxe?puhmr=${configId()}&tokqoz=${getDevAndId(context)}"
    }

    private fun valueUrl(): String = "${configInfo()}/jle/pkuap/umaie/build2/fdgl"

    private fun proUrl(): String {
        val context = CleanSuperAiApp.app
        return "${configInfo()}/deaqll/diqcpk/upfp/build1/cox/dlw?dowwj=${configId()}&sscp=${getDevAndId(context)}"
    }

    private fun currentActivityName(): String = runCatching {
        ActivityUtils.getTopActivity()?.localClassName ?: ""
    }.getOrDefault("")

    private fun appVersionName(): String {
        val context = CleanSuperAiApp.app ?: return ""
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName ?: ""
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) "debug" else ""
        }
    }

    private const val TAG = "InformationRecord"
}
