package com.glyphplay.player.data

import android.content.Context
import org.json.JSONArray

class LocalStore(context: Context) {

    private val prefs = context.getSharedPreferences("glyphplay", Context.MODE_PRIVATE)

    var lastTrackId: Long
        get() = prefs.getLong("last_track", -1L)
        set(value) { prefs.edit().putLong("last_track", value).apply() }

    var lastPositionMs: Long
        get() = prefs.getLong("last_pos", 0L)
        set(value) { prefs.edit().putLong("last_pos", value).apply() }

    var shuffle: Boolean
        get() = prefs.getBoolean("shuffle", false)
        set(value) { prefs.edit().putBoolean("shuffle", value).apply() }

    var repeatMode: Int
        get() = prefs.getInt("repeat", 0)
        set(value) { prefs.edit().putInt("repeat", value).apply() }

    fun favoriteIds(): Set<Long> = readIds("favorites")

    fun isFavorite(id: Long): Boolean = favoriteIds().contains(id)

    fun toggleFavorite(id: Long): Set<Long> {
        val next = favoriteIds().toMutableSet()
        if (!next.add(id)) next.remove(id)
        writeIds("favorites", next.toList())
        return next
    }

    fun historyIds(): List<Long> = readIds("history").toList().asReversed().let {
        readOrdered("history")
    }

    fun recordPlayed(id: Long) {
        val next = readOrdered("history").toMutableList()
        next.remove(id)
        next.add(0, id)
        writeIds("history", next.take(200))
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
    }

    private fun readOrdered(key: String): List<Long> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) add(arr.getLong(i))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun readIds(key: String): Set<Long> = readOrdered(key).toSet()

    private fun writeIds(key: String, ids: List<Long>) {
        val arr = JSONArray()
        ids.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
