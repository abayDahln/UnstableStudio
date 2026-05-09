# 🚀 Unstable Studio

**Unstable Studio** is a high-performance, native Android IDE designed to bring a desktop-grade development experience to your pocket. Built from the ground up with **Kotlin** and **Jetpack Compose**, it draws inspiration from VS Code's modular architecture while staying optimized for mobile constraints.

![Banner Placeholder](https://via.placeholder.com/1200x400?text=Unstable+Studio+-+Modern+IDE+for+Android)

## ✨ Features

- **⚡ High-Performance Editor**: Powered by the Sora Editor engine, capable of handling thousands of lines with smooth scrolling and syntax highlighting.
- **🏗️ Modular Architecture**: Inspired by VS Code, featuring a workspace explorer, multi-tab editor, and extensible panel system.
- **🔍 LSP Integration**: Real-time diagnostics, autocomplete, and "Go to Definition" support via Language Server Protocol.
- **🎨 Premium UI/UX**: A modern, Jetpack Compose-based interface with full Material 3 support, dynamic theming, and smooth animations.
- **💻 Integrated Terminal**: A functional terminal environment for running build commands and scripts directly on your device.
- **📂 Project Management**: Fast file search, tree-view explorer, and seamless workspace management.

## 🛠️ Technology Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose)
- **Architecture**: MVVM + Clean Architecture
- **Editor Engine**: [Sora Editor](https://github.com/Rosemoe/sora-editor)
- **Asynchronous**: Kotlin Coroutines & Flow
- **Dependency Management**: Gradle Version Catalog (libs.versions.toml)

## 🚀 Getting Started

### Prerequisites
- Android Device/Emulator (API 26+)
- Android Studio Ladybug or newer
- JDK 17

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/username/UnstableStudio.git
   ```
2. Open the project in Android Studio.
3. Build the project using the Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install to your device:
   ```bash
   ./gradlew installDebug
   ```

## 🏗️ Project Structure

```text
app/
├── src/main/java/com/example/unstablestudio/
│   ├── core/           # Core logic and utilities
│   ├── data/           # Repository and Data sources
│   ├── domain/         # Use cases and Models
│   ├── editor_engine/  # Integration with Sora Editor
│   └── ui/             # Compose screens, components, and themes
├── src/main/assets/    # Fonts, icons, and proot binaries
└── build.gradle.kts    # Module configuration
```

## 🗺️ Roadmap

- [ ] Full Git integration (Clone, Commit, Push/Pull)
- [ ] Improved LSP support for more languages (Java, Python, C++)
- [ ] Plugin system for community extensions
- [ ] Remote development (SSH/Code-server) support

## 🤝 Contributing

Contributions are welcome! Please read `AGENTS.md` for our internal development guidelines and coding standards.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ for the Android Development Community
</p>
