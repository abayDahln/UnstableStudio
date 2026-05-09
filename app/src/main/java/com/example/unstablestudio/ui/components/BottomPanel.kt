package com.example.unstablestudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unstablestudio.ui.viewmodels.TerminalViewModel
import com.example.unstablestudio.ui.components.LocalKeyboardManager
import com.example.unstablestudio.ui.components.SpecialKey

enum class PanelTab { TERMINAL, OUTPUT, DEBUG, PROBLEMS, PORTS }

@Composable
fun BottomPanel(
    terminalViewModel: TerminalViewModel,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Monospace,
    onClose: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(PanelTab.TERMINAL) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val panelHeight = if (isLandscape) 280.dp else 450.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            PanelTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab.name.lowercase().capitalize(),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                PanelTab.TERMINAL -> TerminalTab(terminalViewModel, fontFamily)
                PanelTab.OUTPUT   -> PlaceholderContent("Output is empty")
                PanelTab.DEBUG    -> PlaceholderContent("Debug Console")
                PanelTab.PROBLEMS -> PlaceholderContent("No problems detected")
                PanelTab.PORTS    -> PlaceholderContent("No active ports")
            }
        }
    }
}

@Composable
fun TerminalTab(viewModel: TerminalViewModel, fontFamily: FontFamily) {
    val output by viewModel.terminalOutput.collectAsStateWithLifecycle()
    val currentDirectory by viewModel.currentDirectory.collectAsStateWithLifecycle()
    val isStarting by viewModel.isStarting.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Auto-scroll to the latest line whenever output grows
    LaunchedEffect(output.size) {
        if (output.isNotEmpty()) {
            listState.animateScrollToItem(output.size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    if (!isStarting) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            items(output) { line ->
                Text(
                    text = line,
                    color = colorScheme.onSurface,
                    fontFamily = fontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Input row — only shown once the shell is ready
            if (!isStarting) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                    ) {
                        val submitCommand: () -> Unit = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendCommand(inputText)
                                inputText = ""
                            }
                        }
                        val keyboardManager = LocalKeyboardManager.current
                        BasicTextField(
                            value = inputText,
                            onValueChange = { newValue ->
                                if (newValue.contains('\n')) {
                                    val command = newValue.replace("\n", "")
                                    if (command.isNotBlank()) {
                                        viewModel.sendCommand(command)
                                    }
                                    inputText = ""
                                } else {
                                    inputText = newValue
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        keyboardManager.show()
                                        keyboardManager.registerInput(
                                            onTextInput = { text -> inputText += text },
                                            onSpecialKey = { key ->
                                                when (key) {
                                                    SpecialKey.ENTER -> submitCommand()
                                                    SpecialKey.BACKSPACE -> if (inputText.isNotEmpty()) inputText = inputText.dropLast(1)
                                                    SpecialKey.TAB -> inputText += "    "
                                                    else -> {}
                                                }
                                            }
                                        )
                                    }
                                },
                            readOnly = true, // Disable system keyboard
                            textStyle = TextStyle(
                                color = colorScheme.onSurface,
                                fontFamily = fontFamily,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            cursorBrush = SolidColor(colorScheme.onSurface),
                            singleLine = false,
                            maxLines = 4,
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${formatPromptDirectory(currentDirectory)} $ ",
                                        color = colorScheme.primary,
                                        fontFamily = fontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    Box(modifier = Modifier.weight(1f)) {
                                        innerTextField()
                                    }
                                }
                            }
                        )

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalIconButton(
                                onClick = { viewModel.sendCtrlC() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("^C", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalIconButton(
                                onClick = {
                                    viewModel.getPreviousCommand()?.let { inputText = it }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "History Up", modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalIconButton(
                                onClick = {
                                    viewModel.getNextCommand()?.let { inputText = it }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "History Down", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Shell initialising overlay — spinner + text at the bottom
        if (isStarting) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Initializing shell…",
                    color = colorScheme.onSurfaceVariant,
                    fontFamily = fontFamily,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatPromptDirectory(path: String): String {
    val normalized = if (path.startsWith("/root")) path.replaceFirst("/root", "~") else path
    if (normalized == "/" || normalized == "~") return normalized

    val hasHomePrefix = normalized.startsWith("~")
    val cleaned = normalized.trim('/').removePrefix("~/")
    val parts = cleaned.split("/").filter { it.isNotBlank() }
    if (parts.isEmpty()) return if (hasHomePrefix) "~" else "/"

    val tail = parts.takeLast(2).joinToString("/")
    val hasMore = parts.size > 2
    return when {
        hasHomePrefix && hasMore -> "~.../$tail"
        hasHomePrefix            -> "~/$tail"
        hasMore                  -> ".../$tail"
        else                     -> "/$tail"
    }
}

@Composable
fun PlaceholderContent(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.capitalize() = this.lowercase().replaceFirstChar { it.uppercase() }
