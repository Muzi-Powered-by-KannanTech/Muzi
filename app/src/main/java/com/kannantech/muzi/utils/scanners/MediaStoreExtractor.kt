package com.kannantech.muzi.utils.scanners

import com.kannantech.muzi.models.SongTempData
import java.io.File

class MediaStoreExtractor : MetadataScanner {
    /**
     * Given a path to a file, extract necessary metadata.
     *
     * @param file Full file path
     */
    override suspend fun getAllMetadataFromFile(file: File): SongTempData {
        throw IllegalStateException("MediaStore scanner does not use the advanced tag extractor")
    }
}
