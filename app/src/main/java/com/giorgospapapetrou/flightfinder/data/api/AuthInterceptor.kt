package com.giorgospapapetrou.flightfinder.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds Authorization: Bearer <api-key> to every outbound request.
 *
 * The API key is supplied at construction time, sourced from BuildConfig.
 */
class AuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        return chain.proceed(request)
    }
}