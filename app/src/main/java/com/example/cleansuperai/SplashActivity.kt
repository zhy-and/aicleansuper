package com.example.cleansuperai

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.cleansuperai.ads.AdConfig
import com.example.cleansuperai.databinding.ActivitySplashBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : AppCompatActivity() {
    private val navigated = AtomicBoolean(false)
    private var timeoutJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressRing.setIndeterminate(true)

        val app = CleanSuperAiApp.from(this)
        app.isColdStartHandledBySplash = true

        timeoutJob = lifecycleScope.launch {
            delay(AdConfig.COLD_START_LOAD_TIMEOUT_MS)
            goToMain()
        }

        app.initializeMobileAds {
            runOnUiThread {
                if (isFinishing || isDestroyed || navigated.get()) return@runOnUiThread
                app.appOpenAdManager.loadAd(this) { loaded ->
                    runOnUiThread {
                        if (isFinishing || isDestroyed || navigated.get()) return@runOnUiThread
                        if (!loaded) {
                            goToMain()
                            return@runOnUiThread
                        }
                        timeoutJob?.cancel()
                        app.appOpenAdManager.showAdIfAvailable(this) {
                            goToMain()
                        }
                    }
                }
            }
        }
    }

    private fun goToMain() {
        if (!navigated.compareAndSet(false, true)) return
        timeoutJob?.cancel()
        CleanSuperAiApp.from(this).isColdStartHandledBySplash = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
