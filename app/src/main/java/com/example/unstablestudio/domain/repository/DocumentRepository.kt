package com.example.unstablestudio.domain.repository

import com.example.unstablestudio.domain.model.Document
import com.example.unstablestudio.domain.model.FileNode

interface DocumentRepository {
    suspend fun loadDocument(id: String): Document
    suspend fun saveDocument(document: Document)
    suspend fun listDirectory(uriString: String): List<FileNode>
    suspend fun createFile(parentUriString: String, name: String, isDirectory: Boolean): FileNode?
    suspend fun deleteFile(uriString: String): Boolean
    suspend fun renameFile(uriString: String, newName: String): FileNode?
    suspend fun moveFile(sourceUriString: String, sourceParentUriString: String, targetParentUriString: String): FileNode?
}