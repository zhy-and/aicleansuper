package com.aetherquorion.cleansuperai

import android.Manifest
import android.os.Bundle
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.aetherquorion.cleansuperai.ads.ListenerTrans
import com.aetherquorion.cleansuperai.ads.banner.BannerView
import com.aetherquorion.cleansuperai.ads.employment.backSpecialToCy
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
import com.aetherquorion.cleansuperai.ui.common.MediaPermissionHelper
import com.aetherquorion.cleansuperai.ui.common.FeaturePermissionCoordinator
import com.aetherquorion.cleansuperai.ui.common.FeatureAccess
import com.aetherquorion.cleansuperai.ui.common.StoragePermissionDialog
import com.aetherquorion.cleansuperai.ui.common.StoragePermissionGuideActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.aetherquorion.cleansuperai.ui.tools.ToolsFragment
import com.aetherquorion.cleansuperai.ui.cache.CacheCleanupFragment
import com.aetherquorion.cleansuperai.ui.tools.AppManagerFragment
import com.aetherquorion.cleansuperai.ui.tools.NetworkTrafficFragment
import com.aetherquorion.cleansuperai.ui.tools.SystemInfoFragment
import com.aetherquorion.cleansuperai.ui.media.CategoryMediaFragment
import com.aetherquorion.cleansuperai.ui.largefiles.LargeFilesFragment
import com.aetherquorion.cleansuperai.ui.clean.detail.ScreenshotListFragment
import com.aetherquorion.cleansuperai.widget.CleanWidgetProvider
import com.aetherquorion.cleansuperai.notification.NotificationPermissionFlow
import com.aetherquorion.cleansuperai.analytics.PathAnalytics
import com.aetherquorion.cleansuperai.analytics.PathEvent
import com.aetherquorion.cleansuperai.keepalive.KeepAliveCoordinator
import com.aetherquorion.cleansuperai.keepalive.KeepAliveReason
import com.aetherquorion.cleansuperai.notification.LocalEventPushHelper
import com.aetherquorion.cleansuperai.notification.LocalPushSummaryManager
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    var homePermissionSequencePending = true
        private set
    private var notificationAskedOnEntry = false
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        NotificationPermissionFlow.recordResult(granted)
        PathAnalytics.track(this, if (granted) PathEvent.NOTIF_PERM2_GRANTED else PathEvent.NOTIF_PERM2_DENIED)
        if (granted) KeepAliveCoordinator.bootstrap(this, KeepAliveReason.MANUAL)
        (supportFragmentManager.findFragmentByTag(R.id.menu_home.toString()) as? HomeFragment)
            ?.refreshNotificationTip()
        finishHomePermissionSequence()
    }
    private val swipeStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    private var swipePermissionDialog: AlertDialog? = null
    private lateinit var binding: ActivityMainBinding
    private var systemBottomInset = 0
    private var currentAdHost: FrameLayout? = null
    private var currentNativePosition: Long? = null
    private var previousBackStackEntryCount = 0
    private var suppressNextBackInterstitial = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PathAnalytics.trackHomePageOnce(this)

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
                clearDetailBackStackForNavigation()
                showFragment(it, item.itemId.toString())
                supportFragmentManager.executePendingTransactions()
                showNativeAd(tabNativePosition(item.itemId))
                if (item.itemId == R.id.menu_swipe && !MediaPermissionHelper.hasPermissions(this)) {
                    showSwipeStoragePermissionPrompt()
                }
                true
            } ?: false
        }
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.menu_swipe && !MediaPermissionHelper.hasPermissions(this)) {
                showSwipeStoragePermissionPrompt()
            }
        }

        previousBackStackEntryCount = supportFragmentManager.backStackEntryCount
        supportFragmentManager.addOnBackStackChangedListener {
            handleBackStackChanged()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    showExitPrompt()
                }
            }
        })

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.menu_home
            if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
                showFragment(HomeFragment(), R.id.menu_home.toString())
                supportFragmentManager.executePendingTransactions()
            }
        }
        binding.root.post { openWidgetRoute(intent) }
        showNativeAd(homeNativeAd())
        binding.root.postDelayed({
            if (isFinishing || isDestroyed || notificationAskedOnEntry) return@postDelayed
            notificationAskedOnEntry = true
            if (NotificationPermissionFlow.shouldRequest(Build.VERSION.SDK_INT, NotificationPermissionFlow.isGranted(this)) &&
                notifPerm2AskedThisProcess.compareAndSet(false, true)) {
                PathAnalytics.track(this, PathEvent.NOTIF_PERM2_SHOWN)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                finishHomePermissionSequence()
            }
        }, 500)
    }

    private fun finishHomePermissionSequence() {
        homePermissionSequencePending = false
        (supportFragmentManager.findFragmentByTag(R.id.menu_home.toString()) as? HomeFragment)
            ?.continueHomePermissionsAfterNotification()
    }

    fun requestNotificationPermissionFromHomeTip() {
        if (NotificationPermissionFlow.isGranted(this)) {
            (supportFragmentManager.findFragmentByTag(R.id.menu_home.toString()) as? HomeFragment)
                ?.refreshNotificationTip()
            return
        }
        when (NotificationPermissionFlow.tipAction(NotificationPermissionFlow.deniedCount())) {
            NotificationPermissionFlow.TipAction.REQUEST -> {
                PathAnalytics.track(this, PathEvent.NOTIF_PERM2_SHOWN)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            NotificationPermissionFlow.TipAction.SETTINGS -> startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openWidgetRoute(intent)
    }

    private fun openWidgetRoute(intent: android.content.Intent?) {
        intent?.getStringExtra("local_push_scene")?.let { scene ->
            intent.removeExtra("local_push_scene")
            LocalEventPushHelper.recordClick(this, scene)
            val id = intent.getIntExtra("local_push_notify_id", -1)
            if (id >= 0) {
                val manager = getSystemService(android.app.NotificationManager::class.java)
                manager?.cancel(id)
                if (manager != null) LocalPushSummaryManager.sync(this, manager)
            }
        }
        val route = intent?.getStringExtra(EXTRA_WIDGET_ROUTE) ?: return
        intent.removeExtra(EXTRA_WIDGET_ROUTE)
        when (route) {
            "cache", "cache_clean" -> openDetail(CacheCleanupFragment(), "cache_cleanup")
            "large", "large_file" -> openDetail(LargeFilesFragment(), "large_files")
            "screenshots", "screenshot_cleaner" -> openDetail(ScreenshotListFragment(), "screenshot_list")
            "apps", "app_manager" -> openDetail(AppManagerFragment(), "app_manager")
            "picture_clean" -> openDetail(CategoryMediaFragment.picture(), "picture_cleanup")
            "video_clean" -> openDetail(CategoryMediaFragment.video(), "video_cleanup")
            "audio_clean" -> openDetail(CategoryMediaFragment.audio(), "audio_cleanup")
            "battery_info" -> openDetail(SystemInfoFragment.battery(), "battery_info")
            "network_traffic" -> openDetail(NetworkTrafficFragment(), "network_traffic")
        }
    }

    private fun showExitPrompt() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, CleanWidgetProvider::class.java)
        val canPinWidget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.isRequestPinAppWidgetSupported && manager.getAppWidgetIds(provider).isEmpty()
        val content = layoutInflater.inflate(R.layout.dialog_exit_retain, null)
        val dialog = AlertDialog.Builder(this).setView(content).create()
        if (!canPinWidget) {
            content.findViewById<android.widget.TextView>(R.id.messageText)
                .setText(R.string.exit_retain_message_existing)
        }
        content.findViewById<android.view.View>(R.id.exitButton).setOnClickListener {
            dialog.dismiss()
            moveTaskToBack(true)
        }
        content.findViewById<android.view.View>(R.id.addWidgetButton).apply {
            visibility = if (canPinWidget) android.view.View.VISIBLE else android.view.View.GONE
            setOnClickListener {
                dialog.dismiss()
                manager.requestPinAppWidget(provider, null, null)
            }
        }
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.88f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        dialog.show()
    }

    companion object {
        const val EXTRA_WIDGET_ROUTE = "widget_route"
        private val notifPerm2AskedThisProcess = AtomicBoolean(false)
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
        clearDetailBackStackForNavigation()
        binding.bottomNavigation.selectedItemId = itemId
    }

    private fun showSwipeStoragePermissionPrompt() {
        if (swipePermissionDialog?.isShowing == true) return
        swipePermissionDialog = StoragePermissionDialog.show(this) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startActivity(FeaturePermissionCoordinator.settingsIntent(this, FeatureAccess.ALL_FILES))
                startActivity(Intent(this, StoragePermissionGuideActivity::class.java))
            } else {
                swipeStoragePermissionLauncher.launch(MediaPermissionHelper.requiredPermissions())
            }
        }
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

    private fun clearDetailBackStackForNavigation() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            suppressNextBackInterstitial = true
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun handleBackStackChanged() {
        val currentBackStackEntryCount = supportFragmentManager.backStackEntryCount
        if (currentBackStackEntryCount < previousBackStackEntryCount) {
            if (suppressNextBackInterstitial) {
                suppressNextBackInterstitial = false
            } else {
                showInterstitial(backSpecialToCy())
            }
        }
        previousBackStackEntryCount = currentBackStackEntryCount
        applyDetailChrome()
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
