package com.aetherquorion.cleansuperai

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.aetherquorion.cleansuperai.ads.ListenerTrans
import com.aetherquorion.cleansuperai.ads.banner.BannerView
import com.aetherquorion.cleansuperai.ads.employment.cleanCenterInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.cleanCenterNativeAd
import com.aetherquorion.cleansuperai.ads.employment.compressInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.compressNativeAd
import com.aetherquorion.cleansuperai.ads.employment.contactsInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.contactsNativeAd
import com.aetherquorion.cleansuperai.ads.employment.detailInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.detailNativeAd
import com.aetherquorion.cleansuperai.ads.employment.duplicateVideoInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.duplicateVideoNativeAd
import com.aetherquorion.cleansuperai.ads.employment.homeNativeAd
import com.aetherquorion.cleansuperai.ads.employment.largeVideoInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.largeVideoNativeAd
import com.aetherquorion.cleansuperai.ads.employment.screenshotListInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.screenshotListNativeAd
import com.aetherquorion.cleansuperai.ads.employment.settingInter
import com.aetherquorion.cleansuperai.ads.employment.settingNative
import com.aetherquorion.cleansuperai.ads.employment.similarImagesInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.similarImagesNativeAd
import com.aetherquorion.cleansuperai.ads.employment.swipeDetailInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.swipeDetailNativeAd
import com.aetherquorion.cleansuperai.ads.employment.swipeInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.swipeNativeAd
import com.aetherquorion.cleansuperai.ads.employment.toolsInterstitialAd
import com.aetherquorion.cleansuperai.ads.employment.toolsNativeAd
import com.aetherquorion.cleansuperai.ads.employment.manager.mobtools.LoadManagerTools
import com.aetherquorion.cleansuperai.databinding.ActivityMainBinding
import com.aetherquorion.cleansuperai.ui.compress.CompressFragment
import com.aetherquorion.cleansuperai.ui.home.HomeFragment
import com.aetherquorion.cleansuperai.ui.profile.LanguageSettingsFragment
import com.aetherquorion.cleansuperai.ui.profile.ProfileFragment
import com.aetherquorion.cleansuperai.ui.swipe.SwipeFragment
import com.aetherquorion.cleansuperai.ui.tools.ToolsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var systemBottomInset = 0
    private var currentAdHost: FrameLayout? = null
    private var currentNativePosition: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomPadding = binding.bottomNavigation.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            systemBottomInset = systemBars.bottom
            v.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.paddingLeft,
                binding.bottomNavigation.paddingTop,
                binding.bottomNavigation.paddingRight,
                bottomPadding + systemBars.bottom,
            )
            applyDetailChrome()
            insets
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.menu_home -> HomeFragment()
                R.id.menu_swipe -> SwipeFragment()
                R.id.menu_compress -> CompressFragment()
                R.id.menu_tools -> ToolsFragment()

                else -> null
            }
            fragment?.let {
                tabInterstitialPosition(item.itemId)?.let(::showInterstitial)
                supportFragmentManager.popBackStack(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
                )
                showFragment(it, item.itemId.toString())
                supportFragmentManager.executePendingTransactions()
                showNativeAd(tabNativePosition(item.itemId))
                true
            } ?: false
        }

        supportFragmentManager.addOnBackStackChangedListener {
            applyDetailChrome()
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.menu_home
        }
        showNativeAd(homeNativeAd())
    }

    /**
     * Detail pages hide the bottom nav. Re-anchor the fragment container to the parent bottom
     * and apply the system gesture/nav inset so NestedScrollView / RecyclerView get a bounded
     * viewport shorter than their content (otherwise tall pages cannot scroll).
     */
    private fun applyDetailChrome() {
        val showingDetail = supportFragmentManager.backStackEntryCount > 0
        binding.bottomNavigation.isVisible = !showingDetail

        val params = binding.fragmentContainer.layoutParams as ConstraintLayout.LayoutParams
        if (showingDetail) {
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            binding.fragmentContainer.updatePadding(bottom = systemBottomInset)
        } else {
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = binding.bottomNavigation.id
            binding.fragmentContainer.updatePadding(bottom = 0)
        }
        params.topToBottom = ConstraintLayout.LayoutParams.UNSET
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        binding.fragmentContainer.layoutParams = params
    }

    fun selectDestination(itemId: Int) {
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
        binding.bottomNavigation.selectedItemId = itemId
    }

    fun openProfile() {
        openDetail(ProfileFragment(), "profile")
    }

    fun openLanguageSettings() {
        openDetail(LanguageSettingsFragment(), "language_settings")
    }

    fun openDetail(fragment: Fragment, tag: String) {
        showInterstitial(detailInterstitialPosition(tag))
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment, tag)
            addToBackStack(tag)
        }
        supportFragmentManager.executePendingTransactions()
        showNativeAd(detailNativePosition(tag))
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment, tag)
        }
    }

    private fun showNativeAd(position: Long) {
        currentNativePosition = position
        showNativeAdIfAvailable(position, loadIfMissing = true)
    }

    private fun showNativeAdIfAvailable(position: Long, loadIfMissing: Boolean) {
        val targetHost = currentInlineAdHost()
        if (targetHost == null) {
            currentAdHost?.removeAllViews()
            currentAdHost?.isVisible = false
            currentAdHost = null
            return
        }
        val nativeAd = LoadManagerTools.adSpInstance.getCurSpecialToNative(position, this)
        if (nativeAd == null) {
            if (loadIfMissing) {
                LoadManagerTools.adSpInstance.interspaceStudyLoadTransBanner(
                    position,
                    object : ListenerTrans {
                        override fun loadTransAdStatus(callResult: Boolean) {
                            if (!callResult) return
                            runOnUiThread {
                                val activePosition = currentNativePosition ?: return@runOnUiThread
                                showNativeAdIfAvailable(activePosition, loadIfMissing = false)
                            }
                        }
                    },
                )
            }
            return
        }
        val bannerView = BannerView(this, null).apply {
            setNativeAd(nativeAd)
        }
        if (currentAdHost !== targetHost) {
            currentAdHost?.removeAllViews()
            currentAdHost?.isVisible = false
        }
        targetHost.removeAllViews()
        targetHost.addView(
            bannerView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        targetHost.isVisible = true
        currentAdHost = targetHost
        LoadManagerTools.adSpInstance.bannerShowButton(bannerView)
    }

    private fun currentInlineAdHost(): FrameLayout? {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            ?.view
            ?.findViewById(R.id.inlineAdContainer)
    }

    private fun showInterstitial(position: Long) {
        LoadManagerTools.adSpInstance.getShowCyAds(position, this)
    }

    private fun tabNativePosition(itemId: Int): Long {
        return when (itemId) {
            R.id.menu_swipe -> swipeNativeAd()
            R.id.menu_compress -> compressNativeAd()
            R.id.menu_tools -> toolsNativeAd()
            else -> homeNativeAd()
        }
    }

    private fun tabInterstitialPosition(itemId: Int): Long? {
        return when (itemId) {
            R.id.menu_swipe -> swipeInterstitialAd()
            R.id.menu_compress -> compressInterstitialAd()
            R.id.menu_tools -> toolsInterstitialAd()
            else -> null
        }
    }

    private fun detailNativePosition(tag: String): Long {
        return when (tag) {
            "profile", "language_settings" -> settingNative()
            "photo_detail" -> swipeDetailNativeAd()
            "clean_center" -> cleanCenterNativeAd()
            "contacts_cleanup" -> contactsNativeAd()
            "videos", "large_video_list" -> largeVideoNativeAd()
            "screenshots", "screenshot_list" -> screenshotListNativeAd()
            "duplicate_videos" -> duplicateVideoNativeAd()
            "similar", "duplicates", "similar_photos" -> similarImagesNativeAd()
            else -> detailNativeAd()
        }
    }

    private fun detailInterstitialPosition(tag: String): Long {
        return when (tag) {
            "profile", "language_settings" -> settingInter()
            "photo_detail" -> swipeDetailInterstitialAd()
            "clean_center" -> cleanCenterInterstitialAd()
            "contacts_cleanup" -> contactsInterstitialAd()
            "videos", "large_video_list" -> largeVideoInterstitialAd()
            "screenshots", "screenshot_list" -> screenshotListInterstitialAd()
            "duplicate_videos" -> duplicateVideoInterstitialAd()
            "similar", "duplicates", "similar_photos" -> similarImagesInterstitialAd()
            else -> detailInterstitialAd()
        }
    }

}
