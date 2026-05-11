package com.example.unstablestudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuBar(
    onFileOpen: () -> Unit,
    onFileSave: () -> Unit,
    onFileSaveAll: () -> Unit,
    onFileExit: () -> Unit,
    onEditUndo: () -> Unit,
    onEditRedo: () -> Unit,
    onEditFind: () -> Unit,
    onEditCut: () -> Unit,
    onEditCopy: () -> Unit,
    onEditPaste: () -> Unit,
    onSelectionSelectAll: () -> Unit,
    onSelectionExpandSelection: () -> Unit,
    onSelectionShrinkSelection: () -> Unit,
    onSelectionCopyLineUp: () -> Unit,
    onSelectionCopyLineDown: () -> Unit,
    onSelectionMoveLineUp: () -> Unit,
    onSelectionMoveLineDown: () -> Unit,
    onViewFontSizeIncrease: () -> Unit,
    onViewFontSizeDecrease: () -> Unit,
    onViewWordWrapToggle: () -> Unit,
    onViewShortcutKeysShow: () -> Unit,
    onViewSettings: () -> Unit,
    onTerminalOpen: () -> Unit,
    onCloseProject: () -> Unit,
    wordWrapEnabled: Boolean
) {
    var activeMenu by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- FILE MENU ---
        MenuItem(
            text = "File",
            expanded = activeMenu == "File",
            onToggle = { activeMenu = if (activeMenu == "File") null else "File" }
        ) {
            MenuActionItem("Open Folder...", "Ctrl+K Ctrl+O") { onFileOpen(); activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Save", "Ctrl+S") { onFileSave(); activeMenu = null }
            MenuActionItem("Save All", "Ctrl+Shift+S") { onFileSaveAll(); activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Close Project") { onCloseProject(); activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Exit") { onFileExit(); activeMenu = null }
        }

        // --- EDIT MENU ---
        MenuItem(
            text = "Edit",
            expanded = activeMenu == "Edit",
            onToggle = { activeMenu = if (activeMenu == "Edit") null else "Edit" }
        ) {
            MenuActionItem("Undo", "Ctrl+Z") { onEditUndo(); activeMenu = null }
            MenuActionItem("Redo", "Ctrl+Y") { onEditRedo(); activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Cut", "Ctrl+X") { onEditCut(); activeMenu = null }
            MenuActionItem("Copy", "Ctrl+C") { onEditCopy(); activeMenu = null }
            MenuActionItem("Paste", "Ctrl+V") { onEditPaste(); activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Find", "Ctrl+F") { onEditFind(); activeMenu = null }
        }

        // --- SELECTION MENU ---
        MenuItem(
            text = "Selection",
            expanded = activeMenu == "Selection",
            onToggle = { activeMenu = if (activeMenu == "Selection") null else "Selection" }
        ) {
            MenuActionItem("Select All", "Ctrl+A") { onSelectionSelectAll(); activeMenu = null }
            MenuActionItem("Expand Selection", "Shift+Alt+Right") { onSelectionExpandSelection(); activeMenu = null }
            MenuActionItem("Shrink Selection", "Shift+Alt+Left") { onSelectionShrinkSelection(); activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Copy Line Up", "Shift+Alt+Up") { onSelectionCopyLineUp(); activeMenu = null }
            MenuActionItem("Copy Line Down", "Shift+Alt+Down") { onSelectionCopyLineDown(); activeMenu = null }
            MenuActionItem("Move Line Up", "Alt+Up") { onSelectionMoveLineUp(); activeMenu = null }
            MenuActionItem("Move Line Down", "Alt+Down") { onSelectionMoveLineDown(); activeMenu = null }
        }

        // --- VIEW MENU ---
        MenuItem(
            text = "View",
            expanded = activeMenu == "View",
            onToggle = { activeMenu = if (activeMenu == "View") null else "View" }
        ) {
            MenuActionItem("Command Palette...", "Ctrl+Shift+P") { activeMenu = null }
            MenuActionItem("Open View...") { activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Appearance") { activeMenu = null }
            MenuActionItem("Editor Layout") { activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Zoom In", "Ctrl+=") { onViewFontSizeIncrease(); activeMenu = null }
            MenuActionItem("Zoom Out", "Ctrl+-") { onViewFontSizeDecrease(); activeMenu = null }
            MenuActionItem("Reset Zoom", "Ctrl+0") { activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Word Wrap", shortcut = "Alt+Z", isChecked = wordWrapEnabled) { onViewWordWrapToggle(); activeMenu = null }
            MenuActionItem("Shortcut Keys") { onViewShortcutKeysShow(); activeMenu = null }
        }

        // --- TERMINAL MENU ---
        MenuItem(
            text = "Terminal",
            expanded = activeMenu == "Terminal",
            onToggle = { activeMenu = if (activeMenu == "Terminal") null else "Terminal" }
        ) {
            MenuActionItem("New Terminal", "Ctrl+Shift+`") { onTerminalOpen(); activeMenu = null }
            MenuActionItem("Split Terminal", "Ctrl+Shift+5") { onTerminalOpen(); activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("Run Build Task...", "Ctrl+Shift+B") { activeMenu = null }
            MenuActionItem("Run Active File") { activeMenu = null }
        }

        // --- HELP MENU ---
        MenuItem(
            text = "Help",
            expanded = activeMenu == "Help",
            onToggle = { activeMenu = if (activeMenu == "Help") null else "Help" }
        ) {
            MenuActionItem("Welcome") { activeMenu = null }
            MenuActionItem("Show All Commands") { activeMenu = null }
            MenuActionItem("Documentation") { activeMenu = null }
            MenuActionItem("Keyboard Shortcuts Reference") { activeMenu = null }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            MenuActionItem("About") { activeMenu = null }
        }

        // --- SETTINGS MENU ---
        MenuItem(
            text = "Settings",
            expanded = activeMenu == "Settings",
            onToggle = { activeMenu = if (activeMenu == "Settings") null else "Settings" }
        ) {
            MenuActionItem("Open Settings", "Ctrl+,") { onViewSettings(); activeMenu = null }
        }
    }
}

@Composable
private fun MenuItem(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box {
        Text(
            text = text,
            modifier = Modifier
                .clickable { onToggle() }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 13.sp
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onToggle,
            modifier = Modifier.width(240.dp).background(MaterialTheme.colorScheme.surface),
            content = content
        )
    }
}

@Composable
private fun MenuActionItem(
    text: String,
    shortcut: String? = null,
    isChecked: Boolean? = null,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isChecked != null) {
                        Icon(
                            imageVector = if (isChecked) Icons.Default.Check else Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 8.dp),
                            tint = if (isChecked) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    }
                    Text(text = text, style = MaterialTheme.typography.bodyMedium)
                }
                if (shortcut != null) {
                    Text(
                        text = shortcut,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    )
}
