# Parallel Development Plan

This document records the repository's delivery sequencing and ownership rules. Historical lanes remain useful because they explain the current boundaries; future parallel work must reuse those boundaries rather than inventing duplicates.

Read first:

- `../AGENTS.md`
- `STABLE_BASELINE.md`
- `ENGINEERING_BASELINE.md`
- `TECH_STACK.md`
- `PLAN.md`
- `ACCEPTANCE_CRITERIA.md`
- `MOSCOW.md`

## 1. Non-negotiable parallel-development rules

Every coding/integration lane must:

1. start from the required current `main`;
2. refresh HEAD and CI before continuing existing work;
3. read the authoritative baseline/scope docs before editing;
4. consume app-owned contracts rather than create parallel models;
5. stay inside explicit file ownership where practical;
6. avoid dependency/toolchain changes unless the lane is specifically an upgrade;
7. run the cheapest complete quality lane that proves its behavior;
8. report unresolved physical-device steps honestly;
9. leave its PR unmerged unless the owner explicitly asks for merge.

Shared-contract pressure is resolved by the smallest coordinated contract change, followed by rebasing affected lanes. Duplicated scanners, category engines, repositories, state models or UI models are not acceptable conflict avoidance.

## 2. Historical v0.1 delivery — complete

The v0.1 implementation used Agent 00 foundation, parallel Agents 10/20/30/40 for platform/domain/data/UI, and Agent 50 integration/acceptance.

The original shared contracts included:

```text
AppId
LaunchTargetId
InstalledApp
AppCategory
ClassificationSource
CategorizedApp
OrganizerState
InstalledAppSource
AppLauncher
CategoryEngine
OrganizerStateStore
OrganizerRepository
```

v0.1 is merged.

## 3. Permanent architecture/file ownership

The historical lane split remains the normal ownership guide:

```text
platform/apps/**  -> Android discovery/launch adapters
domain/**         -> pure categorization/search/report logic
rules/**          -> deterministic bundled rules
model/**          -> app-owned shared models
data/**           -> persisted state/repository adapters
ui/**             -> Compose presentation/UI-state mapping/design system
```

Build/toolchain policy remains centrally owned and effectively frozen during feature work.

Rules:

- Android types do not enter model/domain contracts;
- DataStore/serialization types do not enter repository/domain contracts;
- Compose/Material types do not enter data/domain contracts;
- user-owned state is explicitly versioned;
- no generic `AndroidManager`, `DataManager` or service-locator container.

## 4. Post-v0.1 classification wave — complete and merged

Agent 60 / PR #9 froze local classification reporting, additive Web/game taxonomy, selector precedence and separate rule-pack ownership. PRs #10–#13 supplied Web/PWA, triage/explanation UI, general rules and game rules.

Agent 70 / PR #14 integrated those lanes, repaired the evidence-backed Eden/Yuzu false positive, added the `Emulators` category/rules and completed owner Samsung acceptance. PR #14 is merged to `main`.

The historical merge shape was:

```text
60 Foundation / evidence
          |
          +----------------+----------------+
          |                |                |
     General rules      Game rules       Web/PWA
          |                |                |
          +----------------+----------------+
                           |
                    Triage/explanation UI
                           |
                    70 Integration
                           |
                 owner Samsung acceptance
                           |
                       PR #14 merged
```

This wave remains architecturally important because automatic rule packs continue to own only built-in `AppCategory` results and the outer precedence remains user override > bundled rule > Android category > `Unsorted`.

## 5. User-owned category-management wave — complete and merged

Merge shape and final status:

```text
80 Category identity / persistence foundation  -> PR #15 merged
                 |
        +--------+--------+
        |                 |
81 Category domain     82 Category UI
   PR #16 merged          PR #17 merged
        |                 |
        +--------+--------+
                 |
        83 Integration / acceptance
             PR #18 merged
```

Agent 80 froze durable `CategoryId`, shared category definitions, schema-v2 migration, custom-category persistence and deterministic category order. Agent 81 owns repository lifecycle behavior. Agent 82 owns Compose category-management presentation. Agent 83 integrated those contracts into the real app and completed the Samsung migration/count acceptance.

Later lanes consume those stable contracts and must not reopen category identity, lifecycle ownership or create another organizer/category state owner.

## 6. Portability + category-shortcut wave — Agent 92 integration complete

PR #19 delivered versioned local backup/export/import and PR #20 delivered dynamic/pinned category shortcuts. Both are merged. Agent 92 integrates them on PR #21 / `integration/portability-shortcuts`.

The integrated design keeps one `OrganizerStateStore`, uses Android SAF for user-selected backup files, preserves stable custom `CategoryId` across backup/import, and resolves shortcut destinations by stable category identity. Permanent CI and signed-debug build are green on the integration head. The next wave starts only from `main` containing PR #21; physical Samsung / One UI observations remain an explicit acceptance record to capture.

Historical merge shape:

```text
accepted category-management baseline
                 |
        +--------+--------+
        |                 |
90 Backup/import       91 Shortcuts
    PR #19 merged       PR #20 merged
        |                 |
        +--------+--------+
                 |
        92 Integration / acceptance
               PR #21
```

## 7. Recommended following wave — Agents 100–102

Start only from current `main` after PR #21 is merged and the post-merge HEAD/CI are refreshed.

```text
          merged Agent-92 baseline
                    |
          +---------+---------+
          |                   |
100 Supported metadata   101 Presentation polish
          |                   |
          +---------+---------+
                    |
          102 Integration / acceptance
```

### Agent 100 — supported metadata enrichment

Suggested branch:

```text
feature/supported-metadata-enrichment
```

Own only the supported metadata/provider path and the minimum shared classification contract it needs:

- app-owned metadata/provider interfaces and models;
- a documented provider, with F-Droid as the first candidate for covered packages;
- no unofficial Google Play scraping;
- provider/network types restricted to adapters;
- bounded local cache and best-effort/non-blocking lookup;
- explicit mapping from provider categories/tags to existing built-in organizer categories;
- first-rollout precedence `USER_OVERRIDE` > bundled rule > Android category > supported metadata > `Unsorted`;
- provider metadata may therefore resolve only entries that would otherwise remain `Unsorted`;
- distinct diagnostic/classification-source evidence when metadata decides category;
- `INTERNET` added only in this provider implementation PR, with privacy/cache documentation;
- offline/provider-failure tests proving the existing local shelf remains fully usable.

Do not modify custom-category lifecycle, backup/import semantics, shortcut identity or broad presentation.

### Agent 101 — presentation polish

Suggested branch:

```text
feature/presentation-polish
```

Own presentation-only refinement:

- One UI-inspired hierarchy, spacing and motion;
- shelf/category-management/Backup & restore navigation and action hierarchy;
- loading, empty, error, destructive-confirmation and shortcut-pin feedback states;
- large-font/scaled-text, touch-target, semantics and contrast review;
- edge-to-edge, launch, back/dismiss and sheet regressions;
- optional blur/visual effects only where platform behavior is reliable.

Do not modify repository/schema/classifier/network/toolchain contracts. This ownership separation allows Agent 100 and Agent 101 to run in parallel.

### Merge order

1. Run Agents 100 and 101 in parallel from the same merged Agent-92 baseline.
2. Merge Agent 100 first after its own tests/privacy/offline acceptance are green.
3. Refresh `main`, rebase Agent 101, inspect its real diff/CI and merge it.
4. Start Agent 102 from that exact current `main`.

The lower-level metadata lane merges first because it may introduce the shared metadata classification source and justified `INTERNET` permission. The presentation lane must remain independent enough that the rebase is mechanical rather than architectural.

### Agent 102 — integration / acceptance

Suggested branch:

```text
integration/metadata-presentation
```

Own only cross-feature integration and acceptance:

- prove all previously resolved manual/bundled/Android classifications remain stable;
- measure and record which formerly `Unsorted` packages improve through the supported provider;
- audit false positives and provider/category mappings;
- verify cold/offline/provider-error behavior;
- regress category lifecycle, backup/import, shortcuts, search, favourites, hide/restore, exact launch and back/dismiss;
- run permanent CI and relevant instrumentation;
- perform Samsung online/offline and presentation acceptance;
- audit that `INTERNET` is justified while `QUERY_ALL_PACKAGES`, broad storage, telemetry, accounts/cloud sync and background services remain absent;
- update authoritative docs and recommend the next wave based on evidence.

### Performance rule after Agent 102

Performance is not a standing parallel lane. Agent 102 should capture simple startup/scan/UI observations; create a dedicated performance branch only if measurements expose a concrete regression or target. No speculative benchmark or baseline-profile project.

## 8. Quality gates by responsibility

### Pure domain/rules

- deterministic unit tests;
- duplicate/conflicting selector failures;
- precedence tests;
- no Android framework dependency;
- no label/network inference.

### Persistence/repository

- explicit schema version;
- literal prior-schema migration fixtures;
- override/favourite/hidden survival;
- custom-category serialization round-trip;
- order round-trip and duplicate/stale normalization;
- deterministic stale/uninstall behavior.

### UI

- UI-state mapping derives from app-owned state;
- custom and built-in display names come from the shared category definition;
- Compose behavior/semantics tests;
- no classifier/persistence implementation duplicated in Compose;
- physical Samsung review where presentation behavior matters.

### Permanent repository lane

- assemble debug app;
- compile instrumentation test APK;
- JVM tests;
- Android Lint;
- ktlint;
- dependency `buildHealth`;
- strict dependency verification;
- Gradle warning-mode failure;
- configuration-cache creation/reuse;
- forbidden-permission checks.

## 9. Conflict-resolution policy

When two lanes need the same concept:

1. identify the actual lower-level owner;
2. change the smallest app-owned contract only if truly required;
3. make sibling impact explicit;
4. rebase affected branches;
5. keep framework/library-specific types at adapters;
6. do not solve merge pressure through duplicated implementations.

For category management, identity/schema/order representation came from Agent 80, lifecycle behavior from Agent 81, presentation from Agent 82 and integration-only repair from Agent 83. Later backup and shortcuts consume those contracts rather than reopening them.

## 10. PR control

Green CI is necessary but not sufficient. A PR is merge-ready only when its lane-specific acceptance is satisfied and all unresolved device/migration steps are visible.

No coding/integration agent merges its own PR unless the owner explicitly instructs it to do so.
