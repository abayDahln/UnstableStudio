package com.example.unstablestudio.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.unstablestudio.core.config.AppConstants
import com.example.unstablestudio.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Terminal
import com.example.unstablestudio.core.runtime.RuntimeManager

@Composable
fun SettingsDialog(
    viewModel: SettingsViewModel,
    runtimeManager: RuntimeManager,
    onDismiss: () -> Unit
) {
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val wordWrap by viewModel.wordWrap.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val runtimeStatus by runtimeManager.status.collectAsStateWithLifecycle()
    val runtimeProgress by runtimeManager.progress.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Font Size
                Text(text = "Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = fontSize,
                    onValueChange = { viewModel.setFontSize(it) },
                    valueRange = AppConstants.Editor.MIN_FONT_SIZE..AppConstants.Editor.MAX_FONT_SIZE,
                    steps = AppConstants.Editor.FONT_SIZE_STEPS,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Word Wrap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Word Wrap", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = { viewModel.setWordWrap(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Theme Mode
                Text(text = "Color Theme", style = MaterialTheme.typography.titleSmall)
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                val themeOptions = listOf("light" to "Light", "dark" to "Dark")
                
                Column(Modifier.selectableGroup()) {
                    themeOptions.forEach { (mode, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (mode == themeMode),
                                    onClick = { viewModel.setThemeMode(mode) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (mode == themeMode), onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Internal Runtime Section
                Text(
                    text = "Internal Environment",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bundled runtime for Node.js, Python, and Git",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Status: $runtimeStatus",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (runtimeStatus == "DOWNLOADING" || runtimeStatus == "EXTRACTING" || runtimeStatus == "PKG_INSTALLING") {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { runtimeProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    if (runtimeStatus == "READY") runtimeManager.reinstall()
                                    else runtimeManager.ensureRuntimeReady()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = runtimeStatus == "IDLE" || runtimeStatus.startsWith("ERROR") || runtimeStatus == "READY"
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    runtimeStatus == "READY"           -> "Reinstall Runtime"
                                    runtimeStatus.startsWith("ERROR")  -> "Retry Setup"
                                    else                               -> "Set Up Runtime"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
