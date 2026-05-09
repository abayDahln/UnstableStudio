# Repository Guidelines

## Project Structure & Module Organization
This repository is an Android app project with one main module: `:app`.
- Source code: `app/src/main/java/com/example/unstablestudio/`
- UI layers: `ui/components`, `ui/screens`, `ui/viewmodels`, `ui/theme`
- Domain/data/core: `domain`, `data`, `core`, `editor_engine`
- Assets (themes, icons, fonts, runtime files): `app/src/main/assets/`
- Unit tests: `app/src/test/java/`
- Instrumented tests: `app/src/androidTest/java/`

Large generated/vendor-like directories such as `build/`, `app/build/`, and `vscode-1.115.0/` should not be used for feature work unless explicitly required.

## Build, Test, and Development Commands
Use Gradle wrapper from repo root.
- `./gradlew assembleDebug` (Windows: `.\gradlew.bat assembleDebug`): build debug APK.
- `./gradlew installDebug`: install debug build to connected emulator/device.
- `./gradlew testDebugUnitTest`: run JVM unit tests in `app/src/test`.
- `./gradlew connectedDebugAndroidTest`: run instrumentation tests on device/emulator.
- `./gradlew lintDebug`: run Android lint checks.

## Coding Style & Naming Conventions
Code is Kotlin-first with Jetpack Compose and MVVM.
- Follow Kotlin conventions: 4-space indentation, no tabs, descriptive names.
- Types/classes/Composable names: `PascalCase` (`EditorScreen`, `WorkspaceViewModel`).
- Functions/variables/properties: `camelCase`.
- Constants: `UPPER_SNAKE_CASE` (see `AppConstants`).
- Keep screen logic in ViewModels; keep Composables UI-focused.
- Prefer small, focused files; group by feature package already used in `ui/*`, `data/*`, `domain/*`.

## Testing Guidelines
Testing uses JUnit4 (`junit`) with AndroidX test libraries for instrumentation.
- Name tests with subject + behavior, e.g. `AppConstantsTest`, `EditorViewModelLoadsFileTest`.
- Add/adjust unit tests for ViewModel/domain changes.
- Add instrumentation tests for Android framework or UI interaction behavior.

## Commit & Pull Request Guidelines
Git history is not available in this workspace snapshot (`.git` directory missing), so use Conventional Commit style:
- `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`

PRs should include:
1. Clear scope and rationale.
2. Linked issue/task (if available).
3. Test evidence (commands run and results).
4. Screenshots/video for UI changes (Compose screens, editor/workspace behavior).

## Security & Configuration Tips
- Do not commit local machine paths or secrets from `local.properties`.
- Keep language/runtime binaries and large assets intentional and reviewed before adding to `app/src/main/assets/`.
