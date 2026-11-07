package com.musicfy.app

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Music catalogue: Jamendo (https://developer.jamendo.com) - a licensed catalogue of
 * independent / Creative Commons music that may legally be streamed and downloaded.
 * A free client_id is required.
 */
object JamendoApi {
    private const val BASE = "https://api.jamendo.com/v3.0"
    var clientId: String = ""

    private suspend fun call(path: String, vararg params: Pair<String, String>): JSONArray =
        withContext(Dispatchers.IO) {
            if (clientId.isBlank()) throw IllegalStateException("missing_key")
            val b = Uri.parse("$BASE/$path").buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("format", "json")
            params.forEach { b.appendQueryParameter(it.first, it.second) }
            val c = URL(b.build().toString()).openConnection() as HttpURLConnection
            c.connectTimeout = 15000
            c.readTimeout = 20000
            try {
                val body = c.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val h = json.getJSONObject("headers")
                if (h.optString("status") != "success") {
                    throw IllegalStateException(h.optString("error_message", "API error"))
                }
                json.getJSONArray("results")
            } finally {
                c.disconnect()
            }
        }

    private inline fun <T> JSONArray.mapObjects(f: (JSONObject) -> T): List<T> =
        (0 until length()).map { f(getJSONObject(it)) }

    private fun JSONObject.toTrack(
        albumName: String? = null,
        albumId: String? = null,
        artistName: String? = null,
        artistId: String? = null,
        image: String? = null,
    ): Track {
        val dlAllowed = optBoolean("audiodownload_allowed", true)
        return Track(
            id = optString("id"),
            title = optString("name"),
            artist = artistName ?: optString("artist_name"),
            artistId = artistId ?: optString("artist_id"),
            album = albumName ?: optString("album_name"),
            albumId = albumId ?: optString("album_id"),
            imageUrl = image ?: optString("image").ifBlank { optString("album_image") },
            streamUrl = optString("audio"),
            downloadUrl = if (dlAllowed) optString("audiodownload") else "",
            duration = optInt("duration"),
        )
    }

    suspend fun tracks(
        order: String,
        tag: String? = null,
        query: String? = null,
        artistId: String? = null,
        limit: Int = 30,
    ): List<Track> {
        val p = mutableListOf(
            "limit" to limit.toString(), "order" to order,
            "imagesize" to "300", "audioformat" to "mp32",
        )
        if (tag != null) p += "tags" to tag.lowercase()
        if (query != null) p += "search" to query
        if (artistId != null) p += "artist_id" to artistId
        return call("tracks/", *p.toTypedArray()).mapObjects { it.toTrack() }
    }

    suspend fun searchAlbums(q: String): List<Album> =
        call("albums/", "namesearch" to q, "limit" to "30", "imagesize" to "300")
            .mapObjects { it.toAlbum() }

    suspend fun searchArtists(q: String): List<Artist> =
        call("artists/", "namesearch" to q, "limit" to "30")
            .mapObjects { Artist(it.optString("id"), it.optString("name"), it.optString("image")) }

    private fun JSONObject.toAlbum() = Album(
        id = optString("id"), name = optString("name"), artist = optString("artist_name"),
        artistId = optString("artist_id"), imageUrl = optString("image"),
    )

    suspend fun album(id: String): AlbumDetail {
        val o = call("albums/tracks/", "id" to id, "imagesize" to "500", "audioformat" to "mp32")
            .getJSONObject(0)
        val album = o.toAlbum()
        val tracks = o.optJSONArray("tracks")?.mapObjects {
            it.toTrack(album.name, album.id, album.artist, album.artistId, album.imageUrl)
        } ?: emptyList()
        return AlbumDetail(album, tracks)
    }

    suspend fun artist(id: String): ArtistDetail {
        val a = call("artists/", "id" to id).getJSONObject(0)
        val artist = Artist(a.optString("id"), a.optString("name"), a.optString("image"))
        val tracks = tracks(order = "popularity_total", artistId = id, limit = 40)
        val albums = call("albums/", "artist_id" to id, "limit" to "30", "imagesize" to "300")
            .mapObjects { it.toAlbum() }
        return ArtistDetail(artist, tracks, albums)
    }
}
