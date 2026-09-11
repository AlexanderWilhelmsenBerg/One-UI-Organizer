# MoSCoW Scope Analysis

This document separates the shipped core from the highest-value follow-up work. v0.1 is merged; classification-quality implementation is merged and is passing through Agent 70 integration/owner-device acceptance.

Execution sequencing is defined in [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md). The current integrated classification result is in [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md).

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

- deterministic local classification;
- `Unsorted` remains usable and launchable;
- `Games` remains a valid broad fallback;
- rule-pack changes never erase user corrections;
- no label guessing, cloud classifier or runtime network lookup.

### Manual correction and organization

- Change an app's primary category.
- Persist the user correction locally.
- Keep user correction authoritative over later rule changes.
- Favourite/unfavourite.
- Hide and restore hidden apps.
- Search by app and category name.
- Launch the exact selected target.

### Privacy / minimalism

- No account.
- No analytics or ads.
- No cloud service.
- No `INTERNET` permission.
- No `QUERY_ALL_PACKAGES`.
- No unnecessary background service.

### Engineering baseline

- Exact stable-compatible versions come from `STABLE_BASELINE.md`.
- Reproducible Gradle daemon JDK and explicit Android Java/JVM toolchain.
- Central version catalog and dependency verification.
- Warning-free compiler/Gradle/Lint/ktlint baseline.
- Dependency-health and configuration-cache gates.
- App-owned cross-layer contracts.
- Explicit persisted schema version.
- No relied-on transitive dependencies.

### Testing / acceptance

- Clean build from checkout.
- Pure Kotlin category/search/state tests.
- Compose semantics/interaction tests for critical UI behavior.
- Instrumented/system tests where Android behavior requires them.
- Physical Samsung acceptance for One UI/platform-critical behavior.
- Persisted-state changes include migration tests.
- Performance is measured before optimization.

## Completed post-v0.1 Should work

### Classification quality and taxonomy tuning — implemented

The classification wave delivered:

- evidence-driven general known-app expansion using exact package identity;
- a narrow deterministic `Web Shortcuts` rule for Chromium WebAPK packages under `org.chromium.webapk.`;
- five broad game subcategories while preserving `Games` fallback;
- `Unsorted` triage without forcing uncertain matches;
- classification explanation sourced from the real `ClassificationSource`;
- local explicit classification-report sharing for diagnostic review;
- no taxonomy rename/removal and therefore no organizer-state schema migration.

Against the same frozen 566-target evidence set, the combined projection changes:

- `Unsorted`: 225 -> 124;
- `Games`: 129 -> 23;
- bundled known-rule source: 5 -> 235.

A fresh same-device capture is still part of Agent 70 physical acceptance; these numbers are the deterministic projection against the original evidence rather than a fabricated new device run.

### Better `Unsorted` management / rule explanation — implemented baseline

The app now distinguishes automatic fallback from deliberate user overrides and gives automatic `Unsorted` entries a direct correction affordance. Explanation labels cover user override, bundled rule, Android category and fallback.

A larger dedicated/bulk management surface remains a follow-up opportunity rather than unfinished baseline work.

## Should have — next high-value work

### Custom categories and category order

- Create custom user categories.
- Rename custom categories.
- Delete custom categories with an explicit reassignment/fallback policy.
- Persist stable app-owned custom-category identifiers.
- Reorder categories and persist that order.
- Choose category presentation metadata/icons from a bundled safe set if useful.
- Provide migration tests for the required persisted-state evolution.

This is the recommended next coherent wave because automatic classification is now materially better and the next product leverage comes from user-owned organization rather than increasingly aggressive automatic guesses.

### Richer category management

- Dedicated category-management surface.
- Efficient bulk/manual moves where UX evidence justifies them.
- Better visibility into category order and custom-category lifecycle.
- Preserve the simple one-primary-category model unless scope is explicitly changed.

### Local backup / portability

- Export organizer-owned state to a local file.
- Import a previously exported local file.
- Version the export format from its first release.
- Preserve custom category identity/order once those contracts are stable.

Backup/import should follow the custom-category persistence contract rather than freezing an export format just before the schema changes.

### Shortcuts

- Android dynamic shortcuts for useful categories.
- Request a pinned home-screen shortcut for a selected category.

Shortcuts should follow stable category identity so persisted/pinned destinations do not depend on fragile display names.

### Presentation polish

- More complete One UI-inspired spacing/motion tokens.
- Smoother transition between home and Organizer.
- Optional blur only where reliable.
- Layout refinements only when they preserve accessibility and device behavior.

### Performance hardening

- Add benchmark/profile infrastructure only when measured regressions or stable historical baselines justify it.
- Baseline profiles only when the stable plugin line cleanly supports the selected toolchain.

## Could have

### Home-screen widget

- Glance widget for favourites or selected categories.
- Configurable compact layouts.

### Multiple tags/categories

- Allow secondary tags while retaining one primary category.
- Only after the single-category custom-category model is stable.

### Usage-based suggestions

- Explicit opt-in only.
- Local processing only.
- Requires a separately justified permission/product decision.

### Work-profile support

- Distinguish personal/work launch targets and state correctly by profile.

### Better search

- Fuzzy matching.
- Acronym/initial matching.
- User-defined aliases.

### Rule tooling

- User-editable local rules.
- Import local/community rule packs from a file.
- More detailed matched-rule explanation if useful.

### Large-screen/foldable adaptation

- Adaptive layouts when actual use warrants them.

## Won't have — unless scope is explicitly reopened

### Native One UI manipulation

- Rewriting Samsung's app-drawer order.
- Creating/editing Samsung launcher folders programmatically.
- Writing Samsung launcher databases.
- Undocumented Samsung launcher APIs.

### Launcher replacement

- Becoming default HOME.
- Reimplementing launcher widgets, wallpaper, gestures, recents or home screen.

### Cloud / AI classification

- Server-side classification.
- LLM classification.
- Accounts/sync backend.
- Remote analytics/telemetry.

### Broad package access for convenience

- `QUERY_ALL_PACKAGES` without a later explicit requirement and policy review.

### Always-on monitoring

- Permanent foreground/background service merely to keep the app list current.

### Cross-platform implementation

- iOS/desktop port or cross-platform framework for hypothetical portability.

## Priority interpretation

When new work appears:

1. correctness/state-preservation/privacy regressions are blockers;
2. user-owned organization comes before more aggressive automatic guessing;
3. persisted-state work defines migration contracts before UI spreads the representation;
4. backup and shortcuts follow stable category identity;
5. performance work follows measurements;
6. launcher replacement, cloud services, unsupported Samsung internals and speculative infrastructure remain out of scope;
7. coding agents leave merge decisions to the owner unless explicitly instructed otherwise.
