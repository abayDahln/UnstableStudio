package com.example.unstablestudio.domain.model

data class Document(
    val id: String,
    val title: String,
    val content: String,
    val languageId: String,
    val isModified: Boolean = false,
    val isMissingFromWorkspace: Boolean = false
)
