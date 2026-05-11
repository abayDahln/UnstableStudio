package com.example.unstablestudio.ui.screens

import com.example.unstablestudio.ui.theme.Roboto

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unstablestudio.core.runtime.RuntimeManager
import com.example.unstablestudio.domain.model.FileNode
import com.example.unstablestudio.ui.viewmodels.WorkspaceViewModel
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import java.util.Locale

@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel,
    runtimeManager: RuntimeManager,
    activeDocumentId: String?,
    onOpenFolder: () -> Unit,
    onSettingsClick: () -> Unit,
    onFileSelected: (String) -> Unit
) {
    val flatFileTree by viewModel.flatFileTree.collectAsStateWithLifecycle()
    val rootUri by viewModel.rootUri.collectAsStateWithLifecycle()
    val useOriginalColors by viewModel.settingsRepository.useOriginalIconColors.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val availableIconNames = remember(context) {
        context.assets.list("icons")
            ?.asSequence()
            ?.filter { it.endsWith(".svg", ignoreCase = true) }
            ?.map { it.removeSuffix(".svg").lowercase(Locale.ROOT) }
            ?.toSet()
            ?: emptySet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Header - Simple title only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORER",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = Roboto,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (rootUri == null) {
            // Simple empty state message for the sidebar
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Open a folder to start coding",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Roboto,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else if (flatFileTree.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Empty project",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = Roboto,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(flatFileTree, key = { it.node.uri }) { flatNode ->
                    FileNodeItem(
                        node = flatNode.node,
                        level = flatNode.level,
                        isActive = activeDocumentId == flatNode.node.uri,
                        useOriginalColors = useOriginalColors,
                        availableIconNames = availableIconNames,
                        onNodeClick = { clickedNode ->
                            if (clickedNode.isDirectory) {
                                viewModel.toggleFolder(clickedNode)
                            } else {
                                onFileSelected(clickedNode.uri)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileNodeItem(
    node: FileNode,
    level: Int,
    isActive: Boolean = false,
    useOriginalColors: Boolean,
    availableIconNames: Set<String>,
    onNodeClick: (FileNode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.secondaryContainer 
                else Color.Transparent
            )
            .clickable { onNodeClick(node) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(level) {
            Box(modifier = Modifier.width(16.dp).fillMaxHeight()) {
                VerticalDivider(
                    modifier = Modifier.align(Alignment.Center).fillMaxHeight().width(0.5.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        if (node.isDirectory) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Spacer(Modifier.width(4.dp))

        FileIcon(
            fileName = node.name,
            isDirectory = node.isDirectory,
            isExpanded = node.isExpanded,
            useOriginalColors = useOriginalColors,
            availableIconNames = availableIconNames,
            modifier = Modifier.size(18.dp),
            tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else Color.Unspecified
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = node.name,
            style = if (isActive) MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = Roboto
            ) else MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Roboto
            ),
            color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer 
                    else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FileIcon(
    fileName: String,
    isDirectory: Boolean,
    isExpanded: Boolean,
    useOriginalColors: Boolean,
    availableIconNames: Set<String>,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val context = LocalContext.current
    val assetName = remember(fileName, isDirectory, isExpanded, availableIconNames) {
        resolveExplorerIconName(
            fileName = fileName,
            isDirectory = isDirectory,
            isExpanded = isExpanded,
            availableIconNames = availableIconNames
        )
    }
    val resolvedTint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint

    if (assetName != null) {
        val request = remember(assetName, context) {
            ImageRequest.Builder(context)
                .data("file:///android_asset/icons/$assetName.svg")
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(false)
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            modifier = modifier,
            colorFilter = if (useOriginalColors) {
                null
            } else {
                androidx.compose.ui.graphics.ColorFilter.tint(resolvedTint)
            },
            loading = {
                Icon(
                    imageVector = if (isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = modifier,
                    tint = resolvedTint
                )
            },
            error = {
                Icon(
                    imageVector = if (isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = modifier,
                    tint = resolvedTint
                )
            }
        )
    } else {
        Icon(
            imageVector = if (isDirectory) {
                if (isExpanded) Icons.Default.Folder else Icons.Outlined.Folder
            } else {
                Icons.AutoMirrored.Filled.InsertDriveFile
            },
            contentDescription = null,
            modifier = modifier,
            tint = resolvedTint
        )
    }
}

private fun resolveExplorerIconName(
    fileName: String,
    isDirectory: Boolean,
    isExpanded: Boolean,
    availableIconNames: Set<String>
): String? {
    val rawName = fileName.lowercase(Locale.ROOT)
    val sanitizedName = sanitizeForIcon(rawName)
    val extension = rawName.substringAfterLast('.', missingDelimiterValue = "")
    val simpleName = if (rawName.contains('.')) rawName.substringBeforeLast('.') else rawName
    val sanitizedSimpleName = sanitizeForIcon(simpleName)

    val candidates = mutableListOf<String>()

    if (isDirectory) {
        if (isExpanded) {
            candidates += "folder-$sanitizedName-open"
        }
        candidates += "folder-$sanitizedName"
        candidates += "folder-open"
        candidates += "folder"
    } else {
        val extensionIcon = extensionToIconName(extension)
        if (rawName == "dockerfile") candidates += "docker"
        if (rawName == "makefile") candidates += "makefile"
        if (rawName == "readme" || rawName.startsWith("readme.")) candidates += "readme"
        if (rawName == "license" || rawName.startsWith("license.")) candidates += "license"
        if (rawName == "changelog" || rawName.startsWith("changelog.")) candidates += "changelog"
        if (rawName == "package.json") candidates += "nodejs"
        if (rawName == "tsconfig.json") candidates += "tsconfig"
        if (rawName == "jsconfig.json") candidates += "jsconfig"
        candidates += sanitizedName
        candidates += sanitizedSimpleName
        if (extensionIcon != null) candidates += extensionIcon
        if (extension.isNotBlank()) candidates += extension
        candidates += "default_file"
    }

    return candidates.firstOrNull { it in availableIconNames }
}

private fun sanitizeForIcon(value: String): String {
    return value
        .trim()
        .replace(" ", "-")
        .replace("_", "-")
}

private fun extensionToIconName(extension: String): String? {
    return when (extension.lowercase(Locale.ROOT)) {
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "xml" -> "xml"
        "json" -> "json"
        "yaml", "yml" -> "yaml"
        "toml" -> "toml"
        "md", "markdown" -> "markdown"
        "html", "htm" -> "html"
        "css" -> "css"
        "scss", "sass" -> "sass"
        "less" -> "less"
        "js", "mjs", "cjs" -> "javascript"
        "ts", "mts", "cts" -> "typescript"
        "jsx" -> "react"
        "tsx" -> "react_ts"
        "py" -> "python"
        "rb" -> "ruby"
        "go" -> "go"
        "rs" -> "rust"
        "php" -> "php"
        "swift" -> "swift"
        "dart" -> "dart"
        "c" -> "c"
        "h" -> "h"
        "cc", "cpp", "cxx", "hpp", "hh" -> "cpp"
        "cs" -> "csharp"
        "sh", "bash", "zsh", "fish" -> "terminal"
        "ps1" -> "powershell"
        "sql" -> "database"
        "gradle" -> "gradle"
        "vue" -> "vue"
        "svelte" -> "svelte"
        "astro" -> "astro"
        "lock" -> "lock"
        else -> null
    }
}
