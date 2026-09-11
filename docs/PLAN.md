# Product and Delivery Plan

**Current status:** v0.1 and the classification-quality wave through Agent 70 / PR #14 are merged to `main`. Agents 80–82 of the user-owned category-management wave are also merged as PRs #15–#17. Agent 83 integrates those foundations in PR #18. The implementation/automated gate belongs to PR #18; the wave remains **pending physical Samsung upgrade/migration acceptance** and must not be marked complete or merged until that pass is recorded. See [`CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md`](CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md).

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

The category-management wave requests neither `INTERNET` nor `QUERY_ALL_PACKAGES` and adds no dependency/toolchain change.

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
- explicit local classification-report sharing for diagnostic review;
- create/rename/delete custom categories;
- reorder built-in and custom categories;
- explicit delete-and-reassign or delete-and-return-to-automatic behavior.

The custom-category lifecycle is integrated in Agent 83 / PR #18 but remains pending the mandatory physical Samsung migration acceptance before it is treated as shipped/merge-ready.

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

Automatic rule packs produce built-in `AppCategory` results only. User overrides may resolve to either a built-in or user-created category without changing `ClassificationSource.USER_OVERRIDE` semantics.

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
       DefaultOrganizerRepository
        |                    |
        v                    v
OrganizerRepository   CategoryManagementRepository
        \                    /
         \                  /
          v                v
          ViewModel / UI state
                  |
                  v
          Jetpack Compose shelf
```

`DefaultOrganizerRepository` is one state owner exposed through separate app-owned read/organizer and category-lifecycle contracts. Agent 83 does not create a second category repository or duplicate lifecycle validation in the ViewModel.

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

User-created categories use `custom:<opaque-id>` identities. The opaque portion is generated once when the category is created and remains unchanged across rename, process death, serialization and reorder. Display name is metadata, never the persisted key.

### Schema-v1 migration

Literal schema-v1 payloads remain supported. On read:

- each persisted v1 `AppCategory` enum value maps deterministically to that built-in category's explicit ID;
- favourites survive unchanged;
- hidden-app state survives unchanged;
- custom-category definitions start empty;
- category order starts at the existing built-in order;
- the in-memory state is schema v2.

The next write always uses schema v2. Supported v1 state is not treated as corruption.

### Category order

`Favourites` remains a virtual section and never participates in persisted category order.

Effective persisted-order normalization is deterministic:

1. preserve the first occurrence of each known persisted ID in persisted order;
2. drop stale/unknown order IDs;
3. drop later duplicates;
4. append missing built-in IDs in the frozen built-in default order;
5. append missing defined custom-category IDs in definition order.

Lifecycle reorder requests are intentionally stricter: the repository accepts only an exact permutation of every current normal built-in/custom category ID and then persists that requested order.

## 7. Delivery history

### v0.1 — complete

Agents 00–50 delivered the warning-free Android scaffold/CI, supported launcher discovery and exact launch targeting, deterministic categorization/search, DataStore-backed organizer state, Compose shelf/design system and owner Samsung proof.

### Classification foundation/rule/UI wave — complete and merged

Agent 60 / PR #9 added evidence reporting, additive game/Web taxonomy and deterministic selector infrastructure. PRs #10–#13 added narrow Web/PWA rules, `Unsorted` triage/explanation, general rules and game rules.

Agent 70 / PR #14 integrated and accepted the wave. Owner-device evidence identified and repaired the Eden/Yuzu false positive, added the evidence-backed `Emulators` category and confirmed the final 566-target classification result. PR #14 is merged.

The accepted final Samsung report includes 123 `Unsorted`, 15 `Emulators`, 10 broad `Games`, 37 RPG, 249 bundled-rule classifications and 194 Android-declared classifications, with total target count unchanged at 566.

## 8. Current development wave — user-owned category management

Merge shape:

```text
80 Category identity / persistence foundation  (#15 merged)
                 |
        +--------+--------+
        |                 |
81 Domain/repository   82 Compose management UI
   (#16 merged)           (#17 merged)
        |                 |
        +--------+--------+
                 |
        83 Integration / acceptance
             (PR #18)
                 |
      physical Samsung migration gate
```

### Agent 80 — merged

PR #15 froze durable built-in/custom identity, shared category definitions, schema-v2 migration, custom-definition/order persistence and cross-layer representation.

### Agent 81 — merged

PR #16 added repository-owned creation, stable-ID rename, explicit deletion policies, exact reorder validation, app-owned failures, atomic state mutation and regression tests.

### Agent 82 — merged

PR #17 added the Compose category-management surface, create/rename/delete/reorder interactions, accessibility semantics and UI tests without persistence duplication.

### Agent 83 — integration/acceptance

PR #18 wires the real composition root, repository, ViewModel, shelf/picker and management surface. Management presentation is derived from persisted `OrganizerState`; assigned counts include retained hidden/uninstalled overrides; repository failures are mapped to safe presentation messages; Android back/dismiss returns to the shelf.

Agent 83 must leave the PR unmerged until both the final automated lane and the physical Samsung upgrade/migration gate in [`CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md`](CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md) pass.

## 9. Recommended next development wave

After Agent 83 acceptance, the strongest next work is:

1. **local backup/export/import** — versioned local portability of organizer-owned state, preserving stable custom IDs/order;
2. **dynamic/pinned category shortcuts** — Android shortcuts that consume stable category IDs without redefining them.

These two lanes can run in parallel from the accepted category-management baseline because they consume the same stable identity but own different product behavior. Integrate them only after both individual lanes are green.

Optional F-Droid metadata enrichment and presentation polish remain independent later candidates. Performance work stays measurement-driven and should start only if a regression is observed.

## 10. Search and diagnostic reporting

Search uses the resolved category display name, so built-in and custom category names are searchable without a second category-name table.

The local classification report preserves the existing built-in report labels/format. If an effective category is custom, the report includes its stable identity and display name while retaining the independent `ClassificationSource`, including `USER_OVERRIDE`.

## 11. Metadata enrichment direction

Optional Internet-backed metadata enrichment remains approved in principle but not implemented. Any future provider must be supported/documented, local-first and best-effort, preserve existing precedence, cache locally with bounded policy, add `INTERNET` only with the supported implementation, and avoid unofficial brittle Play Store scraping.

F-Droid remains a plausible documented source for its subset of packages. Metadata enrichment is separate from category identity/lifecycle work.

## 12. Testing and quality policy

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

Persisted-state evolution additionally requires literal old-schema migration fixtures. Category-management tests cover stable built-in mapping, rename-stable custom identity, serialization/process recreation, explicit deletion policies, deterministic order validation, failure atomicity, classification precedence, search and diagnostic reporting.

Real Samsung acceptance remains mandatory for product-critical package discovery, launching, presentation, persisted upgrade behavior and integrated category-management behavior.

## 13. Remaining accepted limitations

- the classifier remains deliberately conservative;
- 123 entries in the accepted classification report remain `Unsorted`;
- 10 entries remain broad `Games`;
- standard TWA wrappers are not generically classified;
- no generic Samsung Internet/other-browser shortcut signature is established;
- optional external metadata enrichment is not implemented;
- the model remains one primary category per app;
- local backup/import/export is not implemented;
- dynamic/pinned category shortcuts are not implemented;
- the Agent 80–83 category-management wave remains pending until the physical Samsung migration pass is recorded.

## 14. Explicit non-goals

Do not expand the current product into:

- Samsung launcher database/folder manipulation;
- default HOME replacement;
- `QUERY_ALL_PACKAGES` for convenience;
- cloud/AI classification services;
- accounts, analytics or advertising;
- permanent monitoring service;
- unofficial brittle Play Store scraping;
- cross-platform implementation.
