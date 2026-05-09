package com.example.unstablestudio.ui.screens

import android.content.*
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unstablestudio.core.runtime.RuntimeManager
import com.example.unstablestudio.domain.model.FileNode
import com.example.unstablestudio.ui.components.LocalKeyboardManager
import com.example.unstablestudio.ui.components.SpecialKey
import com.example.unstablestudio.ui.viewmodels.FlatFileNode

import com.example.unstablestudio.ui.viewmodels.WorkspaceViewModel
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    runtimeManager: RuntimeManager,
    activeDocumentId: String?,
    onOpenFolder: () -> Unit,
    onSettingsClick: () -> Unit,
    onFileSelected: (String) -> Unit
) {
    val fileTree by viewModel.fileTree.collectAsStateWithLifecycle()
    val flatFileTree by viewModel.flatFileTree.collectAsStateWithLifecycle()
    val rootUri by viewModel.rootUri.collectAsStateWithLifecycle()
    val creatingNode by viewModel.creatingNode.collectAsStateWithLifecycle()
    val focusedNodeUri by viewModel.focusedNodeUri.collectAsStateWithLifecycle()
    val isDraggingGlobal by viewModel.isDragging.collectAsStateWithLifecycle()
    val isRootHoveredGlobal by viewModel.isRootHovered.collectAsStateWithLifecycle()
    val useOriginalColors by viewModel.settingsRepository.useOriginalIconColors.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val availableIconNames = remember(context) {
        context.assets.list("icons")
            ?.asSequence()
            ?.filter { it.endsWith(".svg", ignoreCase = true) }
            ?.map { it.removeSuffix(".svg").lowercase(Locale.ROOT) }
            ?.toSet()
            ?: emptySet()
    }

    var contextMenuNodeUri by remember { mutableStateOf<String?>(null) }
    var actionNodeForDialog by remember { mutableStateOf<FileNode?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var localFileTree by remember { mutableStateOf<List<FlatFileNode>>(emptyList()) }
    var draggingNodeUri by remember { mutableStateOf<String?>(null) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(flatFileTree, isDraggingGlobal) {
        if (draggingNodeUri == null && !isDraggingGlobal) {
            localFileTree = flatFileTree
        }
    }

    val onReorder = { draggedUri: String, targetUri: String ->
        val fromIndex = localFileTree.indexOfFirst { it.node.uri == draggedUri }
        val toIndex = localFileTree.indexOfFirst { it.node.uri == targetUri }
        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
            val newList = localFileTree.toMutableList()
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            localFileTree = newList
        }
    }

    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer

    val rootDndTarget = remember(fileTree, rootUri) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                viewModel.setIsRootHovered(false)
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData.itemCount > 0) {
                    val draggedUri = clipData.getItemAt(0).text.toString()
                    rootUri?.let { uri ->
                        val draggedNode = findNodeByUri(fileTree, draggedUri)
                        if (draggedNode != null) {
                            viewModel.moveFile(draggedNode, uri)
                            draggingNodeUri = null
                            viewModel.setIsDragging(false)
                            return true
                        }
                    }
                }
                viewModel.setIsDragging(false)
                return false
            }

            override fun onEntered(event: DragAndDropEvent) {
                viewModel.setIsRootHovered(true)
            }

            override fun onExited(event: DragAndDropEvent) {
                viewModel.setIsRootHovered(false)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(
                if (isRootHoveredGlobal) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.setFocus(null)
                contextMenuNodeUri = null
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = rootDndTarget
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    },
                    target = rootDndTarget
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORER",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            Row {
                IconButton(
                    onClick = { viewModel.startCreating(null, false) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "New File", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { viewModel.startCreating(null, true) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = { 
                        viewModel.refreshWorkspace() 
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Explorer", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = { 
                        viewModel.collapseAllFolders() 
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.UnfoldLess, contentDescription = "Collapse Folders", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (rootUri == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No folder opened",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Open a folder to start coding",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FilledTonalButton(onClick = onOpenFolder) {
                        Text("Open Folder")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                            event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                        },
                        target = rootDndTarget
                    ),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (creatingNode != null && creatingNode!!.parentUri == rootUri) {
                    item(key = "inline-root-creator") {
                        InlineCreatorItem(
                            isDirectory = creatingNode!!.isDirectory,
                            level = 0,
                            useOriginalColors = useOriginalColors,
                            availableIconNames = availableIconNames,
                            onFinish = { viewModel.finishCreating(it) },
                            onCancel = { viewModel.cancelCreating() }
                        )
                    }
                }

                items(localFileTree, key = { it.node.uri }) { flatNode ->
                    Column {
                        FileNodeItem(
                            node = flatNode.node,
                            level = flatNode.level,
                            isActive = activeDocumentId == flatNode.node.uri,
                            draggingNodeUri = draggingNodeUri,
                            focusedNodeUri = focusedNodeUri,
                            textMeasurer = textMeasurer,
                            useOriginalColors = useOriginalColors,
                            availableIconNames = availableIconNames,
                            onNodeClick = { clickedNode ->
                                contextMenuNodeUri = null
                                viewModel.setFocus(clickedNode.uri)
                                if (clickedNode.isDirectory) {
                                    viewModel.toggleFolder(clickedNode)
                                } else {
                                    onFileSelected(clickedNode.uri)
                                }
                            },
                            isContextMenuExpanded = contextMenuNodeUri == flatNode.node.uri,
                            onContextMenuRequest = { clickedNode ->
                                viewModel.setFocus(clickedNode.uri)
                                contextMenuNodeUri = clickedNode.uri
                            },
                            onContextMenuDismiss = {
                                if (contextMenuNodeUri == flatNode.node.uri) {
                                    contextMenuNodeUri = null
                                }
                            },
                            onCreateFileInDirectory = { node ->
                                viewModel.startCreating(node.uri, false)
                                contextMenuNodeUri = null
                            },
                            onCreateFolderInDirectory = { node ->
                                viewModel.startCreating(node.uri, true)
                                contextMenuNodeUri = null
                            },
                            onCopyName = { node ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("File Name", node.name)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Name copied", Toast.LENGTH_SHORT).show()
                                contextMenuNodeUri = null
                            },
                            onCopyPath = { node ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("File Path", node.uri)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                                contextMenuNodeUri = null
                            },
                            onOpenInFolder = { node ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        val folderUri = if (node.isDirectory) Uri.parse(node.uri) else Uri.parse(node.uri).let {
                                            val uriStr = it.toString()
                                            Uri.parse(uriStr.substringBeforeLast("/"))
                                        }
                                        setDataAndType(folderUri, "resource/folder")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open folder directly", Toast.LENGTH_SHORT).show()
                                }
                                contextMenuNodeUri = null
                            },
                            onDelete = { nodeToDelete ->
                                actionNodeForDialog = nodeToDelete
                                showDeleteConfirmDialog = true
                                contextMenuNodeUri = null
                            },
                            onRename = { nodeToRename ->
                                actionNodeForDialog = nodeToRename
                                renameValue = nodeToRename.name
                                showRenameDialog = true
                                contextMenuNodeUri = null
                            },
                            onMoveFile = { draggedUri, targetUri ->
                                val draggedNode = findNodeByUri(fileTree, draggedUri)
                                val targetNode = findNodeByUri(fileTree, targetUri)
                                if (draggedNode != null && targetNode != null) {
                                    if (targetNode.isDirectory) {
                                        viewModel.moveFile(draggedNode, targetUri)
                                    } else {
                                        viewModel.moveFileToSibling(draggedNode, targetUri)
                                    }
                                }
                            },
                            onDragStart = { uri -> 
                                draggingNodeUri = uri 
                                viewModel.setIsDragging(true)
                            },
                            onDragOver = { targetUri ->
                                draggingNodeUri?.let { draggedUri ->
                                    if (draggedUri != targetUri) {
                                        onReorder(draggedUri, targetUri)
                                    }
                                }
                            },
                            onDragEnd = { success ->
                                if (!success) {
                                    localFileTree = flatFileTree
                                }
                                draggingNodeUri = null
                                viewModel.setIsDragging(false)
                                viewModel.setIsRootHovered(false)
                            }
                        )
                        
                        if (creatingNode != null && creatingNode!!.parentUri == flatNode.node.uri && flatNode.node.isExpanded) {
                            InlineCreatorItem(
                                isDirectory = creatingNode!!.isDirectory,
                                level = flatNode.level + 1,
                                useOriginalColors = useOriginalColors,
                                availableIconNames = availableIconNames,
                                onFinish = { viewModel.finishCreating(it) },
                                onCancel = { viewModel.cancelCreating() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                actionNodeForDialog = null
            },
            title = { Text("Rename") },
            text = {
                val keyboardManager = com.example.unstablestudio.ui.components.LocalKeyboardManager.current
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("New Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                keyboardManager.show()
                                keyboardManager.registerInput(
                                    onTextInput = { text -> renameValue += text },
                                    onSpecialKey = { key ->
                                        if (key == com.example.unstablestudio.ui.components.SpecialKey.BACKSPACE && renameValue.isNotEmpty()) {
                                            renameValue = renameValue.dropLast(1)
                                        }
                                    }
                                )
                            }
                        },
                    singleLine = true,
                    readOnly = true // Disable system keyboard
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameValue.isNotBlank()) {
                        actionNodeForDialog?.let { viewModel.renameFile(it, renameValue) }
                        showRenameDialog = false
                        actionNodeForDialog = null
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    actionNodeForDialog = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                actionNodeForDialog = null
            },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete '${actionNodeForDialog?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    actionNodeForDialog?.let { viewModel.deleteFile(it) }
                    showDeleteConfirmDialog = false
                    actionNodeForDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    actionNodeForDialog = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InlineCreatorItem(
    isDirectory: Boolean,
    level: Int,
    useOriginalColors: Boolean,
    availableIconNames: Set<String>,
    onFinish: (String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardManager = com.example.unstablestudio.ui.components.LocalKeyboardManager.current

    // Using rememberUpdatedState to ensure lambdas always see current 'name'
    val currentName = rememberUpdatedState(name)
    val currentOnFinish = rememberUpdatedState(onFinish)
    val currentOnCancel = rememberUpdatedState(onCancel)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(level) {
            Box(modifier = Modifier.width(16.dp).fillMaxHeight()) {
                VerticalDivider(
                    modifier = Modifier.align(Alignment.Center).fillMaxHeight().width(0.5.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(Modifier.width(24.dp))

        FileIcon(
            fileName = if (isDirectory) "folder" else "document",
            isDirectory = isDirectory,
            isExpanded = false,
            useOriginalColors = useOriginalColors,
            availableIconNames = availableIconNames,
            modifier = Modifier.size(16.dp),
            tint = if (isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(8.dp))

        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            color = Color.Transparent,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        ) {
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                keyboardManager.show()
                                keyboardManager.registerInput(
                                    onTextInput = { text -> 
                                        // Update state directly
                                        name += text 
                                    },
                                    onSpecialKey = { key ->
                                        when (key) {
                                            com.example.unstablestudio.ui.components.SpecialKey.BACKSPACE -> {
                                                if (name.isNotEmpty()) name = name.dropLast(1)
                                            }
                                            com.example.unstablestudio.ui.components.SpecialKey.ENTER -> {
                                                currentOnFinish.value(name)
                                                keyboardManager.hide()
                                            }
                                            com.example.unstablestudio.ui.components.SpecialKey.ESCAPE -> {
                                                currentOnCancel.value()
                                                keyboardManager.hide()
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.Enter -> { onFinish(name); keyboardManager.hide(); true }
                                    Key.Escape -> { onCancel(); keyboardManager.hide(); true }
                                    else -> false
                                }
                            } else false
                        },
                    readOnly = true, // Disable system keyboard
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun FileIcon(
    fileName: String,
    isDirectory: Boolean,
    isExpanded: Boolean,
    useOriginalColors: Boolean,
    availableIconNames: Set<String>,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val assetName = remember(fileName, isDirectory, isExpanded, availableIconNames) {
        resolveExplorerIconName(
            fileName = fileName,
            isDirectory = isDirectory,
            isExpanded = isExpanded,
            availableIconNames = availableIconNames
        )
    }
    val resolvedTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint

    if (assetName != null) {
        val request = remember(assetName, context) {
            ImageRequest.Builder(context)
                .data("file:///android_asset/icons/$assetName.svg")
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(false)
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            modifier = modifier,
            colorFilter = if (useOriginalColors) {
                null
            } else {
                androidx.compose.ui.graphics.ColorFilter.tint(
                    resolvedTint
                )
            },
            loading = {
                Icon(
                    imageVector = if (isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = modifier,
                    tint = resolvedTint
                )
            },
            error = {
                Icon(
                    imageVector = if (isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = modifier,
                    tint = resolvedTint
                )
            }
        )
    } else {
        Icon(
            imageVector = if (isDirectory) {
                if (isExpanded) Icons.Default.Folder else Icons.Outlined.Folder
            } else {
                Icons.AutoMirrored.Filled.InsertDriveFile
            },
            contentDescription = null,
            modifier = modifier,
            tint = resolvedTint
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileNodeItem(
    node: FileNode,
    level: Int,
    isActive: Boolean = false,
    draggingNodeUri: String?,
    focusedNodeUri: String?,
    textMeasurer: TextMeasurer,
    useOriginalColors: Boolean,
    availableIconNames: Set<String>,
    onNodeClick: (FileNode) -> Unit,
    isContextMenuExpanded: Boolean,
    onContextMenuRequest: (FileNode) -> Unit,
    onContextMenuDismiss: () -> Unit,
    onCreateFileInDirectory: (FileNode) -> Unit,
    onCreateFolderInDirectory: (FileNode) -> Unit,
    onCopyName: (FileNode) -> Unit,
    onCopyPath: (FileNode) -> Unit,
    onOpenInFolder: (FileNode) -> Unit,
    onDelete: (FileNode) -> Unit,
    onRename: (FileNode) -> Unit,
    onMoveFile: (String, String) -> Unit,
    onDragStart: (String) -> Unit,
    onDragOver: (String) -> Unit,
    onDragEnd: (Boolean) -> Unit,
    onDragOverFolder: (String) -> Unit = {}
) {
    val isFocused = remember(node.uri, focusedNodeUri) { node.uri == focusedNodeUri }
    var isHovered by remember { mutableStateOf(false) }

    val dndTarget = remember(node) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isHovered = false
                val clipData = event.toAndroidDragEvent().clipData
                if (clipData.itemCount > 0) {
                    val draggedUri = clipData.getItemAt(0).text.toString()
                    if (draggedUri != node.uri) {
                        onMoveFile(draggedUri, node.uri)
                        onDragEnd(true)
                        return true
                    }
                }
                onDragEnd(false)
                return false
            }

            override fun onEntered(event: DragAndDropEvent) {
                if (node.isDirectory) isHovered = true
                onDragOver(node.uri)
                if (node.isDirectory) onDragOverFolder(node.uri)
            }

            override fun onExited(event: DragAndDropEvent) {
                isHovered = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isHovered = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp)) 
                .background(
                    when {
                        isActive -> MaterialTheme.colorScheme.secondaryContainer
                        isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = if (isFocused && node.isDirectory) 1.dp else 0.dp,
                    color = if (isFocused && node.isDirectory) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(19.dp)
                )
                .clickable { onNodeClick(node) }
                .dragAndDropSource(
                    drawDragDecoration = {
                        val itemWidth = 240.dp.toPx()
                        val itemHeight = 38.dp.toPx()
                        
                        // 1. Draw pill-shaped background (matching hovered list item)
                        drawRoundRect(
                            color = Color(0xFFEADDFF).copy(alpha = 0.6f), // secondaryContainer
                            size = Size(width = itemWidth, height = itemHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(19.dp.toPx()),
                            topLeft = Offset(0f, 0f)
                        )
                        
                        // 2. Draw a subtle primary border to make it pop
                        drawRoundRect(
                            color = Color(0xFF6750A4).copy(alpha = 0.5f), // primary
                            size = Size(width = itemWidth, height = itemHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(19.dp.toPx()),
                            topLeft = Offset(0f, 0f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                        
                        // 3. Draw the file name at the exact offset where item text starts
                        drawText(
                            textMeasurer = textMeasurer,
                            text = node.name,
                            style = TextStyle(
                                fontSize = 14.sp, 
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            topLeft = Offset(50.dp.toPx(), 9.dp.toPx()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    block = {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                onDragStart(node.uri)
                                startTransfer(
                                    DragAndDropTransferData(
                                        clipData = ClipData.newPlainText("FileUri", node.uri)
                                    )
                                )
                            },
                            onDrag = { _, _ -> },
                            onDragEnd = { onDragEnd(true) },
                            onDragCancel = { onDragEnd(false) }
                        )
                    }
                )
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    },
                    target = dndTarget
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(level) {
                Box(modifier = Modifier.width(16.dp).fillMaxHeight()) {
                    VerticalDivider(
                        modifier = Modifier.align(Alignment.Center).fillMaxHeight().width(0.5.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            if (node.isDirectory) {
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Spacer(Modifier.width(4.dp))

            FileIcon(
                fileName = node.name,
                isDirectory = node.isDirectory,
                isExpanded = node.isExpanded,
                useOriginalColors = useOriginalColors,
                availableIconNames = availableIconNames,
                modifier = Modifier.size(18.dp),
                tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else Color.Unspecified
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = node.name,
                style = if (isActive) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) 
                        else MaterialTheme.typography.bodyMedium,
                color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer 
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(
                    onClick = { onContextMenuRequest(node) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                DropdownMenu(
                    expanded = isContextMenuExpanded,
                    onDismissRequest = onContextMenuDismiss,
                    modifier = Modifier
                        .width(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    if (node.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("New File") },
                            onClick = { onCreateFileInDirectory(node) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            onClick = { onCreateFolderInDirectory(node) },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { onRename(node) },
                        leadingIcon = {
                            Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Name") },
                        onClick = { onCopyName(node) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Path") },
                        onClick = { onCopyPath(node) },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open in Folder") },
                        onClick = { onOpenInFolder(node) },
                        leadingIcon = {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(node) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

fun findNodeByUri(nodes: List<FileNode>, uri: String): FileNode? {
    for (node in nodes) {
        if (node.uri == uri) return node
        if (node.children != null) {
            val found = findNodeByUri(node.children, uri)
            if (found != null) return found
        }
    }
    return null
}

private fun resolveExplorerIconName(
    fileName: String,
    isDirectory: Boolean,
    isExpanded: Boolean,
    availableIconNames: Set<String>
): String? {
    val rawName = fileName.lowercase(Locale.ROOT)
    val sanitizedName = sanitizeForIcon(rawName)
    val extension = rawName.substringAfterLast('.', missingDelimiterValue = "")
    val simpleName = if (rawName.contains('.')) rawName.substringBeforeLast('.') else rawName
    val sanitizedSimpleName = sanitizeForIcon(simpleName)

    val candidates = mutableListOf<String>()

    if (isDirectory) {
        if (isExpanded) {
            candidates += "folder-$sanitizedName-open"
        }
        candidates += "folder-$sanitizedName"
        candidates += "folder-open"
        candidates += "folder"
    } else {
        val extensionIcon = extensionToIconName(extension)
        if (rawName == "dockerfile") candidates += "docker"
        if (rawName == "makefile") candidates += "makefile"
        if (rawName == "readme" || rawName.startsWith("readme.")) candidates += "readme"
        if (rawName == "license" || rawName.startsWith("license.")) candidates += "license"
        if (rawName == "changelog" || rawName.startsWith("changelog.")) candidates += "changelog"
        if (rawName == "package.json") candidates += "nodejs"
        if (rawName == "tsconfig.json") candidates += "tsconfig"
        if (rawName == "jsconfig.json") candidates += "jsconfig"
        candidates += sanitizedName
        candidates += sanitizedSimpleName
        if (extensionIcon != null) candidates += extensionIcon
        if (extension.isNotBlank()) candidates += extension
        candidates += "default_file"
    }

    return candidates.firstOrNull { it in availableIconNames }
}

private fun sanitizeForIcon(value: String): String {
    return value
        .trim()
        .replace(" ", "-")
        .replace("_", "-")
}

private fun extensionToIconName(extension: String): String? {
    return when (extension.lowercase(Locale.ROOT)) {
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "xml" -> "xml"
        "json" -> "json"
        "yaml", "yml" -> "yaml"
        "toml" -> "toml"
        "md", "markdown" -> "markdown"
        "html", "htm" -> "html"
        "css" -> "css"
        "scss", "sass" -> "sass"
        "less" -> "less"
        "js", "mjs", "cjs" -> "javascript"
        "ts", "mts", "cts" -> "typescript"
        "jsx" -> "react"
        "tsx" -> "react_ts"
        "py" -> "python"
        "rb" -> "ruby"
        "go" -> "go"
        "rs" -> "rust"
        "php" -> "php"
        "swift" -> "swift"
        "dart" -> "dart"
        "c" -> "c"
        "h" -> "h"
        "cc", "cpp", "cxx", "hpp", "hh" -> "cpp"
        "cs" -> "csharp"
        "sh", "bash", "zsh", "fish" -> "terminal"
        "ps1" -> "powershell"
        "sql" -> "database"
        "gradle" -> "gradle"
        "vue" -> "vue"
        "svelte" -> "svelte"
        "astro" -> "astro"
        "lock" -> "lock"
        else -> null
    }
}
