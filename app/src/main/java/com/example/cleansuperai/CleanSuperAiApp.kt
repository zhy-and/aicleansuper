package com.example.cleansuperai

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.cleansuperai.ads.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class CleanSuperAiApp : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    private var currentActivity: Activity? = null
    private val mobileAdsInitialized = AtomicBoolean(false)
    private val mobileAdsInitializing = AtomicBoolean(false)
    private val mobileAdsListeners = CopyOnWriteArrayList<() -> Unit>()

    /** 冷启动由 SplashActivity 负责展示，避免与热启动逻辑重复。 */
    @Volatile
    var isColdStartHandledBySplash: Boolean = true

    override fun onCreate() {
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        initializeMobileAds()
    }

    fun initializeMobileAds(onComplete: (() -> Unit)? = null) {
        if (mobileAdsInitialized.get()) {
            onComplete?.invoke()
            return
        }
        if (onComplete != null) {
            mobileAdsListeners.add(onComplete)
        }
        if (!mobileAdsInitializing.compareAndSet(false, true)) {
            return
        }
        MobileAds.initialize(this) {
            mobileAdsInitialized.set(true)
            mobileAdsInitializing.set(false)
            val listeners = mobileAdsListeners.toList()
            mobileAdsListeners.clear()
            listeners.forEach { it() }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (isColdStartHandledBySplash) {
            return
        }
        val activity = currentActivity ?: return
        if (activity is SplashActivity) {
            return
        }
        appOpenAdManager.showAdIfAvailable(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    companion object {
        fun from(activity: Activity): CleanSuperAiApp {
            return activity.application as CleanSuperAiApp
        }
    }
}
