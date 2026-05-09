package com.example.unstablestudio.data.remote.lsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Model dasar untuk JSON-RPC 2.0 (Language Server Protocol).
 */

@Serializable
data class JsonRpcMessage(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String? = null,
    val params: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class InitializeParams(
    val processId: Int?,
    val rootUri: String?,
    val capabilities: JsonElement
)

@Serializable
data class CompletionItem(
    val label: String,
    val kind: Int? = null,
    val detail: String? = null,
    val documentation: JsonElement? = null, // Can be string or MarkupContent
    val insertText: String? = null
)

@Serializable
data class Position(
    val line: Int,
    val character: Int
)

@Serializable
data class Range(
    val start: Position,
    val end: Position
)

