package com.voidstream.app.innertube.pages

import com.voidstream.app.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
