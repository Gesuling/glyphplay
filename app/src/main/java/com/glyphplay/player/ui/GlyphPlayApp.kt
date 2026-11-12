package com.glyphplay.player.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.glyphplay.player.data.Track
import com.glyphplay.player.ui.components.GlyphEq
import com.glyphplay.player.ui.components.GlyphProgress
import com.glyphplay.player.ui.components.GlyphText
import com.glyphplay.player.ui.components.formatGlyphTime
import com.glyphplay.player.ui.theme.GlyphBlack
import com.glyphplay.player.ui.theme.GlyphLine
import com.glyphplay.player.ui.theme.GlyphMute
import com.glyphplay.player.ui.theme.GlyphRed
import com.glyphplay.player.ui.theme.GlyphWhite

@Composable
fun GlyphPlayApp(
    state: PlayerUiState,
    onPlay: (Track) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSleep: (Int) -> Unit,
    onTab: (Tab) -> Unit,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(count = state.tracks.size, playing = state.playing)
        Box(modifier = Modifier.weight(1f).clipToBounds()) {
            when {
                state.permissionNeeded -> PermissionPane(onRequestPermission)
                state.tab == Tab.Library -> LibraryPane(state, onPlay)
                else -> NowPlayingPane(
                    state = state,
                    onToggle = onToggle,
                    onNext = onNext,
                    onPrev = onPrev,
                    onSeek = onSeek,
                    onShuffle = onShuffle,
                    onRepeat = onRepeat,
                    onSleep = onSleep,
                )
            }
        }
        BottomBar(
            state = state,
            onTab = onTab,
            onToggle = onToggle)
    }
}

@Composable
private fun TopBar(count: Int, playing: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "GLYPHPLAY",
                color = GlyphWhite,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
            )
            Text(
                text = if (playing) "LIVE" else "IDLE",
                color = if (playing) GlyphRed else GlyphMute,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        GlyphEq(active = playing, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$count TRACKS  ·  LOCAL STORAGE",
            color = GlyphMute,
            fontSize = 10.sp,
            letterSpacing = 1.6.sp,
        )
    }
}

@Composable
private fun PermissionPane(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("AUDIO ACCESS", color = GlyphWhite, letterSpacing = 3.sp, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "glyphplay reads music already on this phone. nothing leaves the device.",
            color = GlyphMute,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .border(1.dp, GlyphWhite)
                .clickable(onClick = onRequest)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Text("ALLOW", color = GlyphWhite, letterSpacing = 3.sp, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LibraryPane(state: PlayerUiState, onPlay: (Track) -> Unit) {
    if (!state.loading && state.tracks.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Text("NO FILES", color = GlyphWhite, letterSpacing = 3.sp, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "copy mp3 / flac / m4a into the phone music folder, then open glyphplay again.",
                color = GlyphMute,
                fontSize = 15.sp,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
            val selected = state.current?.id == track.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(track) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "%02d".format((index + 1) % 100),
                    color = if (selected) GlyphRed else GlyphMute,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.width(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = GlyphWhite,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist.uppercase(),
                        color = GlyphMute,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                GlyphText(
                    text = formatGlyphTime(track.durationMs).trim(),
                    cell = 2.dp,
                    gap = 0.7.dp,
                    color = if (selected) GlyphWhite else GlyphMute,
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlyphLine))
        }
    }
}

@Composable
private fun NowPlayingPane(
    state: PlayerUiState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSleep: (Int) -> Unit,
) {
    val track = state.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(280.dp)
                .border(1.dp, GlyphWhite, RoundedCornerShape(4.dp))
                .background(GlyphBlack),
            contentAlignment = Alignment.Center,
        ) {
            if (track != null) {
                AsyncImage(
                    model = track.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text("NO SIGNAL", color = GlyphMute, letterSpacing = 3.sp, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = track?.title ?: "—",
            color = GlyphWhite,
            fontSize = 26.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = listOfNotNull(track?.artist, track?.album).joinToString("  ·  ").ifBlank { "select a track" },
            color = GlyphMute,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(28.dp))
        val dur = state.durationMs.takeIf { it > 0 } ?: (track?.durationMs ?: 0)
        val progress = if (dur > 0) state.positionMs.toFloat() / dur else 0f
        GlyphProgress(progress = progress, onSeek = onSeek)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GlyphText(text = formatGlyphTime(state.positionMs), cell = 2.4.dp, gap = 0.8.dp)
            GlyphText(text = formatGlyphTime(dur), cell = 2.4.dp, gap = 0.8.dp)
        }
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = "previous",
                tint = GlyphWhite,
                modifier = Modifier.size(36.dp).clickable(onClick = onPrev),
            )
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(1.dp, GlyphWhite, CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "play",
                    tint = GlyphWhite,
                    modifier = Modifier.size(36.dp),
                )
            }
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = "next",
                tint = GlyphWhite,
                modifier = Modifier.size(36.dp).clickable(onClick = onNext),
            )
        }
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeChip("SHUFFLE", state.shuffle, Icons.Filled.Shuffle, onShuffle)

            val repeatOn = state.repeat != Player.REPEAT_MODE_OFF
            val repeatIcon =
                if (state.repeat == Player.REPEAT_MODE_ONE) {
                    Icons.Filled.RepeatOne
                } else {
                    Icons.Filled.Repeat
                }

            val repeatLabel = when (state.repeat) {
                Player.REPEAT_MODE_ONE -> "REPEAT 1"
                Player.REPEAT_MODE_ALL -> "REPEAT"
                else -> "REPEAT"
            }

            ModeChip(repeatLabel, repeatOn, repeatIcon, onRepeat)

            ModeChip(
                label = if (state.sleepMinutes > 0) {
                    "SLEEP ${state.sleepMinutes}M"
                } else {
                    "SLEEP"
                },
                on = state.sleepMinutes > 0,
                icon = Icons.Filled.Bedtime,
                onClick = {
                    when (state.sleepMinutes) {
                        0 -> onSleep(15)
                        15 -> onSleep(30)
                        30 -> onSleep(60)
                        else -> onSleep(0)
                    }
                },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RowScope.ModeChip(
    label: String,
    on: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val borderColor = if (on) GlyphWhite else GlyphLine
    Row(
        modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val inset = stroke / 2f
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = stroke),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (on) GlyphWhite else GlyphMute,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (on) GlyphWhite else GlyphMute,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BottomBar(state: PlayerUiState, onTab: (Tab) -> Unit, onToggle: () -> Unit) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlyphLine))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabLabel("LIBRARY", state.tab == Tab.Library) { onTab(Tab.Library) }
            TabLabel("NOW", state.tab == Tab.Now) { onTab(Tab.Now) }
            Spacer(Modifier.weight(1f))
            if (state.current != null) {
                Column(modifier = Modifier.weight(1.4f), horizontalAlignment = Alignment.End) {
                    Text(
                        state.current.title,
                        color = GlyphWhite,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (state.playing) "PLAYING" else "PAUSED",
                        color = GlyphRed,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, GlyphWhite, CircleShape)
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = GlyphWhite,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (selected) GlyphWhite else GlyphMute,
        fontSize = 11.sp,
        letterSpacing = 2.4.sp,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp).clickable(onClick = onClick),
    )
}
