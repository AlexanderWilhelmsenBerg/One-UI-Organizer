# Acceptance Criteria

These criteria define the acceptance bar for the first useful release of One UI Organizer. They are intentionally stricter than a demo but narrower than a full launcher replacement.

Implementation ownership and sequencing are defined in `PARALLEL_DEVELOPMENT.md`; these product criteria remain the final acceptance contract regardless of which agent implements a behavior.

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

At minimum the internal model carries app-owned equivalents of:

- package name;
- launch component/target identity;
- display label;
- Android-declared application category when available.

Android framework types must not become the cross-layer domain model.

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

Stale state may be retained for reinstall recovery or cleaned deterministically, but the chosen behavior must be tested and documented.

### C4 — Versioned local state

The first persisted organizer state uses an explicit schema version beginning at version 1.

Any later persisted-state schema change requires migration tests proving existing category overrides, favourites and hidden state survive unless an explicitly documented migration decision says otherwise.

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

For a cold start with no cached snapshot, the UI shows a meaningful loading state and completes a normal scan without ANR behavior. On the primary Samsung test device with up to roughly 500 launch targets, first usable categorized results should appear within 2 seconds.

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

Text and touch targets remain usable with common Android font/display scaling settings.

### E7 — Replaceable host presentation

The shelf composables are not coupled to Samsung-only blur/window APIs. The same content can be hosted in the selected translucent/sheet presentation or the documented fallback without rewriting domain/data code.

## F. Privacy and permissions

### F1 — No Internet permission

The v0.1 manifest does not request `android.permission.INTERNET`.

### F2 — No broad package-query permission

The v0.1 manifest does not request `android.permission.QUERY_ALL_PACKAGES`.

### F3 — No telemetry/account

v0.1 contains no analytics SDK, advertising SDK, telemetry backend, account system, or cloud synchronization.

### F4 — Minimal local persistence

Persist only organizer state required for product behavior. Installed-app labels and icons should be read from Android rather than stored as long-lived authoritative copies unless a later measured performance need justifies caching.

## G. Architecture and maintainability

### G1 — Scanner boundary

PackageManager/Android discovery code is hidden behind a boundary that can be replaced by a fake in unit tests.

### G2 — Pure category engine

Automatic category selection is testable as pure Kotlin logic without requiring Android instrumentation.

### G3 — Repository is source of UI state

The UI does not independently merge package scans and persistence. One repository/use-case boundary produces the organizer model consumed by the ViewModel/UI.

### G4 — No premature infrastructure

v0.1 does not add Room, Hilt/Koin, WorkManager, a background service, or a networking stack unless an accepted requirement is added that materially needs it.

### G5 — App-owned contracts

Cross-layer contracts use app-owned models. Android framework, DataStore/serialization and Compose-specific types remain inside their appropriate adapters/layers.

### G6 — Upgrade-friendly dependencies

Every direct dependency has a documented purpose, is declared explicitly rather than relied on transitively, uses the stable-compatible baseline, and is isolated so replacing/upgrading it does not require unrelated feature rewrites.

## H. Quality gates

Before v0.1 is considered testable:

- project builds from a clean checkout;
- exact toolchain/library versions comply with `STABLE_BASELINE.md`;
- unit tests pass;
- Compose/instrumented tests covering critical flows pass in the supported test lane;
- Android Lint passes with no greenfield lint baseline;
- ktlint passes;
- dependency health passes;
- Gradle/compiler/deprecation output has no unexplained warnings;
- dependency verification passes;
- normal verification tasks remain configuration-cache compatible;
- debug APK installs and launches on the target Samsung device;
- no crash occurs during scan, search, category override, hide/unhide, favourite, or launching sampled apps.

Before v0.1 is considered releasable:

- all Must items in `MOSCOW.md` are complete;
- all acceptance criteria above pass or an explicit documented exception is approved;
- physical Samsung device acceptance is complete;
- package visibility is rechecked against the expected launchable-app set;
- release notes and privacy behavior are documented;
- performance-sensitive journeys have been measured after integration rather than optimized from intuition.

## I. Milestone-1 platform spike acceptance

The project must **not** treat the platform lane as complete until the Android integration spike demonstrates all of the following on the target Samsung device:

1. the launcher-intent package query returns a practically complete expected app set without `QUERY_ALL_PACKAGES`;
2. the chosen launch-target model reliably opens sampled apps;
3. duplicate/alias behavior is understood and deterministic;
4. the proposed companion sheet/translucent activity is visually/functionally acceptable, or a documented fallback presentation is selected.

If the agent environment cannot access the physical device, the PR may be code-review-ready but this acceptance remains explicitly pending for owner testing.

## J. Parallel-development acceptance

Parallel coding must not reduce maintainability merely to finish sooner.

### J1 — Foundation gate

Agents 10/20/30/40 start from `main` only after Agent 00 has merged a clean scaffold with shared app-owned contracts, reproducible toolchains and CI gates.

### J2 — Lane ownership

Wave-1 PRs remain inside the ownership defined by `PARALLEL_DEVELOPMENT.md`. A feature agent does not create a duplicate scanner, category engine, state model, repository or UI model merely to avoid coordinating a shared contract.

### J3 — Shared contract changes are explicit

If a Wave-1 PR must change a frozen shared contract, the PR explains why, makes the smallest compatible change, and identifies sibling branches that must rebase before merge.

### J4 — Integration is a separate gate

Agent 50 integrates the merged lanes and runs cross-layer regression/acceptance tests. Four individually green Wave-1 PRs do not by themselves make v0.1 testable.

### J5 — Agents do not merge themselves

Coding agents leave their PRs unmerged for owner/reviewer control unless the owner explicitly instructs otherwise.
