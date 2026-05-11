package com.example.unstablestudio.core.config

/**
 * Centralized configuration and constants for the application.
 * All magic numbers, strings, and values should be defined here.
 */
object AppConstants {
    
    // LSP Configuration
    object Lsp {
        const val DEFAULT_MODE = "tcp"
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 9999
        const val DEFAULT_COMMAND = "kotlin-language-server"
        
        // LSP Methods
        const val METHOD_INITIALIZE = "initialize"
        const val METHOD_INITIALIZED = "initialized"
        const val METHOD_TEXT_DOCUMENT_DID_OPEN = "textDocument/didOpen"
        const val METHOD_TEXT_DOCUMENT_DID_CLOSE = "textDocument/didClose"
        const val METHOD_TEXT_DOCUMENT_DID_CHANGE = "textDocument/didChange"
        const val METHOD_TEXT_DOCUMENT_COMPLETION = "textDocument/completion"
        const val METHOD_TEXT_DOCUMENT_HOVER = "textDocument/hover"
        const val METHOD_TEXT_DOCUMENT_PUBLISH_DIAGNOSTICS = "textDocument/publishDiagnostics"
        const val METHOD_TEXT_DOCUMENT_DEFINITION = "textDocument/definition"
        const val METHOD_TEXT_DOCUMENT_REFERENCES = "textDocument/references"
        
        // Debounce configuration
        const val DID_CHANGE_DEBOUNCE_MS = 300L
        const val COMPLETION_DEBOUNCE_MS = 200L
        
        // Timeout configuration
        const val LSP_REQUEST_TIMEOUT_MS = 5000L
        const val LSP_RECONNECT_DELAY_MS = 3000L
    }
    
    // LSP Mode values (for when clauses)
    object LspModeValues {
        const val TCP = "tcp"
        const val PROCESS = "process"
    }
    
    // Editor Configuration
    object Editor {
        const val DEFAULT_FONT_SIZE = 14f
        const val MIN_FONT_SIZE = 10f
        const val MAX_FONT_SIZE = 30f
        const val FONT_SIZE_STEPS = 20
        
        const val DEFAULT_WORD_WRAP = false
        const val DEFAULT_TARGET_FPS = 60
        const val DEFAULT_THEME_MODE = "dark"
        
        // Font assets
        const val FONT_JETBRAINS_MONO = "fonts/JetBrainsMono/JetBrainsMono-Regular.ttf"
        const val FONT_JETBRAINS_MONO_MEDIUM = "fonts/JetBrainsMono/JetBrainsMono-Medium.ttf"
        const val FONT_ROBOTO_REGULAR = "fonts/Roboto/Roboto-Regular.ttf"
        const val FONT_ROBOTO_MEDIUM = "fonts/Roboto/Roboto-Medium.ttf"
        const val FONT_ROBOTO_BOLD = "fonts/Roboto/Roboto-Bold.ttf"
        
        // Grammar assets
        const val GRAMMAR_KOTLIN = "grammars/kotlin.tmLanguage.json"
        const val GRAMMAR_JAVA = "grammars/java.tmLanguage.json"
        const val GRAMMAR_JSON = "grammars/json.tmLanguage.json"
        const val GRAMMAR_XML = "grammars/xml.tmLanguage.json"
        
        // Theme assets
        const val THEME_DARK_PLUS = "themes/dark_plus.json"
    }
    
    // UI Configuration
    object Ui {
        const val DRAWER_WIDTH_DP = 300
        const val EXPLORER_PADDING_DP = 8
        const val FILE_NODE_INDENT_DP = 16
        const val TAB_EDGE_PADDING_DP = 0
        
        // Keyboard constants
        const val KEYBOARD_KEY_HEIGHT_DP = 40
        const val KEYBOARD_KEY_PADDING_DP = 2
        const val KEYBOARD_KEY_SPACING_DP = 4
        const val KEYBOARD_CORNER_RADIUS_DP = 4
        const val KEYBOARD_LABEL_FONT_SIZE_SP = 12
        const val KEYBOARD_KEY_FONT_SIZE_SP = 14
        const val KEYBOARD_ICON_SIZE_DP = 20
        
        // Dialog constants
        const val DIALOG_WIDTH_DP = 400
        const val DIALOG_ELEVATION_DP = 6
        const val DIALOG_PADDING_DP = 16
        const val DIALOG_ROW_HEIGHT_DP = 48
        
        // Menu bar constants
        const val MENU_BAR_HEIGHT_DP = 30
        const val MENU_BAR_ELEVATION_DP = 1
        const val MENU_ITEM_PADDING_HORIZONTAL_DP = 12
        const val MENU_ITEM_PADDING_VERTICAL_DP = 6
        const val MENU_DIVIDER_THICKNESS_DP = 1
        
        // Explorer constants
        const val EXPLORER_ICON_SIZE_SMALL_DP = 16
        const val EXPLORER_ICON_SIZE_MEDIUM_DP = 24
        const val EXPLORER_ICON_SIZE_LARGE_DP = 64
        const val FILE_TREE_INDENT_DP = 12
        const val FILE_TREE_START_PADDING_DP = 4
    }
    
    // File Extensions & MIME Types
    object FileTypes {
        val KOTLIN_EXTENSIONS = setOf(".kt", ".kts")
        val JAVA_EXTENSIONS = setOf(".java")
        val JSON_EXTENSIONS = setOf(".json")
        val XML_EXTENSIONS = setOf(".xml")
        val MARKDOWN_EXTENSIONS = setOf(".md")
        val HTML_EXTENSIONS = setOf(".html", ".htm")
        val CSS_EXTENSIONS = setOf(".css")
        val JS_EXTENSIONS = setOf(".js", ".mjs")
        val TS_EXTENSIONS = setOf(".ts", ".tsx")
        val PYTHON_EXTENSIONS = setOf(".py")
        val C_EXTENSIONS = setOf(".c", ".h")
        val CPP_EXTENSIONS = setOf(".cpp", ".hpp", ".cc", ".hh")
        
        const val MIME_KOTLIN = "text/x-kotlin"
        const val MIME_JAVA = "text/x-java"
        const val MIME_JSON = "application/json"
        const val MIME_XML = "text/xml"
        const val MIME_MARKDOWN = "text/markdown"
        const val MIME_HTML = "text/html"
        const val MIME_CSS = "text/css"
        const val MIME_JS = "text/javascript"
        const val MIME_PYTHON = "text/x-python"
        const val MIME_C = "text/x-csrc"
        const val MIME_CPP = "text/x-c++src"
        const val MIME_PLAIN = "text/plain"
        const val MIME_ANY = "*/*"
    }
    
    // Language IDs
    object LanguageIds {
        const val KOTLIN = "kotlin"
        const val JAVA = "java"
        const val JSON = "json"
        const val XML = "xml"
        const val MARKDOWN = "markdown"
        const val HTML = "html"
        const val CSS = "css"
        const val JAVASCRIPT = "javascript"
        const val TYPESCRIPT = "typescript"
        const val PYTHON = "python"
        const val CPP = "cpp"
        const val C = "c"
        const val PLAINTEXT = "plaintext"
    }
    
    // Performance Configuration
    object Performance {
        const val FPS_POWER_SAVING = 30
        const val FPS_STANDARD = 60
        const val FPS_SMOOTH = 120
        
        val FPS_OPTIONS = listOf(FPS_POWER_SAVING, FPS_STANDARD, FPS_SMOOTH)
    }
    
    // SharedPreferences Keys
    object PrefKeys {
        const val FONT_SIZE = "font_size"
        const val WORD_WRAP = "word_wrap"
        const val THEME_MODE = "theme_mode" // light, dark
        const val COLOR_MODE = "color_mode" // static, dynamic
        const val STATIC_THEME_STYLE = "static_theme_style" // bw, dark_blue, cream, dark_green, custom
        const val CUSTOM_STATIC_PRIMARY = "custom_static_primary"
        const val CUSTOM_STATIC_SECONDARY = "custom_static_secondary"
        const val CUSTOM_STATIC_TERTIARY = "custom_static_tertiary"
        const val CUSTOM_STATIC_TEXT = "custom_static_text"
        const val CUSTOM_STATIC_BACKGROUND = "custom_static_background"
        const val CUSTOM_STATIC_SURFACE = "custom_static_surface"
        const val DYNAMIC_THEME_SEED = "dynamic_theme_seed" // custom color int or presets
        const val USE_DYNAMIC_COLOR = "use_dynamic_color" // we'll keep this for legacy or replace it
        const val LSP_MODE = "lsp_mode"
        const val LSP_HOST = "lsp_host"
        const val LSP_PORT = "lsp_port"
        const val LSP_COMMAND = "lsp_command"
        const val LAST_ROOT_URI = "last_root_uri"
        const val OPEN_FILES = "open_files"
        const val RECENT_PROJECTS = "recent_projects"
        const val PREFS_NAME = "unstable_studio_prefs"
        const val KEEP_SCREEN_ON = "keep_screen_on"
        const val SHOW_LINE_NUMBERS = "show_line_numbers"
        const val USE_ORIGINAL_ICON_COLORS = "use_original_icon_colors"
        const val ACTIVE_DOCUMENT_ID = "active_document_id"
    }
    
    // Notification Configuration
    object Notifications {
        const val LSP_SERVICE_CHANNEL_ID = "LspServiceChannel"
        const val LSP_SERVICE_CHANNEL_NAME = "Language Server Service"
        const val LSP_SERVICE_CHANNEL_DESCRIPTION = "Running Language Server in the background"
        const val LSP_SERVICE_NOTIFICATION_ID = 1001
    }
    
    // Scope Names for TextMate
    object TextMateScopes {
        const val KOTLIN = "source.kotlin"
        const val JAVA = "source.java"
        const val JSON = "source.json"
        const val XML = "source.xml"
    }
}
