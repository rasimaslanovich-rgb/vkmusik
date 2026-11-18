package com.vkmusic.car

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.session.MediaSession
import java.io.File
import java.util.concurrent.Executors

class DlState(val state: Int, val percent: Int)

class App : Application() {
    lateinit var store: Store
    lateinit var cache: SimpleCache
    lateinit var dm: DownloadManager
    lateinit var dataSourceFactory: DataSource.Factory
    lateinit var player: PlayerController
    val dl = mutableStateMapOf<String, DlState>()

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        val db = StandaloneDatabaseProvider(this)
        cache = SimpleCache(File(filesDir, "media"), NoOpCacheEvictor(), db)
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(VkApi.UA)
            .setAllowCrossProtocolRedirects(true)
        dataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(http)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        dm = DownloadManager(this, db, cache, http, Executors.newFixedThreadPool(2))
        dm.maxParallelDownloads = 2
        dm.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(d: DownloadManager, download: Download, finalException: Exception?) {
                dl[download.request.id] = DlState(download.state, download.percentDownloaded.toInt().coerceIn(0, 100))
            }

            override fun onDownloadRemoved(d: DownloadManager, download: Download) {
                dl.remove(download.request.id)
            }
        })
        dm.resumeDownloads()
        try {
            dm.downloadIndex.getDownloads().use { c ->
                while (c.moveToNext()) {
                    val d = c.download
                    dl[d.request.id] = DlState(d.state, d.percentDownloaded.toInt().coerceIn(0, 100))
                }
            }
        } catch (e: Exception) { /* пусто */ }

        player = PlayerController(this)
        MediaSession.Builder(this, player.player).build() // кнопки руля и медиаклавиши
    }

    fun download(t: Track) {
        val b = DownloadRequest.Builder(t.id, Uri.parse(t.url))
        if (t.url.contains(".m3u8")) b.setMimeType(MimeTypes.APPLICATION_M3U8)
        dm.addDownload(b.build())
        store.addDownloaded(t)
    }

    fun removeDownload(id: String) {
        dm.removeDownload(id)
        store.removeDownloaded(id)
    }

    fun isDownloaded(id: String): Boolean = dl[id]?.state == Download.STATE_COMPLETED
}
