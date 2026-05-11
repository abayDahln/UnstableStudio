package com.example.unstablestudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.unstablestudio.data.repository.SafDocumentRepository
import com.example.unstablestudio.data.repository.SettingsRepository
import com.example.unstablestudio.ui.components.GlobalKeyboardContainer
import com.example.unstablestudio.ui.screens.EditorScreen
import com.example.unstablestudio.ui.screens.WorkspaceScreen
import com.example.unstablestudio.ui.theme.UnstableStudioTheme
import com.example.unstablestudio.ui.viewmodels.*
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var editorViewModel: EditorViewModel
    private lateinit var terminalViewModel: TerminalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = SafDocumentRepository(this)
        val settingsRepository = SettingsRepository.getInstance(this)
        val runtimeManager = com.example.unstablestudio.core.runtime.RuntimeManager(this)
        runtimeManager.ensureRuntimeReady()

        val openFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                workspaceViewModel.openWorkspace(it.toString())
                settingsRepository.setLastRootUri(it.toString())
            }
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(settingsRepository)
            )
            
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val colorMode by settingsViewModel.colorMode.collectAsStateWithLifecycle()
            val staticThemeStyle by settingsViewModel.staticThemeStyle.collectAsStateWithLifecycle()
            val dynamicThemeSeed by settingsViewModel.dynamicThemeSeed.collectAsStateWithLifecycle()
            val customStaticPrimary by settingsViewModel.customStaticPrimary.collectAsStateWithLifecycle()
            val customStaticSecondary by settingsViewModel.customStaticSecondary.collectAsStateWithLifecycle()
            val customStaticTertiary by settingsViewModel.customStaticTertiary.collectAsStateWithLifecycle()
            val customStaticText by settingsViewModel.customStaticText.collectAsStateWithLifecycle()
            val customStaticBackground by settingsViewModel.customStaticBackground.collectAsStateWithLifecycle()
            val customStaticSurface by settingsViewModel.customStaticSurface.collectAsStateWithLifecycle()
            val useDynamicColor by settingsViewModel.useDynamicColor.collectAsStateWithLifecycle()
            
            val keepScreenOn by settingsViewModel.keepScreenOn.collectAsStateWithLifecycle()

            if (keepScreenOn) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            UnstableStudioTheme(
                themeMode = themeMode,
                colorMode = colorMode,
                staticThemeStyle = staticThemeStyle,
                dynamicThemeSeed = dynamicThemeSeed,
                customStaticPrimary = customStaticPrimary,
                customStaticSecondary = customStaticSecondary,
                customStaticTertiary = customStaticTertiary,
                customStaticText = customStaticText,
                customStaticBackground = customStaticBackground,
                customStaticSurface = customStaticSurface,
                useDynamicColor = useDynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GlobalKeyboardContainer {
                        workspaceViewModel = viewModel(
                            factory = WorkspaceViewModelFactory(repository, settingsRepository)
                        )
                        editorViewModel = viewModel(
                            factory = EditorViewModelFactory(repository, settingsRepository)
                        )
                        terminalViewModel = viewModel(
                            factory = TerminalViewModelFactory(File(filesDir, "runtime"))
                        )

                        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                        val scope = rememberCoroutineScope()
                        val keyboardManager = com.example.unstablestudio.ui.components.LocalKeyboardManager.current

                        // Hide keyboard when drawer interaction starts
                        LaunchedEffect(drawerState.currentValue, drawerState.isAnimationRunning) {
                            if (drawerState.currentValue != DrawerValue.Closed || drawerState.isAnimationRunning) {
                                keyboardManager.hide()
                            }
                        }

                        val activeDocumentId by editorViewModel.activeDocumentId.collectAsStateWithLifecycle()
                        val openDocuments by editorViewModel.openDocuments.collectAsStateWithLifecycle()
                        val flatFileTree by workspaceViewModel.flatFileTree.collectAsStateWithLifecycle()
                        val rootUri by workspaceViewModel.rootUri.collectAsStateWithLifecycle()

                        var isRestoring by remember { mutableStateOf(false) }

                        // Persistence: Restore session on startup
                        LaunchedEffect(Unit) {
                            val lastRoot = settingsRepository.lastRootUri.value
                            if (lastRoot != null) {
                                isRestoring = true
                                workspaceViewModel.openWorkspace(lastRoot)
                                val files = settingsRepository.getFilesForWorkspace(lastRoot)
                                val active = settingsRepository.getActiveDocumentIdForWorkspace(lastRoot)
                                if (files.isNotEmpty()) {
                                    editorViewModel.restoreSession(files, active)
                                }
                                isRestoring = false
                            }
                        }

                        // Persistence: Save session when it changes
                        LaunchedEffect(activeDocumentId, rootUri, isRestoring) {
                            if (!isRestoring) {
                                rootUri?.let {
                                    settingsRepository.setActiveDocumentIdForWorkspace(it, activeDocumentId)
                                }
                            }
                        }

                        LaunchedEffect(openDocuments, rootUri, isRestoring) {
                            if (!isRestoring) {
                                rootUri?.let {
                                    settingsRepository.setFilesForWorkspace(it, openDocuments.map { it.id })
                                }
                            }
                        }

                        LaunchedEffect(workspaceViewModel, editorViewModel) {
                            workspaceViewModel.fileEvents.collect { event ->
                                when (event) {
                                    is WorkspaceFileEvent.Renamed -> editorViewModel.onWorkspaceFileRenamed(
                                        oldUri = event.oldUri,
                                        newUri = event.newUri,
                                        newName = event.newName
                                    )

                                    is WorkspaceFileEvent.Deleted -> editorViewModel.onWorkspaceFileDeleted(event.uri)
                                }
                            }
                        }


                        LaunchedEffect(flatFileTree, rootUri) {
                            editorViewModel.syncWorkspaceFiles(
                                existingUris = flatFileTree.map { it.node.uri }.toSet(),
                                hasWorkspace = rootUri != null
                            )
                        }

                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp

                        // Drawer content
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = drawerState.isOpen,
                            drawerContent = {
                                ModalDrawerSheet {
                                    WorkspaceScreen(
                                        viewModel = workspaceViewModel,
                                        runtimeManager = runtimeManager,
                                        activeDocumentId = activeDocumentId,
                                        onOpenFolder = {
                                            openFolderLauncher.launch(null)
                                        },
                                        onSettingsClick = {
                                            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                                            startActivity(intent)
                                        },
                                        onFileSelected = { uri ->
                                            editorViewModel.loadFile(uri)
                                            scope.launch { drawerState.close() }
                                        }
                                    )
                                }
                            }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                var showShortcutKeys by remember { mutableStateOf(false) }
                                val density = LocalDensity.current

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(drawerState.currentValue, density) {
                                            // Open drawer only from a small left-edge swipe to avoid
                                            // blocking horizontal scroll/selection inside the editor.
                                            val edgePx = with(density) { 24.dp.toPx() }
                                            awaitEachGesture {
                                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                                if (down.position.x > edgePx || !drawerState.isClosed) return@awaitEachGesture

                                                val drag = awaitHorizontalDragOrCancellation(down.id)
                                                if (drag != null) {
                                                    val deltaX = drag.position.x - down.position.x
                                                    if (deltaX > 0) {
                                                        scope.launch { drawerState.open() }
                                                        drag.consume()
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    EditorScreen(
                                        viewModel = editorViewModel,
                                        workspaceViewModel = workspaceViewModel,
                                        terminalViewModel = terminalViewModel,
                                        settingsRepository = settingsRepository,
                                        onOpenExplorer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        onOpenFolder = {
                                            openFolderLauncher.launch(null)
                                        },
                                        onOpenSettings = {
                                            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                                            startActivity(intent)
                                        },
                                        onShortcutKeysShow = { showShortcutKeys = true },
                                        onCloseProject = {
                                            editorViewModel.closeAllFiles()
                                            workspaceViewModel.closeWorkspace()
                                        },
                                        onExitApp = { finish() }
                                    )
                                }

                                if (showShortcutKeys) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        com.example.unstablestudio.ui.screens.ShortcutKeysScreen(
                                            onDismiss = { showShortcutKeys = false }
                                        )
                                    }
                                }

                                // Sidebar FAB
                                if (drawerState.isClosed) {
                                    FloatingActionButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Toggle Sidebar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
