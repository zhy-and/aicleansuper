# Purgu Push and Keepalive Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align local push, FRC, analytics, keepalive, notification UI, shortcuts, and notification permission with Purgu.

**Architecture:** Keep cleansuperai navigation and app identity. Port Purgu policy and scheduling modules with adapters for routes and backend requests. Preserve existing changes; do not add widget pin triggers.

**Tech Stack:** Android Kotlin/Java, Firebase Messaging/Analytics/Remote Config, MMKV, JobScheduler, AlarmManager, RemoteViews.

**Spec:** `docs/superpowers/specs/2026-11-05-purgu-push-keepalive-parity-design.md`

## Global Constraints

- Preserve existing uncommitted work.
- Notification permission: Splash first, Home second after 500 ms if still denied, then storage.
- No extra widget pin entry points.
- Keep this app's package, Firebase project, backend identity, and navigation.

## Review Focus

- Permission callback arrives after activity recreation: resume storage flow once.
- User denies twice: home tip goes to app settings.
- FRC malformed or unavailable: retain last valid snapshot or built-in default.
- A duplicate notification submission must not duplicate analytics or frequency counts.
- FCM data-only wake must not create an ordinary visible notification.

---

### Task 1: Notification permission order

**Files:** `SplashActivity.kt`, `MainActivity.kt`, `ui/home/HomeFragment.kt`, `notification/NotificationPermissionFlow.kt`, unit test.

- [x] Write and run policy tests for first/second request and denial threshold.
- [x] Implement launcher callbacks, persistent denial count, and home handoff.
- [ ] Add separate 10009/Firebase events and Purgu home tip behavior.
- [ ] Build and verify permission order on API 33+ device.

### Task 2: FRC and local push policy

**Files:** new `config`, `notification` modules; `app/build.gradle`.

- [ ] Port defaults, parser, snapshots, frequency and repeat policy with relevant Purgu tests.
- [ ] Wire Firebase Remote Config cold and warm refresh, including schedule restart.
- [ ] Build and test malformed/empty config fallback.

### Task 3: Scene triggers, notification rendering, routing

**Files:** new `notification` watchers, scheduler, resources; `MainActivity.kt` and feature flows.

- [ ] Port 39 scenes, all seven templates, channels, alarms and recovery.
- [ ] Map scene routes into current fragments, connect scan/clean callbacks.
- [ ] Verify each scene on device against Purgu.

### Task 4: Keepalive, FCM, analytics

**Files:** new `keepalive` and `analytics` modules, `FireMessageService.java`, app class, Manifest.

- [ ] Port foreground service, jobs/alarms/boot/home-key/fcm and topic retry.
- [ ] Adapt 10008/10009/10018/10019 requests and Firebase events.
- [ ] Verify delivery, recovery, notification, and event payloads on device.

### Task 5: Static shortcuts and final parity audit

**Files:** `res/xml/shortcuts.xml`, Manifest, route adapters, parity report.

- [x] Add four shortcuts and migrate the uninstall guide; debug build passes.
- [ ] Verify each shortcut destination on device.
- [ ] Compare runtime behavior across Android versions and record remaining feature differences.
