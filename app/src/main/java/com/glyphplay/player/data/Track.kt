package com.glyphplay.player.data

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: Uri,
    val albumId: Long,
    val dateAdded: Long,
) {
    val artworkUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}

data class AlbumGroup(
    val album: String,
    val artist: String,
    val albumId: Long,
    val tracks: List<Track>,
)

data class ArtistGroup(
    val artist: String,
    val tracks: List<Track>,
)
