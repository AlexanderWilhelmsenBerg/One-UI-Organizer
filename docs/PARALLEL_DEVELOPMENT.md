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

The original implementation used three waves.

### Agent 00 — foundation / scaffold / CI

Owned:

- Gradle/Android scaffold;
- central versions/toolchains;
- dependency verification;
- CI/quality gates;
- shared app-owned model/contract skeletons.

This foundation merged before feature implementation.

### Wave 1 — Agents 10/20/30/40

| Agent | Lane | Primary ownership |
| --- | --- | --- |
| 10 | Android platform apps | launcher discovery/launch adapters and platform tests |
| 20 | Categorization/search | pure domain category/search behavior and rules |
| 30 | Persistence/repository | DataStore state and repository merge behavior |
| 40 | Compose UI/design | UI models, shelf components, theme/tokens and UI tests |

The shared app-owned contracts established by the foundation included:

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

### Agent 50 — v0.1 integration / acceptance

After the four feature lanes merged, Agent 50 owned real composition/wiring, presentation state and cross-layer acceptance. v0.1 is merged.

The historical merge shape was:

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
                     v0.1
```

## 3. Permanent architecture/file ownership

The historical lane split remains the normal ownership guide:

```text
platform/apps/**  -> Android discovery/launch adapters
domain/**         -> pure categorization/search/report logic
rules/**          -> deterministic bundled rules
model/**          -> app-owned shared models
<data>/**         -> persisted state/repository adapters
ui/**             -> Compose presentation/UI-state mapping/design system
```

Build/toolchain policy remains centrally owned and effectively frozen during feature work.

Rules:

- Android types do not enter model/domain contracts;
- DataStore/serialization types do not enter repository/domain contracts;
- Compose/Material types do not enter data/domain contracts;
- user-owned state is explicitly versioned;
- no generic `AndroidManager`, `DataManager` or service-locator container.

## 4. Post-v0.1 classification wave — implementation merged

Owner testing after v0.1 produced a private 566-target Samsung report with 225 `Unsorted` and 129 broad `Games` entries. Classification-quality work used a new evidence-first gate rather than reopening v0.1 lanes.

### Agent 60 — classification foundation / evidence — merged PR #9

Agent 60 froze:

- local classification reporting and aggregate output;
- additive Web/game taxonomy;
- exact-component > exact-package > package-prefix selector contract;
- shared selector/index/composition implementation;
- separate general/game/Web rule packs;
- raw owner-device report privacy rules.

The outer precedence remained user override > bundled rule > Android category > `Unsorted`.

### Parallel classification rule lanes — merged

After PR #9 merged, the rule lanes worked from the same foundation:

| PR | Lane | Owned production surface |
| --- | --- | --- |
| #12 | General rules | `rules/general/GeneralKnownAppRules.kt` |
| #13 | Game rules | `rules/games/GameKnownAppRules.kt` |
| #10 | Web/PWA rules | `rules/web/WebShortcutKnownAppRules.kt` |

They did not independently modify selector infrastructure, taxonomy or persistence.

The game lane uses only the five frozen narrow buckets and retains `Games` fallback. The Web lane uses only the evidence-backed Chromium WebAPK namespace and does not infer arbitrary TWA/browser entries.

### Agent 64 / PR #11 — triage / explanation UI — merged

The UI lane carried the real `ClassificationSource` through the presentation mapper, added source explanation and reused the existing category picker as a direct correction affordance for automatic `Unsorted` fallback.

No classifier/persistence/platform dependency was duplicated in UI.

## 5. Agent 70 — classification-quality integration / acceptance

Agent 70 starts only after PRs #12, #13, #10 and #11 are merged to `main`.

Branch:

```text
integration/classification-quality
```

Agent 70 owns only genuine integration/acceptance work:

- prove all rule packs compose through the shared `KnownAppRuleSet`;
- re-prove outer precedence across merged packs;
- prove additive taxonomy remains compatible with pre-wave schema-v1 state;
- verify UI presentation uses actual `ClassificationSource`;
- tighten permanent quality coverage when a real gap is found;
- record integrated sanitized evidence and migration decisions;
- update authoritative planning docs;
- define the next coherent wave;
- leave the PR unmerged.

Agent 70 must not use the integration label to redesign the classifier or add speculative features.

### Agent 70 quality correction

PR #11 added useful Compose instrumentation tests under `androidTest`, but the existing CI lane did not compile that source set. Agent 70 therefore adds `:app:assembleDebugAndroidTest` to the permanent verification and configuration-cache reuse commands.

This is intentionally a compile gate, not a false claim of emulator/device execution. Instrumentation behavior and Samsung-specific acceptance remain physical/device work.

### Agent 70 device gate

Repository integration can be completed remotely, but classification acceptance is not complete until the owner-device pass:

1. builds/downloads the current signed debug APK;
2. installs it over the existing app without clearing state;
3. verifies category overrides, favourites and hidden state survive;
4. verifies expanded taxonomy/search/move/explanation/triage UI;
5. verifies launch, back/dismiss and rescan behavior;
6. generates a fresh report from the same Samsung device;
7. records only sanitized aggregate counts/generalized false-positive findings.

The deterministic combined projection against the original frozen 566-target report is recorded in `CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`; it must not be misrepresented as a fresh device capture.

### Classification merge shape

```text
60 Foundation / evidence
          |
          +----------------+----------------+
          |                |                |
     12 General       13 Games         10 Web/PWA
          |                |                |
          +----------------+----------------+
                           |
                    11 Triage/UI
                           |
                    70 Integration
                           |
                 owner Samsung acceptance
                           |
                    owner merge decision
```

The exact historical merge ordering of #10/#11/#12/#13 is less important than the gate: Agent 70 starts from current `main` only after all required inputs are merged.

## 6. Quality gates by responsibility

### Pure domain/rules

- deterministic unit tests;
- duplicate/conflicting selector failures;
- precedence tests;
- no Android framework dependency;
- no label/network inference.

### Persistence/repository

- explicit schema version;
- override/favourite/hidden survival;
- migration tests for any schema/category-identity change;
- deterministic stale/uninstall behavior.

### UI

- UI-state mapping derives from app-owned state;
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

Persisted-state/category-identity changes are especially coordinated: category rename/removal/custom identity/order are migration work, not incidental UI changes.

## 8. Recommended next parallel wave — user-owned category management

After Agent 70 is accepted, the next wave should focus on user-owned organization rather than more aggressive automatic classification.

Recommended sequencing:

```text
80 Category identity/persistence foundation
                 |
        +--------+--------+
        |                 |
81 Category domain     82 Category UI
        |                 |
        +--------+--------+
                 |
        83 Integration/acceptance
```

### Agent 80 — custom-category/persistence foundation

Owns the smallest shared contract required for:

- durable custom-category identifiers;
- category metadata/order representation;
- persisted schema evolution;
- migration tests from current schema v1;
- no UI feature implementation beyond contract proof.

This must merge before parallel domain/UI lanes if shared models/schema change.

### Agent 81 — category management domain/repository

Potential ownership:

- create/rename/delete behavior;
- deletion reassignment/fallback policy;
- reorder operations;
- repository/state behavior;
- pure tests.

### Agent 82 — category management UI

Potential ownership:

- create/rename/delete/reorder UI;
- richer category-management surface;
- bulk/manual organization only where the frozen domain contract supports it;
- Compose tests and accessibility.

### Agent 83 — integration / acceptance

Owns cross-layer wiring, migration/device acceptance and any genuine integration repair.

Do **not** fold local backup/export/import into the foundation merely because persistence is changing. Define the custom-category representation first, then version the export format against a stable contract. Likewise, dynamic/pinned shortcuts should follow stable category identity.

Presentation polish may later be split into a low-conflict UI lane. Performance/benchmark work should only become a lane after measurements identify a meaningful target.

## 9. PR control

Green CI is necessary but not sufficient. A PR is merge-ready only when its lane-specific acceptance is satisfied and all unresolved device/migration steps are visible.

No coding/integration agent merges its own PR unless the owner explicitly instructs it to do so.
