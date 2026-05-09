package com.kannantech.muzi.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.kannantech.muzi.constants.LocalLibraryEnableKey
import com.kannantech.muzi.constants.SCANNER_OWNER_LM
import com.kannantech.muzi.constants.ScanPathsKey
import com.kannantech.muzi.constants.ExcludedScanPathsKey
import com.kannantech.muzi.constants.ScannerImpl
import com.kannantech.muzi.constants.ScannerImplKey
import com.kannantech.muzi.constants.ScannerMatchCriteria
import com.kannantech.muzi.constants.ScannerSensitivityKey
import com.kannantech.muzi.constants.ScannerStrictExtKey
import com.kannantech.muzi.constants.ScannerStrictFilePathsKey
import com.kannantech.muzi.db.MusicDatabase
import com.kannantech.muzi.ui.utils.MEDIA_PERMISSION_LEVEL
import com.kannantech.muzi.ui.utils.clearDtCache
import com.kannantech.muzi.utils.scanners.LocalMediaScanner
import com.kannantech.muzi.utils.scanners.LocalMediaScanner.Companion.destroyScanner
import com.kannantech.muzi.utils.scanners.LocalMediaScanner.Companion.scannerState
import com.kannantech.muzi.utils.scanners.uriListFromString

private const val TAG = "LocalLibraryBootstrap"

suspend fun ensureLocalLibraryReady(
    context: Context,
    database: MusicDatabase,
): Boolean {
    val preBootstrapCount = database.allLocalSongs().size
    Log.i(TAG, "Bootstrap check started. Existing local songs: $preBootstrapCount")

    if (context.checkSelfPermission(MEDIA_PERMISSION_LEVEL) != PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "Skipping bootstrap: media permission not granted")
        return false
    }

    if (!context.dataStore.get(LocalLibraryEnableKey, defaultValue = true)) {
        Log.i(TAG, "Skipping bootstrap: local library disabled")
        return false
    }

    if (preBootstrapCount > 0) {
        Log.i(TAG, "Skipping bootstrap: local library already populated")
        return false
    }

    if (scannerState.value > 0) {
        Log.i(TAG, "Skipping bootstrap: scanner already in progress")
        return false
    }

    val scannerImpl by enumPreference(
        context = context,
        key = ScannerImplKey,
        defaultValue = ScannerImpl.MEDIASTORE
    )
    val scannerSensitivity by enumPreference(
        context = context,
        key = ScannerSensitivityKey,
        defaultValue = ScannerMatchCriteria.LEVEL_2
    )
    val scanPaths = context.dataStore.get(ScanPathsKey, defaultValue = "")
    val excludedScanPaths = context.dataStore.get(ExcludedScanPathsKey, defaultValue = "")
    val strictExtensions = context.dataStore.get(ScannerStrictExtKey, defaultValue = false)
    val strictFilePaths = context.dataStore.get(ScannerStrictFilePathsKey, defaultValue = false)

    return try {
        Log.i(TAG, "Bootstrapping local library from device storage with $scannerImpl")
        val scanner = LocalMediaScanner.getScanner(context, scannerImpl, SCANNER_OWNER_LM)
        if (scannerImpl == ScannerImpl.MEDIASTORE) {
            scanner.fullMediaStoreSync(
                database = database,
                scanPaths = uriListFromString(scanPaths),
                excludedScanPaths = uriListFromString(excludedScanPaths),
                matchCriteria = scannerSensitivity,
                strictFileNames = strictExtensions,
                strictFilePaths = strictFilePaths,
                refreshExisting = false,
            )
        } else {
            val uris = scanner.scanLocal(scanPaths, excludedScanPaths)
            scanner.quickSync(
                database = database,
                newSongs = uris,
                matchCriteria = scannerSensitivity,
                strictFileNames = strictExtensions,
                strictFilePaths = strictFilePaths,
            )
        }
        val postBootstrapCount = database.allLocalSongs().size
        Log.i(TAG, "Bootstrap completed. Local songs after scan: $postBootstrapCount")
        postBootstrapCount > 0
    } catch (e: Exception) {
        Log.e(TAG, "Bootstrap failed: ${e.message}", e)
        reportException(e)
        false
    } finally {
        clearDtCache()
        destroyScanner(SCANNER_OWNER_LM)
    }
}
