# Acceptance Criteria

These criteria define the acceptance bar for the first useful release of One UI Organizer. They are intentionally stricter than a demo but narrower than a full launcher replacement.

## A. Platform integration

### A1 — Discover launchable apps

Given an ordinary Samsung device user profile,
when Organizer scans installed applications,
then it lists the apps that expose normal launcher activities and are visible through the supported launcher-intent query.

Acceptance notes:

- no `QUERY_ALL_PACKAGES` permission;
- no Samsung private API or launcher database access;
- Organizer itself is excluded from normal results;
- duplicate launcher activities are handled deterministically rather than silently producing unstable duplicates.

### A2 — App identity

Each listed launch target has enough identity to distinguish and reopen the correct launcher activity.

At minimum the internal model carries:

- package name;
- launch component or equivalent stable launch target;
- display label;
- Android-declared application category when available.

### A3 — Launch selected app

When the user taps a visible app entry, the corresponding launchable activity opens successfully.

Physical-device acceptance must include a representative sample of:

- Samsung system app;
- Google app;
- third-party app;
- game;
- work/productivity app;
- at least one package using a launcher alias or other non-trivial launch entry if present on the test device.

### A4 — Rescan without background service

When an app is installed or removed and Organizer is subsequently reopened/resumed, the next scan reflects the change without requiring an always-running service.

## B. Categorization

### B1 — Deterministic precedence

For every app, primary-category selection follows this exact precedence:

1. user override;
2. bundled known-app rule;
3. mapped Android application category;
4. `Unsorted` fallback.

Unit tests must prove each precedence boundary.

### B2 — Unknown apps remain usable

An app that cannot be classified is visible in `Unsorted` and remains searchable and launchable.

Classification failure must never hide an otherwise launchable app.

### B3 — Manual correction wins

When a user moves an app to another category, that choice persists locally and continues to override automatic categorization on future scans.

### B4 — Rule changes do not erase user choice

If a bundled known-app rule later changes, an existing user override remains authoritative.

## C. User organization state

### C1 — Favourite state

The user can mark and unmark an app as a favourite.

Favourite state persists across process death and rescans.

### C2 — Hidden state

The user can hide an app from ordinary category sections and later restore it from a management surface.

Hidden apps must not be deleted from the underlying organizer state merely because they are hidden from the normal shelf.

### C3 — Safe uninstall handling

If an app with persisted organizer state is uninstalled, Organizer does not crash or surface a dead launcher entry in the normal shelf.

Stale state may be retained temporarily for reinstall recovery or cleaned deterministically, but the chosen behaviour must be tested and documented.

## D. Search

### D1 — App-name search

Search filters visible apps by display label without requiring exact case.

### D2 — Category search

Searching for a category name can surface the apps assigned to that category.

### D3 — Search remains local

Search performs no network request and sends no query text off-device.

## E. User interface

### E1 — Companion presentation

Launching Organizer must not replace the system launcher or request default-launcher status.

The preferred presentation is a One UI-inspired translucent/sheet activity. If blur is unavailable or unreliable, a dimmed/translucent or normal edge-to-edge fallback is acceptable.

### E2 — Immediate useful state

Organizer must not block the main thread while scanning packages.

For a cold start with no cached snapshot, the UI shows a meaningful loading state and completes a normal scan without ANR behaviour. On the primary Samsung test device with up to roughly 500 launch targets, first usable categorized results should appear within 2 seconds.

This is a product acceptance threshold, not a benchmark guarantee across all hardware.

### E3 — Category browsing

The user can browse categories and launch an app without entering a separate configuration screen.

### E4 — Long-press organization action

Long-pressing an app provides organization actions for at least:

- move/change category;
- favourite/unfavourite;
- hide.

### E5 — Theme support

The app remains readable and usable in both light and dark system themes.

### E6 — Accessibility

Interactive controls expose meaningful accessibility semantics/content descriptions where the icon or gesture alone would otherwise be ambiguous.

Text and touch targets must remain usable with common Android font/display scaling settings.

## F. Privacy and permissions

### F1 — No Internet permission

The v0.1 manifest does not request `android.permission.INTERNET`.

### F2 — No broad package-query permission

The v0.1 manifest does not request `android.permission.QUERY_ALL_PACKAGES`.

### F3 — No telemetry/account

v0.1 contains no analytics SDK, advertising SDK, telemetry backend, account system, or cloud synchronization.

### F4 — Minimal local persistence

Persist only organizer state required for product behaviour. Installed-app labels and icons should be read from Android rather than stored as long-lived authoritative copies unless a later measured performance need justifies caching.

## G. Architecture and maintainability

### G1 — Scanner boundary

PackageManager/Android discovery code is hidden behind a boundary that can be replaced by a fake in unit tests.

### G2 — Pure category engine

Automatic category selection is testable as pure Kotlin logic without requiring Android instrumentation.

### G3 — Repository is source of UI state

The UI does not independently merge package scans and persistence. One repository/use-case boundary produces the organizer model consumed by the ViewModel/UI.

### G4 — No premature infrastructure

v0.1 does not add Room, Hilt/Koin, WorkManager, a background service, or a networking stack unless an accepted requirement is added that materially needs it.

## H. Quality gates

Before v0.1 is considered testable:

- project builds from a clean checkout;
- unit tests pass;
- Compose/instrumented tests covering critical UI flows pass in the supported test lane;
- Android lint passes or every suppression is documented;
- debug APK installs and launches on the target Samsung device;
- no crash occurs during scan, search, category override, hide/unhide, favourite, or launching sampled apps.

Before v0.1 is considered releasable:

- all Must items in `MOSCOW.md` are complete;
- all acceptance criteria above pass or an explicit documented exception is approved;
- physical Samsung device acceptance is complete;
- package visibility is rechecked against the user's expected launchable-app set;
- release notes and privacy behaviour are documented.

## I. Milestone-1 spike acceptance

The project must **not** proceed into full UI implementation until the first Android integration spike demonstrates all of the following on the target Samsung device:

1. the launcher-intent package query returns a practically complete expected app set without `QUERY_ALL_PACKAGES`;
2. the chosen launch-target model reliably opens sampled apps;
3. duplicate/alias behaviour is understood;
4. the proposed companion sheet/translucent activity is visually and functionally acceptable, or a documented fallback presentation is selected.
