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

## 5. User-owned category-management wave — integrated, physical acceptance pending

Merge shape and current status:

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
             PR #18 draft
                 |
      physical Samsung migration gate
```

### Agent 80 — merged foundation

Branch used:

```text
feature/category-identity-foundation
```

Agent 80 froze:

- `CategoryId` durable identity;
- built-in IDs explicitly attached to `AppCategory` rather than derived from enum names/labels;
- `CategoryDefinition` shared app-owned category contract;
- `CustomCategoryDefinition` representation;
- `CategorizedApp` effective category widened to the shared definition contract;
- `OrganizerState` schema v2;
- category overrides stored as `CategoryId`;
- persisted custom-category definitions;
- persisted category order and deterministic normalization;
- literal schema-v1 migration;
- repository/search/report/UI-state compatibility required to represent custom categories.

Built-ins use explicit IDs in the `builtin:` namespace. User-created categories use `custom:<opaque-id>`. Display name is metadata and never persisted identity.

Schema v2 persists override IDs, favourites, hidden apps, custom definitions and category order. Literal v1 enum override values migrate to explicit built-in IDs while favourites/hidden state survives.

`Favourites` stays virtual and outside persisted order. Read normalization preserves first valid occurrences, drops stale/duplicates, appends missing built-ins, then missing custom definitions.

### Agent 81 — merged domain/repository

PR #16 owns:

- creation and opaque ID generation;
- rename without identity change;
- delete with explicit reassignment/return-to-automatic policy;
- exact-permutation category reorder mutation;
- app-owned lifecycle errors;
- atomic repository/state operations;
- pure/persistence regression tests.

It consumes the Agent-80 models/schema and does not redefine automatic rule packs.

### Agent 82 — merged Compose UI

PR #17 owns:

- create/rename/delete/reorder presentation;
- dedicated category-management surface;
- accessible Compose state/semantics/tests;
- UI behavior against app-owned category definitions/order.

It does not implement DataStore/repository behavior.

### Agent 83 — integration / acceptance

Branch:

```text
integration/category-management
```

PR #18 owns:

- one real `DefaultOrganizerRepository` exposed through the separate app-owned `OrganizerRepository` and `CategoryManagementRepository` contracts;
- ViewModel intent/result translation without duplicated lifecycle rules;
- real shelf/category-picker/category-management wiring;
- management state derived from persisted `OrganizerState`;
- assignment counts that include retained hidden/uninstalled overrides;
- sanitized lifecycle failure presentation;
- Android back/dismiss integration;
- direct integration regression tests;
- final automated permanent lane;
- physical schema-v1/pre-category-management over-install acceptance on the primary Samsung device;
- final authoritative documentation.

PR #18 remains draft/unmerged until the physical checklist in `CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md` passes. Green CI alone is not sufficient.

## 6. Recommended next wave after Agent 83 acceptance

Once PR #18 has passed physical Samsung migration acceptance and is merged, two independent consumers of stable category identity are strong parallel candidates:

```text
accepted category-management baseline
                 |
        +--------+--------+
        |                 |
 local backup/export   category shortcuts
        |                 |
        +--------+--------+
                 |
        integration / acceptance
```

### Local backup/export/import lane

Own only:

- versioned local export format;
- local file export/import UX and adapter behavior;
- validation before replacing organizer state;
- preservation of custom IDs/order/overrides/favourites/hidden state;
- import/export migration and corruption tests.

Do not redefine category identity or lifecycle semantics.

### Dynamic/pinned shortcuts lane

Own only:

- Android dynamic category shortcuts;
- pinned shortcut request flow;
- stable `CategoryId` payload/resolution;
- rename-safe labels/refresh behavior;
- platform/Compose tests appropriate to shortcuts.

Do not duplicate category persistence or make shortcuts a second source of category state.

These lanes may run in parallel because they consume the accepted category ID contract but do not own the same feature behavior. A later integration lane should re-prove upgrade/state compatibility and Samsung behavior.

Optional F-Droid metadata enrichment and presentation polish remain separate later work. Performance work starts only after measurement proves a regression.

## 7. Quality gates by responsibility

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

## 8. Conflict-resolution policy

When two lanes need the same concept:

1. identify the actual lower-level owner;
2. change the smallest app-owned contract only if truly required;
3. make sibling impact explicit;
4. rebase affected branches;
5. keep framework/library-specific types at adapters;
6. do not solve merge pressure through duplicated implementations.

For category management, identity/schema/order representation came from Agent 80, lifecycle behavior from Agent 81, presentation from Agent 82 and integration-only repair from Agent 83. Later backup and shortcuts consume those contracts rather than reopening them.

## 9. PR control

Green CI is necessary but not sufficient. A PR is merge-ready only when its lane-specific acceptance is satisfied and all unresolved device/migration steps are visible.

No coding/integration agent merges its own PR unless the owner explicitly instructs it to do so.
