package com.musicfy.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Playlist(val id: String, val name: String, val tracks: List<Track>)

/** User playlists, saved as JSON in the app's private storage (they work offline). */
class PlaylistStore(ctx: Context) {
    private val file = File(ctx.filesDir, "playlists.json")
    val playlists = MutableStateFlow(load())

    private fun load(): List<Playlist> = try {
        if (!file.exists()) emptyList() else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val t = o.optJSONArray("tracks") ?: JSONArray()
                Playlist(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    tracks = (0 until t.length()).map { trackFromJson(t.getJSONObject(it)) }
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save() {
        val arr = JSONArray()
        playlists.value.forEach { p ->
            arr.put(
                JSONObject().put("id", p.id).put("name", p.name)
                    .put("tracks", JSONArray(p.tracks.map { it.toJson() }))
            )
        }
        file.writeText(arr.toString())
    }

    /** Creates a playlist and returns it. A blank name becomes "New playlist". */
    fun create(name: String): Playlist {
        val p = Playlist(UUID.randomUUID().toString(), name.trim().ifBlank { "New playlist" }, emptyList())
        playlists.update { it + p }
        save()
        return p
    }

    fun rename(id: String, name: String) {
        val n = name.trim()
        if (n.isBlank()) return
        playlists.update { list -> list.map { if (it.id == id) it.copy(name = n) else it } }
        save()
    }

    fun delete(id: String) {
        playlists.update { list -> list.filterNot { it.id == id } }
        save()
    }

    /** Returns false if the track was already in the playlist. */
    fun addTrack(id: String, track: Track): Boolean {
        val p = playlists.value.firstOrNull { it.id == id } ?: return false
        if (p.tracks.any { it.id == track.id }) return false
        playlists.update { list -> list.map { if (it.id == id) it.copy(tracks = it.tracks + track) else it } }
        save()
        return true
    }

    fun removeTrack(id: String, trackId: String) {
        playlists.update { list ->
            list.map { if (it.id == id) it.copy(tracks = it.tracks.filterNot { t -> t.id == trackId }) else it }
        }
        save()
    }
}
