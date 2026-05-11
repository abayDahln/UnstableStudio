# Self-Analysis Checklist (Unstable Studio)

## Before Delivering Code

### Build & Compilation
- No compile errors
- No critical warnings (nullability, unused)
- Clean imports

### Compose Correctness
- No state leakage (proper state hoisting)
- No heavy recomposition without `remember/derivedStateOf`
- `LazyList` uses stable keys

### Concurrency
- No coroutine leaks
- Heavy work uses the correct dispatcher
- Debounce/throttle applied for editor events (didChange/completion)

### Editor Performance
- No O(n^2) behavior for common operations (typing, scrolling)
- Viewport-based rendering
- Efficient offset <-> line/col mapping

### LSP
- Requests/notifications follow the spec
- Diagnostics updates do not wipe state without a reason
- Clear reconnect strategy

## How to Report Findings

Format:
- Issue
- Location (module/file)
- Impact
- Suggested fix
