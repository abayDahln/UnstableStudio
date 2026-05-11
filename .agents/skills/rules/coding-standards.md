# Coding Standards - Unstable Studio (Kotlin + Compose)

## Kotlin Conventions

### Naming
- Classes/Interfaces: PascalCase (`EditorViewModel`, `LspClient`)
- Functions/variables: camelCase (`renderLine`, `tokenCache`)
- Constants: UPPER_SNAKE_CASE only for true compile-time `const val`
- Files: PascalCase for the main class in a file (`EditorState.kt`)

### Nullability
- Avoid `!!`.
- Prefer early returns + safe calls.

```kotlin
// ✅ Good
val name = user?.name ?: return

// ❌ Bad
val name = user!!.name
```

### Immutability
- Prefer `val`.
- Domain data models should be immutable.

### Sealed classes for state
```kotlin
sealed interface LoadState {
  data object Idle : LoadState
  data object Loading : LoadState
  data class Error(val message: String) : LoadState
}
```

## Coroutines & Flow

### Structured Concurrency
- Every coroutine must have a clear scope (ViewModelScope, lifecycleScope, or an injected scope).

### Dispatchers
- I/O: `Dispatchers.IO`
- CPU heavy parsing/tokenization: `Dispatchers.Default`
- UI updates: Main (default di ViewModelScope)

### Flow collection
- UI state: `StateFlow` + `collectAsStateWithLifecycle()`
- One-off events: `SharedFlow` or Channel

## Jetpack Compose Standards

### State hoisting
- UI composables should receive state + callbacks.
- Avoid keeping state in lower layers unless it is strictly UI-only state.

### Recomposition efficiency
- Use `remember` and `derivedStateOf` for computations.
- Avoid creating new objects in composable parameters if they can be hoisted.

```kotlin
// ✅ Good
val filtered by remember(items, query) {
  derivedStateOf { items.filter { it.matches(query) } }
}

// ❌ Bad
val filtered = items.filter { it.matches(query) } // recompute on every recomposition
```

### Stable keys for lists
- Use stable keys (file path, id) for LazyList.

## Anti-Hardcode (Config/Constants)

### Single source of truth
All static values must be sourced from:
- `core:config` (BuildConfig/Gradle properties)
- `core:constants` (domain/UI constants)
- `core:theme` (colors, typography, spacing)

Avoid:
- Magic numbers for debounce, timeouts, max history, sizes
- Scattered strings for routes/command ids

Example:
```kotlin
// ✅ Good
val timeoutMs = AppConfig.lspTimeoutMs
val debounceMs = UiConstants.DEBounceMs

// ❌ Bad
val timeoutMs = 15000
val debounceMs = 300
```

## Error Handling
- Use `Result<T>` or a sealed result type for domain operations.
- In the UI layer, show user-friendly errors.
- Use a logger wrapper (not `println`).

## Logging
- Provide a `Logger` abstraction (interface) to allow swapping implementations.
- Do not log sensitive data.

## Code Style
- Use guard clauses to reduce nesting.
- Functions should stay within ~30-60 lines (split if larger).
- One file should have one primary responsibility.
