package com.vkmusic.car

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.offline.Download
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Accent = Color(0xFFF0345A)
val Ink = Color(0xFFD6D6DE)
val Muted = Color(0xFF9A9AA6)
val Panel = Color(0x12FFFFFF)

enum class Screen(val label: String) {
    NOW("Сейчас играет"), SEARCH("Поиск"), MY("Моя музыка"), FAV("Избранное"),
    PLAYLISTS("Плейлисты"), RECENT("Недавно прослушанные"), DOWNLOADS("Скачанное"), SETTINGS("Настройки")
}

fun Modifier.appBg(): Modifier = this.background(Color(0xFF07070B)).drawBehind {
    drawRect(Brush.radialGradient(listOf(Color(0x8C781E3C), Color.Transparent),
        center = Offset(size.width * 0.62f, size.height * 0.4f), radius = size.width * 0.5f))
    drawRect(Brush.radialGradient(listOf(Color(0x73961946), Color.Transparent),
        center = Offset(size.width * 0.4f, size.height * 0.95f), radius = size.width * 0.4f))
    drawRect(Brush.radialGradient(listOf(Color(0x733C328C), Color.Transparent),
        center = Offset(size.width * 0.55f, 0f), radius = size.width * 0.35f))
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

// Интерфейс нарисован на холсте 1280x720 и масштабируется под экран магнитолы
@Composable
fun AppRoot(app: App) {
    val m = LocalContext.current.resources.displayMetrics
    val s = minOf(m.widthPixels / 1280f, m.heightPixels / 720f)
    CompositionLocalProvider(LocalDensity provides Density(s, 1f)) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Color.Black, surface = Color(0xFF1E1E26))) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Box(Modifier.requiredSize(1280.dp, 720.dp)) {
                    if (app.store.token == null) LoginFlow(app) else Shell(app)
                }
            }
        }
    }
}

@Composable
fun LoginFlow(app: App) {
    var web by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().appBg()) {
        if (web) {
            VkWebView(onToken = { t, u -> app.store.login(t, u) }, onCancel = { web = false })
        } else {
            Column(
                Modifier.align(Alignment.Center).width(520.dp).clip(RoundedCornerShape(32.dp)).background(Panel).padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF2787F5)), Alignment.Center) {
                    Text("VK", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Text("VK Музыка", fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
                Text(
                    "Войдите через ВКонтакте, чтобы слушать свою музыку и скачивать треки для офлайна",
                    fontSize = 22.sp, color = Muted, modifier = Modifier.padding(top = 12.dp, bottom = 32.dp)
                )
                Button(
                    onClick = { web = true },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2787F5), contentColor = Color.White)
                ) { Text("Войти через ВКонтакте", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                Text("Данные входа хранятся только на устройстве", fontSize = 17.sp, color = Muted,
                    modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
fun VkWebView(onToken: (String, Long) -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        TextButton(onClick = onCancel) { Text("← Назад", fontSize = 22.sp) }
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    private fun handle(url: String?): Boolean {
                        if (url != null && url.startsWith("https://oauth.vk.com/blank.html") && url.contains("access_token=")) {
                            val q = Uri.parse(url.replace('#', '?'))
                            val t = q.getQueryParameter("access_token")
                            val u = q.getQueryParameter("user_id")?.toLongOrNull() ?: 0L
                            if (t != null) {
                                onToken(t, u)
                                return true
                            }
                        }
                        return false
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                        handle(request?.url?.toString())

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        handle(url)
                    }
                }
                loadUrl(VkApi.AUTH_URL)
            }
        })
    }
}

@Composable
fun Shell(app: App) {
    var screen by remember { mutableStateOf(Screen.NOW) }
    val c = app.player
    LaunchedEffect(Unit) { while (true) { c.tick(); delay(500) } }
    val go = { screen = Screen.NOW }

    Box(Modifier.fillMaxSize().appBg()) {
        TopBar(Modifier.align(Alignment.TopStart))
        Sidebar(screen, { screen = it }, Modifier.align(Alignment.TopStart).offset(12.dp, 98.dp))
        Box(Modifier.fillMaxSize().padding(start = 247.dp, top = 92.dp, end = 24.dp, bottom = 122.dp)) {
            when (screen) {
                Screen.NOW -> NowPlaying(app)
                Screen.SEARCH -> SearchScreen(app, go)
                Screen.MY -> MyMusic(app, false, go)
                Screen.DOWNLOADS -> MyMusic(app, true, go)
                Screen.FAV -> SavedList(app, "Избранное", app.store.favorites, go)
                Screen.RECENT -> SavedList(app, "Недавно прослушанные", app.store.recent, go)
                Screen.PLAYLISTS -> PlaylistsScreen(app, go)
                Screen.SETTINGS -> SettingsScreen(app)
            }
        }
        BottomBar(screen, { screen = it }, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}

@Composable
fun TopBar(modifier: Modifier) {
    val time by produceState("") {
        while (true) {
            value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(10_000)
        }
    }
    Row(
        modifier.fillMaxWidth().height(80.dp).padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Icon(Icons.Default.Home, null, Modifier.size(38.dp), tint = Color.White)
        Text(time, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2787F5)), Alignment.Center) {
            Text("VK", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text("VK Музыка", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.offset((-14).dp, 0.dp))
    }
}

@Composable
fun Sidebar(cur: Screen, onSel: (Screen) -> Unit, modifier: Modifier) {
    val items = listOf(
        Screen.NOW to Icons.Default.MusicNote, Screen.SEARCH to Icons.Default.Search,
        Screen.MY to Icons.Default.Menu, Screen.FAV to Icons.Default.FavoriteBorder,
        Screen.PLAYLISTS to Icons.Default.Equalizer, Screen.RECENT to Icons.Default.History,
        Screen.SETTINGS to Icons.Default.Settings
    )
    Column(
        modifier.width(214.dp).height(496.dp).clip(RoundedCornerShape(30.dp)).background(Panel).padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { (sc, ic) ->
            val sel = cur == sc
            Row(
                Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) Brush.horizontalGradient(listOf(Color(0x8CF0345A), Color(0x26F0345A))) else SolidColor(Color.Transparent))
                    .clickable { onSel(sc) }.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(ic, null, Modifier.size(32.dp), tint = Color.White)
                Text(sc.label, fontSize = 17.sp, lineHeight = 19.sp, color = Color(0xFFEEEEEE))
            }
        }
    }
}

@Composable
fun BottomBar(cur: Screen, onSel: (Screen) -> Unit, modifier: Modifier) {
    val tabs = listOf(
        Triple(Screen.NOW, "Главная", Icons.Default.Home),
        Triple(Screen.SEARCH, "Поиск", Icons.Default.Search),
        Triple(Screen.MY, "Моя музыка", Icons.Default.LibraryMusic),
        Triple(Screen.PLAYLISTS, "Плейлисты", Icons.Default.QueueMusic)
    )
    Row(
        modifier.fillMaxWidth().height(98.dp).clip(RoundedCornerShape(49.dp)).background(Panel).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (sc, label, ic) -> Tab(sc == cur, label, ic, Modifier.weight(1f)) { onSel(sc) } }
        Box(Modifier.padding(horizontal = 20.dp).width(2.dp).height(60.dp).background(Color(0x2EFFFFFF)))
        Tab(cur == Screen.DOWNLOADS, "Скачанное", Icons.Default.FileDownload, Modifier.width(150.dp)) { onSel(Screen.DOWNLOADS) }
    }
}

@Composable
fun Tab(sel: Boolean, label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(40.dp), tint = if (sel) Accent else Ink)
        Text(label, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Accent else Ink)
    }
}

@Composable
fun Cover(url: String?, modifier: Modifier, radius: Int) {
    Box(
        modifier.clip(RoundedCornerShape(radius.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFF6A88), Color(0xFF4A2C8A))))
    ) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.MusicNote, null, Modifier.align(Alignment.Center).size(48.dp), tint = Color(0xB3FFFFFF))
        }
    }
}

@Composable
fun IconBtn(icon: ImageVector, tint: Color, size: Int, onClick: () -> Unit) {
    Box(Modifier.size(80.dp).clip(CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Icon(icon, null, Modifier.size(size.dp), tint = tint)
    }
}

@Composable
fun DlIcon(app: App, t: Track, size: Dp = 34.dp) {
    val st = app.dl[t.id]
    Box(
        Modifier.size(52.dp).clip(CircleShape).clickable {
            if (st?.state == Download.STATE_COMPLETED) app.removeDownload(t.id)
            else if (st == null || st.state == Download.STATE_FAILED) app.download(t)
        },
        Alignment.Center
    ) {
        when {
            st?.state == Download.STATE_COMPLETED -> Icon(Icons.Default.CheckCircle, null, Modifier.size(size), tint = Accent)
            st != null && st.state != Download.STATE_FAILED ->
                Text("${st.percent}%", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            else -> Icon(Icons.Default.FileDownload, null, Modifier.size(size), tint = Muted)
        }
    }
}

@Composable
fun NowPlaying(app: App) {
    val c = app.player
    val t = c.current
    if (t == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Выберите трек в «Моей музыке» или через поиск", fontSize = 28.sp, color = Muted)
        }
        return
    }
    val fav = app.store.favorites.any { it.id == t.id }
    var drag by remember { mutableStateOf<Float?>(null) }
    val dur = maxOf(c.duration, t.duration * 1000L, 1L)

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(t.cover, Modifier.size(310.dp, 320.dp), 24)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 44.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, fontSize = 32.sp, color = Color(0xFFCFCFD8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.padding(start = 12.dp).size(68.dp).clip(CircleShape).background(Color(0x1AFFFFFF)), Alignment.Center) {
                    DlIcon(app, t)
                }
                Box(
                    Modifier.padding(start = 12.dp).size(68.dp).clip(CircleShape).background(Color(0x1AFFFFFF))
                        .clickable { app.store.toggleFav(t) },
                    Alignment.Center
                ) {
                    Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(36.dp),
                        tint = if (fav) Accent else Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
            Slider(
                value = drag ?: c.position.toFloat().coerceIn(0f, dur.toFloat()),
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { c.seek(it.toLong()) }; drag = null },
                valueRange = 0f..dur.toFloat(),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Accent, inactiveTrackColor = Color(0x33FFFFFF))
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(c.position), fontSize = 22.sp, color = Ink, modifier = Modifier.weight(1f))
                Text("−" + fmt(dur - c.position), fontSize = 22.sp, color = Ink)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconBtn(Icons.Default.Shuffle, if (c.shuffle) Accent else Ink, 40) { c.toggleShuffle() }
                IconBtn(Icons.Default.SkipPrevious, Color.White, 56) { c.prev() }
                Box(Modifier.size(118.dp).clip(CircleShape).background(Color(0x1FFFFFFF)).clickable { c.toggle() }, Alignment.Center) {
                    Icon(if (c.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(64.dp), tint = Color.White)
                }
                IconBtn(Icons.Default.SkipNext, Color.White, 56) { c.next() }
                IconBtn(Icons.Default.Repeat, if (c.repeat) Accent else Ink, 40) { c.toggleRepeat() }
            }
            c.error?.let { Text(it, color = Accent, fontSize = 18.sp) }
        }
    }
}

@Composable
fun Header(title: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 38.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun Msg(t: String) {
    Text(t, color = Muted, fontSize = 22.sp, modifier = Modifier.padding(8.dp))
}

@Composable
fun Chip(text: String, sel: Boolean, onClick: () -> Unit) {
    Text(
        text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else Ink,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) Accent else Color(0x14FFFFFF))
            .clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
fun TrackRows(app: App, list: List<Track>, modifier: Modifier = Modifier, onPlay: (Int) -> Unit) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(list, key = { i, t -> "${t.id}#$i" }) { i, t ->
            val playing = app.player.current?.id == t.id
            Row(
                Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (playing) Color(0x29F0345A) else Color.Transparent)
                    .clickable { onPlay(i) }.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Cover(t.cover, Modifier.size(50.dp), 10)
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, fontSize = 18.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(fmt(t.duration * 1000L), fontSize = 19.sp, color = Muted)
                DlIcon(app, t)
            }
        }
    }
}

@Composable
fun MyMusic(app: App, startDl: Boolean, openNow: () -> Unit) {
    var onlyDl by remember { mutableStateOf(startDl) }
    var items by remember { mutableStateOf<List<Track>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { items = VkApi.myAudio(app.store.token!!) } catch (e: Exception) { err = e.message ?: "Нет связи с ВК" }
    }
    val done = app.store.downloaded.filter { app.isDownloaded(it.id) }
    val shown = if (onlyDl) done else (items ?: emptyList())
    Column(Modifier.fillMaxSize()) {
        Header(if (onlyDl) "Скачанное" else "Моя музыка") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Все", !onlyDl) { onlyDl = false }
                Chip("Скачанные", onlyDl) { onlyDl = true }
            }
        }
        if (!onlyDl && items == null && err == null) Msg("Загрузка…")
        if (!onlyDl && err != null) Msg("Не удалось загрузить: $err. Скачанные треки доступны без интернета.")
        if (onlyDl && done.isEmpty()) Msg("Пока ничего не скачано. Нажмите на иконку скачивания у трека.")
        TrackRows(app, shown, Modifier.weight(1f)) { i -> app.player.play(shown, i); openNow() }
    }
}

@Composable
fun SavedList(app: App, title: String, list: List<Track>, openNow: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Header(title)
        if (list.isEmpty()) Msg("Пока пусто")
        TrackRows(app, list, Modifier.weight(1f)) { i ->
            scope.launch {
                val fresh = try { VkApi.refresh(app.store.token!!, list) } catch (e: Exception) { list }
                app.player.play(fresh, i)
                openNow()
            }
        }
    }
}

@Composable
fun SearchScreen(app: App, openNow: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Track>>(emptyList()) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val kb = LocalSoftwareKeyboardController.current
    fun run() {
        if (q.isBlank()) return
        scope.launch {
            msg = "Поиск…"
            try {
                res = VkApi.search(app.store.token!!, q)
                msg = if (res.isEmpty()) "Ничего не найдено" else ""
            } catch (e: Exception) { msg = e.message ?: "Ошибка поиска" }
        }
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            label = { Text("Поиск музыки") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { kb?.hide(); run() }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        if (msg.isNotEmpty()) Msg(msg)
        TrackRows(app, res, Modifier.weight(1f)) { i -> app.player.play(res, i); openNow() }
    }
}

@Composable
fun PlaylistsScreen(app: App, openNow: () -> Unit) {
    var lists by remember { mutableStateOf<List<Playlist>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<Playlist?>(null) }
    var tracks by remember { mutableStateOf<List<Track>?>(null) }
    LaunchedEffect(Unit) {
        try { lists = VkApi.playlists(app.store.token!!, app.store.userId) } catch (e: Exception) { err = e.message ?: "Ошибка" }
    }
    LaunchedEffect(open) {
        val p = open
        tracks = null
        if (p != null) {
            try { tracks = VkApi.playlistTracks(app.store.token!!, p) } catch (e: Exception) { err = e.message ?: "Ошибка" }
        }
    }
    val p = open
    Column(Modifier.fillMaxSize()) {
        if (p == null) {
            Header("Плейлисты")
            if (lists == null && err == null) Msg("Загрузка…")
            err?.let { Msg("Не удалось загрузить: $it") }
            if (lists?.isEmpty() == true) Msg("Плейлистов нет")
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(lists ?: emptyList()) { _, pl ->
                    Row(
                        Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(16.dp)).clickable { open = pl }.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Cover(null, Modifier.size(50.dp), 10)
                        Text(pl.title, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("${pl.count}", fontSize = 19.sp, color = Muted)
                    }
                }
            }
        } else {
            Header(p.title) { Chip("← Назад", false) { open = null } }
            val tr = tracks
            if (tr == null) Msg("Загрузка…")
            else TrackRows(app, tr, Modifier.weight(1f)) { i -> app.player.play(tr, i); openNow() }
        }
    }
}

@Composable
fun SettingsCard(modifier: Modifier, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(Panel).padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun SettingsScreen(app: App) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    var status by remember { mutableStateOf("") }
    var prog by remember { mutableStateOf(-1) }
    val btn = ButtonDefaults.buttonColors(containerColor = Color(0x1FFFFFFF), contentColor = Color.White)

    Column(Modifier.fillMaxSize()) {
        Header("Настройки")
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SettingsCard(Modifier.fillMaxWidth(), "Аккаунт") {
                    Text("ID ВКонтакте: ${app.store.userId}", fontSize = 21.sp, color = Ink)
                    Button(onClick = { app.player.stop(); app.store.logout() }, colors = btn) { Text("Выйти", fontSize = 21.sp) }
                }
                SettingsCard(Modifier.fillMaxWidth(), "Загрузки") {
                    Text("Скачано треков: ${app.store.downloaded.count { app.isDownloaded(it.id) }}", fontSize = 21.sp, color = Ink)
                    Button(onClick = { app.store.downloaded.toList().forEach { app.removeDownload(it.id) } }, colors = btn) {
                        Text("Удалить все загрузки", fontSize = 21.sp)
                    }
                }
            }
            SettingsCard(Modifier.weight(1f).fillMaxSize(), "Обновление приложения") {
                Text("Установлена версия: ${Updater.installedName(ctx)}", fontSize = 21.sp, color = Ink)
                OutlinedTextField(
                    value = app.store.updateUrl, onValueChange = { app.store.saveUpdateUrl(it) },
                    label = { Text("Ссылка на version.json в облаке") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    scope.launch {
                        status = "Проверка…"
                        try {
                            val i = Updater.check(app.store.updateUrl.trim())
                            info = i
                            status = if (i.code > Updater.installedCode(ctx)) "Доступна ${i.name}. ${i.changes}" else "Установлена последняя версия"
                        } catch (e: Exception) { status = "Ошибка: ${e.message}" }
                    }
                }, colors = btn) { Text("Проверить обновления", fontSize = 21.sp) }
                if (status.isNotEmpty()) Text(status, fontSize = 19.sp, color = Muted)
                val i = info
                if (i != null && i.code > Updater.installedCode(ctx)) {
                    Button(onClick = {
                        scope.launch {
                            try {
                                prog = 0
                                val f = Updater.download(ctx, i) { prog = it }
                                prog = -1
                                Updater.install(ctx, f)
                            } catch (e: Exception) { prog = -1; status = "Ошибка загрузки: ${e.message}" }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)) {
                        Text("Скачать и установить", fontSize = 21.sp)
                    }
                }
                if (prog >= 0) Text("Скачивание… $prog%", fontSize = 19.sp, color = Accent)
            }
        }
    }
}
