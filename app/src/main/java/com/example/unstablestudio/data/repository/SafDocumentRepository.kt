package com.example.unstablestudio.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.unstablestudio.core.common.AppLogger
import com.example.unstablestudio.core.config.AppConstants
import com.example.unstablestudio.domain.model.Document
import com.example.unstablestudio.domain.model.FileNode
import com.example.unstablestudio.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

private const val TAG = "SafDocumentRepository"

class SafDocumentRepository(private val context: Context) : DocumentRepository {

    override suspend fun loadDocument(id: String): Document {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(id)
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                    ?: throw Exception("Cannot access file")

                // Use streaming to avoid loading large files entirely into memory
                val contentBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            contentBuilder.append(line).append("\n")
                        }
                    }
                } ?: throw Exception("Cannot open file for reading")

                val name = documentFile.name ?: "Unknown"
                val langId = detectLanguageId(name)

                Document(
                    id = id,
                    title = name,
                    content = contentBuilder.toString(),
                    languageId = langId
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load document: $id", e)
                throw e
            }
        }
    }

    override suspend fun saveDocument(document: Document) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.id)
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    outputStream.write(document.content.toByteArray())
                } ?: throw Exception("Cannot open file for writing")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to save document: ${document.id}", e)
                throw e
            }
        }
    }

    override suspend fun listDirectory(uriString: String): List<FileNode> {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val result = mutableListOf<FileNode>()
            
            try {
                // If it's a tree URI, we can use the root directory ID.
                // If it's a document URI within a tree, we must get its document ID.
                val treeUri = if (android.provider.DocumentsContract.isTreeUri(uri)) {
                    uri
                } else {
                    val path = uri.path ?: ""
                    if (path.contains("/tree/")) {
                        val treeId = path.substringAfter("/tree/").substringBefore("/document/")
                        android.provider.DocumentsContract.buildTreeDocumentUri(uri.authority, treeId)
                    } else {
                        null
                    }
                }

                val parentDocumentId = try {
                    android.provider.DocumentsContract.getDocumentId(uri)
                } catch (e: Exception) {
                    if (treeUri != null) android.provider.DocumentsContract.getTreeDocumentId(treeUri) else null
                }

                if (treeUri != null && parentDocumentId != null) {
                    val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                        treeUri,
                        parentDocumentId
                    )

                    context.contentResolver.query(
                        childrenUri,
                        arrayOf(
                            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                        ),
                        null, null, null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val docId = cursor.getString(0)
                            val name = cursor.getString(1)
                            val mimeType = cursor.getString(2)
                            val isDir = android.provider.DocumentsContract.Document.MIME_TYPE_DIR == mimeType
                            
                            val childUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                                treeUri,
                                docId
                            )
                            
                            result.add(
                                FileNode(
                                    uri = childUri.toString(),
                                    name = name ?: "Unknown",
                                    isDirectory = isDir
                                )
                            )
                        }
                    }
                } else {
                    // Fallback to DocumentFile for non-tree URIs or if manual query fails
                    val root = getDocumentFile(uri) ?: return@withContext emptyList()
                    if (root.isDirectory) {
                        root.listFiles().forEach { file ->
                            result.add(
                                FileNode(
                                    uri = file.uri.toString(),
                                    name = file.name ?: "Unknown",
                                    isDirectory = file.isDirectory
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to list directory: $uriString", e)
            }
            
            result.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
    }

    override suspend fun createFile(parentUriString: String, name: String, isDirectory: Boolean): FileNode? {
        return withContext(Dispatchers.IO) {
            try {
                val parentUri = Uri.parse(parentUriString)
                val parentFolder = getDocumentFile(parentUri)
                    ?: return@withContext null

                if (!parentFolder.isDirectory) return@withContext null

                val newDoc = if (isDirectory) {
                    parentFolder.createDirectory(name)
                } else {
                    val mimeType = detectMimeType(name)
                    parentFolder.createFile(mimeType, name)
                }

                newDoc?.let {
                    FileNode(
                        uri = it.uri.toString(),
                        name = it.name ?: name,
                        isDirectory = it.isDirectory
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to create file: $name in $parentUriString", e)
                null
            }
        }
    }

    override suspend fun deleteFile(uriString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri)
            } catch (e: Exception) {
                AppLogger.w(TAG, "DocumentsContract delete failed, trying fallback: $uriString", e)
                try {
                    val documentFile = DocumentFile.fromSingleUri(context, Uri.parse(uriString))
                    documentFile?.delete() ?: false
                } catch (e2: Exception) {
                    AppLogger.e(TAG, "Failed to delete file: $uriString", e2)
                    false
                }
            }
        }
    }

    override suspend fun renameFile(uriString: String, newName: String): FileNode? {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            try {
                val renamedUri = android.provider.DocumentsContract.renameDocument(
                    context.contentResolver,
                    uri,
                    newName
                )

                if (renamedUri != null) {
                    val renamedDoc = DocumentFile.fromSingleUri(context, renamedUri)
                    return@withContext FileNode(
                        uri = renamedUri.toString(),
                        name = renamedDoc?.name ?: newName,
                        isDirectory = renamedDoc?.isDirectory ?: false
                    )
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "DocumentsContract rename failed, trying fallback: $uriString", e)
            }

            try {
                val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return@withContext null
                val success = documentFile.renameTo(newName)
                if (!success) {
                    AppLogger.w(TAG, "Failed to rename file: $uriString to $newName")
                    return@withContext null
                }

                val refreshedDoc = DocumentFile.fromSingleUri(context, documentFile.uri)
                FileNode(
                    uri = documentFile.uri.toString(),
                    name = refreshedDoc?.name ?: documentFile.name ?: newName,
                    isDirectory = refreshedDoc?.isDirectory ?: documentFile.isDirectory
                )
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to rename file: $uriString", e)
                null
            }
        }
    }

    override suspend fun moveFile(sourceUriString: String, sourceParentUriString: String, targetParentUriString: String): FileNode? {
        return withContext(Dispatchers.IO) {
            try {
                val sourceUri = Uri.parse(sourceUriString)
                var sourceParentUri = Uri.parse(sourceParentUriString)
                var targetParentUri = Uri.parse(targetParentUriString)

                // Android moveDocument requires DOCUMENT URIs, not TREE URIs.
                if (android.provider.DocumentsContract.isTreeUri(targetParentUri)) {
                    val documentId = android.provider.DocumentsContract.getTreeDocumentId(targetParentUri)
                    targetParentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(targetParentUri, documentId)
                }

                if (android.provider.DocumentsContract.isTreeUri(sourceParentUri)) {
                    val documentId = android.provider.DocumentsContract.getTreeDocumentId(sourceParentUri)
                    sourceParentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(sourceParentUri, documentId)
                }

                // Check if target already exists to avoid IllegalStateException
                val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
                val targetParentFile = getDocumentFile(targetParentUri)
                
                if (sourceFile != null && targetParentFile != null && targetParentFile.isDirectory) {
                    val existingFile = targetParentFile.findFile(sourceFile.name ?: "")
                    if (existingFile != null) {
                        // Only block if the existing file is DIFFERENT from our source file.
                        // We compare document IDs to handle Tree vs Document URI variations.
                        val sourceDocId = try { android.provider.DocumentsContract.getDocumentId(sourceUri) } catch (_: Exception) { null }
                        val existingDocId = try { android.provider.DocumentsContract.getDocumentId(existingFile.uri) } catch (_: Exception) { null }
                        
                        if (sourceDocId == null || existingDocId == null || sourceDocId != existingDocId) {
                            AppLogger.e(TAG, "Failed to move file: Target already exists: ${sourceFile.name}")
                            return@withContext null
                        } else {
                            // It's the same file, we can skip the move or let it proceed (usually a no-op)
                            AppLogger.d(TAG, "Move skipped: Source and target are the same file: ${sourceFile.name}")
                            return@withContext FileNode(
                                uri = existingFile.uri.toString(),
                                name = existingFile.name ?: sourceFile.name ?: "Unknown",
                                isDirectory = existingFile.isDirectory
                            )
                        }
                    }
                }

                var movedUri: Uri? = null
                try {
                    movedUri = android.provider.DocumentsContract.moveDocument(
                        context.contentResolver,
                        sourceUri,
                        sourceParentUri,
                        targetParentUri
                    )
                } catch (e: Exception) {
                    AppLogger.w(TAG, "DocumentsContract.moveDocument failed, trying copy+delete fallback", e)
                }

                if (movedUri == null) {
                    try {
                        movedUri = android.provider.DocumentsContract.copyDocument(
                            context.contentResolver,
                            sourceUri,
                            targetParentUri
                        )
                        if (movedUri != null) {
                            android.provider.DocumentsContract.deleteDocument(context.contentResolver, sourceUri)
                            AppLogger.d(TAG, "Move succeeded via copyDocument fallback")
                        }
                    } catch (e: Exception) {
                        AppLogger.w(TAG, "DocumentsContract.copyDocument failed, trying manual copy fallback", e)
                    }
                }

                if (movedUri == null) {
                    try {
                        movedUri = copyNodeRecursively(sourceUri, targetParentUri)
                        if (movedUri != null) {
                            deleteFile(sourceUriString)
                            AppLogger.d(TAG, "Move succeeded via manual copy fallback")
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Manual copy fallback failed", e)
                    }
                }

                movedUri?.let {
                    val movedFile = DocumentFile.fromSingleUri(context, it)
                    FileNode(
                        uri = it.toString(),
                        name = movedFile?.name ?: sourceFile?.name ?: "Unknown",
                        isDirectory = movedFile?.isDirectory ?: sourceFile?.isDirectory ?: false
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to move file: $sourceUriString from $sourceParentUriString to $targetParentUriString", e)
                null
            }
        }
    }

    /**
     * Helper to get a DocumentFile that correctly handles both tree roots and subfolders.
     * Subfolders within a tree need to be opened using buildDocumentUriUsingTree to support
     * directory operations like listFiles() and createFile().
     */
    private fun getDocumentFile(uri: Uri): DocumentFile? {
        return if (android.provider.DocumentsContract.isTreeUri(uri)) {
            DocumentFile.fromTreeUri(context, uri)
        } else {
            // Check if it's a document within a tree
            val isDocumentUri = android.provider.DocumentsContract.isDocumentUri(context, uri)
            val path = uri.path ?: ""
            if (isDocumentUri && path.contains("/tree/")) {
                // It's a document URI that belongs to a tree. 
                // We convert it to a Tree URI to allow directory operations.
                val treeId = path.substringAfter("/tree/").substringBefore("/document/")
                val documentId = android.provider.DocumentsContract.getDocumentId(uri)
                val treeUri = android.provider.DocumentsContract.buildTreeDocumentUri(uri.authority, treeId)
                val subFolderUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                DocumentFile.fromTreeUri(context, subFolderUri)
            } else {
                DocumentFile.fromSingleUri(context, uri)
            }
        }
    }
    
    private fun detectLanguageId(name: String): String = when {
        name.endsWithAny(AppConstants.FileTypes.KOTLIN_EXTENSIONS) -> AppConstants.LanguageIds.KOTLIN
        name.endsWithAny(AppConstants.FileTypes.JAVA_EXTENSIONS) -> AppConstants.LanguageIds.JAVA
        name.endsWithAny(AppConstants.FileTypes.JSON_EXTENSIONS) -> AppConstants.LanguageIds.JSON
        name.endsWithAny(AppConstants.FileTypes.XML_EXTENSIONS) -> AppConstants.LanguageIds.XML
        name.endsWithAny(AppConstants.FileTypes.MARKDOWN_EXTENSIONS) -> AppConstants.LanguageIds.MARKDOWN
        name.endsWithAny(AppConstants.FileTypes.HTML_EXTENSIONS) -> AppConstants.LanguageIds.HTML
        name.endsWithAny(AppConstants.FileTypes.CSS_EXTENSIONS) -> AppConstants.LanguageIds.CSS
        name.endsWithAny(AppConstants.FileTypes.JS_EXTENSIONS) -> AppConstants.LanguageIds.JAVASCRIPT
        name.endsWithAny(AppConstants.FileTypes.TS_EXTENSIONS) -> AppConstants.LanguageIds.TYPESCRIPT
        name.endsWithAny(AppConstants.FileTypes.PYTHON_EXTENSIONS) -> AppConstants.LanguageIds.PYTHON
        name.endsWithAny(AppConstants.FileTypes.C_EXTENSIONS) -> AppConstants.LanguageIds.C
        name.endsWithAny(AppConstants.FileTypes.CPP_EXTENSIONS) -> AppConstants.LanguageIds.CPP
        else -> AppConstants.LanguageIds.PLAINTEXT
    }
    
    private fun detectMimeType(name: String): String = when {
        name.endsWithAny(AppConstants.FileTypes.KOTLIN_EXTENSIONS) -> AppConstants.FileTypes.MIME_KOTLIN
        name.endsWithAny(AppConstants.FileTypes.JAVA_EXTENSIONS) -> AppConstants.FileTypes.MIME_JAVA
        name.endsWithAny(AppConstants.FileTypes.JSON_EXTENSIONS) -> AppConstants.FileTypes.MIME_JSON
        name.endsWithAny(AppConstants.FileTypes.XML_EXTENSIONS) -> AppConstants.FileTypes.MIME_XML
        name.endsWithAny(AppConstants.FileTypes.MARKDOWN_EXTENSIONS) -> AppConstants.FileTypes.MIME_MARKDOWN
        name.endsWithAny(AppConstants.FileTypes.HTML_EXTENSIONS) -> AppConstants.FileTypes.MIME_HTML
        name.endsWithAny(AppConstants.FileTypes.CSS_EXTENSIONS) -> AppConstants.FileTypes.MIME_CSS
        name.endsWithAny(AppConstants.FileTypes.JS_EXTENSIONS) -> AppConstants.FileTypes.MIME_JS
        name.endsWithAny(AppConstants.FileTypes.PYTHON_EXTENSIONS) -> AppConstants.FileTypes.MIME_PYTHON
        name.endsWithAny(AppConstants.FileTypes.C_EXTENSIONS) -> AppConstants.FileTypes.MIME_C
        name.endsWithAny(AppConstants.FileTypes.CPP_EXTENSIONS) -> AppConstants.FileTypes.MIME_CPP
        name.startsWith(".") -> AppConstants.FileTypes.MIME_ANY
        else -> AppConstants.FileTypes.MIME_PLAIN
    }
    
    private fun String.endsWithAny(extensions: Set<String>): Boolean = 
        extensions.any { endsWith(it, ignoreCase = true) }

    private suspend fun copyNodeRecursively(sourceUri: Uri, targetParentUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = DocumentFile.fromSingleUri(context, sourceUri) ?: return@withContext null
                val targetParentFile = getDocumentFile(targetParentUri) ?: return@withContext null

                if (sourceFile.isDirectory) {
                    val newDir = targetParentFile.createDirectory(sourceFile.name!!) ?: return@withContext null
                    sourceFile.listFiles().forEach { child ->
                        copyNodeRecursively(child.uri, newDir.uri)
                    }
                    newDir.uri
                } else {
                    val mimeType = sourceFile.type ?: detectMimeType(sourceFile.name ?: "")
                    val newFile = targetParentFile.createFile(mimeType, sourceFile.name!!) ?: return@withContext null
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    newFile.uri
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to copy node recursively: $sourceUri", e)
                null
            }
        }
    }
}
