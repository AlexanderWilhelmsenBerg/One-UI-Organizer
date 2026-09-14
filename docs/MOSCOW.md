# MoSCoW Scope Analysis

This document separates the shipped core from the highest-value follow-up work. v0.1, classification quality through PR #14, category management through PR #18, portability/shortcuts through accepted PR #21, presentation/navigation PR #22 and supported-metadata PR #23 are merged to `main`. Agent 102 is the current integration/acceptance lane for the combined metadata + two-destination presentation; automated CI and physical Samsung online/offline acceptance remain the final gate.

Execution sequencing is defined in [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md). Classification acceptance is recorded in [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md); category-management acceptance is recorded in [`CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md`](CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md).

## Must have — current product contract

### Companion behavior

- One UI Home remains the default launcher.
- Organizer is a separate companion activity/shelf.
- Primary application navigation contains exactly `Organizer` and `Categories`.
- Phones use a One UI-style bottom navigation treatment; Backup & restore, diagnostics and settings remain contextual rather than additional primary destinations.
- Feature screens remain independent of the primary-navigation chrome so a future large-screen/foldable shell can use a navigation rail without rewriting Organizer/Categories.
- Dismiss/back returns to the existing home experience.

### Supported app discovery

- Discover normal launcher activities through supported Android APIs.
- Do not request `QUERY_ALL_PACKAGES`.
- Model package/component identity deterministically, including aliases/multiple launcher activities.
- Rescan on open/resume without a persistent service.

### Automatic categorization

Classification precedence remains:

1. user override;
2. bundled known-app rule;
3. Android-declared category mapping;
4. supported metadata when explicitly mapped to an existing built-in category;
5. `Unsorted`.

Requirements:

- deterministic local baseline classification;
- `Unsorted` remains usable and launchable;
- `Games` remains a valid broad fallback;
- `Emulators` is separate from game genres;
- automatic rule packs classify only into built-in categories;
- user overrides may target a built-in or user-created category;
- rule-pack changes never erase user corrections;
- no display-label guessing or probabilistic classification in the baseline;
- ambiguous modified/repacked identities prefer exact-component evidence or a broad fallback over a narrow false positive.

### Manual correction and organization

- Change an app's primary category.
- Persist the user correction locally.
- Keep user correction authoritative over later rule changes.
- Favourite/unfavourite.
- Hide and restore hidden apps.
- Search by app and category name.
- Launch the exact selected target.
- Create and rename custom categories with stable identity.
- Reorder built-in and custom categories.
- Delete custom categories only with an explicit reassignment or return-to-automatic choice.
- Keep built-in categories protected from rename/delete.

### Category identity and persistence

The schema-v2 category contract is the current integrated model:

- built-in categories have explicit durable IDs independent of enum names and display labels;
- user-created categories use durable app-owned `custom:<opaque-id>` identities independent of display name;
- one app still has one primary category;
- `Favourites` remains a virtual section, not a persisted normal category;
- custom category definitions and category order are persisted locally;
- existing schema-v1 overrides, favourites and hidden state migrate without loss;
- order normalization is deterministic for duplicates, stale IDs and missing/new categories;
- lifecycle reorder accepts only an exact permutation of current normal categories;
- supported old state must not be treated as corruption merely because the schema version increased.

### Privacy / minimalism

- No account.
- No analytics or ads.
- No cloud classification service.
- No `QUERY_ALL_PACKAGES`.
- No unnecessary background service.
- Network access, if introduced, must serve an explicit supported feature and remain behind an app-owned boundary.

The current app declares `INTERNET` only for the merged Agent-100 / PR-23 F-Droid metadata provider and its documented privacy/cache policy. `QUERY_ALL_PACKAGES`, broad storage permissions, telemetry, accounts/cloud sync and background services remain absent.

### Engineering baseline

- Exact stable-compatible versions come from `STABLE_BASELINE.md`.
- Reproducible Gradle daemon JDK and explicit Android Java/JVM toolchain.
- Central version catalog and dependency verification.
- Warning-free compiler/Gradle/Lint/ktlint baseline.
- Dependency-health and configuration-cache gates.
- App-owned cross-layer contracts.
- Explicit persisted schema version and real migrations.
- No relied-on transitive dependencies.

### Testing / acceptance

- Clean build from checkout.
- Pure Kotlin category/search/state tests.
- Literal old-schema migration fixtures for persisted-state evolution.
- Compose semantics/interaction tests for critical UI behavior.
- Instrumented/system tests where Android behavior requires them.
- Physical Samsung acceptance for One UI/platform-critical behavior.
- Physical over-install migration proof for persisted schema/category waves.
- Online/offline physical acceptance when supported metadata is introduced.
- Performance is measured before optimization.

## Completed post-v0.1 work

### Classification quality and taxonomy tuning — complete and merged

The classification wave delivered evidence-driven general rules, narrow Chromium WebAPK classification, five game genres plus broad `Games`, an evidence-backed `Emulators` category, `Unsorted` triage, real `ClassificationSource` explanation and local diagnostic reporting.

Agent 70 / PR #14 repaired the Eden/Yuzu package ambiguity using exact-component evidence and completed owner Samsung acceptance. The accepted 566-target report contains 123 `Unsorted`, 15 `Emulators`, 10 broad `Games`, 37 RPG, 249 bundled-rule and 194 Android-declared classifications. PR #14 is merged.

### User-owned category management — complete and merged

Agents 80–83 / PRs #15–#18 delivered durable built-in/custom category identity, schema-v2 migration, lifecycle/reorder repository behavior, Compose management UI, real-app integration and Samsung migration/count acceptance. Later features consume this state/identity contract rather than redefining it.

### Local backup + category shortcuts — complete, merged and Samsung-accepted

PR #19 delivered versioned local export/import using user-selected Android documents and atomic validated replacement. PR #20 delivered dynamic and user-requested pinned category shortcuts using stable `CategoryId`. Agent 92 integrated both in PR #21 using the same authoritative `OrganizerStateStore`; permanent CI, signed-debug build and owner Samsung / One UI acceptance are complete. Agent 102 treats this as the accepted regression baseline.

## Current integration wave

### Agent 100 — supported metadata enrichment — merged in PR #23

Internet-backed metadata is implemented for the subset where it materially improves unresolved entries. The merged first rollout:

- use a supported/documented source rather than brittle Google Play scraping;
- define an app-owned metadata/provider contract;
- keep network/provider types at the adapter boundary;
- use a bounded local cache and make lookup best-effort/non-blocking;
- map provider categories/tags explicitly into existing built-ins;
- preserve first-rollout precedence as user override > bundled rule > Android-declared category > supported metadata > `Unsorted`;
- therefore only resolve packages that would otherwise remain `Unsorted`;
- expose metadata-derived classification distinctly when it decides category;
- add `INTERNET` only in the provider implementation PR and document privacy/cache behavior;
- prove no-network/provider failure leaves the local shelf and launch flow fully usable.

F-Droid's documented index v2 is the implemented first provider for the subset of apps it covers.

### Agent 101 — presentation polish / primary navigation — merged in PR #22

The merged presentation lane remains presentation-only:

- exactly two primary destinations: `Organizer` and `Categories`;
- One UI-style bottom navigation on phones with an adaptable shell boundary for later rail/large-screen treatment;
- stronger One UI-inspired hierarchy, spacing and motion;
- clearer shelf/category-management/Backup & restore navigation and action hierarchy;
- improved loading, empty, error, confirmation and shortcut-pin feedback states;
- large-font/scaled-text, touch-target, semantics and contrast review;
- smoother transition/back/dismiss behavior;
- optional blur only where reliable.

No repository/schema/classifier/network/toolchain redesign belongs in this lane.

### Agent 102 — metadata + presentation integration / acceptance — current

PR #22 and PR #23 are already merged on current `main`; actual merge history is authoritative even though the earlier plan preferred metadata first. Agent 102 integrates from that real combined baseline and must prove no regression in existing classifications/state/features, record exactly which formerly `Unsorted` entries improve, audit false positives, verify offline behavior, run full CI, perform Samsung acceptance and re-audit permissions/privacy. The lane does not replay or reorder already-merged history.

### Performance hardening — evidence-triggered only

Do not schedule performance work merely because the feature set is larger. Add benchmark/profile infrastructure only when Agent-102 measurements or later evidence identify a concrete regression or stable target. Baseline profiles remain conditional on the stable plugin/toolchain line.

## Could have

- Glance widget for favourites or selected categories.
- Secondary tags/categories only after the one-primary-category custom model is stable.
- Explicit opt-in local usage-based suggestions only after a separate permission/product decision.
- Work-profile-aware identity/state.
- Fuzzy/acronym/alias search.
- User-editable local rules or file-imported rule packs.
- More detailed matched-rule explanation.
- Large-screen/foldable adaptation when actual use warrants it.

## Won't have — unless scope is explicitly reopened

- rewriting Samsung's app-drawer order or launcher folders;
- writing Samsung launcher databases or using undocumented Samsung launcher APIs;
- becoming default HOME or reimplementing launcher responsibilities;
- cloud/LLM classification, accounts/sync backend, analytics or ads;
- unofficial brittle Google Play scraping as a production dependency;
- `QUERY_ALL_PACKAGES` for convenience;
- permanent foreground/background monitoring merely to keep the app list current;
- speculative cross-platform implementation.

## Priority interpretation

When new work appears:

1. correctness/state-preservation/privacy regressions are blockers;
2. low false-positive risk outranks maximizing narrow classification counts;
3. user-owned organization comes before more aggressive automatic guessing;
4. persisted-state work freezes identity/migration contracts before consumers spread them;
5. one repository/app-owned state remains the source of category behavior; UI does not create a second lifecycle model;
6. backup and shortcuts consume stable category identity rather than redefine it;
7. supported metadata enrichment may complement local rules but does not replace deterministic/manual precedence;
8. performance work follows measurements;
9. launcher replacement, unsupported Samsung internals and speculative infrastructure remain out of scope;
10. coding/integration agents leave merge decisions to the owner unless explicitly instructed otherwise.
