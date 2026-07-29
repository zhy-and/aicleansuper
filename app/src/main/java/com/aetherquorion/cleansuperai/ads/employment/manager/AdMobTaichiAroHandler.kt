package com.aetherquorion.cleansuperai.ads.employment.manager

import android.os.Bundle
import android.util.Log
import com.aetherquorion.cleansuperai.CleanSuperAiApp
import com.aetherquorion.cleansuperai.ads.employment.VALUE_TOTAL_LINES
import com.aetherquorion.cleansuperai.ads.employment.VALUE_TOTAL_LINES_TWO
import com.google.android.gms.ads.AdValue
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.mmkv.MMKV

/**
 * AdMob Mediation Taichi&ARO 接入.
 */
object AdMobTaichiAroHandler {
    private const val TAG = "AdMobTaichiAro"
    private const val KEY_TAICHI_TROAS_CACHE = "TaichiTroasCache"
    private const val KEY_TAICHI_TROAS_CACHE_TWO = "TaichiTroasCacheTwo"

    private val appContext by lazy {
        CleanSuperAiApp.app!!
    }

    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Log.d(TAG, "FirebaseAnalytics 初始化完成")
        FirebaseAnalytics.getInstance(appContext)
    }

    private val mmkv: MMKV by lazy {
        Log.d(TAG, "MMKV 初始化完成")
        MMKV.defaultMMKV()
    }

    fun handlePaidEvent(adValue: AdValue?) {
        try {
            Log.d(TAG, "-------- 收到 AdMob onPaidEvent 回调 --------")

            adValue ?: run {
                Log.w(TAG, "adValue 为空，跳过处理")
                return
            }

            val microsValue = adValue.valueMicros
            val currentImpressionRevenue = microsValue.toDouble() / 1_000_000
            Log.d(TAG, "【价值换算】原始微美金: $microsValue, 换算后美金: $currentImpressionRevenue")

            val precisionType = parsePrecisionType(adValue.precisionType)
            Log.d(TAG, "【精度类型】Type: ${adValue.precisionType} -> $precisionType")

            logTaichiImpressionEvent(currentImpressionRevenue, precisionType)
            accumulateAndCheckTroas(currentImpressionRevenue)
            accumulateAndCheckTroas2(currentImpressionRevenue)

            Log.d(TAG, "-------- 本次回调处理完成 --------\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parsePrecisionType(precisionType: Int): String {
        return when (precisionType) {
            0 -> "UNKNOWN"
            1 -> "ESTIMATED"
            2 -> "PUBLISHER_PROVIDED"
            3 -> "PRECISE"
            else -> "Invalid"
        }
    }

    private fun logTaichiImpressionEvent(revenue: Double, precisionType: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "USD")
            putDouble(FirebaseAnalytics.Param.VALUE, revenue)
            putString("precisionType", precisionType)
        }

        Log.d(TAG, "【上报事件】Ad_Impression_Revenue, 价值: $revenue, 精度: $precisionType")
        firebaseAnalytics.logEvent("Ad_Impression_Revenue", params)
    }

    private fun accumulateAndCheckTroas(currentRevenue: Double) {
        try {
            val previousCache = mmkv.getFloat(KEY_TAICHI_TROAS_CACHE, 0f)
            Log.d(TAG, "【缓存读取】历史累计: $previousCache")

            val currentCache = (previousCache + currentRevenue).toFloat()
            Log.d(TAG, "【缓存更新】累加本次: $currentRevenue, 当前累计: $currentCache")

            val lines = mmkv.getString(VALUE_TOTAL_LINES, "0")?.toDoubleOrNull() ?: 0.0
            Log.w(TAG, "配置total 为 $lines ---")

            if (currentCache >= lines) {
                Log.w(TAG, "【阈值触发】当前累计 $currentCache >= 阈值 $lines，准备上报核心事件！")

                val roasBundle = Bundle().apply {
                    putDouble(FirebaseAnalytics.Param.VALUE, currentCache.toDouble())
                    putString(FirebaseAnalytics.Param.CURRENCY, "USD")
                }

                Log.d(TAG, "【上报事件】Total_Ads_Revenue_001, 上报价值: $currentCache")
                firebaseAnalytics.logEvent("Total_Ads_Revenue_001", roasBundle)

                Log.d(TAG, "【缓存清零】事件上报完成，缓存已重置为 0")
                mmkv.putFloat(KEY_TAICHI_TROAS_CACHE, 0f)
            } else {
                Log.d(TAG, "【阈值未达】当前累计 $currentCache < 阈值 $lines，仅更新缓存，暂不上报")
                mmkv.putFloat(KEY_TAICHI_TROAS_CACHE, currentCache)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun accumulateAndCheckTroas2(currentRevenue: Double) {
        try {
            val previousCache = mmkv.getFloat(KEY_TAICHI_TROAS_CACHE_TWO, 0f)
            Log.d(TAG, "【缓存2读取】历史累计: $previousCache")

            val currentCache = (previousCache + currentRevenue).toFloat()
            Log.d(TAG, "【缓存2更新】累加本次: $currentRevenue, 当前累计: $currentCache")

            val lines = mmkv.getString(VALUE_TOTAL_LINES_TWO, "0")?.toDoubleOrNull() ?: 0.0
            Log.w(TAG, "配置2 total 为 $lines ---")

            if (currentCache >= lines) {
                Log.w(TAG, "【阈值2 触发】当前累计 $currentCache >= 阈值 $lines，准备上报核心事件！")

                val roasBundle = Bundle().apply {
                    putDouble(FirebaseAnalytics.Param.VALUE, currentCache.toDouble())
                    putString(FirebaseAnalytics.Param.CURRENCY, "USD")
                }

                Log.d(TAG, "【上报事件】Total_Ads_Revenue_002, 上报价值: $currentCache")
                firebaseAnalytics.logEvent("Total_Ads_Revenue_002", roasBundle)

                Log.d(TAG, "【缓存2清零】事件上报完成，缓存2已重置为 0")
                mmkv.putFloat(KEY_TAICHI_TROAS_CACHE_TWO, 0f)
            } else {
                Log.d(TAG, "【阈值2未达】当前累计 $currentCache < 阈值 $lines，仅更新缓存，暂不上报")
                mmkv.putFloat(KEY_TAICHI_TROAS_CACHE_TWO, currentCache)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
