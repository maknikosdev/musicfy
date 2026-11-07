package com.musicfy.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicfy.app.AppVM
import com.musicfy.app.Async
import com.musicfy.app.Track
import com.musicfy.app.fmtTime

// ---------------------------------------------------------------- animation helpers

/** Clickable with a springy "press down" scale animation. */
fun Modifier.pressable(onClick: () -> Unit, scaleDown: Float = 0.96f): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Swipe left = next track, swipe right = previous track. */
fun Modifier.swipeToChange(vm: AppVM): Modifier = pointerInput(Unit) {
    var dx = 0f
    detectHorizontalDragGestures(
        onDragStart = { dx = 0f },
        onDragEnd = {
            if (dx < -90f) vm.player.next() else if (dx > 90f) vm.player.previous()
        },
        onHorizontalDrag = { change, amount ->
            change.consume()
            dx += amount
        }
    )
}

/** Slide transition used when the current track changes (direction-aware). */
fun slideSpec(vm: AppVM): AnimatedContentTransitionScope<Track>.() -> ContentTransform = {
    val d = vm.player.direction.value
    (slideInHorizontally(tween(320)) { w -> d * w } + fadeIn(tween(320))) togetherWith
        (slideOutHorizontally(tween(320)) { w -> -d * w } + fadeOut(tween(160)))
}

@Composable
fun PressIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
    content: @Composable () -> Unit,
) {
    Box(Modifier.pressable(onClick, 0.85f).then(modifier), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun MfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val look = if (filled) Modifier.background(Brush.linearGradient(listOf(Green, Blue)), shape)
    else Modifier.border(1.dp, Color(0x66FFFFFF), shape)
    val fg = if (filled) Color.Black else Color.White
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .pressable(onClick = { if (enabled) onClick() }, scaleDown = 0.93f)
            .then(look)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        if (text.isNotEmpty()) Text(text, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Green else ChipBg, tween(220), label = "pillBg")
    val fg by animateColorAsState(if (selected) Color.Black else Color.White, tween(220), label = "pillFg")
    Text(
        label, color = fg, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.pressable(onClick, 0.92f).clip(RoundedCornerShape(50)).background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun Skeleton(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "skeleton")
    val a by t.animateFloat(
        initialValue = 0.06f, targetValue = 0.16f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
    )
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = a)))
}

@Composable
fun SkeletonRows(n: Int = 5) {
    Column {
        repeat(n) {
            Row(Modifier.padding(16.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Skeleton(Modifier.size(52.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Skeleton(Modifier.height(14.dp).width(180.dp))
                    Spacer(Modifier.height(8.dp))
                    Skeleton(Modifier.height(10.dp).width(110.dp))
                }
            }
        }
    }
}

@Composable
fun EqBars(playing: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eq")
    Row(
        modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(450, 600, 760).forEach { d ->
            val h by t.animateFloat(
                initialValue = 0.25f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(d), RepeatMode.Reverse), label = "bar"
            )
            Box(
                Modifier.width(3.dp).fillMaxHeight(if (playing) h else 0.25f)
                    .background(Green, RoundedCornerShape(1.dp))
            )
        }
    }
}

// ---------------------------------------------------------------- images

@Composable
fun Art(vm: AppVM, t: Track, modifier: Modifier = Modifier) {
    val local = vm.store.cover(t)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(if (local.exists()) local else t.imageUrl.ifBlank { null })
            .crossfade(true).build(),
        contentDescription = "Cover of ${t.album}",
        contentScale = ContentScale.Crop,
        modifier = modifier.background(SurfaceDark)
    )
}

@Composable
fun Cover(url: String, modifier: Modifier = Modifier, desc: String = "") {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url.ifBlank { null }).crossfade(true).build(),
        contentDescription = desc,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(SurfaceDark)
    )
}

// ---------------------------------------------------------------- building blocks

@Composable
fun BackHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        PressIconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DownloadButton(vm: AppVM, t: Track) {
    val done by vm.store.downloaded.collectAsState()
    val prog by vm.store.progress.collectAsState()
    val state = when {
        done.any { it.id == t.id } -> 2
        prog.containsKey(t.id) -> 1
        else -> 0
    }
    Crossfade(targetState = state, label = "download") { s ->
        when (s) {
            2 -> PressIconButton(onClick = { vm.store.remove(t) }) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded - tap to remove", tint = Green)
            }
            1 -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { prog[t.id] ?: 0f }, color = Green,
                    modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp
                )
            }
            else -> PressIconButton(onClick = { vm.store.download(t) }) {
                Icon(Icons.Outlined.Download, contentDescription = "Download for offline")
            }
        }
    }
}

@Composable
fun TrackRow(vm: AppVM, t: Track, onPlay: () -> Unit) {
    val cur by vm.player.current.collectAsState()
    val playing by vm.player.isPlaying.collectAsState()
    val isCur = cur?.id == t.id
    val titleColor by animateColorAsState(if (isCur) Green else Color.White, tween(250), label = "title")
    Row(
        Modifier.fillMaxWidth().pressable(onPlay, 0.98f)
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Art(vm, t, Modifier.fillMaxSize())
            if (isCur) Box(
                Modifier.fillMaxSize().background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) { EqBars(playing) }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = titleColor, fontWeight = FontWeight.Medium)
            Text(
                "${t.artist} • ${fmtTime(t.duration * 1000L)}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DownloadButton(vm, t)
    }
}

@Composable
fun TrackCard(vm: AppVM, t: Track, onPlay: () -> Unit) {
    Column(Modifier.width(148.dp).pressable(onPlay, 0.95f)) {
        Art(vm, t, Modifier.size(148.dp).clip(RoundedCornerShape(14.dp)))
        Text(
            t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MfButton("Try again", onRetry, Modifier.padding(top = 12.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/** Vertical list section with skeleton loading and error state. */
fun LazyListScope.trackSection(
    vm: AppVM,
    title: String?,
    state: Async<List<Track>>,
    onRetry: () -> Unit,
) {
    if (title != null) item { SectionTitle(title) }
    when {
        state.loading -> item { SkeletonRows() }
        state.error != null -> item { ErrorBox(state.error, onRetry) }
        else -> {
            val list = state.data.orEmpty()
            if (list.isEmpty()) item { Empty("Nothing found") }
            itemsIndexed(list, key = { i, t -> "${title}_${t.id}_$i" }) { i, t ->
                TrackRow(vm, t) { vm.player.play(list, i) }
            }
        }
    }
}

/** Horizontal carousel of cover cards. */
fun LazyListScope.trackCarousel(
    vm: AppVM,
    title: String,
    state: Async<List<Track>>,
    onRetry: () -> Unit,
) {
    item { SectionTitle(title) }
    item {
        when {
            state.loading -> LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(4) { Skeleton(Modifier.size(148.dp)) }
            }
            state.error != null -> ErrorBox(state.error, onRetry)
            else -> {
                val list = state.data.orEmpty()
                if (list.isEmpty()) Empty("Nothing found") else LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list.size, key = { i -> "${list[i].id}_$i" }) { i ->
                        TrackCard(vm, list[i]) { vm.player.play(list, i) }
                    }
                }
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
