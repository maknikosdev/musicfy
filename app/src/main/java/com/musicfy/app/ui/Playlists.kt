package com.musicfy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.musicfy.app.AppVM
import com.musicfy.app.Playlist
import com.musicfy.app.Track

/** The "add to playlist" button shown next to every track. */
@Composable
fun AddButton(vm: AppVM, t: Track) {
    PressIconButton(onClick = { vm.addTarget = t }) {
        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
    }
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Playlist name") }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Green, focusedLabelColor = Green, cursorColor = Green
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(confirmLabel, color = if (text.isNotBlank()) Green else Color.Gray)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddToPlaylistDialog(vm: AppVM, track: Track) {
    val pls by vm.playlists.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog(
            "New playlist", "", "Create & add",
            onDismiss = { creating = false }
        ) { name ->
            vm.createAndAdd(name, track)
            creating = false
            vm.addTarget = null
        }
    } else {
        AlertDialog(
            onDismissRequest = { vm.addTarget = null },
            containerColor = SurfaceDark,
            title = { Text("Add to playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Row(
                        Modifier.fillMaxWidth().pressable({ creating = true }, 0.98f).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = Green)
                        Text("New playlist", color = Green, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
                    }
                    pls.forEach { p ->
                        Column(
                            Modifier.fillMaxWidth()
                                .pressable({ vm.addToPlaylist(p.id, track); vm.addTarget = null }, 0.98f)
                                .padding(vertical = 10.dp)
                        ) {
                            Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text(
                                "${p.tracks.size} tracks", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { vm.addTarget = null }) { Text("Close") } }
        )
    }
}

@Composable
fun PlaylistRow(p: Playlist, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressable(onClick, 0.98f).padding(16.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(p.tracks.firstOrNull()?.imageUrl.orEmpty(), Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), p.name)
        Column(Modifier.padding(start = 12.dp)) {
            Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(
                "Playlist • ${p.tracks.size} tracks", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaylistScreen(vm: AppVM, nav: NavHostController, id: String) {
    val pls by vm.playlists.playlists.collectAsState()
    val p = pls.firstOrNull { it.id == id }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // If the playlist was deleted, leave this screen.
    LaunchedEffect(p == null) { if (p == null) nav.popBackStack() }
    if (p == null) return

    if (renaming) NameDialog("Rename playlist", p.name, "Save", onDismiss = { renaming = false }) { name ->
        vm.playlists.rename(p.id, name)
        renaming = false
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = SurfaceDark,
        title = { Text("Delete playlist?", fontWeight = FontWeight.Bold) },
        text = { Text("\"${p.name}\" will be removed. The songs themselves are not deleted.") },
        confirmButton = {
            TextButton(onClick = { confirmDelete = false; vm.playlists.delete(p.id) }) {
                Text("Delete", color = Color(0xFFFF6B6B))
            }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF123A2A), Bg), endY = 1000f))
            .statusBarsPadding()
    ) {
        BackHeader(p.name) { nav.popBackStack() }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        p.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${p.tracks.size} tracks", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MfButton("Play", onClick = { vm.player.play(p.tracks, 0) }, icon = Icons.Default.PlayArrow, enabled = p.tracks.isNotEmpty())
                        MfButton("", onClick = { vm.player.play(p.tracks, 0, shuffled = true) }, icon = Icons.Default.Shuffle, filled = false, enabled = p.tracks.isNotEmpty())
                        PressIconButton(onClick = { renaming = true }) { Icon(Icons.Default.Edit, "Rename playlist") }
                        PressIconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "Delete playlist") }
                    }
                }
            }
            if (p.tracks.isEmpty()) item {
                Empty("This playlist is empty. Tap the playlist button next to any song to add it here.")
            }
            items(p.tracks.size, key = { p.tracks[it].id }) { i ->
                TrackRow(vm, p.tracks[i], onRemove = { vm.playlists.removeTrack(p.id, p.tracks[i].id) }) {
                    vm.player.play(p.tracks, i)
                }
            }
        }
    }
}
