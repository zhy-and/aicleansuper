package com.aetherquorion.cleansuperai.ads.employment.manager

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.aetherquorion.cleansuperai.BuildConfig
import com.aetherquorion.cleansuperai.CleanSuperAiApp
import com.aetherquorion.cleansuperai.ads.analysis.EasyMethodManager
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_REFER_INFO
import com.aetherquorion.cleansuperai.ads.employment.GLOBAL_CONSTANT_COEFS
import com.aetherquorion.cleansuperai.ads.employment.GLOBAL_CONSTANT_FB_SYS
import com.aetherquorion.cleansuperai.ads.employment.GLOBAL_CONSTANT_LINES
import com.aetherquorion.cleansuperai.ads.employment.manager.studio.InformationRecord
import com.appsflyer.AdRevenueScheme
import com.appsflyer.AFAdRevenueData
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import com.appsflyer.MediationNetwork
import com.google.android.gms.ads.AdValue
import com.tencent.mmkv.MMKV
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object NativeTools {
    @JvmStatic
    fun interspaceStudyEncryptResponseData(text: String): String {
        return try {
            val decrypt = dataDecrypt(
                Base64.decode(text.toByteArray(StandardCharsets.UTF_8), Base64.DEFAULT),
                InformationRecord.configKy().toByteArray(StandardCharsets.UTF_8),
            )
            String(decrypt, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "null"
        }
    }

    @JvmStatic
    fun getCountryInfo(context: Context?): String {
        if (context == null) return "NULL"
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val countryIso = telephonyManager.networkCountryIso
        return if (countryIso.isNullOrEmpty()) {
            context.resources.configuration.locales[0].country
        } else {
            countryIso
        }
    }

    @JvmStatic
    fun getDevAndId(context: Context?): String {
        if (context == null || BuildConfig.DEBUG) return "8888"
        @SuppressLint("HardwareIds")
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val id = androidId + Build.SERIAL + Build.TIME
        return try {
            interspaceStudyMd5(id)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            id
        }
    }

    @JvmStatic
    fun createFaceListener(adValue: AdValue, adType: String, adId: String, pos: Long) {
        try {
            Log.d(TAG, "-------- 收到 AdMob onPaidEvent 回调 --------")
            var configValues = 0.0
            val distanceTimes = MMKV.defaultMMKV().getString(GLOBAL_CONSTANT_FB_SYS, "100")!!.toLong()
            if (System.currentTimeMillis() - InformationRecord.getInstallSys() <= distanceTimes * 60 * 60 * 1000) {
                val value = BigDecimal(adValue.valueMicros.toString()).divide(BigDecimal(1000000.0))
                Log.d(TAG, "【价值换算】原始微美金: ${adValue.valueMicros}, 换算后: $value ${adValue.currencyCode}")
                Log.d(TAG, "【精度类型】Type: ${adValue.precisionType}")
                val lines = MMKV.defaultMMKV().getString(GLOBAL_CONSTANT_LINES, "0")!!.toDouble()
                if (value.toDouble() >= lines) {
                    val coef = MMKV.defaultMMKV().getString(GLOBAL_CONSTANT_COEFS, "1")!!.toDouble()
                    Currency.getInstance(adValue.currencyCode)
                    configValues = BigDecimal(coef * value.toDouble()).toDouble()
                    Log.d(TAG, "【上报事件】AppsFlyer purchase/ad revenue, type=$adType, pos=$pos, adId=$adId, value=$configValues")

                    val appsFlyer = AppsFlyerLib.getInstance()
                    val eventValues = hashMapOf<String, Any>(
                        AFInAppEventParameterName.CURRENCY to adValue.currencyCode,
                        AFInAppEventParameterName.REVENUE to configValues,
                    )
                    appsFlyer.logEvent(
                        CleanSuperAiApp.app,
                        AFInAppEventType.PURCHASE,
                        eventValues,
                    )

                    val adRevenueData = AFAdRevenueData(
                        "AdMob",
                        MediationNetwork.GOOGLE_ADMOB,
                        adValue.currencyCode,
                        configValues,
                    )
                    val additionalParameters = hashMapOf<String, Any>(
                        AdRevenueScheme.COUNTRY to Locale.getDefault().country,
                        AdRevenueScheme.AD_UNIT to adId,
                        AdRevenueScheme.AD_TYPE to adType,
                        AdRevenueScheme.PLACEMENT to "place",
                    )
                    appsFlyer.logAdRevenue(adRevenueData, additionalParameters)

                    // TODO: restore Facebook purchase upload if the target project keeps Facebook analytics.
                } else {
                    Log.d(TAG, "【阈值未达】当前价值 $value < 阈值 $lines，仅上传广告价值日志")
                }
            } else {
                Log.d(TAG, "【窗口期外】跳过 AppsFlyer purchase，仅上传广告价值日志")
            }
            Log.d(TAG, "【上报广告价值】type=$adType, pos=$pos, adId=$adId, micros=${adValue.valueMicros}")
            InformationRecord.upPekVas(adId, adType, adValue, configValues, pos)
            Log.d(TAG, "-------- 本次回调处理完成 --------")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    fun interspaceStudyMd5(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray())
        val sb = StringBuilder()
        for (b in digest) {
            val digestInt = b.toInt() and 0xff
            val hexString = Integer.toHexString(digestInt)
            if (hexString.length < 2) sb.append(0)
            sb.append(hexString)
        }
        return sb.substring(8, 24)
    }

    @JvmStatic
    fun createCommonHeaders(): HashMap<String, String> {
        val context = CleanSuperAiApp.app ?: return HashMap()
        val params = HashMap<String, String>()
        try {
            val inMillis = Calendar.getInstance().timeInMillis
            val author = EasyMethodManager.interspaceStudylateMd5Encrypt32Lower(
                InformationRecord.configId() + context.packageName + InformationRecord.configKy() + inMillis,
            )
            params["pkg"] = context.packageName
            params["timestamp"] = inMillis.toString()
            params["dynKeyFlag"] = "101"
            if (!TextUtils.isEmpty(author)) {
                params["Authorization"] = author
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    @JvmStatic
    fun configReferConfig(): HashMap<String, String> {
        val context = CleanSuperAiApp.app ?: return HashMap()
        val params = HashMap<String, String>()
        try {
            val inMillis = Calendar.getInstance().timeInMillis
            val author = EasyMethodManager.interspaceStudylateMd5Encrypt32Lower(
                InformationRecord.configId() + context.packageName + InformationRecord.configKy() + inMillis,
            )
            var infos = MMKV.defaultMMKV().getString(DATA_CONSTANT_REFER_INFO, "")
            if (infos.isNullOrEmpty()) {
                infos = InformationRecord.configId() + "_normal"
            }
            params["Original"] = infos
            params["pkg"] = context.packageName
            params["timestamp"] = inMillis.toString()
            params["dynKeyFlag"] = "101"
            if (!TextUtils.isEmpty(author)) {
                params["Authorization"] = author
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return params
    }

    fun dataDecrypt(signaldata: ByteArray, key: ByteArray): ByteArray {
        if (key.size != 16) {
            throw RuntimeException("Invalid AES key length (must be 16 bytes)")
        }
        try {
            val keySpec = SecretKeySpec(key, "AES")
            val keySpec1 = SecretKeySpec(keySpec.encoded, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = IvParameterSpec(key)
            cipher.init(Cipher.DECRYPT_MODE, keySpec1, iv)
            return cipher.doFinal(signaldata)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("decrypt fail!", e)
        }
    }

    private const val TAG = "NativeTools"
}
