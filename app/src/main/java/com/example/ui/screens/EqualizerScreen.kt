package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EqPreset
import com.example.player.AudioFxController
import com.example.player.EqUiState
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    eqState: EqUiState,
    customPresets: List<EqPreset>,
    playbackSpeed: Float,
    playbackPitch: Float,
    skipSilence: Boolean,
    monoAudio: Boolean,
    softLimiter: Boolean,
    onToggleEq: (Boolean) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onBassChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onPreampChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onSpeedPitchChange: (Float, Float) -> Unit,
    onSkipSilenceChange: (Boolean) -> Unit,
    onMonoAudioChange: (Boolean) -> Unit,
    onSoftLimiterChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Estudio de Ecualización", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = when {
                            !eqState.enabled -> "Procesamiento DSP desactivado"
                            eqState.hardwareAttached -> "DSP hardware activo · ${eqState.activePresetName}"
                            else -> "DSP software activo · ${eqState.activePresetName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("save_eq_preset_button")
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Guardar perfil EQ")
                    }
                    Switch(
                        checked = eqState.enabled,
                        onCheckedChange = onToggleEq,
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }
        }

        // Graphic Frequency Response Curve Card
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Curva de Frecuencia", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (eqState.enabled) "ACTIVO" else "BYPASS",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (eqState.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    EqCurveCanvas(
                        bandLevelsMb = eqState.bandLevelsMb,
                        minMb = eqState.minLevelMb,
                        maxMb = eqState.maxLevelMb,
                        enabled = eqState.enabled,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        eqState.bandLevelsMb.forEachIndexed { idx, levelMb ->
                            val freqHz = eqState.centerFreqsHz.getOrElse(idx) { 1000 }
                            val freqLabel = if (freqHz >= 1000) {
                                "${(freqHz / 1000.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) }} kHz"
                            } else {
                                "$freqHz Hz"
                            }
                            val dbValue = levelMb / 100f
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(freqLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
                                Slider(
                                    value = levelMb.toFloat(),
                                    onValueChange = { onBandLevelChange(idx, it.roundToInt()) },
                                    valueRange = eqState.minLevelMb.toFloat()..eqState.maxLevelMb.toFloat(),
                                    enabled = eqState.enabled,
                                    modifier = Modifier.weight(1f).testTag("eq_band_slider_$idx")
                                )
                                Text(
                                    text = "%+.1f dB".format(dbValue),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (eqState.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(68.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Presets Row (Built-in + Custom)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Perfiles de Ecualización", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customPresets, key = { "custom_${it.name}" }) { preset ->
                        FilterChip(
                            selected = eqState.activePresetName == preset.name,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Eliminar perfil",
                                    modifier = Modifier.size(18.dp).clickable { onDeleteCustomPreset(preset.name) }
                                )
                            }
                        )
                    }
                    items(AudioFxController.builtInPresets, key = { it.name }) { preset ->
                        FilterChip(
                            selected = eqState.activePresetName == preset.name,
                            onClick = { onApplyPreset(preset) },
                            label = { Text(preset.name) },
                            modifier = Modifier.testTag("eq_preset_${preset.name}")
                        )
                    }
                }
            }
        }

        // Spatial & Bass FX Card
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Procesadores de Sonido", style = MaterialTheme.typography.titleMedium)
                    }

                    FxSliderRow(
                        label = "Refuerzo de Graves (Bass Boost)",
                        value = eqState.bassStrength.toFloat(),
                        range = 0f..1000f,
                        valueText = "${(eqState.bassStrength / 10)}%",
                        enabled = eqState.enabled,
                        onValueChange = { onBassChange(it.roundToInt()) },
                        testTag = "bass_boost_slider"
                    )

                    FxSliderRow(
                        label = "Espacializador 3D (Virtualizer)",
                        value = eqState.virtualizerStrength.toFloat(),
                        range = 0f..1000f,
                        valueText = "${(eqState.virtualizerStrength / 10)}%",
                        enabled = eqState.enabled,
                        onValueChange = { onVirtualizerChange(it.roundToInt()) },
                        testTag = "virtualizer_slider"
                    )

                    FxSliderRow(
                        label = "Realce Dinámico (Loudness)",
                        value = eqState.loudnessGainMb.toFloat(),
                        range = 0f..1500f,
                        valueText = "+%.1f dB".format(eqState.loudnessGainMb / 100f),
                        enabled = eqState.enabled,
                        onValueChange = { onLoudnessChange(it.roundToInt()) },
                        testTag = "loudness_slider"
                    )

                    FxSliderRow(
                        label = "Ganancia Preamp",
                        value = eqState.preampDb,
                        range = -12f..12f,
                        valueText = "%+.1f dB".format(eqState.preampDb),
                        enabled = eqState.enabled,
                        onValueChange = onPreampChange,
                        testTag = "preamp_slider"
                    )

                    FxSliderRow(
                        label = "Balance Estéreo (Izq / Der)",
                        value = eqState.stereoBalance,
                        range = -1f..1f,
                        valueText = when {
                            eqState.stereoBalance < -0.05f -> "L ${(-eqState.stereoBalance * 100).roundToInt()}%"
                            eqState.stereoBalance > 0.05f -> "R ${(eqState.stereoBalance * 100).roundToInt()}%"
                            else -> "Centro"
                        },
                        enabled = true,
                        onValueChange = onBalanceChange,
                        testTag = "balance_slider"
                    )
                }
            }
        }

        // DSP Lab Toggles Card
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Laboratorio DSP Avanzado", style = MaterialTheme.typography.titleMedium)
                    }
                    EqSwitchRow("Omitir silencios", "Salta pausas largas automáticamente", skipSilence, onSkipSilenceChange)
                    EqSwitchRow("Audio Mono", "Mezcla L+R en un canal para un solo auricular", monoAudio, onMonoAudioChange)
                    EqSwitchRow("Limitador Anti-Clipping", "Suaviza picos al subir Preamp o Graves", softLimiter, onSoftLimiterChange)
                }
            }
        }

        // Playback Speed & Pitch Card
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("Velocidad y Tono", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = { onSpeedPitchChange(1.0f, 1.0f) }) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("1.0x")
                        }
                    }

                    FxSliderRow(
                        label = "Velocidad de reproducción",
                        value = playbackSpeed,
                        range = 0.5f..2.0f,
                        valueText = "%.2fx".format(playbackSpeed),
                        enabled = true,
                        onValueChange = { onSpeedPitchChange(it, playbackPitch) },
                        testTag = "speed_slider"
                    )

                    FxSliderRow(
                        label = "Afinación / Tono (Pitch)",
                        value = playbackPitch,
                        range = 0.5f..2.0f,
                        valueText = "%.2fx".format(playbackPitch),
                        enabled = true,
                        onValueChange = { onSpeedPitchChange(playbackSpeed, it) },
                        testTag = "pitch_slider"
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar perfil de ecualización") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del perfil (ej. Acústico Suave)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { onSaveCustomPreset(name); showSaveDialog = false },
                    enabled = name.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EqSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EqCurveCanvas(
    bandLevelsMb: List<Int>,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val primary = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val nodeCenterColor = MaterialTheme.colorScheme.surfaceContainerLow

    Canvas(modifier = modifier.clip(MaterialTheme.shapes.medium).background(containerColor)) {
        val w = size.width
        val h = size.height
        val padX = 20.dp.toPx()
        val padY = 18.dp.toPx()
        val usableW = (w - padX * 2).coerceAtLeast(1f)
        val usableH = (h - padY * 2).coerceAtLeast(1f)

        for (line in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val y = padY + usableH * line
            drawLine(
                color = if (line == 0.5f) primary.copy(alpha = 0.4f) else gridColor,
                start = Offset(padX, y),
                end = Offset(w - padX, y),
                strokeWidth = if (line == 0.5f) 1.5f else 1f
            )
        }

        if (bandLevelsMb.isEmpty()) return@Canvas
        val span = (maxMb - minMb).coerceAtLeast(1).toFloat()
        val points = bandLevelsMb.mapIndexed { idx, level ->
            val x = if (bandLevelsMb.size == 1) w / 2f else padX + (idx.toFloat() / (bandLevelsMb.size - 1)) * usableW
            val norm = ((level - minMb) / span).coerceIn(0f, 1f)
            val y = padY + (1f - norm) * usableH
            Offset(x, y)
        }

        val curvePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val cx = (prev.x + curr.x) / 2f
                cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            }
        }

        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(path = fillPath, color = primary.copy(alpha = 0.14f))
        drawPath(path = curvePath, color = primary, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        points.forEach { pt ->
            drawCircle(color = primary, radius = 5.5.dp.toPx(), center = pt)
            drawCircle(color = nodeCenterColor, radius = 2.5.dp.toPx(), center = pt)
        }
    }
}

@Composable
private fun FxSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, enabled = enabled, modifier = Modifier.testTag(testTag))
    }
}
