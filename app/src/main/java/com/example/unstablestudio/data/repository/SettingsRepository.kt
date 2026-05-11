package com.example.unstablestudio.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.unstablestudio.core.common.AppLogger
import com.example.unstablestudio.core.config.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class SettingsRepository private constructor(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        AppConstants.PrefKeys.PREFS_NAME, 
        Context.MODE_PRIVATE
    )

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context).also { instance = it }
            }
        }
    }

    // Flows
    private val _fontSize = MutableStateFlow(prefs.getFloat(AppConstants.PrefKeys.FONT_SIZE, AppConstants.Editor.DEFAULT_FONT_SIZE))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _wordWrap = MutableStateFlow(prefs.getBoolean(AppConstants.PrefKeys.WORD_WRAP, AppConstants.Editor.DEFAULT_WORD_WRAP))
    val wordWrap: StateFlow<Boolean> = _wordWrap.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(AppConstants.PrefKeys.THEME_MODE, "dark") ?: "dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _colorMode = MutableStateFlow(prefs.getString(AppConstants.PrefKeys.COLOR_MODE, "dynamic") ?: "dynamic")
    val colorMode: StateFlow<String> = _colorMode.asStateFlow()

    private val _staticThemeStyle = MutableStateFlow(prefs.getString(AppConstants.PrefKeys.STATIC_THEME_STYLE, "bw") ?: "bw")
    val staticThemeStyle: StateFlow<String> = _staticThemeStyle.asStateFlow()

    private val _dynamicThemeSeed = MutableStateFlow(prefs.getLong(AppConstants.PrefKeys.DYNAMIC_THEME_SEED, 0xFF6750A4)) // Default to purple
    val dynamicThemeSeed: StateFlow<Long> = _dynamicThemeSeed.asStateFlow()

    private val _customStaticPrimary = MutableStateFlow(prefs.getLong(AppConstants.PrefKeys.CUSTOM_STATIC_PRIMARY, 0xFF6200EE))
    val customStaticPrimary: StateFlow<Long> = _customStaticPrimary.asStateFlow()

    private val _customStaticSecondary = MutableStateFlow(prefs.getLong(AppConstants.PrefKeys.CUSTOM_STATIC_SECONDARY, 0xFF03DAC6))
    val customStaticSecondary: StateFlow<Long> = _customStaticSecondary.asStateFlow()

    private val _customStaticTertiary = MutableStateFlow(prefs.getLong(AppConstants.PrefKeys.CUSTOM_STATIC_TERTIARY, 0xFF018786))
    val customStaticTertiary: StateFlow<Long> = _customStaticTertiary.asStateFlow()

    private val _customStaticText = MutableStateFlow(prefs.getLong(AppConstants.PrefKeys.CUSTOM_STATIC_TEXT, 0xFFFFFFFF))
    val customStaticText: StateFlow<Long> = _customStaticText.asStateFlow()

    private val _customStaticBackground = MutableStateFlow(prefs.getLong(AppConstants.PrefKeys.CUSTOM_STATIC_BACKGROUND, 0xFF121212))
    val customStaticBackground: StateFlow<Long> = _customStaticBackground.asStateFlow()

    private val _customStaticSurface = MutableStateFlow(prefs.getLong(AppConstants.PrefKeys.CUSTOM_STATIC_SURFACE, 0xFF1E1E1E))
    val customStaticSurface: StateFlow<Long> = _customStaticSurface.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(prefs.getBoolean(AppConstants.PrefKeys.USE_DYNAMIC_COLOR, true))
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean(AppConstants.PrefKeys.KEEP_SCREEN_ON, false))
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _showLineNumbers = MutableStateFlow(prefs.getBoolean(AppConstants.PrefKeys.SHOW_LINE_NUMBERS, true))
    val showLineNumbers: StateFlow<Boolean> = _showLineNumbers.asStateFlow()

    private val _useOriginalIconColors = MutableStateFlow(prefs.getBoolean(AppConstants.PrefKeys.USE_ORIGINAL_ICON_COLORS, false))
    val useOriginalIconColors: StateFlow<Boolean> = _useOriginalIconColors.asStateFlow()

    private val _lastRootUri = MutableStateFlow(prefs.getString(AppConstants.PrefKeys.LAST_ROOT_URI, null))
    val lastRootUri: StateFlow<String?> = _lastRootUri.asStateFlow()

    private val _recentProjects = MutableStateFlow(
        prefs.getString(AppConstants.PrefKeys.RECENT_PROJECTS, "")
            ?.split("|")
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    )
    val recentProjects: StateFlow<List<String>> = _recentProjects.asStateFlow()

    private val _openFiles = MutableStateFlow(
        prefs.getString(AppConstants.PrefKeys.OPEN_FILES, "")
            ?.split("|")
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    )
    val openFiles: StateFlow<List<String>> = _openFiles.asStateFlow()
    
    private val _activeDocumentId = MutableStateFlow(prefs.getString(AppConstants.PrefKeys.ACTIVE_DOCUMENT_ID, null))
    val activeDocumentId: StateFlow<String?> = _activeDocumentId.asStateFlow()

    fun setFontSize(size: Float) {
        prefs.edit().putFloat(AppConstants.PrefKeys.FONT_SIZE, size).apply()
        _fontSize.value = size
    }

    fun setWordWrap(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.PrefKeys.WORD_WRAP, enabled).apply()
        _wordWrap.value = enabled
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(AppConstants.PrefKeys.THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.PrefKeys.KEEP_SCREEN_ON, enabled).apply()
        _keepScreenOn.value = enabled
    }

    fun setShowLineNumbers(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.PrefKeys.SHOW_LINE_NUMBERS, enabled).apply()
        _showLineNumbers.value = enabled
    }

    fun setUseOriginalIconColors(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.PrefKeys.USE_ORIGINAL_ICON_COLORS, enabled).apply()
        _useOriginalIconColors.value = enabled
    }

    fun setUseDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(AppConstants.PrefKeys.USE_DYNAMIC_COLOR, enabled).apply()
        _useDynamicColor.value = enabled
    }

    fun setColorMode(mode: String) {
        prefs.edit().putString(AppConstants.PrefKeys.COLOR_MODE, mode).apply()
        _colorMode.value = mode
    }

    fun setStaticThemeStyle(style: String) {
        prefs.edit().putString(AppConstants.PrefKeys.STATIC_THEME_STYLE, style).apply()
        _staticThemeStyle.value = style
    }

    fun setDynamicThemeSeed(seed: Long) {
        prefs.edit().putLong(AppConstants.PrefKeys.DYNAMIC_THEME_SEED, seed).apply()
        _dynamicThemeSeed.value = seed
    }

    fun setCustomStaticColors(primary: Long, secondary: Long, tertiary: Long, text: Long, background: Long, surface: Long) {
        prefs.edit().apply {
            putLong(AppConstants.PrefKeys.CUSTOM_STATIC_PRIMARY, primary)
            putLong(AppConstants.PrefKeys.CUSTOM_STATIC_SECONDARY, secondary)
            putLong(AppConstants.PrefKeys.CUSTOM_STATIC_TERTIARY, tertiary)
            putLong(AppConstants.PrefKeys.CUSTOM_STATIC_TEXT, text)
            putLong(AppConstants.PrefKeys.CUSTOM_STATIC_BACKGROUND, background)
            putLong(AppConstants.PrefKeys.CUSTOM_STATIC_SURFACE, surface)
        }.apply()
        _customStaticPrimary.value = primary
        _customStaticSecondary.value = secondary
        _customStaticTertiary.value = tertiary
        _customStaticText.value = text
        _customStaticBackground.value = background
        _customStaticSurface.value = surface
    }

    fun setLastRootUri(uri: String?) {
        prefs.edit().putString(AppConstants.PrefKeys.LAST_ROOT_URI, uri).apply()
        _lastRootUri.value = uri
        if (!uri.isNullOrEmpty()) {
            val currentRecent = _recentProjects.value.toMutableList()
            currentRecent.remove(uri)
            currentRecent.add(0, uri)
            val limitedRecent = currentRecent.take(10)
            _recentProjects.value = limitedRecent
            prefs.edit().putString(AppConstants.PrefKeys.RECENT_PROJECTS, limitedRecent.joinToString("|")).apply()
        }
    }

    fun clearRecentProjects() {
        prefs.edit().remove(AppConstants.PrefKeys.RECENT_PROJECTS).apply()
        _recentProjects.value = emptyList()
    }

    fun setOpenFiles(uris: List<String>) {
        prefs.edit().putString(AppConstants.PrefKeys.OPEN_FILES, uris.joinToString("|")).apply()
        _openFiles.value = uris
    }

    fun getFilesForWorkspace(workspaceUri: String): List<String> {
        val key = "open_files_${workspaceUri.hashCode()}"
        return prefs.getString(key, "")?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    fun setFilesForWorkspace(workspaceUri: String, uris: List<String>) {
        val key = "open_files_${workspaceUri.hashCode()}"
        prefs.edit().putString(key, uris.joinToString("|")).apply()
    }

    fun getActiveDocumentIdForWorkspace(workspaceUri: String): String? {
        val key = "active_doc_${workspaceUri.hashCode()}"
        return prefs.getString(key, null)
    }

    fun setActiveDocumentIdForWorkspace(workspaceUri: String, id: String?) {
        val key = "active_doc_${workspaceUri.hashCode()}"
        prefs.edit().putString(key, id).apply()
        _activeDocumentId.value = id
    }
}
