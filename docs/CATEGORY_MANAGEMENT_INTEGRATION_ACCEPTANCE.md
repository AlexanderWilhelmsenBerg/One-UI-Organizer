# Category Management Integration Acceptance

This document records the integrated user-owned category-management design and the acceptance gate for the Agent 80–83 wave.

## Status

Agents 80, 81 and 82 are merged to `main` as PRs #15, #16 and #17. Agent 83 integrates those lanes in PR #18 on `integration/category-management`.

The implementation is not considered fully accepted, and PR #18 is not merge-ready, until the physical Samsung upgrade/migration checklist in this document has been completed without clearing application data and the generalized result has been recorded. The final PR CI is the authoritative automated-quality result.

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

The category-management presentation is derived from persisted `OrganizerState`:

- definitions/order come from `orderedCategories()`;
- assigned counts come from persisted override IDs, including retained overrides for hidden or currently uninstalled apps;
- the shelf and move picker continue to consume the same app-owned definitions/order;
- category-management and hidden-app management are mutually exclusive surfaces;
- Android back/dismiss returns from category management to the organizer shelf.

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

The permanent PR quality lane is the automated gate and must be green on the final PR head. It covers:

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

## Physical Samsung migration gate — pending until recorded

Use the repository `Build APK` workflow with the **debug** variant on the Agent 83 branch. That workflow uses the permanent distribution keystore, verifies the signature and publishes the signed debug APK. Install it over the existing pre-category-management application; do not clear data.

Before upgrade, retain representative existing state:

- one manual built-in category override;
- one favourite;
- one hidden app.

After upgrade verify, using only generalized/sanitized evidence:

1. installation succeeds over the existing app without clearing data;
2. the previous built-in override survives;
3. the previous favourite survives;
4. the previous hidden state survives;
5. at least two custom categories can be created;
6. several apps can be moved into them;
7. custom categories appear in the shelf and move picker;
8. search by a custom-category name returns assigned apps;
9. renaming a custom category preserves its assigned apps;
10. reordering changes shelf order;
11. force-stop/reopen preserves categories, order and assignments;
12. deleting one populated custom category can reassign its apps;
13. deleting another using return-to-automatic restores automatic classification;
14. built-in categories cannot be renamed or deleted;
15. representative apps still launch;
16. back/dismiss behavior is correct;
17. classification explanation/reporting still identifies user override correctly;
18. additional custom categories cause no obvious layout failure.

Also sanity-check organizer startup/scan against the existing first-use usability threshold. Do not add benchmark infrastructure unless a measured regression is observed.

## Permission/privacy gate

Category management requires no network or broad package permission. The integrated manifest must continue to omit both `android.permission.INTERNET` and `android.permission.QUERY_ALL_PACKAGES` and must not add analytics, telemetry, accounts or background services.

## Completion rule

The Agent 80–83 wave is complete only when:

1. PR #18 final automated CI is green; and
2. the physical Samsung migration gate above passes and its generalized result is recorded.

Until both are true, keep PR #18 unmerged and keep the category-management wave marked as pending physical acceptance.

## Recommended next wave

After this wave is accepted, the strongest parallel candidates are:

- local versioned backup/export/import of organizer-owned state; and
- dynamic/pinned category shortcuts.

Both can consume the now-stable category IDs without owning or redefining category lifecycle behavior. Optional F-Droid metadata enrichment and presentation polish remain independent later candidates. Performance work remains measurement-driven.
