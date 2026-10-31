# Forget About It

A personal Android reminder app for things that are easy to forget on busy days
(litter boxes, medicine, bills). Reminders keep nudging until they are marked done.
Single user, no accounts, no server: everything lives on the phone.

Target device: Pixel 9a (Android 15+). `minSdk` is 33 on purpose, so there are no
legacy code paths. Sideloaded, never on the Play Store.

## Stack

Kotlin, Jetpack Compose (Material 3 used sparingly), Room (KSP), Navigation Compose,
`AlarmManager` exact alarms. No DI framework, no network. Package: `app.forgetaboutit`.

## Building

There is no Android SDK in the cloud session this was written in, so **the compile
check is GitHub Actions**: `.github/workflows/build.yml` builds a signed release APK on
every push to `main` and publishes it to the `latest` release. A separate `test` job runs
the JVM unit tests and never blocks the APK. If the signing secrets are missing, the
workflow still does a debug compile check and just skips publishing.

- Install: open the repo's Releases page on the phone, download `ForgetAboutIt.apk`,
  install over the previous one (data is kept).
- Local: `./gradlew assembleDebug` (needs JDK 17 + Android SDK 35). Tests: `./gradlew testDebugUnitTest`.
- Signing: the release key is **not** in the repo. It comes from GitHub Actions secrets
  `ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; the build
  reads it via env vars (see `app/build.gradle.kts`). A stable key is what lets each build
  update the installed app in place; losing it means uninstall + reinstall (use the in-app
  backup first). `versionCode` comes from `GITHUB_RUN_NUMBER`.

## How reminders work (the part to be careful with)

- `domain/Planner.kt` is pure Kotlin and unit-tested (`app/src/test`). It answers
  "what is on Today" (`itemsFor`), "when should this next alert" (`nextAlert`) and
  "when must the phone wake up next" (`nextTrigger`).
- `notify/Scheduler.kt` keeps **one** exact alarm set: the earliest moment anything needs
  attention. `AlarmReceiver` -> `Scheduler.processDue` posts whatever is due, updates
  `Occurrence.lastNotifiedAt/nudgeCount`, then re-arms. `BootReceiver` re-arms after
  reboot / app update / clock or time zone change. `MainActivity` re-arms on every open.
- Data model: `Reminder` (the rule) and `Occurrence` (one reminder on one day at one
  slot, created lazily when something happens to it). `slot` is minutes since midnight,
  or `ANYTIME` (-1) for "anytime today" reminders, which start nudging at `nudgeFromMinutes`.
- Nudge styles: GENTLE = once, PERSISTENT = every 10 min, RELENTLESS = every 5 min plus a
  full-screen alert. Nudging stops at midnight. A one-time reminder stays on Today (overdue)
  until done or skipped.
- Notification buttons (Done / Snooze 10) go through `ActionReceiver` -> `Actions`, the same
  code the screens use.
- `Scheduler.onSaved` seeds today's already-passed slots on create/edit so saving a reminder
  never causes an instant burst of alerts.

## Design

Design D ("Focus"), mid-dark palette. Source of truth is `ui/Theme.kt` (`P`):
background `#17191C`, cards `#212428`, chips `#2C3036`, lines `#31353B`, bottom bar
`#1C1F22`, text `#F2F2EE`, secondary `#A0A6AC`, accent `#9DB4FF` (text on accent `#0F1420`).
Font: Manrope (variable, bundled in `res/font`). Cards 20dp radius, buttons 14-18dp,
minimum 44dp touch targets. Icons are stroke paths on a 24x24 grid in `ui/Glyphs.kt`
(same paths as the mockups), not an icon library.

Screens: Today (grid + day bar), Alert (full-screen "it's time"), New/Edit reminder,
Reminder detail (streak, 5-week grid, recent), All reminders, Settings (permission checks,
test notification, JSON backup/restore).

## Known limits / ideas

- Not yet run on a real phone when this was written: first thing to verify is that a
  reminder fires on the minute with the screen off, and that Done/Snooze buttons work.
- Doze can delay nudges shorter than ~9 minutes when the phone is idle; the Battery row in
  Settings (set to Unrestricted) helps.
- Possible next steps: quiet hours, per-reminder snooze length, home-screen widget,
  scheduled automatic backups.
