package com.musicfy.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.musicfy.app.AppVM
import com.musicfy.app.Track
import com.musicfy.app.fmtTime

@Composable
fun MiniPlayer(vm: AppVM, onOpen: () -> Unit) {
    val t by vm.player.current.collectAsState()
    val playing by vm.player.isPlaying.collectAsState()
    val pos by vm.player.position.collectAsState()
    val dur by vm.player.duration.collectAsState()
    val track: Track = t ?: return

    Column(
        Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            .pressable(onOpen, 0.98f)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .swipeToChange(vm)
    ) {
        Row(Modifier.padding(start = 10.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            // The artwork and title slide sideways whenever the track changes.
            AnimatedContent(
                targetState = track,
                contentKey = { it.id },
                transitionSpec = slideSpec(vm),
                modifier = Modifier.weight(1f),
                label = "miniTrack"
            ) { tr ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Art(vm, tr, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(tr.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text(
                            tr.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            PressIconButton(onClick = { vm.player.playPause() }) {
                Crossfade(targetState = playing, label = "miniPlay") { p ->
                    Icon(if (p) Icons.Default.Pause else Icons.Default.PlayArrow, if (p) "Pause" else "Play")
                }
            }
            PressIconButton(onClick = { vm.player.next() }) { Icon(Icons.Default.SkipNext, "Next") }
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

    // A soft colour wash that changes from track to track.
    val tints = remember {
        listOf(Color(0xFF0F4A3F), Color(0xFF0E3550), Color(0xFF30205A), Color(0xFF5A2045), Color(0xFF4A4410))
    }
    val tint by animateColorAsState(
        targetValue = t?.let { tints[(it.id.hashCode() and 0x7fffffff) % tints.size] } ?: tints[0],
        animationSpec = tween(700), label = "tint"
    )
    // The cover "breathes": full size while playing, slightly smaller when paused.
    val artScale by animateFloatAsState(
        targetValue = if (playing) 1f else 0.88f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "artScale"
    )

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tint, 0.6f to Bg, 1f to Bg))) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PressIconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "Close") }
                Text(
                    "Now playing", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            val track: Track = t ?: return@Column

            // Cover + title slide in the direction of the skip. Swipe the cover to change song.
            AnimatedContent(
                targetState = track,
                contentKey = { it.id },
                transitionSpec = slideSpec(vm),
                modifier = Modifier.fillMaxWidth().swipeToChange(vm),
                label = "nowTrack"
            ) { tr ->
                Column(Modifier.fillMaxWidth()) {
                    Art(
                        vm, tr,
                        Modifier.padding(top = 12.dp, bottom = 4.dp).fillMaxWidth().aspectRatio(1f)
                            .graphicsLayer { scaleX = artScale; scaleY = artScale }
                            .shadow(20.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                tr.artist, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        DownloadButton(vm, tr)
                    }
                }
            }

            Slider(
                value = if (dragging) dragValue else pos.toFloat(),
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = { vm.player.seekTo(dragValue.toLong()); dragging = false },
                valueRange = 0f..(if (dur > 0) dur.toFloat() else 1f),
                colors = SliderDefaults.colors(
                    thumbColor = Green, activeTrackColor = Green, inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtTime(if (dragging) dragValue.toLong() else pos), style = MaterialTheme.typography.bodySmall)
                Text(fmtTime(dur), style = MaterialTheme.typography.bodySmall)
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                PressIconButton(onClick = { vm.player.toggleShuffle() }) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) Green else Color.White)
                }
                PressIconButton(onClick = { vm.player.previous() }, Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(36.dp))
                }
                PressIconButton(
                    onClick = { vm.player.playPause() },
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Green, Blue)))
                ) {
                    Crossfade(targetState = playing, label = "bigPlay") { p ->
                        Icon(
                            if (p) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (p) "Pause" else "Play", Modifier.size(40.dp), tint = Color.Black
                        )
                    }
                }
                PressIconButton(onClick = { vm.player.next() }, Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", Modifier.size(36.dp))
                }
                PressIconButton(onClick = { vm.player.cycleRepeat() }) {
                    Icon(
                        if (repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat", tint = if (repeat != Player.REPEAT_MODE_OFF) Green else Color.White
                    )
                }
            }

            Text(
                "Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            queue.forEachIndexed { i, q ->
                val titleColor by animateColorAsState(if (q.id == track.id) Green else Color.White, tween(250), label = "q")
                Row(
                    Modifier.fillMaxWidth().pressable({ vm.player.jumpTo(i) }, 0.98f).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Art(vm, q, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(q.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = titleColor)
                        Text(
                            q.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
