package com.example.unstablestudio.data.repository

import com.example.unstablestudio.domain.model.Document
import com.example.unstablestudio.domain.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDocumentRepository : DocumentRepository {

    // Dummy data in memory for draft implementation
    private val mockStorage = mutableMapOf(
        "file1" to Document(
            id = "file1",
            title = "MainActivity.kt",
            content = "package com.example.unstablestudio\n\nfun main() {\n    println(\"Hello, Unstable Studio!\")\n}\n",
            languageId = "kotlin"
        )
    )

    override suspend fun loadDocument(id: String): Document {
        return withContext(Dispatchers.IO) {
            mockStorage[id] ?: throw Exception("File not found")
        }
    }

    override suspend fun saveDocument(document: Document) {
        withContext(Dispatchers.IO) {
            mockStorage[document.id] = document.copy(isModified = false)
            // Here you would normally write to the actual File system using File IO or SAF
        }
    }

    override suspend fun listDirectory(uriString: String): List<com.example.unstablestudio.domain.model.FileNode> {
        return withContext(Dispatchers.IO) {
            emptyList()
        }
    }

    override suspend fun createFile(parentUriString: String, name: String, isDirectory: Boolean): com.example.unstablestudio.domain.model.FileNode? {
        return withContext(Dispatchers.IO) {
            null
        }
    }

    override suspend fun deleteFile(uriString: String): Boolean {
        return withContext(Dispatchers.IO) {
            false
        }
    }

    override suspend fun renameFile(uriString: String, newName: String): com.example.unstablestudio.domain.model.FileNode? {
        return withContext(Dispatchers.IO) {
            null
        }
    }

    override suspend fun moveFile(sourceUriString: String, sourceParentUriString: String, targetParentUriString: String): com.example.unstablestudio.domain.model.FileNode? {
        return withContext(Dispatchers.IO) {
            null
        }
    }
}