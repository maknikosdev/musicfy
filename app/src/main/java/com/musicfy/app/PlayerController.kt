package com.musicfy.app

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** App-side remote for the PlaybackService. Unlimited skipping, shuffle, repeat, queue. */
class PlayerController(private val ctx: Context, private val store: DownloadStore) {
    val current = MutableStateFlow<Track?>(null)
    val isPlaying = MutableStateFlow(false)
    val position = MutableStateFlow(0L)
    val duration = MutableStateFlow(0L)
    val shuffle = MutableStateFlow(false)
    val repeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val queue = MutableStateFlow<List<Track>>(emptyList())
    val errors = MutableSharedFlow<String>(extraBufferCapacity = 8)

    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private val known = HashMap<String, Track>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)

        override fun onPlayerError(error: PlaybackException) {
            errors.tryEmit("This track can't be played (check your connection or download it)")
            controller?.let { c ->
                if (c.hasNextMediaItem()) {
                    c.seekToNextMediaItem()
                    c.prepare()
                    c.play()
                }
            }
        }
    }

    init {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(listener)
            sync(c)
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(ctx))

        scope.launch {
            while (true) {
                controller?.let {
                    position.value = it.currentPosition.coerceAtLeast(0)
                    duration.value = it.duration.coerceAtLeast(0)
                }
                delay(400)
            }
        }
    }

    private fun sync(p: Player) {
        current.value = p.currentMediaItem?.mediaId?.let { known[it] }
        isPlaying.value = p.isPlaying
        shuffle.value = p.shuffleModeEnabled
        repeat.value = p.repeatMode
        queue.value = (0 until p.mediaItemCount).mapNotNull { known[p.getMediaItemAt(it).mediaId] }
        duration.value = p.duration.coerceAtLeast(0)
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending += block
    }

    private fun itemFor(t: Track): MediaItem {
        known[t.id] = t
        val uri = if (store.isDownloaded(t.id)) Uri.fromFile(store.file(t)) else Uri.parse(t.streamUrl)
        val art = when {
            store.cover(t).exists() -> Uri.fromFile(store.cover(t))
            t.imageUrl.isNotBlank() -> Uri.parse(t.imageUrl)
            else -> null
        }
        return MediaItem.Builder()
            .setMediaId(t.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album)
                    .setArtworkUri(art).build()
            ).build()
    }

    fun play(list: List<Track>, index: Int, shuffled: Boolean = false) {
        if (list.isEmpty()) return
        val items = list.map { itemFor(it) }
        withController { c ->
            c.shuffleModeEnabled = shuffled
            c.setMediaItems(items, if (shuffled) 0 else index.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun playPause() = withController { if (it.isPlaying) it.pause() else it.play() }
    fun next() = withController { it.seekToNextMediaItem() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun jumpTo(index: Int) = withController { it.seekToDefaultPosition(index); it.play() }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
