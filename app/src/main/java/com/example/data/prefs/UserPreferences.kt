package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.LibrarySortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sonora_prefs")

enum class ThemeMode(val label: String) {
    DARK_M3("Oscuro M3"),
    AMOLED_DARK("Oscuro AMOLED"),
    LIGHT_M3("Claro M3"),
    SYSTEM("Sistema")
}

enum class AccentTheme(val label: String, val previewHex: Long) {
    DYNAMIC_M3("Material You", 0xFF6750A4),
    PLUM_TONAL("Ciruela M3", 0xFFD0BCFF),
    INDIGO_TONAL("Índigo M3", 0xFFBCC2FF),
    COBALT_TONAL("Cobalto M3", 0xFFA8C7FA),
    OCEAN_TONAL("Océano M3", 0xFF69D3F8),
    SAGE_TONAL("Salvia M3", 0xFFA3D398),
    EMERALD_TONAL("Esmeralda M3", 0xFF7FD9B8),
    AMBER_TONAL("Ámbar M3", 0xFFF3C66F),
    TERRACOTTA_TONAL("Terracota M3", 0xFFFFB59D),
    ROSE_TONAL("Rosa M3", 0xFFFFB1C8),
    MONOCHROME_M3("Monocromo", 0xFFC6C6CA)
}

enum class AppFontChoice(val label: String) {
    MODERN_GROTESK("Space Grotesk"),
    GEOMETRIC_SANS("Plus Jakarta"),
    EDITORIAL_SERIF("Serif Editorial"),
    TECHNICAL_MONO("JetBrains Mono")
}

enum class NowPlayingStyle(val label: String) {
    COVER_CARD("Carátula M3"),
    VINYL_DISC("Disco Clásico"),
    SPECTRUM("Espectro Tonal")
}

enum class VisualizerStyle(val label: String) {
    TONAL_BARS("Barras M3"),
    WAVEFORM("Onda Tonal"),
    RADIAL("Circular M3"),
    OFF("Desactivado")
}

enum class CornerStyle(val label: String) {
    ROUNDED("Estándar M3"),
    COMPACT("Compacto"),
    PILL("Suave")
}

enum class ArtworkShapeStyle(val label: String, val cornerDp: Int) {
    ROUNDED("Redondeada", 24),
    SOFT("Suave", 36),
    SQUARE("Cuadrada", 6),
    CIRCLE("Circular", 180)
}

enum class NavLabelMode(val label: String) {
    ALWAYS("Siempre"),
    SELECTED("Solo activa"),
    ICONS_ONLY("Solo iconos")
}

enum class LyricsSizeOption(val label: String, val multiplier: Float) {
    COMPACT("Compacta", 0.85f),
    MEDIUM("Normal", 1.0f),
    LARGE("Grande", 1.22f)
}

enum class LyricsAlignOption(val label: String) {
    CENTER("Centrada"),
    START("Izquierda")
}

enum class MiniPlayerStyle(val label: String) {
    FLOATING_CARD("Tarjeta M3"),
    DOCKED_BAR("Barra Anclada")
}

enum class LibraryLayoutMode(val label: String) {
    LIST("Lista"),
    GRID("Cuadrícula")
}

@Serializable
data class UserCustomization(
    val themeMode: ThemeMode = ThemeMode.DARK_M3,
    val accentTheme: AccentTheme = AccentTheme.DYNAMIC_M3,
    val fontChoice: AppFontChoice = AppFontChoice.MODERN_GROTESK,
    val nowPlayingStyle: NowPlayingStyle = NowPlayingStyle.COVER_CARD,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.TONAL_BARS,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val artworkShape: ArtworkShapeStyle = ArtworkShapeStyle.ROUNDED,
    val navLabelMode: NavLabelMode = NavLabelMode.ALWAYS,
    val lyricsSize: LyricsSizeOption = LyricsSizeOption.MEDIUM,
    val lyricsAlign: LyricsAlignOption = LyricsAlignOption.CENTER,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.FLOATING_CARD,
    val libraryLayout: LibraryLayoutMode = LibraryLayoutMode.LIST,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENT_ADDED,
    val fontScale: Float = 1.0f,
    val compactDensity: Boolean = false,
    val showFormatBadges: Boolean = true,
    val useTonalContainerBackdrop: Boolean = true,
    val showQuickSeekButtons: Boolean = true,
    val seekStepSeconds: Int = 10,
    val keepScreenOnLyrics: Boolean = true,
    val showAlbumArtInMiniPlayer: Boolean = true,
    val crossfadeSeconds: Int = 3,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val skipSilence: Boolean = false,
    val monoAudio: Boolean = false,
    val softLimiter: Boolean = true,
    val eqEnabled: Boolean = true,
    val activeEqPresetName: String = "Estudio Sonora",
    val eqBandLevelsCsv: String = "200,100,0,150,250",
    val bassStrength: Int = 280,
    val virtualizerStrength: Int = 220,
    val loudnessGainMb: Int = 0,
    val preampDb: Float = 0f,
    val stereoBalance: Float = 0f,
    val lastSyncTimestampMs: Long = 0L,
    val demoSeeded: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val FONT_CHOICE = stringPreferencesKey("font_choice")
        val NOW_PLAYING_STYLE = stringPreferencesKey("now_playing_style")
        val VISUALIZER_STYLE = stringPreferencesKey("visualizer_style")
        val CORNER_STYLE = stringPreferencesKey("corner_style")
        val ARTWORK_SHAPE = stringPreferencesKey("artwork_shape")
        val NAV_LABEL_MODE = stringPreferencesKey("nav_label_mode")
        val LYRICS_SIZE = stringPreferencesKey("lyrics_size")
        val LYRICS_ALIGN = stringPreferencesKey("lyrics_align")
        val MINI_PLAYER_STYLE = stringPreferencesKey("mini_player_style")
        val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val COMPACT_DENSITY = booleanPreferencesKey("compact_density")
        val SHOW_FORMAT_BADGES = booleanPreferencesKey("show_format_badges")
        val TONAL_BACKDROP = booleanPreferencesKey("tonal_backdrop")
        val SHOW_QUICK_SEEK = booleanPreferencesKey("show_quick_seek")
        val SEEK_STEP_SECONDS = intPreferencesKey("seek_step_seconds")
        val KEEP_SCREEN_ON_LYRICS = booleanPreferencesKey("keep_screen_on_lyrics")
        val SHOW_MINI_ART = booleanPreferencesKey("show_mini_art")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val MONO_AUDIO = booleanPreferencesKey("mono_audio")
        val SOFT_LIMITER = booleanPreferencesKey("soft_limiter")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_NAME = stringPreferencesKey("eq_preset_name")
        val EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        val BASS_STRENGTH = intPreferencesKey("bass_strength")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val LOUDNESS_GAIN = intPreferencesKey("loudness_gain")
        val PREAMP_DB = floatPreferencesKey("preamp_db")
        val STEREO_BALANCE = floatPreferencesKey("stereo_balance")
        val LAST_SYNC_MS = longPreferencesKey("last_sync_ms")
        val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
    }

    val customizationFlow: Flow<UserCustomization> = context.dataStore.data.map { prefs ->
        readFromPreferences(prefs)
    }

    suspend fun updateCustomization(transform: (UserCustomization) -> UserCustomization) {
        context.dataStore.edit { prefs ->
            val current = readFromPreferences(prefs)
            val updated = transform(current)
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.ACCENT_THEME] = updated.accentTheme.name
            prefs[Keys.FONT_CHOICE] = updated.fontChoice.name
            prefs[Keys.NOW_PLAYING_STYLE] = updated.nowPlayingStyle.name
            prefs[Keys.VISUALIZER_STYLE] = updated.visualizerStyle.name
            prefs[Keys.CORNER_STYLE] = updated.cornerStyle.name
            prefs[Keys.ARTWORK_SHAPE] = updated.artworkShape.name
            prefs[Keys.NAV_LABEL_MODE] = updated.navLabelMode.name
            prefs[Keys.LYRICS_SIZE] = updated.lyricsSize.name
            prefs[Keys.LYRICS_ALIGN] = updated.lyricsAlign.name
            prefs[Keys.MINI_PLAYER_STYLE] = updated.miniPlayerStyle.name
            prefs[Keys.LIBRARY_LAYOUT] = updated.libraryLayout.name
            prefs[Keys.SORT_ORDER] = updated.sortOrder.name
            prefs[Keys.FONT_SCALE] = updated.fontScale
            prefs[Keys.COMPACT_DENSITY] = updated.compactDensity
            prefs[Keys.SHOW_FORMAT_BADGES] = updated.showFormatBadges
            prefs[Keys.TONAL_BACKDROP] = updated.useTonalContainerBackdrop
            prefs[Keys.SHOW_QUICK_SEEK] = updated.showQuickSeekButtons
            prefs[Keys.SEEK_STEP_SECONDS] = updated.seekStepSeconds
            prefs[Keys.KEEP_SCREEN_ON_LYRICS] = updated.keepScreenOnLyrics
            prefs[Keys.SHOW_MINI_ART] = updated.showAlbumArtInMiniPlayer
            prefs[Keys.CROSSFADE_SECONDS] = updated.crossfadeSeconds
            prefs[Keys.PLAYBACK_SPEED] = updated.playbackSpeed
            prefs[Keys.PLAYBACK_PITCH] = updated.playbackPitch
            prefs[Keys.SKIP_SILENCE] = updated.skipSilence
            prefs[Keys.MONO_AUDIO] = updated.monoAudio
            prefs[Keys.SOFT_LIMITER] = updated.softLimiter
            prefs[Keys.EQ_ENABLED] = updated.eqEnabled
            prefs[Keys.EQ_PRESET_NAME] = updated.activeEqPresetName
            prefs[Keys.EQ_BAND_LEVELS] = updated.eqBandLevelsCsv
            prefs[Keys.BASS_STRENGTH] = updated.bassStrength
            prefs[Keys.VIRTUALIZER_STRENGTH] = updated.virtualizerStrength
            prefs[Keys.LOUDNESS_GAIN] = updated.loudnessGainMb
            prefs[Keys.PREAMP_DB] = updated.preampDb
            prefs[Keys.STEREO_BALANCE] = updated.stereoBalance
            prefs[Keys.LAST_SYNC_MS] = updated.lastSyncTimestampMs
            prefs[Keys.DEMO_SEEDED] = updated.demoSeeded
        }
    }

    private fun readFromPreferences(prefs: Preferences): UserCustomization = UserCustomization(
        themeMode = enumByName(prefs[Keys.THEME_MODE], ThemeMode.DARK_M3),
        accentTheme = enumByName(prefs[Keys.ACCENT_THEME], AccentTheme.DYNAMIC_M3),
        fontChoice = enumByName(prefs[Keys.FONT_CHOICE], AppFontChoice.MODERN_GROTESK),
        nowPlayingStyle = enumByName(prefs[Keys.NOW_PLAYING_STYLE], NowPlayingStyle.COVER_CARD),
        visualizerStyle = enumByName(prefs[Keys.VISUALIZER_STYLE], VisualizerStyle.TONAL_BARS),
        cornerStyle = enumByName(prefs[Keys.CORNER_STYLE], CornerStyle.ROUNDED),
        artworkShape = enumByName(prefs[Keys.ARTWORK_SHAPE], ArtworkShapeStyle.ROUNDED),
        navLabelMode = enumByName(prefs[Keys.NAV_LABEL_MODE], NavLabelMode.ALWAYS),
        lyricsSize = enumByName(prefs[Keys.LYRICS_SIZE], LyricsSizeOption.MEDIUM),
        lyricsAlign = enumByName(prefs[Keys.LYRICS_ALIGN], LyricsAlignOption.CENTER),
        miniPlayerStyle = enumByName(prefs[Keys.MINI_PLAYER_STYLE], MiniPlayerStyle.FLOATING_CARD),
        libraryLayout = enumByName(prefs[Keys.LIBRARY_LAYOUT], LibraryLayoutMode.LIST),
        sortOrder = enumByName(prefs[Keys.SORT_ORDER], LibrarySortOrder.RECENT_ADDED),
        fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.25f),
        compactDensity = prefs[Keys.COMPACT_DENSITY] ?: false,
        showFormatBadges = prefs[Keys.SHOW_FORMAT_BADGES] ?: true,
        useTonalContainerBackdrop = prefs[Keys.TONAL_BACKDROP] ?: true,
        showQuickSeekButtons = prefs[Keys.SHOW_QUICK_SEEK] ?: true,
        seekStepSeconds = (prefs[Keys.SEEK_STEP_SECONDS] ?: 10).coerceIn(5, 30),
        keepScreenOnLyrics = prefs[Keys.KEEP_SCREEN_ON_LYRICS] ?: true,
        showAlbumArtInMiniPlayer = prefs[Keys.SHOW_MINI_ART] ?: true,
        crossfadeSeconds = (prefs[Keys.CROSSFADE_SECONDS] ?: 3).coerceIn(0, 12),
        playbackSpeed = (prefs[Keys.PLAYBACK_SPEED] ?: 1.0f).coerceIn(0.5f, 2.0f),
        playbackPitch = (prefs[Keys.PLAYBACK_PITCH] ?: 1.0f).coerceIn(0.5f, 2.0f),
        skipSilence = prefs[Keys.SKIP_SILENCE] ?: false,
        monoAudio = prefs[Keys.MONO_AUDIO] ?: false,
        softLimiter = prefs[Keys.SOFT_LIMITER] ?: true,
        eqEnabled = prefs[Keys.EQ_ENABLED] ?: true,
        activeEqPresetName = prefs[Keys.EQ_PRESET_NAME] ?: "Estudio Sonora",
        eqBandLevelsCsv = prefs[Keys.EQ_BAND_LEVELS] ?: "200,100,0,150,250",
        bassStrength = (prefs[Keys.BASS_STRENGTH] ?: 280).coerceIn(0, 1000),
        virtualizerStrength = (prefs[Keys.VIRTUALIZER_STRENGTH] ?: 220).coerceIn(0, 1000),
        loudnessGainMb = (prefs[Keys.LOUDNESS_GAIN] ?: 0).coerceIn(0, 1500),
        preampDb = (prefs[Keys.PREAMP_DB] ?: 0f).coerceIn(-12f, 12f),
        stereoBalance = (prefs[Keys.STEREO_BALANCE] ?: 0f).coerceIn(-1f, 1f),
        lastSyncTimestampMs = prefs[Keys.LAST_SYNC_MS] ?: 0L,
        demoSeeded = prefs[Keys.DEMO_SEEDED] ?: false
    )

    private inline fun <reified T : Enum<T>> enumByName(raw: String?, default: T): T {
        return enumValues<T>().firstOrNull { it.name == raw } ?: default
    }
}
