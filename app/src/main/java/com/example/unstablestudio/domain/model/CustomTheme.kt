package com.example.unstablestudio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomTheme(
    val id: String,
    val name: String,
    val primary: String,
    val surface: String,
    val background: String,
    val isSystem: Boolean = false
)
