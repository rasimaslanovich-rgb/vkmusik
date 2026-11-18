package com.vkmusic.car

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val url: String,
    val cover: String?
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("title", title).put("artist", artist)
        .put("duration", duration).put("url", url).put("cover", cover ?: "")

    companion object {
        fun fromJson(o: JSONObject) = Track(
            o.getString("id"), o.getString("title"), o.getString("artist"),
            o.optInt("duration"), o.getString("url"),
            o.optString("cover").ifBlank { null }
        )
    }
}

data class Playlist(val id: Long, val owner: Long, val title: String, val key: String?, val count: Int)

class Store(ctx: Context) {
    private val p = ctx.getSharedPreferences("vkmusic", Context.MODE_PRIVATE)

    var token by mutableStateOf(p.getString("token", null))
        private set
    var userId by mutableStateOf(p.getLong("uid", 0L))
        private set
    var updateUrl by mutableStateOf(p.getString("update_url", "") ?: "")
        private set
    var favorites by mutableStateOf(load("fav"))
        private set
    var recent by mutableStateOf(load("recent"))
        private set
    var downloaded by mutableStateOf(load("downloaded"))
        private set

    fun login(t: String, uid: Long) {
        token = t; userId = uid
        p.edit().putString("token", t).putLong("uid", uid).apply()
    }

    fun logout() {
        token = null; userId = 0L
        p.edit().remove("token").remove("uid").apply()
    }

    fun saveUpdateUrl(u: String) {
        updateUrl = u
        p.edit().putString("update_url", u).apply()
    }

    fun toggleFav(t: Track) {
        favorites = if (favorites.any { it.id == t.id }) favorites.filter { it.id != t.id } else listOf(t) + favorites
        save("fav", favorites)
    }

    fun addRecent(t: Track) {
        recent = (listOf(t) + recent.filter { it.id != t.id }).take(50)
        save("recent", recent)
    }

    fun addDownloaded(t: Track) {
        downloaded = listOf(t) + downloaded.filter { it.id != t.id }
        save("downloaded", downloaded)
    }

    fun removeDownloaded(id: String) {
        downloaded = downloaded.filter { it.id != id }
        save("downloaded", downloaded)
    }

    fun downloadedTrack(id: String): Track? = downloaded.firstOrNull { it.id == id }

    private fun load(key: String): List<Track> = try {
        val a = JSONArray(p.getString(key, "[]"))
        (0 until a.length()).map { Track.fromJson(a.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save(key: String, list: List<Track>) {
        val a = JSONArray()
        list.forEach { a.put(it.toJson()) }
        p.edit().putString(key, a.toString()).apply()
    }
}
