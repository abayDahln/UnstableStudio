package com.example.unstablestudio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unstablestudio.core.common.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URLDecoder


private const val TAG = "TerminalViewModel"

// Sentinel printed after every command to track current directory.
// Must be unique enough to never appear in normal command output.
private const val PWD_MARKER = "__UNSTABLE_PWD_MARKER__="

// Sent once after the shell initialises so we know it is ready to accept input.
private const val READY_MARKER = "__UNSTABLE_SHELL_READY__"

// Maximum lines kept in the terminal output buffer.
private const val MAX_OUTPUT_LINES = 2000

class TerminalViewModel(private val runtimeDir: File) : ViewModel() {

    // ──────────────────────────────────────────────────────────
    // Public state
    // ──────────────────────────────────────────────────────────

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private val _currentDirectory = MutableStateFlow("/root")
    val currentDirectory: StateFlow<String> = _currentDirectory.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()
    private var historyIndex = -1

    fun getPreviousCommand(): String? {
        val history = _commandHistory.value
        if (history.isEmpty()) return null
        if (historyIndex < history.size - 1) {
            historyIndex++
        }
        return history[history.size - 1 - historyIndex]
    }

    fun getNextCommand(): String? {
        val history = _commandHistory.value
        if (history.isEmpty() || historyIndex < 0) return null
        if (historyIndex > 0) {
            historyIndex--
            return history[history.size - 1 - historyIndex]
        }
        historyIndex = -1
        return ""
    }

    fun sendCtrlC() {
        viewModelScope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                try {
                    writer?.write("\u0003")
                    writer?.flush()
                    processOutputChunk("^C\n")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to send Ctrl+C", e)
                }
            }
        }
    }

    /** True while the shell is still initialising (before READY_MARKER received). */
    private val _isStarting = MutableStateFlow(true)
    val isStarting: StateFlow<Boolean> = _isStarting.asStateFlow()

    // ──────────────────────────────────────────────────────────
    // Internal state  (all writes must happen on IO dispatcher)
    // ──────────────────────────────────────────────────────────

    private var process: Process? = null
    private var writer: BufferedWriter? = null

    /**
     * Queue of commands waiting for the shell to become ready.
     * Protected by [writeMutex] – coroutine-safe, no blocking `synchronized`.
     */
    private val pendingCommands = ArrayDeque<String>()
    private val writeMutex = Mutex()

    /** Set to true only after READY_MARKER has been received from the shell. */
    @Volatile private var isShellReady = false

    private var readerJob: Job? = null
    private var lastWorkspaceUri: String? = null

    // ──────────────────────────────────────────────────────────
    // Init
    // ──────────────────────────────────────────────────────────

    init {
        startShell()
    }

    // ──────────────────────────────────────────────────────────
    // Shell lifecycle
    // ──────────────────────────────────────────────────────────

    private fun startShell() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pb = buildProcessBuilder()
                pb.redirectErrorStream(true)

                val proc = pb.start()
                process = proc
                writer  = BufferedWriter(OutputStreamWriter(proc.outputStream))

                // Start reading output in a separate job so that sendCommand()
                // can run concurrently and is never blocked by the reader loop.
                readerJob = viewModelScope.launch(Dispatchers.IO) {
                    runReaderLoop(InputStreamReader(proc.inputStream))
                }

                // Send the bootstrap environment setup.
                // The shell is NOT yet marked ready – we wait for READY_MARKER first.
                sendBootstrapAndReadyProbe()

            } catch (e: CancellationException) {
                throw e // always re-throw cancellation
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to start shell", e)
                processOutputChunk("Error: Could not start shell process — ${e.message}\n")
                _isStarting.value = false
            }
        }
    }

    /**
     * Continuous reader loop running in its own coroutine.
     * Handles:
     *   - READY_MARKER → mark shell ready, flush pending commands
     *   - PWD_MARKER   → update current-directory state
     *   - Everything else → append to visible output
     */
    private suspend fun runReaderLoop(reader: InputStreamReader) {
        try {
            val buffer = CharArray(4096)
            var read: Int
            while (withContext(Dispatchers.IO) { reader.read(buffer) }.also { read = it } != -1) {
                val chunk = String(buffer, 0, read)
                processOutputChunk(chunk)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "Reader loop ended", e)
        }
    }

    /**
     * Called once when READY_MARKER is received.
     * Marks the shell as ready and dispatches any queued commands.
     */
    private suspend fun onShellReady() {
        isShellReady = true
        _isStarting.value = false
        processOutputChunk("Shell ready ✓\n")

        // Flush every command that was sent before the shell was ready.
        writeMutex.withLock {
            while (pendingCommands.isNotEmpty()) {
                writeRaw(pendingCommands.removeFirst())
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────

    /**
     * Send a user command.  Safe to call at any time — if the shell isn't
     * ready yet the command is queued and flushed automatically once it is.
     */
    fun sendCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        // Update history
        val currentHistory = _commandHistory.value.toMutableList()
        currentHistory.remove(trimmed)
        currentHistory.add(trimmed)
        if (currentHistory.size > 100) currentHistory.removeAt(0)
        _commandHistory.value = currentHistory
        historyIndex = -1

        // Echo the command into the output immediately for responsiveness.
        val prompt = formatPromptDirectory(_currentDirectory.value)
        processOutputChunk("$prompt $ $trimmed\n")

        viewModelScope.launch(Dispatchers.IO) {
            writeMutex.withLock {
                if (!isShellReady) {
                    pendingCommands.addLast(trimmed)
                } else {
                    writeRaw(trimmed)
                }
            }
        }
    }

    /**
     * Automatically `cd` into the workspace directory when the user opens a project.
     * Only triggers once per unique URI.
     */
    fun setWorkspaceUri(uri: String?) {
        if (uri == null || uri == lastWorkspaceUri) return
        lastWorkspaceUri = uri

        try {
            val decoded = URLDecoder.decode(uri, "UTF-8")
            val pathSegment = decoded.substringAfter("primary:", "")
            if (pathSegment.isNotEmpty()) {
                sendCommand("cd \"/sdcard/$pathSegment\"")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse workspace URI", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        readerJob?.cancel()
        try {
            writer?.write("exit\n")
            writer?.flush()
        } catch (_: Exception) {}
        writer?.close()
        process?.destroyForcibly()
    }

    // ──────────────────────────────────────────────────────────
    // Process builder helpers
    // ──────────────────────────────────────────────────────────

    private fun buildProcessBuilder(): ProcessBuilder {
        val prootFile = File(runtimeDir, "proot")
        val rootfsDir = File(runtimeDir, "rootfs")

        return if (prootFile.exists() && rootfsDir.exists()) {
            prootFile.setExecutable(true) // Ensure it has execution permissions
            
            val shellPath = resolveShellPath(rootfsDir)
            val rootHome = File(rootfsDir, "root")
            if (!rootHome.exists()) rootHome.mkdirs()

            val pb = ProcessBuilder(
                prootFile.absolutePath,
                "--link2symlink",
                "-0",
                "-r", rootfsDir.absolutePath,
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-b", "/sdcard:/sdcard",
                "-b", "/storage:/storage",
                "-w", "/root",
                shellPath,
                "-l"
            )
            
            val env = pb.environment()
            env.clear()
            val tmpDir = File(runtimeDir, "tmp")
            if (!tmpDir.exists()) tmpDir.mkdirs()
            env["PROOT_TMP_DIR"] = tmpDir.absolutePath
            env["HOME"] = "/root"
            env["PREFIX"] = "/usr"
            env["TERM"] = "xterm-256color"
            env["LANG"] = "C.UTF-8"
            env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

            pb
        } else {
            ProcessBuilder("/system/bin/sh")
        }
    }

    private fun resolveShellPath(rootfsDir: File): String {
        val candidates = listOf(
            "/bin/bash",
            "/usr/bin/bash",
            "/bin/sh",
            "/usr/bin/sh"
        )
        for (candidate in candidates) {
            val file = File(rootfsDir, candidate.removePrefix("/"))
            if (file.exists() && file.canExecute()) return candidate
        }
        return "/bin/sh"
    }

    // ──────────────────────────────────────────────────────────
    // Write helpers
    // ──────────────────────────────────────────────────────────

    /**
     * Bootstrap environment then send a probe command whose output contains
     * READY_MARKER.  The reader loop will detect this and mark the shell ready.
     *
     * Each line is a discrete write so partial-flush failures are isolated.
     */
    private fun sendBootstrapAndReadyProbe() {
        writeRaw("export PREFIX=/usr")
        writeRaw("export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:\$PATH")
        writeRaw("export PS1='\\u@unstable:\\w\\$ '")
        writeRaw("hash -r 2>/dev/null || true")
        // Probe: echo the ready marker so runReaderLoop() knows the shell is alive.
        writeRaw("echo '$READY_MARKER'")
    }

    /**
     * Write a command to the shell stdin, automatically appending a PWD probe
     * so we can track directory changes without blocking.
     *
     * MUST be called from [writeMutex] context or before the reader loop starts.
     */
    private fun writeRaw(command: String) {
        val w = writer ?: return
        try {
            // Append the PWD probe after the command finishes.
            // Using a subshell `(...)` ensures the probe always runs even if the
            // command exits with a non-zero status.
            val line = "$command; printf '$PWD_MARKER%s\\n' \"\$PWD\""
            w.write("$line\n")
            w.flush()
        } catch (e: Exception) {
            AppLogger.e(TAG, "writeRaw failed: $command", e)
        }
    }

    // ──────────────────────────────────────────────────────────
    // Output helpers
    // ──────────────────────────────────────────────────────────

    private fun processOutputChunk(chunk: String) {
        val currentLines = _terminalOutput.value.toMutableList()
        if (currentLines.isEmpty()) {
            currentLines.add("")
        }

        var lastLine = currentLines.last()
        var i = 0
        while (i < chunk.length) {
            val c = chunk[i]
            
            // Fast ANSI escape skipping
            if (c == '\u001B') {
                var j = i + 1
                if (j < chunk.length && chunk[j] == '[') {
                    j++
                    while (j < chunk.length && chunk[j] in '\u0030'..'\u003F') j++
                    while (j < chunk.length && chunk[j] in '\u0020'..'\u002F') j++
                    if (j < chunk.length && chunk[j] in '\u0040'..'\u007E') {
                        val seq = chunk.substring(i, j + 1)
                        if (seq == "\u001B[K" || seq == "\u001B[2K") {
                            lastLine = ""
                        }
                        i = j + 1
                        continue
                    }
                } else if (j < chunk.length && chunk[j] == ']') {
                    var belFound = false
                    while (j < chunk.length) {
                        if (chunk[j] == '\u0007') {
                            belFound = true
                            break
                        }
                        j++
                    }
                    if (belFound) {
                        i = j + 1
                        continue
                    }
                }
                // Skip the ESC character if not matched
                i++
                continue
            }

            when (c) {
                '\r' -> {
                    lastLine = "" // Clear line for progress bars
                }
                '\n' -> {
                    var skipLine = false
                    if (lastLine.contains(READY_MARKER)) {
                        viewModelScope.launch { onShellReady() }
                        lastLine = lastLine.replace(READY_MARKER, "").trim()
                        if (lastLine.isEmpty()) skipLine = true
                    }
                    if (lastLine.startsWith(PWD_MARKER)) {
                        val pwd = lastLine.removePrefix(PWD_MARKER).trim()
                        if (pwd.isNotEmpty()) _currentDirectory.value = pwd
                        skipLine = true
                    }

                    if (!skipLine) {
                        currentLines[currentLines.size - 1] = lastLine
                        currentLines.add("")
                    } else {
                        currentLines[currentLines.size - 1] = ""
                    }
                    lastLine = ""
                }
                '\u0008' -> {
                    if (lastLine.isNotEmpty()) lastLine = lastLine.dropLast(1)
                }
                '\u0000' -> {} // ignore
                else -> {
                    lastLine += c
                }
            }
            i++
        }
        
        currentLines[currentLines.size - 1] = lastLine
        
        if (currentLines.size > MAX_OUTPUT_LINES) {
            val dropCount = currentLines.size - MAX_OUTPUT_LINES
            _terminalOutput.value = currentLines.drop(dropCount)
        } else {
            _terminalOutput.value = currentLines
        }
    }

    // ──────────────────────────────────────────────────────────
    // Prompt formatting  (pure, no side effects)
    // ──────────────────────────────────────────────────────────

    private fun formatPromptDirectory(path: String): String {
        val normalised = if (path.startsWith("/root")) path.replaceFirst("/root", "~") else path
        if (normalised == "/" || normalised == "~") return normalised

        val hasHomePrefix = normalised.startsWith("~")
        val cleaned = normalised.trim('/').removePrefix("~/")
        val parts = cleaned.split("/").filter { it.isNotBlank() }
        if (parts.isEmpty()) return if (hasHomePrefix) "~" else "/"

        val tail = parts.takeLast(2).joinToString("/")
        val hasMore = parts.size > 2
        return when {
            hasHomePrefix && hasMore -> "~.../$tail"
            hasHomePrefix            -> "~/$tail"
            hasMore                  -> ".../$tail"
            else                     -> "/$tail"
        }
    }
}
