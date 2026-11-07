# Purgu Feature Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Purgu's user-facing cleanup, tools, vault, permissions, entry points and settings to cleansuperai while retaining its current features and visual style.

**Architecture:** MainActivity stays the navigation host. Feature-specific scanners and repositories supply data to Fragments. A permission coordinator resumes the selected feature after settings return. Destructive operations report only confirmed results.

**Tech Stack:** Kotlin, Android Views/Fragments, MediaStore, StorageStatsManager, UsageStatsManager, AppWidgetProvider, AndroidX Biometric.

**Spec:** `docs/superpowers/specs/2026-11-04-purgu-feature-parity-design.md`

## Global Constraints

- Keep minSdk 24 and targetSdk 36.
- Keep four current tabs, all existing functions, layout structure and color style.
- Put new home shortcuts below the existing shortcut area.
- Exclude keepalive, local push, resident notification, background scheduling and operational analytics.
- Preserve Purgu's vault move behavior; report when source deletion fails.

## Review Focus

- Permission denial and returning from settings must reopen only the intended feature.
- Android system deletion refusal must leave the candidate visible and uncounted.
- Cancelling a long scan must not publish a partial result as complete.
- Vault copy or index failure must leave the original available and not create a false successful entry.
- App uninstall cancellation must keep that app in the list.

---

### Task 1: Permissions and feature routing

**Files:** Modify `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `ui/common/MediaPermissionHelper.kt`; create `ui/common/FeaturePermissionCoordinator.kt` and tests.

- [ ] Write failing tests for feature-to-permission mapping and pending-route consumption.
- [ ] Run `./gradlew :app:testDebugUnitTest` and confirm expected failures.
- [ ] Implement all-files and usage-access checks, settings intents, pending route recovery, and per-feature gating.
- [ ] Run unit tests and `:app:assembleDebug`.

### Task 2: Shared scan, delete and report flow

**Files:** Create focused `ui/flow` state, scan and result Fragments, layouts and tests; modify `MainActivity.kt` routes.

- [ ] Write failing state tests for progress, cancellation and confirmed-delete accounting.
- [ ] Implement flow UI and operation coordinator.
- [ ] Run unit tests and build.

### Task 3: Cache cleanup

**Files:** Create `data/scanner/FileCleanupScanner.kt`, `ui/cache` page/adapter, layouts and tests.

- [ ] Write failing candidate classification and selection tests.
- [ ] Implement app cache, obsolete APK and residual scans, usage-access lock, safe deletion and report.
- [ ] Run tests and build.

### Task 4: Large files

**Files:** Extend `FileCleanupScanner.kt`; create `ui/largefiles` page/adapter and tests.

- [ ] Write failing tests for seven type filters, five size thresholds and six age filters.
- [ ] Implement scan, filter, detail/open, select all and delete flow.
- [ ] Run tests and build.

### Task 5: Media cleaners

**Files:** Extend `data/scanner/AndroidMediaScanner.kt`; create `ui/media` photo/video/audio page and tests; adjust existing similar, screenshot and swipe pages.

- [ ] Write failing tests for group selection, best-photo retention, audio classification and swipe trash recovery.
- [ ] Implement and compare candidate sets with Purgu fixtures.
- [ ] Run tests and build.

### Task 6: App manager and device tools

**Files:** Extend `ui/tools/AppManagerFragment.kt`; create `ui/tools/NetworkTrafficFragment.kt`, `BatteryInfoFragment.kt`, `DeviceInfoFragment.kt` and tests.

- [ ] Write failing tests for app sorting and network range calculations.
- [ ] Implement multi-select uninstall, traffic totals and device/battery fields.
- [ ] Run tests and build.

### Task 7: Vault

**Files:** Create `vault` repository/auth/session, `ui/vault` screens/layouts and tests; add biometric dependency.

- [ ] Write failing tests for PIN verification, move failure, restore naming collision and deletion accounting.
- [ ] Implement setup, unlock, import, gallery, preview, restore and delete.
- [ ] Run tests and build.

### Task 8: Entrypoints, settings and widget

**Files:** Modify `fragment_home.xml`, `HomeFragment.kt`, `fragment_tools.xml`, `ToolsFragment.kt`, `ProfileFragment.kt`; create terms page, widget provider/layout/metadata and tests.

- [ ] Write failing route mapping tests and verify all destination tags resolve.
- [ ] Add home cards below current shortcuts, tool and settings rows, permission tips, exit prompt and widget pin/click route.
- [ ] Run tests, build, and device smoke test all old and new destinations.

### Task 9: End-to-end parity audit

**Files:** Update `docs/superpowers/specs/2026-11-04-purgu-feature-parity-design.md` with an acceptance matrix.

- [ ] Exercise each flow on the same emulator/device and test dataset as Purgu.
- [ ] Compare visible candidates, filters, selections, deletion results and return behavior.
- [ ] Fix discrepancies with regression tests, run full unit suite and debug build.
