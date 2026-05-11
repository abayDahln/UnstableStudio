package com.example.unstablestudio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unstablestudio.core.common.AppLogger
import com.example.unstablestudio.core.config.AppConstants
import com.example.unstablestudio.domain.model.Document
import com.example.unstablestudio.domain.model.EditorAction
import com.example.unstablestudio.domain.undo.TextChangeCommand
import com.example.unstablestudio.domain.undo.UndoManager
import com.example.unstablestudio.domain.repository.DocumentRepository
import com.example.unstablestudio.data.repository.SettingsRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

private const val TAG = "EditorViewModel"

data class PendingChange(
    val docId: String,
    val startOffset: Int,
    val oldText: String,
    val newText: String,
    val oldCursor: Int,
    val newCursor: Int
)

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val repository: DocumentRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val undoManagers = mutableMapOf<String, UndoManager>()
    private val _pendingChanges = MutableSharedFlow<PendingChange>()

    init {
        // Handle text changes immediately to avoid history loss
        _pendingChanges
            .onEach { change ->
                // Update content in state for the UI
                updateContentInternal(change.docId, change.newText, false)
                // Commit to undo stack (merging logic in UndoCommand handles grouping)
                commitChangeToUndoStack(change)
            }
            .launchIn(viewModelScope)
    }

    private fun getUndoManager(docId: String): UndoManager {
        return undoManagers.getOrPut(docId) { UndoManager() }
    }

    fun onTextChange(
        docId: String,
        startOffset: Int,
        oldText: String,
        newText: String,
        oldCursor: Int,
        newCursor: Int
    ) {
        viewModelScope.launch {
            _pendingChanges.emit(PendingChange(docId, startOffset, oldText, newText, oldCursor, newCursor))
        }
    }

    private fun commitChangeToUndoStack(change: PendingChange) {
        val manager = getUndoManager(change.docId)
        val command = TextChangeCommand(
            startOffset = change.startOffset,
            oldText = change.oldText,
            newText = change.newText,
            oldCursor = change.oldCursor,
            newCursor = change.newCursor,
            onApply = { offset, deleteCount, insertText, cursor ->
                dispatchEditorAction(EditorAction.PerformTextChange(offset, deleteCount, insertText, cursor))
            }
        )
        manager.pushCommand(command)
    }

    fun undo() {
        val docId = _activeDocumentId.value ?: return
        getUndoManager(docId).undo()
    }

    fun redo() {
        val docId = _activeDocumentId.value ?: return
        getUndoManager(docId).redo()
    }

    val fontSize: StateFlow<Float> = settingsRepository.fontSize
    val wordWrap: StateFlow<Boolean> = settingsRepository.wordWrap

    private val _editorActions = MutableSharedFlow<EditorAction>()
    val editorActions: SharedFlow<EditorAction> = _editorActions.asSharedFlow()

    fun dispatchEditorAction(action: EditorAction) {
        viewModelScope.launch {
            _editorActions.emit(action)
        }
    }

    fun setWordWrap(enabled: Boolean) {
        settingsRepository.setWordWrap(enabled)
    }
    
    fun setFontSize(size: Float) {
        settingsRepository.setFontSize(size)
    }

    private val _openDocuments = MutableStateFlow<List<Document>>(emptyList())
    val openDocuments: StateFlow<List<Document>> = _openDocuments.asStateFlow()

    private val _activeDocumentId = MutableStateFlow<String?>(null)
    val activeDocumentId: StateFlow<String?> = _activeDocumentId.asStateFlow()

    val currentDocument = combine(_openDocuments, _activeDocumentId) { docs, activeId ->
        docs.find { it.id == activeId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun loadFile(id: String) {
        if (_openDocuments.value.any { it.id == id }) {
            _openDocuments.value = _openDocuments.value.map { doc ->
                if (doc.id == id && doc.isMissingFromWorkspace) {
                    doc.copy(isMissingFromWorkspace = false)
                } else {
                    doc
                }
            }
            _activeDocumentId.value = id
            return
        }

        viewModelScope.launch {
            try {
                val document = repository.loadDocument(id)
                _openDocuments.value = _openDocuments.value + document
                _activeDocumentId.value = id
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load file: $id", e)
            }
        }
    }

    fun setActiveFile(id: String) {
        if (_openDocuments.value.any { it.id == id }) {
            _activeDocumentId.value = id
        }
    }

    suspend fun restoreSession(ids: List<String>, activeId: String?) {
        if (ids.isEmpty()) return
        
        withContext(Dispatchers.IO) {
            val loadedDocs = mutableListOf<Document>()
            for (id in ids) {
                try {
                    // Check if already open
                    val existing = _openDocuments.value.find { it.id == id }
                    if (existing != null) {
                        loadedDocs.add(existing)
                    } else {
                        val document = repository.loadDocument(id)
                        loadedDocs.add(document)
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to restore file: $id", e)
                }
            }
            
            if (loadedDocs.isNotEmpty()) {
                _openDocuments.value = loadedDocs
                if (activeId != null && loadedDocs.any { it.id == activeId }) {
                    _activeDocumentId.value = activeId
                } else if (loadedDocs.isNotEmpty()) {
                    _activeDocumentId.value = loadedDocs.last().id
                }
            }
        }
    }

    fun closeFile(id: String) {
        val currentList = _openDocuments.value
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val newList = currentList.filter { it.id != id }
            _openDocuments.value = newList

            if (_activeDocumentId.value == id) {
                if (newList.isNotEmpty()) {
                    val nextIndex = if (index >= newList.size) newList.size - 1 else index
                    _activeDocumentId.value = newList[nextIndex].id
                } else {
                    _activeDocumentId.value = null
                }
            }
            // Cleanup undo manager
            undoManagers.remove(id)
        }
    }

    fun closeAllFiles() {
        _openDocuments.value = emptyList()
        _activeDocumentId.value = null
        undoManagers.clear()
    }

    fun onWorkspaceFileRenamed(oldUri: String, newUri: String, newName: String) {
        if (oldUri == newUri) {
            _openDocuments.value = _openDocuments.value.map { doc ->
                if (doc.id == oldUri) doc.copy(title = newName, isMissingFromWorkspace = false) else doc
            }
            return
        }

        val hasTargetDoc = _openDocuments.value.any { it.id == newUri }
        val hasOldDoc = _openDocuments.value.any { it.id == oldUri }
        if (!hasOldDoc) return

        _openDocuments.value = _openDocuments.value.mapNotNull { doc ->
            if (doc.id != oldUri) return@mapNotNull doc
            if (hasTargetDoc) {
                null
            } else {
                doc.copy(id = newUri, title = newName, isMissingFromWorkspace = false)
            }
        }

        if (_activeDocumentId.value == oldUri) {
            _activeDocumentId.value = if (hasTargetDoc) newUri else newUri
        }

        val manager = undoManagers.remove(oldUri)
        if (manager != null && !hasTargetDoc) {
            undoManagers[newUri] = manager
        }
    }

    fun onWorkspaceFileDeleted(uri: String) {
        _openDocuments.value = _openDocuments.value.map { doc ->
            if (doc.id == uri) doc.copy(isMissingFromWorkspace = true) else doc
        }
    }

    fun syncWorkspaceFiles(existingUris: Set<String>, hasWorkspace: Boolean) {
        if (!hasWorkspace) return
        _openDocuments.value = _openDocuments.value.map { doc ->
            val missing = doc.id !in existingUris
            if (doc.isMissingFromWorkspace == missing) doc else doc.copy(isMissingFromWorkspace = missing)
        }
    }

    fun updateContent(id: String, content: String) {
        updateContentInternal(id, content, false)
    }

    private fun updateContentInternal(id: String, content: String, clearUndo: Boolean) {
        val currentDocs = _openDocuments.value
        val index = currentDocs.indexOfFirst { it.id == id }
        if (index != -1 && currentDocs[index].content != content) {
            val newList = currentDocs.toMutableList()
            newList[index] = newList[index].copy(content = content)
            _openDocuments.value = newList
            
            if (clearUndo) {
                undoManagers[id]?.clear()
            }
        }
    }

    fun markAsModified(id: String, content: String? = null) {
        val currentDocs = _openDocuments.value
        val index = currentDocs.indexOfFirst { it.id == id }
        if (index != -1) {
            val doc = currentDocs[index]
            val needsUpdate = !doc.isModified || (content != null && doc.content != content)
            if (needsUpdate) {
                val newList = currentDocs.toMutableList()
                newList[index] = doc.copy(
                    isModified = true,
                    content = content ?: doc.content
                )
                _openDocuments.value = newList
            }
        }
    }

    fun saveFile() {
        val document = currentDocument.value ?: return
        saveDocument(document)
    }

    fun saveAllFiles() {
        viewModelScope.launch {
            _openDocuments.value.filter { it.isModified }.forEach { doc ->
                saveDocument(doc)
            }
        }
    }

    private fun saveDocument(document: Document) {
        viewModelScope.launch {
            try {
                repository.saveDocument(document)
                _openDocuments.value = _openDocuments.value.map { doc ->
                    if (doc.id == document.id) {
                        doc.copy(isModified = false)
                    } else {
                        doc
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to save file: ${document.id}", e)
            }
        }
    }
}
