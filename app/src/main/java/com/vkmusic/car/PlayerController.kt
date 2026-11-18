package com.vkmusic.car

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

class PlayerController(private val app: App) {
    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setMediaSourceFactory(DefaultMediaSourceFactory(app.dataSourceFactory))
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(), true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    var queue by mutableStateOf<List<Track>>(emptyList())
        private set
    var current by mutableStateOf<Track?>(null)
        private set
    var playing by mutableStateOf(false)
        private set
    var position by mutableLongStateOf(0L)
        private set
    var duration by mutableLongStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeat by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val t = queue.firstOrNull { it.id == mediaItem?.mediaId }
                current = t
                error = null
                if (t != null) app.store.addRecent(t)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (player.duration > 0) duration = player.duration
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "Ошибка воспроизведения: ${e.errorCodeName}"
            }
        })
    }

    private fun item(t: Track): MediaItem {
        // скачанный трек играем по сохранённой ссылке, чтобы сработал офлайн-кэш
        val src = if (app.isDownloaded(t.id)) (app.store.downloadedTrack(t.id) ?: t) else t
        val b = MediaItem.Builder()
            .setMediaId(t.id)
            .setUri(Uri.parse(src.url))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(t.title)
                    .setArtist(t.artist)
                    .apply { t.cover?.let { setArtworkUri(Uri.parse(it)) } }
                    .build()
            )
        if (src.url.contains(".m3u8")) b.setMimeType(MimeTypes.APPLICATION_M3U8)
        return b.build()
    }

    fun play(list: List<Track>, index: Int) {
        if (list.isEmpty()) return
        queue = list
        current = list[index]
        error = null
        app.store.addRecent(list[index])
        player.setMediaItems(list.map { item(it) }, index, 0L)
        player.prepare()
        player.playWhenReady = true
    }

    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun next() = player.seekToNext()
    fun prev() = player.seekToPrevious()
    fun seek(ms: Long) = player.seekTo(ms)

    fun toggleShuffle() {
        shuffle = !shuffle
        player.shuffleModeEnabled = shuffle
    }

    fun toggleRepeat() {
        repeat = !repeat
        player.repeatMode = if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        current = null
        queue = emptyList()
    }

    fun tick() {
        position = player.currentPosition
        if (player.duration > 0) duration = player.duration
    }
}
