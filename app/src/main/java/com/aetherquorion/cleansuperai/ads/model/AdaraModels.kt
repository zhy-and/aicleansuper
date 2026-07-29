package com.aetherquorion.cleansuperai.ads.model

import java.io.Serializable

data class AdaraImportBean(
    var gtccxhqbvngsib: ImportRoot? = null,
) : Serializable

data class ImportRoot(
    var tkronmqm: ImportTkronmqm? = null,
) : Serializable

data class ImportTkronmqm(
    var jakvynjmamdu: ImportPayload? = null,
) : Serializable

data class ImportPayload(
    var vrvqt: Int = 0,
    var byvc: MutableList<AdPlaceBean>? = null,
) : Serializable

data class AdPlaceBean(
    var xchh: String? = null,
    var dlz: String? = null,
    var adsStatus: Int = 0,
    var wdrhlh: String? = null,
    var xvamby: Long = 0,
    var xanpxg: Int = 0,
    var ahgkh: Int = 0,
    var btn: Long = 0,
    var ivbjpa: String? = null,
    var xqjpmc: String? = null,
) : Serializable

data class AdaraInfoBean(
    var gtccxhqbvngsib: InfoRoot? = null,
) : Serializable

data class InfoRoot(
    var tkronmqm: InfoTkronmqm? = null,
) : Serializable

data class InfoTkronmqm(
    var jakvynjmamdu: InfoPayload? = null,
) : Serializable

data class InfoPayload(
    var qdlo: Int = 0,
    var aufnfo: AppConfigPayload? = null,
) : Serializable

data class AppConfigPayload(
    var tpv: Int = 0,
    var qieq: List<AppConfigItem>? = null,
) : Serializable

data class AppConfigItem(
    var pao: String? = null,
    var hsdq: String? = null,
) : Serializable

data class UploadLogInfoBean(
    var gtccxhqbvngsib: UploadRoot? = null,
) : Serializable

data class UploadRoot(
    var tkronmqm: UploadTkronmqm? = null,
) : Serializable

data class UploadTkronmqm(
    var jakvynjmamdu: UploadPayload? = null,
) : Serializable

data class UploadPayload(
    var qttza: Int = 0,
) : Serializable

data class InfoDestroySp(val isShow: Boolean)
data class InfoDestroyCentre(val isShow: Boolean)
data class HomeEventsReceive(val isShow: Boolean)
