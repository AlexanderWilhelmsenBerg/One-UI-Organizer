# Product and Delivery Plan

**Current status:** v0.1 and the classification-quality wave through Agent 70 / PR #14 are merged to `main`. The classification result is accepted and recorded in [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md). The next product wave is user-owned category management. Agent 80 is the shared identity/persistence foundation; Agents 81 and 82 must consume that contract rather than invent parallel category representations.

Execution ownership and sequencing live in [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md).

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

The current category-management foundation still requests neither `INTERNET` nor `QUERY_ALL_PACKAGES` and adds no dependency/toolchain change.

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

Custom-category lifecycle UI is not part of the shipped baseline yet.

## 4. Automatic-classification taxonomy

The built-in one-primary-category taxonomy remains:

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

Automatic classification precedence remains exactly:

1. user override;
2. bundled deterministic rule;
3. Android-declared category mapping;
4. `Unsorted`.

Automatic rule packs continue to produce the built-in `AppCategory` taxonomy only. User overrides may resolve to either a built-in or user-created category without changing `ClassificationSource.USER_OVERRIDE` semantics.

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

Category identity is app-owned. `AppCategory` remains the built-in automatic-classification taxonomy and implements the shared category-definition contract. User-created categories use app-owned `CustomCategoryDefinition` values. Both expose a durable `CategoryId` and a user-visible display name. No Compose, DataStore or Android type participates in that identity contract.

A future network metadata provider must follow the same pattern:

```text
provider/network API -> adapter -> app-owned metadata model -> category mapping
```

Provider/library types must not leak into domain/application contracts.

## 6. Persistence and category identity

Organizer state schema is **version 2**.

Schema v2 persists:

- category overrides as durable `CategoryId` values;
- favourites;
- hidden apps;
- user-created category definitions;
- category order.

### Built-in identity

Every built-in `AppCategory` has an explicit stable ID such as `builtin:work`. Persisted identity is not derived from the enum constant name or display label. A future enum rename or display-label change therefore does not silently change stored identity.

### Custom identity

User-created categories use `custom:<opaque-id>` identities. The opaque portion is generated once when the category is created by the later category-domain lane and remains unchanged across rename, process death and serialization. Display name is metadata, never the persisted key.

### Schema-v1 migration

Literal schema-v1 payloads remain supported. On read:

- each persisted v1 `AppCategory` enum value is mapped deterministically to that built-in category's explicit ID;
- favourites survive unchanged;
- hidden-app state survives unchanged;
- custom-category definitions start empty;
- category order starts at the existing built-in order;
- the in-memory state is schema v2.

The next write always uses schema v2. Supported v1 state is not treated as corruption.

### Category order

`Favourites` remains a virtual section and never participates in persisted category order.

Effective order normalization is deterministic:

1. preserve the first occurrence of each known persisted ID in persisted order;
2. drop stale/unknown order IDs;
3. drop later duplicates;
4. append missing built-in IDs in the frozen built-in default order;
5. append missing defined custom-category IDs in definition order.

This handles future built-ins, incomplete state and stale order entries without crashing. Reorder mutation/UI is owned by later lanes; Agent 80 freezes only the representation and semantics.

## 7. Delivery history

### v0.1 — complete

Agents 00–50 delivered the warning-free Android scaffold/CI, supported launcher discovery and exact launch targeting, deterministic categorization/search, DataStore-backed organizer state, Compose shelf/design system and owner Samsung proof.

### Classification foundation/rule/UI wave — complete and merged

Agent 60 / PR #9 added evidence reporting, additive game/Web taxonomy and deterministic selector infrastructure. PRs #10–#13 added narrow Web/PWA rules, `Unsorted` triage/explanation, general rules and game rules.

Agent 70 / PR #14 integrated and accepted the wave. Owner-device evidence identified and repaired the Eden/Yuzu false positive, added the evidence-backed `Emulators` category and confirmed the final 566-target classification result. PR #14 is merged.

The accepted final Samsung report includes 123 `Unsorted`, 15 `Emulators`, 10 broad `Games`, 37 RPG, 249 bundled-rule classifications and 194 Android-declared classifications, with total target count unchanged at 566.

## 8. Current development wave — user-owned category management

Recommended merge shape:

```text
80 Category identity / persistence foundation
                 |
        +--------+--------+
        |                 |
81 Domain/repository   82 Compose management UI
        |                 |
        +--------+--------+
                 |
        83 Integration / acceptance
```

### Agent 80 — current foundation

Owns:

- stable built-in/custom identity contract;
- shared category definition contract;
- schema-v2 migration from literal v1 payloads;
- category-order representation/normalization;
- cross-layer compatibility needed for custom categories to be representable;
- regression tests and authoritative documentation.

Does **not** own full create/rename/delete/reorder workflows or a management screen.

### Agent 81 — domain/repository lifecycle

Owns later behavior for:

- custom-category creation and ID generation;
- rename;
- deletion with explicit reassignment/fallback policy;
- reorder mutation operations;
- repository/domain tests.

### Agent 82 — Compose management UI

Owns later presentation for:

- create/rename/delete/reorder controls;
- dedicated category-management surface;
- accessible Compose behavior/tests;
- no persistence/classification duplication in UI.

### Agent 83 — integration/acceptance

Owns cross-layer wiring, migration/device acceptance and genuine integration repair after 81/82 merge.

Local backup/export/import follows stable custom-category lifecycle semantics. Dynamic/pinned shortcuts follow the same stable identity contract. Neither belongs in Agent 80.

## 9. Search and diagnostic reporting

Search uses the resolved category display name, so built-in names remain searchable and a future custom category is searchable without a second category-name table.

The local classification report preserves the existing built-in report labels/format. If an effective category is custom, the report includes its stable identity and display name while retaining the original classification source.

## 10. Metadata enrichment direction

Optional Internet-backed metadata enrichment remains approved in principle but not implemented. Any future provider must be supported/documented, local-first and best-effort, preserve existing precedence, cache locally with bounded policy, add `INTERNET` only with the supported implementation, and avoid unofficial brittle Play Store scraping.

F-Droid remains a plausible documented source for its subset of packages. Metadata enrichment is separate from category identity/lifecycle work.

## 11. Testing and quality policy

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

Persisted-state evolution additionally requires literal old-schema migration fixtures. Category identity/order tests must cover stable built-in mapping, rename-stable custom identity, serialization round-trip, deterministic duplicate/stale order behavior and preservation of existing user state.

Real Samsung acceptance remains mandatory for product-critical package discovery, launching, presentation, persisted upgrade behavior and integrated category-management behavior.

## 12. Remaining accepted limitations

- the classifier remains deliberately conservative;
- 123 entries in the accepted classification report remain `Unsorted`;
- 10 entries remain broad `Games`;
- standard TWA wrappers are not generically classified;
- no generic Samsung Internet/other-browser shortcut signature is established;
- optional external metadata enrichment is not implemented;
- the model remains one primary category per app;
- custom-category lifecycle UI/domain operations are not complete until Agents 81/82/83 land.

## 13. Explicit non-goals

Do not expand the current product into:

- Samsung launcher database/folder manipulation;
- default HOME replacement;
- `QUERY_ALL_PACKAGES` for convenience;
- cloud/AI classification services;
- accounts, analytics or advertising;
- permanent monitoring service;
- unofficial brittle Play Store scraping;
- cross-platform implementation.
