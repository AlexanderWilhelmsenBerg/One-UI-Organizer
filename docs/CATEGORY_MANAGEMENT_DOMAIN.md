# Category Management Domain Contract

This document records the Agent 81 domain/repository behavior built on the category identity and schema-v2 persistence foundation from Agent 80.

## Scope

Agent 81 owns custom-category lifecycle behavior only:

- create;
- rename;
- delete with an explicit override policy;
- reorder;
- app-owned validation/failures;
- repository/state mutation and regression tests.

This lane does not add Compose management UI, dependency/toolchain changes, networking, backup/import/export, shortcuts, or automatic-rule changes.

## Repository contract

`CategoryManagementRepository` is a separate app-owned contract implemented by `DefaultOrganizerRepository`. Keeping lifecycle mutation separate from the existing `OrganizerRepository` avoids forcing the parallel Compose lane to rewrite unrelated repository fakes while Agent 81 and Agent 82 are in flight.

The lifecycle API returns `CategoryManagementResult<T>` and `CategoryManagementError`. Persistence implementation exceptions are not exposed through this contract; failed writes become the app-owned `PersistenceFailure` error while coroutine cancellation is preserved.

## Category identity and creation

Creation generates one opaque custom `CategoryId` before the atomic state update. The production generator uses a random UUID inside the existing `custom:` namespace; tests inject deterministic generators.

The generated ID:

- is independent of the display name;
- must use the custom namespace;
- must not already appear as a category definition, category-order entry, or persisted override reference;
- remains stable through rename, process recreation, serialization, and reorder.

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

## Reorder

Reorder is intentionally strict at the mutation boundary. The submitted list must be an exact permutation of every currently valid built-in and custom normal category.

Validation order is deterministic:

1. reject duplicate IDs;
2. reject unknown IDs;
3. reject missing valid IDs.

No category is silently dropped or invented by a reorder request. `Favourites` remains a virtual section and has no category ID in the persisted normal-category order. Successful order is persisted exactly and survives process recreation.

The lower-level schema-v2 normalization from Agent 80 remains unchanged and still protects reads/writes from stale or malformed persisted order data.

## Classification and search

No automatic classifier rule changes are made in this lane. Existing precedence remains:

1. user override;
2. bundled known-app rule;
3. Android-declared category;
4. `Unsorted`.

A custom override therefore outranks automatic classification exactly like a built-in override. Removing that override during delete/return-to-automatic reveals the classification that would otherwise have applied.

Local search already consumes the resolved category display name from `CategorizedApp`, so created/renamed custom category names remain searchable without a second search index or network access.

## Atomicity and concurrency

State-dependent validation executes inside `OrganizerStateStore.update`. DataStore serializes these transactions, so concurrent lifecycle mutations revalidate against the latest committed state rather than a stale pre-read snapshot. Tests cover concurrent duplicate creation and prove that only one conflicting name is committed.
