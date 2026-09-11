# Category Management Integration Acceptance

This document records the integrated user-owned category-management design and the acceptance gate for the Agent 80–83 wave.

## Status

Agents 80, 81 and 82 are merged to `main` as PRs #15, #16 and #17. Agent 83 integrates those lanes in PR #18 on `integration/category-management`.

The owner-device lifecycle/migration exercise passed all requested behavior except the category-assignment count shown by **Manage categories**. The first correction changed the management mapper from persisted overrides to the live classified-app stream, but a signed-debug retest from exact head `f642b570...` still showed only explicit/manual assignments: built-in automatic categories remained at zero, while `Video` showed the previously moved app and custom test categories showed their manual assignments. That first correction is therefore not considered physically validated.

The follow-up hardening removes the parallel count pipeline. Effective category-assignment counts are now computed once in the same unfiltered organizer inventory mapping that powers the shelf and are then consumed by category management. Automated code acceptance is green on hardened implementation head `7af810328fb89a91fffd2762b8e5fdf4cc105364` in CI run #253; the documentation-only acceptance-record commit is subject to the same final CI gate. PR #18 remains non-merge-ready until this hardened count path passes the targeted Samsung retest.

## Final category model

Category identity is app-owned and independent of presentation labels.

- Built-in categories remain `AppCategory` values with explicit durable `builtin:*` `CategoryId` values.
- User-created categories are `CustomCategoryDefinition` values with durable `custom:<opaque-id>` identities.
- The custom ID is generated once at creation and is never derived from the display name.
- Rename changes display metadata only and preserves the ID, overrides, favourites, hidden state and category order.
- `CategoryDefinition` is the shared effective-category contract used by categorization, search, reporting and presentation.
- `Favourites` remains a virtual section and is not a persisted normal category.

## Persistence and migration

Organizer state schema is version 2. It persists:

- app-to-category override IDs;
- favourites;
- hidden app IDs;
- custom-category definitions;
- normal-category order.

Literal schema-v1 state is supported. V1 enum category overrides map deterministically to the corresponding explicit built-in IDs; favourites and hidden state survive; custom definitions start empty; the initial migrated order is the frozen built-in order; the next write uses schema v2.

Effective order normalization preserves the first valid persisted occurrence, drops stale/duplicate IDs, appends missing built-ins in default order and then appends missing custom definitions in definition order.

The category-management mutation boundary is stricter than persisted-state normalization: reorder requests must be an exact permutation of all current normal built-in and custom category IDs.

## Lifecycle behavior

The integrated application exposes these repository-owned lifecycle operations:

- create a custom category using the app-owned name policy and one opaque generated ID;
- rename a custom category without changing its identity;
- reorder built-in and custom normal categories;
- delete a custom category and explicitly reassign affected overrides to another current category; or
- delete a custom category and remove affected overrides so automatic classification becomes effective again.

Built-in categories cannot be renamed or deleted.

Deletion, reassignment/override clearing, definition removal and order removal are one persisted transaction. Favourite and hidden state are preserved.

## Application wiring

`OneUiOrganizerApplication` creates one `DefaultOrganizerRepository` instance and exposes it through the separate app-owned `OrganizerRepository` and `CategoryManagementRepository` contracts. No second category-state owner is created.

`OrganizerViewModel` translates presentation intents to those repository contracts. It does not repeat name validation, lifecycle rules or persistence behavior. Adjacent UI reorder actions are translated to the current complete ordered-ID permutation and the repository performs final validation.

The organizer presentation now owns one shared unfiltered category-assignment count map:

- definitions/order still come from `orderedCategories()`;
- the same effective `CategorizedApp` inventory that creates shelf sections also creates `categoryAssignmentCounts` before search or hidden-app filtering;
- every currently installed launch target contributes to its effective category, regardless of whether that category came from a user override, bundled rule, Android category or unsorted fallback;
- hidden installed apps therefore remain counted even though they do not appear in ordinary shelf sections;
- a retained override for an app that is not currently installed contributes one retained assignment;
- an installed overridden app is counted from effective inventory and is not counted again from persisted override state;
- category management consumes this shared count map rather than independently re-deriving counts from another live-flow pipeline;
- the shelf and move picker continue to consume the same app-owned definitions/order;
- category-management and hidden-app management are mutually exclusive surfaces;
- Android back/dismiss returns from category management to the organizer shelf.

This shared path is intentional: if the organizer shelf has effective app membership, category management receives counts from the same mapped inventory rather than maintaining a second interpretation of that membership.

## Classification, search and diagnostics

Classification precedence remains:

1. user override;
2. bundled known-app rule;
3. Android-declared category;
4. `Unsorted`.

Custom overrides use `ClassificationSource.USER_OVERRIDE` exactly like built-in overrides. Clearing an override, including delete-to-automatic, exposes the automatic category/source that would otherwise have applied.

Local search uses the resolved effective category display name, so custom-category names are searchable without a second index or network access.

The local classification report distinguishes both pieces of information required for diagnostics:

- the effective custom category identity and display name; and
- the independent classification source, including `USER_OVERRIDE`.

No raw installed-app inventory is committed as acceptance evidence.

## Failure behavior

Lifecycle validation/persistence failures remain app-owned results. Raw storage exceptions are not exposed to presentation.

The integrated presentation maps failures to safe messages for blank/too-long/duplicate names, missing categories, immutable built-ins, invalid reassignment destinations, invalid order and persistence failure. A failed persisted mutation does not update the persisted category state.

## Automated acceptance

The permanent PR quality lane passed on hardened implementation head `7af810328fb89a91fffd2762b8e5fdf4cc105364` in CI run #253. Any later documentation-only acceptance-record commit must also retain a green final CI result before merge readiness.

It covers:

- debug app assembly;
- instrumentation-test APK compilation;
- JVM tests, including migration/category lifecycle/integration regressions;
- Android Lint;
- ktlint;
- dependency `buildHealth`;
- strict dependency verification;
- warning-mode failure;
- configuration-cache creation and reuse;
- forbidden-permission checks.

Repository tests additionally prove stable rename identity, explicit deletion policies, invalid-destination atomicity, exact reorder validation, custom-category search, persistence recreation, schema-v1 migration, classification precedence and custom diagnostic-report representation.

The hardened count regression set specifically proves:

- automatic effective category membership contributes to the shared assignment-count map;
- hidden current apps remain counted;
- retained uninstalled overrides remain counted;
- current overridden apps are not double-counted;
- search filtering does not change the shared assignment counts; and
- the ViewModel exposes the same effective count to category management that the shelf inventory contains.

## Physical Samsung migration gate — targeted retest pending

Use the repository `Build APK` workflow with the **debug** variant on the Agent 83 branch. That workflow uses the permanent distribution keystore, verifies the signature and publishes the signed debug APK. Install it over the existing application; do not clear data.

Before the original upgrade exercise, representative existing state was retained for migration verification:

- one manual built-in category override;
- one favourite;
- one hidden app.

### Recorded owner-device evidence — 2026-09-11

A manual move of one previously automatically classified/unsorted app into the built-in `Video` category was exercised on the primary Samsung device and a fresh local classification report was reviewed.

Sanitized evidence:

- total launch-target inventory remained **566**;
- category totals still summed to 566;
- classification-source totals still summed to 566;
- `Video` increased by one while `Unsorted` decreased by one relative to the accepted pre-override baseline;
- the report showed exactly one `USER_OVERRIDE`;
- the moved target's effective category was `VIDEO` with classification source `USER_OVERRIDE`;
- bundled-rule and Android-declared source counts remained unchanged at 249 and 194 respectively;
- `UNSORTED_FALLBACK` decreased to 122, matching the single manual override.

This proves the real-device manual-override path and diagnostic-report source attribution without changing target inventory.

The owner subsequently completed the requested migration/category-management lifecycle exercise and reported all other requested behaviors as passing, including preservation through the over-install, custom-category creation/movement/search/rename/reorder, process recreation, both deletion policies, built-in protection, representative launch behavior, back/dismiss behavior and layout sanity.

### Count defect — first finding

Opening **Manage categories** showed `0 assigned apps` for built-in categories even though those categories contained automatically classified apps. Newly created test categories showed nonzero counts because their membership came from explicit user overrides. Inspection found that the initial management mapper counted only `OrganizerState.categoryOverrides`.

### Count defect — first correction retest failed

The first correction changed that mapper to count `CategorizedApp.category` directly and retained uninstalled overrides separately. Automated CI passed, and Build APK run #14 produced `OneUIOrganizer-debug-f642b57.apk` from exact head `f642b57015002ed563c6496ccf5bf1f8d324b620`.

The signed-debug physical retest still failed in the same user-visible way:

- automatic built-in categories remained at `0 assigned apps`;
- `Video` showed one assignment, consistent with the previously moved app;
- manually populated custom/test categories showed assignments.

Because the exact corrected artifact was exercised, the first correction is recorded as insufficient rather than as an artifact-selection failure.

### Hardened correction

The follow-up correction removes the independent management count derivation. `OrganizerUiStateMapper` now computes one unfiltered `categoryAssignmentCounts` map from the same effective inventory used to build shelf sections, adds only retained overrides for currently absent apps, and exposes that map in `OrganizerShelfUiState`. `OrganizerViewModel` then supplies that already-mapped count set to category management.

This deliberately ties the count shown in **Manage categories** to the same presentation inventory that proves category membership on the shelf.

### Required targeted retest after the hardened fix

Build a new signed debug APK from the current `integration/category-management` head, install it over the current test installation without clearing data, open **Manage categories**, and verify:

1. populated automatic built-in categories show nonzero counts consistent with their effective shelf membership;
2. `Video` still includes the previously exercised manual override in its count;
3. custom/test categories still show their expected assignment counts;
4. the previously exercised user override remains present after the over-install;
5. no obvious category-management layout or interaction regression is introduced.

No repetition of the full lifecycle/migration checklist is required unless one of those checks fails.

## Permission/privacy gate

Category management requires no network or broad package permission. The integrated manifest continues to omit both `android.permission.INTERNET` and `android.permission.QUERY_ALL_PACKAGES` and does not add analytics, telemetry, accounts or background services.

## Completion rule

The Agent 80–83 wave is complete only when:

1. PR #18 final automated CI is green on the current head; and
2. the targeted physical count retest above passes and its generalized result is recorded.

Until both are true, keep PR #18 unmerged and keep the category-management wave marked as pending physical acceptance.

## Recommended next wave

After this wave is accepted, the strongest parallel candidates are:

- local versioned backup/export/import of organizer-owned state; and
- dynamic/pinned category shortcuts.

Both can consume the now-stable category IDs without owning or redefining category lifecycle behavior. Optional F-Droid metadata enrichment and presentation polish remain independent later candidates. Performance work remains measurement-driven.
