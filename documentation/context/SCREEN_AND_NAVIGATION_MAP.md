# Screen & Navigation Map

This document serves as a complete blueprint for all application views, navigation configurations, bottom bars, drawer items, and shared UI elements in the **Finance Tracker** app.

---

## 🗺️ Navigation Topology

The application uses **Jetpack Navigation Compose** for handling all transitions. The root entry point is `FinanceApp` which coordinates a persistent modal navigation drawer, a dynamic bottom bar, and the primary content backstack.

```
                         +-----------------------+
                         |     SplashScreen      |
                         +-----------+-----------+
                                     |
                         Biometric/Saved Session?
                         [No]        |        [Yes]
                     +---------------+---------------+
                     v                               v
             +---------------+               +---------------+
             |  AuthScreen   |               |  FinanceApp   | (Scaffold Main Frame)
             +-------+-------+               +-------+-------+
                     | Auth Success                  |
                     +-------------------------------+
                                                     |
                                     +---------------+---------------+
                                     |  Navigation Drawer & Bottom   |
                                     +---------------+---------------+
                                                     |
             +---------------------+-----------------+---------------------+
             v                     v                 v                     v
     +---------------+     +---------------+ +---------------+     +---------------+
     |  Dashboard    |     |  Transaction  | |   Analytics   |     |   Settings    |
     |  Screen       |     |  History      | |   Screen      |     |   Screen      |
     +-------+-------+     +---------------+ +---------------+     +-------+-------+
             |                                                             |
             +---------------+                                             v
             |               |                                     +---------------+
             v               v                                     | Category Mgmt |
     +---------------+ +---------------+                           +---------------+
     |  Add/Edit     | | Admin Console |                                   v
     |  Transaction  | | (Admins Only) |                           +---------------+
     +---------------+ +---------------+                           | Local Backup/ |
                                                                   | Firebase Sync |
                                                                   +---------------+
```

---

## 📍 Route Identifiers

The navigation graph defines the following destinations:

| Route / Screen | Destination Composable | Purpose | Access Level |
| :--- | :--- | :--- | :--- |
| `Screen.Splash` | `SplashScreen` | System initialization, checks session / biometric support. | Public |
| `Screen.Auth` | `AuthScreen` | Passcode entry, registration, password verification. | Public |
| `Screen.Dashboard` | `DashboardScreen` | Overall balances, income/expense cards, recent list. | Authenticated |
| `Screen.AddTransaction` | `AddTransactionScreen` | Form to create/edit income and expense items. | Authenticated |
| `Screen.History` | `TransactionHistoryScreen` | Searchable, paginated list of all financial records. | Authenticated |
| `Screen.Analytics` | `AnalyticsScreen` | Charts, category percentages, cash flows. | Authenticated |
| `Screen.Settings` | `SettingsScreen` | Theme selection, pin reset, local JSON export, manual sync. | Authenticated |
| `Screen.CategoryMgmt` | `CategoryManagementScreen` | Custom category list, add, archive, edit icons. | Authenticated |
| `Screen.AdminConsole` | `AdminConsoleScreen` | Push announcements, broadcast message cards. | Admin Role Only |

---

## 📱 Navigation Shell Structure (`FinanceApp.kt`)

The navigation container implements a Material 3 **ModalNavigationDrawer** wrapped around a core **Scaffold**.

### 1. Modal Navigation Drawer Items
*   **Overview Dashboard:** Takes user to the main `Dashboard`.
*   **Transaction History:** Takes user to the historical spreadsheet view.
*   **Analytics & Visuals:** Takes user to charts and metrics.
*   **Settings & Backups:** Takes user to settings controls.
*   **Admin Console:** Visible *only* if the logged-in user is flagged as an administrator (`user.isAdmin == true`).
*   **Log Out:** Positioned prominently at the bottom of the drawer, it closes the drawer, clears security state, resets memory references, and redirects immediately to the `AuthScreen` backstack root.

### 2. Bottom Navigation Bar
Provides rapid, single-tap thumb access to the four main application panes:
1.  **Dashboard:** Icon representation: `Icons.Default.Dashboard` / `Icons.Outlined.Dashboard`
2.  **History:** Icon representation: `Icons.Default.History` / `Icons.Outlined.History`
3.  **Analytics:** Icon representation: `Icons.Default.BarChart` / `Icons.Outlined.BarChart`
4.  **Settings:** Icon representation: `Icons.Default.Settings` / `Icons.Outlined.Settings`

---

## 🧩 Reusable UI Components (`CommonComponents.kt`)

To maintain clean code and visual uniformity, common elements are separated into centralized components:

*   **`FinanceButton` & `FinanceOutlinedButton`:** Styled buttons implementing standard M3 paddings, rounded shapes, and accessibility text scaling support.
*   **`FinanceCard`:** Elevations and borders aligned with Material Design 3 guidelines to present clean transaction details, balances, and setting panels.
*   **`TimePeriodSelector` & `PeriodNavigator`:** Shared widgets used by both the History and Analytics screens to choose, swipe, or increment active dates (Days, Weeks, Months, Years, or custom ranges).
*   **`CustomPeriodPickerDialog`:** A robust custom date range selector that avoids heavy external calendar dependencies.
*   **`MonthPickerContent` & `YearPickerContent`:** Clean grid selectors to switch specific calendar increments.
