# Testing Strategy & Regression Verification

This document outlines the testing methodologies, frameworks, test catalogs, and commands used to verify the correctness, reliability, and visual appearance of the **Finance Tracker** codebase.

---

## 🧪 Testing Frameworks

The project maintains a comprehensive, fast-executing test suite designed to run locally on the JVM without requiring physical devices or active emulators.

### 1. JUnit 4
Standard testing runner for isolated logic, math verifications, and simple model transformations (e.g., `ExampleUnitTest.kt`).

### 2. Robolectric (`RobolectricTestRunner`)
Enables full-fidelity Android framework testing directly in the JVM. This allows the testing of Activities, Contexts, SQLite (Room), SharedPreferences, and complex Compose layouts quickly.

### 3. Roborazzi (`captureRoboImage`)
A state-of-the-art screenshot testing library integrated with Robolectric. It renders Composables to image files, which are stored and automatically diffed against baseline screenshots to prevent subtle UI distortions or color regressions.

---

## 📂 Test Directory Mapping

All tests reside under `/app/src/test/java/com/example/`. Here is the purpose of each test class:

| Test Class | Type | Targets | Validates |
| :--- | :--- | :--- | :--- |
| `ExampleUnitTest` | Local Unit Test | Arithmetic, Mock objects. | Simple state assertions. |
| `ExampleRobolectricTest` | Robolectric Test | Application context, Resource strings. | Confirms valid app titles and resource bindings. |
| `AddTransactionScreenTest` | Robolectric UI | Transaction form inputs, Category grids. | Form submission, numeric input validations, category changes. |
| `DashboardScreenTest` | Robolectric UI | Summary cards, recent lists, state changes. | Calculates exact balances (Total, Income, Expense) correctly. |
| `AnalyticsScreenTest` | Robolectric UI | Charts, percentages, range switchers. | Visualizes category distributions and period navigations. |
| `CategoryManagementScreenTest` | Robolectric UI | Add category sheet, archive triggers. | Creating and editing custom categories. |
| `SettingsScreenTest` | Robolectric UI | Pin locks, biometrics toggles, backup utilities. | Secure toggle settings and backup export requests. |
| `SplashScreenTest` | Robolectric UI | Core entry point, redirection logic. | Auto-routing behaviors based on session states. |
| `TransactionHistoryScreenTest` | Robolectric UI | Filtering cards, date sliders. | Filtering outputs by string matching or specific dates. |
| `OfflineSyncAndNotificationTest` | Robolectric System | Alarm schedulers, broadcast triggers. | Schedules standard 21:00 alarms and registers notification outputs. |
| `GreetingScreenshotTest` | Roborazzi Screenshot | Theme headers, font weights. | Theme colors and typography layouts on simulated Pixel 8 screens. |

---

## 🚀 Execution Commands

To execute or record reference screenshots, use the following commands.

### 1. Run Standard Unit and Robolectric UI Tests
Runs all business logic and UI state transition tests:
```bash
gradle :app:testDebugUnitTest
```

### 2. Verify UI Screenshots (Visual Regression)
Compares the current UI layouts against baseline screenshots:
```bash
gradle :app:verifyRoborazziDebug
```

### 3. Record Baseline Screenshots
If you have intentionally modified UI styles, colors, or fonts, update the reference baselines:
```bash
gradle :app:recordRoborazziDebug
```

---

## 🛡️ Regression Prevention Rules for Future Agents

*   **Offline-First Compliance:** Tests modifying Room records must complete without mocking network layers.
*   **Database Version Integrity:** If changes are made to `Category.kt` or `TransactionEntity.kt`, verify that tests pass or update the database version in `AppDatabase.kt` to trigger safe upgrades.
*   **TestTags Coverage:** Always apply `Modifier.testTag("snake_case_id")` to any new interactive elements. All UI integration tests utilize these tags for reliable node selection.
