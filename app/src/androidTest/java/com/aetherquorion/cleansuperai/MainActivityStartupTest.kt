package com.aetherquorion.cleansuperai

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aetherquorion.cleansuperai.ui.home.HomeFragment
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityStartupTest {
    @Test
    fun coldStartDisplaysHomeFragment() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragmentContainer) is HomeFragment)
            }
        }
    }
}
