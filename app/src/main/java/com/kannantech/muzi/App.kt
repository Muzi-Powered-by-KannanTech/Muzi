/*
 * Copyright (C) 2024 z-huang/InnerTune
 * Copyright (C) 2025 MUZI Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.kannantech.muzi

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.kannantech.muzi.constants.AccountChannelHandleKey
import com.kannantech.muzi.constants.AccountEmailKey
import com.kannantech.muzi.constants.AccountNameKey
import com.kannantech.muzi.constants.ContentCountryKey
import com.kannantech.muzi.constants.ContentLanguageKey
import com.kannantech.muzi.constants.CountryCodeToName
import com.kannantech.muzi.constants.DataSyncIdKey
import com.kannantech.muzi.constants.InnerTubeCookieKey
import com.kannantech.muzi.constants.LanguageCodeToName
import com.kannantech.muzi.constants.MaxImageCacheSizeKey
import com.kannantech.muzi.constants.ProxyEnabledKey
import com.kannantech.muzi.constants.ProxyTypeKey
import com.kannantech.muzi.constants.ProxyUrlKey
import com.kannantech.muzi.constants.SYSTEM_DEFAULT
import com.kannantech.muzi.constants.UseLoginForBrowse
import com.kannantech.muzi.constants.VisitorDataKey
import com.kannantech.muzi.extensions.toEnum
import com.kannantech.muzi.extensions.toInetSocketAddress
import com.kannantech.muzi.utils.CoilBitmapLoader
import com.kannantech.muzi.utils.LocalArtworkPathKeyer
import com.kannantech.muzi.utils.dataStore
import com.kannantech.muzi.utils.get
import com.kannantech.muzi.utils.reportException
import com.kannantech.innertube.YouTube
import com.kannantech.innertube.models.YouTubeLocale
import com.kannantech.kugou.KuGou
import com.kannantech.muzi.utils.NetworkBoost
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.Proxy
import java.util.Locale

@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {
    private val TAG = App::class.simpleName.toString()

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()

        instance = this

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "")
        YouTube.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        GlobalScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { preferences ->
                    Triple(
                        YouTubeLocale(
                            gl = preferences[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                                ?: locale.country.takeIf { it in CountryCodeToName }
                                ?: "US",
                            hl = preferences[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                                ?: locale.language.takeIf { it in LanguageCodeToName }
                                ?: languageTag.takeIf { it in LanguageCodeToName }
                                ?: "en"
                        ),
                        preferences[UseLoginForBrowse] != false,
                        preferences[ProxyEnabledKey] == true
                    ) to Pair(preferences[ProxyTypeKey], preferences[ProxyUrlKey])
                }
                .distinctUntilChanged()
                .collect { (config, proxyInfo) ->
                    val (ytLocale, useLoginForBrowse, proxyEnabled) = config
                    val (proxyType, proxyUrl) = proxyInfo

                    YouTube.locale = ytLocale
                    KuGou.useTraditionalChinese = ytLocale.hl == "zh-TW"
                    YouTube.useLoginForBrowse = useLoginForBrowse

                    YouTube.proxy = if (proxyEnabled && !proxyUrl.isNullOrBlank()) {
                        try {
                            Proxy(
                                proxyType.toEnum(defaultValue = Proxy.Type.HTTP),
                                proxyUrl.toInetSocketAddress()
                            )
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@App, "Failed to parse proxy url.", LENGTH_SHORT).show()
                            }
                            reportException(e)
                            null
                        }
                    } else {
                        null
                    }
                }
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" }
                        ?: run {
                            delay(3000)
                            YouTube.visitorData()
                        }.onFailure {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@App, "Failed to get visitorData.", LENGTH_SHORT).show()
                            }
                            reportException(it)
                        }.getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not parse cookie. Clearing existing cookie. ${e.message}")
                        forgetAccount(this@App)
                    }
                }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cacheSize = dataStore[MaxImageCacheSizeKey]

        val builder = ImageLoader.Builder(context)
            .components {
                add(CoilBitmapLoader.Factory(context))
                add(LocalArtworkPathKeyer())
                add(OkHttpNetworkFetcherFactory(NetworkBoost.getClient(context)))
            }
            .crossfade(false)
            .allowHardware(false)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.3)
                    .build()
            }

        if (cacheSize != 0) {
            builder.diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 512) * 1024 * 1024L)
                    .build()
            }
        } else {
            builder.diskCachePolicy(CachePolicy.DISABLED)
        }

        return builder.build()
    }

    companion object {
        lateinit var instance: App
            private set

        fun forgetAccount(context: Context) {
            runBlocking {
                context.dataStore.edit { settings ->
                    settings.remove(InnerTubeCookieKey)
                    settings.remove(VisitorDataKey)
                    settings.remove(DataSyncIdKey)
                    settings.remove(AccountNameKey)
                    settings.remove(AccountEmailKey)
                    settings.remove(AccountChannelHandleKey)
                }
            }
        }
    }
}
