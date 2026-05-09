# GEMINI.md

## Jetpack Compose Expert
For all Jetpack Compose tasks, follow the workflow and checklists in
`skills/compose-expert/SKILL.md`.

Before answering any Compose question, consult the relevant reference:
- State management -> `skills/compose-expert/references/state-management.md`
- Performance -> `skills/compose-expert/references/performance.md`
- Navigation -> `skills/compose-expert/references/navigation.md`
- (see SKILL.md for the full topic -> file mapping)

For implementation details, check actual source code in
`skills/compose-expert/references/source-code/`.

# Unstable Studio


## Project Overview
Unstable Studio is a native Android IDE built with Kotlin and Jetpack Compose. It aims to deliver a desktop-like IDE experience on mobile, inspired by VS Code architecture and powered by Language Server Protocol (LSP). 

Key features include:
*   A high-performance code editor capable of handling thousands of lines smoothly.
*   A VS Code-inspired modular IDE architecture (workspace, explorer, editor, panels).
*   LSP integration for autocomplete, real-time diagnostics, and go-to-definition / navigation.
*   Theming (light/dark + customizable), fast search, project tree management, and an integrated terminal.
*   Respect for Android constraints (memory/battery/background execution).

*Note: The repository also contains the `vscode-1.115.0` source code, which serves as an architectural reference and inspiration.*

## Building and Running
The project uses the Gradle build system with Kotlin DSL. It is configured as a standard Android application.

*   **Build the project:** 
    ```bash
    ./gradlew build
    ```
*   **Run the application on a connected device or emulator:**
    ```bash
    ./gradlew installDebug
    ```
    *(Alternatively, open the project in Android Studio and run it from the IDE)*
*   **Run unit tests:** 
    ```bash
    ./gradlew test
    ```
*   **Run UI tests:**
    ```bash
    ./gradlew connectedAndroidTest
    ```

## Development Conventions
This project enforces strict, non-negotiable development workflows to maintain discipline and quality:

1.  **Planning First:** Work must be broken down into small checklist items with clear acceptance criteria in `.agent/planning/PLANNING.md` before execution.
2.  **Change Logging:** Every file addition, modification, or removal must be explicitly logged in `.agent/logs/CHANGELOG.md`. This includes the file path, action (ADD/EDIT/REMOVE/FIX/REFACTOR/MOVE), line counts, and a short explanation.
3.  **Self-Analysis:** Before delivery, developers (or agents) must run through the checklist in `.agent/rules/self-analysis.md` to identify potential conflicts, performance regressions, lifecycle/coroutine issues, and Compose recomposition risks.
4.  **Research-First:** If there is any uncertainty about APIs, syntax, or behavior (Android, Compose, LSP, performance data structures), research must be conducted first following `.agent/rules/research-guidelines.md`. Guessing is strictly prohibited.

### Technical Principles
*   **No hardcoded values:** Centralize constants and configuration in dedicated modules.
*   **Decoupling:** Keep modules decoupled using interfaces.
*   **Platform Agnosticism:** Ensure the `editor-core` module is Android-agnostic (pure Kotlin) where possible.
*   **Priorities:** Favor correctness, performance, and maintainability above all else.


