package com.example.unstablestudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unstablestudio.core.runtime.RuntimeManager
import com.example.unstablestudio.data.repository.SettingsRepository
import com.example.unstablestudio.ui.screens.SettingsScreen
import com.example.unstablestudio.ui.theme.UnstableStudioTheme
import com.example.unstablestudio.ui.viewmodels.SettingsViewModel
import com.example.unstablestudio.ui.viewmodels.SettingsViewModelFactory

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository.getInstance(this)
        val runtimeManager = RuntimeManager(this)
        runtimeManager.ensureRuntimeInstalled()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(settingsRepository)
            )

            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val colorMode by settingsViewModel.colorMode.collectAsStateWithLifecycle()
            val staticThemeStyle by settingsViewModel.staticThemeStyle.collectAsStateWithLifecycle()
            val dynamicThemeSeed by settingsViewModel.dynamicThemeSeed.collectAsStateWithLifecycle()
            val customStaticPrimary by settingsViewModel.customStaticPrimary.collectAsStateWithLifecycle()
            val customStaticSecondary by settingsViewModel.customStaticSecondary.collectAsStateWithLifecycle()
            val customStaticTertiary by settingsViewModel.customStaticTertiary.collectAsStateWithLifecycle()
            val customStaticText by settingsViewModel.customStaticText.collectAsStateWithLifecycle()
            val customStaticBackground by settingsViewModel.customStaticBackground.collectAsStateWithLifecycle()
            val customStaticSurface by settingsViewModel.customStaticSurface.collectAsStateWithLifecycle()
            val useDynamicColor by settingsViewModel.useDynamicColor.collectAsStateWithLifecycle()

            UnstableStudioTheme(
                themeMode = themeMode,
                colorMode = colorMode,
                staticThemeStyle = staticThemeStyle,
                dynamicThemeSeed = dynamicThemeSeed,
                customStaticPrimary = customStaticPrimary,
                customStaticSecondary = customStaticSecondary,
                customStaticTertiary = customStaticTertiary,
                customStaticText = customStaticText,
                customStaticBackground = customStaticBackground,
                customStaticSurface = customStaticSurface,
                useDynamicColor = useDynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    com.example.unstablestudio.ui.components.GlobalKeyboardContainer {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            runtimeManager = runtimeManager,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }
}
