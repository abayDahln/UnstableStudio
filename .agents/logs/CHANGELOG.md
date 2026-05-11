# Unstable Studio - CHANGELOG

## Format

```markdown
## [YYYY-MM-DD HH:MM] - [Task Name]

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `path/to/File.kt` | ADD | +50 | ... |

### Summary
- **Total Files**: X
- **Lines Added**: +X
- **Lines Edited**: ~X
- **Lines Removed**: -X

### Notes
- Reason:
- Risks:
- Testing:
```

## Entries

## [2026-05-09 17:00] - Shortcut Keys & View Menu

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/ShortcutKeysScreen.kt` | ADD | +50 | Created new screen to display keyboard shortcuts table. |
| `app/src/main/java/com/example/unstablestudio/ui/components/MenuBar.kt` | EDIT | +5 | Added "Shortcut Keys" menu item. |

### Summary
- **Total Files**: 2
- **Lines Added**: ~55
- **Lines Removed**: 0

### Notes
- Reason: Improved discoverability of keyboard shortcuts for power users.
- Testing: Verified shortcut screen UI, menu integration, and correct display of the shortcut table.

## [2026-05-09 16:30] - Selection Menu Cleanup

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/components/MenuBar.kt` | EDIT | -2 | Removed "Add Cursor Above" and "Add Cursor Below" from the Selection menu. |

### Summary
- **Total Files**: 1
- **Lines Added**: 0
- **Lines Removed**: ~2

### Notes
- Reason: Simplified the Selection menu to keep only essential selection-related actions.
- Testing: Verified that the Selection menu items are now clean and correctly structured.

## [2026-05-09 16:00] - Edit Menu Cleanup

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/components/MenuBar.kt` | EDIT | -5 | Simplified Edit menu by removing "Replace" and "in Files" options. |

### Summary
- **Total Files**: 1
- **Lines Added**: 0
- **Lines Removed**: ~5

### Notes
- Reason: Cleaned up the Edit menu for better UX by keeping only essential editing commands.
- Testing: Verified menu item removal and ensured existing items (Undo/Redo/Cut/Copy/Paste/Find) remain correctly structured.

## [2026-05-09 15:30] - File Menu Refactor & Close Project

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/WorkspaceViewModel.kt` | EDIT | +5 | Implemented `closeWorkspace` to clear workspace state and settings. |
| `app/src/main/java/com/example/unstablestudio/ui/components/MenuBar.kt` | EDIT | +10 | Refactored File menu: Added "Close Project", removed obsolete items, and added support for conditional Save button visibility. |

### Summary
- **Total Files**: 2
- **Lines Added**: ~15
- **Lines Removed**: ~10

### Notes
- Reason: Cleaned up the File menu and added a proper "Close Project" functionality to improve project lifecycle management.
- Testing: Verified that "Close Project" correctly clears the current project state and returns to the project selection screen.

## [2026-05-09 15:00] - Sidebar FAB & Empty State

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +20 | Added Floating Action Button (FAB) overlay to toggle sidebar. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | +40 | Enhanced empty state UI with actionable buttons and updated messaging. |

### Summary
- **Total Files**: 2
- **Lines Added**: ~60
- **Lines Removed**: ~10

### Notes
- Reason: Improved sidebar accessibility for mobile and added guidance for users when projects or file selections are empty.
- Testing: Verified FAB visibility, toggle sidebar functionality, and correct empty state messaging.

## [2026-05-09 14:30] - Remove Auto-Extension .txt

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/WorkspaceViewModel.kt` | EDIT | -4 | Removed auto-appending of `.txt` extension during file creation. |

### Summary
- **Total Files**: 1
- **Lines Added**: 0
- **Lines Removed**: ~4

### Notes
- Reason: The IDE was forcing `.txt` extensions on files that didn't have one, preventing users from creating files without extensions or with custom ones.
- Fix: Modified `createFile` to use the raw input name provided by the user.
- Testing: Verified creation of files without extensions (e.g., `Makefile`, `README`) and standard files (e.g., `index.html`).

## [2026-05-09 14:00] - Dynamic Toolbar Layout

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/components/MenuBar.kt` | EDIT | +1 | Changed `horizontalArrangement` to `Arrangement.SpaceBetween` to implement dynamic spread layout. |

### Summary
- **Total Files**: 1
- **Lines Added**: ~1
- **Lines Removed**: ~1

### Notes
- Reason: Improved UI responsiveness by making the menu strip layout dynamic and proportional to screen width.
- Fix: Used `Arrangement.SpaceBetween` in the toolbar `Row` to ensure menu items are well-distributed across different device sizes.
- Testing: Verified that menu items are correctly distributed and responsive.

## [2026-05-09 13:00] - Drag & Drop File/Folder

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | +15 | Replaced plain-text DnD with custom `application/x-unstablestudio-node` MIME type. Added visual feedback for drag targets. |

### Summary
- **Total Files**: 1
- **Lines Added**: ~15
- **Lines Removed**: ~10

### Notes
- Reason: Prevented accidental file path pasting into the code editor and provided visual feedback for drag-and-drop actions.
- Fix: Used a custom MIME type to isolate DnD events within the IDE.
- Testing: Dragged files/folders into various targets (child/parent/root) and verified successful move without editor interference.

## [2026-05-09 12:00] - Scroll Editor & Sidebar Trigger

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +30 | Implemented custom 40% left zone sidebar trigger using `pointerInput`. Disabled default drawer gestures for opening. |
| `io/github/rosemoe/sora/widget/CodeEditor.java` | EDIT | +120 | Refactored `onTouchEvent` to support 2-finger horizontal scroll and restricted 1-finger vertical scroll. Integrated 40% zone exclusion for sidebar. |

### Summary
- **Total Files**: 2
- **Lines Added**: ~150
- **Lines Removed**: ~20

### Notes
- Reason: Resolve conflicts between editor horizontal scrolling and sidebar opening gestures.
- Improvements: 
    - Sidebar only triggers from left 40% zone.
    - 2-finger horizontal scroll for precise editor navigation.
    - 1-finger vertical scroll only (outside sidebar zone).
    - Mouse wheel + Shift for horizontal scroll (existing logic verified).
- Testing: Verified gesture separation on touch and mouse input.

## [2026-05-09 11:00] - Duplicate Name Conflict Handling

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/WorkspaceViewModel.kt` | EDIT | +120 | Implemented `ConflictState`, `ConflictSource`, and comprehensive conflict resolution logic (Overwrite, Rename, Auto-Rename). |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | +60 | Integrated `ConflictDialog` to handle name collisions during creation, renaming, and moving. Cleaned up legacy rename dialog code. |

### Summary
- **Total Files**: 2
- **Lines Added**: ~180
- **Lines Removed**: ~40 (Legacy dialog code)

### Notes
- Reason: Prevent accidental overwrites and provide better UX for handling file name collisions.
- Features: Overwrite, Manual Rename, and Auto-Rename (suffix increment) are supported.
- Testing: Verified conflicts in creation, renaming, and drag-and-drop moving.

## [2026-05-09 10:00] - Inline Rename File/Folder

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/WorkspaceViewModel.kt` | EDIT | +35 | Added `renamingNodeUri` state and management methods (`startRenaming`, `finishRenaming`, `cancelRenaming`). |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | +150 | Replaced popup rename dialog with inline `BasicTextField`. Updated `FileNodeItem` to support inline editing mode. |

### Summary
- **Total Files**: 2
- **Lines Added**: ~185
- **Lines Removed**: ~50 (Dialog code)

### Notes
- Reason: Improved UX by allowing users to rename files directly in the sidebar, matching VS Code behavior and the existing new file/folder flow.
- Risks: Inline input field focus and keyboard interaction on various Android versions.
- Testing: Verified inline input appearance, focus, Enter to confirm, and Escape to cancel.

## [2026-04-26 11:00] - Global Custom Keyboard System

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/components/CodingKeyboard.kt` | REFACTOR | +250 | Redesigned UI to match Gboard style. Added Tri-State Shift (Caps Lock). |
| `app/src/main/java/com/example/unstablestudio/ui/components/KeyboardManager.kt` | ADD | +45 | Created global manager for visibility and input routing. |
| `app/src/main/java/com/example/unstablestudio/ui/components/GlobalKeyboardContainer.kt` | ADD | +40 | Created root wrapper to host the custom keyboard. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +15 | Integrated global keyboard container. |
| `app/src/main/java/com/example/unstablestudio/SettingsActivity.kt` | EDIT | +5 | Integrated global keyboard container. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +120 | Integrated CodeEditor and dialogs with KeyboardManager. Disabled system IME. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/SettingsScreen.kt` | EDIT | +60 | Integrated settings fields with custom keyboard. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | +40 | Integrated Explorer input fields with custom keyboard. |
| `app/src/main/java/com/example/unstablestudio/ui/components/BottomPanel.kt` | EDIT | +30 | Integrated Terminal input with custom keyboard. |

### Summary
- Replaced the Android system keyboard with a professional, theme-aware custom virtual keyboard.
- Implemented a global architecture for input routing using `CompositionLocal` and `KeyboardManager`.
- Disabled system soft keyboard across all application activities and input fields.
- Polished keyboard UI with Gboard-like proportions, spacing, and Caps Lock support.

### Notes
- Reason: User requested a full custom keyboard experience and disabling the system keyboard.
- Risks: `readOnly = true` on some Android versions might still trigger minor IME artifacts; requires extensive testing on various devices.
- Testing: Verified focus behavior and input routing across all major screens.

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/core/config/AppConstants.kt` | EDIT | +5 | Added keys for custom theme colors. |
| `app/src/main/java/com/example/unstablestudio/data/repository/SettingsRepository.kt` | EDIT | +20 | Added persistence for custom colors and new settings (Keep Screen On, Show Line Numbers). |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/SettingsViewModel.kt` | EDIT | +15 | Added draft state for theme preview and `applyTheme` method. |
| `app/src/main/java/com/example/unstablestudio/ui/theme/Theme.kt` | EDIT | +30 | Updated `UnstableStudioTheme` to support dynamic custom color schemes. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/SettingsScreen.kt` | EDIT | +100 | Implemented "Custom" theme option with hex inputs, live preview, and "Apply" button. Added new settings toggles. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` & `SettingsActivity.kt` | EDIT | +10 | Passed custom colors into the theme engine. |

### Summary
- **Total Files**: 7
- **Lines Added**: +180

### Notes
- Users can now design their own theme by specifying Primary, Surface, and Background colors.
- Added "Keep Screen On" and "Show Line Numbers" for better IDE customization.
- Applied Material 3 design principles throughout the Settings UI.

## [2026-04-16 17:30] - Settings Activity and Terminal UX Refinement

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/SettingsActivity.kt` | ADD | +40 | Created a dedicated Activity for application settings. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/SettingsScreen.kt` | REFACTOR | +150 | Implemented a tabbed UI for settings (General, Appearance, Editor, Terminal, Runtime, About). |
| `app/src/main/java/com/example/unstablestudio/ui/components/MenuBar.kt` | REFACTOR | +80 | Restored VS Code style dropdown menus and added Settings navigation. |
| `app/src/main/java/com/example/unstablestudio/core/runtime/RuntimeManager.kt` | EDIT | +20 | Fixed crash by using an independent CoroutineScope for installations. |
| `app/src/main/java/com/example/unstablestudio/ui/components/BottomPanel.kt` | EDIT | +20 | Improved Terminal focus; clicking the panel now triggers the keyboard. |
| `app/src/main/AndroidManifest.xml` | EDIT | +5 | Registered `SettingsActivity`. |

### Summary
- **Total Files**: 6
- **Lines Added**: +315
- **Lines Edited**: ~50

### Notes
- Settings are now persistent and easily accessible via the restored top-bar Menu.
- Terminal is much more usable on mobile with the new focus-on-click behavior.

## [2026-04-16 16:30] - Terminal Proot Integration

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/core/runtime/RuntimeManager.kt` | EDIT | +20 | Added logic to copy the `proot` binary from assets to the executable internal files directory. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/TerminalViewModel.kt` | EDIT | +20 | Refactored `ProcessBuilder` to launch `/bin/sh` via `proot` instead of standard Android `sh` when the rootfs is installed. |
| `app/src/main/assets/proots/proot` | ADD | N/A | Added the prebuilt static ARM64 `proot` binary from `green-green-avk/build-proot-android`. |

### Summary
- **Total Files**: 3
- **Lines Added**: ~40
- **Lines Edited**: ~10

### Notes
- Terminal now seamlessly bridges to the internal Linux environment if the runtime has been downloaded via Settings.
- Prepares the IDE for full package manager (`apk`/`apt`) support, unlocking Node.js, Python, and Git.

## [2026-04-16 15:30] - VS Code style Bottom Panel and Terminal

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/TerminalViewModel.kt` | ADD | +60 | Created ViewModel to manage a process-based interactive shell (`sh`). |
| `app/src/main/java/com/example/unstablestudio/ui/components/BottomPanel.kt` | ADD | +120 | Built a tabbed UI hosting Terminal, Output, Debug, Problems, and Ports. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +150 | Integrated `ModalBottomSheet` for the bottom panel and added `Ctrl + J` shortcut. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | ~100 | Refactored ViewModel initialization and passed `TerminalViewModel` to the UI. |

### Summary
- **Total Files**: 4
- **Lines Added**: +350
- **Lines Edited**: ~100

### Notes
- Terminal currently uses a basic `ProcessBuilder` wrapper. For a full PTY (Pseudo-Terminal) with full ANSI/color support, a native library integration would be required in future phases.
- Use `Ctrl + J` or the new Floating Action Button to toggle the panel.

## [2026-04-16 14:45] - Automatic Project Restoration on Startup

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/WorkspaceViewModel.kt` | EDIT | +10 | Modified `init` block to automatically open the last used workspace from `SettingsRepository`. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +5 | Updated `WorkspaceViewModel` factory to inject the required `SettingsRepository`. |

### Summary
- **Total Files**: 2
- **Lines Added**: +15
- **Lines Edited**: ~5

### Notes
- Users can now resume work immediately upon opening the app without manually selecting the last project.

## [2026-04-16 14:15] - Per-Project Session Persistence and Welcome Screen Shortcuts

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/data/repository/SettingsRepository.kt` | EDIT | +20 | Added workspace-specific persistence for open files. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/EditorViewModel.kt` | EDIT | +15 | Added `closeAllFiles` for clean workspace switching. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +30 | Integrated logic to restore project-specific tabs when opening recent projects. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +50 | Added shortcut hints to Welcome Screen when project history is empty. |

### Summary
- **Total Files**: 4
- **Lines Added**: +115
- **Lines Edited**: ~10

### Notes
- Clicking a "Recent Project" now fully restores the editor state (all previous tabs).
- Provides guidance for new users via shortcut hints on the empty welcome screen.

## [2026-04-16 13:30] - Editor Layout and Styling Refinement

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +20 | Enabled horizontal scrolling, added spacing/margins for line numbers, and refined colors for active line/number highlighting. |

### Summary
- **Total Files**: 1
- **Lines Added**: +15
- **Lines Edited**: ~10

### Notes
- Spacing is now approximately 1 tab (24dp) for both left margin and divider.
- Colors are automatically adapted to Light and Dark Material 3 themes.

## [2026-04-16 13:00] - Welcome Screen and Project History Persistence

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +120 | Implemented a modern Welcome Screen in the editor center with an "Open Folder" button and a list of the 10 most recent projects. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | -40 | Removed the redundant recent projects list from the sidebar to focus on current project exploration. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +10 | Updated component injection to pass necessary viewmodels and repositories to the new Welcome Screen. |

### Summary
- **Total Files**: 3
- **Lines Added**: +130
- **Lines Removed**: -40

### Notes
- Enhances the startup experience by providing quick access to recent work.
- Standardizes the "No projects open" state across the entire IDE.

## [2026-04-16 12:15] - Theme Persistence and Editor Styling

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/SettingsScreen.kt` | EDIT | +30 | Added Theme Mode selection UI to the settings screen. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +50 | Integrated Material 3 `ColorScheme` into the `CodeEditor`. Editor background, text, selection, and line numbers now update dynamically based on the theme. |

### Summary
- **Total Files**: 2
- **Lines Added**: +80
- **Lines Edited**: ~10

### Notes
- Fixes the issue where the editor remained white in dark mode.
- Users can now select and persist Light/Dark themes.

## [2026-04-16 11:30] - Undo/Redo Stabilization

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/domain/undo/UndoCommand.kt` | EDIT | +25 | Refined `TextChangeCommand` merge logic to include character categories (Alphanumeric, Whitespace, Symbol). This ensures undo/redo operates on logical word boundaries. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/EditorViewModel.kt` | EDIT | +15 | Removed `debounce` from text change processing to prevent data loss in undo history. Fixed `updateContent` to preserve undo stack on file save. |

### Summary
- **Total Files**: 2
- **Lines Added**: +15
- **Lines Edited**: ~40
- **Lines Removed**: -5

### Notes
- Reason: Undo/Redo was losing history due to debouncing and clearing the stack on save. Grouping was also too aggressive, making it hard to undo specific words.
- Risks: Performance might be slightly affected by processing every keystroke, but the logic is lightweight and necessary for correctness.
- Testing: Verified that sequential typing is merged by category and history persists across saves.

## [2026-04-16 11:00] - Keyboard and Editor Interaction Fixes

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +120 | Added physical keyboard Ctrl shortcut support via `onPreviewKeyEvent`. Fixed Undo/Redo behavior by cleaning stack after load and adding `Ctrl+Shift+Z`. Implemented virtual keyboard visibility controls (Hide on Esc, Back, file change). |

### Summary
- **Total Files**: 1
- **Lines Added**: +120
- **Lines Edited**: ~60
- **Lines Removed**: -40

### Notes
- Reason: Physical keyboard Ctrl keys were unreliable, Undo was clearing the entire file, and the virtual keyboard was stuck on screen.
- Risks: Low. Keyboard interceptor is scoped to the editor view.
- Testing: Project compiles successfully. Manual verification needed for all shortcut and visibility logic.

## [2026-04-14 03:00] - Arrow Key Behavior Fix

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +20 | Added key event interceptor to fix arrow key behavior. Arrow keys now only move cursor without selecting (unless Shift is held). Matches VS Code behavior. |

### Summary
- **Total Files**: 1
- **Lines Added**: +20
- **Lines Edited**: ~2
- **Lines Removed**: -0

### Notes
- Reason: Arrow keys were creating text selections instead of just moving cursor. Now behaves like VS Code - arrow keys move cursor, Shift+Arrow creates selection.
- Risks: Low. Key interceptor only affects arrow keys and clears selection when Shift is not pressed.
- Testing: Project compiles successfully. Manual testing needed to verify arrow key behavior in editor.

## [2026-04-14 02:00] - UI/UX Fixes and LSP Error Handling

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +6 | Added auto-close sidebar when opening file. Sidebar now closes automatically after file selection. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +5 | Fixed IndexOutOfBoundsException on ScrollableTabRow with proper bounds checking using when expression. |
| `app/src/main/java/com/example/unstablestudio/data/remote/lsp/LspService.kt` | EDIT | +35 | Added connection state tracking, specific exception handling for ConnectException/SocketTimeoutException, and connection state callback. |

### Summary
- **Total Files**: 3
- **Lines Added**: +46
- **Lines Edited**: ~15
- **Lines Removed**: -0

### Notes
- Reason: Fixed sidebar auto-close behavior, TabRow crash (IndexOutOfBoundsException), and improved LSP connection error handling with specific error messages.
- Risks: Low. All changes are backward compatible and improve user experience.
- Testing: Project compiles successfully. Manual testing needed for sidebar auto-close and TabRow behavior.

## [2026-04-14 01:00] - SharedPreferences ClassCastException Fix

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/data/repository/SettingsRepository.kt` | EDIT | +25 | Added migration logic to handle ClassCastException when reading openFiles. Safely converts old Set format to new String format. Added AppLogger import. |

### Summary
- **Total Files**: 1
- **Lines Added**: +25
- **Lines Edited**: ~5
- **Lines Removed**: -0

### Notes
- Reason: Fixed java.lang.ClassCastException: HashSet cannot be cast to String error caused by changing storage format from Set to String without migration.
- Risks: Low. Migration is safe and backward compatible - preserves all existing user data.
- Testing: Project compiles successfully. Migration will automatically run on first launch after update.

## [2026-04-14 00:00] - Critical Bug Fixes and Performance Improvements

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | ~25 | Fixed empty catch block with proper error logging and snackbar feedback. Added derivedStateOf for tab index. Replaced collectAsState with collectAsStateWithLifecycle. |
| `app/src/main/java/com/example/unstablestudio/editor_engine/syntax/LspLanguageWrapper.kt` | EDIT | +3 | Fixed coroutine scope leak by adding autocompleteScope.cancel() in destroy(). |
| `app/src/main/java/com/example/unstablestudio/data/remote/lsp/LspService.kt` | VERIFY | 0 | Verified serviceScope.cancel() is called in onDestroy (already correct). |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | +3 | Replaced collectAsState with collectAsStateWithLifecycle. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/SettingsScreen.kt` | EDIT | ~10 | Replaced collectAsState with collectAsStateWithLifecycle. |
| `app/src/main/java/com/example/unstablestudio/ui/components/SettingsDialog.kt` | EDIT | ~10 | Replaced collectAsState with collectAsStateWithLifecycle. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | ~5 | Replaced collectAsState with collectAsStateWithLifecycle. |
| `app/src/main/java/com/example/unstablestudio/data/repository/SettingsRepository.kt` | EDIT | ~8 | Fixed file order preservation by using delimited string instead of Set. |
| `app/src/main/java/com/example/unstablestudio/editor_engine/syntax/TextMateConfig.kt` | EDIT | ~30 | Added proper error handling for missing fonts/grammars with graceful fallback. |
| `app/src/main/java/com/example/unstablestudio/core/config/AppConstants.kt` | EDIT | +30 | Added comprehensive UI constants for keyboard, dialog, menu bar, and explorer components. |

### Summary
- **Total Files**: 10
- **Lines Added**: +50
- **Lines Edited**: ~120
- **Lines Removed**: -5

### Notes
- Reason: Multiple critical bugs causing sidebar errors, poor performance, and silent failures.
- Risks: Low. All changes are backward compatible and improve robustness.
- Testing: Project compiles successfully. Manual testing recommended for all modified features.

## [2026-04-13 14:30] - Build Fixes

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `gradle/libs.versions.toml` | EDIT | +2 | Added material-icons-extended library. |
| `app/build.gradle.kts` | EDIT | +1 | Added material-icons-extended dependency. |
| `app/src/main/java/com/example/unstablestudio/data/repository/LocalDocumentRepository.kt` | EDIT | +25 | Implemented missing interface methods with stubs. |
| `app/src/main/java/com/example/unstablestudio/editor_engine/syntax/TextMateConfig.kt` | EDIT | ~10 | Fixed TextMate API usage for sora-editor v0.23.6. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | ~10 | Fixed isFocused, subscribeEvent, and missing icons. |

## [2026-04-13 15:15] - Ripple Migration and UI Polish

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `gradle/libs.versions.toml` | EDIT | ~1 | Updated Compose BOM to 2024.10.00. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +5 | Added Ripple migration flag. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | ~5 | Updated to AutoMirrored icons. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | ~5 | Updated to AutoMirrored icons. |

## [2026-04-13 15:30] - Drag and Drop Fixes and Dotfile Support

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/data/repository/SafDocumentRepository.kt` | EDIT | ~2 | Updated dotfile MIME type to `*/*` to prevent `.txt` extension appending. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/WorkspaceScreen.kt` | EDIT | ~40 | Fixed gesture conflict by replacing `combinedClickable` with `clickable` and `MoreVert` icon button. Updated `dragAndDropSource` to Compose 1.7.0 API. |
| `app/src/main/java/com/example/unstablestudio/domain/repository/DocumentRepository.kt` | EDIT | +1 | Added `moveFile` method interface. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/WorkspaceViewModel.kt` | EDIT | ~20 | Added `moveFile` logic and improved `createFile` to refresh subfolders. |

## [2026-04-13 16:00] - Settings and Performance Feature

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/data/repository/SettingsRepository.kt` | ADD | +40 | Created repository for SharedPreferences to handle font size, word wrap, and FPS. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/SettingsViewModel.kt` | ADD | +25 | Created ViewModel to expose settings state to the UI. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/SettingsScreen.kt` | ADD | +120 | Created Settings UI with font size slider, word wrap toggle, and target FPS radio buttons. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +40 | Injected settings, implemented settings screen routing, and handled window frame rate for FPS control. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/EditorViewModel.kt` | EDIT | +10 | Added SettingsRepository injection to provide settings state flow. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +20 | Added dynamic font size handling and a settings navigation button. |

## [2026-04-13 16:30] - LSP Integration (Mock)

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/data/remote/lsp/LspService.kt` | EDIT | +15 | Added LocalBinder to expose the service to MainActivity. Added mock JSON-RPC response for diagnostics. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +30 | Implemented `ServiceConnection` to bind `LspService` to `EditorViewModel`. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/EditorViewModel.kt` | EDIT | +60 | Added logic to connect `LspClient`, send `didOpen`/`didChange`/`didClose` notifications, and parse incoming diagnostic JSON messages. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +20 | Added a bottom UI panel below CodeEditor to display real-time diagnostics from the LSP. |

## [2026-04-13 17:00] - LSP TCP and ProcessBuilder Integration

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/data/remote/lsp/LspService.kt` | EDIT | ~60 | Removed mock PipedStreams and implemented real `ProcessBuilder` and TCP `Socket` connections for Language Servers. |

## [2026-04-13 18:30] - LSP Autocomplete and Hover UI Integration

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/editor_engine/syntax/LspLanguageWrapper.kt` | ADD | +110 | Created a delegation wrapper for `Language` to inject LSP completion into `TextMateLanguage`. |
| `app/src/main/java/com/example/unstablestudio/editor_engine/syntax/TextMateConfig.kt` | EDIT | ~10 | Applied `LspLanguageWrapper` to the editor setup. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/EditorViewModel.kt` | EDIT | +100 | Implemented `requestCompletion` and `requestHover` methods with JSON-RPC handling and pending request tracking. |
| `app/src/main/java/com/example/unstablestudio/data/remote/lsp/LspModels.kt` | EDIT | +20 | Added `CompletionItem`, `Position`, and `Range` models for LSP data. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +20 | Added `LongClickListener` to Sora Editor to trigger LSP Hover info via Snackbar. |

## [2026-04-13 19:15] - Offline Support and Stability Fixes

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/AndroidManifest.xml` | EDIT | +1 | Added `INTERNET` permission for local TCP LSP connections. |
| `app/src/main/java/com/example/unstablestudio/data/repository/SettingsRepository.kt` | EDIT | +20 | Added persistence for `lastRootUri` and `openFiles` list. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +30 | Implemented auto-restoration of workspace and tabs on startup. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | ~5 | Fixed `IndexOutOfBoundsException` in TabRow by adding bounds check. |
| `app/src/main/java/com/example/unstablestudio/data/repository/SafDocumentRepository.kt` | EDIT | ~10 | Switched to `DocumentsContract.deleteDocument` for more reliable deletion. Added persistent permission check. |

## [2026-04-13 19:45] - Syntax Fixes and Refresh Logic

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | ~5 | Fixed initialization order of ViewModels to prevent unresolved reference and type inference errors. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/WorkspaceViewModel.kt` | EDIT | ~20 | Improved refresh logic after file deletion/rename to ensure sidebar synchronization. |

### Summary
- **Total Files**: 2
- **Lines Added**: +10
- **Lines Edited**: ~25
- **Lines Removed**: -0

### Notes
- Reason: Fixed compilation errors introduced by recent refactoring and ensured UI stays in sync with file system operations.
- Risks: None.
- Testing: Compiled successfully. Manual verification needed for file deletion refresh.

### Summary
- **Total Files**: 5
- **Lines Added**: +50
- **Lines Edited**: ~15
- **Lines Removed**: -0

### Notes
- Reason: App now behaves like VS Code by remembering the state and working offline. Fixed critical crash when opening multiple tabs.
- Risks: Persistent URI permissions can occasionally be revoked by the OS if the app is unused for a long time.
- Testing: Verified that app restores folders and tabs after being killed and restarted. Compiled successfully.

### Summary
- **Total Files**: 5
- **Lines Added**: +260
- **Lines Edited**: ~30
- **Lines Removed**: -0

### Notes
- Reason: Completed the advanced LSP integration. Users now get code suggestions as they type and documentation on long press.
- Risks: Synchronous `runBlocking` in the completion provider might cause UI lag on very slow servers. Future optimization: Caching or async provider.
- Testing: Compiled successfully. UI logic verified through static analysis.

## [2026-04-13 17:30] - LSP Settings UI Integration

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/data/repository/SettingsRepository.kt` | EDIT | +20 | Added properties to save and load LSP Mode, Host, Port, and Command via SharedPreferences. |
| `app/src/main/java/com/example/unstablestudio/ui/viewmodels/SettingsViewModel.kt` | EDIT | +15 | Exposed the new LSP configuration state flows. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/SettingsScreen.kt` | EDIT | +60 | Built UI for LSP settings (Mode RadioButtons, Host/Port/Command OutlinedTextFields). Added vertical scrolling for smaller screens. |
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +5 | Read LSP settings before starting `LspService` and passed them via Intent extras. |

### Summary
- **Total Files**: 4
- **Lines Added**: +100
- **Lines Edited**: ~10
- **Lines Removed**: -0

### Notes
- Reason: Allowed users to dynamically configure how the IDE connects to the Language Server (TCP vs Local Process) and specify host/ports.
- Risks: If the TCP server is unreachable, the socket connection will time out but shouldn't crash the app due to try-catch blocks.
- Testing: Compiled successfully. UI verified.

### Summary
- **Total Files**: 1
- **Lines Added**: +60
- **Lines Edited**: ~60
- **Lines Removed**: -0

### Notes
- Reason: Replaced the mock Language Server with an architecture capable of spawning an actual Language Server binary or connecting to one via TCP socket (e.g., through Termux).
- Risks: Process execution depends heavily on Android version and target SDK restrictions (W^X execution limits). TCP fallback is provided.
- Testing: Compiled successfully.

### Summary
- **Total Files**: 4
- **Lines Added**: +125
- **Lines Edited**: ~10
- **Lines Removed**: -0

### Notes
- Reason: Initiated the "Heavy/Complex" task of integrating Language Server Protocol. Successfully bound the background service, handled JSON-RPC messaging in the ViewModel, and updated the UI dynamically based on text changes.
- Risks: None. Currently using a Mock PipedStream server.
- Testing: Compiled successfully. Tested `textDocument/publishDiagnostics` mock payload display.

### Summary
- **Total Files**: 6
- **Lines Added**: +255
- **Lines Edited**: ~10
- **Lines Removed**: -0

### Notes
- Reason: Added Settings configuration to manage font size, word wrap, and dynamic FPS targets (30, 60, 120 FPS).
- Risks: None. `preferredRefreshRate` requires API >= 30, which is handled with SDK checks.
- Testing: Compiled successfully.

### Summary
- **Total Files**: 4
- **Lines Added**: +25
- **Lines Edited**: ~40
- **Lines Removed**: -0

### Notes
- Reason: Fixed compilation errors related to `dragAndDropSource` changes in Compose 1.7.0. Enabled proper `.env` file creation without automatic `.txt` extensions. Allowed Drag & Drop between recursive folders.
- Risks: None expected. Move functionality depends on Android SDK >= 24 (Nougat).
- Testing: Validated through static analysis and compilation.

### Summary
- **Total Files**: 4
- **Lines Added**: +10
- **Lines Edited**: ~15
- **Lines Removed**: -0

### Notes
- Reason: Fixed IllegalArgumentException related to Ripple system in Compose 1.7.0+. Polished UI with AutoMirrored icons.
- Risks: None expected.
- Testing: Validated through static analysis and compilation.

### Summary
- **Total Files**: 5
- **Lines Added**: +30
- **Lines Edited**: ~25
- **Lines Removed**: -0

### Notes
- Reason: Project failed to compile due to missing interface implementations, incorrect API usage of sora-editor, and missing Material icons.
- Risks: Mock repository returns empty results for file operations.
- Testing: Ran `./gradlew compileDebugKotlin` successfully.

---

## [2026-04-24 10:30] - Keyboard UX Refinement

### Files Modified
| File | Action | Lines | Details |
|------|--------|-------|---------|
| `app/src/main/java/com/example/unstablestudio/MainActivity.kt` | EDIT | +2 | Enabled `enableEdgeToEdge()` for better inset handling. |
| `app/src/main/java/com/example/unstablestudio/ui/screens/EditorScreen.kt` | EDIT | +25 | Linked `CodingKeyboard` visibility to actual system IME state. Improved focus handling with explicit `showSoftInput` call. Switched to `safeDrawing` insets and added `imePadding()`. |

### Summary
- Fixed issue where the custom coding toolbar appeared without the system keyboard.
- Ensured the system keyboard reliably shows up when the editor is focused.
- Corrected the layout to move content (including the toolbar) above the keyboard using modern Insets API.
- Synchronized focus state with keyboard visibility to prevent "ghost" toolbars.

---

