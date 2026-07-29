package com.aetherquorion.cleansuperai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_FO
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_LAUCH_MODE
import com.aetherquorion.cleansuperai.ads.employment.DATA_CONSTANT_REFER_INFO
import com.aetherquorion.cleansuperai.ads.employment.MMKV_NOTIFICATION_CONTENT
import com.aetherquorion.cleansuperai.ads.employment.MMKV_NOTIFICATION_TITLE
import com.aetherquorion.cleansuperai.ads.employment.checkRules
import com.aetherquorion.cleansuperai.network.OKRequestManager
import com.appsflyer.AppsFlyerLib
import com.blankj.utilcode.util.ActivityUtils
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.tencent.mmkv.MMKV

class CleanSuperAiApp : Application() {
    var anyCount = 0
    private var defaultLifecycleObserver: DefaultLifecycleObserver? = null
    private var referrerClient: InstallReferrerClient? = null
    private var pendingBackgroundPushRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        app = this
        initAppFly()
        startNorStatus = true
        try {
            if (!checkRules()) {
                MobileAds.initialize(this)
            }
            OKRequestManager.get().netInit()
            if (MMKV.defaultMMKV().getLong(DATA_CONSTANT_FO, 0) == 0L) {
                MMKV.defaultMMKV().putLong(DATA_CONSTANT_FO, System.currentTimeMillis())
            }
            defaultLifecycleObserver = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    cancelBackgroundPush()
                    try {
                        isIntercept = false
                        if (!MMKV.defaultMMKV().getBoolean(DATA_CONSTANT_LAUCH_MODE, true) || !isFirstLauch) {
                            isFirstLauch = true
                            return
                        }
                        anyCount += 1
                        if (anyCount >= 15) return
                        ActivityUtils.startActivity(HotActivity::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onStop(owner: LifecycleOwner) {
                    try {
                        if (!MMKV.defaultMMKV().getBoolean(DATA_CONSTANT_LAUCH_MODE, true)) return
                        if (ActivityUtils.isActivityExistsInStack(AdActivity::class.java)) {
                            isIntercept = true
                            ActivityUtils.finishActivity(AdActivity::class.java)
                        }
                        if (ActivityUtils.isActivityExistsInStack(HotActivity::class.java)) {
                            ActivityUtils.finishActivity(HotActivity::class.java)
                        }
                        scheduleBackgroundPush()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            defaultLifecycleObserver?.let {
                ProcessLifecycleOwner.get().lifecycle.addObserver(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        createReferChannel()
    }

    private fun initAppFly() {
        AppsFlyerLib.getInstance().init("TX2uVpH9Wi74UNhFV9RLvY", null, this)
        AppsFlyerLib.getInstance().start(this)
        //todo
        AppsFlyerLib.getInstance().setDebugLog(true)
    }

    private fun scheduleBackgroundPush() {
        cancelBackgroundPush()
        val runnable = Runnable {
            pendingBackgroundPushRunnable = null
            val title = MMKV.defaultMMKV().getString(MMKV_NOTIFICATION_TITLE, "Your Weekly Discovery is Here!")
            val content = MMKV.defaultMMKV().getString(MMKV_NOTIFICATION_CONTENT, "Content!")
            createNotification(title, content, title)
        }
        pendingBackgroundPushRunnable = runnable
        mainHandler.postDelayed(runnable, 5000)
    }

    private fun cancelBackgroundPush() {
        pendingBackgroundPushRunnable?.let {
            mainHandler.removeCallbacks(it)
            pendingBackgroundPushRunnable = null
        }
    }

    private fun createNotification(messageTitle: String?, messageBody: String?, title: String? = "") {
        try {
            val intent = Intent(this, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val uniqueInt = (System.currentTimeMillis() and 0xffL).toInt()
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
                NotificationCompat.Builder(this, CHANNEL_ID)
            } else {
                NotificationCompat.Builder(this)
            }
            notificationBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle(if (!messageTitle.isNullOrEmpty()) title else getString(R.string.app_name))
                .setContentText(messageBody)
                .setContentIntent(pendingIntent)
            notificationManager.notify(uniqueInt, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createReferChannel() {
        try {
            referrerClient = InstallReferrerClient.newBuilder(this).build()
            referrerClient?.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    statusCall = true
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            try {
                                referrerClient?.installReferrer?.let {
                                    val referrerUrl = it.installReferrer
                                    if (!TextUtils.isEmpty(referrerUrl)) {
                                        MMKV.defaultMMKV().putString(DATA_CONSTANT_REFER_INFO, referrerUrl)
                                        referrerClient?.endConnection()
                                    }
                                }
                            } catch (e: Exception) {
                                referrerClient?.endConnection()
                                e.printStackTrace()
                            }
                        }
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE,
                        -> referrerClient?.endConnection()
                    }
                }

                override fun onInstallReferrerServiceDisconnected() = Unit
            })
            if (TextUtils.isEmpty(MMKV.defaultMMKV().getString("gaid", ""))) {
                Thread {
                    runCatching {
                        val advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(this)
                        MMKV.defaultMMKV().putString("gaid", advertisingIdInfo.id)
                    }
                }.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == TRIM_MEMORY_UI_HIDDEN && !isClicked && !MMKV.defaultMMKV().getBoolean(DATA_CONSTANT_LAUCH_MODE, true)) {
            ActivityUtils.finishAllActivities()
        }
    }

    companion object {
        var umengLoadFlag = false
        var statusCall = false
        @JvmStatic var startNorStatus = false
        @JvmStatic var app: Context? = null
        @JvmStatic var isClicked = false
        @JvmStatic var isFirstLauch = false
        @JvmStatic var isIntercept = false
        private const val CHANNEL_ID = "10001"
        private const val CHANNEL_NAME = "push_channel"
    }
}
