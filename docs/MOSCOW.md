# MoSCoW Scope Analysis

This document separates the shipped core from the highest-value follow-up work. v0.1, classification quality through PR #14, category management through PR #18, local backup/import PR #19 and category shortcuts PR #20 are merged. Agent 92 integration is complete on PR #21 with green permanent CI and signed-debug build; the owner intends to merge it, while physical Samsung / One UI observations remain an explicit acceptance record. The recommended following wave is Agents 100–102: supported metadata enrichment and presentation polish in parallel, followed by integration/acceptance.

Execution sequencing is defined in [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md). Classification acceptance is recorded in [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md); category-management acceptance is recorded in [`CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md`](CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md).

## Must have — current product contract

### Companion behavior

- One UI Home remains the default launcher.
- Organizer is a separate companion activity/shelf.
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
4. `Unsorted`.

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

The current Agent-92 baseline has no `INTERNET` permission. Agent 100 may add `INTERNET` only together with the supported metadata provider implementation and its documented privacy/cache policy.

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

### Local backup + category shortcuts — implementation merged, Agent 92 integration ready

PR #19 delivered versioned local export/import using user-selected Android documents and atomic validated replacement. PR #20 delivered dynamic and user-requested pinned category shortcuts using stable `CategoryId`. Agent 92 integrates both on PR #21 using the same authoritative `OrganizerStateStore`; permanent CI and the signed-debug build are green. Owner merge is pending, and physical One UI observations remain to be recorded.

## Should have — recommended following wave

### Agent 100 — supported metadata enrichment

Internet-backed metadata is now the next classification-quality candidate, but only where it materially improves unresolved entries. The first rollout must:

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

F-Droid's documented indexes/metadata are the preferred first candidate for the subset of apps they cover.

### Agent 101 — presentation polish

Run in parallel with Agent 100 while staying presentation-only:

- stronger One UI-inspired hierarchy, spacing and motion;
- clearer shelf/category-management/Backup & restore navigation and action hierarchy;
- improved loading, empty, error, confirmation and shortcut-pin feedback states;
- large-font/scaled-text, touch-target, semantics and contrast review;
- smoother transition/back/dismiss behavior;
- optional blur only where reliable.

No repository/schema/classifier/network/toolchain redesign belongs in this lane.

### Agent 102 — integration / acceptance

After Agents 100 and 101 are individually green, merge Agent 100 first, rebase/merge Agent 101, then integrate from current `main`. Agent 102 must prove no regression in existing classifications/state/features, record exactly which formerly `Unsorted` entries improve, audit false positives, verify offline behavior, run full CI, perform Samsung acceptance and re-audit permissions/privacy.

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
