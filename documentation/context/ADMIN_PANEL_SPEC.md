# Web Admin Panel — MVP & Feature Specifications

This document defines the Minimum Viable Product (MVP), feature list, and system design for the future **Web Admin Panel** of the Finance Tracker application. The Web Admin Panel is designed as a secure, real-time administrative interface enabling system administrators to monitor active users, manage app-wide announcements, configure system modes, and audit backup analytics.

---

## 🧭 1. Architectural System Overview

The Web Admin Panel integrates directly with the existing **Firebase** infrastructure (Authentication, Firestore/Realtime Database, and Cloud Functions) serving the mobile client. This ensures immediate synchronization of configurations and announcements between the administration web portal and active mobile devices.

```
+------------------+         Sync (Realtime / Firestore)         +-------------------+
|  Mobile Client   |<===========================================>| Firebase Services |
|   (Android App)  |                                             |  - FirebaseAuth   |
+------------------+                                             |  - Firestore      |
                                                                 |  - Cloud Storage  |
                                                                 +---------^---------+
                                                                           |
+------------------+                                                       |
|  Web Admin Portal|<======================================================+
| (Next.js / Vite) |                     Admin Sync
+------------------+
```

---

## 🔒 2. MVP Feature List

The MVP focuses on establishing secure access control, basic monitoring capability, system mode toggles, and managing the global announcement feeds.

### 2.1 Administrator Authentication & Session Management
- **Role-Based Login**: Login screen requiring standard email/password credentials.
- **Admin Verification**: Access is strictly restricted to accounts containing the `ADMIN` role. Non-admin logins are automatically rejected and redirect to a permission denied screen.
- **Session Auto-Timeout**: Automatic logout after 15 minutes of inactivity to protect sensitive administrative actions.

### 2.2 Dashboard Analytics & User Monitoring
- **Live Connection Counter**: Real-time ticker showing active WebSockets/connections and daily active user sessions (DAU).
- **Registration Metrics**: Graphical representation of user sign-up trends over days/weeks.
- **User Detail Table**:
  - Searchable list of all registered users displaying UID, Display Name, Email, Role (USER/ADMIN), Creation Date, Last Login, and Cloud Backup Health.
  - **Admin Elevation**: Quick-action toggles to promote standard users to the `ADMIN` tier or revoke permissions.

### 2.3 Broadcast Announcement Manager (Real-time Feed)
- **Announcement Table**: View all active and scheduled announcements grouped by categories (e.g., *System Update*, *Chitti Info*, *Business Tip*, *Privacy*, *General*).
- **Composer Console**: Form to draft new broadcasts:
  - **Title**: Header text.
  - **Category Dropdown**: Categorizes the warning or information.
  - **Content Area**: Markdown-supported rich text body.
  - **Scheduled Release**: Optional picker to delay release.
- **Interactive Actions**: Live edit, delete, or temporarily archive announcements. Updates are pushed instantly to mobile users via database listener triggers.

### 2.4 App Configuration & System Lockdown Toggles
- **App Mode Selector**: Dropdown to switch the global application mode:
  - `Normal Mode`: Full transactional and backup functionality available to users.
  - `Maintenance Mode`: Restricts cloud-sync functionality on the client app, displaying a warning banner to mobile users.
  - `Lockdown Mode`: Temporarily locks all cloud connections to resolve security patches.
- **Global Reset Fallback**: Wipe mock test databases or clear stale cloud tables during dev/staging cycles.

---

## 📈 3. MVP Database Schema (Admin Collections)

The panel interacts with the following Firestore collections:

### 3.1 `announcements` (Collection)
```json
{
  "id": "announcement_uuid",
  "title": "System Security Upgrade",
  "content": "We have successfully rolled out end-to-end local encryption...",
  "category": "System Update",
  "timestamp": 1780579200000,
  "authorId": "admin_user_uuid",
  "status": "PUBLISHED"
}
```

### 3.2 `system_config` (Document)
```json
{
  "app_mode": "NORMAL",
  "min_required_version": "1.1",
  "maintenance_message": "Scheduled cloud maintenance is underway.",
  "last_updated": 1780579200000,
  "updated_by": "admin_user_uuid"
}
```

---

## 🗺️ 4. MVP UI/UX Sitemap

The frontend structure of the Web Admin Portal is partitioned into four primary views:

1.  **`/login`**: Centered card layout with secure email/password input, loading states, and error handling.
2.  **`/dashboard`**: Unified landing grid displaying high-level key metrics (DAU, Total Backups, App Mode) and system status status pills.
3.  **`/users`**: Data table with paginated user records, search filters, and promotion action dialogs.
4.  **`/broadcasts`**: Split-pane view featuring the announcement lists on the left, and the rich text compose editor on the right.
