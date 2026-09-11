# Product and Delivery Plan

**Implementation status:** v0.1 integration is complete on the integration branch and is in final merge verification. Classification-quality tuning is explicitly deferred to the next post-v0.1 PR; see [`CLASSIFICATION_ROADMAP.md`](CLASSIFICATION_ROADMAP.md).

Execution details and agent ownership are defined in [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md). Copy/paste coding briefs live under [`agents/`](agents/README.md).

## 1. Product definition

**One UI Organizer** is a small Android companion app that provides an automatically categorized app shelf while leaving Samsung One UI Home untouched as the system launcher.

The problem it solves is simple: One UI can sort apps alphabetically and lets the user create folders manually, but it does not maintain a useful category system automatically. One UI Organizer provides that organized view without taking ownership of the home screen, widgets, gestures, or Samsung launcher state.

## 2. Core constraints

1. One UI Home remains the default launcher.
2. The app must not write to Samsung private launcher databases or rely on undocumented Samsung APIs.
3. The app must not request `QUERY_ALL_PACKAGES` for v0.1.
4. v0.1 requires no Internet permission, account, telemetry, analytics, advertising, or backend.
5. Manual user classification always overrides automatic classification.
6. Scanning must not require a persistent service.
7. New dependencies must earn their place; the first version remains structurally small.
8. The primary physical-device target is a current Samsung phone running modern One UI / Android 16, while the planned minimum Android version is API 28.
9. Toolchains and libraries follow `STABLE_BASELINE.md`: stable releases only and the newest mutually compatible stack.
10. Parallel agents must follow frozen app-owned contracts and explicit file ownership instead of duplicating models or implementations.

## 3. User experience

### Primary flow

- User taps the Organizer icon from One UI Home.
- Organizer opens as a fast One UI-inspired activity/sheet.
- A search control and categorized app sections are immediately available.
- User taps an app and the selected launchable activity opens.
- Back/dismiss returns directly to the existing Samsung home experience.

### Category model

v0.1 uses one **primary category** per app plus independent favourite/hidden state. Multi-category tagging is deliberately deferred.

Classification priority:

1. User override.
2. Bundled known-app rule.
3. Android-declared category mapping where useful.
4. `Unsorted` fallback.

Classification is deterministic. The same installed-app metadata, user state, and rule set must produce the same result.

### Planned initial categories

The baseline taxonomy is:

- Favourites (virtual section)
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
- Development
- Tools
- Games
- Other / Unsorted

Categories are intentionally correctable; the UI must make manual correction easy.

## 4. Architecture

Keep v0.1 as a single Android application module unless a concrete implementation constraint proves a split necessary.

Logical boundaries:

```text
Android PackageManager
        |
        v
InstalledAppSource
        |
        v
CategoryEngine <--- bundled known-app rules
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
        Jetpack Compose UI
```

### Shared app-owned contracts

The foundation/scaffold PR freezes the smallest useful shared models/contracts before feature agents start:

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
CategoryEngine/AppCategorizer
OrganizerStateStore
OrganizerRepository
```

Rules:

- Android framework objects do not leak through domain/application contracts.
- DataStore/serialization implementation types do not leak through repository contracts.
- Compose types do not leak into data/domain contracts.
- persisted state starts with explicit schema version 1.
- no service locator or generic god-manager abstraction.

### InstalledAppSource

Responsibilities:

- discover launcher-visible activities through supported Android APIs;
- expose package/component identity, label/icon access, and declared Android category through app-owned models;
- exclude Organizer itself;
- handle packages with more than one launcher entry deterministically;
- rescan when Organizer opens/resumes rather than maintaining a permanent observer in v0.1.

Implementation should prefer an `ACTION_MAIN` + `CATEGORY_LAUNCHER` query declared in manifest package visibility over broad package visibility.

### CategoryEngine

Pure Kotlin logic with no Android UI dependency.

Inputs:

- installed-app metadata;
- bundled known-app rules;
- user overrides.

Outputs:

- primary category;
- classification source/reason.

### OrganizerRepository

Single source of truth combining discovered apps, categorization, and persisted user-owned state.

Persist only what must survive scans/process recreation:

- category overrides;
- favourite state;
- hidden state;
- category metadata/order when those features are later introduced.

Do not persist app icons or labels as authoritative data.

### Persistence

Start with typed DataStore state plus Kotlin serialization rather than Room.

The initial state is small and non-relational. Room/KSP remain a future migration path only if the product genuinely develops relational requirements.

### UI

Use Jetpack Compose + Material 3 with a small One UI-inspired design layer:

- thumb-reachable controls;
- generous spacing/proportions;
- rounded surfaces;
- dynamic colour where appropriate with deterministic fallback;
- edge-to-edge support;
- translucent/sheet host where reliable on Samsung devices.

Do not copy Samsung proprietary assets.

## 5. Technical risks to prove first

### Risk A — package visibility

Prove the intended launcher query returns the practical expected app set on the Samsung device without `QUERY_ALL_PACKAGES`.

### Risk B — reliable launching

Prove exact launcher-component selection works, including aliases/multiple launcher activities.

### Risk C — companion presentation

Prove the companion can open over One UI acceptably on Android 16. Preferred order:

1. sheet/translucent;
2. dimmed/translucent fallback;
3. normal edge-to-edge fallback.

Blur is optional.

### Risk D — duplicate launcher activities

Define deterministic identity and duplicate behavior before package name is treated as unique.

## 6. Delivery sequence and parallel waves

### Milestone 0 — planning baseline — complete

Delivered on `main`:

- README;
- product/delivery plan;
- acceptance criteria;
- MoSCoW analysis;
- technology decisions;
- verified stable toolchain/library inventory;
- engineering/testing/benchmark/upgrade policy;
- parallel coding plan;
- coding-agent prompts.

### Wave 0 / Milestone 1A — Agent 00 foundation — complete

Delivered:

- Android/Gradle scaffold;
- exact stable-compatible toolchain;
- version catalog;
- reproducible JDK/toolchain setup;
- dependency verification;
- warning-free lint/ktlint/dependency-health/CI lane;
- minimal Compose smoke app;
- shared app-owned contracts/models frozen for the parallel lanes.

### Wave 1 / Milestone 1B-3 — complete

The four parallel lanes delivered platform discovery/launching, category/search domain behavior, persistence/repository state, and the Compose UI/design system.

#### Agent 10 — platform apps / integration spike

Delivered:

- Android launcher-app discovery;
- exact launch target handling;
- alias/duplicate policy;
- package visibility behavior;
- companion host presentation spike;
- platform/instrumented tests and Samsung test checklist/results.

#### Agent 20 — category/search domain

Delivered:

- deterministic precedence engine;
- Android category mapping from app-owned metadata;
- bundled known-app rules;
- classification source;
- pure local search normalization/filtering;
- comprehensive unit tests.

#### Agent 30 — state/repository

Delivered:

- DataStore-backed schema version 1;
- state serialization/default/corruption policy;
- category override/favourite/hidden persistence;
- repository merge behavior;
- uninstall/stale/reinstall policy;
- persistence/repository tests.

#### Agent 40 — Compose UI/design system

Delivered:

- One UI-inspired theme/tokens;
- shelf/category/app-tile/search/loading/empty surfaces;
- long-press organization UI;
- hidden-app management components;
- light/dark/accessibility behavior;
- UI tests against fake app-owned state.

### Wave 2 / Milestone 4 — Agent 50 integration and hardening — merge verification

Delivered in the integration PR:

- real dependency/composition wiring;
- app-owned presentation/UI-state integration;
- complete v0.1 Must behavior;
- boundary regression repairs;
- privacy/package audit;
- full warning-free quality lane;
- physical Samsung proof that the integrated app launches and category shelf renders.

The integration PR remains owner-controlled and must not merge automatically.

### Next post-v0.1 slice — classification quality and taxonomy tuning

Owner testing found the starter categorization useful but coarse, with 225 entries in `Unsorted` and 129 in `Games`, plus some tiny categories and browser-created/PWA-style launcher shortcuts.

This is deliberately a **separate follow-up PR**, not an expansion of the integration PR. The detailed scope lives in [`CLASSIFICATION_ROADMAP.md`](CLASSIFICATION_ROADMAP.md).

The next slice should improve classification from real device evidence while preserving deterministic rules, user-override precedence, local-only behavior, and `Unsorted` as the safe fallback. It should evaluate Web Shortcuts, broad game subcategories, rule-pack expansion, and consolidation of low-value tiny categories.

## 7. Merge/conflict rules

Required merge structure:

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
```

If a Wave-1 agent needs to change a frozen shared contract:

1. make the smallest compatible change;
2. call it out prominently in the PR;
3. pause affected sibling merges;
4. rebase affected sibling branches after the contract change;
5. never create duplicated parallel models to avoid coordination.

See `PARALLEL_DEVELOPMENT.md` for exact file ownership and quality gates.

## 8. Testing strategy

### Unit tests

Prioritize pure tests for:

- category precedence;
- Android category mapping;
- known rule matching;
- fallback behavior;
- search normalization;
- state schema/default/migrations;
- persisted override merge behavior;
- uninstall/reinstall identity edge cases.

### Compose tests

Cover semantic/user behavior for:

- search;
- category rendering;
- long-press actions;
- favourite/hidden UI;
- empty/loading/unsorted states;
- accessibility.

### Instrumented/system tests

Cover:

- package discovery;
- launch targets;
- launcher aliases;
- system/package visibility;
- cross-app launch with UI Automator where reliable;
- Samsung window/presentation behavior.

### Physical-device acceptance

A Samsung physical-device pass is mandatory for v0.1. Emulator-only acceptance is insufficient.

## 9. Performance strategy

Do not optimize before the integrated flow exists.

Measure real journeys using the stable tooling in `STABLE_BASELINE.md` when performance work is justified:

- cold/warm start;
- initial/resume scan;
- search latency;
- category scrolling/frame timing;
- Organizer -> external app launch.

Macrobenchmark is preferred for end-to-end journeys; Microbenchmark only for isolated hot code. Do not add benchmark tooling merely to satisfy a checklist when no measured issue exists.

## 10. Post-v0.1 roadmap

### Immediate next slice

- evidence-driven classification-quality tuning from real Samsung launcher data;
- expand deterministic bundled known-app rules;
- identify browser/PWA launcher entries as `Web Shortcuts` only when supported metadata gives a reliable signature;
- evaluate a small broad game taxonomy rather than one 129-app bucket;
- review/consolidate categories with only one or two apps;
- add local diagnostic/export support only if it materially helps rule tuning, with no telemetry/networking.

See [`CLASSIFICATION_ROADMAP.md`](CLASSIFICATION_ROADMAP.md) for the detailed acceptance direction.

### Other Should candidates

- custom categories;
- reorder categories;
- shortcuts/pinned category shortcuts;
- export/import organizer rules;
- richer `Unsorted` management;
- more complete One UI-inspired polish;
- performance/baseline profiles when the stable toolchain supports them cleanly and measurements justify them.

### Could candidates

- Glance widget;
- multiple category tags;
- work-profile support;
- opt-in local usage-based suggestions;
- fuzzy search;
- editable/importable rule packs;
- large-screen/foldable adaptation.

## 11. Explicit non-goals

Do not broaden v0.1 into:

- native One UI folder/page/database manipulation;
- default-launcher replacement;
- cloud/AI classification;
- analytics/ads/accounts;
- `QUERY_ALL_PACKAGES` for convenience;
- permanent monitoring service;
- cross-platform implementation.

## 12. Definition of done for v0.1

v0.1 is done only when:

- all Must-have items in `MOSCOW.md` are complete;
- all criteria in `ACCEPTANCE_CRITERIA.md` pass or an explicit exception is approved;
- the build/toolchain/library baseline remains compliant and warning-free;
- physical Samsung acceptance passes;
- no `INTERNET` or `QUERY_ALL_PACKAGES` is present;
- the owner explicitly approves release/merge decisions.
