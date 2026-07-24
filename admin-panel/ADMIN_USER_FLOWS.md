# 🖤 Obsidian Analytics — Admin User Flows

> **Version:** v2.4.2 · **Platform:** Web + Mobile · **Scope:** Admin Role Only

---

## 📐 High-Level Navigation Map

```
Admin Entry → [Login / Auth] → Dashboard (Overview)
                                     │
             ┌───────────────────────┼────────────────────────┐
             │                       │                        │
        User Mgmt              System Oversight           Settings
             │                  (Analytics)                   │
     ┌───────┴──────┐           ┌────┴────┐           ┌──────┴──────┐
  Add User   Manage         Incident   Chart         API Keys   System
  Modal      User Row        Logs     Filters        Panel      Config
```

---

## FLOW 1 — Login & Admin Authentication

### 1.1 Initial Access

```
Admin navigates to Admin Panel URL
          │
          ▼
Auth Gate Screen
  [Email Field]
  [Password Field]
  [Login Button]
          │
    ┌─────┴─────┐
    │           │
  Valid      Invalid
    │           │
    ▼           ▼
Dashboard   Error Toast
(Overview)  "Invalid credentials. Try again."
```

### 1.2 First-Time Admin Setup

| Step | Action | System Response |
|------|--------|----------------|
| 1 | Admin opens panel first time | Redirect → Setup Wizard |
| 2 | Sets new admin password | Password strength indicator shown |
| 3 | Enables 2FA option | QR code modal displayed |
| 4 | Confirms 2FA code | Success badge — "2FA Enabled" |
| 5 | Panel loads | Routes to Dashboard Overview |

### 1.3 Session Management

- **Auto-logout:** After 30 minutes of inactivity, session is revoked
- **Multi-tab:** Only one active admin session allowed at a time
- **Logout Action:** Sidebar avatar → "Sign Out" → Confirmation toast → Redirect to Login

---

## FLOW 2 — Dashboard (Overview) Screen

### 2.1 Screen Entry

```
Admin clicks "Overview" in Sidebar / Bottom Nav
          │
          ▼
Dashboard renders with:
  • 5 Metric Cards (Revenue, MRR, ARPU, Churn, LTV)
  • Revenue Growth vs Projections SVG Chart
  • Revenue by Plan donut chart
  • Recent Subscriptions table (last 4 entries)
  • Footer branding
```

### 2.2 Period Switcher Flow

```
Admin clicks [Monthly] or [Quarterly] toggle
          │
          ▼
System updates:
  • Total Revenue card: Monthly → $1,284,500 / Quarterly → $3,853,500
  • MRR card:           Monthly → $142,320   / Quarterly → $426,960
  • SVG Revenue chart path animates smoothly to new data curve
  • Active period button gets highlighted bg + box-shadow
```

| Trigger | State Before | State After |
|---------|-------------|-------------|
| Click "Monthly" | Quarterly active | Monthly highlighted, metrics update |
| Click "Quarterly" | Monthly active | Quarterly highlighted, metrics multiply ×3 |

### 2.3 Export Report Flow

```
Admin clicks [Export Report] button
          │
          ▼
Toast 1: "📊 Compiling report data..."      (1.2s)
          │
          ▼
Toast 2: "📁 Generating PDF export..."      (1.8s)
          │
          ▼
Toast 3: "✅ Report ready. Downloading..."  (final)
          │
          ▼
[Simulated file download triggers]
```

### 2.4 View All Users Redirect

```
Admin clicks "View All Users" link in Recent Subscriptions card header
          │
          ▼
Tab switches to → User Management Screen
Sidebar nav "Users" becomes active (highlighted with border-r accent)
```

### 2.5 Subscription Row Hover

```
Admin hovers over a subscription table row
          │
          ▼
Row bg: transparent → bg-white/5
Chevron icon fades in (opacity 0 → 100, transition 200ms)
Click chevron → Profile modal opens (see Flow 3.4.1)
```

---

## FLOW 3 — User Management Screen

### 3.1 Screen Entry

```
Admin clicks "Users" in sidebar or bottom nav
          │
          ▼
User Management renders:
  • 3 Stats Cards: Total Users | Active Now | Active Business Profiles
  • Search Bar (real-time filter)
  • Control Row: [Filter] [+ Add User] [⬇ Download]
  • High-Density Table (Web) / Accordion Cards (Mobile)
  • Pagination controls
  • Data Precision Alert insight card (bottom)
```

### 3.2 Real-Time Search Flow

```
Admin types in search bar
          │
          ▼
Table/card list filters in real-time by name or email
          │
     ┌────┴────┐
     │         │
  Found     Not Found
     │         │
   Rows       Empty state shown:
  Filtered    🔎 "No users match your search."
```

### 3.3 Add New User Flow (Primary CTA)

```
Admin clicks [+ Add User] button
          │
          ▼
Overlay Modal opens (backdrop-blur + scale animation):

  ┌─────────────────────────────────────┐
  │   ➕ Add New User            [✕]    │
  │  ─────────────────────────────────  │
  │  Full Name:    [___________________]│
  │  Email:        [___________________]│
  │  Profile Type: [Basic ▼]           │
  │  Status:       [Active ▼]          │
  │                                     │
  │         [Cancel]   [Create User]    │
  └─────────────────────────────────────┘
          │
    ┌─────┴──────────┐
    │                │
  Valid Input    Invalid Input
    │                │
    ▼                ▼
  User added      Field border → red
  to table top    Error msg shown inline
  Toast: "✅ User 'Name' created successfully"
  Stats card "Total Users" increments +1
```

**Form Validation Rules:**

| Field | Rule | Error Message |
|-------|------|---------------|
| Full Name | Min 2 characters | "Name must be at least 2 characters" |
| Email | Must contain `@` and `.` | "Enter a valid email address" |
| Profile Type | Pre-selected (Basic) | — |
| Status | Pre-selected (Active) | — |

### 3.4 User Row Actions Menu (Web)

```
Admin clicks [⋯] on any user table row
          │
          ▼
Contextual dropdown menu renders below button:
  ┌─────────────────────────────┐
  │  👤  View Profile            │
  │  🔒  Reset Password          │
  │  ⛔  Suspend  (or) ✅ Activate│
  │  🗑️   Delete User            │
  └─────────────────────────────┘
```

#### 3.4.1 — View Profile

```
Click "View Profile"
          │
          ▼
Profile Details Modal opens (scale-in animation):

  ┌────────────────────────────────────────────┐
  │  [Avatar]   Elena Alistair                  │
  │             elena@obsidian.io               │
  │             ● Active  │  Business Plan       │
  │  ─────────────────────────────────────────  │
  │  System ID:      USR-00124                  │
  │  Member Since:   Oct 8, 2023                │
  │  Last Active:    2 mins ago                 │
  │  Total Sessions: 47                         │
  │  Device:         macOS / Chrome 118         │
  │  Region:         IN — Mumbai                │
  │  ─────────────────────────────────────────  │
  │  [Reset Password]    [Suspend]    [Close]   │
  └────────────────────────────────────────────┘
```

#### 3.4.2 — Suspend User

```
Click "Suspend" on an Active user
          │
          ▼
Inline confirmation banner in menu:
  "Suspend Elena Alistair? They will lose access immediately."
  [Cancel]    [Confirm Suspend]
          │
    ┌─────┴──────┐
    │            │
  Cancel      Confirm
    │            │
    ▼            ▼
Menu closes    Row status badge → "Suspended" (red)
               Row border → error/20 tint
               Avatar → grayscale filter
               Toast: "⚠️ Elena Alistair has been suspended"
```

#### 3.4.3 — Reactivate User

```
Click "Activate" on a Suspended user
          │
          ▼
No confirmation required (safe action)
          │
          ▼
Row status badge → "Active" (teal)
Row border → default
Avatar → full colour restored
Toast: "✅ Elena Alistair account reactivated"
```

#### 3.4.4 — Reset Password

```
Click "Reset Password"
          │
          ▼
Toast 1: "🔐 Generating secure one-time link..."   (0.8s)
          │
          ▼
Toast 2: "📨 Reset link sent to elena@obsidian.io" (final)
```

#### 3.4.5 — Delete User (Destructive)

```
Click "Delete User"
          │
          ▼
Danger Confirmation Modal:

  ┌──────────────────────────────────────────┐
  │  ⚠️  Permanently Delete User?            │
  │                                          │
  │  This action cannot be undone.           │
  │  Type the user's full name to confirm:   │
  │                                          │
  │  [_____________________________]         │
  │                                          │
  │    [Cancel]    [Delete Permanently]      │
  └──────────────────────────────────────────┘
          │
    ┌─────┴──────────────┐
    │                    │
  Name matches        Cancel
  exactly              │
    │                    ▼
    ▼               Modal closes
  User removed from table
  Toast: "🗑️ User deleted permanently"
  Total Users stats card decrements -1
```

### 3.5 Mobile Card Accordion Flow

```
Admin taps a user card (Mobile Mode)
          │
          ▼
Card height animates open
Chevron icon rotates 180° (transition: transform 300ms)
Expanded section shows 3 action buttons:
  [Suspend/Reactivate]  [Reset Password]  [View Profile]
          │
Tap same card again       → Card collapses
Tap a different card      → Previous collapses, new opens
Tap an action button      → Corresponding sub-flow triggers
```

### 3.6 Filter Users Sheet Flow

```
Admin clicks [Filter] button
          │
          ▼
Filter Sheet slides in from right (translateX animation):

  ┌───────────────────────────────────────┐
  │  Filter Users               [✕ Close] │
  │  ───────────────────────────────────  │
  │  STATUS                               │
  │  [● All]  [Active]  [Suspended]       │
  │                                       │
  │  PROFILE TYPE                         │
  │  [● All]  [Basic]  [Business]         │
  │                                       │
  │  LAST ACTIVE                          │
  │  [● Any]  [24h]  [7 days]  [30 days]  │
  │                                       │
  │    [Reset Filters]      [Apply]       │
  └───────────────────────────────────────┘
          │
  Click Apply → Sheet closes, table filters update
  Badge on Filter button shows count: "Filter (2)"
```

### 3.7 Export Users CSV Flow

```
Admin clicks [⬇] Download icon button
          │
          ▼
Toast: "📊 Preparing CSV export..."
          │
          ▼
Toast: "✅ users_export_2026-07-14.csv ready"
[Download] simulates file save
```

---

## FLOW 4 — System Oversight (Analytics) Screen

### 4.1 Screen Entry

```
Admin clicks "Analytics" in sidebar or bottom nav
          │
          ▼
System Oversight renders:
  • System Health banner (Operational / Degraded / Outage)
  • Infrastructure Node Map visual
  • Incident Log (live-feed, filterable)
  • Security Events summary cards
  • Performance Metrics charts
  • [Deploy Update] danger CTA
```

### 4.2 Incident Log Filter Flow

```
Admin clicks severity filter tab: [All] [Critical] [Warning] [Info]
          │
          ▼
Incident list filters to matching severity
Active tab highlighted

Severity colour coding:
  Critical → bg-error/10  + text-error
  Warning  → bg-tertiary/10 + text-tertiary
  Info     → bg-primary/10  + text-primary
```

### 4.3 Chart Data Refresh Flow

```
Admin clicks [🔄 Refresh] button on chart
          │
          ▼
Refresh icon spins (300ms CSS rotation)
Toast: "🔄 Syncing cluster telemetry..."
          │
          ▼
Chart values animate/update (simulated)
Toast: "✅ Data synced — Jul 14, 2026 06:38 IST"
```

### 4.4 Deploy System Update Flow

```
Admin clicks [🚀 Deploy Update] button
          │
          ▼
Danger Confirmation Modal:

  ┌──────────────────────────────────────────┐
  │  🚀  Deploy System Update?               │
  │                                          │
  │  This will restart active services.      │
  │  Expected downtime: ~2 minutes.          │
  │  1,248 active sessions will be paused.   │
  │                                          │
  │       [Cancel]     [Deploy Now]          │
  └──────────────────────────────────────────┘
          │
    ┌─────┴──────┐
    │            │
  Cancel      Deploy Now
    │            │
    ▼            ▼
Closes      Deployment Progress Modal:
            "🔄 Initialising deployment... 0%"
            Progress bar animates: 0% → 100%
            "✅ Deployment complete. v2.4.3 is now live."
```

### 4.5 Node Detail Drill-Down

```
Admin clicks on a server node in the infrastructure map
          │
          ▼
Node Detail Panel slides in:
  Node ID: node-07-us-east
  Region: US East (N. Virginia)
  CPU: 42%  |  Memory: 68%  |  Uptime: 99.97%
  Last incident: 3 days ago
  ─────────────────────────────
  [Restart Node]    [View Full Logs]
```

---

## FLOW 5 — Settings Screen

### 5.1 Screen Entry

```
Admin clicks "Settings" in sidebar or bottom nav
          │
          ▼
Settings screen renders in 4 sections:
  A. Security Preferences    (toggles: 2FA, Auto-Suspend, Session)
  B. Notification Preferences (toggles: Email, SMS, Incidents)
  C. System Configuration    (dropdowns: Plan, Retention, Timezone)
  D. API Key Management      (key list, generate, copy, delete)
```

### 5.2 Toggle / Switch Interactions

```
Admin flips any toggle switch
          │
          ▼
Switch animates (slide + colour change, 200ms)
Toast fires immediately with context-specific message
```

**Complete Toggle Toast Reference:**

| Toggle | ON Message | OFF Message |
|--------|-----------|-------------|
| Enforce 2FA | "🔐 2FA enforced for all users" | "2FA enforcement disabled" |
| Auto-Suspend (30d) | "🕐 Auto-suspend enabled at 30-day threshold" | "Auto-suspend disabled" |
| Email Alerts | "📧 Email notifications enabled" | "Email notifications paused" |
| SMS Alerts | "📱 SMS alerts enabled" | "SMS alerts paused" |
| Maintenance Mode | "⚠️ Maintenance Mode ENABLED — users see maintenance page" | "✅ Platform is live again" |
| Incident Notifications | "🔔 Critical incident alerts active" | "Incident notifications muted" |

### 5.3 API Key Management (Slide-Out Sheet)

```
Admin clicks [Manage API Keys] button
          │
          ▼
Side Sheet slides in from right edge (translateX -100% → 0):

  ┌─────────────────────────────────────────────┐
  │  🔑 API Key Management           [✕ Close]  │
  │  ─────────────────────────────────────────  │
  │                                             │
  │  ● Live Production Key                      │
  │    obs_live_8f3d9c2a       [📋 Copy] [🗑️]  │
  │    Created: Oct 10, 2023                    │
  │                                             │
  │  ● Development Staging                      │
  │    obs_test_4a7e1b8c       [📋 Copy] [🗑️]  │
  │    Created: Oct 14, 2023                    │
  │                                             │
  │  ─────────────────────────────────────────  │
  │  [+ Generate New API Key]                   │
  └─────────────────────────────────────────────┘
```

#### 5.3.1 — Generate New Key

```
Admin clicks [+ Generate New API Key]
          │
          ▼
Inline name prompt:
  "Key Label: [______________]  [Create]"
Admin types label → clicks Create
          │
          ▼
New key created: "obs_live_[random8chars]"
Key added to list with copy/delete controls
Toast: "✅ New API key 'My Key' generated"
```

#### 5.3.2 — Copy Key to Clipboard

```
Admin clicks [📋 Copy]
          │
          ▼
Key value copied to OS clipboard
Copy icon → ✓ checkmark (1.5s) → reverts to copy icon
Toast: "📋 API key copied to clipboard"
```

#### 5.3.3 — Delete API Key

```
Admin clicks [🗑️] delete icon
          │
          ▼
Inline confirmation:
  "Delete 'Live Production Key'? This is irreversible."
  [Cancel]    [Delete Key]
          │
    ┌─────┴─────┐
    │           │
  Cancel     Delete
    │           │
    ▼           ▼
Closes      Key removed from list instantly
            Toast: "🗑️ API key deleted"
```

### 5.4 Save / Discard Settings Flow

```
Admin changes any dropdown or text setting
          │
          ▼
Amber "Unsaved Changes" chip appears in settings header

Admin clicks [Save System Changes]
          │
          ▼
Button shows loading spinner (500ms)
All changes persisted to state
Toast: "✅ System configuration saved successfully"
"Unsaved Changes" chip disappears

Admin clicks [Discard]
          │
          ▼
All fields revert to last-saved values
Toast: "↩️ Changes discarded"
```

---

## FLOW 6 — Admin Profile & Global Actions

### 6.1 Admin Profile Menu

```
Admin clicks avatar (sidebar bottom-left or top-right header)
          │
          ▼
Profile Dropdown Menu opens:

  ┌────────────────────────────────────┐
  │  [Avatar]  Admin User              │
  │            admin@obsidian.io       │
  │  ────────────────────────────────  │
  │  👤  Edit Profile                  │
  │  🔑  Change Password               │
  │  🛡️  My Security Settings          │
  │  ────────────────────────────────  │
  │  🚪  Sign Out                      │
  └────────────────────────────────────┘
```

### 6.2 Notifications Bell

```
Admin clicks 🔔 bell icon in top header
          │
          ▼
Notification panel slides down:

  ─── Today ─────────────────────────────────
  🔴  Marcus Thorne — 3 failed login attempts  (2m ago)
  🟡  Nordic region Business signups +15%      (14m ago)
  🟢  MRR Q4 target exceeded                  (1h ago)
  ─── Yesterday ─────────────────────────────
  🔴  Cluster Node-07 offline event            (23h ago)

  Admin clicks notification → Routes to relevant screen
  Admin clicks "Mark All Read" → All items → 50% opacity
```

### 6.3 Web ↔ Mobile View Mode

```
Admin clicks [Web View] or [Mobile View] toggle in top toolbar
          │
    ┌─────┴──────────┐
    │                │
Web Mode         Mobile Mode
    │                │
    ▼                ▼
Desktop sidebar   Phone frame
shown             shown
Web viewport      Mobile simulator
renders           renders
Both modes show the same active tab content
```

---

## ⚠️ Edge Cases & Error States

| Scenario | System Behaviour |
|----------|-----------------|
| Add User with existing email | Toast: "⚠️ A user with this email already exists" |
| Empty name in Add User form | Red border + "Name must be at least 2 characters" |
| Network failure during Export | Toast: "❌ Export failed. Check your connection and try again." |
| Attempt to delete last API key | Warning: "⚠️ You must keep at least one active API key." |
| Deploy with Maintenance OFF | Prompt: "Enable maintenance mode before deploying?" |
| Session expired mid-flow | Redirect to Login + Toast: "🔐 Session expired. Please log in again." |
| Filter with 0 results | Empty state: 🔎 "No users match the selected filters. [Reset Filters]" |
| Concurrent admin sessions | Warning banner: "Another admin session is active on a different device." |

---

## 🎨 UI State & Visual Reference

| State | Visual Cue |
|-------|-----------|
| Active user | Teal `status-active` badge + live pulse dot |
| Suspended user | Red `status-suspended` badge + grayscale avatar + red border tint |
| Business profile | Teal `business_center` icon + teal BUSINESS badge |
| Basic profile | Grey `person` icon + muted badge |
| Unsaved settings | Amber "Unsaved Changes" chip in section header |
| Critical incident | Red row bg + `text-error` severity label |
| Loading action | Spinner on CTA button + pointer-events disabled |
| Success feedback | Green ✅ toast, slides in from top-right, auto-dismiss 3s |
| Danger confirmation | Red-bordered modal + typed-confirmation guard |
| Empty search | Empty state illustration + friendly message |

---

## 📱 Mobile-Specific Flows

### M1 — FAB Quick Add (Mobile Only)

```
Admin taps the [+] Floating Action Button (bottom-right, above nav bar)
          │
          ▼
FAB scales: 1 → 1.25 → 1 (spring animation, 150ms)
Add User modal opens (full-screen bottom sheet on mobile)
Same validation rules as Web form apply
```

### M2 — Swipe-to-Action on Cards (Mobile)

```
Admin swipes left on a user card
          │
          ▼
Red "Suspend" reveal action appears from right
Admin releases swipe → Suspend confirmation triggers
```

### M3 — Pull-to-Refresh (Mobile)

```
Admin pulls down past the top of any screen
          │
          ▼
Refresh spinner appears
Data reloads (simulated 800ms)
Screen scrolls back to top
Toast: "✅ Data refreshed"
```

### M4 — Bottom Navigation Reference

| Icon | Label | Destination |
|------|-------|------------|
| `home` | Home | Dashboard Overview |
| `history` | History | User Management |
| `bar_chart` | Analytics | System Oversight |
| `settings` | Settings | Settings Screen |

---

## 🗂️ Screen × Action Matrix

| Action | Dashboard | Users | Analytics | Settings |
|--------|-----------|-------|-----------|----------|
| Period Toggle | ✅ | — | — | — |
| Export / Download | ✅ | ✅ | — | — |
| Add User | — | ✅ | — | — |
| Suspend / Activate | — | ✅ | — | — |
| Reset Password | — | ✅ | — | — |
| Delete User | — | ✅ | — | — |
| View Profile | ✅ (hover) | ✅ | — | — |
| Filter | — | ✅ | ✅ | — |
| Refresh Data | — | — | ✅ | — |
| Deploy Update | — | — | ✅ | — |
| API Key Manage | — | — | — | ✅ |
| Toggle Settings | — | — | — | ✅ |
| Save / Discard | — | — | — | ✅ |

---

*Document maintained by: Obsidian Analytics — Engineering & Design*  
*Last updated: 2026-07-14*
