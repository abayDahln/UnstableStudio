package com.example.unstablestudio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                        val flatFileTree by workspaceViewModel.flatFileTree.collectAsStateWithLifecycle()
                        val rootUri by workspaceViewModel.rootUri.collectAsStateWithLifecycle()

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

                        ModalNavigationDrawer(
                            drawerState = drawerState,
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
                            EditorScreen(
                                viewModel = editorViewModel,
                                workspaceViewModel = workspaceViewModel,
                                terminalViewModel = terminalViewModel,
                                settingsRepository = settingsRepository,
                                onOpenExplorer = {
                                    scope.launch { drawerState.open() }
                                },
                                onOpenSettings = {
                                    val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                                    startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
