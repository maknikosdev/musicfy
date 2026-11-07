package com.musicfy.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.IOException

class AppVM(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("musicfy", 0)
    val store = DownloadStore(app)
    val player = PlayerController(app, store)

    val genres = listOf(
        "Pop", "Rock", "Electronic", "HipHop", "Jazz", "Classical",
        "Lounge", "Metal", "Ambient", "Reggae", "Folk", "Blues"
    )

    var hasKey by mutableStateOf(false)
    var genre by mutableStateOf<String?>(null)
    var popular by mutableStateOf(Async<List<Track>>(loading = true))
    var fresh by mutableStateOf(Async<List<Track>>(loading = true))

    var query by mutableStateOf("")
    var sTracks by mutableStateOf(Async<List<Track>>())
    var sAlbums by mutableStateOf(Async<List<Album>>())
    var sArtists by mutableStateOf(Async<List<Artist>>())

    var album by mutableStateOf(Async<AlbumDetail>())
    var artist by mutableStateOf(Async<ArtistDetail>())

    init {
        JamendoApi.clientId = prefs.getString("client_id", null) ?: BuildConfig.JAMENDO_CLIENT_ID
        hasKey = JamendoApi.clientId.isNotBlank()
        loadHome()
    }

    private fun friendly(e: Exception): String = when {
        e.message == "missing_key" -> "Enter your Jamendo client_id first (Home screen)."
        e is IOException -> "No internet connection. Your Library still works offline."
        else -> e.message ?: "Something went wrong"
    }

    private fun <T> load(set: (Async<T>) -> Unit, f: suspend () -> T) {
        set(Async(loading = true))
        viewModelScope.launch {
            set(try { Async(data = f()) } catch (e: Exception) { Async(error = friendly(e)) })
        }
    }

    fun saveKey(k: String) {
        val key = k.trim()
        prefs.edit().putString("client_id", key).apply()
        JamendoApi.clientId = key
        hasKey = key.isNotBlank()
        loadHome()
    }

    fun selectGenre(g: String?) {
        genre = if (genre == g) null else g
        loadHome()
    }

    fun loadHome() {
        load<List<Track>>({ popular = it }) { JamendoApi.tracks("popularity_week", tag = genre) }
        load<List<Track>>({ fresh = it }) { JamendoApi.tracks("releasedate_desc", tag = genre) }
    }

    fun search(q: String) {
        if (q.isBlank()) {
            sTracks = Async(); sAlbums = Async(); sArtists = Async()
            return
        }
        load<List<Track>>({ sTracks = it }) { JamendoApi.tracks("relevance", query = q, limit = 40) }
        load<List<Album>>({ sAlbums = it }) { JamendoApi.searchAlbums(q) }
        load<List<Artist>>({ sArtists = it }) { JamendoApi.searchArtists(q) }
    }

    fun openAlbum(id: String) = load<AlbumDetail>({ album = it }) { JamendoApi.album(id) }
    fun openArtist(id: String) = load<ArtistDetail>({ artist = it }) { JamendoApi.artist(id) }
}
