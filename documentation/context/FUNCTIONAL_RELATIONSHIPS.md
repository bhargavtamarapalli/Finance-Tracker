# Functional Relationships & Features

This document details the core logic engines, background utilities, and systems that implement the functional operations of the **Finance Tracker** app.

---

## 🔒 1. Authentication & Biometrics Lifecycle

The application provides secure local login via a custom numeric passcode and hardware-based biometric authentication (Fingerprint / Face Unlock).

```
[SplashScreen] 
   │
   ├─► Check session token? ──► [Yes] ──► Auto-login ──► [Dashboard]
   │
   └─► Check biometric toggle? ──► [On] ──► Prompt Biometrics
                                                │
       ┌────────────────────────────────────────┴──────────────┐
       ▼ [Success]                                             ▼ [Fail/Cancel]
  [Dashboard]                                             [AuthScreen (Pin entry)]
```

### Key Components
*   **`PasswordHasher`:** Performs secure local hashing (using standard hashing or PBKDF2/BCrypt equivalents) to store and check user PINs. The raw PIN is never stored in plain text.
*   **`BiometricHelper`:** Inspects system hardware capabilities (`BiometricManager.from(context)`) to verify if security credentials exist (`BIOMETRIC_SUCCESS`). It presents the system-level biometric prompt.
*   **`AuthViewModel`:** Tracks state flows (`isAuthed`, `isLoading`, `errorMessage`). When biometrics succeed, it generates a secure session token and persists it via `EncryptedPrefsManager`.

---

## ☁️ 2. Offline Synchronization & Cloud Backup

The application is completely offline-first. All actions write immediately to the local Room database, ensuring instant responsiveness without network dependencies. Sync is handled on-demand.

```
                  +--------------------------------+
                  |  Local DB: Room (SQLite Tables) |
                  +---------------+----------------+
                                  |
                           Backup Triggered
                                  v
                  +--------------------------------+
                  | JsonDataManager (Moshi Adapt)  | => Compiles Tables into BackupPayload
                  +---------------+----------------+
                                  |
                                  +-----------------------+
                                  |                       |
                           Manual Export            Firebase Sync
                                  v                       v
                           [Download JSON]        [REST HTTP POST/PUT]
                                                          |
                                                    Merge on restore
                                                          v
                                                  Re-populate Room DB
```

### Backup Flow
1.  **Serialization:** The user taps "Backup". `JsonDataManager` queries `FinanceDao` to get all categories and transactions, packing them into a standard `BackupPayload`.
2.  **Moshi Conversion:** Moshi parses the payload object into a single JSON string.
3.  **Firebase Sync:** `FinanceRepository.backupToFirebase(userId)` transmits this JSON securely using HTTPS to the cloud bucket/database under the user's isolated document ID.

### Restore & Merge Flow
1.  **Download:** `restoreFromFirebase(userId)` pulls down the user's latest `BackupPayload` JSON.
2.  **Merging Conflict Rules:**
    *   **Categories:** Imported categories that match existing names/types are merged or updated; completely new categories are appended.
    *   **Transactions:** Transactions are upserted by matching their generated unique IDs. No duplicate transactions are created if the sync is repeated.

---

## ⏰ 3. Daily Reminders & AlarmManager Scheduler

To help users build healthy tracking habits, the application schedules a repeating daily reminder at 9:00 PM (21:00) using Android's system alarm services.

```
[App Launched] ──► ReminderScheduler.scheduleDailyReminder()
                         │
                         ▼ (Configures Calendar for 21:00)
                  [AlarmManager.setInexactRepeating()]
                         │
                         ▼ (Triggers daily at 9:00 PM)
                  [DailyReminderReceiver.onReceive()]
                         │
                         ├─► Creates Notification Channel (SDK >= 26)
                         ├─► Checks / Loads custom "ic_app_logo"
                         └─► Issues Notification (NotificationManager)
```

### Implementation Details
*   **`ReminderScheduler`:** Calculates the Milliseconds remaining until 21:00. If that time has already passed today, it advances the calendar calendar trigger by 1 day (`Calendar.DAY_OF_YEAR, 1`) to avoid triggering an immediate notification upon app launch. It registers the broadcast via a pending intent.
*   **`DailyReminderReceiver`:** A system `BroadcastReceiver` that processes the alarm event. It dynamically sets up the required `NotificationChannel` (importance set to `IMPORTANCE_DEFAULT`) and builds a rich text reminder. Tapping the notification deep-links the user directly back into `MainActivity`.

---

## 📢 4. Admin Console Notifications

Administrators have access to a distinct admin console route to broadcast messages, general updates, or financial tips to users.

*   **`Announcement`:** An immutable data structure depicting published administrative notifications.
*   **ViewModel Sync:** When a user logs in, the `FinanceViewModel` polls or listens to the database for global announcements, showing them as a dismissible marquee or dashboard banner.

---

## 🛡️ 5. Error Handling & State Resilience

To prevent sudden app closures (crashes) and preserve data integrity, the app incorporates defensive error handling:

*   **Network Resiliency (`NetworkMonitor`):** Tracks cellular and Wi-Fi internet state in real-time. If the connection is lost, visual sync indicators are updated, and background network requests are deferred rather than allowed to fail with socket exceptions.
*   **Database Fault Tolerance:** All critical database reads and writes are wrapped in safe coroutine try-catch clauses. ViewModels expose these errors as user-friendly Snackbar triggers rather than hard crashing the app.
*   **Keystore Failover:** If a user's Android security keystore becomes corrupted (common on customized ROMs or older operating systems), `EncryptedPrefsManager` automatically intercepts the cryptographic error, logs it, and falls back to standard key-value storage to preserve settings.
