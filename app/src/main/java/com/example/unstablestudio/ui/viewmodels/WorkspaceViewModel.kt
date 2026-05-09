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
            if (node.uri == uri) return node
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

    init {
        // Auto-load last workspace on startup
        settingsRepository.lastRootUri.value?.let { uri ->
            if (uri.isNotEmpty()) {
                openWorkspace(uri)
            }
        }
    }

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
        loadDirectory(uri)
    }

    fun refreshWorkspace() {
        _rootUri.value?.let { loadDirectory(it) }
    }

    fun closeWorkspace() {
        _rootUri.value = null
        _fileTree.value = emptyList()
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

    fun createFile(name: String, isDirectory: Boolean, parentUri: String? = null) {
        val root = _rootUri.value ?: return
        val targetUri = parentUri ?: root
        val inputName = name.trim()
        if (inputName.isBlank()) return
        
        // Only append .txt if it's a file (not a directory) AND it doesn't have an extension
        val finalName = if (!isDirectory && !inputName.contains(".") && !inputName.startsWith(".")) {
            "$inputName.txt"
        } else {
            inputName
        }

        viewModelScope.launch {
            try {
                if (isNameTakenInFolder(targetUri, finalName)) {
                    AppLogger.w(TAG, "Skip create: duplicate name '$finalName' in $targetUri")
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

    fun moveFile(sourceNode: FileNode, targetParentUri: String) {
        viewModelScope.launch {
            try {
                val sourceParentUri = findParentUri(_fileTree.value, sourceNode.uri) ?: _rootUri.value
                if (sourceParentUri == targetParentUri) {
                    AppLogger.d(TAG, "Skip move: source and target parent are the same")
                    return@launch
                }
                
                val movedNode = sourceParentUri?.let { repository.moveFile(sourceNode.uri, it, targetParentUri) }
                if (movedNode != null) {
                    // Mark target folder as expanded so it remains open (if it's not root)
                    if (targetParentUri != _rootUri.value) {
                        _expandedUris.value += targetParentUri
                    }
                    
                    // Surgical refresh
                    if (targetParentUri == _rootUri.value) {
                        _rootUri.value?.let { loadDirectory(it) }
                    } else {
                        refreshFolder(targetParentUri)
                    }
                    
                    if (sourceParentUri != null && sourceParentUri != targetParentUri) {
                        if (sourceParentUri == _rootUri.value) {
                            loadDirectory(sourceParentUri)
                        } else {
                            refreshFolder(sourceParentUri)
                        }
                    }
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
                if (uri == _rootUri.value) {
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

    fun renameFile(node: FileNode, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            try {
                if (trimmedName.equals(node.name, ignoreCase = true)) {
                    return@launch
                }

                val parentUri = findParentUri(_fileTree.value, node.uri) ?: _rootUri.value
                if (parentUri != null && isNameTakenInFolder(parentUri, trimmedName, excludeUri = node.uri)) {
                    AppLogger.w(TAG, "Skip rename: duplicate name '$trimmedName' in $parentUri")
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
            targetUri == root -> loadDirectory(root)
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
            if (node.uri == targetUri) {
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
            if (excludeUri != null && child.uri == excludeUri) return@any false
            child.name.normalizeForCompare() == normalizedTarget
        }
    }

    fun findParentUri(nodes: List<FileNode>, targetUri: String, parentUri: String? = null): String? {
        for (node in nodes) {
            if (node.uri == targetUri) return parentUri
            val children = node.children ?: continue
            val found = findParentUri(children, targetUri, node.uri)
            if (found != null) return found
        }
        return null
    }

    private fun String.normalizeForCompare(): String = trim().lowercase(Locale.ROOT)
}
