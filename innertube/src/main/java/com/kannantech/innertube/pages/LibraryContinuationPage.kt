package com.kannantech.innertube.pages

import com.kannantech.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
