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
            try {
                val uri = Uri.parse(uriString)
                val root = DocumentFile.fromTreeUri(context, uri)
                    ?: return@withContext emptyList()

                root.listFiles().mapNotNull { file ->
                    FileNode(
                        uri = file.uri.toString(),
                        name = file.name ?: "Unknown",
                        isDirectory = file.isDirectory
                    )
                }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to list directory: $uriString", e)
                emptyList()
            }
        }
    }

    override suspend fun createFile(parentUriString: String, name: String, isDirectory: Boolean): FileNode? {
        return withContext(Dispatchers.IO) {
            try {
                val parentUri = Uri.parse(parentUriString)
                val parentFolder = DocumentFile.fromTreeUri(context, parentUri) ?: return@withContext null

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
                // If we are moving to/from the root of a tree, we must convert the Tree URI to a Document URI.
                if (android.provider.DocumentsContract.isTreeUri(targetParentUri)) {
                    val documentId = android.provider.DocumentsContract.getTreeDocumentId(targetParentUri)
                    targetParentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(targetParentUri, documentId)
                }
                
                if (android.provider.DocumentsContract.isTreeUri(sourceParentUri)) {
                    val documentId = android.provider.DocumentsContract.getTreeDocumentId(sourceParentUri)
                    sourceParentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(sourceParentUri, documentId)
                }

                val movedUri = android.provider.DocumentsContract.moveDocument(
                    context.contentResolver,
                    sourceUri,
                    sourceParentUri,
                    targetParentUri
                )

                movedUri?.let {
                    val movedFile = DocumentFile.fromSingleUri(context, it)
                    FileNode(
                        uri = it.toString(),
                        name = movedFile?.name ?: "Unknown",
                        isDirectory = movedFile?.isDirectory ?: false
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to move file: $sourceUriString from $sourceParentUriString to $targetParentUriString", e)
                null
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
}
