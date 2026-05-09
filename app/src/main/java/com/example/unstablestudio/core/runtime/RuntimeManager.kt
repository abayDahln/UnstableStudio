package com.example.unstablestudio.core.runtime

import android.content.Context
import android.os.Build
import com.example.unstablestudio.core.common.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

private const val TAG = "RuntimeManager"

// ─── Bootstrap download URLs (Termux bootstrap – includes apt, dpkg, bash) ───
private const val BOOTSTRAP_BASE_URL =
    "https://github.com/termux/termux-packages/releases/latest/download"

// Packages to auto-install after the base rootfs is ready.
// These are Termux package names (different from Debian – e.g. "python" = Python 3).
private val AUTO_INSTALL_PACKAGES = listOf(
    "nodejs",       // Node.js + npm
    "python",       // Python 3.x
    "python-pip",   // pip3
)

// Marker file written inside rootfs when auto-packages are successfully installed.
private const val PACKAGES_MARKER = "root/.unstable_packages_ready"

/**
 * Phase states reported via [status]:
 *
 *  IDLE             → nothing done yet
 *  DOWNLOADING      → downloading bootstrap zip
 *  EXTRACTING       → extracting rootfs
 *  PKG_INSTALLING   → running apt-get to install Node.js / Python
 *  READY            → everything installed, terminal can be used
 *  ERROR: <msg>     → something failed
 */
class RuntimeManager(private val context: Context) {

    private val runtimeDir = File(context.filesDir, "runtime")
    private val rootfsDir  = File(runtimeDir, "rootfs")

    // Independent scope – not tied to any ViewModel/Activity lifecycle
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow("IDLE")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /** Streaming log lines from the package installer (apt output). */
    private val _installLog = MutableStateFlow<List<String>>(emptyList())
    val installLog: StateFlow<List<String>> = _installLog.asStateFlow()

    // ──────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────

    /**
     * Called at app startup.  Idempotent – safe to call multiple times.
     *
     * Flow:
     *  1. If rootfs not present → download + extract bootstrap
     *  2. If packages not installed → run apt-get install in the rootfs
     *  3. Emit READY when done
     */
    fun ensureRuntimeReady() {
        // Avoid double-starting
        if (_status.value in listOf("DOWNLOADING", "EXTRACTING", "PKG_INSTALLING")) return

        managerScope.launch {
            try {
                // ── Step 1: Base rootfs ──────────────────────────────────
                if (!isRootfsInstalled()) {
                    _status.value = "DOWNLOADING"
                    _progress.value = 0f
                    copyProotBinary()
                    val zip = downloadBootstrap()
                    _status.value = "EXTRACTING"
                    extractRuntime(zip)
                }

                // ── Step 2: Node.js + Python ─────────────────────────────
                if (!arePackagesInstalled()) {
                    _status.value = "PKG_INSTALLING"
                    installPackages()
                    markPackagesInstalled()
                }

                _status.value = "READY"
                _progress.value = 1f

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Runtime setup failed", e)
                _status.value = "ERROR: ${e.message}"
            }
        }
    }

    /**
     * Legacy entry-point kept for backward compatibility.
     * Delegates to [ensureRuntimeReady].
     */
    fun ensureRuntimeInstalled() = ensureRuntimeReady()

    /** Reinstall everything from scratch (used by the "Repair" button). */
    fun reinstall() {
        if (_status.value in listOf("DOWNLOADING", "EXTRACTING", "PKG_INSTALLING")) return
        managerScope.launch {
            try {
                _installLog.value = emptyList()
                _status.value = "DOWNLOADING"
                _progress.value = 0f

                if (rootfsDir.exists()) rootfsDir.deleteRecursively()
                copyProotBinary()
                val zip = downloadBootstrap()
                _status.value = "EXTRACTING"
                extractRuntime(zip)

                _status.value = "PKG_INSTALLING"
                installPackages()
                markPackagesInstalled()

                _status.value = "READY"
                _progress.value = 1f
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Reinstall failed", e)
                _status.value = "ERROR: ${e.message}"
            }
        }
    }

    fun isRootfsInstalled(): Boolean {
        if (!rootfsDir.exists() || !rootfsDir.isDirectory) return false
        if (!File(runtimeDir, "proot").exists()) return false
        val shellCandidates = listOf(
            File(rootfsDir, "bin/sh"),
            File(rootfsDir, "usr/bin/sh"),
            File(rootfsDir, "bin/bash"),
            File(rootfsDir, "usr/bin/bash"),
        )
        return shellCandidates.any { it.exists() && it.canExecute() }
    }

    fun arePackagesInstalled(): Boolean =
        File(rootfsDir, PACKAGES_MARKER).exists()

    // ──────────────────────────────────────────────────────────
    // Step 1 helpers – bootstrap download & extraction
    // ──────────────────────────────────────────────────────────

    private fun resolveAbi(): String {
        val abis = Build.SUPPORTED_ABIS.toList()
        return when {
            abis.any { it.contains("arm64") }            -> "aarch64"
            abis.any { it.contains("armeabi-v7a") || it.contains("armeabi") } -> "arm"
            abis.any { it.contains("x86_64") }           -> "x86_64"
            abis.any { it == "x86" }                     -> "i686"
            else -> throw IllegalStateException("Unsupported ABI: $abis")
        }
    }

    private suspend fun copyProotBinary() = withContext(Dispatchers.IO) {
        if (!runtimeDir.exists()) runtimeDir.mkdirs()
        val prootFile = File(runtimeDir, "proot")
        if (!prootFile.exists()) {
            context.assets.open("proots/proot").use { input ->
                FileOutputStream(prootFile).use { output -> input.copyTo(output) }
            }
            prootFile.setExecutable(true)
        }
    }

    /**
     * Downloads `bootstrap-<abi>.zip` from Termux GitHub releases.
     * Reports byte-level progress to [_progress].
     */
    private suspend fun downloadBootstrap(): File = withContext(Dispatchers.IO) {
        val abi       = resolveAbi()
        val fileName  = "bootstrap-$abi.zip"
        val urlString = "$BOOTSTRAP_BASE_URL/$fileName"

        appendLog("⬇ Downloading $fileName …")

        if (!runtimeDir.exists()) runtimeDir.mkdirs()
        val destFile = File(runtimeDir, "bootstrap.zip")
        if (destFile.exists()) destFile.delete()

        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout    = 60_000
        connection.connect()

        val totalBytes = connection.contentLengthLong.toFloat().coerceAtLeast(1f)
        var downloaded = 0L

        connection.inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    _progress.value = (downloaded / totalBytes).coerceIn(0f, 0.4f)
                }
            }
        }

        appendLog("✓ Download complete")
        destFile
    }

    private suspend fun extractRuntime(zipFile: File) = withContext(Dispatchers.IO) {
        if (!zipFile.exists()) throw IllegalStateException("Bootstrap zip not found")

        appendLog("📦 Extracting rootfs …")
        if (rootfsDir.exists()) rootfsDir.deleteRecursively()
        rootfsDir.mkdirs()

        val symlinkLines = mutableListOf<String>()

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            var count = 0
            while (entry != null) {
                val name = normalizeZipEntryPath(entry.name)
                if (name.isNotEmpty()) {
                    val dest = File(rootfsDir, name)
                    if (entry.isDirectory) {
                        dest.mkdirs()
                    } else if (name == "SYMLINKS.txt") {
                        symlinkLines.addAll(zis.bufferedReader().readLines())
                    } else {
                        dest.parentFile?.mkdirs()
                        FileOutputStream(dest).use { zis.copyTo(it) }
                        if (shouldBeExecutable(name)) dest.setExecutable(true)
                    }
                    count++
                    if (count % 200 == 0) {
                        _progress.value = (0.4f + count / 8000f).coerceIn(0.4f, 0.7f)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        applySymlinks(symlinkLines)
        zipFile.delete()
        _progress.value = 0.75f
        appendLog("✓ Rootfs ready")
    }

    // ──────────────────────────────────────────────────────────
    // Step 2 helpers – package installation
    // ──────────────────────────────────────────────────────────

    /**
     * Runs `apt-get update` then `apt-get install` for all [AUTO_INSTALL_PACKAGES]
     * inside the proot environment, streaming output to [_installLog].
     */
    private suspend fun installPackages() = withContext(Dispatchers.IO) {
        appendLog("📦 Setting up package manager …")
        // Update package lists first
        runProotCommand(
            "DEBIAN_FRONTEND=noninteractive apt-get update -y",
            label = "apt update"
        )
        _progress.value = 0.82f

        appendLog("🟢 Installing Node.js, Python …")
        val pkgList = AUTO_INSTALL_PACKAGES.joinToString(" ")
        runProotCommand(
            "DEBIAN_FRONTEND=noninteractive apt-get install -y $pkgList",
            label = "apt install"
        )
        _progress.value = 0.97f
        appendLog("✓ Node.js & Python installed!")
    }

    /**
     * Runs a single shell command inside proot and streams every output line
     * to [_installLog].  Throws if the process exits with a non-zero code.
     */
    private fun runProotCommand(command: String, label: String) {
        val prootFile = File(runtimeDir, "proot")
        if (!prootFile.exists()) throw IllegalStateException("proot binary missing")
        prootFile.setExecutable(true)

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
            "-w", "/root",
            "/bin/sh", "-c", command
        )
        
        val env = pb.environment()
        env.clear()
        val tmpDir = File(runtimeDir, "tmp")
        if (!tmpDir.exists()) tmpDir.mkdirs()
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath
        env["HOME"] = "/root"
        env["PREFIX"] = "/usr"
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        env["LANG"] = "C.UTF-8"
        env["DEBIAN_FRONTEND"] = "noninteractive"
        pb.redirectErrorStream(true)

        val proc = pb.start()

        // Stream stdout + stderr to the log in real-time
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val cleaned = cleanAnsi(line!!)
            if (cleaned.isNotBlank()) appendLog(cleaned)
        }

        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("$label failed with exit code $exitCode")
        }
    }

    private fun markPackagesInstalled() {
        val marker = File(rootfsDir, PACKAGES_MARKER)
        marker.parentFile?.mkdirs()
        marker.createNewFile()
    }

    // ──────────────────────────────────────────────────────────
    // Zip helpers
    // ──────────────────────────────────────────────────────────

    private fun normalizeZipEntryPath(rawPath: String): String {
        var path = rawPath.replace('\\', '/').trim()
        if (path.isEmpty() || path == ".") return ""
        while (path.startsWith("./")) path = path.removePrefix("./")
        val termuxPrefix = "data/data/com.termux/files/"
        if (path.startsWith(termuxPrefix)) path = path.removePrefix(termuxPrefix)
        path = path.trimStart('/')
        if (path.contains("..")) return ""
        return path
    }

    private fun shouldBeExecutable(path: String): Boolean =
        path.startsWith("bin/") ||
        path.startsWith("usr/bin/") ||
        path.startsWith("usr/libexec/") ||
        path.startsWith("lib/ld")

    private fun applySymlinks(lines: List<String>) {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            try {
                val (rawTarget, rawLink) = when {
                    trimmed.contains('←') -> {
                        val parts = trimmed.split('←', limit = 2)
                        if (parts.size != 2) continue
                        parts[0] to parts[1]
                    }
                    trimmed.contains("->") -> {
                        val parts = trimmed.split("->", limit = 2)
                        if (parts.size != 2) continue
                        parts[0] to parts[1]
                    }
                    else -> continue
                }

                val target = normalizeZipEntryPath(rawTarget.trim())
                val link   = normalizeZipEntryPath(rawLink.trim())
                if (target.isEmpty() || link.isEmpty()) continue

                val linkFile = File(rootfsDir, link)
                linkFile.parentFile?.mkdirs()
                if (linkFile.exists()) linkFile.delete()

                try {
                    Files.createSymbolicLink(linkFile.toPath(), Path.of(target))
                } catch (_: Exception) {
                    val src = File(rootfsDir, target)
                    if (src.exists() && src.isFile) {
                        src.copyTo(linkFile, overwrite = true)
                        linkFile.setExecutable(src.canExecute())
                    }
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Skipping symlink: $line", e)
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Log helpers
    // ──────────────────────────────────────────────────────────

    private fun appendLog(line: String) {
        val current = _installLog.value
        _installLog.value = if (current.size > 500) {
            current.drop(current.size - 499) + line
        } else {
            current + line
        }
    }

    private fun cleanAnsi(raw: String): String = raw
        .replace(Regex("\u001B\\[[;\\d]*[ -/]*[@-~]"), "")
        .replace(Regex("\u001B][^\u0007]*\u0007"), "")
        .replace(Regex("\u001B."), "")
        .replace("\u0000", "")
        .replace("\r", "")
        .trimEnd()
}
