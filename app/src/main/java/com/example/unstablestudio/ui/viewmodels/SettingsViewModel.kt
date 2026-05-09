package com.example.unstablestudio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.unstablestudio.data.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val fontSize: StateFlow<Float> = repository.fontSize
    val wordWrap: StateFlow<Boolean> = repository.wordWrap
    val themeMode: StateFlow<String> = repository.themeMode
    val colorMode: StateFlow<String> = repository.colorMode
    val staticThemeStyle: StateFlow<String> = repository.staticThemeStyle
    val dynamicThemeSeed: StateFlow<Long> = repository.dynamicThemeSeed
    val customStaticPrimary: StateFlow<Long> = repository.customStaticPrimary
    val customStaticSecondary: StateFlow<Long> = repository.customStaticSecondary
    val customStaticTertiary: StateFlow<Long> = repository.customStaticTertiary
    val customStaticText: StateFlow<Long> = repository.customStaticText
    val customStaticBackground: StateFlow<Long> = repository.customStaticBackground
    val customStaticSurface: StateFlow<Long> = repository.customStaticSurface
    val useDynamicColor: StateFlow<Boolean> = repository.useDynamicColor
    val keepScreenOn: StateFlow<Boolean> = repository.keepScreenOn
    val showLineNumbers: StateFlow<Boolean> = repository.showLineNumbers
    val useOriginalIconColors: StateFlow<Boolean> = repository.useOriginalIconColors

    fun setFontSize(size: Float) {
        repository.setFontSize(size)
    }

    fun setUseOriginalIconColors(enabled: Boolean) {
        repository.setUseOriginalIconColors(enabled)
    }

    fun setWordWrap(enabled: Boolean) {
        repository.setWordWrap(enabled)
    }

    fun setThemeMode(mode: String) {
        repository.setThemeMode(mode)
    }

    fun setColorMode(mode: String) {
        repository.setColorMode(mode)
    }

    fun setStaticThemeStyle(style: String) {
        repository.setStaticThemeStyle(style)
    }

    fun setDynamicThemeSeed(seed: Long) {
        repository.setDynamicThemeSeed(seed)
    }

    fun setCustomStaticColors(primary: Long, secondary: Long, tertiary: Long, text: Long, background: Long, surface: Long) {
        repository.setCustomStaticColors(primary, secondary, tertiary, text, background, surface)
    }

    fun setUseDynamicColor(enabled: Boolean) {
        repository.setUseDynamicColor(enabled)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        repository.setKeepScreenOn(enabled)
    }

    fun setShowLineNumbers(enabled: Boolean) {
        repository.setShowLineNumbers(enabled)
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
