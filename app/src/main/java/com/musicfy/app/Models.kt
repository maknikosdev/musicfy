package com.musicfy.app

import org.json.JSONObject

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val imageUrl: String,
    val streamUrl: String,
    val downloadUrl: String,
    val duration: Int, // seconds
)

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String,
    val imageUrl: String,
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
)

data class AlbumDetail(val album: Album, val tracks: List<Track>)
data class ArtistDetail(val artist: Artist, val tracks: List<Track>, val albums: List<Album>)

class Async<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: String? = null,
)

fun Track.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("artist", artist).put("artistId", artistId)
    .put("album", album).put("albumId", albumId).put("imageUrl", imageUrl)
    .put("streamUrl", streamUrl).put("downloadUrl", downloadUrl).put("duration", duration)

fun trackFromJson(o: JSONObject) = Track(
    id = o.optString("id"),
    title = o.optString("title"),
    artist = o.optString("artist"),
    artistId = o.optString("artistId"),
    album = o.optString("album"),
    albumId = o.optString("albumId"),
    imageUrl = o.optString("imageUrl"),
    streamUrl = o.optString("streamUrl"),
    downloadUrl = o.optString("downloadUrl"),
    duration = o.optInt("duration"),
)

fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
