package com.example.unstablestudio.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unstablestudio.core.runtime.RuntimeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeSettingsScreen(
    runtimeManager: RuntimeManager,
    onBack: () -> Unit
) {
    val status   by runtimeManager.status.collectAsStateWithLifecycle()
    val progress by runtimeManager.progress.collectAsStateWithLifecycle()
    val log      by runtimeManager.installLog.collectAsStateWithLifecycle()
    val scope    = rememberCoroutineScope()
    val logState = rememberLazyListState()

    // Auto-scroll the log to the latest entry
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) logState.animateScrollToItem(log.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Runtime Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Header card ──────────────────────────────────────────────
            HeaderCard(status = status)

            // ── Progress card (visible during any installation phase) ────
            AnimatedVisibility(
                visible = isInstalling(status),
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
            ) {
                ProgressCard(status = status, progress = progress)
            }

            // ── Log card (visible once log has entries) ──────────────────
            AnimatedVisibility(
                visible = log.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit  = fadeOut() + shrinkVertically()
            ) {
                LogCard(log = log, listState = logState)
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Action buttons ───────────────────────────────────────────
            ActionButtons(
                status  = status,
                onSetup = { scope.launch { runtimeManager.ensureRuntimeReady() } },
                onRepair = { scope.launch { runtimeManager.reinstall() } }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(status: String) {
    val isReady   = status == "READY"
    val isError   = status.startsWith("ERROR")
    val iconColor = when {
        isReady -> Color(0xFF4CAF50)
        isError -> MaterialTheme.colorScheme.error
        else    -> MaterialTheme.colorScheme.primary
    }
    val iconVec = when {
        isReady -> Icons.Default.CheckCircle
        isError -> Icons.Default.Error
        else    -> Icons.Default.Terminal
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.large,
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = iconVec,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = "Internal Linux Environment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "Termux rootfs · Node.js · Python 3 · pip",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Status chip
            StatusChip(status = status)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, containerColor, contentColor) = when {
        status == "READY"          -> Triple("✓ Ready", Color(0xFF1B5E20), Color(0xFF81C784))
        status == "IDLE"           -> Triple("Not installed", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        status == "DOWNLOADING"    -> Triple("Downloading…",  MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        status == "EXTRACTING"     -> Triple("Extracting…",   MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        status == "PKG_INSTALLING" -> Triple("Installing packages…", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        status.startsWith("ERROR") -> Triple("Error", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        else                       -> Triple(status, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text  = label,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ProgressCard(status: String, progress: Float) {
    val label = when (status) {
        "DOWNLOADING"    -> "Downloading Termux bootstrap…"
        "EXTRACTING"     -> "Extracting Linux rootfs…"
        "PKG_INSTALLING" -> "Installing Node.js & Python…"
        else             -> "Working…"
    }

    // Animate progress bar value
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = MaterialTheme.shapes.medium,
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text  = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun LogCard(log: List<String>, listState: androidx.compose.foundation.lazy.LazyListState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape  = MaterialTheme.shapes.medium,
        color  = Color(0xFF0D1117),
        tonalElevation = 0.dp
    ) {
        Box {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(log) { line ->
                    val lineColor = when {
                        line.startsWith("✓")  -> Color(0xFF56D364)
                        line.startsWith("⬇")  -> Color(0xFF79C0FF)
                        line.startsWith("📦") -> Color(0xFFD2A8FF)
                        line.startsWith("🟢") -> Color(0xFF56D364)
                        line.startsWith("ERROR") || line.startsWith("error") -> Color(0xFFF85149)
                        else -> Color(0xFFE6EDF3)
                    }
                    Text(
                        text       = line,
                        color      = lineColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            // Fade gradient at top so it feels like a real terminal window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D1117), Color.Transparent)
                        )
                    )
            )
        }
    }
}

@Composable
private fun ActionButtons(
    status: String,
    onSetup: () -> Unit,
    onRepair: () -> Unit
) {
    val installing = isInstalling(status)
    val isReady    = status == "READY"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isReady || status.startsWith("ERROR")) {
            Button(
                onClick  = onSetup,
                modifier = Modifier.fillMaxWidth(),
                enabled  = !installing
            ) {
                if (installing) {
                    CircularProgressIndicator(
                        modifier   = Modifier.size(18.dp),
                        color      = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Working…")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (status.startsWith("ERROR")) "Retry Setup"
                        else "Set Up Runtime"
                    )
                }
            }
        }

        if (isReady) {
            // Show repair option when ready
            OutlinedButton(
                onClick  = onRepair,
                modifier = Modifier.fillMaxWidth(),
                enabled  = !installing
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reinstall Runtime")
            }
        }

        Text(
            text   = if (isReady)
                "Node.js & Python are ready. Open the terminal tab to start coding."
            else
                "Runtime is set up automatically on first launch. Requires internet connection.",
            style  = MaterialTheme.typography.labelSmall,
            color  = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun isInstalling(status: String): Boolean =
    status in listOf("DOWNLOADING", "EXTRACTING", "PKG_INSTALLING")
