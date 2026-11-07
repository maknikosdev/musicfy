package com.musicfy.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Offline library: files live in the app's private storage, playable only through MusicFy. */
class DownloadStore(ctx: Context) {
    private val dir = File(ctx.filesDir, "music").apply { mkdirs() }
    private val index = File(dir, "library.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(2)

    val downloaded = MutableStateFlow(load())
    val progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val errors = MutableSharedFlow<String>(extraBufferCapacity = 8)

    fun file(t: Track) = File(dir, "${t.id}.mp3")
    fun cover(t: Track) = File(dir, "${t.id}.jpg")
    fun isDownloaded(id: String) = downloaded.value.any { it.id == id }

    private fun load(): List<Track> = try {
        if (!index.exists()) emptyList() else {
            val arr = JSONArray(index.readText())
            (0 until arr.length()).map { trackFromJson(arr.getJSONObject(it)) }
                .filter { File(dir, "${it.id}.mp3").exists() }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun persist() {
        index.writeText(JSONArray(downloaded.value.map { it.toJson() }).toString())
    }

    private fun setProgress(id: String, p: Float?) = progress.update { m ->
        if (p == null) m - id else m + (id to p)
    }

    fun downloadAll(list: List<Track>) = list.forEach { download(it) }

    fun download(t: Track) {
        if (isDownloaded(t.id) || progress.value.containsKey(t.id)) return
        val url = t.downloadUrl
        if (url.isBlank()) {
            errors.tryEmit("\"${t.title}\" can't be downloaded")
            return
        }
        setProgress(t.id, 0f)
        scope.launch {
            val part = File(dir, "${t.id}.part")
            try {
                gate.withPermit {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 15000
                    c.readTimeout = 30000
                    try {
                        val total = c.contentLengthLong
                        c.inputStream.use { input ->
                            part.outputStream().use { out ->
                                val buf = ByteArray(32 * 1024)
                                var done = 0L
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                    done += n
                                    setProgress(
                                        t.id,
                                        if (total > 0) (done.toFloat() / total).coerceIn(0.01f, 0.99f) else 0.05f
                                    )
                                }
                            }
                        }
                    } finally {
                        c.disconnect()
                    }
                }
                if (!part.renameTo(file(t))) throw IllegalStateException("rename failed")
                downloadCover(t)
                downloaded.update { it + t }
                persist()
            } catch (e: Exception) {
                part.delete()
                errors.tryEmit("Download failed: ${t.title}")
            } finally {
                setProgress(t.id, null)
            }
        }
    }

    private fun downloadCover(t: Track) {
        if (t.imageUrl.isBlank()) return
        try {
            val c = URL(t.imageUrl).openConnection() as HttpURLConnection
            c.connectTimeout = 10000
            c.readTimeout = 15000
            c.inputStream.use { i -> cover(t).outputStream().use { o -> i.copyTo(o) } }
            c.disconnect()
        } catch (_: Exception) {
            cover(t).delete()
        }
    }

    fun remove(t: Track) {
        file(t).delete()
        cover(t).delete()
        downloaded.update { list -> list.filterNot { it.id == t.id } }
        persist()
    }
}
