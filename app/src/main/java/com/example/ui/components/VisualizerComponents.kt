package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Track
import com.example.data.prefs.MiniPlayerStyle
import com.example.data.prefs.VisualizerStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AlbumArtworkView(
    artworkUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = artworkUri,
                contentDescription = contentDescription,
                placeholder = painterResource(id = R.drawable.img_m3_album_art),
                error = painterResource(id = R.drawable.img_m3_album_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_m3_album_art),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun VinylDiscView(
    artworkUri: String?,
    isPlaying: Boolean,
    useTonalRing: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "vinyl_spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val tonalRingColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (useTonalRing) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.94f)
                    .clip(CircleShape)
                    .background(tonalRingColor)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize(0.84f)
                .rotate(if (isPlaying) angle else 0f)
                .clip(CircleShape)
                .background(Color(0xFF1A1B1F))
                .border(1.5.dp, outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                for (ring in 1..6) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = r * (0.48f + ring * 0.075f),
                        style = Stroke(width = 1.2f)
                    )
                }
            }
            AlbumArtworkView(
                artworkUri = artworkUri,
                contentDescription = "Carátula del disco",
                cornerRadius = 200.dp,
                modifier = Modifier
                    .fillMaxSize(0.52f)
                    .border(2.dp, primary.copy(alpha = 0.6f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(surface)
                    .border(2.dp, outlineVariant, CircleShape)
            )
        }
    }
}

@Composable
fun AudioSpectrumVisualizer(
    bands: List<Float>,
    style: VisualizerStyle,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    if (style == VisualizerStyle.OFF) return
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(modifier = modifier) {
        val count = bands.size.coerceAtLeast(1)
        when (style) {
            VisualizerStyle.TONAL_BARS -> {
                val gap = 6.dp.toPx()
                val totalGap = gap * (count - 1)
                val barW = ((size.width - totalGap) / count).coerceAtLeast(3f)
                for (i in 0 until count) {
                    val amp = if (isPlaying) bands[i].coerceIn(0.08f, 1f) else 0.08f
                    val barH = size.height * amp
                    val x = i * (barW + gap)
                    val y = size.height - barH
                    drawRoundRect(
                        color = trackColor.copy(alpha = 0.45f),
                        topLeft = Offset(x, 0f),
                        size = Size(barW, size.height),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                    drawRoundRect(
                        color = if (i % 2 == 0) primary else secondary,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }
            VisualizerStyle.WAVEFORM -> {
                val path = Path()
                val midY = size.height * 0.5f
                val stepX = size.width / (count - 1).coerceAtLeast(1)
                path.moveTo(0f, midY)
                for (i in 0 until count) {
                    val x = i * stepX
                    val amp = if (isPlaying) bands[i] else 0.06f
                    val sign = if (i % 2 == 0) -1f else 1f
                    val y = midY + sign * amp * (size.height * 0.42f)
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val cx = (prevX + x) / 2f
                        val prevAmp = if (isPlaying) bands[i - 1] else 0.06f
                        val prevSign = if ((i - 1) % 2 == 0) -1f else 1f
                        val prevY = midY + prevSign * prevAmp * (size.height * 0.42f)
                        path.cubicTo(cx, prevY, cx, y, x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = primary,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            VisualizerStyle.RADIAL -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.25f
                val maxExtend = size.minDimension * 0.22f
                drawCircle(
                    color = trackColor,
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                for (i in 0 until count) {
                    val angleRad = (i.toFloat() / count) * (Math.PI * 2.0)
                    val amp = if (isPlaying) bands[i].coerceIn(0.08f, 1f) else 0.08f
                    val start = Offset(
                        x = center.x + (baseRadius * cos(angleRad)).toFloat(),
                        y = center.y + (baseRadius * sin(angleRad)).toFloat()
                    )
                    val end = Offset(
                        x = center.x + ((baseRadius + maxExtend * amp) * cos(angleRad)).toFloat(),
                        y = center.y + ((baseRadius + maxExtend * amp) * sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = if (i % 2 == 0) primary else secondary,
                        start = start,
                        end = end,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            VisualizerStyle.OFF -> Unit
        }
    }
}

@Composable
fun FormatBadgeChip(
    badge: String,
    modifier: Modifier = Modifier
) {
    val isLossless = badge in setOf("FLAC", "WAV", "ALAC", "AIFF", "DSF")
    val container = if (isLossless) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (isLossless) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = container
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    style: MiniPlayerStyle = MiniPlayerStyle.FLOATING_CARD,
    showArtwork: Boolean = true,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveDuration = durationMs.takeIf { it > 0L } ?: track.durationMs
    val progress = if (effectiveDuration > 0L) {
        (positionMs.toFloat() / effectiveDuration).coerceIn(0f, 1f)
    } else {
        0f
    }

    val isFloating = style == MiniPlayerStyle.FLOATING_CARD
    val cardModifier = if (isFloating) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("mini_player_bar")
            .clickable(onClick = onOpenNowPlaying)
    } else {
        modifier
            .fillMaxWidth()
            .testTag("mini_player_bar")
            .clickable(onClick = onOpenNowPlaying)
    }

    ElevatedCard(
        modifier = cardModifier,
        shape = if (isFloating) MaterialTheme.shapes.large else RoundedCornerShape(0.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isFloating) 3.dp else 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = if (isFloating) 10.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showArtwork) {
                    AlbumArtworkView(
                        artworkUri = track.artworkUri,
                        contentDescription = track.title,
                        cornerRadius = 10.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${track.artist} · ${track.formatBadge}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente pista",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "%d:%02d".format(mins, secs)
}
