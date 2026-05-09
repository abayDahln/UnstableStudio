package com.example.unstablestudio.data.remote.lsp

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.JsonElement

/**
 * Antarmuka komunikasi dengan Language Server.
 */
interface LspClient {

    /**
     * Aliran data masuk (responses dan notifications) dari Language Server.
     */
    val incomingMessages: SharedFlow<JsonRpcMessage>

    /**
     * Memulai koneksi ke Language Server (melalui proses lokal, socket, atau stdin/stdout).
     */
    suspend fun connect()

    /**
     * Memutus koneksi ke Language Server.
     */
    suspend fun disconnect()

    /**
     * Mengirim *Request* (membutuhkan respons).
     */
    suspend fun sendRequest(method: String, params: JsonElement?): Int

    /**
     * Mengirim *Notification* (tidak membutuhkan respons).
     */
    suspend fun sendNotification(method: String, params: JsonElement?)
    
    /**
     * Initialize handshake dengan LSP server.
     * Harus dipanggil sekali setelah koneksi dan sebelum request lainnya.
     */
    suspend fun initialize(
        processId: Int?,
        rootUri: String?,
        capabilities: JsonElement
    ): JsonElement?
}
