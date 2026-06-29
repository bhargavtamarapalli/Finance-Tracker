# Finance Tracker - Repository Context Documentation

Welcome to the **Finance Tracker** context documentation. This folder is a comprehensive, structured map of the entire repository designed for both developers and agentic AI systems. It provides deep architectural, behavioral, and database flow context so that any agent or developer can instantly understand the project's layout, interactions, and features without having to parse the entire codebase.

---

## 📂 Documentation Directory

Click on any file below to access detailed specifications:

### 1. [System Architecture](context/ARCHITECTURE.md)
*   **Overview:** The top-level design patterns of the application.
*   **Key Concepts:** Model-View-ViewModel (MVVM), unidirectional data flow (UDF), repository patterns, and concurrency (Kotlin Coroutines & Flow).
*   **Visual Map:** Structural diagram linking UI, ViewModels, Repositories, Database, and Receivers.

### 2. [Data Model & Database Schema](context/DATA_MODEL_AND_DB.md)
*   **Overview:** Detailed schema of local storage and data persistence.
*   **Key Concepts:** Room Database setup, table schemas (`transactions`, `categories`), type converters, EncryptedSharedPreferences, and the Moshi-driven JSON Backup/Payload model.
*   **Visual Map:** Entity-Relationship (ER) diagram showing tables, types, keys, and relational fields.

### 3. [Screen & Navigation Map](context/SCREEN_AND_NAVIGATION_MAP.md)
*   **Overview:** Complete visual blueprint of all Jetpack Compose screens, routes, and layout structures.
*   **Key Concepts:** Navigation Compose routes, bottom-bar structure, NavigationDrawer elements (including the Log Out function), and common reusable UI components.
*   **Visual Map:** App Screen Flow chart depicting navigation paths and screen state inputs/outputs.

### 4. [Functional Relationships & Features](context/FUNCTIONAL_RELATIONSHIPS.md)
*   **Overview:** Deep-dive into main application features and cross-cutting systems.
*   **Key Concepts:** Authentication & Biometrics, Offline Synchronization & Backup engines, Daily Reminders & `AlarmManager` schedulers, Admin Console notifications, and error handling.
*   **Visual Map:** Sequential lifecycle flows for background syncing and local reminder scheduling.

### 5. [Testing Strategy & Regression Verification](context/TESTING_STRATEGY.md)
*   **Overview:** Documentation of the testing suite ensuring robust builds.
*   **Key Concepts:** JVM-based Robolectric unit and UI testing, Roborazzi screenshot verification, test coverage areas, and regression prevention rules.

---

## 🎯 How to Use This Documentation
When working on this repository (e.g., adding a feature, fixing a bug, or generating new tests), **always read these context files first**. 

*   **For UI changes:** Refer to [Screen & Navigation Map](context/SCREEN_AND_NAVIGATION_MAP.md) to inspect paths, and check [System Architecture](context/ARCHITECTURE.md) to see how ViewModels drive them.
*   **For Database modifications:** Refer to [Data Model & Database Schema](context/DATA_MODEL_AND_DB.md) to avoid breaking local SQLite tables and ensure clean migrations.
*   **For Sync/Alarm bugs:** Refer to [Functional Relationships](context/FUNCTIONAL_RELATIONSHIPS.md) to understand dependencies on lifecycle events and external configurations.
