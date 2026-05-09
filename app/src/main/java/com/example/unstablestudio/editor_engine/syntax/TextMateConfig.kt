package com.example.unstablestudio.editor_engine.syntax

import android.content.Context
import com.example.unstablestudio.core.common.AppLogger
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource

private const val TAG = "TextMateConfig"

/**
 * Konfigurasi TextMate untuk Sora-Editor.
 * Menggunakan skema JSON dari VS Code (seperti tmTheme atau json).
 */
object TextMateConfig {

    private var isInitialized = false

    /**
     * Inisialisasi registry grammar dan tema TextMate.
     * Biasanya dipanggil sekali saat aplikasi atau layar editor pertama kali dimuat.
     */
    suspend fun initialize(context: Context) {
        if (isInitialized) return

        withContext(Dispatchers.IO) {
            try {
                // Resolver untuk membaca file grammar (.json, .plist) dan theme (.tmTheme, .json) dari assets/
                val fileResolver = AssetsFileResolver(context.assets)
                FileProviderRegistry.getInstance().addFileProvider(fileResolver)

                // Load tema VS Code default (Dark+) dari assets
                val themePath = "themes/dark_plus.json"
                try {
                    val themeSource = IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(themePath),
                        themePath,
                        null
                    )
                    ThemeRegistry.getInstance().loadTheme(themeSource)
                    ThemeRegistry.getInstance().setTheme("dark_plus.json")
                    AppLogger.d(TAG, "Theme loaded successfully")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Failed to load theme, using default theme", e)
                    // Continue with default theme
                }

                // Load grammar untuk Kotlin
                try {
                    GrammarRegistry.getInstance().loadGrammars(
                        io.github.rosemoe.sora.langs.textmate.registry.dsl.languages {
                            language("kotlin") {
                                grammar = "grammars/kotlin.tmLanguage.json"
                                scopeName = "source.kotlin"
                            }
                        }
                    )
                    AppLogger.d(TAG, "Kotlin grammar loaded successfully")
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Failed to load Kotlin grammar, syntax highlighting may not work", e)
                    // Continue without grammar
                }

                isInitialized = true
                AppLogger.d(TAG, "TextMate initialization completed")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to initialize TextMate", e)
            }
        }
    }

    /**
     * Terapkan bahasa (Syntax Highlighting) ke instans CodeEditor.
     * LSP autocomplete akan dihandle secara terpisah.
     */
    fun setupLanguage(editor: CodeEditor, scopeName: String) {
        try {
            // scopeName contoh: "source.kotlin", "source.java"
            val textMateLanguage = TextMateLanguage.create(scopeName, true)
            editor.setEditorLanguage(textMateLanguage)

            // Set Color Scheme berdasarkan tema TextMate yang dimuat
            val scheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
            editor.colorScheme = scheme
            
            AppLogger.d(TAG, "Language setup complete for $scopeName")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to setup language: $scopeName", e)
        }
    }
}
