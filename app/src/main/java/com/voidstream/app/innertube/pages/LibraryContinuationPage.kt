package com.voidstream.app.innertube.pages

import com.voidstream.app.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
