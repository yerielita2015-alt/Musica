package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LibrarySortOrder
import com.example.data.prefs.AccentTheme
import com.example.data.prefs.AppFontChoice
import com.example.data.prefs.ArtworkShapeStyle
import com.example.data.prefs.CornerStyle
import com.example.data.prefs.LibraryLayoutMode
import com.example.data.prefs.LyricsAlignOption
import com.example.data.prefs.LyricsSizeOption
import com.example.data.prefs.MiniPlayerStyle
import com.example.data.prefs.NavLabelMode
import com.example.data.prefs.NowPlayingStyle
import com.example.data.prefs.ThemeMode
import com.example.data.prefs.UserCustomization
import com.example.data.prefs.VisualizerStyle

@Composable
fun PersonalizeScreen(
    customization: UserCustomization,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAccentThemeChange: (AccentTheme) -> Unit,
    onFontChoiceChange: (AppFontChoice) -> Unit,
    onNowPlayingStyleChange: (NowPlayingStyle) -> Unit,
    onVisualizerStyleChange: (VisualizerStyle) -> Unit,
    onCornerStyleChange: (CornerStyle) -> Unit,
    onArtworkShapeChange: (ArtworkShapeStyle) -> Unit,
    onNavLabelModeChange: (NavLabelMode) -> Unit,
    onMiniPlayerStyleChange: (MiniPlayerStyle) -> Unit,
    onLibraryLayoutChange: (LibraryLayoutMode) -> Unit,
    onSortOrderChange: (LibrarySortOrder) -> Unit,
    onLyricsSizeChange: (LyricsSizeOption) -> Unit,
    onLyricsAlignChange: (LyricsAlignOption) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onCompactDensityChange: (Boolean) -> Unit,
    onShowFormatBadgesChange: (Boolean) -> Unit,
    onUseTonalBackdropChange: (Boolean) -> Unit,
    onShowQuickSeekChange: (Boolean) -> Unit,
    onSeekStepChange: (Int) -> Unit,
    onKeepScreenOnLyricsChange: (Boolean) -> Unit,
    onShowMiniPlayerArtChange: (Boolean) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onOpenCloudSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Personalización Total", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Configura temas Material 3, tipografía, geometría, letras, reproductor y audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Cloud Sync Entry Card
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().testTag("open_cloud_sync_card").clickable(onClick = onOpenCloudSync)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sincronización en la Nube y Respaldos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Exporta e importa biblioteca, listas, marcadores, letras y perfiles EQ.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                    }
                }
            }
        }

        // Theme Mode
        item {
            SectionCard(title = "Modo de Tema", icon = Icons.Rounded.DarkMode) {
                ChipRow(ThemeMode.entries, customization.themeMode, { it.label }, onThemeModeChange, { "theme_mode_${it.name.lowercase()}" })
            }
        }

        // Accent Palette Swatches (11 M3 palettes)
        item {
            SectionCard(title = "Paleta Tonal Material 3 (11 temas)", icon = Icons.Rounded.ColorLens) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(AccentTheme.entries) { accent ->
                        val selected = customization.accentTheme == accent
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { onAccentThemeChange(accent) }.padding(4.dp).testTag("accent_${accent.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.previewHex))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Seleccionado", tint = Color.Black.copy(alpha = 0.78f), modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = accent.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Typography & Font Choice
        item {
            SectionCard(title = "Tipografía y Escala", icon = Icons.Rounded.FormatSize) {
                Text("Familia tipográfica", style = MaterialTheme.typography.labelLarge)
                ChipRow(AppFontChoice.entries, customization.fontChoice, { it.label }, onFontChoiceChange)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Escala global de texto", style = MaterialTheme.typography.bodyMedium)
                    Text("${(customization.fontScale * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = customization.fontScale, onValueChange = onFontScaleChange, valueRange = 0.85f..1.25f)
            }
        }

        // Geometry & Shapes
        item {
            SectionCard(title = "Geometría y Carátulas", icon = Icons.Rounded.RoundedCorner) {
                Text("Bordes de tarjetas y botones", style = MaterialTheme.typography.labelLarge)
                ChipRow(CornerStyle.entries, customization.cornerStyle, { it.label }, onCornerStyleChange)
                Spacer(Modifier.height(4.dp))
                Text("Forma de carátula en reproductor", style = MaterialTheme.typography.labelLarge)
                ChipRow(ArtworkShapeStyle.entries, customization.artworkShape, { it.label }, onArtworkShapeChange)
            }
        }

        // Navigation & Library Layout
        item {
            SectionCard(title = "Navegación y Biblioteca", icon = Icons.Rounded.Navigation) {
                Text("Etiquetas en barra inferior", style = MaterialTheme.typography.labelLarge)
                ChipRow(NavLabelMode.entries, customization.navLabelMode, { it.label }, onNavLabelModeChange)
                Spacer(Modifier.height(4.dp))
                Text("Estilo del mini-reproductor", style = MaterialTheme.typography.labelLarge)
                ChipRow(MiniPlayerStyle.entries, customization.miniPlayerStyle, { it.label }, onMiniPlayerStyleChange)
                Spacer(Modifier.height(4.dp))
                Text("Diseño predeterminado de biblioteca", style = MaterialTheme.typography.labelLarge)
                ChipRow(LibraryLayoutMode.entries, customization.libraryLayout, { it.label }, onLibraryLayoutChange)
                Spacer(Modifier.height(4.dp))
                Text("Orden predeterminado", style = MaterialTheme.typography.labelLarge)
                ChipRow(LibrarySortOrder.entries, customization.sortOrder, { it.label }, onSortOrderChange)
                ToggleSettingRow("Modo lista compacta", "Muestra más canciones por pantalla", customization.compactDensity, onCompactDensityChange)
                ToggleSettingRow("Insignias de códec (FLAC, WAV, MP3)", "Muestra el formato de cada archivo", customization.showFormatBadges, onShowFormatBadgesChange)
                ToggleSettingRow("Carátula en mini-reproductor", "Muestra miniatura en la barra inferior", customization.showAlbumArtInMiniPlayer, onShowMiniPlayerArtChange)
            }
        }

        // Now Playing & Visualizer Style
        item {
            SectionCard(title = "Reproductor y Visualizador", icon = Icons.Rounded.GraphicEq) {
                Text("Vista principal", style = MaterialTheme.typography.labelLarge)
                ChipRow(NowPlayingStyle.entries, customization.nowPlayingStyle, { it.label }, onNowPlayingStyleChange)
                Spacer(Modifier.height(4.dp))
                Text("Visualizador de espectro", style = MaterialTheme.typography.labelLarge)
                ChipRow(VisualizerStyle.entries, customization.visualizerStyle, { it.label }, onVisualizerStyleChange)
                Spacer(Modifier.height(4.dp))
                Text("Salto rápido (-/+ segundos)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { sec ->
                        FilterChip(selected = customization.seekStepSeconds == sec, onClick = { onSeekStepChange(sec) }, label = { Text("${sec}s") })
                    }
                }
                ToggleSettingRow("Botones de salto rápido", "Muestra -${customization.seekStepSeconds}s y +${customization.seekStepSeconds}s en controles", customization.showQuickSeekButtons, onShowQuickSeekChange)
                ToggleSettingRow("Contenedor tonal en carátula", "Enmarca el escenario con superficie M3", customization.useTonalContainerBackdrop, onUseTonalBackdropChange)
            }
        }

        // Lyrics Customization
        item {
            SectionCard(title = "Personalización de Letras", icon = Icons.Rounded.Lyrics) {
                Text("Tamaño de letra", style = MaterialTheme.typography.labelLarge)
                ChipRow(LyricsSizeOption.entries, customization.lyricsSize, { it.label }, onLyricsSizeChange)
                Spacer(Modifier.height(4.dp))
                Text("Alineación", style = MaterialTheme.typography.labelLarge)
                ChipRow(LyricsAlignOption.entries, customization.lyricsAlign, { it.label }, onLyricsAlignChange)
                ToggleSettingRow("Mantener pantalla encendida", "Evita que se apague la pantalla al leer letras", customization.keepScreenOnLyrics, onKeepScreenOnLyricsChange)
            }
        }

        // Crossfade
        item {
            SectionCard(title = "Transición de Audio", icon = Icons.Rounded.Tune) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fundido cruzado (Crossfade)", style = MaterialTheme.typography.bodyMedium)
                    Text("${customization.crossfadeSeconds}s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = customization.crossfadeSeconds.toFloat(), onValueChange = { onCrossfadeChange(it.toInt()) }, valueRange = 0f..12f, steps = 11)
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    tagOf: ((T) -> String)? = null
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            val mod = if (tagOf != null) Modifier.testTag(tagOf(item)) else Modifier
            FilterChip(selected = selected == item, onClick = { onSelect(item) }, label = { Text(labelOf(item)) }, modifier = mod)
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun ToggleSettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
