package com.aetherquorion.cleansuperai.ads.model

import java.io.Serializable

data class AdaraImportBean(
    var pzdyeqenjrku: ImportPzdyeqenjrku? = null,
) : Serializable

data class ImportPzdyeqenjrku(
    var hktkevvwwbw: ImportHktkevvwwbw? = null,
) : Serializable

data class ImportHktkevvwwbw(
    var hdvrhqxtjsr: ImportHdvrhqxtjsr? = null,
) : Serializable

data class ImportHdvrhqxtjsr(
    var ntpwxttl: ImportNtpwxttl? = null,
) : Serializable

data class ImportNtpwxttl(
    var xjcchmpevr: ImportXjcchmpevr? = null,
) : Serializable

data class ImportXjcchmpevr(
    var udjpjamfklmglw: ImportPayload? = null,
) : Serializable

data class ImportPayload(
    var beepiw: Int = 0,
    var lcpakw: MutableList<AdPlaceBean>? = null,
) : Serializable

data class AdPlaceBean(
    var aaojq: String? = null,
    var wqcm: String? = null,
    var bcpfpn: String? = null,
    var vlthm: Long = 0,
    var jxlwhu: Int = 0,
    var dgsup: Int = 0,
    var dhdeit: String? = null,
) : Serializable

data class AdaraInfoBean(
    var pzdyeqenjrku: InfoPzdyeqenjrku? = null,
) : Serializable

data class InfoPzdyeqenjrku(
    var hktkevvwwbw: InfoHktkevvwwbw? = null,
) : Serializable

data class InfoHktkevvwwbw(
    var hdvrhqxtjsr: InfoHdvrhqxtjsr? = null,
) : Serializable

data class InfoHdvrhqxtjsr(
    var ntpwxttl: InfoNtpwxttl? = null,
) : Serializable

data class InfoNtpwxttl(
    var xjcchmpevr: InfoXjcchmpevr? = null,
) : Serializable

data class InfoXjcchmpevr(
    var udjpjamfklmglw: InfoPayload? = null,
) : Serializable

data class InfoPayload(
    var ljg: Int = 0,
    var pupyzt: AppConfigPayload? = null,
) : Serializable

data class AppConfigPayload(
    var xnr: Int = 0,
    var yhanof: List<AppConfigItem>? = null,
) : Serializable

data class AppConfigItem(
    var walj: String? = null,
    var hqcz: String? = null,
) : Serializable

data class UploadLogInfoBean(
    var pzdyeqenjrku: UploadPzdyeqenjrku? = null,
) : Serializable

data class UploadPzdyeqenjrku(
    var hktkevvwwbw: UploadHktkevvwwbw? = null,
) : Serializable

data class UploadHktkevvwwbw(
    var hdvrhqxtjsr: UploadHdvrhqxtjsr? = null,
) : Serializable

data class UploadHdvrhqxtjsr(
    var ntpwxttl: UploadNtpwxttl? = null,
) : Serializable

data class UploadNtpwxttl(
    var xjcchmpevr: UploadXjcchmpevr? = null,
) : Serializable

data class UploadXjcchmpevr(
    var udjpjamfklmglw: UploadPayload? = null,
) : Serializable

data class UploadPayload(
    var evj: Int = 0,
) : Serializable

data class InfoDestroySp(val isShow: Boolean)
data class InfoDestroyCentre(val isShow: Boolean)
data class HomeEventsReceive(val isShow: Boolean)
