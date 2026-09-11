# Category Management Domain Contract

This document records the category-management domain/repository behavior delivered by Agent 81 on the category identity/schema-v2 foundation from Agent 80, plus the integrated application contract established by Agent 83.

Detailed final acceptance, including the mandatory Samsung migration gate, is recorded in [`CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md`](CATEGORY_MANAGEMENT_INTEGRATION_ACCEPTANCE.md).

## Scope

The category lifecycle owns:

- create;
- rename;
- delete with an explicit override policy;
- reorder;
- app-owned validation/failures;
- repository/state mutation and regression tests.

It does not own dependency/toolchain changes, networking, backup/import/export, shortcuts, or automatic-rule changes.

Compose owns presentation only; Agent 83 owns integration plumbing only. Neither duplicates lifecycle validation/persistence behavior.

## Repository contract

`CategoryManagementRepository` is a separate app-owned contract implemented by `DefaultOrganizerRepository`. The application composition root exposes the same `DefaultOrganizerRepository` instance through both `OrganizerRepository` and `CategoryManagementRepository`; there is no second category repository or independent category-state owner.

The lifecycle API returns `CategoryManagementResult<T>` and `CategoryManagementError`. Persistence implementation exceptions are not exposed through this contract; failed writes become the app-owned `PersistenceFailure` error while coroutine cancellation is preserved.

Agent 83 maps those app-owned failures to safe presentation messages. Raw DataStore/storage exceptions do not cross into UI state.

## Category identity and creation

Creation generates one opaque custom `CategoryId` before the atomic state update. The production generator uses a random UUID inside the existing `custom:` namespace; tests inject deterministic generators.

The generated ID:

- is independent of the display name;
- must use the custom namespace;
- must not already appear as a category definition, category-order entry, or persisted override reference;
- remains stable through rename, process recreation, serialization, reorder and integration with shelf/search/reporting.

A new category is appended after the current normalized category order and its definition/order entry are written in one state-store transaction.

## Name validation

Category names follow these product rules:

1. surrounding whitespace is trimmed before storage;
2. blank names are rejected;
3. the maximum is 48 Unicode code points after trimming;
4. names must not collide with either built-in or custom display names;
5. duplicate comparison is deterministic and case-insensitive using the same locale-independent `uppercase().lowercase()` expansion strategy already used by local search;
6. rename excludes the category being renamed from duplicate comparison.

No Unicode-normalization library or other dependency is added. Internal whitespace is preserved.

## Rename

Rename changes only custom-category display metadata. It never changes the `CategoryId`, category order, app overrides, favourites, hidden state, or automatic classification rules.

Built-in categories are immutable through this feature and return `BuiltInCategoryImmutable`.

## Delete

Deletion supports two explicit policies.

### Reassign

Every override targeting the deleted custom category is changed to a supplied existing built-in or custom destination. The destination cannot be the category being deleted and cannot be unknown.

### Return to automatic

Every override targeting the deleted custom category is removed. The affected apps therefore expose the existing automatic classification path again: bundled rule, Android category, or `Unsorted` fallback.

For either policy, override changes, custom-definition removal, and category-order removal happen in one state-store transaction. Favourite and hidden state are not changed.

Built-in categories cannot be deleted.

The management UI determines whether a category is populated from persisted `OrganizerState.categoryOverrides`, not only currently scanned/visible apps. Retained overrides for hidden or currently uninstalled apps therefore still require an explicit populated-category deletion decision.

## Reorder

Reorder is intentionally strict at the mutation boundary. The submitted list must be an exact permutation of every currently valid built-in and custom normal category.

Validation order is deterministic:

1. reject duplicate IDs;
2. reject unknown IDs;
3. reject missing valid IDs.

No category is silently dropped or invented by a reorder request. `Favourites` remains a virtual section and has no category ID in the persisted normal-category order. Successful order is persisted exactly and survives process recreation.

The lower-level schema-v2 normalization from Agent 80 remains unchanged and still protects reads/writes from stale or malformed persisted order data.

The integrated ViewModel handles only the UI gesture translation: it reads the current app-owned `orderedCategories()` IDs, performs one adjacent swap, and submits the complete permutation to `CategoryManagementRepository`. Repository validation remains authoritative.

## Classification and search

No automatic classifier rule changes are made by category management. Existing precedence remains:

1. user override;
2. bundled known-app rule;
3. Android-declared category;
4. `Unsorted`.

A custom override therefore outranks automatic classification exactly like a built-in override. Removing that override during delete/return-to-automatic reveals the classification that would otherwise have applied.

Local search consumes the resolved category display name from `CategorizedApp`, so created/renamed custom category names remain searchable without a second search index or network access.

The classification report represents a custom effective category with stable identity plus display name while retaining the independent `ClassificationSource`, including `USER_OVERRIDE`.

## Integrated presentation state

The category-management UI state is a pure projection of persisted `OrganizerState` plus transient safe error text:

- category definitions/order come from `OrganizerState.orderedCategories()`;
- assigned counts come from persisted category overrides;
- built-in/custom mutability comes from the app-owned category kind;
- presentation does not persist its own category list/order.

The existing organizer shelf and move picker consume the same app-owned category definitions/order, so a create/rename/reorder operation is reflected throughout the real application without a second synchronization path.

## Atomicity and concurrency

State-dependent validation executes inside `OrganizerStateStore.update`. DataStore serializes these transactions, so concurrent lifecycle mutations revalidate against the latest committed state rather than a stale pre-read snapshot. Tests cover concurrent duplicate creation and prove that only one conflicting name is committed.

Invalid deletion destinations/order and persistence failure leave committed organizer state unchanged. Favourite/hidden preservation is covered by lifecycle and DataStore recreation tests.

## Acceptance boundary

Automated tests/CI prove the domain, migration, integration and quality contracts, but they are not sufficient to close this persisted-state wave. PR #18 remains unmerged until a signed debug APK is installed over an existing pre-category-management Samsung installation without clearing data and the full physical migration/lifecycle checklist passes.
