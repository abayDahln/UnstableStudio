package com.example.unstablestudio.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unstablestudio.core.config.AppConstants
import com.example.unstablestudio.core.runtime.RuntimeManager
import com.example.unstablestudio.ui.components.LocalKeyboardManager
import com.example.unstablestudio.ui.components.SpecialKey
import com.example.unstablestudio.ui.theme.UnstableStudioTheme
import com.example.unstablestudio.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    runtimeManager: RuntimeManager,
    onBack: () -> Unit
) {
    val keyboardManager = LocalKeyboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("SELESAI", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            CustomSettingsTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari pengaturan...", style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
            
            // Settings List
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                if (isLandscape) {
                    // Two-column layout for landscape
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            GeneralSettings(viewModel, searchQuery)
                            AppearanceSettings(viewModel, searchQuery)
                            TerminalSettings(viewModel, searchQuery)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            EditorSettings(viewModel, searchQuery)
                            RuntimeSettings(runtimeManager, searchQuery)
                            AboutSettings(searchQuery)
                        }
                    }
                } else {
                    // Single-column layout for portrait
                    GeneralSettings(viewModel, searchQuery)
                    AppearanceSettings(viewModel, searchQuery)
                    EditorSettings(viewModel, searchQuery)
                    TerminalSettings(viewModel, searchQuery)
                    RuntimeSettings(runtimeManager, searchQuery)
                    AboutSettings(searchQuery)
                }
            }
        }
    }
}

@Composable
private fun CustomSettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = OutlinedTextFieldDefaults.shape,
    singleLine: Boolean = false,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    val keyboardManager = LocalKeyboardManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        modifier = modifier.onFocusChanged {
            if (it.isFocused) {
                keyboardManager.show()
                keyboardManager.registerInput(
                    onTextInput = { text -> onValueChange(value + text) },
                    onSpecialKey = { key ->
                        if (key == SpecialKey.BACKSPACE && value.isNotEmpty()) {
                            onValueChange(value.dropLast(1))
                        }
                    }
                )
            }
        },
        shape = shape,
        singleLine = singleLine,
        readOnly = true,
        colors = colors
    )
}

fun String.matchesSearch(query: String): Boolean {
    return query.isBlank() || this.contains(query, ignoreCase = true)
}

@Composable
fun SettingSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun SettingsListItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        trailingContent = trailingContent,
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun GeneralSettings(viewModel: SettingsViewModel, searchQuery: String) {
    val showKeepScreenOn = "Tetap Nyalakan Layar".matchesSearch(searchQuery) || "Mencegah layar redup".matchesSearch(searchQuery)

    if (showKeepScreenOn) {
        SettingSection("Inti IDE")
        SettingsCard {
            val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
            SettingsListItem(
                title = "Tetap Nyalakan Layar",
                subtitle = "Mencegah layar redup",
                trailingContent = {
                    Switch(checked = keepScreenOn, onCheckedChange = { viewModel.setKeepScreenOn(it) })
                },
                onClick = { viewModel.setKeepScreenOn(!keepScreenOn) }
            )
        }
    }
}

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel, searchQuery: String) {
    val showMode = "Mode Kecerahan".matchesSearch(searchQuery) || "Gelap".matchesSearch(searchQuery) || "Terang".matchesSearch(searchQuery)
    val showColorMode = "Tipe Warna".matchesSearch(searchQuery) || "Dinamis".matchesSearch(searchQuery) || "Statis".matchesSearch(searchQuery)
    val showThemeStyle = "Gaya Warna".matchesSearch(searchQuery) || "Kustom".matchesSearch(searchQuery)
    val showPreview = "Pratinjau Tema".matchesSearch(searchQuery) || "Visualisasi tema".matchesSearch(searchQuery)
    val showIconColor = "Warna Ikon Explorer".matchesSearch(searchQuery) || "ikon asli".matchesSearch(searchQuery) || "ikuti tema".matchesSearch(searchQuery)
    val colorMode by viewModel.colorMode.collectAsStateWithLifecycle()
    val showBrightnessMode = showMode && colorMode == "dynamic"

    if (showMode || showColorMode || showThemeStyle || showPreview || showIconColor) {
        SettingSection("Tampilan")
        SettingsCard {
            if (showBrightnessMode) {
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                var showModeDialog by remember { mutableStateOf(false) }
                
                SettingsListItem(
                    title = "Mode Kecerahan",
                    subtitle = if (themeMode == "dark") "Gelap" else "Terang",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (themeMode == "dark") "Dark" else "Light", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { showModeDialog = true }
                )
                
                if (showModeDialog) {
                    AlertDialog(
                        onDismissRequest = { showModeDialog = false },
                        title = { Text("Mode Kecerahan") },
                        text = {
                            Column {
                                listOf("light" to "Terang", "dark" to "Gelap").forEach { (mode, label) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.setThemeMode(mode)
                                                showModeDialog = false 
                                            }
                                            .padding(vertical = 12.dp)
                                    ) {
                                        RadioButton(
                                            selected = themeMode == mode,
                                            onClick = { 
                                                viewModel.setThemeMode(mode)
                                                showModeDialog = false 
                                            }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(label, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showModeDialog = false }) {
                                Text("BATAL")
                            }
                        }
                    )
                }
            }
            
            if (showBrightnessMode && showColorMode) SettingsDivider()
            
            if (showColorMode) {
                var showColorModeDialog by remember { mutableStateOf(false) }
                
                SettingsListItem(
                    title = "Tipe Warna",
                    subtitle = if (colorMode == "dynamic") "Dinamis (Matematis)" else "Statis",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (colorMode == "dynamic") "Dinamis" else "Statis", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { showColorModeDialog = true }
                )
                
                if (showColorModeDialog) {
                    AlertDialog(
                        onDismissRequest = { showColorModeDialog = false },
                        title = { Text("Tipe Warna") },
                        text = {
                            Column {
                                listOf("dynamic" to "Dinamis (Palette Generator)", "static" to "Statis (Warna Tetap)").forEach { (mode, label) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                viewModel.setColorMode(mode)
                                                showColorModeDialog = false 
                                            }
                                            .padding(vertical = 12.dp)
                                    ) {
                                        RadioButton(
                                            selected = colorMode == mode,
                                            onClick = { 
                                                viewModel.setColorMode(mode)
                                                showColorModeDialog = false 
                                            }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(label, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showColorModeDialog = false }) {
                                Text("BATAL")
                            }
                        }
                    )
                }
            }

            if ((showBrightnessMode || showColorMode) && showThemeStyle) SettingsDivider()

            if (showThemeStyle) {
                if (colorMode == "dynamic") {
                    val dynamicThemeSeed by viewModel.dynamicThemeSeed.collectAsStateWithLifecycle()
                    var showDynamicSeedDialog by remember { mutableStateOf(false) }
                    
                    val dynamicLabel = when (dynamicThemeSeed) {
                        0L -> "Wallpaper Sistem"
                        0xFF6750A4 -> "Ungu (Default)"
                        0xFF1A73E8 -> "Biru"
                        0xFF1B6B34 -> "Hijau"
                        0xFFB3261E -> "Merah"
                        else -> "Kustom"
                    }

                    SettingsListItem(
                        title = "Sumber Warna",
                        subtitle = dynamicLabel,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dynamicLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = { showDynamicSeedDialog = true }
                    )
                    
                    if (showDynamicSeedDialog) {
                        var customHex by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and dynamicThemeSeed.toInt())) }
                        AlertDialog(
                            onDismissRequest = { showDynamicSeedDialog = false },
                            title = { Text("Sumber Warna Dinamis") },
                            text = {
                                Column {
                                    val options = listOf(
                                        0L to "Wallpaper Sistem",
                                        0xFF6750A4 to "Ungu",
                                        0xFF1A73E8 to "Biru",
                                        0xFF1B6B34 to "Hijau",
                                        0xFFB3261E to "Merah",
                                        -1L to "Kustom (Input HEX)"
                                    )
                                    
                                    options.forEach { (seed, label) ->
                                        val isSelected = if (seed == -1L) {
                                            options.none { it.first == dynamicThemeSeed }
                                        } else {
                                            dynamicThemeSeed == seed
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { 
                                                    if (seed != -1L) {
                                                        viewModel.setDynamicThemeSeed(seed)
                                                        showDynamicSeedDialog = false
                                                    } else {
                                                        viewModel.setDynamicThemeSeed(0xFF999999) // Temp placeholder for custom
                                                    }
                                                }
                                                .padding(vertical = 12.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { 
                                                    if (seed != -1L) {
                                                        viewModel.setDynamicThemeSeed(seed)
                                                        showDynamicSeedDialog = false
                                                    }
                                                }
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(label, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                    
                                    val isCustomSelected = options.none { it.first == dynamicThemeSeed }
                                    if (isCustomSelected || dynamicThemeSeed == 0xFF999999) {
                                        CustomSettingsTextField(
                                            value = customHex,
                                            onValueChange = { customHex = it },
                                            label = { Text("Warna Utama (HEX)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { 
                                    val isCustomSelected = listOf(0L, 0xFF6750A4, 0xFF1A73E8, 0xFF1B6B34, 0xFFB3261E).none { it == dynamicThemeSeed }
                                    if (isCustomSelected || dynamicThemeSeed == 0xFF999999) {
                                        try {
                                            var hexStr = customHex
                                            if (hexStr.startsWith("#")) hexStr = hexStr.substring(1)
                                            if (hexStr.length == 6) hexStr = "FF$hexStr"
                                            val parsedColor = hexStr.toLong(16)
                                            viewModel.setDynamicThemeSeed(parsedColor)
                                        } catch (e: Exception) {
                                            // Invalid color, ignore
                                        }
                                    }
                                    showDynamicSeedDialog = false 
                                }) {
                                    Text("SIMPAN")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDynamicSeedDialog = false }) {
                                    Text("BATAL")
                                }
                            }
                        )
                    }
                } else {
                    val staticThemeStyle by viewModel.staticThemeStyle.collectAsStateWithLifecycle()
                    var showStaticStyleDialog by remember { mutableStateOf(false) }
                    
                    val staticLabel = when (staticThemeStyle) {
                        "white", "bw", "cream" -> "White"
                        "dark", "dark_green" -> "Dark"
                        "dark_blue" -> "Biru Gelap"
                        "dark_contrast" -> "Dark Contrast"
                        else -> "Kustom"
                    }

                    SettingsListItem(
                        title = "Gaya Tema Statis",
                        subtitle = staticLabel,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(staticLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = { showStaticStyleDialog = true }
                    )
                    
                    if (showStaticStyleDialog) {
                        val customPrimary by viewModel.customStaticPrimary.collectAsStateWithLifecycle()
                        val customSecondary by viewModel.customStaticSecondary.collectAsStateWithLifecycle()
                        val customTertiary by viewModel.customStaticTertiary.collectAsStateWithLifecycle()
                        val customText by viewModel.customStaticText.collectAsStateWithLifecycle()
                        val customBg by viewModel.customStaticBackground.collectAsStateWithLifecycle()
                        val customSurface by viewModel.customStaticSurface.collectAsStateWithLifecycle()
                        
                        var pHex by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and customPrimary.toInt())) }
                        var sHex by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and customSecondary.toInt())) }
                        var tHex by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and customTertiary.toInt())) }
                        var txHex by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and customText.toInt())) }
                        var bgHex by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and customBg.toInt())) }
                        var sfHex by remember { mutableStateOf(String.format("#%06X", 0xFFFFFF and customSurface.toInt())) }
                        
                        AlertDialog(
                            onDismissRequest = { showStaticStyleDialog = false },
                            title = { Text("Gaya Tema Statis") },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    val options = listOf(
                                        "white" to "White",
                                        "dark" to "Dark",
                                        "dark_blue" to "Biru Gelap",
                                        "dark_contrast" to "Dark Contrast",
                                        "custom" to "Kustom (Input HEX)"
                                    )
                                    
                                    options.forEach { (style, label) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.setStaticThemeStyle(style) }
                                                .padding(vertical = 12.dp)
                                        ) {
                                            RadioButton(
                                                selected = staticThemeStyle == style,
                                                onClick = { viewModel.setStaticThemeStyle(style) }
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(label, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                    
                                    if (staticThemeStyle == "custom") {
                                        Spacer(Modifier.height(8.dp))
                                        CustomSettingsTextField(value = pHex, onValueChange = { pHex = it }, label = { Text("Primary (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        CustomSettingsTextField(value = sHex, onValueChange = { sHex = it }, label = { Text("Secondary (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        CustomSettingsTextField(value = tHex, onValueChange = { tHex = it }, label = { Text("Accent (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        CustomSettingsTextField(value = txHex, onValueChange = { txHex = it }, label = { Text("Text/Icons (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        CustomSettingsTextField(value = bgHex, onValueChange = { bgHex = it }, label = { Text("Background (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        CustomSettingsTextField(value = sfHex, onValueChange = { sfHex = it }, label = { Text("Surface (HEX)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { 
                                    if (staticThemeStyle == "custom") {
                                        try {
                                            fun parse(hex: String): Long {
                                                var h = hex
                                                if (h.startsWith("#")) h = h.substring(1)
                                                if (h.length == 6) h = "FF$h"
                                                return h.toLong(16)
                                            }
                                            viewModel.setCustomStaticColors(
                                                parse(pHex), parse(sHex), parse(tHex), parse(txHex), parse(bgHex), parse(sfHex)
                                            )
                                        } catch (e: Exception) {
                                            // Invalid, ignore
                                        }
                                    }
                                    showStaticStyleDialog = false 
                                }) {
                                    Text("SIMPAN")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showStaticStyleDialog = false }) {
                                    Text("BATAL")
                                }
                            }
                        )
                    }
                }
            }
            
            if ((showBrightnessMode || showColorMode || showThemeStyle) && showPreview) SettingsDivider()
            
            if (showPreview) {
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                val useDynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
                val colorMode by viewModel.colorMode.collectAsStateWithLifecycle()
                val staticThemeStyle by viewModel.staticThemeStyle.collectAsStateWithLifecycle()
                val dynamicThemeSeed by viewModel.dynamicThemeSeed.collectAsStateWithLifecycle()
                val customPrimary by viewModel.customStaticPrimary.collectAsStateWithLifecycle()
                val customSecondary by viewModel.customStaticSecondary.collectAsStateWithLifecycle()
                val customTertiary by viewModel.customStaticTertiary.collectAsStateWithLifecycle()
                val customText by viewModel.customStaticText.collectAsStateWithLifecycle()
                val customBg by viewModel.customStaticBackground.collectAsStateWithLifecycle()
                val customSurface by viewModel.customStaticSurface.collectAsStateWithLifecycle()
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pratinjau Tema", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    
                    UnstableStudioTheme(
                        themeMode = themeMode, 
                        colorMode = colorMode,
                        staticThemeStyle = staticThemeStyle,
                        dynamicThemeSeed = dynamicThemeSeed,
                        customStaticPrimary = customPrimary,
                        customStaticSecondary = customSecondary,
                        customStaticTertiary = customTertiary,
                        customStaticText = customText,
                        customStaticBackground = customBg,
                        customStaticSurface = customSurface,
                        useDynamicColor = useDynamicColor
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            color = MaterialTheme.colorScheme.background,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Box(Modifier.width(120.dp).height(8.dp).background(MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small))
                                        Spacer(Modifier.height(4.dp))
                                        Box(Modifier.width(80.dp).height(6.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), MaterialTheme.shapes.small))
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(24.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                                    Box(Modifier.size(24.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape))
                                }
                            }
                        }
                    }
                }
            }

            if ((showBrightnessMode || showColorMode || showThemeStyle || showPreview) && showIconColor) SettingsDivider()

            if (showIconColor) {
                val useOriginalIconColors by viewModel.useOriginalIconColors.collectAsStateWithLifecycle()
                SettingsListItem(
                    title = "Warna Ikon Explorer",
                    subtitle = if (useOriginalIconColors) "Gunakan warna asli ikon" else "Ikuti warna tema saat ini",
                    trailingContent = {
                        Switch(
                            checked = useOriginalIconColors,
                            onCheckedChange = { viewModel.setUseOriginalIconColors(it) }
                        )
                    },
                    onClick = { viewModel.setUseOriginalIconColors(!useOriginalIconColors) }
                )
            }
        }
    }
}

@Composable
fun EditorSettings(viewModel: SettingsViewModel, searchQuery: String) {
    val showFontSize = "Font Size".matchesSearch(searchQuery) || "Ukuran teks".matchesSearch(searchQuery)
    val showWordWrap = "Word Wrap".matchesSearch(searchQuery) || "Bungkus baris".matchesSearch(searchQuery)
    val showLineNumbers = "Tampilkan Nomor Baris".matchesSearch(searchQuery) || "angka baris".matchesSearch(searchQuery)

    if (showFontSize || showWordWrap || showLineNumbers) {
        SettingSection("Kustomisasi Editor")
        SettingsCard {
            if (showFontSize) {
                val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
                var showFontSizeDialog by remember { mutableStateOf(false) }
                
                SettingsListItem(
                    title = "Font Size",
                    subtitle = "${fontSize.toInt()} sp",
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${fontSize.toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { showFontSizeDialog = true }
                )
                
                if (showFontSizeDialog) {
                    AlertDialog(
                        onDismissRequest = { showFontSizeDialog = false },
                        title = { Text("Font Size") },
                        text = {
                            Column {
                                Text("${fontSize.toInt()} sp", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.displaySmall)
                                Slider(
                                    value = fontSize,
                                    onValueChange = { viewModel.setFontSize(it) },
                                    valueRange = AppConstants.Editor.MIN_FONT_SIZE..AppConstants.Editor.MAX_FONT_SIZE,
                                    steps = AppConstants.Editor.FONT_SIZE_STEPS
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFontSizeDialog = false }) {
                                Text("TUTUP")
                            }
                        }
                    )
                }
            }
            
            if (showFontSize && (showWordWrap || showLineNumbers)) SettingsDivider()
            
            if (showWordWrap) {
                val wordWrap by viewModel.wordWrap.collectAsStateWithLifecycle()
                SettingsListItem(
                    title = "Word Wrap",
                    subtitle = "Bungkus baris panjang agar muat",
                    trailingContent = {
                        Switch(checked = wordWrap, onCheckedChange = { viewModel.setWordWrap(it) })
                    },
                    onClick = { viewModel.setWordWrap(!wordWrap) }
                )
            }
            
            if (showWordWrap && showLineNumbers) SettingsDivider()
            
            if (showLineNumbers) {
                val showLineNumbersVal by viewModel.showLineNumbers.collectAsStateWithLifecycle()
                SettingsListItem(
                    title = "Tampilkan Nomor Baris",
                    subtitle = "Menampilkan angka baris di kiri",
                    trailingContent = {
                        Switch(checked = showLineNumbersVal, onCheckedChange = { viewModel.setShowLineNumbers(it) })
                    },
                    onClick = { viewModel.setShowLineNumbers(!showLineNumbersVal) }
                )
            }
        }
    }
}

@Composable
fun TerminalSettings(viewModel: SettingsViewModel, searchQuery: String) {
    val showFont = "Font Terminal".matchesSearch(searchQuery) || "JetBrains Mono".matchesSearch(searchQuery)

    if (showFont) {
        SettingSection("Terminal Konfigurasi")
        SettingsCard {
            SettingsListItem(
                title = "Font Terminal",
                subtitle = "Monospace (JetBrains Mono)",
                onClick = {} 
            )
        }
    }
}

@Composable
fun RuntimeSettings(runtimeManager: RuntimeManager, searchQuery: String) {
    val showRuntime = "Lingkungan Internal".matchesSearch(searchQuery) || "Runtime Engine".matchesSearch(searchQuery) || "Linux".matchesSearch(searchQuery)

    if (showRuntime) {
        SettingSection("Lingkungan Internal")
        SettingsCard {
            val status by runtimeManager.status.collectAsStateWithLifecycle()
            val progress by runtimeManager.progress.collectAsStateWithLifecycle()

            Column(modifier = Modifier.padding(16.dp)) {
                Text("Runtime Engine", style = MaterialTheme.typography.titleMedium)
                Text("Linux environment bawaan untuk Node.js & Python.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (status == "READY") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    Text("Status: ", style = MaterialTheme.typography.bodyMedium)
                    Text(status, color = statusColor, fontWeight = FontWeight.Bold)
                }

                if (status == "DOWNLOADING" || status == "EXTRACTING" || status == "PKG_INSTALLING") {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(progress * 100).toInt()}%", modifier = Modifier.align(Alignment.End), style = MaterialTheme.typography.labelSmall)
                }

                Spacer(Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = {
                        if (status == "READY") runtimeManager.reinstall()
                        else runtimeManager.ensureRuntimeReady()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = status == "IDLE" || status.startsWith("ERROR") || status == "READY"
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            status == "READY"          -> "Reinstall Runtime"
                            status.startsWith("ERROR") -> "Coba Lagi"
                            else                       -> "Siapkan Runtime"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AboutSettings(searchQuery: String) {
    val showAbout = "Tentang".matchesSearch(searchQuery) || "Unstable Studio".matchesSearch(searchQuery)

    if (showAbout) {
        SettingSection("Tentang")
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.DeveloperBoard, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Unstable Studio", style = MaterialTheme.typography.headlineSmall)
                Text("Versi 1.0.0-alpha", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Mobile IDE berfitur lengkap dengan dukungan Proot dan Terminal Linux.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
