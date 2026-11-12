package com.aetherquorion.cleansuperai.ui.common

import android.content.Intent
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.MainActivity
import android.widget.TextView

class StoragePermissionGuideActivity : AppCompatActivity() {
    companion object { const val EXTRA_ACCESS = "usage_access" }
    private var toggleAnimator: ObjectAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val permissionWatcher = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            val granted = if (intent.getBooleanExtra(EXTRA_ACCESS, false)) {
                FeaturePermissionCoordinator.hasAccess(this@StoragePermissionGuideActivity, FeatureAccess.USAGE)
            } else android.os.Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
            if (granted) {
                startActivity(Intent(this@StoragePermissionGuideActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
                finish()
            } else {
                handler.postDelayed(this, 400)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_permission_guide)
        if (intent.getBooleanExtra(EXTRA_ACCESS, false)) {
            findViewById<TextView>(R.id.permissionGuideMessage).setText(R.string.traffic_guide_message)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        findViewById<android.view.View>(R.id.scrimView).setOnClickListener { finish() }
        val travel = 26f * resources.displayMetrics.density
        toggleAnimator = ObjectAnimator.ofFloat(findViewById(R.id.toggleThumb), "translationX", 0f, travel).apply {
            duration = 700
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
        handler.post(permissionWatcher)
    }

    override fun onDestroy() {
        handler.removeCallbacks(permissionWatcher)
        toggleAnimator?.cancel()
        super.onDestroy()
    }
}
