package com.musicfy.app

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Second music source: Audius (https://docs.audius.org), an open catalogue of independent artists.
 * No API key needed. Stream-only: Audius tracks cannot be downloaded for offline use.
 * All ids from this source are prefixed with "au_" so they never clash with Jamendo ids.
 */
object AudiusApi {
    const val PREFIX = "au_"
    private const val HOST = "https://api.audius.co"
    private const val APP = "MusicFy"

    fun owns(id: String) = id.startsWith(PREFIX)

    private suspend fun get(path: String, vararg params: Pair<String, String>): JSONObject =
        withContext(Dispatchers.IO) {
            val b = Uri.parse("$HOST$path").buildUpon().appendQueryParameter("app_name", APP)
            params.forEach { b.appendQueryParameter(it.first, it.second) }
            val c = URL(b.build().toString()).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 20000
            c.setRequestProperty("Accept", "application/json")
            try {
                val code = c.responseCode
                if (code !in 200..299) throw IllegalStateException("Audius error ($code)")
                JSONObject(c.inputStream.bufferedReader().readText())
            } finally {
                c.disconnect()
            }
        }

    // org.json returns the string "null" for JSON null, so read strings through this helper.
    private fun JSONObject.str(name: String): String =
        if (isNull(name)) "" else optString(name, "")

    private fun JSONObject.list(): List<JSONObject> {
        val arr: JSONArray = optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    private fun JSONObject.one(): JSONObject? =
        optJSONObject("data") ?: optJSONArray("data")?.optJSONObject(0)

    private fun JSONObject?.img(): String {
        if (this == null) return ""
        return str("480x480").ifBlank { str("1000x1000").ifBlank { str("150x150") } }
    }

    private fun JSONObject.toTrack(): Track? {
        val id = str("id")
        if (id.isBlank()) return null
        if (has("is_streamable") && !optBoolean("is_streamable", true)) return null
        val u = optJSONObject("user")
        val uid = u?.str("id").orEmpty()
        return Track(
            id = PREFIX + id,
            title = str("title"),
            artist = u?.str("name").orEmpty(),
            artistId = if (uid.isBlank()) "" else PREFIX + uid,
            album = "",
            albumId = "",
            imageUrl = optJSONObject("artwork").img(),
            streamUrl = "$HOST/v1/tracks/$id/stream?app_name=$APP",
            downloadUrl = "", // stream-only
            duration = optInt("duration"),
        )
    }

    private fun JSONObject.toAlbum(): Album? {
        val id = str("id")
        if (id.isBlank()) return null
        val u = optJSONObject("user")
        val uid = u?.str("id").orEmpty()
        return Album(
            id = PREFIX + id,
            name = str("playlist_name").ifBlank { str("name") },
            artist = u?.str("name").orEmpty(),
            artistId = if (uid.isBlank()) "" else PREFIX + uid,
            imageUrl = optJSONObject("artwork").img(),
        )
    }

    private fun JSONObject.toArtist(): Artist? {
        val id = str("id")
        if (id.isBlank()) return null
        return Artist(PREFIX + id, str("name"), optJSONObject("profile_picture").img())
    }

    suspend fun trending(genre: String?, time: String): List<Track> {
        val p = mutableListOf("time" to time, "limit" to "30")
        if (genre != null) p += "genre" to genre
        return get("/v1/tracks/trending", *p.toTypedArray()).list().mapNotNull { it.toTrack() }
    }

    suspend fun searchTracks(q: String): List<Track> =
        get("/v1/tracks/search", "query" to q, "limit" to "40").list().mapNotNull { it.toTrack() }

    suspend fun searchAlbums(q: String): List<Album> =
        get("/v1/playlists/search", "query" to q, "limit" to "30").list().mapNotNull { it.toAlbum() }

    suspend fun searchArtists(q: String): List<Artist> =
        get("/v1/users/search", "query" to q, "limit" to "30").list().mapNotNull { it.toArtist() }

    suspend fun album(prefixedId: String): AlbumDetail {
        val id = prefixedId.removePrefix(PREFIX)
        val meta = get("/v1/playlists/$id").one()
        val album = meta?.toAlbum()
            ?: Album(prefixedId, "Album", "", "", "")
        val tracks = get("/v1/playlists/$id/tracks").list()
            .mapNotNull { it.toTrack() }
            .map { it.copy(album = album.name, albumId = album.id, imageUrl = it.imageUrl.ifBlank { album.imageUrl }) }
        return AlbumDetail(album, tracks)
    }

    suspend fun artist(prefixedId: String): ArtistDetail {
        val id = prefixedId.removePrefix(PREFIX)
        val artist = get("/v1/users/$id").one()?.toArtist()
            ?: Artist(prefixedId, "Artist", "")
        val tracks = get("/v1/users/$id/tracks", "limit" to "40").list().mapNotNull { it.toTrack() }
        // Albums/playlists of an artist are optional: if this call fails, just show the tracks.
        val albums = try {
            get("/v1/users/$id/playlists").list().mapNotNull { it.toAlbum() }
        } catch (e: Exception) {
            emptyList()
        }
        return ArtistDetail(artist, tracks, albums)
    }
}
