package com.aetherquorion.cleansuperai.network

import com.aetherquorion.cleansuperai.ads.employment.manager.NativeTools.configReferConfig
import com.aetherquorion.cleansuperai.ads.employment.manager.NativeTools.createCommonHeaders
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class OKRequestManager private constructor() {
    fun flagRequestInfo(
        map: HashMap<String, String>?,
        speUrl: String,
        listener: OKHttpInterceptor.OKHTTPRequestListener,
    ) {
        loadGetRequest(map, speUrl, configReferConfig(), listener)
    }

    fun loadNormalRequestInfo(
        map: HashMap<String, String>?,
        speUrl: String,
        listener: OKHttpInterceptor.OKHTTPRequestListener,
    ) {
        loadGetRequest(map, speUrl, createCommonHeaders(), listener)
    }

    fun upValues(
        paramsOut: HashMap<String, String>,
        pekUrls: String,
        listener: OKHttpInterceptor.OKHTTPRequestListener,
    ) {
        if (interspaceStudyInit()) return
        val requestBody = Gson().toJson(paramsOut).toRequestBody("application/json".toMediaTypeOrNull())
        val builder = Headers.Builder()
        for ((key, value) in createCommonHeaders()) {
            builder.add(key, value)
        }
        val request = Request.Builder()
            .url(pekUrls)
            .headers(builder.build())
            .addHeader("content-type", "application/json")
            .post(requestBody)
            .build()
        interspaceStudyClient?.newCall(request)?.enqueue(callback(listener))
    }

    fun netInit() {
        interspaceStudyClient = OkHttpClient().newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(OKHttpInterceptor())
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun loadGetRequest(
        map: HashMap<String, String>?,
        speUrl: String,
        headers: HashMap<String, String>,
        listener: OKHttpInterceptor.OKHTTPRequestListener,
    ) {
        if (interspaceStudyInit()) return
        val translateUrl = if (map?.isNotEmpty() == true) {
            map.entries.joinToString(prefix = "$speUrl?", separator = "&") { "${it.key}=${it.value}" }
        } else {
            speUrl
        }
        val heads = Headers.Builder()
        for ((key, value) in headers) {
            heads.add(key, value)
        }
        interspaceStudyClient?.newCall(methodReq(translateUrl, heads))?.enqueue(callback(listener))
    }

    private fun callback(listener: OKHttpInterceptor.OKHTTPRequestListener): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                listener.okError(e.message.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { listener.okGetInfos(it.string()) }
            }
        }
    }

    private var interspaceStudyClient: OkHttpClient? = null

    private fun methodReq(baseUrl: String, specialHeads: Headers.Builder): Request {
        return Request.Builder()
            .url(baseUrl)
            .headers(specialHeads.build())
            .build()
    }

    private fun interspaceStudyInit(): Boolean = interspaceStudyClient == null

    companion object {
        private var instance: OKRequestManager? = null

        @Synchronized
        fun get(): OKRequestManager {
            if (instance == null) {
                instance = OKRequestManager()
            }
            return instance!!
        }
    }
}
