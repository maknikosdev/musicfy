package com.musicfy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.musicfy.app.AppVM
import com.musicfy.app.fmtTime

@Composable
fun MiniPlayer(vm: AppVM, onOpen: () -> Unit) {
    val t by vm.player.current.collectAsState()
    val playing by vm.player.isPlaying.collectAsState()
    val pos by vm.player.position.collectAsState()
    val dur by vm.player.duration.collectAsState()
    val track = t ?: return
    Column(Modifier.fillMaxWidth().background(SurfaceDark).clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(vm, track, Modifier.size(44.dp).clip(RoundedCornerShape(4.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.player.playPause() }) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pause" else "Play")
            }
            IconButton(onClick = { vm.player.next() }) { Icon(Icons.Default.SkipNext, "Next") }
        }
        LinearProgressIndicator(
            progress = { if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth(), color = Green, trackColor = Color.Transparent
        )
    }
}

@Composable
fun NowPlaying(vm: AppVM, onClose: () -> Unit) {
    val t by vm.player.current.collectAsState()
    val playing by vm.player.isPlaying.collectAsState()
    val pos by vm.player.position.collectAsState()
    val dur by vm.player.duration.collectAsState()
    val shuffle by vm.player.shuffle.collectAsState()
    val repeat by vm.player.repeat.collectAsState()
    val queue by vm.player.queue.collectAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val track = t

    Column(
        Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "Close") }
            Text("Now playing", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        }
        if (track == null) return@Column

        Art(vm, track, Modifier.padding(top = 12.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DownloadButton(vm, track)
        }

        Slider(
            value = if (dragging) dragValue else pos.toFloat(),
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = { vm.player.seekTo(dragValue.toLong()); dragging = false },
            valueRange = 0f..(if (dur > 0) dur.toFloat() else 1f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmtTime(if (dragging) dragValue.toLong() else pos), style = MaterialTheme.typography.bodySmall)
            Text(fmtTime(dur), style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.player.toggleShuffle() }) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) Green else Color.White)
            }
            IconButton(onClick = { vm.player.previous() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp))
            }
            IconButton(
                onClick = { vm.player.playPause() },
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Green)
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (playing) "Pause" else "Play", Modifier.size(40.dp), tint = Color.Black
                )
            }
            IconButton(onClick = { vm.player.next() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp))
            }
            IconButton(onClick = { vm.player.cycleRepeat() }) {
                Icon(
                    if (repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat", tint = if (repeat != Player.REPEAT_MODE_OFF) Green else Color.White
                )
            }
        }

        Text("Queue", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
        queue.forEachIndexed { i, q ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.player.jumpTo(i) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Art(vm, q, Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(q.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (q.id == track.id) Green else Color.White)
                    Text(q.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
