package com.aetherquorion.cleansuperai

import android.os.Bundle
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
import com.aetherquorion.cleansuperai.ads.banner.BannerView
import com.aetherquorion.cleansuperai.ads.employment.backSpecialToCy
import com.aetherquorion.cleansuperai.ads.employment.homeTabBanner
import com.aetherquorion.cleansuperai.ads.employment.zhuanTabInter
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
//
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
                showInterstitial(zhuanTabInter())
                supportFragmentManager.popBackStack(
                    null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE,
                )
                showFragment(it, item.itemId.toString())
                showNativeAd(homeTabBanner())
                true
            } ?: false
        }

        supportFragmentManager.addOnBackStackChangedListener {
            applyDetailChrome()
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.menu_home
        }
        showNativeAd(homeTabBanner())
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
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = binding.adContainer.id
            val adParams = binding.adContainer.layoutParams as ConstraintLayout.LayoutParams
            adParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            adParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            binding.adContainer.layoutParams = adParams
            binding.fragmentContainer.updatePadding(bottom = systemBottomInset)
        } else {
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.bottomToTop = binding.adContainer.id
            val adParams = binding.adContainer.layoutParams as ConstraintLayout.LayoutParams
            adParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            adParams.bottomToTop = binding.bottomNavigation.id
            binding.adContainer.layoutParams = adParams
            binding.fragmentContainer.updatePadding(bottom = 0)
        }
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
        showInterstitial(backSpecialToCy())
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment, tag)
            addToBackStack(tag)
        }
        showNativeAd(homeTabBanner())
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment, tag)
        }
    }

    private fun showNativeAd(position: Long) {
        val nativeAd = LoadManagerTools.adSpInstance.getCurSpecialToNative(position, this)
        if (nativeAd == null) {
            LoadManagerTools.adSpInstance.interspaceStudyLoadTransBanner(position)
            return
        }
        val bannerView = BannerView(this, null).apply {
            setNativeAd(nativeAd)
        }
        binding.adContainer.removeAllViews()
        binding.adContainer.addView(bannerView)
        binding.adContainer.isVisible = true
        LoadManagerTools.adSpInstance.bannerShowButton(bannerView)
    }

    private fun showInterstitial(position: Long) {
        LoadManagerTools.adSpInstance.getShowCyAds(position, this)
    }

}
