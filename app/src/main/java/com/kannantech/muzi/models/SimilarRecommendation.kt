package com.kannantech.muzi.models

import com.kannantech.muzi.db.entities.LocalItem
import com.kannantech.innertube.models.YTItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
