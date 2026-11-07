package com.musicfy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musicfy.app.AppVM
import com.musicfy.app.Async
import com.musicfy.app.Track
import com.musicfy.app.fmtTime

@Composable
fun Art(vm: AppVM, t: Track, modifier: Modifier = Modifier) {
    val local = vm.store.cover(t)
    AsyncImage(
        model = if (local.exists()) local else t.imageUrl.ifBlank { null },
        contentDescription = "Cover of ${t.album}",
        contentScale = ContentScale.Crop,
        modifier = modifier.background(SurfaceDark)
    )
}

@Composable
fun Cover(url: String, modifier: Modifier = Modifier, desc: String = "") {
    AsyncImage(
        model = url.ifBlank { null },
        contentDescription = desc,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(SurfaceDark)
    )
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun DownloadButton(vm: AppVM, t: Track) {
    val done by vm.store.downloaded.collectAsState()
    val prog by vm.store.progress.collectAsState()
    val p = prog[t.id]
    when {
        done.any { it.id == t.id } -> IconButton(onClick = { vm.store.remove(t) }) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded - tap to remove", tint = Green)
        }
        p != null -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { p }, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        }
        else -> IconButton(onClick = { vm.store.download(t) }) {
            Icon(Icons.Outlined.Download, contentDescription = "Download for offline")
        }
    }
}

@Composable
fun TrackRow(vm: AppVM, t: Track, onPlay: () -> Unit) {
    val cur by vm.player.current.collectAsState()
    val isCur = cur?.id == t.id
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Art(vm, t, Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                t.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (isCur) Green else MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${t.artist} • ${fmtTime(t.duration * 1000L)}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DownloadButton(vm, t)
    }
}

/** Loading / error / list section for any Async<List<Track>> inside a LazyColumn. */
fun LazyListScope.trackSection(
    vm: AppVM,
    title: String?,
    state: Async<List<Track>>,
    onRetry: () -> Unit,
) {
    if (title != null) item {
        Text(
            title, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
        )
    }
    when {
        state.loading -> item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> item {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Try again") }
            }
        }
        else -> {
            val list = state.data.orEmpty()
            itemsIndexed(list, key = { i, t -> "${title}_${t.id}_$i" }) { i, t ->
                TrackRow(vm, t) { vm.player.play(list, i) }
            }
        }
    }
}

@Composable
fun Empty(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

val RowSpacing = Arrangement.spacedBy(12.dp)
