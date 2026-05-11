package com.example.unstablestudio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unstablestudio.core.common.AppLogger
import com.example.unstablestudio.domain.model.FileNode
import com.example.unstablestudio.domain.repository.DocumentRepository
import com.example.unstablestudio.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private const val TAG = "WorkspaceViewModel"

data class FlatFileNode(val node: FileNode, val level: Int)

data class CreatingNodeState(
    val parentUri: String,
    val isDirectory: Boolean
)

enum class ConflictSource {
    CREATE, RENAME, MOVE
}

data class ConflictState(
    val source: ConflictSource,
    val targetName: String,
    val targetParentUri: String,
    val isDirectory: Boolean,
    val originalNode: FileNode? = null,
    val moveSourceParentUri: String? = null
)

sealed class WorkspaceFileEvent {
    data class Renamed(val oldUri: String, val newUri: String, val newName: String) : WorkspaceFileEvent()
    data class Deleted(val uri: String) : WorkspaceFileEvent()
}

class WorkspaceViewModel(
    private val repository: DocumentRepository,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _rootUri = MutableStateFlow<String?>(null)
    val rootUri: StateFlow<String?> = _rootUri.asStateFlow()

    private val _expandedUris = MutableStateFlow<Set<String>>(emptySet())
    val expandedUris: StateFlow<Set<String>> = _expandedUris.asStateFlow()

    private val _creatingNode = MutableStateFlow<CreatingNodeState?>(null)
    val creatingNode: StateFlow<CreatingNodeState?> = _creatingNode.asStateFlow()

    private val _focusedNodeUri = MutableStateFlow<String?>(null)
    val focusedNodeUri: StateFlow<String?> = _focusedNodeUri.asStateFlow()

    private val _renamingNodeUri = MutableStateFlow<String?>(null)
    val renamingNodeUri: StateFlow<String?> = _renamingNodeUri.asStateFlow()

    private val _conflictState = MutableStateFlow<ConflictState?>(null)
    val conflictState: StateFlow<ConflictState?> = _conflictState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _fileEvents = MutableSharedFlow<WorkspaceFileEvent>(extraBufferCapacity = 32)
    val fileEvents: SharedFlow<WorkspaceFileEvent> = _fileEvents.asSharedFlow()

    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    private val _isRootHovered = MutableStateFlow(false)
    val isRootHovered: StateFlow<Boolean> = _isRootHovered.asStateFlow()

    fun setIsDragging(value: Boolean) {
        _isDragging.value = value
    }

    fun setIsRootHovered(value: Boolean) {
        _isRootHovered.value = value
    }

    fun setFocus(uri: String?) {
        _focusedNodeUri.value = uri
    }

    /**
     * Helper to check if two URIs point to the same document/tree.
     * SAF URIs for the same item can have different string representations (Tree vs Document).
     */
    private fun areUrisEqual(uri1: String?, uri2: String?): Boolean {
        if (uri1 == uri2) return true
        if (uri1 == null || uri2 == null) return false
        
        try {
            val u1 = android.net.Uri.parse(uri1)
            val u2 = android.net.Uri.parse(uri2)
            
            // Compare document IDs if available
            val id1 = try { android.provider.DocumentsContract.getDocumentId(u1) } catch (_: Exception) { null }
            val id2 = try { android.provider.DocumentsContract.getDocumentId(u2) } catch (_: Exception) { null }
            
            if (id1 != null && id2 != null) return id1 == id2
            
            // Fallback to tree ID comparison
            val tree1 = try { android.provider.DocumentsContract.getTreeDocumentId(u1) } catch (_: Exception) { null }
            val tree2 = try { android.provider.DocumentsContract.getTreeDocumentId(u2) } catch (_: Exception) { null }
            
            if (tree1 != null && tree2 != null) return tree1 == tree2
            
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to compare URIs: $uri1 vs $uri2", e)
        }
        
        return false
    }

    fun startCreating(parentUri: String?, isDirectory: Boolean) {
        // Use provided parent, or focused node if it's a directory, or parent of focused node, or root
        val target = parentUri ?: run {
            val focused = _focusedNodeUri.value
            if (focused != null) {
                val node = findNodeByUri(_fileTree.value, focused)
                // If focused item is directory, use it as parent. 
                // If it's a file, we could heuristic get its parent from URI or just use root for simplicity
                if (node?.isDirectory == true) focused 
                else focused.substringBeforeLast("/", _rootUri.value ?: "")
            } else {
                _rootUri.value
            }
        } ?: return
        
        _creatingNode.value = CreatingNodeState(target, isDirectory)
    }

    private fun findNodeByUri(nodes: List<FileNode>, uri: String): FileNode? {
        for (node in nodes) {
            if (areUrisEqual(node.uri, uri)) return node
            if (node.children != null) {
                val found = findNodeByUri(node.children, uri)
                if (found != null) return found
            }
        }
        return null
    }

    fun cancelCreating() {
        _creatingNode.value = null
    }

    fun finishCreating(name: String) {
        val state = _creatingNode.value ?: return
        if (name.isNotBlank()) {
            createFile(name, state.isDirectory, state.parentUri)
        }
        _creatingNode.value = null
    }

    fun startRenaming(uri: String) {
        _renamingNodeUri.value = uri
    }

    fun cancelRenaming() {
        _renamingNodeUri.value = null
    }

    fun finishRenaming(node: FileNode, newName: String) {
        if (newName.isNotBlank() && newName != node.name) {
            renameFile(node, newName)
        }
        _renamingNodeUri.value = null
    }

    fun dismissConflict() {
        _conflictState.value = null
    }

    fun resolveConflict(action: String, newName: String? = null) {
        val state = _conflictState.value ?: return
        _conflictState.value = null

        when (action) {
            "OVERWRITE" -> {
                executeConflictAction(state, state.targetName, overwrite = true)
            }
            "AUTORENAME" -> {
                viewModelScope.launch {
                    val uniqueName = generateUniqueName(state.targetParentUri, state.targetName, state.isDirectory)
                    executeConflictAction(state, uniqueName, overwrite = false)
                }
            }
            "RENAME" -> {
                if (!newName.isNullOrBlank()) {
                    executeConflictAction(state, newName, overwrite = false)
                }
            }
        }
    }

    private fun executeConflictAction(state: ConflictState, finalName: String, overwrite: Boolean) {
        viewModelScope.launch {
            try {
                when (state.source) {
                    ConflictSource.CREATE -> {
                        if (overwrite) {
                            val existing = repository.listDirectory(state.targetParentUri).find { 
                                it.name.normalizeForCompare() == finalName.normalizeForCompare() 
                            }
                            existing?.let { repository.deleteFile(it.uri) }
                        }
                        createFile(finalName, state.isDirectory, state.targetParentUri, skipConflictCheck = true)
                    }
                    ConflictSource.RENAME -> {
                        val node = state.originalNode ?: return@launch
                        if (overwrite) {
                            val existing = repository.listDirectory(state.targetParentUri).find { 
                                it.name.normalizeForCompare() == finalName.normalizeForCompare() && !areUrisEqual(it.uri, node.uri)
                            }
                            existing?.let { repository.deleteFile(it.uri) }
                        }
                        renameFile(node, finalName, skipConflictCheck = true)
                    }
                    ConflictSource.MOVE -> {
                        val node = state.originalNode ?: return@launch
                        val sourceParent = state.moveSourceParentUri ?: return@launch
                        if (overwrite) {
                            val existing = repository.listDirectory(state.targetParentUri).find { 
                                it.name.normalizeForCompare() == finalName.normalizeForCompare() && !areUrisEqual(it.uri, node.uri)
                            }
                            existing?.let { repository.deleteFile(it.uri) }
                        }
                        
                        var currentUri = node.uri
                        if (finalName != node.name) {
                            val renamed = repository.renameFile(node.uri, finalName)
                            if (renamed != null) {
                                currentUri = renamed.uri
                            }
                        }
                        
                        val movedNode = repository.moveFile(currentUri, sourceParent, state.targetParentUri)
                        if (movedNode != null) {
                            // Expand target folder after drop
                            if (!areUrisEqual(state.targetParentUri, _rootUri.value)) {
                                _expandedUris.value += state.targetParentUri
                            }
                            
                            refreshAfterChange(currentUri)
                            refreshFolder(state.targetParentUri)
                            
                            if (!areUrisEqual(sourceParent, state.targetParentUri)) {
                                refreshFolder(sourceParent)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to resolve conflict for ${state.targetName}", e)
            }
        }
    }

    private suspend fun generateUniqueName(folderUri: String, baseName: String, isDirectory: Boolean): String {
        var name = baseName
        var index = 1
        val dotIndex = baseName.lastIndexOf('.')
        val nameWithoutExt = if (dotIndex != -1 && !isDirectory) baseName.substring(0, dotIndex) else baseName
        val ext = if (dotIndex != -1 && !isDirectory) baseName.substring(dotIndex) else ""

        while (isNameTakenInFolder(folderUri, name)) {
            name = "${nameWithoutExt}_$index$ext"
            index++
        }
        return name
    }

    // Redundant init restoration removed to allow MainActivity to coordinate session restore

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    val flatFileTree: StateFlow<List<FlatFileNode>> = _fileTree.map { nodes ->
        withContext(Dispatchers.Default) {
            flattenTree(nodes)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun flattenTree(nodes: List<FileNode>, currentLevel: Int = 0): List<FlatFileNode> {
        val result = mutableListOf<FlatFileNode>()
        for (node in nodes) {
            result.add(FlatFileNode(node, currentLevel))
            if (node.isDirectory && node.isExpanded && node.children != null) {
                result.addAll(flattenTree(node.children, currentLevel + 1))
            }
        }
        return result
    }

    fun openWorkspace(uri: String) {
        _rootUri.value = uri
        settingsRepository.setLastRootUri(uri)
        loadDirectory(uri)
    }

    fun refreshWorkspace() {
        _rootUri.value?.let { loadDirectory(it) }
    }

    fun closeWorkspace() {
        _rootUri.value = null
        _fileTree.value = emptyList()
        settingsRepository.setLastRootUri(null)
    }

    private fun loadDirectory(uri: String) {
        viewModelScope.launch {
            try {
                val nodes = repository.listDirectory(uri)
                val updatedNodes = restoreExpansionState(nodes)
                _fileTree.value = updatedNodes
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load directory: $uri", e)
            }
        }
    }

    private suspend fun restoreExpansionState(nodes: List<FileNode>): List<FileNode> {
        return nodes.map { node ->
            if (node.isDirectory && _expandedUris.value.contains(node.uri)) {
                val children = repository.listDirectory(node.uri)
                node.copy(
                    isExpanded = true, 
                    children = restoreExpansionState(children)
                )
            } else {
                node
            }
        }
    }

    fun createFile(name: String, isDirectory: Boolean, parentUri: String? = null, skipConflictCheck: Boolean = false) {
        val root = _rootUri.value ?: return
        val targetUri = parentUri ?: root
        val inputName = name.trim()
        if (inputName.isBlank()) return
        
        // Use the input name as is
        val finalName = inputName

        viewModelScope.launch {
            try {
                if (!skipConflictCheck && isNameTakenInFolder(targetUri, finalName)) {
                    _conflictState.value = ConflictState(
                        source = ConflictSource.CREATE,
                        targetName = finalName,
                        targetParentUri = targetUri,
                        isDirectory = isDirectory
                    )
                    return@launch
                }
                val newNode = repository.createFile(targetUri, finalName, isDirectory)
                if (newNode != null) {
                    refreshFolder(targetUri)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to create file: $finalName", e)
            }
        }
    }

    fun moveFile(sourceNode: FileNode, targetParentUri: String, skipConflictCheck: Boolean = false) {
        viewModelScope.launch {
            try {
                val sourceParentUri = findParentUri(_fileTree.value, sourceNode.uri) ?: _rootUri.value
                if (areUrisEqual(sourceParentUri, targetParentUri)) {
                    AppLogger.d(TAG, "Skip move: source and target parent are the same")
                    return@launch
                }
                
                if (!skipConflictCheck && isNameTakenInFolder(targetParentUri, sourceNode.name, excludeUri = sourceNode.uri)) {
                    _conflictState.value = ConflictState(
                        source = ConflictSource.MOVE,
                        targetName = sourceNode.name,
                        targetParentUri = targetParentUri,
                        isDirectory = sourceNode.isDirectory,
                        originalNode = sourceNode,
                        moveSourceParentUri = sourceParentUri
                    )
                    return@launch
                }

                AppLogger.d(TAG, "Attempting to move: ${sourceNode.name} from $sourceParentUri to $targetParentUri")
                val movedNode = sourceParentUri?.let { repository.moveFile(sourceNode.uri, it, targetParentUri) }
                if (movedNode != null) {
                    AppLogger.d(TAG, "Move successful: ${movedNode.uri}")
                    // Mark target folder as expanded so it remains open (if it's not root)
                    if (!areUrisEqual(targetParentUri, _rootUri.value)) {
                        _expandedUris.value += targetParentUri
                    }
                    
                    // Surgical refresh
                    refreshAfterChange(sourceNode.uri)
                    refreshFolder(targetParentUri)
                    
                    if (sourceParentUri != null && !areUrisEqual(sourceParentUri, targetParentUri)) {
                        refreshFolder(sourceParentUri)
                    }
                } else {
                    AppLogger.e(TAG, "Repository moveFile returned null for ${sourceNode.name}")
                    _errorMessage.emit("Failed to move '${sourceNode.name}'. Make sure you have permission.")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to move file: ${sourceNode.uri}", e)
            }
        }
    }

    fun moveFileToSibling(sourceNode: FileNode, siblingUri: String) {
        val targetParentUri = findParentUri(_fileTree.value, siblingUri) ?: _rootUri.value
        if (targetParentUri != null) {
            moveFile(sourceNode, targetParentUri)
        }
    }

    private fun refreshFolder(uri: String) {
        viewModelScope.launch {
            try {
                val children = repository.listDirectory(uri)
                val updatedChildren = restoreExpansionState(children)
                if (areUrisEqual(uri, _rootUri.value)) {
                    _fileTree.value = updatedChildren
                } else {
                    _fileTree.value = updateNodeInTree(_fileTree.value, uri) { target ->
                        target.copy(children = updatedChildren, isExpanded = true)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to refresh folder: $uri", e)
            }
        }
    }

    fun deleteFile(node: FileNode) {
        viewModelScope.launch {
            try {
                val success = repository.deleteFile(node.uri)
                if (success) {
                    _fileEvents.emit(WorkspaceFileEvent.Deleted(node.uri))
                    refreshAfterChange(node.uri)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to delete file: ${node.uri}", e)
            }
        }
    }

    fun renameFile(node: FileNode, newName: String, skipConflictCheck: Boolean = false) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            try {
                if (trimmedName.equals(node.name, ignoreCase = true)) {
                    return@launch
                }

                val parentUri = findParentUri(_fileTree.value, node.uri) ?: _rootUri.value
                if (parentUri != null && !skipConflictCheck && isNameTakenInFolder(parentUri, trimmedName, excludeUri = node.uri)) {
                    _conflictState.value = ConflictState(
                        source = ConflictSource.RENAME,
                        targetName = trimmedName,
                        targetParentUri = parentUri,
                        isDirectory = node.isDirectory,
                        originalNode = node
                    )
                    return@launch
                }

                val renamedNode = repository.renameFile(node.uri, trimmedName)
                if (renamedNode != null) {
                    _fileEvents.emit(
                        WorkspaceFileEvent.Renamed(
                            oldUri = node.uri,
                            newUri = renamedNode.uri,
                            newName = renamedNode.name
                        )
                    )
                    refreshAfterChange(node.uri)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to rename file: ${node.uri}", e)
            }
        }
    }

    private fun refreshAfterChange(targetUri: String) {
        val root = _rootUri.value ?: return
        val parentUri = findParentUri(_fileTree.value, targetUri)
        when {
            parentUri != null -> refreshFolder(parentUri)
            areUrisEqual(targetUri, root) -> loadDirectory(root)
            else -> loadDirectory(root)
        }
    }

    fun toggleFolder(node: FileNode) {
        viewModelScope.launch {
            if (node.isDirectory) {
                if (!node.isExpanded) {
                    _expandedUris.value += node.uri
                    val children = repository.listDirectory(node.uri)
                    _fileTree.value = updateNodeInTree(_fileTree.value, node.uri) { target ->
                        target.copy(isExpanded = true, children = children)
                    }
                } else {
                    _expandedUris.value -= node.uri
                    _fileTree.value = updateNodeInTree(_fileTree.value, node.uri) { target ->
                        target.copy(isExpanded = false, children = null)
                    }
                }
            }
        }
    }

    private fun updateNodeInTree(
        nodes: List<FileNode>,
        targetUri: String,
        update: (FileNode) -> FileNode
    ): List<FileNode> {
        return nodes.map { node ->
            if (areUrisEqual(node.uri, targetUri)) {
                update(node)
            } else if (node.children != null) {
                node.copy(children = updateNodeInTree(node.children, targetUri, update))
            } else {
                node
            }
        }
    }

    fun collapseAllFolders() {
        _expandedUris.value = emptySet()
        _fileTree.value = collapseTree(_fileTree.value)
    }

    private fun collapseTree(nodes: List<FileNode>): List<FileNode> {
        return nodes.map { node ->
            if (node.isDirectory) {
                node.copy(isExpanded = false, children = null)
            } else {
                node
            }
        }
    }

    private suspend fun isNameTakenInFolder(
        folderUri: String,
        targetName: String,
        excludeUri: String? = null
    ): Boolean {
        val normalizedTarget = targetName.normalizeForCompare()
        return repository.listDirectory(folderUri).any { child ->
            if (excludeUri != null && areUrisEqual(child.uri, excludeUri)) return@any false
            child.name.normalizeForCompare() == normalizedTarget
        }
    }

    fun findParentUri(nodes: List<FileNode>, targetUri: String, parentUri: String? = null): String? {
        for (node in nodes) {
            if (areUrisEqual(node.uri, targetUri)) return parentUri
            val children = node.children ?: continue
            val found = findParentUri(children, targetUri, node.uri)
            if (found != null) return found
        }
        return null
    }

    private fun String.normalizeForCompare(): String = trim().lowercase(Locale.ROOT)
}
