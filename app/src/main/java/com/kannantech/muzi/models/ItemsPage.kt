package com.kannantech.muzi.models

import com.kannantech.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
