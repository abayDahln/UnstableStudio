# Project Structure - Unstable Studio (Android IDE)

## High-level Modules (Recommended)

```
unstable-studio/
├── app/                       # Android application
├── core/
│   ├── common/                # shared utils, Result, Logger, threading
│   ├── config/                # app config, build constants, feature flags
│   ├── theme/                 # compose theme, colors, typography, spacing
│   ├── model/                 # shared models (FileItem, Project, Range)
│   └── persistence/           # DataStore, preferences
├── feature/
│   ├── workspace/             # project explorer, workspace management
│   ├── editor/                # editor UI + editor core adapter
│   ├── search/                # file search, quick open
│   ├── terminal/              # terminal panel, process I/O
│   └── settings/              # settings UI + storage
├── lsp/
│   ├── protocol/              # LSP types, JSON-RPC models
│   ├── client/                # LSP client (transport + lifecycle)
│   └── bridge/                # mapping offsets/ranges, diagnostics sync
└── editor-core/               # high-perf text engine (rope/piece table), tokenizer
```

## Package Rules
- `feature/*` must not directly access implementation details of other modules without an interface.
- `editor-core` must stay Android-agnostic (pure Kotlin) to be easy to test.

## Editor Core Layering

- `editor-core/text`:
  - Document storage (rope/piece-table)
  - Change events
  - Cursor/selection model
- `editor-core/render`:
  - Layout per line
  - Viewport rendering strategy
- `editor-core/syntax`:
  - Tokenization
  - Incremental updates
- `editor-core/diagnostics`:
  - Issue model
  - Mapping to ranges

## LSP Layering

- `lsp/protocol`: data types + serialization
- `lsp/client`: JSON-RPC requests, notifications, reconnect
- `lsp/bridge`: mapping between editor offsets and LSP positions

## Theme & Constants

- `core/theme`: all color palettes & tokens
- `core/config`: all configuration/feature flags
- `core/common`: shared non-UI constants

## Test Strategy (Recommended)
- `editor-core`: unit tests heavy
- `lsp/*`: protocol serialization tests
- `feature/editor`: UI tests minimal (critical flows)
