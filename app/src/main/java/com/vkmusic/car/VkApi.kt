package com.vkmusic.car

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Неофициальный доступ к музыке ВК через клиентский ключ Kate Mobile.
 * Официально методы audio.* закрыты, поэтому в любой момент это может перестать работать.
 */
object VkApi {
    const val UA = "KateMobileAndroid/56 lite-460 (Android 4.4.2; SDK 19; x86; unknown Android SDK built for x86; en)"
    const val AUTH_URL = "https://oauth.vk.com/authorize?client_id=2685278&display=mobile" +
        "&redirect_uri=https%3A%2F%2Foauth.vk.com%2Fblank.html&scope=1073737727&response_type=token&v=5.131&revoke=1"

    private val http = OkHttpClient()

    private suspend fun call(method: String, token: String, params: Map<String, String>): Any =
        withContext(Dispatchers.IO) {
            val b = "https://api.vk.com/method/$method".toHttpUrl().newBuilder()
                .addQueryParameter("access_token", token)
                .addQueryParameter("v", "5.131")
            params.forEach { (k, v) -> b.addQueryParameter(k, v) }
            val req = Request.Builder().url(b.build()).header("User-Agent", UA).build()
            http.newCall(req).execute().use { r ->
                val text = r.body?.string() ?: throw IOException("Пустой ответ")
                val j = JSONObject(text)
                if (j.has("error")) {
                    val e = j.getJSONObject("error")
                    throw IOException("ВК: ${e.optString("error_msg")} (${e.optInt("error_code")})")
                }
                j.get("response")
            }
        }

    private fun items(r: Any): JSONArray =
        if (r is JSONArray) r else (r as JSONObject).getJSONArray("items")

    private fun parse(a: JSONArray): List<Track> = (0 until a.length()).mapNotNull { i ->
        val o = a.getJSONObject(i)
        val url = o.optString("url")
        if (url.isBlank()) null else Track(
            id = "${o.optLong("owner_id")}_${o.optLong("id")}",
            title = o.optString("title"),
            artist = o.optString("artist"),
            duration = o.optInt("duration"),
            url = url,
            cover = o.optJSONObject("album")?.optJSONObject("thumb")?.optString("photo_300")?.ifBlank { null }
        )
    }

    suspend fun myAudio(token: String): List<Track> =
        parse(items(call("audio.get", token, mapOf("count" to "500"))))

    suspend fun search(token: String, q: String): List<Track> =
        parse(items(call("audio.search", token, mapOf("q" to q, "count" to "100", "auto_complete" to "1"))))

    suspend fun playlists(token: String, uid: Long): List<Playlist> {
        val a = items(call("audio.getPlaylists", token, mapOf("owner_id" to uid.toString(), "count" to "100")))
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Playlist(o.getLong("id"), o.getLong("owner_id"), o.optString("title"),
                o.optString("access_key").ifBlank { null }, o.optInt("count"))
        }
    }

    suspend fun playlistTracks(token: String, p: Playlist): List<Track> {
        val m = mutableMapOf("owner_id" to p.owner.toString(), "playlist_id" to p.id.toString(), "count" to "500")
        p.key?.let { m["access_key"] = it }
        return parse(items(call("audio.get", token, m)))
    }

    /** Ссылки на треки быстро «протухают», поэтому перед воспроизведением избранного и истории обновляем их. */
    suspend fun refresh(token: String, list: List<Track>): List<Track> {
        val fresh = HashMap<String, Track>()
        list.map { it.id }.chunked(100).forEach { ids ->
            val r = call("audio.getById", token, mapOf("audios" to ids.joinToString(",")))
            parse(items(r)).forEach { fresh[it.id] = it }
        }
        return list.map { fresh[it.id] ?: it }
    }
}
