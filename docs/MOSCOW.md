# MoSCoW Scope Analysis

This document separates the shipped core from the highest-value follow-up work. v0.1 and the classification-quality wave through Agent 70 / PR #14 are merged. Agents 80–82 of the user-owned category-management wave are merged as PRs #15–#17; Agent 83 integrates them in PR #18. The category-management wave remains pending the mandatory physical Samsung upgrade/migration acceptance before PR #18 can be considered merge-ready.

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

The category-management implementation still has no `INTERNET` permission. Optional metadata enrichment remains a future separate feature.

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
- Physical over-install migration proof for the category-management schema wave.
- Performance is measured before optimization.

## Completed post-v0.1 work

### Classification quality and taxonomy tuning — complete and merged

The classification wave delivered evidence-driven general rules, narrow Chromium WebAPK classification, five game genres plus broad `Games`, an evidence-backed `Emulators` category, `Unsorted` triage, real `ClassificationSource` explanation and local diagnostic reporting.

Agent 70 / PR #14 repaired the Eden/Yuzu package ambiguity using exact-component evidence and completed owner Samsung acceptance. The accepted 566-target report contains 123 `Unsorted`, 15 `Emulators`, 10 broad `Games`, 37 RPG, 249 bundled-rule and 194 Android-declared classifications. PR #14 is merged.

## Should have — current high-value wave

### Custom categories and category order — integrated, physical acceptance pending

Agent 80 / PR #15 delivered the identity/persistence foundation:

- durable built-in/custom category identities;
- schema-v2 migration from literal schema-v1 payloads;
- persisted custom definitions;
- persisted deterministic category order;
- shared category representation across repository/search/report/UI-state mapping.

Agent 81 / PR #16 delivered lifecycle/domain behavior:

- create custom categories and generate the opaque ID once;
- rename without changing identity;
- delete with explicit reassignment/automatic policy;
- strict reorder mutation;
- app-owned failures and atomic repository behavior;
- persistence/recreation regression tests.

Agent 82 / PR #17 delivered presentation:

- create/rename/delete/reorder UI;
- dedicated category-management surface;
- accessible Compose behavior/tests;
- no persistence or classifier duplication in UI.

Agent 83 / PR #18 owns integration/acceptance:

- one real repository instance through app-owned organizer/lifecycle contracts;
- real ViewModel/shelf/picker/management wiring;
- sanitized failure presentation;
- retained hidden/uninstalled override counts for safe deletion decisions;
- Android back/dismiss behavior;
- complete automated quality lane;
- physical over-install Samsung migration/behavior proof.

Do not mark this wave complete or merge PR #18 until the physical checklist in `CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md` passes.

### Local backup / portability — next candidate

- Export organizer-owned state to a local file.
- Import a previously exported local file.
- Version the export format from its first release.
- Preserve stable custom category identity/order.
- Validate imported state before replacing current state.

Backup consumes the accepted category lifecycle/identity contract and must not redefine it.

### Shortcuts — next candidate

- Android dynamic shortcuts for useful categories.
- Request a pinned home-screen shortcut for a selected category.
- Resolve shortcuts through stable category IDs so category rename does not break identity.

Shortcuts consume stable category identity and must not redefine it.

Backup and shortcuts are suitable for parallel development after Agent 83 acceptance because they consume the same stable IDs but own different behavior.

### Optional metadata enrichment

Internet-backed metadata may be used only when it materially improves classification and must:

- use a supported/documented source rather than brittle Google Play scraping;
- define an app-owned metadata/provider contract;
- keep network/provider types at the adapter boundary;
- cache locally and make lookup best-effort/non-blocking;
- map provider categories/tags explicitly into organizer built-ins;
- preserve user override and bundled-rule precedence;
- expose metadata-derived classification distinctly if it can decide category;
- add `INTERNET` only in the provider implementation PR.

F-Droid's documented metadata remains a plausible source for its subset of apps.

### Presentation polish

- More complete One UI-inspired spacing/motion tokens.
- Smoother transition between home and Organizer.
- Optional blur only where reliable.
- Layout refinements only when they preserve accessibility and device behavior.

### Performance hardening

- Add benchmark/profile infrastructure only when measured regressions or stable historical baselines justify it.
- Baseline profiles only when the stable plugin line cleanly supports the selected toolchain.

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
