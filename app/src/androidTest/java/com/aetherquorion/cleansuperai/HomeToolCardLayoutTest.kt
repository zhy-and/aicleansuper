package com.aetherquorion.cleansuperai

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aetherquorion.cleansuperai.databinding.FragmentHomeBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class HomeToolCardLayoutTest {
    @Test
    fun longVietnameseTitlesFitBelowIconsAtLargeFontSize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val app = instrumentation.targetContext
            val configuration = Configuration(app.resources.configuration).apply {
                setLocale(Locale.forLanguageTag("vi"))
                fontScale = 1.5f
            }
            val context = ContextThemeWrapper(
                app.createConfigurationContext(configuration), R.style.Theme_Cleansuperai,
            )
            val home = FragmentHomeBinding.inflate(LayoutInflater.from(context))
            val titles = listOf(
                home.cardCacheCleanup to R.string.home_cache_cleanup,
                home.cardNetworkTraffic to R.string.home_network_traffic,
                home.cardVault to R.string.home_private_vault,
            )
            for ((card, label) in titles) card.tvFeatureTitle.setText(label)

            val width = (320 * context.resources.displayMetrics.density).roundToInt()
            home.root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            home.root.layout(0, 0, width, home.root.measuredHeight)

            assertEquals(1, titles.map { (card, _) -> card.root.height }.distinct().size)
            for ((card, label) in titles) {
                val title = card.tvFeatureTitle
                val textLayout = checkNotNull(title.layout)
                assertTrue(context.getString(label), title.top > card.iconContainer.bottom)
                assertTrue(context.getString(label), title.bottom <= card.root.height)
                assertEquals(title.text.length, textLayout.getLineEnd(textLayout.lineCount - 1))
                assertTrue((0 until textLayout.lineCount).all { textLayout.getEllipsisCount(it) == 0 })
            }
        }
    }
}
