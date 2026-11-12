package com.glyphplay.player.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MusicRepository(private val context: Context) {

    private val minDurationMs = 30_000L

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = ArrayList<Track>(512)
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
        )

        try {
            context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val isMusicCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val addedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else ""
                    val isMusic = isMusicCol < 0 || cursor.getInt(isMusicCol) != 0
                    val name = cursor.getString(nameCol).orEmpty()
                    if (!isMusic && !isSupported(mime, name)) continue

                    val durationMs = readDurationMs(cursor, durationCol)
                    if (durationMs < minDurationMs) continue

                    val title = cleanTitle(
                        cursor.getString(titleCol),
                        name,
                    )
                    tracks += Track(
                        id = cursor.getLong(idCol),
                        title = title,
                        artist = cleanPerson(cursor.getString(artistCol), "Unknown artist"),
                        album = cleanPerson(cursor.getString(albumCol), "Unknown album"),
                        durationMs = durationMs,
                        uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol)),
                        albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else 0L,
                        dateAdded = if (addedCol >= 0) cursor.getLong(addedCol) else 0L,
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        } catch (_: Exception) {
            return@withContext tracks
        }
        tracks
    }

    private fun readDurationMs(cursor: android.database.Cursor, column: Int): Long {
        if (column < 0 || cursor.isNull(column)) return -1L
        return try {
            cursor.getLong(column)
        } catch (_: Exception) {
            -1L
        }
    }

    private fun isSupported(mime: String, displayName: String): Boolean {
        val m = mime.lowercase(Locale.US)
        if (m.startsWith("audio/mpeg") || m == "audio/mp3") return true
        if (m.contains("flac")) return true
        if (m.contains("wav") || m == "audio/x-wav") return true
        if (m.contains("aac") || m == "audio/mp4" || m == "audio/m4a") return true
        if (m.contains("ogg") || m.contains("vorbis") || m.contains("opus")) return true
        val n = displayName.lowercase(Locale.US)
        return n.endsWith(".mp3") || n.endsWith(".flac") || n.endsWith(".wav") ||
            n.endsWith(".m4a") || n.endsWith(".aac") || n.endsWith(".ogg") || n.endsWith(".opus")
    }

    private fun cleanTitle(title: String?, displayName: String): String {
        val t = title?.trim().orEmpty()
        if (t.isNotEmpty() && !t.equals("<unknown>", ignoreCase = true)) return t
        val file = displayName.substringBeforeLast('.').trim()
        return file.ifBlank { "Unknown title" }
    }

    private fun cleanPerson(value: String?, fallback: String): String {
        val v = value?.trim().orEmpty()
        if (v.isEmpty() || v.equals("<unknown>", ignoreCase = true) || v == "-") return fallback
        return v
    }
}
