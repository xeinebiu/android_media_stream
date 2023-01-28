package com.xeinebiu.media_stream.util

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL

/**
 * Get IP address from first non-localhost interface
 * @param useIPv4   true=return ipv4, false=return ipv6
 * @return address or empty string
 * @ref https://stackoverflow.com/a/13007325
 */
internal fun getIPAddress(useIPv4: Boolean): String {
    try {
        val interfaces = NetworkInterface
            .getNetworkInterfaces()
            .toList()

        for (networkInterface in interfaces) {
            val inetAddresses = networkInterface.inetAddresses.toList()

            for (address in inetAddresses) {
                if (!address.isLoopbackAddress) {
                    val hostAddress = address.hostAddress ?: continue

                    val isIPv4 = hostAddress.indexOf(':') < 0
                    if (useIPv4) {
                        if (isIPv4) return hostAddress
                    } else {
                        if (!isIPv4) {
                            val delimiter = hostAddress.indexOf('%') // drop ip6 zone suffix
                            return if (delimiter < 0) hostAddress.uppercase()
                            else hostAddress.substring(0, delimiter).uppercase()
                        }
                    }
                }
            }
        }
    } catch (ignored: Exception) {
    }

    return ""
}

/**
 * Retrieve the stream length from given [uri] and [headers]
 */
internal suspend fun getContentLength(
    uri: String,
    headers: Map<String, String>?
): Long = withContext(Dispatchers.IO) {
    val connection = createHttpURLConnection(uri, headers)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) connection.contentLengthLong
    else connection.contentLength.toLong()
}

/**
 * Create a Http Connection from given [uri] and [headers]
 */
internal suspend fun createHttpURLConnection(
    uri: String,
    headers: Map<String, String>? = null
): HttpURLConnection = withContext(Dispatchers.IO) {
    val connection = URL(uri).openConnection() as HttpURLConnection

    headers?.let {
        for (h in it) connection.setRequestProperty(h.key, h.value)
    }

    connection
}
