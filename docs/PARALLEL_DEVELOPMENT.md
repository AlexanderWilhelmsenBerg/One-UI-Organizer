# Parallel Development Plan

**Prepared:** 2026-09-08

This document is the execution plan for moving One UI Organizer from planning into implementation using several coding agents without creating overlapping ownership, incompatible abstractions, or build-tool drift.

Read first:

- `../AGENTS.md`
- `STABLE_BASELINE.md`
- `ENGINEERING_BASELINE.md`
- `TECH_STACK.md`
- `PLAN.md`
- `ACCEPTANCE_CRITERIA.md`
- `MOSCOW.md`

## 1. Parallelization strategy

Do **not** start every coder from the current empty repository independently. The project has a strict build/toolchain baseline and shared contracts; allowing several agents to invent those simultaneously would create avoidable merge conflicts and incompatible architecture.

Use two implementation waves.

### Wave 0 — foundation, one owner

**Agent 00 — Foundation / Scaffold / CI**

This agent starts first and owns the only initial build-system PR.

It creates:

- Gradle wrapper and Android project scaffold;
- version catalog using the exact stable baseline;
- Gradle daemon JVM criteria and Java toolchain configuration;
- dependency verification;
- Kotlin DSL settings/build files;
- `:app` module;
- package structure and app-owned shared contracts/models needed by the parallel lanes;
- warning-free compile/lint/ktlint/dependency-health checks;
- baseline GitHub Actions workflow;
- minimal smoke activity only as needed to prove the scaffold;
- no organizer feature implementation beyond contract shells and smoke wiring.

**Merge Agent 00 before starting Wave 1.**

This is the only hard sequencing gate.

### Wave 1 — four agents in parallel

After Agent 00 is merged, start Agents 10, 20, 30 and 40 from the same updated `main`.

| Agent | Lane | Primary ownership | Must not own |
|---|---|---|---|
| 10 | Android platform spike | `platform/apps/**`, platform integration tests/spike diagnostics | category rules, DataStore, final shelf UI |
| 20 | Categorization/domain | `domain/**`, known rules, search/category pure tests | PackageManager, DataStore, Compose screen |
| 30 | Persistence/repository | `data/**`, state schema/migrations, repository merge logic | package scanning implementation, final UI |
| 40 | Compose/design system | `ui/**`, theme/tokens, reusable shelf components with fake data | PackageManager, DataStore schema, category algorithm |

Agents may add tests inside their owned lane. They should not edit shared contracts unless the contract is genuinely unworkable; such a change must be isolated, explained in the PR, and coordinated before other lanes merge.

### Wave 2 — integration and hardening

After the four Wave-1 PRs are merged, start:

**Agent 50 — Integration / Acceptance / Performance**

This agent owns:

- composition root and real dependency wiring;
- ViewModel/UI-state integration;
- search + category + persistence behavior across real boundaries;
- final long-press actions and hidden-app management needed for v0.1;
- Samsung-device acceptance checklist;
- instrumentation/UI Automator cross-app tests;
- performance measurements and benchmark module only when the product flow exists;
- release-candidate quality gates;
- fixing integration defects without broadening product scope.

Agent 50 must not silently replace architecture or dependencies that already satisfy the documented contracts.

## 2. Shared contract freeze after Agent 00

Agent 00 should create the smallest useful app-owned contracts so parallel work has a stable seam. Names may be adjusted if Kotlin/package conventions demand it, but responsibilities should remain equivalent.

Suggested contracts/models:

```text
model/
  AppId
  LaunchTargetId
  InstalledApp
  AppCategory
  ClassificationSource
  CategorizedApp
  OrganizerState

platform/apps/
  InstalledAppSource
  AppLauncher

domain/
  AppCategorizer / CategoryEngine contract
  SearchNormalizer contract if needed

data/
  OrganizerStateStore
  OrganizerRepository contract
```

Rules:

- Android types do not enter `model`/`domain` contracts.
- DataStore/serialization types do not enter repository/domain contracts.
- Compose types do not enter domain/data contracts.
- user-owned state is versioned from schema version 1.
- interfaces remain narrow; no generic `AndroidManager`, `DataManager`, or service-locator container.

## 3. Branch and PR discipline

Suggested branches:

```text
foundation/scaffold
feature/platform-apps
feature/category-engine
feature/state-repository
feature/shelf-ui
integration/v0.1
```

Every agent must:

1. branch from the latest required `main`;
2. read `AGENTS.md` and all referenced baseline docs before editing;
3. stay inside its lane;
4. use only current versions from `STABLE_BASELINE.md`;
5. not change dependency/toolchain versions unless the task is specifically an upgrade;
6. run the lane's relevant quality gates;
7. leave the PR unmerged for review;
8. report exact tests run and any unresolved device-only acceptance steps.

## 4. File ownership rules

### Agent 00

Owns initially:

```text
settings.gradle.kts
build.gradle.kts
gradle/**
gradlew*
app/build.gradle.kts
.github/workflows/**
shared contract/model skeletons
```

After Agent 00 merges, build files are effectively frozen. Feature agents may request a dependency addition but should not casually modify toolchain versions or common build policy.

### Agent 10

Expected ownership:

```text
app/src/main/java/**/platform/apps/**
app/src/androidTest/**/platform/apps/**
```

### Agent 20

Expected ownership:

```text
app/src/main/java/**/domain/**
app/src/main/java/**/rules/**
app/src/test/**/domain/**
app/src/test/**/rules/**
```

### Agent 30

Expected ownership:

```text
app/src/main/java/**/data/**
app/src/test/**/data/**
```

### Agent 40

Expected ownership:

```text
app/src/main/java/**/ui/**
app/src/test/**/ui/** where appropriate
app/src/androidTest/**/ui/** where appropriate
```

### Agent 50

Owns integration points after Wave 1:

```text
composition root/application wiring
ViewModel integration
cross-layer acceptance tests
benchmark module when justified
release workflow refinements
```

## 5. Merge order

Required order:

```text
00 Foundation
      |
      +----------------+----------------+----------------+
      |                |                |                |
   10 Platform      20 Domain        30 Data          40 UI
      |                |                |                |
      +----------------+----------------+----------------+
                       |
                 50 Integration
                       |
                 device acceptance
                       |
                     v0.1
```

Within Wave 1, PRs can merge in any order **provided they remain inside their ownership boundaries and use the frozen contracts**.

If one Wave-1 PR legitimately changes a shared contract, pause merge of affected sibling PRs, rebase them onto the contract change, then continue. Do not solve contract drift by duplicating models.

## 6. Quality gates by lane

### Foundation

- clean checkout builds;
- Gradle daemon JDK criteria uses the documented JDK 26 baseline;
- Android compile/JVM target remains Java 17;
- dependency verification enabled;
- configuration cache works for normal verification tasks;
- zero compiler/deprecation/lint/ktlint warnings in the scaffold;
- CI uses immutable action SHAs.

### Platform

- launcher-visible discovery without `QUERY_ALL_PACKAGES`;
- deterministic component identity;
- Organizer excluded;
- representative launch targets open correctly;
- package scanning off main thread;
- real Samsung spike result documented.

### Domain

- category precedence fully unit-tested;
- fallback never hides launchable apps;
- user override always wins;
- known rules deterministic;
- search normalization pure and local.

### Persistence/repository

- schema version 1 explicit;
- persisted overrides/favourites/hidden survive process recreation;
- migration tests exist before any schema 2 change;
- uninstall/stale-state policy deterministic;
- repository merges source + state without UI knowledge.

### UI

- fake-data previews/tests require no real PackageManager/DataStore;
- category browsing/search visuals;
- app tiles and long-press affordance;
- light/dark/system theme;
- accessibility semantics;
- One UI-inspired design tokens centralized.

### Integration

- all v0.1 Must items and acceptance criteria exercised;
- physical Samsung acceptance complete;
- no `INTERNET` or `QUERY_ALL_PACKAGES` permission;
- cross-app launch tests via UI Automator where stable;
- release build/lint/tests pass;
- performance measured for startup, scan, search, scroll and launch journeys before optimizing.

## 7. Conflict-resolution policy

When two lanes need the same file:

1. prefer moving the shared concept into an app-owned contract rather than having both agents edit the integration implementation;
2. the agent that owns the lower-level boundary implements it;
3. sibling agents consume the contract using fakes;
4. integration wiring waits for Agent 50 unless it is strictly required to demonstrate a lane;
5. never solve merge pressure by allowing duplicated models or duplicated persistence/scanner implementations.

## 8. What can be developed in parallel later

After v0.1, future work can use the same lane pattern:

- custom categories + export/import;
- shortcuts;
- widget;
- work-profile support;
- performance/baseline profiles;
- rule tooling.

Each feature should receive an explicit owner and adapter boundary before parallel coding begins.

## 9. Ready-to-start criterion

Parallel implementation is ready when Agent 00 is merged and `main` contains:

- a clean, warning-free Android scaffold;
- frozen app-owned contracts;
- reproducible toolchains;
- CI gates;
- no feature agent-specific implementation hidden in the foundation PR.

Copy/paste prompts for each agent live under [`agents/`](agents/README.md).

## 10. Post-v0.1 classification wave

The Agent 00–50 sequence above is the historical v0.1 delivery structure. v0.1 is merged. Classification-quality work uses a new evidence-first gate rather than reopening those completed lanes.

### Agent 60 — classification foundation / evidence

Agent 60 starts alone from current `main` on `feature/classification-foundation` and owns the shared classification seams:

```text
app/src/main/java/**/domain/ClassificationReportFormatter.kt
app/src/main/java/**/rules/KnownAppRules.kt
app/src/main/java/**/rules/BundledKnownAppRules.kt
app/src/main/java/**/rules/general/GeneralKnownAppRules.kt
app/src/main/java/**/rules/games/GameKnownAppRules.kt
app/src/main/java/**/rules/web/WebShortcutKnownAppRules.kt
app/src/main/java/**/model/AppCategory.kt
classification-report integration and its regression tests
classification taxonomy/migration decision
classification-wave documentation
```

The primary Samsung report has been reviewed privately. Sanitized evidence and the frozen decisions are recorded in `CLASSIFICATION_ROADMAP.md`; raw launcher rows remain private and must never be committed.

Agent 60 freezes an additive taxonomy: `Web Shortcuts`, five broad game subcategories (`Action & Adventure`, `RPG`, `Strategy & Simulation`, `Puzzle & Casual`, `Board & Card`) and the existing `Games` fallback. Existing category names remain unchanged, so organizer-state schema version 1 remains valid and no persistence migration is required.

The shared bundled-rule selector contract is also frozen as exact component > exact package > package prefix. Package-prefix matching exists only because the reviewed report established a generated Chromium WebAPK namespace that cannot be represented by a durable finite exact-package list. Prefixes require a trailing package-segment boundary; duplicate or overlapping prefixes fail fast. The outer category precedence remains user override > bundled rule > Android category > `Unsorted`.

### Parallel classification lanes after Agent 60 merges

Only after Agent 60 is merged may the rule expansion lanes start from the same updated `main`.

| Lane | Production ownership | Test ownership | Shared files it must not edit |
|---|---|---|---|
| General rules | `app/src/main/java/**/rules/general/GeneralKnownAppRules.kt` | matching `app/src/test/**/rules/general/**` tests | selector/index/composition, taxonomy, persistence |
| Game rules | `app/src/main/java/**/rules/games/GameKnownAppRules.kt` | matching `app/src/test/**/rules/games/**` tests | selector/index/composition, taxonomy, Android mapping |
| Web/PWA rules | `app/src/main/java/**/rules/web/WebShortcutKnownAppRules.kt` | matching `app/src/test/**/rules/web/**` tests | selector/index/composition, taxonomy, platform scanner unless explicitly re-coordinated |

These lanes add evidence-backed rules only. They do not create alternate engines, duplicate selectors, local taxonomies, or label-guessing systems to avoid coordination.

The game lane uses only the five frozen subcategories and leaves uncertain/mixed titles in `Games`. It does not introduce `Sports & Racing` or `Other Games` without a later coordinated taxonomy decision.

The Web/PWA lane may use the frozen package-prefix selector for evidence-backed generated namespaces such as Chromium WebAPK packages. It must not classify arbitrary TWA wrappers, activity names, labels, or URLs without separate deterministic evidence.

### Classification-wave conflict rules

1. `KnownAppSelector`, `KnownAppRuleSet`, `BundledKnownAppRules`, `AppCategory`, `ClassificationSource`, and persisted schema are shared foundation contracts after Agent 60.
2. Exact component matching precedes exact package matching, which precedes package-prefix matching inside the bundled-rule tier; the outer precedence remains user override > bundled rule > Android category > `Unsorted`.
3. Duplicate selectors and overlapping package prefixes are build/test failures, not merge-time conventions.
4. Another matcher requires reviewed device evidence and an explicit shared-contract PR before parallel rule branches consume it.
5. A taxonomy rename/removal is persistence work and cannot be slipped into a rule-pack PR.
6. Each rule lane reports before/after aggregate effects from the same evidence set where practical, while raw owner-device rows remain private.

### Post-v0.1 merge shape

```text
60 Classification foundation + evidence
                  |
                  +----------------+----------------+
                  |                |                |
             General rules     Game rules      Web/PWA rules
                  |                |                |
                  +----------------+----------------+
                                   |
                         classification integration
                         + owner-device validation
```

Green CI and reviewed owner evidence are both required before Agent 60 is merge-ready. Subsequent rule-expansion PRs remain independently reviewable and unmerged until explicitly approved.
