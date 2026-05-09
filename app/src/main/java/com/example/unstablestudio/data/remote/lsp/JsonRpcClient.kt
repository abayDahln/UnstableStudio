package com.example.unstablestudio.data.remote.lsp

import com.example.unstablestudio.core.common.AppLogger
import com.example.unstablestudio.core.config.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "JsonRpcClient"

/**
 * Implementasi JSON-RPC Client untuk LSP.
 * Mendukung Stream I/O - cocok untuk stdin/stdout server LSP lokal maupun Socket.
 * 
 * @param inputStream Stream untuk membaca dari server
 * @param outputStream Stream untuk menulis ke server
 * @param scope CoroutineScope yang dikelola dari luar (misal dari Service/ViewModel)
 */
class JsonRpcClient(
    private val inputStream: InputStream,
    private val outputStream: OutputStream,
    private val scope: CoroutineScope
) : LspClient {

    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private val _incomingMessages = MutableSharedFlow<JsonRpcMessage>(replay = 10)
    override val incomingMessages: SharedFlow<JsonRpcMessage> = _incomingMessages.asSharedFlow()

    private val messageIdCounter = AtomicInteger(1)
    @Volatile
    private var isConnected = false
    private var readJob: Job? = null

    override suspend fun connect() {
        if (isConnected) {
            AppLogger.w(TAG, "Already connected, ignoring duplicate connect call")
            return
        }
        
        isConnected = true
        AppLogger.d(TAG, "Starting JSON-RPC connection")

        readJob = scope.launch(Dispatchers.IO) {
            try {
                val reader = inputStream.bufferedReader()
                while (isConnected) {
                    var line = reader.readLine() ?: break
                    var contentLength = -1

                    // Parse headers sesuai spesifikasi LSP Base Protocol
                    while (line.isNotEmpty()) {
                        if (line.startsWith("Content-Length: ", ignoreCase = true)) {
                            val valuePart = line.substringAfter("Content-Length: ", missingDelimiterValue = "")
                            contentLength = valuePart.substringBefore("\r", missingDelimiterValue = valuePart).trim().toIntOrNull() ?: -1
                        }
                        line = reader.readLine() ?: break
                    }

                    if (contentLength > 0) {
                        val buffer = CharArray(contentLength)
                        var read = 0
                        while (read < contentLength) {
                            val r = reader.read(buffer, read, contentLength - read)
                            if (r == -1) break
                            read += r
                        }

                        val payload = String(buffer, 0, read)
                        val message = json.decodeFromString<JsonRpcMessage>(payload)
                        _incomingMessages.emit(message)
                    }
                }
            } catch (e: Exception) {
                if (isConnected) {
                    AppLogger.e(TAG, "Error reading from LSP server", e)
                }
            } finally {
                isConnected = false
                AppLogger.d(TAG, "JSON-RPC read loop ended")
            }
        }
    }

    override suspend fun disconnect() {
        if (!isConnected) {
            AppLogger.w(TAG, "Not connected, ignoring disconnect")
            return
        }
        
        isConnected = false
        readJob?.cancel()
        
        try {
            inputStream.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to close input stream", e)
        }
        
        try {
            outputStream.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to close output stream", e)
        }
        
        AppLogger.d(TAG, "Disconnected from LSP server")
    }

    override suspend fun sendRequest(method: String, params: JsonElement?): Int {
        val id = messageIdCounter.getAndIncrement()
        val message = JsonRpcMessage(id = id, method = method, params = params)
        writeMessage(message)
        AppLogger.d(TAG, "Sent request: $method (id=$id)")
        return id
    }

    override suspend fun sendNotification(method: String, params: JsonElement?) {
        val message = JsonRpcMessage(method = method, params = params)
        writeMessage(message)
        AppLogger.d(TAG, "Sent notification: $method")
    }

    private fun writeMessage(message: JsonRpcMessage) {
        try {
            val payload = json.encodeToString(message)
            val payloadBytes = payload.toByteArray(Charsets.UTF_8)
            val header = "Content-Length: ${payloadBytes.size}\r\n\r\n"
            outputStream.write(header.toByteArray(Charsets.UTF_8))
            outputStream.write(payloadBytes)
            outputStream.flush()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to write message", e)
        }
    }
    
    override suspend fun initialize(
        processId: Int?,
        rootUri: String?,
        capabilities: JsonElement
    ): JsonElement? {
        AppLogger.d(TAG, "Sending LSP initialize request")
        
        val processIdElement = processId?.let { JsonPrimitive(it) }
        val rootUriElement = rootUri?.let { JsonPrimitive(it) }
        
        val params = buildJsonObject {
            if (processIdElement != null) put("processId", processIdElement as JsonElement)
            if (rootUriElement != null) put("rootUri", rootUriElement as JsonElement)
            put("capabilities", capabilities as JsonElement)
            put("clientInfo", buildJsonObject {
                put("name", JsonPrimitive("UnstableStudio") as JsonElement)
                put("version", JsonPrimitive("1.0.0") as JsonElement)
            })
        }
        
        val requestId = sendRequest(AppConstants.Lsp.METHOD_INITIALIZE, params)
        
        // Wait for response using incoming messages flow
        return try {
            var response: JsonRpcMessage? = null
            incomingMessages.collect { msg ->
                if (msg.id == requestId && msg.result != null) {
                    response = msg
                    return@collect
                }
            }
            response?.result
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to receive initialize response", e)
            null
        }
    }
}
