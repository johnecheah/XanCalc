# XanCalc

XanCalc is a modern, highly functional, and visually polished Android calculator and unit converter application. Designed from the ground up using Jetpack Compose and Material Design 3, it offers standard mathematical computations, advanced scientific calculations, and a complete multi-category unit converter. It maintains a secure history log utilizing local Room database persistence, capped at 6 entries.

![XanCalc Showcase Banner](./app/src/main/res/drawable/img_xancalc_showcase_1781601614249.jpg)

## 🌟 Key Features

- **Standard & Scientific Calculator**: Full algebraic evaluation support for basic arithmetic as well as complex mathematical functions.
- **Comprehensive Unit Converter**: 
  - Supports multiple conversion categories: Area, Length, Weight, Temperature, and more.
  - Native defaults set to **Square Foot to Square Meter** conversion for quick access in the Area category.
- **Smart History Tracking**:
  - Automatically records previous inputs and results.
  - Powered by Room Database for performant local data persistence.
  - Strict pruning policy that caps history logs to a maximum of **6 entries** to keep the workspace clean and resource-friendly.
- **Material 3 Fluid UI**:
  - Beautiful responsive layout adhering to Material Design 3 standards.
  - Fully dynamic color scheme and fluid edge-to-edge screens with complete window insets integration.
  - Accessible touch sizes, micro-interactions, and beautiful negative space focus.

---

## 🛠️ Architecture & Tech Stack

XanCalc follows modern Android development practices and structured clean architecture (MVVM):

- **UI Framework**: Jetpack Compose (100% declarative UI) with Material Design 3 components.
- **Language**: Kotlin.
- **Concurrency**: Kotlin Coroutines & Flow/StateFlow for unidirectional state flow and real-time state manipulation.
- **Database / Persistence**: Room Database (SQLite abstraction layer) with safe, optimized KSP code generation.
- **State Management**: Android Architecture Components `ViewModel` layer separated cleanly from the UI and repository layers.
- **Dependency Management**: Centralized dependency resolution managed cleanly via Kotlin DSL Gradle scripts and version catalog configurations.

---

## 🚀 Getting Started & Installation

To run this project locally, make sure you have the latest stable version of Android Studio installed.

### 1. Prerequisites
- **Android Studio** Ladybug (or newer)
- **JDK 17** or higher
- **Android SDK** Level 34+

### 2. Gradle Sync & Project Build
Clone or download this repository, and open the project root directory in Android Studio. Android Studio will automatically synchronize dependencies. 

To build or run tests from the command line:

```bash
# Compile the complete debug APK
gradle assembleDebug

# Run local JVM unit tests
gradle :app:testDebugUnitTest
```

---

## 🎨 Visual Aesthetics & Polish

Designed with high readability and modern accessibility in mind:
- **Color Theme**: Deep slate canvases with high-contrast indicator highlights.
- **Dynamic Adapters**: Auto-adapts to various mobile, tablet, and foldable orientations.
- **Touch Ergonomics**: All key interaction targets are designed to adhere to a minimum of 48dp x 48dp sizes with responsive tactile material ripple feedback.

---

## 📄 License

This project is open-source and free to distribute under the standard MIT License terms.
