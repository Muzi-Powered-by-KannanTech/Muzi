package com.kannantech.muzi

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.kannantech.muzi.constants.AUTO_SCAN_COOLDOWN
import com.kannantech.muzi.constants.AUTO_SCAN_SOFT_COOLDOWN
import com.kannantech.muzi.constants.AutomaticScannerKey
import com.kannantech.muzi.constants.ExcludedScanPathsKey
import com.kannantech.muzi.constants.LastLocalScanKey
import com.kannantech.muzi.constants.LastVersionKey
import com.kannantech.muzi.constants.LocalLibraryEnableKey
import com.kannantech.muzi.constants.OOBE_VERSION
import com.kannantech.muzi.constants.OobeStatusKey
import com.kannantech.muzi.constants.SCANNER_OWNER_LM
import com.kannantech.muzi.constants.ScanPathsKey
import com.kannantech.muzi.constants.ScannerImpl
import com.kannantech.muzi.constants.ScannerImplKey
import com.kannantech.muzi.constants.ScannerMatchCriteria
import com.kannantech.muzi.constants.ScannerSensitivityKey
import com.kannantech.muzi.constants.ScannerStrictExtKey
import com.kannantech.muzi.constants.ScannerStrictFilePathsKey
import com.kannantech.muzi.constants.UpdateAvailableKey
import com.kannantech.muzi.db.MusicDatabase
import com.kannantech.muzi.models.toMediaMetadata
import com.kannantech.muzi.playback.DownloadUtil
import com.kannantech.muzi.playback.PlayerConnection
import com.kannantech.muzi.playback.queues.ListQueue
import com.kannantech.muzi.ui.utils.MEDIA_PERMISSION_LEVEL
import com.kannantech.muzi.ui.utils.clearDtCache
import com.kannantech.muzi.utils.dataStore
import com.kannantech.muzi.utils.enumPreference
import com.kannantech.muzi.utils.get
import com.kannantech.muzi.utils.reportException
import com.kannantech.muzi.utils.scanners.LocalMediaScanner
import com.kannantech.muzi.utils.scanners.LocalMediaScanner.Companion.destroyScanner
import com.kannantech.muzi.utils.scanners.LocalMediaScanner.Companion.scannerState
import com.kannantech.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 * Directly navigate to a YouTube page given an YouTube url
 */
fun youtubeNavigator(
    context: Context,
    navController: NavController,
    coroutineScope: CoroutineScope,
    playerConnection: PlayerConnection?,
    snackbarHostState: SnackbarHostState,
    uri: Uri
): Boolean {
    when (val path = uri.pathSegments.firstOrNull()) {
        "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
            if (playlistId.startsWith("OLAK5uy_")) {
                coroutineScope.launch {
                    YouTube.albumSongs(playlistId).onSuccess { songs ->
                        songs.firstOrNull()?.album?.id?.let { browseId ->
                            navController.navigate("album/$browseId")
                        }
                    }.onFailure {
                        reportException(it)
                    }
                }
            } else {
                navController.navigate("online_playlist/$playlistId")
            }
        }

        "channel", "c" -> uri.lastPathSegment?.let { artistId ->
            navController.navigate("artist/$artistId")
        }

        else -> when {
            path == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> path
            else -> return false
        }?.let { videoId ->
            val playlistId = uri.getQueryParameter("list")
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    YouTube.queue(listOf(videoId), playlistId)
                }.onSuccess {
                    val s = it.firstOrNull()
                    if (s == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.err_invalid_ytm_song),
                                withDismissAction = true,
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else {
                        playerConnection?.playQueue(
                            queue = ListQueue(
                                title = s.title,
                                items = listOf(s.toMediaMetadata())
                            )
                        )
                    }
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }

    return true
}

suspend fun scanInit(
    context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    coroutineScope: CoroutineScope,
    playerConnection: PlayerConnection?,
    snackbarHostState: SnackbarHostState?,
    forceScan: Boolean = false,
    requestPermissionIfMissing: Boolean = true,
) {
    val MAIN_TAG = "MainOtActivity"
    val oobeStatus = context.dataStore.get(OobeStatusKey, defaultValue = 0)
    val localLibEnable = context.dataStore.get(LocalLibraryEnableKey, defaultValue = true)
    val ds = context.dataStore.data.first()[ScannerSensitivityKey]
//        .map { it[SkipSilenceKey] ?: false }
    // auto scanner
    val scannerSensitivity by enumPreference(
        context = context,
        key = ScannerSensitivityKey,
        defaultValue = ScannerMatchCriteria.LEVEL_2
    )
    val scannerImpl by enumPreference(
        context = context,
        key = ScannerImplKey,
        defaultValue = ScannerImpl.MEDIASTORE
    )
    val scanPaths = context.dataStore.get(ScanPathsKey, defaultValue = "")
    val excludedScanPaths = context.dataStore.get(ExcludedScanPathsKey, defaultValue = "")
    val strictExtensions = context.dataStore.get(ScannerStrictExtKey, defaultValue = false)
    val strictFilePaths = context.dataStore.get(ScannerStrictFilePathsKey, defaultValue = false)
    val autoScan = context.dataStore.get(AutomaticScannerKey, defaultValue = true)
    val lastLocalScan = context.dataStore.get(LastLocalScanKey, 0L)
    Log.i(
        MAIN_TAG,
        "scanInit called. forceScan=$forceScan, localLibEnable=$localLibEnable, scannerImpl=$scannerImpl, autoScan=$autoScan"
    )

    // updater
    val updateAvailable = context.dataStore.get(
        UpdateAvailableKey,
        defaultValue = false
    )
    val lastVer = context.dataStore.get(LastVersionKey, defaultValue = "0.0.0")

    if (!autoScan || oobeStatus < OOBE_VERSION) {
        Log.i(MAIN_TAG, "Automatic scan is disabled, and/or user has not passed OOBE")
        return
    }
    val timeNow = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
    if (!forceScan && lastLocalScan + AUTO_SCAN_COOLDOWN > timeNow) {
        Log.i(MAIN_TAG, "Aborting automatic scan. Not enough time has passed since the last scan")
        downloadUtil.resumeDownloadsOnStart()
        return
    }
    Log.i(MAIN_TAG, "Starting local media and downloads auto scan")
    context.dataStore.edit { settings ->
        settings[LastLocalScanKey] =
            timeNow - AUTO_SCAN_COOLDOWN + AUTO_SCAN_SOFT_COOLDOWN // min cooldown to avoid crash loops
    }



    // scan download folders
    downloadUtil.scanDownloads()
    downloadUtil.resumeDownloadsOnStart()
    if (!localLibEnable) {
        context.dataStore.edit { settings ->
            settings[LastLocalScanKey] = timeNow
        }
        playerConnection?.service?.initQueue()
        Log.i(MAIN_TAG, "Downloads scan completed. Local media is disabled.")
    }


    // local media scan
    val perms = context.checkSelfPermission(MEDIA_PERMISSION_LEVEL)
    // Check if the permissions for local media access
    if (scannerState.value <= 0 && localLibEnable) {
        if (perms == PackageManager.PERMISSION_GRANTED) {
            // equivalent to (quick scan)
            try {
                withContext(Dispatchers.Main) {
                    playerConnection?.player?.pause()
                }
                val scanner = LocalMediaScanner.getScanner(
                    context, scannerImpl, SCANNER_OWNER_LM
                )
                if (scannerImpl == ScannerImpl.MEDIASTORE) {
                    scanner.fullMediaStoreSync(
                        database = database,
                        scanPaths = com.kannantech.muzi.utils.scanners.uriListFromString(scanPaths),
                        excludedScanPaths = com.kannantech.muzi.utils.scanners.uriListFromString(excludedScanPaths),
                        matchCriteria = scannerSensitivity,
                        strictFileNames = strictExtensions,
                        strictFilePaths = strictFilePaths,
                        refreshExisting = false,
                    )
                } else {
                    val uris = scanner.scanLocal(scanPaths, excludedScanPaths)
                    scanner.quickSync(database, uris, scannerSensitivity, strictExtensions, strictFilePaths)
                }
                Log.i(MAIN_TAG, "Local scan finished. Local DB count=${database.allLocalSongs().size}")
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState?.showSnackbar(
                        message = "${context.getString(R.string.scanner_scan_fail)}: ${e.message}",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                }
                reportException(e)
            } finally {
                clearDtCache()
                destroyScanner(SCANNER_OWNER_LM)
            }

            // post scan actions
            context.dataStore.edit { settings ->
                settings[LastLocalScanKey] = timeNow
            }
            playerConnection?.service?.initQueue()
            Log.i(MAIN_TAG, "Local media and downloads scan completed")
        } else if (perms == PackageManager.PERMISSION_DENIED && requestPermissionIfMissing) {
            (context as MainActivity).permissionLauncher.launch(MEDIA_PERMISSION_LEVEL)
            Log.w(MAIN_TAG, "Not enough permission to perform local media scan")
        } else if (perms == PackageManager.PERMISSION_DENIED) {
            Log.w(MAIN_TAG, "Skipping local media scan because storage permission is missing")
        }
    } else if (localLibEnable) {
        Log.w(MAIN_TAG, "Cannot perform local media scan, scanner is in use")
    }

    Log.i(MAIN_TAG, "Local media and downloads auto scan complete")


}
