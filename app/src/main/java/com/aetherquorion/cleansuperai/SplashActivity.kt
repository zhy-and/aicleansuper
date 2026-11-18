package com.aetherquorion.cleansuperai

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil
import com.aetherquorion.cleansuperai.ads.consent.GoogleMobileAdsConsentManager
import com.aetherquorion.cleansuperai.ads.employment.checkRules
import com.aetherquorion.cleansuperai.ads.employment.manager.mobtools.LoadManagerTools
import com.aetherquorion.cleansuperai.ads.employment.manager.studio.InformationRecord.addConfigInfoLoad
import com.aetherquorion.cleansuperai.ads.employment.manager.studio.InformationRecord.lauchUpload
import com.aetherquorion.cleansuperai.ads.model.InfoDestroySp
import com.aetherquorion.cleansuperai.databinding.ActivitySplashBinding
import com.aetherquorion.cleansuperai.notification.NotificationPermissionFlow
import com.aetherquorion.cleansuperai.analytics.PathAnalytics
import com.aetherquorion.cleansuperai.analytics.PathEvent
import com.aetherquorion.cleansuperai.keepalive.KeepAliveCoordinator
import com.aetherquorion.cleansuperai.keepalive.KeepAliveReason
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : AppCompatActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        NotificationPermissionFlow.recordResult(granted)
        PathAnalytics.track(this, if (granted) PathEvent.NOTIF_PERM_GRANTED else PathEvent.NOTIF_PERM_DENIED)
        if (granted) KeepAliveCoordinator.bootstrap(this, KeepAliveReason.MANUAL)
    }
    private var messages: Handler? = null
    private val adsInitializeCalled = AtomicBoolean(false)
    private lateinit var binding: ActivitySplashBinding
    private lateinit var launchProgress: LaunchProgress

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PathAnalytics.track(this, PathEvent.COLD_START)
        AnalysisDataUtil.markColdLaunch()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        messages = Handler(Looper.getMainLooper())
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        launchProgress = LaunchProgress(binding.progressRing, binding.tvLoadingStatus)
        launchProgress.start(12_000)
        if (NotificationPermissionFlow.shouldRequest(android.os.Build.VERSION.SDK_INT, NotificationPermissionFlow.isGranted(this))) {
            PathAnalytics.track(this, PathEvent.NOTIF_PERM_SHOWN)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        try {
            if (checkRules()) {
                initUmSDK()
            } else {
                CleanSuperAiApp.umengLoadFlag = true
                jmHome()
            }
            addConfigInfoLoad(this, false)
            lauchUpload()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initUmSDK() {
        val manager = GoogleMobileAdsConsentManager.getInstance(CleanSuperAiApp.app)
        manager.gatherConsent(this) { consentError ->
            consentError?.let { Log.e("SplashActivity", "${it.errorCode}: ${it.message}") }
            CleanSuperAiApp.umengLoadFlag = true
            initializeMobileAdsSdk()
        }
        if (manager.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (adsInitializeCalled.getAndSet(true)) return
        if (messages == null) return
        LoadManagerTools.adSpInstance.createKepingInit(this)
        runOnUiThread {
            if (!AnalysisDataUtil.kpCanLoadInterceptor()) {
                LoadManagerTools.adSpInstance.interspaceStudyLoadDefaultCyAds(this)
            }
            LoadManagerTools.adSpInstance.interspaceStudyLoadDefaultNativeAds()
        }
        jmHome()
    }

    private fun jmHome() {
        messages?.postDelayed({
            launchProgress.complete()
            startActivity(Intent(this, MainActivity::class.java).apply {
                intent?.getStringExtra(MainActivity.EXTRA_WIDGET_ROUTE)?.let {
                    putExtra(MainActivity.EXTRA_WIDGET_ROUTE, it)
                    intent?.removeExtra(MainActivity.EXTRA_WIDGET_ROUTE)
                }
            })
            finish()
        }, 12_000)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun remove(re: InfoDestroySp?) {
        if (re?.isShow == true) {
            messages?.removeCallbacksAndMessages(null)
            messages = null
        }
    }

    override fun onDestroy() {
        launchProgress.stop()
        if (!isChangingConfigurations) {
            PendingShortcutRoute.remember(intent?.getStringExtra(MainActivity.EXTRA_WIDGET_ROUTE))
            PendingShortcutRoute.dispatch(this)
        }
        AnalysisDataUtil.markHotLaunch()
        super.onDestroy()
        messages?.removeCallbacksAndMessages(null)
        messages = null
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }
}
