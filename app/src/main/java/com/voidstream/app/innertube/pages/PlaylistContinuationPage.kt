package com.voidstream.app.innertube.pages

import com.voidstream.app.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
