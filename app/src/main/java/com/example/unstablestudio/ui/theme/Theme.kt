package com.example.unstablestudio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeContent

// 1. Static Themes (fixed palettes, no light/dark switching)
private val WhiteStaticColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF333333),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEAEAEA),
    onSecondaryContainer = Color(0xFF000000),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF000000),
)

private val DarkStaticColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF444444),
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFFFFFFF),
)

private val DarkBlueStaticColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    background = Color(0xFF00183F),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF00183F),
    onSurface = Color(0xFFE2E2E9)
)

private val DarkContrastStaticColorScheme = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color(0xFF2A1F00),
    primaryContainer = Color(0xFF4A3A00),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003732),
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFF9FF0E9),
    tertiary = Color(0xFFFFAB91),
    onTertiary = Color(0xFF571E00),
    tertiaryContainer = Color(0xFF7A3008),
    onTertiaryContainer = Color(0xFFFFDBD0),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFF2F2F2),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFF757575)
)

private fun createCustomStaticColorScheme(
    primary: Long,
    secondary: Long,
    tertiary: Long,
    text: Long,
    background: Long,
    surface: Long
): ColorScheme {
    val backgroundColor = Color(background)
    val isDarkBackground = backgroundColor.luminance() < 0.5f
    return if (isDarkBackground) {
        darkColorScheme(
            primary = Color(primary),
            secondary = Color(secondary),
            tertiary = Color(tertiary),
            background = backgroundColor,
            surface = Color(surface),
            onPrimary = Color(text),
            onSecondary = Color(text),
            onTertiary = Color(text),
            onBackground = Color(text),
            onSurface = Color(text)
        )
    } else {
        lightColorScheme(
            primary = Color(primary),
            secondary = Color(secondary),
            tertiary = Color(tertiary),
            background = backgroundColor,
            surface = Color(surface),
            onPrimary = Color(text),
            onSecondary = Color(text),
            onTertiary = Color(text),
            onBackground = Color(text),
            onSurface = Color(text)
        )
    }
}

// 2. Mathematical Dynamic Generator from Material Color Utilities
fun createColorSchemeFromSeed(seed: Long, isDark: Boolean): ColorScheme {
    val scheme = SchemeContent(Hct.fromInt(seed.toInt()), isDark, 0.0)
    
    return ColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        inversePrimary = Color(scheme.inversePrimary),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        surfaceTint = Color(scheme.primary),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        surfaceBright = Color(scheme.surfaceBright),
        surfaceDim = Color(scheme.surfaceDim),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        surfaceContainerHighest = Color(scheme.surfaceContainerHighest),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainerLowest = Color(scheme.surfaceContainerLowest)
    )
}

@Composable
fun UnstableStudioTheme(
    themeMode: String = "dark",
    colorMode: String = "dynamic",
    staticThemeStyle: String = "bw",
    dynamicThemeSeed: Long = 0xFF6750A4,
    customStaticPrimary: Long = 0xFF6200EE,
    customStaticSecondary: Long = 0xFF03DAC6,
    customStaticTertiary: Long = 0xFF018786,
    customStaticText: Long = 0xFFFFFFFF,
    customStaticBackground: Long = 0xFF121212,
    customStaticSurface: Long = 0xFF1E1E1E,
    useDynamicColor: Boolean = true, // System Wallpaper fallback
    content: @Composable () -> Unit
) {
    val isDynamicDark = themeMode == "dark"
    val context = LocalContext.current

    val colorScheme = when (colorMode) {
        "dynamic" -> {
            when (dynamicThemeSeed) {
                0L -> {
                    // 0L means System Wallpaper (Material You)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (isDynamicDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    } else {
                        // Fallback to mathematical generation from a default seed
                        createColorSchemeFromSeed(0xFF6750A4, isDynamicDark)
                    }
                }
                else -> {
                    // Mathematical Custom Generation from Seed
                    createColorSchemeFromSeed(dynamicThemeSeed, isDynamicDark)
                }
            }
        }
        "static" -> {
            when (staticThemeStyle) {
                "white", "bw", "cream" -> WhiteStaticColorScheme
                "dark", "dark_green" -> DarkStaticColorScheme
                "dark_blue" -> DarkBlueStaticColorScheme
                "dark_contrast" -> DarkContrastStaticColorScheme
                "custom" -> createCustomStaticColorScheme(
                    primary = customStaticPrimary,
                    secondary = customStaticSecondary,
                    tertiary = customStaticTertiary,
                    text = customStaticText,
                    background = customStaticBackground,
                    surface = customStaticSurface
                )
                else -> WhiteStaticColorScheme
            }
        }
        else -> WhiteStaticColorScheme
    }
    val isDarkForSystemBars = colorScheme.background.luminance() < 0.5f
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkForSystemBars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
