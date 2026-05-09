package com.example.unstablestudio.domain.model

data class FileNode(
    val uri: String,
    val name: String,
    val isDirectory: Boolean,
    val children: List<FileNode>? = null,
    val isExpanded: Boolean = false
)