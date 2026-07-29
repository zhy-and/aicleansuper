package com.aetherquorion.cleansuperai.ads

interface ListenerTrans {
    fun loadTransAdStatus(callResult: Boolean)
}

interface TemTranslateIntersLis {
    fun loadTransInterStatus(interResult: Boolean)
}

interface TranslateKepShowStatusLis {
    fun statusShowedSuc()
    fun statusShowedFa()
}

interface TransLateLoadedLis {
    fun kepRequestSuc()
    fun kepLoadedError()
}
