package com.kannantech.muzi.utils

import android.content.Context
import com.kannantech.innertube.YouTube
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * NetworkBoost Ultra Engine
 * 
 * Provides a globally shared, highly optimized OkHttpClient for maximum performance.
 * Centralizing network traffic through this engine improves connection pooling,
 * reduces overhead, and speeds up data retrieval across the entire app.
 */
object NetworkBoost {
    private var okHttpClient: OkHttpClient? = null

    /**
     * Get the optimized OkHttpClient instance.
     * Initializes it if it doesn't exist.
     */
    fun getClient(context: Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: buildClient().also { 
                okHttpClient = it 
                prewarm()
            }
        }
    }

    /**
     * Pre-warms the connection pool by initiating a low-cost request.
     * This prepares the dispatcher and socket pool for immediate use.
     */
    private fun prewarm() {
        val client = okHttpClient ?: return
        val request = okhttp3.Request.Builder()
            .url("https://www.google.com/generate_204")
            .head()
            .build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }

    private fun buildClient(): OkHttpClient {
        // Extreme Concurrency Dispatcher for absolute maximum throughput
        val dispatcher = Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 64
        }

        // Expanded ConnectionPool for massive connection reuse
        val connectionPool = ConnectionPool(25, 10, TimeUnit.MINUTES)

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            // Use a ProxySelector that dynamically checks YouTube.proxy
            .proxySelector(object : java.net.ProxySelector() {
                override fun select(uri: java.net.URI?): List<java.net.Proxy> {
                    val p = YouTube.proxy
                    return if (p != null) listOf(p) else listOf(java.net.Proxy.NO_PROXY)
                }
                override fun connectFailed(uri: java.net.URI?, address: java.net.SocketAddress?, e: java.io.IOException?) {
                }
            })
            // REMOVED CACHE: Media3 handles its own caching; OkHttp cache can interfere with fragments and ranges.
            .build()
    }
}
