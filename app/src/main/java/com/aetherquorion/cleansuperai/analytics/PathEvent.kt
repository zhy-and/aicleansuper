package com.aetherquorion.cleansuperai.analytics

enum class PathEvent(val label: String, val firebaseName: String) {
    COLD_START("冷启动", "path_cold_start"), NOTIF_PERM_SHOWN("通知权限显示", "path_notif_perm_shown"),
    NOTIF_PERM_GRANTED("通知权限授权", "path_notif_perm_granted"), NOTIF_PERM_DENIED("通知权限未授权", "path_notif_perm_denied"),
    SPLASH_AD_REQUEST("开屏广告发出请求", "path_splash_ad_request"), SPLASH_AD_REQUEST_FAIL("开屏广告请求失败", "path_splash_ad_request_fail"),
    SPLASH_AD_SHOW("开屏广告展示成功数", "path_splash_ad_show"), SPLASH_AD_CLICK("开屏广告点击数", "path_splash_ad_click"),
    SPLASH_AD_CLOSE("开屏广告关闭", "path_splash_ad_close"), SPLASH_HOME_EXIT("启动页home键退出", "path_splash_home_exit"),
    HOME_PAGE("首页", "path_home_page"), NOTIF_PERM2_SHOWN("通知权限2显示", "path_notif_perm2_shown"),
    NOTIF_PERM2_GRANTED("通知权限2授权", "path_notif_perm2_granted"), NOTIF_PERM2_DENIED("通知权限2未授权", "path_notif_perm2_denied"),
    STORAGE_PERM_SHOWN("所有文件权限显示", "path_storage_perm_shown"), STORAGE_PERM_GRANTED("所有文件权限授权", "path_storage_perm_granted"),
    STORAGE_PERM_DENIED("所有文件权限未授权", "path_storage_perm_denied"), SCAN_AFTER_STORAGE_GRANT("授权后进入扫描页", "path_scan_after_storage_grant"),
    CACHE_CLEAN_CLICK("扫描完成后点击清理按钮", "path_cache_clean_click"), CACHE_RESULT_PAGE("缓存清理结果页", "path_cache_result_page"),
    HOME_CLICK_CACHE("清理缓存", "path_home_click_cache"), HOME_CLICK_LARGE("大文件", "path_home_click_large"),
    HOME_CLICK_PICTURE("图片", "path_home_click_picture"), HOME_CLICK_SCREENSHOTS("截图", "path_home_click_screenshots"),
    HOME_CLICK_SIMILAR("相似图片", "path_home_click_similar"), HOME_CLICK_DUPLICATE("重复图片", "path_home_click_duplicate"),
    HOME_CLICK_SWIPE("滑动相册", "path_home_click_swipe"), HOME_CLICK_VAULT("隐私空间", "path_home_click_vault"),
    HOME_CLICK_VIDEO("视频", "path_home_click_video"), HOME_CLICK_AUDIO("音频", "path_home_click_audio"),
    HOME_CLICK_BATTERY("电池", "path_home_click_battery"), HOME_CLICK_APP("管理应用", "path_home_click_app"),
    HOME_CLICK_NETWORK("网络流量", "path_home_click_network"), HOME_CLICK_DEVICE("扫描设备", "path_home_click_device"),
    WIDGET_ADD_SUCCESS("小组件成功添加", "path_widget_add_success");
    companion object {
        fun fromFeatureKey(key: String): PathEvent? = when (key) {
            "cache" -> HOME_CLICK_CACHE; "large" -> HOME_CLICK_LARGE; "picture" -> HOME_CLICK_PICTURE
            "screenshots" -> HOME_CLICK_SCREENSHOTS; "similar" -> HOME_CLICK_SIMILAR
            "duplicate" -> HOME_CLICK_DUPLICATE; "swipe" -> HOME_CLICK_SWIPE; "vault" -> HOME_CLICK_VAULT
            "video" -> HOME_CLICK_VIDEO; "audio" -> HOME_CLICK_AUDIO; "battery" -> HOME_CLICK_BATTERY
            "app" -> HOME_CLICK_APP; "network" -> HOME_CLICK_NETWORK; "device" -> HOME_CLICK_DEVICE
            else -> null
        }
    }
}
