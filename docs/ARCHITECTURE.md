# Planora Architecture

This document describes the architectural patterns and module structure used in the **Planora** Kotlin Multiplatform (KMP) project. The architecture is designed to be highly scalable, modular, and maintainable, ready for production use.

## Module Structure

The project follows a feature-based multi-module approach. This ensures separation of concerns, improves build times, and makes it easier for multiple developers to work on the project simultaneously.

- **`composeApp`**: The main entry point of the client applications. It is a KMP library module containing the shared Compose Multiplatform UI (`App.kt`) and platform-specific entry points (Android `MainActivity.kt` in `androidApp` module, iOS `MainViewController.kt`).
- **`androidApp`**: The pure Android application entry point. It depends on `composeApp` and provides the Android `Application` class and configuration needed for Android build tools (AGP 9+ compatibility).
- **`core`**: Contains base classes and utilities shared across all modules. This includes:
    - **Base Architecture**: `BaseViewModel`, `UiState`, `UiEvent`, `UiEffect` for the MVI pattern.
    - **Theme**: `PlanoraTheme`, colors, typography, shapes.
    - **Navigation Definitions**: Type-safe screen definitions (e.g., `Screen.Home`) used by Navigation 3.
- **`shared`**: General cross-platform business logic, utility functions, and platform expect/actual implementations (e.g., `getPlatformName()`).
- **`server`**: A placeholder for the Ktor-based backend application. This module will house REST APIs, WebSockets, and database interactions (PostgreSQL via Exposed).
- **`feature-*` modules** (e.g., `feature-auth`, `feature-home`): Self-contained modules that represent a specific feature or domain. Each feature module contains its own UI (screens), domain logic (use cases), and data layer (repositories).

## Architectural Patterns

### MVI (Model-View-Intent) + Clean Architecture

Each feature follows a strict MVI and Clean Architecture pattern:

#### 1. Presentation Layer (MVI)
The UI is built using Compose Multiplatform and driven by the MVI pattern.
- **`UiState`**: A data class representing the complete state of a screen (e.g., `LoginState`). The UI observes this state.
- **`UiEvent`**: A sealed interface representing user actions or system events (e.g., `LoginEvent.LoginClicked`). The UI sends these events to the ViewModel.
- **`UiEffect`**: A sealed interface for one-off side effects like navigation, showing snackbars, or toasts (e.g., `LoginEffect.NavigateToHome`).
- **`ViewModel`**: Inherits from `BaseViewModel`. It consumes `UiEvent`s, executes business logic (via domain UseCases), updates the `UiState`, and emits `UiEffect`s.
- **`Screen`**: Compose functions that observe state via `collectAsStateWithLifecycle` and handle effects via `CollectEffect` extension.

#### 2. Domain Layer
Contains the core business logic.
- **Use Cases**: Encapsulate specific business rules and execute actions (e.g., `LoginUseCase`).
- **Domain Models**: Plain data models independent of any specific data source or UI framework.
- **Repository Interfaces**: Define the contracts for data operations.

#### 3. Data Layer
Handles data retrieval and persistence.
- **Repository Implementations**: Implement the interfaces defined in the domain layer. They orchestrate data between local and remote sources.
- **Data Sources**: Code that interacts directly with Ktor (network) or SQLDelight/DataStore (local).

## Technology Stack

The project relies on a modern Kotlin Multiplatform stack:
- **UI**: Compose Multiplatform
- **Navigation**: JetBrains Navigation 3 (Type-safe, state-based navigation via `NavDisplay`)
- **Dependency Injection**: Koin (Cross-platform DI setup with feature-specific modules)
- **Networking**: Ktor Client (Multiplatform HTTP client)
- **Local Persistence**: SQLDelight (Database) and DataStore (Key-Value preferences)
- **Concurrency**: Kotlin Coroutines & Flow
- **Serialization**: `kotlinx.serialization`

## Navigation Strategy

Navigation is handled centrally in `App.kt` using Navigation 3's `NavDisplay`.
- **Type-safe routes**: Defined in the `core` module as `@Serializable` classes/objects (e.g., `Screen.Login`).
- **State-driven**: The back stack is a developer-controlled `SnapshotStateList<Screen>`.
- **Feature Encapsulation**: Feature screens are completely decoupled. They accept callbacks (e.g., `onNavigateToHome: () -> Unit`) which are implemented in the central `App.kt` router.

## Dependency Injection (DI)

Koin is used for dependency injection across all platforms.
- **Module Registration**: Each feature module provides its own Koin module (e.g., `authModule`, `homeModule`).
- **Central Initialization**: `initKoin()` is defined in `composeApp` and calls all feature modules.
- **Platform Context**: Platform-specific setups (like Android Context) are injected at the platform entry points (`PlanoraApplication.kt` for Android, `MainViewController.kt` for iOS).
