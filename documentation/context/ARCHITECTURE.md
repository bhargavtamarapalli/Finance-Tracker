# System Architecture

This document describes the high-level system architecture of the **Finance Tracker** Android application. It highlights the design patterns, concurrency models, data flows, and structural relationships that govern the app's behavior.

---

## 🏛️ High-Level Architectural Pattern: MVVM + Repository

The application follows the **Model-View-ViewModel (MVVM)** architectural pattern combined with the **Repository Pattern** for a clear separation of concerns, high testability, and deterministic offline-first performance.

```
       +-------------------------------------------------------------+
       |                         UI LAYER                            |
       |  [MainActivity] -> [FinanceApp] (Navigation Compose)        |
       |         |                                                   |
       |         v                                                   |
       |  [Jetpack Compose Screens] <---+                            |
       |  (e.g., DashboardScreen,       | Collects StateFlows        |
       |   AddTransactionScreen)        | (UI State / StateFlow)     |
       +---------+----------------------+----------------------------+
                 |                      ^
                 | Invokes UI Events    | Exposes UI State
                 v                      |
       +--------------------------------+----------------------------+
       |                       VIEWMODEL LAYER                       |
       |  [AuthViewModel]               [FinanceViewModel]           |
       |  (Manages Auth & Biometrics)   (Manages DB Flows & Backups) |
       +---------+---------------------------------------------------+
                 |
                 | Invokes suspend functions or returns Flows
                 v
       +-------------------------------------------------------------+
       |                      REPOSITORY LAYER                       |
       |  [AuthRepository]              [FinanceRepository]          |
       |  (Stores session state in      (Mediates Room DB DB access  |
       |   EncryptedPrefsManager)        and Firebase Json backups)  |
       +---------+-------------------------------+-------------------+
                 |                               |
                 v (Room Queries)                v (File/Network IO)
       +---------------------------+   +-----------------------------+
       |       LOCAL DATABASE      |   |        BACKUP ENGINES       |
       | [AppDatabase]             |   | [JsonDataManager] (Local)   |
       |   └── [FinanceDao] (Room) |   | [Firebase Auth/DB] (Cloud)  |
       +---------------------------+   +-----------------------------+
```

---

## 🔄 Core Architectural Principles

### 1. Unidirectional Data Flow (UDF)
State flows *down* and events flow *up*:
*   **State Down:** ViewModels maintain the single source of truth for the screen states in private `MutableStateFlow` properties, exposing them as read-only `StateFlow` streams. The UI screens collect these streams using `.collectAsStateWithLifecycle()` to automatically update the Composables on changes.
*   **Events Up:** All user interactions (such as pressing buttons, entering text, or triggering pull-to-refresh) are passed up to the ViewModel as function calls (e.g., `viewModel.addTransaction(...)`), preventing UI components from modifying states directly.

### 2. Offline-First Single Source of Truth (SSOT)
*   The **Room SQLite Database** is the local Single Source of Truth for categories and financial transactions.
*   The UI never talks directly to any cloud databases or networks. It queries local flows.
*   Background sync operations (e.g., Firebase backup/restore) update the local Room database, which in turn automatically propagates fresh data to the UI via active Flow collections.

### 3. Separation of Concerns
*   **UI Components:** Lightweight Jetpack Compose functions completely isolated from business rules.
*   **ViewModels:** Survival across configuration changes (using Jetpack `ViewModel`). They coordinate the business logic and convert raw repository models into visual UI states.
*   **Repositories:** Encapsulate data source logic. Repositories choose whether to fetch data from memory, EncryptedSharedPreferences, SQLite databases, or make network calls.
*   **Data Models:** Pure data structures without any business actions.

---

## ⚡ Concurrency & Threading Model

To keep the UI running at a silky-smooth 60fps (or 120fps) without stuttering, the application carefully manages threading:

*   **UI Main Thread:** Reserved strictly for rendering Jetpack Compose layouts and simple UI states.
*   **IO Thread Pool (`Dispatchers.IO`):** All database operations, file reading/writing (JSON backups), network sync operations, and encryption/decryption are dispatched to the IO thread pool to prevent blocking the main thread.
*   **Default Pool (`Dispatchers.Default`):** Used for CPU-heavy processes, such as hashing passwords (using PBKDF2/BCrypt equivalents), generating analytical charts, or compressing payloads.

### Kotlin Coroutines & Flows
*   **Coroutines:** Launched in standard lifecycles (using `viewModelScope` in ViewModels and `lifecycleScope` in `MainActivity`).
*   **Flows:** Used for real-time reactive streams. For instance, Room DAO returning `Flow<List<TransactionWithCategory>>` allows the entire app to react instantly when a transaction is added, updated, or removed from anywhere.
