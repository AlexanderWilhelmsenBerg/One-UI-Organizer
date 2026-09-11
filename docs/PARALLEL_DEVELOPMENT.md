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

## 5. Current wave — user-owned category management

Category management requires a shared identity/persistence contract before domain and Compose work can safely run in parallel.

Merge shape:

```text
80 Category identity / persistence foundation
                 |
        +--------+--------+
        |                 |
81 Category domain     82 Category UI
        |                 |
        +--------+--------+
                 |
        83 Integration / acceptance
```

### Agent 80 — custom-category identity/persistence foundation

Branch:

```text
feature/category-identity-foundation
```

Agent 80 owns the frozen shared contract only:

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
- repository/search/report/UI-state compatibility required to represent custom categories;
- regression tests and authoritative documentation.

Agent 80 does **not** own complete create/rename/delete/reorder workflows or a category-management screen.

#### Identity contract

Built-ins use explicit IDs in the `builtin:` namespace. User-created categories use `custom:<opaque-id>`. Display name is metadata and must never be used as persisted identity. Renaming a custom category therefore changes only display metadata.

The later domain lane generates the opaque custom ID once at creation. It must not derive identity from the user-visible name.

#### Schema-v2 contract

Schema v2 persists:

- category override IDs;
- favourites;
- hidden apps;
- custom category definitions;
- category order.

Literal v1 enum override values migrate to the explicit built-in IDs. Existing favourites/hidden state survives. Migration starts with no custom categories and the existing built-in order. Any later write uses schema v2.

#### Order contract

`Favourites` stays virtual and outside persisted order. Effective order:

1. preserves the first valid persisted occurrence;
2. drops stale/unknown IDs;
3. drops duplicates after the first occurrence;
4. appends missing built-ins in built-in default order;
5. appends missing custom categories in definition order.

New/missing built-ins therefore appear deterministically and malformed stale order state cannot crash the app.

### Agent 81 — category management domain/repository

Starts only after Agent 80 is merged to `main`.

Owns:

- creation and opaque ID generation;
- rename without identity change;
- delete with an explicit reassignment/fallback policy;
- category reorder mutation API;
- repository/state operations;
- pure Kotlin regression tests.

Agent 81 must consume `CategoryId`, `CategoryDefinition`, `CustomCategoryDefinition` and schema-v2 `OrganizerState`; it must not add a second category entity/identity system or rewrite automatic rule packs.

### Agent 82 — category management Compose UI

Starts from the same post-Agent-80 `main` and may run in parallel with Agent 81 only against the frozen shared contract.

Owns:

- create/rename/delete/reorder presentation;
- dedicated category-management surface;
- accessible Compose state/semantics/tests;
- UI-only wiring against app-owned category definitions/order.

Agent 82 must not persist UI-only categories, duplicate repository behavior, or expose Compose/Material types through shared/domain contracts.

### Agent 83 — integration / acceptance

Starts after 81 and 82 are merged.

Owns:

- cross-layer wiring;
- genuine integration repair;
- schema-v1 upgrade proof through the integrated app;
- real Samsung acceptance for lifecycle/order behavior;
- final documentation/acceptance result;
- leaves merge decision to the owner.

## 6. Quality gates by responsibility

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

## 7. Conflict-resolution policy

When two lanes need the same concept:

1. identify the actual lower-level owner;
2. change the smallest app-owned contract only if truly required;
3. make sibling impact explicit;
4. rebase affected branches;
5. keep framework/library-specific types at adapters;
6. do not solve merge pressure through duplicated implementations.

For the current wave, identity/schema/order semantics are Agent 80-owned. Lifecycle behavior is Agent 81-owned. Category-management presentation is Agent 82-owned. Integration-only changes belong to Agent 83.

Backup/export/import and dynamic/pinned shortcuts consume this category identity later; they must not be folded into Agent 80 merely because persistence is already changing.

## 8. PR control

Green CI is necessary but not sufficient. A PR is merge-ready only when its lane-specific acceptance is satisfied and all unresolved device/migration steps are visible.

No coding/integration agent merges its own PR unless the owner explicitly instructs it to do so.
