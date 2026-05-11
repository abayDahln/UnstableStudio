package com.example.unstablestudio.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

data class Shortcut(val category: String, val shortcut: String, val description: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutKeysScreen(onDismiss: () -> Unit) {
    val shortcuts = listOf(
        Shortcut("File", "Ctrl+S", "Save"),
        Shortcut("File", "Ctrl+Shift+S", "Save All"),
        Shortcut("Edit", "Ctrl+Z", "Undo"),
        Shortcut("Edit", "Ctrl+Y", "Redo"),
        Shortcut("Edit", "Ctrl+X", "Cut"),
        Shortcut("Edit", "Ctrl+C", "Copy"),
        Shortcut("Edit", "Ctrl+V", "Paste"),
        Shortcut("Edit", "Ctrl+F", "Find"),
        Shortcut("Selection", "Ctrl+A", "Select All"),
        Shortcut("Selection", "Alt+Shift+Up", "Copy Line Up"),
        Shortcut("Selection", "Alt+Shift+Down", "Copy Line Down"),
        Shortcut("Selection", "Alt+Up", "Move Line Up"),
        Shortcut("Selection", "Alt+Down", "Move Line Down"),
        Shortcut("Navigation", "Ctrl+G", "Go to Line"),
        Shortcut("Navigation", "Ctrl+Home", "Go to Beginning"),
        Shortcut("Navigation", "Ctrl+End", "Go to End"),
        Shortcut("View", "Ctrl+B", "Toggle Sidebar"),
        Shortcut("View", "Ctrl+=", "Zoom In"),
        Shortcut("View", "Ctrl+-", "Zoom Out")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keyboard Shortcuts") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            val grouped = shortcuts.groupBy { it.category }
            grouped.forEach { (category, list) ->
                item {
                    Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(list) { shortcut ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(shortcut.shortcut, modifier = Modifier.width(120.dp), fontFamily = FontFamily.Monospace)
                        Text(shortcut.description)
                    }
                }
            }
        }
    }
}
