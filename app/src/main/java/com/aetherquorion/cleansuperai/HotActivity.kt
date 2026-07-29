package com.aetherquorion.cleansuperai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.aetherquorion.cleansuperai.ads.analysis.AnalysisDataUtil
import com.aetherquorion.cleansuperai.ads.employment.interspaceStudyKepHot
import com.aetherquorion.cleansuperai.ads.employment.manager.studio.InformationRecord.lauchUpload
import com.aetherquorion.cleansuperai.ads.employment.manager.studio.InformationRecord.newMiddleConfigInfoLoad
import com.aetherquorion.cleansuperai.ads.model.InfoDestroyCentre
import com.aetherquorion.cleansuperai.databinding.ActivitySplashBinding
import com.blankj.utilcode.util.ActivityUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HotActivity : AppCompatActivity() {
    private var messages: Handler? = null
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalysisDataUtil.markHotLaunch()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        messages = Handler(Looper.getMainLooper())
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.progressRing.setIndeterminate(true)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        initData()
        CleanSuperAiApp.umengLoadFlag = true
        jmHome()
    }

    private fun initData() {
        try {
            newMiddleConfigInfoLoad(this, false)
            lauchUpload()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        messages?.removeCallbacksAndMessages(null)
        messages = null
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    private fun jmHome() {
        if (AnalysisDataUtil.interspaceStudyRules(interspaceStudyKepHot())) {
            navigateHomeAndFinish()
            return
        }
        messages?.postDelayed({
            if (!isFinishing && !isDestroyed) navigateHomeAndFinish()
        }, 11_000)
    }

    private fun navigateHomeAndFinish() {
        if (!ActivityUtils.isActivityExistsInStack(MainActivity::class.java)) {
            ActivityUtils.startActivity(MainActivity::class.java)
        }
        finish()
    }

    private fun destroyHd() {
        binding.progressRing.setIndeterminate(false)
        binding.progressRing.visibility = View.GONE
        messages?.removeCallbacksAndMessages(null)
        messages = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun remove(re: InfoDestroyCentre?) {
        if (re?.isShow == true) {
            destroyHd()
        }
    }
}
