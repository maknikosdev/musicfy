package com.musicfy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.musicfy.app.Album
import com.musicfy.app.AppVM
import com.musicfy.app.Artist
import com.musicfy.app.Source
import kotlinx.coroutines.delay

private fun glow(color: Color = Color(0xFF0E3A38)) = Brush.verticalGradient(listOf(color, Bg), endY = 1000f)

@Composable
fun SourceChips(vm: AppVM) {
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Source.values().forEach { s -> Pill(s.label, vm.source == s) { vm.selectSource(s) } }
    }
}

@Composable
fun HomeScreen(vm: AppVM) {
    var keyInput by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().background(glow()).statusBarsPadding()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "MusicFy",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = Brush.linearGradient(listOf(Green, Blue)),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    "Your music, everywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { SourceChips(vm) }
        item {
            AnimatedVisibility(
                visible = vm.source == Source.JAMENDO && !vm.hasKey,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    Modifier.padding(16.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(SurfaceDark).padding(16.dp)
                ) {
                    Text("Connect the music catalogue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Create a free account at devportal.jamendo.com, create an application and " +
                            "paste its client_id here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = keyInput, onValueChange = { keyInput = it },
                        label = { Text("client_id") }, singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green, focusedLabelColor = Green, cursorColor = Green
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    MfButton(
                        "Save", onClick = { vm.saveKey(keyInput) },
                        modifier = Modifier.padding(top = 12.dp), enabled = keyInput.isNotBlank()
                    )
                }
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.genres) { g -> Pill(g, vm.genre == g) { vm.selectGenre(g) } }
            }
        }
        val suffix = vm.genre?.let { " • $it" } ?: ""
        trackCarousel(vm, "Popular this week$suffix", vm.popular) { vm.loadHome() }
        trackSection(vm, "${vm.freshTitle}$suffix", vm.fresh) { vm.loadHome() }
        item { Box(Modifier.size(16.dp)) }
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

    Column(Modifier.fillMaxSize().background(glow(Color(0xFF14143A))).statusBarsPadding()) {
        Text(
            "Search", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
        )
        OutlinedTextField(
            value = vm.query, onValueChange = { vm.query = it },
            placeholder = { Text("Songs, albums, artists") },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Green, cursorColor = Green,
                unfocusedContainerColor = SurfaceDark, focusedContainerColor = SurfaceDark,
                unfocusedBorderColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        SourceChips(vm)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Songs", "Albums", "Artists").forEachIndexed { i, label ->
                Pill(label, tab == i) { tab = i }
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
                    if (s.loading) item { SkeletonRows() }
                    else if (s.error != null) item { ErrorBox(s.error) { vm.search(vm.query) } }
                    else if (s.data.orEmpty().isEmpty()) item { Empty("No albums found") }
                    else items(s.data.orEmpty(), key = { it.id }) { a -> AlbumRow(a) { nav.navigate("album/${a.id}") } }
                }
                else -> {
                    val s = vm.sArtists
                    if (s.loading) item { SkeletonRows() }
                    else if (s.error != null) item { ErrorBox(s.error) { vm.search(vm.query) } }
                    else if (s.data.orEmpty().isEmpty()) item { Empty("No artists found") }
                    else items(s.data.orEmpty(), key = { it.id }) { a -> ArtistRow(a) { nav.navigate("artist/${a.id}") } }
                }
            }
        }
    }
}

@Composable
fun AlbumRow(a: Album, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, 0.98f).padding(16.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(a.imageUrl, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), a.name)
        Column(Modifier.padding(start = 12.dp)) {
            Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(
                "Album • ${a.artist}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ArtistRow(a: Artist, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, 0.98f).padding(16.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(a.imageUrl, Modifier.size(56.dp).clip(CircleShape), a.name)
        Column(Modifier.padding(start = 12.dp)) {
            Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(
                "Artist", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LibraryScreen(vm: AppVM) {
    val list by vm.store.downloaded.collectAsState()
    LazyColumn(Modifier.fillMaxSize().background(glow(Color(0xFF123A2A))).statusBarsPadding()) {
        item {
            Text(
                "Your library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
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
                    MfButton("Play", onClick = { vm.player.play(list, 0) }, icon = Icons.Default.PlayArrow)
                    MfButton("Shuffle", onClick = { vm.player.play(list, 0, shuffled = true) }, icon = Icons.Default.Shuffle, filled = false)
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
    Column(Modifier.fillMaxSize().background(glow(Color(0xFF0E3550))).statusBarsPadding()) {
        BackHeader(st.data?.album?.name ?: "Album") { nav.popBackStack() }
        when {
            st.loading || (st.data == null && st.error == null) ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Green) }
            st.error != null -> ErrorBox(st.error) { vm.openAlbum(id) }
            else -> {
                val d = st.data!!
                val allDone = d.tracks.isNotEmpty() && d.tracks.all { t -> done.any { it.id == t.id } }
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Cover(
                                d.album.imageUrl,
                                Modifier.size(220.dp).shadow(20.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)),
                                d.album.name
                            )
                            Text(
                                d.album.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Text(
                                d.album.artist, color = Green, fontWeight = FontWeight.Medium,
                                modifier = Modifier.pressable({ nav.navigate("artist/${d.album.artistId}") }, 0.94f).padding(6.dp)
                            )
                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MfButton("Play", onClick = { vm.player.play(d.tracks, 0) }, icon = Icons.Default.PlayArrow, enabled = d.tracks.isNotEmpty())
                                MfButton("", onClick = { vm.player.play(d.tracks, 0, shuffled = true) }, icon = Icons.Default.Shuffle, filled = false, enabled = d.tracks.isNotEmpty())
                            }
                            MfButton(
                                if (allDone) "Album downloaded" else "Download album",
                                onClick = { vm.store.downloadAll(d.tracks) },
                                modifier = Modifier.padding(top = 10.dp),
                                icon = if (allDone) Icons.Default.CheckCircle else Icons.Outlined.Download,
                                filled = false, enabled = !allDone && d.tracks.isNotEmpty()
                            )
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
    Column(Modifier.fillMaxSize().background(glow(Color(0xFF30205A))).statusBarsPadding()) {
        BackHeader(st.data?.artist?.name ?: "Artist") { nav.popBackStack() }
        when {
            st.loading || (st.data == null && st.error == null) ->
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Green) }
            st.error != null -> ErrorBox(st.error) { vm.openArtist(id) }
            else -> {
                val d = st.data!!
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Cover(
                                d.artist.imageUrl,
                                Modifier.size(170.dp).shadow(20.dp, CircleShape).clip(CircleShape),
                                d.artist.name
                            )
                            Text(
                                d.artist.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MfButton("Play", onClick = { vm.player.play(d.tracks, 0) }, icon = Icons.Default.PlayArrow, enabled = d.tracks.isNotEmpty())
                                MfButton("", onClick = { vm.player.play(d.tracks, 0, shuffled = true) }, icon = Icons.Default.Shuffle, filled = false, enabled = d.tracks.isNotEmpty())
                            }
                        }
                        SectionTitle("Popular")
                    }
                    items(d.tracks.size, key = { d.tracks[it].id }) { i ->
                        TrackRow(vm, d.tracks[i]) { vm.player.play(d.tracks, i) }
                    }
                    if (d.albums.isNotEmpty()) {
                        item { SectionTitle("Albums") }
                        items(d.albums, key = { "a_" + it.id }) { a -> AlbumRow(a) { nav.navigate("album/${a.id}") } }
                    }
                }
            }
        }
    }
}
