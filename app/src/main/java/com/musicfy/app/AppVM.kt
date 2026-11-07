package com.musicfy.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException

enum class Source(val label: String) { JAMENDO("Jamendo"), AUDIUS("Audius") }

class AppVM(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("musicfy", 0)
    val store = DownloadStore(app)
    val player = PlayerController(app, store)
    val playlists = PlaylistStore(app)

    /** Track currently being added to a playlist (shows the "Add to playlist" dialog). */
    var addTarget by mutableStateOf<Track?>(null)
    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)

    private val jamendoGenres = listOf(
        "Pop", "Rock", "Electronic", "HipHop", "Jazz", "Classical",
        "Lounge", "Metal", "Ambient", "Reggae", "Folk", "Blues"
    )
    private val audiusGenres = listOf(
        "Pop", "Rock", "Electronic", "Hip-Hop/Rap", "Jazz", "Classical",
        "Metal", "Ambient", "Reggae", "Folk", "Blues", "Lo-Fi"
    )

    var source by mutableStateOf(Source.JAMENDO)
    val genres: List<String> get() = if (source == Source.AUDIUS) audiusGenres else jamendoGenres
    val freshTitle: String get() = if (source == Source.AUDIUS) "All-time favourites" else "New releases"

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

    fun addToPlaylist(playlistId: String, track: Track) {
        val name = playlists.playlists.value.firstOrNull { it.id == playlistId }?.name ?: return
        toasts.tryEmit(
            if (playlists.addTrack(playlistId, track)) "Added to \"$name\"" else "Already in \"$name\""
        )
    }

    fun createAndAdd(name: String, track: Track) {
        val p = playlists.create(name)
        playlists.addTrack(p.id, track)
        toasts.tryEmit("Added to \"${p.name}\"")
    }

    fun selectSource(s: Source) {
        if (s == source) return
        source = s
        genre = null
        loadHome()
        search(query)
    }

    fun loadHome() {
        if (source == Source.AUDIUS) {
            load<List<Track>>({ popular = it }) { AudiusApi.trending(genre, "week") }
            load<List<Track>>({ fresh = it }) { AudiusApi.trending(genre, "allTime") }
        } else {
            load<List<Track>>({ popular = it }) { JamendoApi.tracks("popularity_week", tag = genre) }
            load<List<Track>>({ fresh = it }) { JamendoApi.tracks("releasedate_desc", tag = genre) }
        }
    }

    fun search(q: String) {
        if (q.isBlank()) {
            sTracks = Async(); sAlbums = Async(); sArtists = Async()
            return
        }
        if (source == Source.AUDIUS) {
            load<List<Track>>({ sTracks = it }) { AudiusApi.searchTracks(q) }
            load<List<Album>>({ sAlbums = it }) { AudiusApi.searchAlbums(q) }
            load<List<Artist>>({ sArtists = it }) { AudiusApi.searchArtists(q) }
        } else {
            load<List<Track>>({ sTracks = it }) { JamendoApi.tracks("relevance", query = q, limit = 40) }
            load<List<Album>>({ sAlbums = it }) { JamendoApi.searchAlbums(q) }
            load<List<Artist>>({ sArtists = it }) { JamendoApi.searchArtists(q) }
        }
    }

    fun openAlbum(id: String) = load<AlbumDetail>({ album = it }) {
        if (AudiusApi.owns(id)) AudiusApi.album(id) else JamendoApi.album(id)
    }

    fun openArtist(id: String) = load<ArtistDetail>({ artist = it }) {
        if (AudiusApi.owns(id)) AudiusApi.artist(id) else JamendoApi.artist(id)
    }
}
