package com.vkmusic.car

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

private suspend fun userIdOf(token: String): Long = withContext(Dispatchers.IO) {
val url = "https://api.vk.com/method/users.get".toHttpUrl().newBuilder().addQueryParameter("access_token", token).addQueryParameter("v", "5.131").build()
OkHttpClient().newCall(Request.Builder().url(url).header("User-Agent", VkApi.UA).build()).execute().use { r ->
val j = JSONObject(r.body?.string() ?: throw IOException("Пустой ответ"))
if (j.has("error")) throw IOException(j.getJSONObject("error").optString("error_msg"))
j.getJSONArray("response").getJSONObject(0).getLong("id")
}
}

@Composable
fun TokenLogin(app: App) {
var text by remember { mutableStateOf("") }
var msg by remember { mutableStateOf("") }
var busy by remember { mutableStateOf(false) }
val scope = rememberCoroutineScope()
Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, label = { Text("Или вставьте токен ВК") }, modifier = Modifier.fillMaxWidth())
Button(
onClick = {
scope.launch {
busy = true
msg = ""
try {
val t = text.substringAfter("access_token=", text).substringBefore("&").trim()
VkApi.myAudio(t)
val id = userIdOf(t)
app.store.login(t, id)
} catch (e: Exception) {
msg = e.message ?: "Ошибка"
}
busy = false
}
},
enabled = !busy && text.isNotBlank(),
modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(56.dp),
colors = ButtonDefaults.buttonColors(containerColor = Color(0x1FFFFFFF), contentColor = Color.White)
) { Text(if (busy) "Проверка…" else "Войти по токену", fontSize = 20.sp) }
if (msg.isNotEmpty()) Text(msg, color = Accent, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
}
}
