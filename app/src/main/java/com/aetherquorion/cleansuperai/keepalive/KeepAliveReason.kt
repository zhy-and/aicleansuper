package com.aetherquorion.cleansuperai.keepalive

enum class KeepAliveReason(val value: String) {
    LOCKED_BOOT("锁屏开机预热"), BOOT("开机启动"), USER_UNLOCKED("用户解锁"),
    PACKAGE_REPLACED("应用更新后启动"), QUICKBOOT("厂商快速开机"),
    SERVICE_START("前台服务启动"), SERVICE_DESTROY("前台服务销毁自愈"),
    SERVICE_TASK_REMOVED("最近任务划掉自愈"), NOTIFICATION_DELETE("通知删除自愈"),
    QUICK_JOB("快速拉活任务"), HEARTBEAT_JOB("心跳任务"), ALARM("定时闹钟"),
    FCM_HIGH("推送高优先级唤醒"), APP_FOREGROUND("应用回到前台"),
    HOME_KEY("桌面键/最近任务键"), MANUAL("手动触发"), REMOTE_CONFIG("远端配置更新补链")
}
