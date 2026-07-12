# App Features & Functional Specifications

This document defines the complete set of features, user roles, interactive UI pages, database integrations, and developer settings implemented in the current version of the **Finance Tracker** application. This specification serves as the foundational blueprint for future feature expansions, database migrations, and the integration of a future **Web Admin Panel**.

---

## 👥 1. User Roles & Access Control (RBAC)

The application implements a simple but robust Role-Based Access Control system to split user permissions across three distinct tiers:

```
                  +--------------------------------+
                  |         Session Launch         |
                  +---------------+----------------+
                                  |
                   Select Login type / Auth Level
                                  v
         ┌────────────────────────┼────────────────────────┐
         ▼ [Guest Session]        ▼ [Normal User]          ▼ [Admin User]
  ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐
  │ - Local DB Ledger    │ │ - Local DB Ledger    │ │ - Local DB Ledger    │
  │ - Local JSON Backup  │ │ - Local JSON Backup  │ │ - Local JSON Backup  │
  │ - Restricted Cloud   │ │ - Cloud Firebase Sync│ │ - Cloud Firebase Sync│
  │ - Restricted Admin   │ │ - Profile Management │ │ - Profile Management │
  │                      │ │                      │ │ - Admin Console Acc  │
  └──────────────────────┘ └──────────────────────┘ └──────────────────────┘
```

### 1.1 Guest Mode
*   **Access Type**: Instant password-less access.
*   **Features Available**: Local database ledger, adding transactions, setting a local monthly budget, local JSON backup and restore, category management, analytics, and history search/filters.
*   **Restrictions**: 
    *   **Cloud Sync Hidden**: The "Backup to Cloud" and "Restore from Cloud" settings are hidden to prevent syncing local guest data with Firebase.
    *   **Admin Console Locked**: Cannot access or navigate to the Admin Console.
    *   **Sign-In CTA**: The settings panel displays a "Sign In / Register" CTA instead of a "Log Out" button.

### 1.2 Registered User (Authenticated)
*   **Access Type**: Requires email/passcode verification.
*   **Features Available**: Access to all Guest features, plus:
    *   **Cloud Backup/Restore**: Securely backs up local Room data into Firebase under their isolated Firebase User ID.
    *   **Profile Management**: Edit name and email from the Settings screen.
    *   **Log Out**: Prominently displayed in settings, clearing the local session preference and returning to the Auth screen.

### 1.3 Administrator User (Admin Role)
*   **Access Type**: Requires login credentials flagged with the `ADMIN` role in user records.
*   **Features Available**: Access to all Registered User features, plus:
    *   **Admin Console Navigation**: Visible in the left navigation drawer and settings screen.
    *   **Broadcast Announcements**: Author and publish global system updates, announcements, or privacy warnings to the announcements board.

---

## 📈 2. Core Ledger & Transaction Management

The transaction manager handles the creation, editing, deletion, and categorization of income and expenses.

### 2.1 Add & Edit Transactions
*   **Interactive FAB**: Accessible from the Dashboard screen.
*   **Form Properties**:
    *   **Amount**: Standard numeric field (defaults to rupees).
    *   **Payee/Store**: Name of the transaction source or recipient.
    *   **Date Picker**: Selects transaction execution date (defaults to current date, future dates blocked).
    *   **Notes (Optional)**: Free-text descriptions or details.
    *   **Category Lazy Row**: Scrollable selector displaying active category chips.
    *   **Payment Method Selector**: Cash, Card, UPI, Bank Transfer.
    *   **Save Button**: Commits details to local SQLite DB via Room and navigates back to Dashboard.

### 2.2 Dashboard Calculations & Budgeting
*   **Total Balance**: Dynamic value calculating `Sum(Income) - Sum(Expenses)`.
*   **Summary Cards**: Renders green Income card and red Expense card for the current month.
*   **Monthly Budget Goal**:
    *   Users can set a target budget (e.g. ₹100,000) from Settings.
    *   Displays progress indicators and a "Budget Left: ₹X" banner on the Dashboard.
    *   **Budget Exceeded State**: Displays a red warning banner once expenses surpass the goal.

---

## 🔍 3. Transaction History & Advanced Filters

The **Transaction History** view provides an interactive spreadsheet-style ledger to track records.

### 3.1 Search & List Organization
*   **Real-time Search**: Triggers text matching on the `source` (Payee/Store) property.
*   **Date Grouping**: Groups transactions chronologically under distinct daily headers (e.g. "July 12, 2026").

### 3.2 Advanced Filters Sheet
*   **Date Range Selector**: Chooses custom start and end date boundaries (presets include Day, Week, Month, Year).
*   **Category Chips**: Dynamic multiple-selection category chip grid to filter items.
*   **Price Range Slider**: Filters transactions falling between minimum and maximum amount boundaries.
*   **Transaction Type Selector**: Toggle button to display All, Income, or Expense items.
*   **Reset Button**: Wipes out active filters instantly.

---

## 📊 4. Interactive Analytics Engine

Provides visual cash flow diagrams to help users audit spending behavior.

### 4.1 Visual Charts
*   **Expense vs. Income Chart**: Renders interactive bar charts depicting periodic comparisons.
*   **Monthly Summary Pills**: Pill-shaped components rendering Income, Expenses, and Savings sums.

### 4.2 Category Breakdown List
*   Lists spending categories ordered by highest expenditure.
*   Displays progress bar representation indicating percentage of total monthly spending.

---

## 🏷️ 5. Category Customization (`CategoryChipGrid`)

Centralized and refactored category component designed for both filter sheets and custom customization boards.

*   **Flow Layout**: Employs Material 3 `FlowRow` to render category chips dynamically without vertical list clipping.
*   **Income / Expense Category Separation**: Displays categories categorized under active tabs.
*   **Rename & Edit Options**: Long-pressing a chip reveals a context dropdown menu with options to **Rename** or **Delete/Archive** the category.
*   **Duplicate Validation**: Rejects category creation requests if a matching category name/type already exists.

---

## 💾 6. Backup, Sync & Devmode Engines

The backend synchronization engine implements offline-first resilience.

### 6.1 Local JSON Backups
*   **Export**: Compiles Category and Transaction tables, converts them to JSON using Moshi, and saves them to local storage.
*   **Restore**: Loads local JSON, checking and resolving entity IDs to update Room tables.

### 6.2 Firebase Cloud Sync
*   Registered users upload and download SQLite records securely to Firebase Realtime Database.
*   **Conflict Resolution**: Merges categories by name/type, and upserts transactions using unique UUID keys.

### 6.3 Developer Settings (Dev Mode)
*   **Dynamic Access**: Visible only under debug builds or testing environments.
*   **Seed Demo Transactions**: Wipes skeleton transaction lists and seeds complete mockup transaction logs (Income: ₹92,000, Expenses: ₹33,400, Balance: ₹58,600).
*   **Clear All Data**: Completely truncates local sqlite tables (`transactions`, `categories`) after user confirmation.

---

## 🏗️ Future Web Admin Panel Architecture

This feature list serves as the model configuration for the **Web Admin Panel** roadmap:
1.  **User Monitoring**: View user sign-ups, session dates, and backup state statistics.
2.  **Ledger Audits**: Read transaction log metrics (excluding decrypted private details).
3.  **Broadcast Manager**: Edit and manage announcements dynamically synced directly into the mobile app's Admin drawer.
