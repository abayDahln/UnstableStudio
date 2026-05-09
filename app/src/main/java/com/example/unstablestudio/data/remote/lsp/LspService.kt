package com.example.unstablestudio.data.remote.lsp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.example.unstablestudio.core.common.AppLogger
import com.example.unstablestudio.core.config.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

private const val TAG = "LspService"

class LspService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var lspClient: JsonRpcClient? = null
        private set

    // Connection state tracking
    private var _isConnected = false
    val isConnected: Boolean get() = _isConnected
    
    private var lastConnectionError: String? = null
    fun getLastConnectionError(): String? = lastConnectionError

    private var lspProcess: Process? = null
    private var lspSocket: Socket? = null

    // Binder for Activity
    inner class LocalBinder : Binder() {
        fun getService(): LspService = this@LspService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = createNotification()
        startForeground(AppConstants.Notifications.LSP_SERVICE_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(AppConstants.PrefKeys.LSP_MODE) ?: AppConstants.Lsp.DEFAULT_MODE

        if (lspClient == null) {
            when (mode) {
                AppConstants.LspModeValues.TCP -> {
                    val host = intent?.getStringExtra(AppConstants.PrefKeys.LSP_HOST) ?: AppConstants.Lsp.DEFAULT_HOST
                    val port = intent?.getIntExtra(AppConstants.PrefKeys.LSP_PORT, AppConstants.Lsp.DEFAULT_PORT) ?: AppConstants.Lsp.DEFAULT_PORT
                    connectToTcpServer(host, port)
                }
                AppConstants.LspModeValues.PROCESS -> {
                    val command = intent?.getStringArrayExtra(AppConstants.PrefKeys.LSP_COMMAND)?.toList()
                        ?: listOf(AppConstants.Lsp.DEFAULT_COMMAND)
                    startProcessServer(command)
                }
            }
        }

        return START_STICKY
    }

    fun connectToTcpServer(host: String, port: Int) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                AppLogger.d(TAG, "Connecting to Language Server at $host:$port")
                lspSocket = Socket(host, port)
                _isConnected = true
                lastConnectionError = null
                
                val inputStream = lspSocket?.getInputStream() ?: return@launch
                val outputStream = lspSocket?.getOutputStream() ?: return@launch

                initClient(inputStream, outputStream)
                AppLogger.d(TAG, "Successfully connected to LSP server at $host:$port")
            } catch (e: java.net.ConnectException) {
                _isConnected = false
                lastConnectionError = "Connection refused: $host:$port. Make sure LSP server is running."
                AppLogger.e(TAG, "Connection refused at $host:$port - LSP server may not be running", e)
            } catch (e: java.net.SocketTimeoutException) {
                _isConnected = false
                lastConnectionError = "Connection timed out: $host:$port"
                AppLogger.e(TAG, "Connection timed out at $host:$port", e)
            } catch (e: Exception) {
                _isConnected = false
                lastConnectionError = "Failed to connect: ${e.message}"
                AppLogger.e(TAG, "Failed to connect to TCP Language Server at $host:$port", e)
            }
        }
    }

    fun startProcessServer(command: List<String>) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                AppLogger.d(TAG, "Starting Language Server Process: ${command.joinToString(" ")}")
                val processBuilder = ProcessBuilder(command)
                processBuilder.redirectErrorStream(true)
                lspProcess = processBuilder.start()
                _isConnected = true
                lastConnectionError = null

                val inputStream = lspProcess?.inputStream ?: return@launch
                val outputStream = lspProcess?.outputStream ?: return@launch

                initClient(inputStream, outputStream)
                AppLogger.d(TAG, "Successfully started LSP process")
            } catch (e: java.io.IOException) {
                _isConnected = false
                lastConnectionError = "Failed to start LSP process: ${e.message}"
                AppLogger.e(TAG, "Failed to start Process Language Server: ${command.joinToString(" ")}", e)
            } catch (e: Exception) {
                _isConnected = false
                lastConnectionError = "Failed to start LSP process: ${e.message}"
                AppLogger.e(TAG, "Failed to start Process Language Server", e)
            }
        }
    }

    private fun initClient(inputStream: InputStream, outputStream: OutputStream) {
        if (lspClient == null) {
            lspClient = JsonRpcClient(
                inputStream = inputStream,
                outputStream = outputStream,
                scope = serviceScope
            )

            serviceScope.launch {
                lspClient?.connect()
            }
            AppLogger.d(TAG, "LSP Client initialized successfully")
        }
    }

    // Callback for connection state changes
    var onConnectionStateChanged: ((Boolean, String?) -> Unit)? = null
        private set
    
    private fun notifyConnectionStateChanged(isConnected: Boolean, error: String?) {
        onConnectionStateChanged?.invoke(isConnected, error)
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        try {
            lspSocket?.close()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to close LSP socket", e)
        }

        try {
            lspProcess?.destroy()
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to destroy Lsp process", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.Notifications.LSP_SERVICE_CHANNEL_ID,
                AppConstants.Notifications.LSP_SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = AppConstants.Notifications.LSP_SERVICE_CHANNEL_DESCRIPTION
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return androidx.core.app.NotificationCompat.Builder(this, AppConstants.Notifications.LSP_SERVICE_CHANNEL_ID)
            .setContentTitle("Unstable Studio LSP")
            .setContentText("Language Server is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
