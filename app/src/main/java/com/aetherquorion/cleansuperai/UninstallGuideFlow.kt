package com.aetherquorion.cleansuperai

internal class UninstallGuideFlow {
    enum class Page { RETAIN, REASONS }
    enum class Destination { HOME, APP_SETTINGS }
    enum class Reason(val analyticsValue: String) {
        NO_NEED("no_need"),
        TOO_MANY_ADS("too_many_ads"),
        NOT_ENOUGH_SPACE("not_enough_space"),
        FILES_NOT_FOUND("files_not_found"),
        HARD_TO_USE("hard_to_use"),
        SLOW_OR_UNSTABLE("slow_or_unstable"),
        OTHER("other"),
    }
    enum class ExitAction(val analyticsValue: String) {
        BACK("back"), CANCEL("cancel"), UNINSTALL("uninstall"),
    }
    data class ReasonExit(val reason: String, val exitAction: String)

    var page = Page.RETAIN
        private set
    var selectedReason = Reason.NO_NEED
        private set
    private var reasonExitRecorded = false

    fun continueUninstall(): Page {
        page = Page.REASONS
        return page
    }

    fun selectReason(reason: Reason) {
        selectedReason = reason
    }

    fun restoreReason(analyticsValue: String?) {
        selectedReason = Reason.entries.firstOrNull { it.analyticsValue == analyticsValue } ?: Reason.NO_NEED
    }

    fun reasonExit(action: ExitAction): ReasonExit? {
        if (page != Page.REASONS || reasonExitRecorded) return null
        reasonExitRecorded = true
        return ReasonExit(selectedReason.analyticsValue, action.analyticsValue)
    }

    fun back(): Destination = Destination.HOME

    fun cancel(): Destination = Destination.HOME

    fun uninstall(): Destination = Destination.APP_SETTINGS
}
