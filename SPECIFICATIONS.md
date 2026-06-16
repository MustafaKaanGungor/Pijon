# Pijon — App Usage Awareness Companion

> **Version:** 1.0  
> **Platform:** Android (Java)  
> **Last Updated:** 2026-06-14

---

## 1. Overview

**Pijon** is an Android application designed to promote mindful phone usage. It monitors user-selected apps and periodically reminds the user how long they have been actively using them. After a configurable interval, Pijon displays a pop-up overlay asking the user to reflect on whether their experience has been positive — encouraging intentional screen time without forcibly blocking any app.

---

## 2. Core Concept

| Aspect | Description |
|---|---|
| **Goal** | Help users become aware of their screen time on specific apps |
| **Approach** | Non-intrusive, periodic pop-up reminders with self-reflection prompts |
| **Philosophy** | Pijon does **not** block or restrict usage — it only raises awareness |

---

## 3. Features

### 3.1 App List & Selection

- On first launch (and accessible anytime from the main screen), Pijon displays a **list of all installed applications** on the device.
- Each app in the list has a **toggle switch** to enable or disable Pijon monitoring for that app.
- The list should support:
  - **Search / filter** by app name.
  - **Sorting** (alphabetical, most recently used, etc.).
  - Display of the app's **icon** and **name** for easy identification.
- User selections are **persisted** locally so they survive app restarts and device reboots.

### 3.2 Usage Timer

- When the user opens a **monitored app**, Pijon starts an internal **countdown timer**.
- The default timer duration is **5 minutes**.
- The timer **resets and restarts** automatically after every pop-up interaction.
- The timer should:
  - Run reliably in the **background** (using a foreground service).
  - **Pause** if the monitored app is no longer in the foreground (user switches away or locks the phone).
  - **Resume** when the user returns to the monitored app.

### 3.3 Pop-Up Reminder (Overlay)

When the timer reaches zero, Pijon displays a **system overlay pop-up** on top of the currently active app. The pop-up contains:

| Element | Details |
|---|---|
| **Title** | "Pijon Reminder" (or similar) |
| **Message** | "You have been using **[App Name]** for **[X] minutes**." |
| **Reflection Prompt** | "Was this time well spent?" |
| **Positive Button** | 👍 "Yes, it was good" |
| **Negative Button** | 👎 "Not really" |

**Behavior after interaction:**
1. User taps either button.
2. Pijon **logs** the response (app name, timestamp, duration, user answer).
3. The pop-up **closes**.
4. The timer **resets** and **restarts** for another interval.
5. This cycle repeats for as long as the monitored app remains in the foreground.

### 3.4 Settings

The settings screen allows users to configure:

| Setting | Default | Description |
|---|---|---|
| **Timer Duration** | 5 minutes | Adjustable reminder interval (e.g., 1 – 60 minutes) |
| **Notification Sound** | On | Play a short sound when the pop-up appears |
| **Vibration** | On | Vibrate the device when the pop-up appears |
| **Pop-Up Style** | Default | Choose from different pop-up visual themes |
| **Enable/Disable Pijon** | Enabled | Global on/off switch to pause all monitoring |

### 3.5 Usage Statistics (Future / Optional)

- A dashboard showing:
  - Total time spent on each monitored app (daily / weekly / monthly).
  - Breakdown of positive vs. negative self-reflection responses.
  - Trends and graphs over time.

---

## 4. Technical Requirements

### 4.1 Permissions

| Permission | Reason |
|---|---|
| `PACKAGE_USAGE_STATS` | Detect which app is currently in the foreground |
| `SYSTEM_ALERT_WINDOW` | Display overlay pop-ups on top of other apps |
| `FOREGROUND_SERVICE` | Run the monitoring timer as a persistent foreground service |
| `RECEIVE_BOOT_COMPLETED` | Restart the monitoring service after device reboot |
| `VIBRATE` | Haptic feedback on pop-up |

### 4.2 Key Android Components

| Component | Purpose |
|---|---|
| **`UsageStatsManager`** | Query foreground app information |
| **Foreground Service** | Persistent background timer that monitors the active app |
| **`WindowManager` + Overlay** | Render the pop-up on top of any active app |
| **`SharedPreferences` / Room DB** | Store user settings, selected apps, and usage logs |
| **`BroadcastReceiver`** | Listen for `BOOT_COMPLETED` to auto-start the service |

### 4.3 Architecture

```
┌─────────────────────────────────────────────────┐
│                   Pijon App                     │
├──────────┬──────────┬───────────┬───────────────┤
│  Main    │ App List │ Settings  │  Stats        │
│  Screen  │ Screen   │ Screen    │  Screen       │
├──────────┴──────────┴───────────┴───────────────┤
│              Foreground Service                 │
│  ┌─────────────┐  ┌──────────────────────────┐  │
│  │ Timer Logic │  │ Foreground App Detector  │  │
│  └──────┬──────┘  └────────────┬─────────────┘  │
│         │                      │                │
│         ▼                      ▼                │
│  ┌──────────────────────────────────────────┐   │
│  │          Overlay Pop-Up Manager          │   │
│  └──────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│              Local Data Layer                   │
│  ┌──────────────┐  ┌────────────────────────┐   │
│  │ Preferences  │  │  Usage Log Database    │   │
│  └──────────────┘  └────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

---

## 5. User Flow

```
┌──────────────┐
│  User opens  │
│    Pijon     │
└──────┬───────┘
       ▼
┌──────────────────┐     ┌─────────────────────┐
│ Grant Permissions│────▶│  Select Apps to      │
│ (first launch)   │     │  Monitor             │
└──────────────────┘     └─────────┬───────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ Pijon Service Starts │
                         │ (runs in background) │
                         └─────────┬───────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ User opens a         │
                         │ monitored app        │
                         └─────────┬───────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ Timer starts         │
                         │ (default: 5 min)     │
                         └─────────┬───────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ Timer reaches 0      │
                         └─────────┬───────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ Pop-up overlay shown │
                         │ "Was it worth it?"   │
                         └─────────┬───────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ User responds        │
                         │ 👍 or 👎             │
                         └─────────┬───────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ Log response,        │
                         │ reset timer, repeat  │
                         └─────────────────────┘
```

---

## 6. Pop-Up Design Mockup

```
╔══════════════════════════════════════╗
║           🐦 Pijon Reminder          ║
╠══════════════════════════════════════╣
║                                      ║
║  You have been using Instagram       ║
║  for 5 minutes.                      ║
║                                      ║
║  Was this time well spent?           ║
║                                      ║
║  ┌────────────┐  ┌────────────────┐  ║
║  │ 👍 Yes     │  │ 👎 Not really │  ║
║  └────────────┘  └────────────────┘  ║
║                                      ║
╚══════════════════════════════════════╝
```

---

## 7. Screens Summary

| # | Screen | Description |
|---|---|---|
| 1 | **Main / Home** | Overview of Pijon status, quick toggle, and navigation |
| 2 | **App Selection** | List of installed apps with enable/disable toggles |
| 3 | **Settings** | Timer duration, sound, vibration, and global on/off |
| 4 | **Statistics** *(v2)* | Usage history, reflection breakdown, trends |
| 5 | **Pop-Up Overlay** | The reminder that appears over monitored apps |

---

## 8. Development Phases

### Phase 1 — MVP
- [ ] Project setup & architecture
- [ ] Permission request flow (Usage Stats, Overlay, Foreground Service)
- [ ] App list screen with toggle selection
- [ ] Foreground service with foreground-app detection
- [ ] Timer logic (start, pause, resume, reset)
- [ ] Overlay pop-up with reflection prompt
- [ ] Basic settings (timer duration, global on/off)
- [ ] Persist selected apps & settings

### Phase 2 — Polish
- [ ] Improved pop-up design and animations
- [ ] Sound & vibration settings
- [ ] Boot receiver for auto-start
- [ ] Edge case handling (split screen, PiP, etc.)

### Phase 3 — Insights
- [ ] Usage logging to local database
- [ ] Statistics dashboard with charts
- [ ] Daily/weekly summaries
- [ ] Export data option

---

## 9. Constraints & Considerations

- **Battery Optimization:** The foreground service polls for the current foreground app. Polling interval should be optimized (e.g., every 1–2 seconds) to balance accuracy and battery drain.
- **Android Version Support:** `UsageStatsManager` is available from API 21+ (Lollipop). Target **API 26+** (Oreo) minimum for reliable foreground service behavior.
- **Manufacturer Restrictions:** Some OEMs (Xiaomi, Huawei, Samsung) aggressively kill background services. Pijon may need manufacturer-specific guidance for users to whitelist the app.
- **Privacy:** Pijon does **not** collect any data about what the user does inside apps — only which app is in the foreground and for how long.

---

## 10. Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java |
| **IDE** | Android Studio |
| **Min SDK** | API 26 (Android 8.0 Oreo) |
| **Target SDK** | Latest stable |
| **Local Storage** | SharedPreferences + Room (SQLite) |
| **UI** | XML Layouts + Material Design Components |
| **Background** | Foreground Service + Handler/Timer |
| **Overlay** | WindowManager + TYPE_APPLICATION_OVERLAY |
