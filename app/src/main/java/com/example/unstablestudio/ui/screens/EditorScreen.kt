package com.example.unstablestudio.ui.screens

import com.example.unstablestudio.ui.theme.Roboto

import android.graphics.Typeface
import android.view.KeyEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import android.content.ClipDescription
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.unstablestudio.core.common.AppLogger
import com.example.unstablestudio.core.config.AppConstants
import com.example.unstablestudio.ui.components.CodingKeyboard
import com.example.unstablestudio.ui.components.LocalKeyboardManager
import com.example.unstablestudio.ui.components.SpecialKey
import com.example.unstablestudio.ui.components.TypingAssist
import com.example.unstablestudio.ui.viewmodels.EditorViewModel
import com.example.unstablestudio.domain.model.EditorAction
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SelectionMovement
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import com.example.unstablestudio.ui.components.BottomPanel
import com.example.unstablestudio.ui.components.MenuBar

private const val TAG = "EditorScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    workspaceViewModel: com.example.unstablestudio.ui.viewmodels.WorkspaceViewModel,
    terminalViewModel: com.example.unstablestudio.ui.viewmodels.TerminalViewModel,
    settingsRepository: com.example.unstablestudio.data.repository.SettingsRepository,
    onOpenExplorer: () -> Unit = {},
    onOpenFolder: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onShortcutKeysShow: () -> Unit = {},
    onCloseProject: () -> Unit = {},
    onExitApp: () -> Unit = {}
) {
    val document by viewModel.currentDocument.collectAsStateWithLifecycle()
    val openDocuments by viewModel.openDocuments.collectAsStateWithLifecycle()
    val activeDocumentId by viewModel.activeDocumentId.collectAsStateWithLifecycle()
    val isWordWrapEnabled by viewModel.wordWrap.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val showLineNumbers by settingsRepository.showLineNumbers.collectAsStateWithLifecycle()
    val recentProjects by settingsRepository.recentProjects.collectAsStateWithLifecycle()
    val currentRootUri by workspaceViewModel.rootUri.collectAsStateWithLifecycle()

    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var editorRef by remember { mutableStateOf<CodeEditor?>(null) }
    var isApplyingAction by remember { mutableStateOf(false) }
    
    var showFindReplace by remember { mutableStateOf(false) }
    var isReplaceModeInitial by remember { mutableStateOf(false) }

    // Bottom Panel State
    var showBottomPanel by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Sync Terminal Workspace
    LaunchedEffect(currentRootUri, showBottomPanel) {
        if (showBottomPanel) {
            terminalViewModel.setWorkspaceUri(currentRootUri)
        }
    }

    val keyboardManager = LocalKeyboardManager.current

    LaunchedEffect(activeDocumentId) { 
        keyboardManager.hide()
        keyboardManager.unregisterInput()
    }

    BackHandler(enabled = keyboardManager.isVisible.collectAsState().value) {
        keyboardManager.hide()
        editorRef?.clearFocus()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(Unit) {
        viewModel.editorActions.collect { action ->
            val editor = editorRef ?: return@collect
            isApplyingAction = true
            try {
                when (action) {
                    is EditorAction.Undo -> viewModel.undo()
                    is EditorAction.Redo -> viewModel.redo()
                    is EditorAction.PerformTextChange -> {
                        editor.text.replace(action.startOffset, action.startOffset + action.deleteCount, action.insertText)
                        val safeCursor = action.cursor.coerceIn(0, editor.text.length)
                        editor.cursor.set(editor.text.indexer.getCharLine(safeCursor), editor.text.indexer.getCharColumn(safeCursor))
                    }
                    is EditorAction.Cut -> editor.cutText()
                    is EditorAction.Copy -> editor.copyText()
                    is EditorAction.Paste -> editor.pasteText()
                    is EditorAction.Find -> { isReplaceModeInitial = false; showFindReplace = true }
                    is EditorAction.Replace -> { isReplaceModeInitial = true; showFindReplace = true }
                    is EditorAction.SelectAll -> editor.selectAll()
                    is EditorAction.SelectLine -> { val line = editor.cursor.leftLine; editor.setSelectionRegion(line, 0, line, editor.text.getLineString(line).length) }
                    is EditorAction.CopyLineUp -> { val line = editor.cursor.leftLine; editor.text.insert(line, 0, editor.text.getLineString(line) + "\n") }
                    is EditorAction.CopyLineDown -> { val line = editor.cursor.leftLine; editor.text.insert(line + 1, 0, editor.text.getLineString(line) + "\n") }
                    is EditorAction.MoveLineUp -> { val line = editor.cursor.leftLine; if (line > 0) { val text = editor.text.getLineString(line); editor.text.delete(line, 0, line, text.length + 1); editor.text.insert(line - 1, 0, text + "\n"); editor.cursor.set(line - 1, 0) } }
                    is EditorAction.MoveLineDown -> { val line = editor.cursor.leftLine; if (line < editor.lineCount - 1) { val text = editor.text.getLineString(line); editor.text.delete(line, 0, line, text.length + 1); editor.text.insert(line + 1, 0, text + "\n"); editor.cursor.set(line + 1, 0) } }
                    is EditorAction.ToggleLineComment -> { val line = editor.cursor.leftLine; val text = editor.text.getLineString(line); if (text.trim().startsWith("//")) editor.text.delete(line, text.indexOf("//"), line, text.indexOf("//") + 2) else editor.text.insert(line, 0, "//") }
                    is EditorAction.DeleteLine -> editor.text.delete(editor.cursor.leftLine, 0, editor.cursor.leftLine, editor.text.getLineString(editor.cursor.leftLine).length + 1)
                    is EditorAction.DuplicateLine -> editor.text.insert(editor.cursor.leftLine + 1, 0, editor.text.getLineString(editor.cursor.leftLine) + "\n")
                    else -> {}
                }
            } finally { isApplyingAction = false }
        }
    }

    val customTypeface = remember {
        try { Typeface.createFromAsset(context.assets, AppConstants.Editor.FONT_JETBRAINS_MONO) }
        catch (e: Exception) { try { androidx.core.content.res.ResourcesCompat.getFont(context, com.example.unstablestudio.R.font.jetbrains_mono_regular) } catch (e2: Exception) { Typeface.MONOSPACE } }
    }

    val customFontFamily = remember(customTypeface) {
        androidx.compose.ui.text.font.FontFamily(customTypeface ?: Typeface.MONOSPACE)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Handle manually
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomPanel = true },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.Code, contentDescription = "Open Panel")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Restore Menu Bar
            MenuBar(
                onFileOpen = onOpenExplorer,
                onFileSave = { viewModel.saveFile() },
                onFileSaveAll = { viewModel.saveAllFiles() },
                onFileExit = onExitApp,
                onEditUndo = { viewModel.undo() },
                onEditRedo = { viewModel.redo() },
                onEditFind = { isReplaceModeInitial = false; showFindReplace = true },
                onEditCut = { viewModel.dispatchEditorAction(EditorAction.Cut) },
                onEditCopy = { viewModel.dispatchEditorAction(EditorAction.Copy) },
                onEditPaste = { viewModel.dispatchEditorAction(EditorAction.Paste) },
                onSelectionSelectAll = { viewModel.dispatchEditorAction(EditorAction.SelectAll) },
                onSelectionExpandSelection = {
                    val editor = editorRef ?: return@MenuBar
                    val cursor = editor.cursor
                    val line = cursor.leftLine
                    val col = cursor.leftColumn

                    if (!cursor.isSelected) {
                        val lineText = editor.text.getLineString(line)
                        if (lineText.isEmpty()) return@MenuBar
                        val safeCol = col.coerceIn(0, lineText.length)
                        fun isWordChar(ch: Char): Boolean = ch.isLetterOrDigit() || ch == '_' || ch == '-'

                        var start = safeCol
                        var end = safeCol
                        if (start < lineText.length && !isWordChar(lineText[start])) {
                            // If on whitespace/symbol, try expand to whole line instead
                            editor.setSelectionRegion(line, 0, line, lineText.length)
                            return@MenuBar
                        }
                        while (start > 0 && isWordChar(lineText[start - 1])) start--
                        while (end < lineText.length && isWordChar(lineText[end])) end++
                        editor.setSelectionRegion(line, start, line, end)
                    } else {
                        // Simple next level: whole line
                        val lineText = editor.text.getLineString(line)
                        editor.setSelectionRegion(line, 0, line, lineText.length)
                    }
                },
                onSelectionShrinkSelection = {
                    val editor = editorRef ?: return@MenuBar
                    val cursor = editor.cursor
                    if (!cursor.isSelected) return@MenuBar
                    // Simple behavior: shrink back to cursor (clear selection)
                    editor.setSelectionRegion(cursor.rightLine, cursor.rightColumn, cursor.rightLine, cursor.rightColumn)
                },
                onSelectionCopyLineUp = { viewModel.dispatchEditorAction(EditorAction.CopyLineUp) },
                onSelectionCopyLineDown = { viewModel.dispatchEditorAction(EditorAction.CopyLineDown) },
                onSelectionMoveLineUp = { viewModel.dispatchEditorAction(EditorAction.MoveLineUp) },
                onSelectionMoveLineDown = { viewModel.dispatchEditorAction(EditorAction.MoveLineDown) },
                onViewFontSizeIncrease = { viewModel.setFontSize(fontSize + 2f) },
                onViewFontSizeDecrease = { viewModel.setFontSize((fontSize - 2f).coerceAtLeast(8f)) },
                onViewWordWrapToggle = { viewModel.setWordWrap(!isWordWrapEnabled) },
                onViewShortcutKeysShow = onShortcutKeysShow,
                onViewSettings = onOpenSettings,
                onTerminalOpen = { showBottomPanel = true },
                onCloseProject = onCloseProject,
                wordWrapEnabled = isWordWrapEnabled
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (openDocuments.isNotEmpty()) {
                        val selectedTabIndex = remember(openDocuments, activeDocumentId) {
                            val index = openDocuments.indexOfFirst { it.id == activeDocumentId }
                            when {
                                openDocuments.isEmpty() -> 0
                                index < 0 -> 0
                                index >= openDocuments.size -> openDocuments.size - 1
                                else -> index
                            }
                        }

                        ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 0.dp, modifier = Modifier.fillMaxWidth(), divider = {}) {
                            openDocuments.forEachIndexed { index, doc ->
                                key(doc.id) {
                                    Tab(selected = index == selectedTabIndex, onClick = { viewModel.setActiveFile(doc.id) }, text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = buildString {
                                                    append(doc.title)
                                                    if (doc.isMissingFromWorkspace) append(" [missing]")
                                                    if (doc.isModified) append(" *")
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (doc.isMissingFromWorkspace) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    LocalContentColor.current
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(onClick = { viewModel.closeFile(doc.id) }, modifier = Modifier.size(18.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    })
                                }
                            }
                        }

                        document?.let { doc ->
                            key(doc.id) {
                                Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .dragAndDropTarget(
                                                shouldStartDragAndDrop = { startEvent ->
                                                    // Intercept sidebar drag payloads (and other text drops) to prevent
                                                    // the system from pasting paths into the editor.
                                                    startEvent.mimeTypes().contains("application/x-unstablestudio-node-uri") ||
                                                        startEvent.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                                                        startEvent.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_URILIST)
                                                },
                                                target = object : DragAndDropTarget {
                                                    override fun onDrop(event: DragAndDropEvent): Boolean {
                                                        // Consume the event to prevent the system from pasting the URI as text
                                                        // into the underlying CodeEditor view.
                                                        return true
                                                    }
                                                }
                                            )
                                            .onPreviewKeyEvent { event ->
                                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                            val isCtrl = event.isCtrlPressed; val isShift = event.isShiftPressed; val key = event.key
                                            when {
                                                isCtrl && key == Key.Z -> { if (isShift) viewModel.redo() else viewModel.undo(); true }
                                                isCtrl && key == Key.Y -> { viewModel.redo(); true }
                                                isCtrl && key == Key.S -> { editorRef?.let { e -> viewModel.updateContent(doc.id, e.text.toString()); viewModel.saveFile() }; true }
                                                isCtrl && key == Key.A -> { editorRef?.selectAll(); true }
                                                isCtrl && key == Key.C -> { editorRef?.copyText(); true }
                                                isCtrl && key == Key.V -> { editorRef?.pasteText(); true }
                                                isCtrl && key == Key.X -> { editorRef?.cutText(); true }
                                                isCtrl && key == Key.W -> { viewModel.closeFile(doc.id); true }
                                                isCtrl && key == Key.J -> { showBottomPanel = !showBottomPanel; true }
                                                key == Key.Escape -> { keyboardManager.hide(); editorRef?.clearFocus(); focusManager.clearFocus(); keyboardController?.hide(); true }


                                                else -> false
                                            }
                                        },
                                        factory = { ctx ->
                                            CodeEditor(ctx).apply {
                                                editorRef = this; typefaceText = customTypeface; typefaceLineNumber = customTypeface; isWordwrap = false; setTextSize(fontSize)
                                                isLineNumberEnabled = showLineNumbers
                                                // Prevent any external/internal drag payload (e.g. file URI from sidebar)
                                                // from being inserted into the editor text by the Android View system.
                                                setOnDragListener { _, _ -> true }
                                                val tabDist = 24 * resources.displayMetrics.density
                                                setLineNumberMarginLeft(tabDist); setDividerMargin(tabDist, tabDist); setDividerWidth(1 * resources.displayMetrics.density)
                                                setText(doc.content); text.isUndoEnabled = false
                                                val scopeName = when (doc.languageId) { AppConstants.LanguageIds.KOTLIN -> AppConstants.TextMateScopes.KOTLIN; AppConstants.LanguageIds.JAVA -> AppConstants.TextMateScopes.JAVA; AppConstants.LanguageIds.JSON -> AppConstants.TextMateScopes.JSON; AppConstants.LanguageIds.XML -> AppConstants.TextMateScopes.XML; else -> null }
                                                if (scopeName != null) com.example.unstablestudio.editor_engine.syntax.TextMateConfig.setupLanguage(this, scopeName)
                                                isFocusable = true
                                                isFocusableInTouchMode = true
                                                setSoftKeyboardEnabled(false)
                                                
                                                setOnFocusChangeListener { _, hasFocus: Boolean -> 
                                                    if (hasFocus) {
                                                        keyboardManager.show()
                                                        keyboardManager.registerInput(
                                                            onTextInput = { text: String ->
                                                                if (text.startsWith("CTRL+")) {
                                                                    val keyChar = text.removePrefix("CTRL+")
                                                                    when (keyChar.lowercase()) {
                                                                        "s" -> { viewModel.updateContent(doc.id, this.text.toString()); viewModel.saveFile() }
                                                                        "a" -> this.selectAll(); "w" -> viewModel.closeFile(doc.id); "z" -> viewModel.undo(); "y" -> viewModel.redo(); "c" -> this.copyText(); "v" -> this.pasteText(); "x" -> this.cutText()
                                                                    }
                                                                } else {
                                                                    if (!TypingAssist.handleKeyPress(this, text)) {
                                                                        this.text.insert(this.cursor.leftLine, this.cursor.leftColumn, text)
                                                                    }
                                                                }
                                                            },
                                                            onSpecialKey = { specialKey: SpecialKey ->
                                                                when (specialKey) {
                                                                    SpecialKey.ENTER -> if (!TypingAssist.handleEnter(this)) this.text.insert(this.cursor.leftLine, this.cursor.leftColumn, "\n")
                                                                    SpecialKey.BACKSPACE -> {
                                                                        val cursor = this.cursor
                                                                        if (cursor.isSelected) this.text.delete(cursor.leftLine, cursor.leftColumn, cursor.rightLine, cursor.rightColumn)
                                                                        else { val line = cursor.leftLine; val col = cursor.leftColumn; if (col > 0) this.text.delete(line, col - 1, line, col) else if (line > 0) this.text.delete(line - 1, this.text.getLineString(line - 1).length, line, 0) }
                                                                    }
                                                                    SpecialKey.DELETE -> {
                                                                        val cursor = this.cursor
                                                                        if (cursor.isSelected) {
                                                                            this.text.delete(cursor.leftLine, cursor.leftColumn, cursor.rightLine, cursor.rightColumn)
                                                                        } else {
                                                                            val line = cursor.leftLine
                                                                            val col = cursor.leftColumn
                                                                            val lineLength = this.text.getLineString(line).length
                                                                            if (col < lineLength) {
                                                                                this.text.delete(line, col, line, col + 1)
                                                                            } else if (line < this.lineCount - 1) {
                                                                                this.text.delete(line, col, line + 1, 0)
                                                                            }
                                                                        }
                                                                    }
                                                                    SpecialKey.TAB -> this.text.insert(this.cursor.leftLine, this.cursor.leftColumn, "    ")
                                                                    SpecialKey.ESCAPE -> { keyboardManager.hide(); this.clearFocus(); focusManager.clearFocus(); keyboardController?.hide() }
                                                                    SpecialKey.ARROW_UP -> this.moveSelection(SelectionMovement.UP); SpecialKey.ARROW_DOWN -> this.moveSelection(SelectionMovement.DOWN); SpecialKey.ARROW_LEFT -> this.moveSelection(SelectionMovement.LEFT); SpecialKey.ARROW_RIGHT -> this.moveSelection(SelectionMovement.RIGHT); SpecialKey.EXPLORER -> onOpenExplorer()
                                                                    else -> {}
                                                                }
                                                            }
                                                        )
                                                    } else {
                                                        keyboardManager.hide()
                                                        keyboardManager.unregisterInput()
                                                    }
                                                }
                                                setOnClickListener {
                                                    requestFocus()
                                                    keyboardManager.show()
                                                }
                                                setOnKeyListener { _, keyCode, event ->
                                                    if (event.action != android.view.KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                                                    if (!event.isCtrlPressed && !event.isAltPressed) {
                                                        val char = event.unicodeChar.toChar()
                                                        if (char != '\u0000' && char.code > 31 && TypingAssist.handleKeyPress(this, char.toString())) return@setOnKeyListener true
                                                    }
                                                    false
                                                }
                                                text.addContentListener(object : ContentListener {
                                                    override fun beforeReplace(content: Content) {}
                                                    override fun afterInsert(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, insertedContent: CharSequence) {
                                                        if (isApplyingAction) return
                                                        val startOffset = content.getCharIndex(startLine, startColumn)
                                                        viewModel.onTextChange(doc.id, startOffset, "", insertedContent.toString(), startOffset, startOffset + insertedContent.length)
                                                        viewModel.markAsModified(doc.id, content.toString())
                                                    }
                                                    override fun afterDelete(content: Content, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, deletedContent: CharSequence) {
                                                        if (isApplyingAction) return
                                                        val startOffset = content.getCharIndex(startLine, startColumn)
                                                        viewModel.onTextChange(doc.id, startOffset, deletedContent.toString(), "", startOffset + deletedContent.length, startOffset)
                                                        viewModel.markAsModified(doc.id, content.toString())
                                                    }
                                                })
                                            }
                                        },
                                        update = { editor ->
                                            if (editor.isWordwrap != isWordWrapEnabled) editor.isWordwrap = isWordWrapEnabled
                                            val currentDensity = editor.resources.displayMetrics.density
                                            if (editor.textSizePx != fontSize * currentDensity) editor.setTextSize(fontSize)
                                            editor.colorScheme.apply {
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND, colorScheme.surface.toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.TEXT_NORMAL, colorScheme.onSurface.toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_BACKGROUND, colorScheme.surface.toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER, colorScheme.onSurfaceVariant.copy(alpha = 0.3f).toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_PANEL, colorScheme.outlineVariant.copy(alpha = 0.1f).toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_NUMBER_PANEL_TEXT, colorScheme.onSurface.toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.LINE_DIVIDER, colorScheme.outlineVariant.copy(alpha = 0.3f).toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.CURRENT_LINE, colorScheme.surfaceVariant.copy(alpha = 0.3f).toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTED_TEXT_BACKGROUND, colorScheme.primaryContainer.copy(alpha = 0.7f).toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_HANDLE, colorScheme.primary.toArgb())
                                                setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.SELECTION_INSERT, colorScheme.primary.toArgb())
                                            }
                                            editor.invalidate()
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        val isProjectOpen = currentRootUri != null
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 32.dp)
                            ) {
                                // Stylized Code Icon < >
                                Row(
                                    modifier = Modifier.padding(bottom = 32.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }

                                Text(
                                    text = if (isProjectOpen) "Unstable Studio" else "Welcome to Unstable Studio",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontFamily = Roboto,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = if (isProjectOpen) "Slide from left to open explorer or use shortcuts" 
                                           else "Open a folder or choose from recent projects to start coding",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = Roboto,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                )

                                if (!isProjectOpen) {
                                    Spacer(modifier = Modifier.height(48.dp))
                                    
                                    Button(
                                        onClick = onOpenFolder,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = RoundedCornerShape(28.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Open Folder", fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = Roboto)
                                    }

                                    if (recentProjects.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(56.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Recent Projects",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = Roboto,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            TextButton(onClick = { settingsRepository.clearRecentProjects() }) {
                                                Text("Clear", fontWeight = FontWeight.Bold, fontFamily = Roboto)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            recentProjects.take(5).forEach { projectUri ->
                                                val projectName = try {
                                                    val uri = android.net.Uri.parse(projectUri)
                                                    val path = uri.path ?: projectUri
                                                    path.substringAfterLast(':').substringAfterLast('/')
                                                } catch (e: Exception) {
                                                    projectUri
                                                }
                                                
                                                Surface(
                                                    onClick = { workspaceViewModel.openWorkspace(projectUri) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(MaterialTheme.colorScheme.surface),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.History,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(24.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = projectName,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontFamily = Roboto,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = projectUri,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontFamily = Roboto,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(48.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(20.dp)
                                    ) {
                                        ShortcutHint("Slide from left", "Open Explorer")
                                        ShortcutHint("Ctrl + O", "Open Folder")
                                        ShortcutHint("Ctrl + S", "Save File")
                                        ShortcutHint("Ctrl + W", "Close Tab")
                                        ShortcutHint("Ctrl + J", "Toggle Bottom Panel")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFindReplace) {
        FindReplaceDialog(editor = editorRef, initialIsReplace = isReplaceModeInitial, onDismiss = { showFindReplace = false })
    }

    if (showBottomPanel) {
        ModalBottomSheet(
            onDismissRequest = { showBottomPanel = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            BottomPanel(
                terminalViewModel = terminalViewModel,
                fontFamily = customFontFamily,
                onClose = { showBottomPanel = false }
            )
        }
    }
}

private fun CodeEditor.disableSelectionActionWindow() {
    runCatching {
        val componentClass = Class.forName("io.github.rosemoe.sora.widget.component.EditorTextActionWindow")
        val getComponentMethod = javaClass.methods.firstOrNull { method ->
            method.name == "getComponent" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == Class::class.java
        } ?: return

        val component = getComponentMethod.invoke(this, componentClass) ?: return
        val setEnabledMethod = component.javaClass.methods.firstOrNull { method ->
            method.name == "setEnabled" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0] == Boolean::class.javaPrimitiveType
        } ?: return

        setEnabledMethod.invoke(component, false)
    }
}

@Composable
private fun ShortcutHint(keys: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Text(text = keys, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CustomEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    val keyboardManager = LocalKeyboardManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.onFocusChanged {
            if (it.isFocused) {
                keyboardManager.show()
                keyboardManager.registerInput(
                    onTextInput = { text: String -> onValueChange(value + text) },
                    onSpecialKey = { key: SpecialKey ->
                        if (key == SpecialKey.BACKSPACE && value.isNotEmpty()) {
                            onValueChange(value.dropLast(1))
                        }
                    }
                )
            }
        },
        singleLine = singleLine,
        readOnly = true // Disable system keyboard
    )
}

@Composable
fun FindReplaceDialog(
    editor: CodeEditor?,
    initialIsReplace: Boolean,
    onDismiss: () -> Unit
) {
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var isReplaceMode by remember { mutableStateOf(initialIsReplace) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isReplaceMode) "Find & Replace" else "Find") },
        text = {
            Column {
                CustomEditorTextField(value = findText, onValueChange = { findText = it }, label = { Text("Find") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (isReplaceMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomEditorTextField(value = replaceText, onValueChange = { replaceText = it }, label = { Text("Replace with") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    val searcher = editor?.searcher
                    if (searcher != null) {
                        searcher.search(findText, SearchOptions(SearchOptions.TYPE_NORMAL, true))
                        searcher.gotoNext()
                    }
                }) { Text("Find Next") }
                if (isReplaceMode) {
                    TextButton(onClick = { editor?.searcher?.replaceCurrentMatch(replaceText) }) { Text("Replace") }
                    TextButton(onClick = { editor?.searcher?.replaceAll(replaceText) }) { Text("Replace All") }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
