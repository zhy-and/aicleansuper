package com.example.cleansuperai.ads

/**
 * AdMob 配置。当前使用 Google 官方测试 ID，上线前请替换为正式 ID。
 */
object AdConfig {
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    /** 冷启动等待开屏广告加载的最长时间。 */
    const val COLD_START_LOAD_TIMEOUT_MS = 12_000L
}
