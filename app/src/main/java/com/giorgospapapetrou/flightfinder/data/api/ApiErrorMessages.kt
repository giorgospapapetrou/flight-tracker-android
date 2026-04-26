package com.giorgospapapetrou.flightfinder.data.api

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Map any network-related throwable to a clean user-facing message.
 * Does not expose IPs, ports, hostnames, or stack details.
 */
fun describeNetworkError(t: Throwable): String = when (t) {
    is SocketTimeoutException -> "Connection timed out"
    is UnknownHostException -> "Cannot reach the server"
    is HttpException -> when (t.code()) {
        401 -> "Authentication failed"
        403 -> "Not authorized"
        404 -> "Resource not found"
        500, 502, 503, 504 -> "Server error"
        else -> "Request failed"
    }
    is IOException -> "Network error"
    else -> "Something went wrong"
}