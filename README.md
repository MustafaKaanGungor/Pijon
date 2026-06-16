# 🐦 Pijon

**Mindful phone usage, one reminder at a time.**

Pijon is an Android app that nudges you to reflect on your screen time — without blocking or restricting anything. You decide which apps to monitor, and Pijon will periodically ask how you're feeling about your usage.

---

## What It Does

After a set interval (default: 5 minutes), Pijon pops up over your monitored app and asks two quick questions:

- *How did this make you feel?* — Good / Meh / Bad  
- *Was this a good use of your time?* — Yes / Unsure / No

That's it. No judgement, no blocking. Just a moment to check in with yourself.

---

## Features

- **App selection** — Choose which apps to monitor from a searchable list
- **Configurable interval** — Set reminders anywhere from 10 seconds to 60 minutes
- **Overlay reminders** — Pop-up appears on top of any app without interrupting your flow
- **Insights dashboard** — See breakdowns per app: time recorded, feeling, and usage quality
- **CSV export** — Export your full reflection history
- **Clear history** — Wipe all data with one tap

---

## How It Works

### As a user

1. Open Pijon and grant the two required permissions (usage access + overlay)
2. Tap **Apps** and toggle on the apps you want to track
3. Hit **Start** on the home screen — Pijon runs quietly in the background
4. Use your phone normally. When the timer is up, a small overlay appears
5. Answer the two questions and carry on — the timer resets automatically
6. Check the **Insights** tab anytime to see patterns in your usage

### Under the hood

Pijon runs a **foreground service** that polls `UsageStatsManager` every second to detect which app is in the foreground. When a monitored app is detected:

- A countdown timer starts (or resumes if you return to the app)
- The timer pauses when you leave the monitored app
- When the countdown hits zero, an overlay is drawn via `WindowManager` on top of the active app
- Your responses are saved to a local **Room (SQLite) database**
- The timer resets and the cycle repeats

All data stays on your device. Pijon never sees what you do inside any app — only which app is open and for how long.

---

## Requirements

- Android 8.0 (API 26) or higher
- **Usage access permission** — to detect the foreground app
- **Display over other apps permission** — to show the overlay reminder

---

## Tech Stack

| | |
|---|---|
| Language | Java |
| Min SDK | API 26 (Android 8.0 Oreo) |
| UI | XML + Material Design 3 |
| Database | Room (SQLite) |
| Background | Foreground Service + Handler |
| Overlay | WindowManager (`TYPE_APPLICATION_OVERLAY`) |