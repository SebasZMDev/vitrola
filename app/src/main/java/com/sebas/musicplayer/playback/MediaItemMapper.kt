package com.sebas.musicplayer.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sebas.musicplayer.domain.model.Song

private const val EXTRA_ALBUM_ID = "albumId"

fun Song.toMediaItem(): MediaItem {
    val extras = Bundle().apply { putLong(EXTRA_ALBUM_ID, albumId) }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri)
        .setDurationMs(durationMs)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

fun MediaItem.toSong(): Song? {
    val id = mediaId.toLongOrNull() ?: return null
    val uri = localConfiguration?.uri ?: requestMetadata.mediaUri ?: return null
    val md = mediaMetadata
    return Song(
        id = id,
        uri = uri,
        title = md.title?.toString().orEmpty(),
        artist = md.artist?.toString().orEmpty(),
        album = md.albumTitle?.toString().orEmpty(),
        albumId = md.extras?.getLong(EXTRA_ALBUM_ID) ?: 0L,
        durationMs = md.durationMs ?: 0L,
        artworkUri = md.artworkUri,
    )
}
