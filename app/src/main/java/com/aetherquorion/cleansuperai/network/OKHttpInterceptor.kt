package com.aetherquorion.cleansuperai.network

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class OKHttpInterceptor : Interceptor {
    interface OKHTTPRequestListener {
        fun okError(message: String)
        fun okGetInfos(configInfo: String)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        interspaceStudyIntersRes = ""
        val request = chain.request().newBuilder().build()
        val res = chain.proceed(request)
        try {
            res.body?.let {
                interspaceStudyIntersRes = it.string()
                interspaceStudyType = it.contentType()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res.newBuilder().body(interspaceStudyIntersRes.toResponseBody(interspaceStudyType)).build()
    }

    private var interspaceStudyType: MediaType? = null
    private var interspaceStudyIntersRes = ""
}
