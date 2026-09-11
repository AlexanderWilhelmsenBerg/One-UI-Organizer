# Acceptance Criteria

These criteria define the acceptance bar for One UI Organizer. They are intentionally stricter than a demo but narrower than a full launcher replacement.

Implementation ownership and sequencing are defined in `PARALLEL_DEVELOPMENT.md`; these product criteria remain the final acceptance contract regardless of which agent implements a behavior.

## A. Platform integration

### A1 — Discover launchable apps

Given an ordinary Samsung device user profile, when Organizer scans installed applications, then it lists the apps that expose normal launcher activities and are visible through the supported launcher-intent query.

Acceptance notes:

- no `QUERY_ALL_PACKAGES` permission;
- no Samsung private API or launcher database access;
- Organizer itself is excluded from normal results;
- duplicate launcher activities are handled deterministically rather than silently producing unstable duplicates.

### A2 — App identity

Each listed launch target has enough app-owned identity to distinguish and reopen the correct launcher activity, including package name, launch component/target identity, display label and Android-declared category when available. Android framework types must not become the cross-layer domain model.

### A3 — Launch selected app

When the user taps a visible app entry, the corresponding launchable activity opens successfully.

Physical-device acceptance must include a representative sample of Samsung, Google, third-party, game and work/productivity apps plus at least one non-trivial alias/multiple-target package if present.

### A4 — Rescan without background service

When an app is installed or removed and Organizer is subsequently reopened/resumed, the next scan reflects the change without requiring an always-running service.

## B. Categorization

### B1 — Deterministic precedence

For every app, primary-category selection follows this exact precedence:

1. user override;
2. bundled known-app rule;
3. mapped Android application category;
4. `Unsorted` fallback.

Unit tests must prove each precedence boundary. A user override may resolve to either a built-in or user-created category and retains `ClassificationSource.USER_OVERRIDE` in both cases.

### B2 — Unknown apps remain usable

An app that cannot be classified is visible in `Unsorted` and remains searchable and launchable. Classification failure must never hide an otherwise launchable app.

### B3 — Manual correction wins

When a user moves an app to another category, that choice persists locally and continues to override automatic categorization on future scans.

### B4 — Rule changes do not erase user choice

If a bundled known-app rule later changes, an existing user override remains authoritative. Automatic rule packs continue to classify only into built-in `AppCategory` values and do not depend on user-created categories.

## C. User organization state

### C1 — Favourite state

The user can mark and unmark an app as a favourite. Favourite state persists across process death and rescans.

### C2 — Hidden state

The user can hide an app from ordinary category sections and later restore it from a management surface. Hidden apps must not be deleted from underlying organizer state merely because they are hidden from the normal shelf.

### C3 — Safe uninstall handling

If an app with persisted organizer state is uninstalled, Organizer does not crash or surface a dead launcher entry in the normal shelf. Stale state may be retained for reinstall recovery or cleaned deterministically, but the chosen behavior must be tested and documented.

### C4 — Versioned local state

The first persisted organizer state began at schema version 1. The current category-identity foundation evolves organizer state to schema version 2.

Every persisted-state schema change requires a real migration/read path and literal prior-schema fixtures. Existing category overrides, favourites and hidden state must survive unless an explicitly documented migration decision says otherwise. Supported old state must not be treated as corruption merely because the schema version increased.

## D. Search

### D1 — App-name search

Search filters visible apps by display label without requiring exact case.

### D2 — Category search

Searching for a category display name can surface apps assigned to that category. This applies to built-in names and user-created category names without a second UI-only category-name model.

### D3 — Search remains local

Search performs no network request and sends no query text off-device.

## E. User interface

### E1 — Companion presentation

Launching Organizer must not replace the system launcher or request default-launcher status. The preferred presentation is a One UI-inspired translucent/sheet activity with a safe non-blur fallback.

### E2 — Immediate useful state

Organizer must not block the main thread while scanning packages. On the primary Samsung test device with up to roughly 500 launch targets, first usable categorized results should appear within 2 seconds. This is a product acceptance threshold, not a benchmark guarantee across all hardware.

### E3 — Category browsing

The user can browse categories and launch an app without entering a separate configuration screen.

### E4 — Long-press organization action

Long-pressing an app provides organization actions for at least move/change category, favourite/unfavourite and hide.

### E5 — Theme support

The app remains readable and usable in both light and dark system themes.

### E6 — Accessibility

Interactive controls expose meaningful accessibility semantics/content descriptions where an icon or gesture alone would otherwise be ambiguous. Text and touch targets remain usable with common Android font/display scaling settings.

### E7 — Replaceable host presentation

Shelf composables are not coupled to Samsung-only blur/window APIs. Presentation can change without rewriting domain/data code.

## F. Privacy and permissions

### F1 — No speculative Internet permission

The shipped baseline and category-identity foundation do not request `android.permission.INTERNET`. A future supported network feature may add it only with the documented provider implementation and privacy/cache behavior.

### F2 — No broad package-query permission

The manifest does not request `android.permission.QUERY_ALL_PACKAGES`.

### F3 — No telemetry/account

The app contains no analytics SDK, advertising SDK, telemetry backend, account system or cloud synchronization.

### F4 — Minimal local persistence

Persist only organizer state required for product behavior. Installed-app labels/icons remain Android-sourced rather than long-lived authoritative copies unless a later measured performance need justifies caching.

## G. Architecture and maintainability

### G1 — Scanner boundary

PackageManager/Android discovery code is hidden behind a boundary replaceable by a fake in unit tests.

### G2 — Pure category engine

Automatic category selection is testable as pure Kotlin logic without Android instrumentation.

### G3 — Repository is source of UI state

The UI does not independently merge package scans and persistence. One repository/use-case boundary produces the organizer model consumed by presentation state.

### G4 — No premature infrastructure

Do not add Room, Hilt/Koin, WorkManager, a background service or networking stack without an accepted requirement that materially needs it.

### G5 — App-owned contracts

Cross-layer contracts use app-owned models. Android framework, DataStore/serialization and Compose-specific types remain inside their appropriate adapters/layers.

### G6 — Upgrade-friendly dependencies

Every direct dependency has a documented purpose, is declared explicitly rather than relied on transitively, uses the stable-compatible baseline and is isolated enough to replace/upgrade without unrelated feature rewrites.

## H. Quality gates

The permanent lane must keep:

- clean debug assembly;
- instrumentation-test APK compilation;
- JVM tests;
- Android Lint;
- ktlint;
- dependency `buildHealth`;
- warning-mode failure;
- strict dependency verification;
- configuration-cache creation/reuse;
- forbidden-permission checks.

Physical Samsung acceptance remains required for package visibility, launching, presentation, persisted upgrade behavior and integrated product-critical flows.

## I. Platform spike acceptance — historical gate

The platform lane is accepted only because owner-device testing demonstrated practical launcher-intent visibility without `QUERY_ALL_PACKAGES`, correct launch-target behavior, deterministic alias/duplicate handling and an acceptable companion presentation/fallback.

## J. Parallel-development acceptance

### J1 — Shared foundation gates parallel work

Feature lanes start from the required merged foundation rather than reproducing its contracts.

### J2 — Lane ownership

Agents remain inside `PARALLEL_DEVELOPMENT.md` ownership. A feature agent does not create a duplicate scanner, category engine, state model, repository, category entity or UI model merely to avoid coordination.

### J3 — Shared contract changes are explicit

A required frozen-contract change must be the smallest compatible change and identify sibling branches that must rebase.

### J4 — Integration is a separate gate

Individually green parallel PRs do not by themselves prove integrated product behavior. The designated integration lane owns cross-layer regression/acceptance.

### J5 — Agents do not merge themselves

Coding agents leave their PRs unmerged for owner/reviewer control unless the owner explicitly instructs otherwise.

## K. Category identity / persistence foundation acceptance

Agent 80 is accepted only when all of the following hold.

### K1 — Stable built-in identity

Every built-in `AppCategory` has one explicit unique durable `CategoryId`. The persisted ID is independent of enum constant name and display label. Built-in categories remain the automatic-classification taxonomy.

### K2 — Stable custom identity

A user-created category is represented by an app-owned `CustomCategoryDefinition` with a `custom:<opaque-id>` identity and separate display name. A rename preserves the ID. The later creation workflow generates the opaque ID once; it must not derive identity from the display name.

### K3 — One effective category contract

`CategorizedApp` can represent either a built-in or custom effective category through the shared app-owned `CategoryDefinition` contract while preserving the existing one-primary-category model and original `ClassificationSource`.

### K4 — Schema-v1 migration

A literal existing schema-v1 payload containing built-in overrides, favourites and hidden apps loads as schema v2 without loss. V1 enum override values map deterministically to explicit built-in IDs. The next write uses schema v2.

### K5 — Schema-v2 contents

Schema v2 persists at minimum category override IDs, favourites, hidden app IDs, custom category definitions and category order. Custom-category and order data round-trip through storage.

### K6 — Deterministic order

The default after v1 migration is the existing built-in order. `Favourites` is outside persisted category order. Effective order preserves first valid occurrences, removes duplicate/stale IDs, appends missing built-ins deterministically and then appends missing custom definitions deterministically. Unknown/stale order IDs do not crash the app.

### K7 — Search/report compatibility

Built-in category-name search remains correct. The shared representation supports searching custom display names. The local classification report remains useful for existing built-in output and can represent a custom effective category's stable identity/display name without losing `ClassificationSource`.

### K8 — Rule compatibility

Existing known-app, game, Web/PWA and emulator rule behavior remains unchanged. Precedence remains user override > bundled rule > Android category > `Unsorted`.

### K9 — Scope boundary

Agent 80 does not implement complete create/rename/delete/reorder behavior or a category-management screen. Those belong to Agents 81/82, with Agent 83 integration/acceptance after both merge.
