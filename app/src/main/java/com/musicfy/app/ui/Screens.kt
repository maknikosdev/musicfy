package com.musicfy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.musicfy.app.AppVM
import com.musicfy.app.Album
import com.musicfy.app.Artist
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: AppVM) {
    var keyInput by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding()) {
        item {
            Text(
                "MusicFy",
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = Brush.linearGradient(listOf(Green, Blue))
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
        if (!vm.hasKey) item {
            Column(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(16.dp)
            ) {
                Text("Connect the music catalogue", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Create a free account at devportal.jamendo.com, create an application and " +
                        "paste its client_id here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = keyInput, onValueChange = { keyInput = it },
                    label = { Text("client_id") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { vm.saveKey(keyInput) },
                    enabled = keyInput.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Save") }
            }
        }
        item {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.genres) { g ->
                    FilterChip(
                        selected = vm.genre == g,
                        onClick = { vm.selectGenre(g) },
                        label = { Text(g) }
                    )
                }
            }
        }
        val suffix = vm.genre?.let { " • $it" } ?: ""
        trackSection(vm, "Popular this week$suffix", vm.popular) { vm.loadHome() }
        trackSection(vm, "New releases$suffix", vm.fresh) { vm.loadHome() }
    }
}

@Composable
fun SearchScreen(vm: AppVM, nav: NavHostController) {
    var tab by remember { mutableStateOf(0) }
    LaunchedEffect(vm.query) {
        delay(400)
        vm.search(vm.query)
    }
    // Back clears the search text first (like most music apps), then navigates back.
    BackHandler(enabled = vm.query.isNotEmpty()) { vm.query = "" }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        OutlinedTextField(
            value = vm.query, onValueChange = { vm.query = it },
            placeholder = { Text("Songs, albums, artists") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Songs", "Albums", "Artists").forEachIndexed { i, label ->
                FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(label) })
            }
        }
        if (vm.query.isBlank()) {
            Empty("Search anything in the catalogue.")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> trackSection(vm, null, vm.sTracks) { vm.search(vm.query) }
                1 -> {
                    val s = vm.sAlbums
                    if (s.loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() } }
                    else if (s.error != null) item { Empty(s.error) }
                    else if (s.data.orEmpty().isEmpty()) item { Empty("No albums found") }
                    else items(s.data.orEmpty(), key = { it.id }) { a -> AlbumRow(a) { nav.navigate("album/${a.id}") } }
                }
                else -> {
                    val s = vm.sArtists
                    if (s.loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() } }
                    else if (s.error != null) item { Empty(s.error) }
                    else if (s.data.orEmpty().isEmpty()) item { Empty("No artists found") }
                    else items(s.data.orEmpty(), key = { it.id }) { a -> ArtistRow(a) { nav.navigate("artist/${a.id}") } }
                }
            }
        }
    }
}

@Composable
fun AlbumRow(a: Album, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(a.imageUrl, Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)), a.name)
        Column(Modifier.padding(start = 12.dp)) {
            Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Album • ${a.artist}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ArtistRow(a: Artist, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Cover(a.imageUrl, Modifier.size(56.dp).clip(CircleShape), a.name)
        Column(Modifier.padding(start = 12.dp)) {
            Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Artist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LibraryScreen(vm: AppVM) {
    val list by vm.store.downloaded.collectAsState()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding()) {
        item {
            Text("Your library", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
            Text(
                "Downloaded tracks (${list.size}) - play without internet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (list.isEmpty()) item {
            Empty("Nothing downloaded yet. Tap the download arrow next to a track or album.")
        } else {
            item {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.player.play(list, 0) }) {
                        Icon(Icons.Default.PlayArrow, null); Text("Play")
                    }
                    OutlinedButton(onClick = { vm.player.play(list, 0, shuffled = true) }) {
                        Icon(Icons.Default.Shuffle, null); Text("Shuffle")
                    }
                }
            }
            items(list.size, key = { list[it].id }) { i ->
                TrackRow(vm, list[i]) { vm.player.play(list, i) }
            }
        }
    }
}

@Composable
fun AlbumScreen(vm: AppVM, nav: NavHostController, id: String) {
    LaunchedEffect(id) { vm.openAlbum(id) }
    val st = vm.album
    val done by vm.store.downloaded.collectAsState()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackHeader(st.data?.album?.name ?: "Album") { nav.popBackStack() }
        when {
            st.loading || (st.data == null && st.error == null) ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            st.error != null -> Empty(st.error)
            else -> {
                val d = st.data!!
                val allDone = d.tracks.isNotEmpty() && d.tracks.all { t -> done.any { it.id == t.id } }
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Cover(d.album.imageUrl, Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)), d.album.name)
                            Text(d.album.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                            Text(
                                d.album.artist, color = Green,
                                modifier = Modifier.clickable { nav.navigate("artist/${d.album.artistId}") }.padding(4.dp)
                            )
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.player.play(d.tracks, 0) }) {
                                    Icon(Icons.Default.PlayArrow, null); Text("Play")
                                }
                                OutlinedButton(onClick = { vm.player.play(d.tracks, 0, shuffled = true) }) {
                                    Icon(Icons.Default.Shuffle, null)
                                }
                            }
                            OutlinedButton(
                                onClick = { vm.store.downloadAll(d.tracks) },
                                enabled = !allDone,
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text(if (allDone) "Album downloaded" else "Download album for offline") }
                        }
                    }
                    items(d.tracks.size, key = { d.tracks[it].id }) { i ->
                        TrackRow(vm, d.tracks[i]) { vm.player.play(d.tracks, i) }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistScreen(vm: AppVM, nav: NavHostController, id: String) {
    LaunchedEffect(id) { vm.openArtist(id) }
    val st = vm.artist
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        BackHeader(st.data?.artist?.name ?: "Artist") { nav.popBackStack() }
        when {
            st.loading || (st.data == null && st.error == null) ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            st.error != null -> Empty(st.error)
            else -> {
                val d = st.data!!
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Cover(d.artist.imageUrl, Modifier.size(160.dp).clip(CircleShape), d.artist.name)
                            Text(d.artist.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.player.play(d.tracks, 0) }, enabled = d.tracks.isNotEmpty()) {
                                    Icon(Icons.Default.PlayArrow, null); Text("Play")
                                }
                                OutlinedButton(onClick = { vm.player.play(d.tracks, 0, shuffled = true) }, enabled = d.tracks.isNotEmpty()) {
                                    Icon(Icons.Default.Shuffle, null)
                                }
                            }
                        }
                        Text("Popular", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
                    }
                    items(d.tracks.size, key = { d.tracks[it].id }) { i ->
                        TrackRow(vm, d.tracks[i]) { vm.player.play(d.tracks, i) }
                    }
                    if (d.albums.isNotEmpty()) {
                        item {
                            Text("Albums", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp))
                        }
                        items(d.albums, key = { "a_" + it.id }) { a -> AlbumRow(a) { nav.navigate("album/${a.id}") } }
                    }
                }
            }
        }
    }
}
