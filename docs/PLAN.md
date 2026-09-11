# Product and Delivery Plan

**Current status:** v0.1 and the post-v0.1 classification implementation are merged to `main`. Agent 70 is the integration/acceptance gate on `integration/classification-quality`. Owner-device review exposed one emulator/game false positive, justified a first-class `Emulators` category, and the final Samsung report now confirms the corrected classification result exactly. Repository/device classification acceptance is complete; merge remains an explicit owner decision.

The detailed result is recorded in [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md). Execution ownership and sequencing live in [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md).

## 1. Product definition

**One UI Organizer** is a small native Android companion for Samsung One UI. It presents launchable apps in a useful categorized shelf while leaving One UI Home as the system launcher.

It is not a launcher replacement and does not write Samsung launcher databases, use Samsung private launcher APIs, or take ownership of widgets, gestures, wallpaper, recents, folders or the home screen.

## 2. Product constraints

1. One UI Home remains the default launcher.
2. No Samsung private launcher/database manipulation.
3. `QUERY_ALL_PACKAGES` remains absent.
4. No account, telemetry, analytics or ads.
5. Manual user classification always overrides automatic classification.
6. Scanning occurs on open/resume; no unnecessary persistent service.
7. Android/framework/library types stay behind app-owned boundaries.
8. Toolchains and dependencies follow [`STABLE_BASELINE.md`](STABLE_BASELINE.md).
9. Persisted-state changes require explicit migration tests.
10. Performance work is measurement-driven.
11. Network access is permitted only for an explicit supported product feature with documented privacy/cache behavior; no speculative networking stack.

The current classification-quality PR still requests no `INTERNET` permission. The owner has approved future optional metadata enrichment, but the permission should be introduced only together with a supported provider implementation.

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
- Emulators
- Action & Adventure
- RPG
- Strategy & Simulation
- Puzzle & Casual
- Board & Card
- Games
- Other
- Unsorted

`Emulators` is separate from game genres. `Games` and `Unsorted` remain deliberate safe fallbacks. Zero `Unsorted` is not a product goal.

Automatic classification precedence remains:

1. user override;
2. bundled known-app rule;
3. Android-declared category mapping;
4. `Unsorted`.

Inside bundled rules, selector precedence is exact component > exact package > package prefix. Exact-component rules are used where package identity alone is ambiguous, as demonstrated by the Eden/Yuzu emulator case.

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

A future network metadata provider must follow the same pattern:

```text
provider/network API -> adapter -> app-owned metadata model -> category mapping
```

Provider/library types must not leak into domain/application contracts.

## 6. Persistence

Organizer state remains explicit schema version 1 and stores only user-owned organization state:

- category overrides;
- favourites;
- hidden apps.

The classification taxonomy expansion is additive only. No existing `AppCategory` value is renamed or removed, so no schema migration is required. Agent 70 includes a regression test that reads a literal pre-classification-wave schema-v1 payload and proves override/favourite/hidden state remains readable.

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

The first fresh Samsung report matched the initial integrated projection at aggregate level but exposed one row-level false positive: an `Eden Optimized` Yuzu-family emulator variant reused `com.miHoYo.Yuanshen` and was incorrectly forced into RPG by an exact-package game rule.

Agent 70 removed that ambiguous game rule. Owner review then approved an `Emulators` category and evidence-backed emulator pack. The Eden/Yuzu case now uses an exact launch-component selector instead of package-only matching.

The final fresh Samsung report from the emulator-category build confirms the predicted result exactly on the same 566-target population:

- `Unsorted`: 225 -> **123**;
- `Emulators`: 0 -> **15**;
- broad `Games`: 129 -> **10**;
- Action & Adventure: 12;
- RPG: **37**;
- Strategy & Simulation: 29;
- Puzzle & Casual: 23;
- Board & Card: 4;
- Web Shortcuts: 1;
- bundled-rule source: 5 -> **249**;
- Android-declared source: 336 -> **194**;
- total remains 566.

Manual review confirms all 15 emulator rows are actual emulator software/components. Eden/Yuzu and Citra are correctly classified as `Emulators`.

The 10 broad `Games` rows are MonsterFactory, Magic Timer, Xbox Game Pass, Moonlight, Artemis, Prado, Better xCloud, Winlator, GameHub and ES-DE. Several are intentionally broad gaming frontends, streaming or compatibility tools; the remaining game entries lack enough evidence for a permanent narrower bucket.

The classification-quality Samsung device gate is therefore complete. The report still cannot observe manual override/favourite/hidden persistence, so a strict physical proof of those state types remains separate if required literally; repository migration coverage is green.

## 8. Metadata enrichment direction

The owner permits future Internet-backed metadata enrichment for useful category/tag information.

Google Play categories/tags are useful conceptually, but the documented Google Play Developer APIs manage a developer's own applications rather than exposing a general arbitrary-package catalog API. Production classification must not depend on unofficial Play scraping.

F-Droid publishes documented indexes and per-package metadata, including categories, and is a viable future provider for packages represented there. A future enrichment slice should be local-first and best-effort:

- never block ordinary scanning on network availability;
- cache metadata locally with a bounded refresh policy;
- map provider categories/tags explicitly into app-owned categories;
- preserve user override and bundled-rule precedence;
- expose the classification source clearly;
- add `INTERNET` only when a supported provider is actually implemented;
- avoid adding a networking library unless platform APIs are insufficient and the dependency passes the normal introduction checklist.

Metadata enrichment is not part of the current Agent 70 acceptance gate.

## 9. Testing and quality policy

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
- current forbidden-permission checks.

When a future approved network feature introduces `INTERNET`, the permission gate must be changed explicitly in that same PR rather than silently disabled.

Real Samsung acceptance remains mandatory for product-critical package discovery, launching, presentation, persisted upgrade behavior and classification sampling.

## 10. Remaining classification limitations

The classifier remains deliberately conservative:

- 123 entries remain `Unsorted`;
- 10 entries remain broad `Games`;
- gaming frontends and streaming clients are not automatically treated as emulators;
- standard TWA wrappers are not generically classified;
- no safe generic Samsung Internet/other-browser shortcut signature is established;
- exact package identities require maintenance and can be ambiguous for modified/repacked software;
- optional external metadata enrichment is approved in scope but not implemented;
- the model remains one primary category per app.

These are accepted tradeoffs, not open blockers for the classification-quality wave.

## 11. Next development wave — user-owned category management

The next coherent product wave should be **custom categories + category reorder + richer category management**.

Recommended sequence:

1. freeze a stable app-owned custom-category identity/order model and persistence migration contract;
2. implement repository/domain behavior with migration tests;
3. implement create/rename/delete/reorder and richer management UI;
4. integrate and run Samsung acceptance.

A metadata-enrichment investigation can run as a separate low-conflict architecture lane, but should not destabilize category identity/schema work.

Local backup/export/import should follow once the custom-category representation is stable. Dynamic/pinned shortcuts should follow stable category identity for the same reason.

## 12. Explicit non-goals

Do not expand the current product into:

- Samsung launcher database/folder manipulation;
- default HOME replacement;
- `QUERY_ALL_PACKAGES` for convenience;
- cloud/AI classification services;
- accounts, analytics or advertising;
- permanent monitoring service;
- unofficial brittle Play Store scraping;
- cross-platform implementation.

## 13. Definition of done for the classification-quality wave

The classification-quality wave is complete when the repository-side acceptance update is green and the owner decides whether to merge PR #14.

Confirmed:

- Agent 70 repository CI is green on the emulator-category implementation;
- signed debug APK is installed/run on the owner Samsung device;
- the final same-device report confirms the sanitized category/source counts exactly;
- Eden/Yuzu and Citra are correctly classified as `Emulators`;
- representative emulator rows show no new false positive;
- broad gaming frontends/streaming/compatibility software remains conservative;
- false-positive findings and corrections are recorded without committing raw inventory;
- current `INTERNET` and `QUERY_ALL_PACKAGES` permissions remain absent;
- repository migration tests are green.

If strict physical persisted-state acceptance is required literally, manual override/favourite/hidden survival remains one separate narrow device check because the report format cannot prove it.

The final merge decision remains with the owner.
