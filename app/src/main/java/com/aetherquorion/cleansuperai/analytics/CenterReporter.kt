package com.aetherquorion.cleansuperai.analytics

import android.content.Context
import android.os.Build
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_FO
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_REFER_INFO
import com.aetherquorion.cleansuperai.ads.employment.manager.NativeTools
import com.aetherquorion.cleansuperai.ads.employment.manager.studio.InformationRecord
import com.aetherquorion.cleansuperai.config.UserSegmentHelper
import com.aetherquorion.cleansuperai.network.OKHttpInterceptor
import com.aetherquorion.cleansuperai.network.OKRequestManager
import com.blankj.utilcode.util.ActivityUtils
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared transport for Purgu's 10008/10009/10018/10019 envelopes. */
object CenterReporter {
    fun report(context: Context, type: String, eventName: String, extra: Map<String, String> = emptyMap()) {
        runCatching {
            val app = context.applicationContext
            val store = MMKV.defaultMMKV()
            val now = System.currentTimeMillis()
            val install = app.packageManager.getPackageInfo(app.packageName, 0).firstInstallTime
            val version = app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()
            val args = linkedMapOf("event_name" to eventName, "page" to ActivityUtils.getTopActivity()?.javaClass?.simpleName.orEmpty(),
                "codp" to AnalysisDataUtil.launchPhaseLabel(), "user_segment" to UserSegmentHelper.currentSegment().jsonKey,
                "event_time" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now)),
                "sdk" to Build.VERSION.SDK_INT.toString(), "app_ver" to version,
                "kciyl" to runCatching { NativeTools.getCountryInfo(app).lowercase() }.getOrDefault(""),
                "adFirstOp" to store.getLong(DATA_CONSTANT_FO, 0L).toString(),
                "adPast" to if (install > 0) ((now - install) / 1000).toString() else "0",
                "adFirstIns" to install.toString())
            extra.filterValues { it.isNotEmpty() }.forEach { (k, v) -> args[k] = v }
            val json = Gson().toJson(args)
            val params = hashMapOf("mbf" to app.applicationInfo.loadLabel(app.packageManager).toString(),
                "fscamq" to "Android", "vbse" to Build.MODEL.orEmpty(), "gsiywi" to Build.BRAND.orEmpty(),
                "ejx" to type, "lmbhy" to now.toString(), "sansx" to ActivityUtils.getTopActivity()?.localClassName.orEmpty(),
                "args" to json, "ovp" to json, "aijxqg" to store.getString(DATA_CONSTANT_REFER_INFO, "").orEmpty(),
                "gpm" to InformationRecord.configId(), "mxmv" to NativeTools.getDevAndId(app),
                "evzxt" to Build.VERSION.RELEASE.orEmpty(), "uoeyag" to version)
            store.getString("gaid", "")?.takeIf { it.isNotEmpty() }?.let { params["xfb"] = it }
            OKRequestManager.get().upValues(params, "${InformationRecord.configInfo()}/jle/pkuap/umaie/build2/fdgl",
                object : OKHttpInterceptor.OKHTTPRequestListener {
                    override fun okError(message: String) { android.util.Log.w("CenterReporter", "$type: $message") }
                    override fun okGetInfos(configInfo: String) { }
                })
        }.onFailure { android.util.Log.e("CenterReporter", "$type $eventName", it) }
    }
}
