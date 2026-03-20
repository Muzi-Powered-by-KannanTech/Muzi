package com.kannantech.innertube.pages

import com.kannantech.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
