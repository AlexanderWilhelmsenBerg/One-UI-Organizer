# Product and Delivery Plan

**Current status:** v0.1 and the post-v0.1 classification implementation are merged to `main`. Agent 70 is the integration/acceptance gate on `integration/classification-quality`. The first fresh Samsung pass ran successfully and exposed one genuine game-classification false positive, which Agent 70 corrected. A second fresh report from the corrected build remains the classification acceptance check.

The detailed result is recorded in [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md). Execution ownership and sequencing live in [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md).

## 1. Product definition

**One UI Organizer** is a small native Android companion for Samsung One UI. It presents launchable apps in a useful categorized shelf while leaving One UI Home as the system launcher.

It is not a launcher replacement and does not write Samsung launcher databases, use Samsung private launcher APIs, or take ownership of widgets, gestures, wallpaper, recents, folders or the home screen.

## 2. Product constraints

1. One UI Home remains the default launcher.
2. No Samsung private launcher/database manipulation.
3. `QUERY_ALL_PACKAGES` remains absent.
4. `INTERNET` remains absent for the current product.
5. No account, telemetry, analytics, ads or cloud classification.
6. Manual user classification always overrides automatic classification.
7. Scanning occurs on open/resume; no unnecessary persistent service.
8. Android/framework/library types stay behind app-owned boundaries.
9. Toolchains and dependencies follow [`STABLE_BASELINE.md`](STABLE_BASELINE.md).
10. Persisted-state changes require explicit migration tests.
11. Performance work is measurement-driven.

## 3. Current user experience

The user opens Organizer from One UI Home, gets a One UI-inspired categorized/searchable shelf, launches the selected exact activity, and dismisses/back-navigates to the existing Samsung home experience.

Current organization behavior includes:

- category browsing;
- app-name and category-name search;
- tap to launch;
- favourite/unfavourite;
- hide and restore;
- manual category override;
- direct triage affordance for automatic `Unsorted` fallback;
- classification explanation based on the real `ClassificationSource`;
- explicit local classification-report sharing for diagnostic review.

## 4. Automatic-classification taxonomy

The current one-primary-category taxonomy is:

- Communication
- Social
- Work
- Productivity
- Smart Home
- Homelab
- Finance
- Shopping
- Travel & Navigation
- Music & Audio
- Video
- Photos
- Reading
- Web Shortcuts
- Development
- Tools
- Action & Adventure
- RPG
- Strategy & Simulation
- Puzzle & Casual
- Board & Card
- Games
- Other
- Unsorted

`Games` and `Unsorted` are deliberate safe fallbacks. Zero `Unsorted` is not a product goal.

Automatic classification precedence is fixed:

1. user override;
2. bundled known-app rule;
3. Android-declared category mapping;
4. `Unsorted`.

Inside bundled rules, selector precedence is exact component > exact package > package prefix. Prefix matching is narrowly reserved for evidence-backed generated namespaces; the shipped Web shortcut rule uses `org.chromium.webapk.` only.

## 5. Architecture

The app remains a small native Kotlin + Jetpack Compose + Material 3 application with app-owned boundaries:

```text
Android PackageManager
        |
        v
InstalledAppSource
        |
        v
CategoryEngine <--- bundled deterministic rule packs
        |
        +---- OrganizerStateStore
        |            |
        v            v
       OrganizerRepository
               |
               v
        ViewModel / UI state
               |
               v
        Jetpack Compose shelf
```

Android framework objects, DataStore implementation types and Compose types remain at their respective boundaries.

## 6. Persistence

Organizer state remains explicit schema version 1 and stores only user-owned organization state:

- category overrides;
- favourites;
- hidden apps.

The classification taxonomy expansion is additive only. No existing `AppCategory` value was renamed or removed, so no schema migration is required. Agent 70 includes a regression test that reads a literal pre-classification-wave schema-v1 payload and proves override/favourite/hidden state remains readable.

Future custom-category/category-order work will be persisted-state work and must define durable category identity plus migration tests before UI implementation spreads.

## 7. Delivery history

### v0.1 — complete

The Agent 00–50 sequence delivered the warning-free Android scaffold/CI, supported launcher discovery and exact launch targeting, deterministic categorization/search, DataStore-backed organizer state, Compose shelf/design system and owner Samsung proof of the v0.1 shelf.

### Classification foundation — complete

Agent 60 / PR #9 added private same-device evidence reporting, additive game/Web taxonomy, deterministic selector/index infrastructure and separate general/game/Web rule-pack ownership.

Foundation evidence contained 566 launcher targets, including 225 `Unsorted` and 129 broad `Games` entries.

### Classification rule/UI lanes — merged

- PR #12: general exact-package rule expansion;
- PR #13: five-bucket game classification with `Games` fallback;
- PR #10: narrow Chromium WebAPK classification;
- PR #11: `Unsorted` triage and classification explanation UI.

### Agent 70 — integration / acceptance

Agent 70 verifies composition, precedence and persisted-state compatibility, tightens the permanent quality lane so `androidTest` sources compile, updates authoritative documentation and leaves the PR unmerged for owner control.

The first fresh Samsung report matched the initial integrated projection at aggregate level but exposed one row-level false positive: an `Eden Optimized` Yuzu-family emulator variant reused `com.miHoYo.Yuanshen` and was incorrectly forced into RPG by the exact-package game rule.

Agent 70 removed that ambiguous rule and added a regression. Final expected aggregates on the same 566-target population are now:

- `Unsorted`: 225 -> 124;
- broad `Games`: 129 -> 24;
- Action & Adventure: 12;
- RPG: 37;
- Strategy & Simulation: 29;
- Puzzle & Casual: 23;
- Board & Card: 4;
- Web Shortcuts: 1;
- bundled-rule source: 5 -> 234;
- Android-declared source: 336 -> 208.

A second fresh report from the corrected build remains required before those final figures are called device-confirmed.

## 8. Testing and quality policy

The permanent repository lane covers:

- debug assembly;
- instrumentation-test APK compilation;
- JVM tests;
- Android Lint;
- ktlint;
- dependency `buildHealth`;
- warning-mode failure;
- strict dependency verification;
- configuration-cache creation/reuse;
- forbidden-permission checks.

Instrumentation execution and One UI-specific behavior are verified on an emulator/physical device where needed; compiling an instrumentation APK is not a substitute for executing it.

Real Samsung acceptance remains mandatory for product-critical package discovery, launching, presentation, persisted upgrade behavior and classification sampling.

## 9. Remaining classification limitations

The classifier is deliberately conservative:

- 124 entries remain `Unsorted`;
- 24 games remain broad `Games` after the false-positive correction;
- standard TWA wrappers are not generically classified;
- no safe generic Samsung Internet/other-browser shortcut signature is established;
- exact package identities require maintenance and can be ambiguous for modified/repacked software;
- the model remains one primary category per app.

These are acceptable limitations, not invitations to introduce cloud or label-guessing classification.

## 10. Next development wave — user-owned category management

The next coherent product wave should be **custom categories + category reorder + richer category management**.

Recommended sequence:

1. freeze a stable app-owned custom-category identity/order model and persistence migration contract;
2. implement repository/domain behavior with migration tests;
3. implement create/rename/delete/reorder and richer management UI;
4. integrate and run Samsung acceptance.

Local backup/export/import should follow once the custom-category representation is stable. Dynamic/pinned shortcuts should follow stable category identity for the same reason.

Presentation polish can proceed later or in a low-conflict UI lane. Performance tooling remains deferred unless measured Samsung behavior demonstrates a real regression.

## 11. Explicit non-goals

Do not expand the current product into:

- Samsung launcher database/folder manipulation;
- default HOME replacement;
- `QUERY_ALL_PACKAGES` for convenience;
- cloud/AI classification;
- accounts, analytics or advertising;
- permanent monitoring service;
- speculative networking/infrastructure;
- cross-platform implementation.

## 12. Definition of done for the classification-quality wave

The wave is accepted only when:

- Agent 70 repository CI is green on the final corrected head;
- corrected signed debug APK is installed/run on the owner Samsung device;
- taxonomy/search/move/explanation/triage behavior remains functional;
- app launching, dismiss/back and rescan still work;
- a fresh corrected same-device classification report confirms the final sanitized counts;
- false-positive findings and corrections are recorded without committing raw inventory;
- `INTERNET` and `QUERY_ALL_PACKAGES` remain absent;
- repository migration tests remain green;
- strict physical override/favourite/hidden preservation is separately checked if required because the report format cannot prove it;
- the owner explicitly decides whether to merge the Agent 70 PR.
