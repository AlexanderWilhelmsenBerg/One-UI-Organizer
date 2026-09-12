# Category Shortcuts

This document records the dynamic and user-requested pinned category-shortcut contract introduced by Agent 91. The feature consumes the stable category identity and order established by the category-management wave; it does not redefine or persist category state.

## Scope

One UI Organizer exposes two Android launcher integrations:

- a small deterministic set of dynamic app shortcuts for useful current categories;
- a user-requested pinned Home screen shortcut for a specific normal category.

`Favourites` remains a virtual shelf section. It is not given a fake `CategoryId` for shortcut support. A future favourites shortcut would require a separate virtual-destination design.

No shortcut library, background service, WorkManager job, network permission, schema change, or additional category-state store is introduced.

## Stable shortcut identity

Every normal category shortcut ID is derived only from the durable app-owned `CategoryId`:

```text
category:<CategoryId.value>
```

Examples:

```text
category:builtin:work
category:custom:550e8400-e29b-41d4-a716-446655440000
```

The display name and category position are deliberately absent from the shortcut ID. Therefore:

- rename keeps the same logical shortcut identity;
- reorder keeps the same logical shortcut identity;
- a custom category keeps its existing opaque ID;
- built-in and custom categories use the same shortcut contract.

Android `ShortcutInfo`, `ShortcutManager`, `Intent`, and `Icon` stay inside the Android platform boundary. The app-owned launch destination is `CategoryShortcutDestination(CategoryId)`.

## Dynamic selection policy

Dynamic category shortcuts are synchronized from the current organizer projection after a successful foreground app scan.

Selection is deterministic:

1. start with `OrganizerState.orderedCategories()` through the existing shelf projection;
2. retain only categories containing at least one currently installed, non-hidden app visible on the normal shelf;
3. preserve the persisted category order;
4. cap the result at the smaller of:
   - Android's device-reported `ShortcutManager.maxShortcutCountPerActivity`; and
   - the product launcher-menu cap of four categories, matching Android's current launcher presentation guidance;
5. assign shortcut ranks in that selected order.

Retained overrides for currently uninstalled apps and currently hidden apps continue to count for category-management/persistence semantics, but neither makes a category eligible for a dynamic launcher shortcut by itself. This prevents shortcut publication from turning retained or deliberately hidden state into apparently useful launcher destinations.

`setDynamicShortcuts()` replaces the dynamic set when the desired IDs, labels, or ranks differ. As a result, deleted categories and categories that stop qualifying are removed from the dynamic set. Existing dynamic shortcuts are inspected first so a no-op foreground refresh does not spend rate-limit budget.

If the launcher reports active shortcut rate limiting, publication is deferred until a later foreground synchronization. If the app scan itself fails, the app does not erase launcher shortcuts based on an unknown/empty inventory.

## Synchronization lifecycle

There is no background shortcut worker.

`MainActivity` observes the existing organizer presentation state while it is alive. Synchronization occurs after a successful foreground refresh and whenever the authoritative category/order/current-visible-app projection changes while the activity is active. The Android adapter performs potentially slow shortcut-manager inspection/publication on `Dispatchers.IO`.

This foreground policy also re-checks dynamic shortcuts after app launch, which is required because dynamic shortcuts are not guaranteed to survive device restore.

## Pinned shortcut UX

Category management shows **Pin to Home screen** for every normal built-in or custom category.

Pinning is always user initiated. The app checks `ShortcutManager.isRequestPinShortcutSupported`:

- when supported, the action calls `requestPinShortcut()` and the launcher owns the confirmation UI;
- when unsupported, the pin action is disabled and category management explains that the current launcher does not support in-app pinning.

The app never silently adds a pinned shortcut.

## Rename and reorder

A rename changes the category label but not its ID. Foreground synchronization republishes current dynamic metadata and calls `updateShortcuts()` for matching pinned category shortcuts that are not part of the selected dynamic set. The launcher can therefore refresh the visible label while preserving the pinned logical identity.

Reorder can change dynamic selection/rank, but it never changes the shortcut ID of any category.

## Deleted categories

A deleted custom category is absent from the next authoritative ordered category list.

During synchronization:

- it disappears from the dynamic set;
- a matching pinned shortcut is disabled with an explanatory platform message where supported.

Pinned shortcuts cannot be removed programmatically by the app. If an old/stale shortcut intent is nevertheless delivered, the app-owned destination resolver does not invent category state. The shelf detects that the `CategoryId` no longer resolves, displays an understandable stale-shortcut message, and falls back to the normal full shelf instead of crashing or presenting a meaningless empty category.

## Launch routing

Category shortcut intents target the existing `MainActivity`; no second Activity exists solely for shortcut handling.

The Android boundary uses a stable explicit action and category-ID extra. `MainActivity` converts that Android payload immediately into `CategoryShortcutDestination` and passes it to the existing `OrganizerViewModel`.

For a valid destination:

- hidden-app/category-management sub-surfaces are closed;
- the search query is cleared;
- the existing shelf projection is scoped exactly to the requested category;
- normal app launching still finishes Organizer and returns the user to One UI;
- the focused shelf includes a **Show all** action to return to the normal category shelf.

`FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` plus `onNewIntent()` allow a shortcut to retarget an existing Organizer activity without introducing another activity class.

## Android APIs

The implementation uses the platform APIs available within the existing minimum SDK:

- `ShortcutManager.maxShortcutCountPerActivity`;
- `ShortcutManager.dynamicShortcuts`;
- `ShortcutManager.setDynamicShortcuts()`;
- `ShortcutManager.pinnedShortcuts`;
- `ShortcutManager.updateShortcuts()`;
- `ShortcutManager.disableShortcuts()`;
- `ShortcutManager.isRateLimitingActive`;
- `ShortcutManager.isRequestPinShortcutSupported`;
- `ShortcutManager.requestPinShortcut()`;
- `ShortcutManager.reportShortcutUsed()`;
- `ShortcutInfo.Builder` and `Icon.createWithResource()`.

No Android shortcut object crosses into the model/domain contracts.

## Persistence boundary

Shortcut code owns no organizer persistence. It does not serialize category IDs, category labels, shortcut selections, or an independent pinned-state registry.

The only authoritative category state remains the existing `OrganizerState`/repository path. Android's launcher shortcut records are a platform projection and are reconciled from that state on foreground synchronization.

## Automated coverage

Permanent tests cover:

- stable `CategoryId` to shortcut-ID mapping;
- rename identity stability and current-label projection;
- reorder identity stability and deterministic order;
- current visible/non-empty dynamic filtering;
- retained-uninstalled and hidden-only category exclusion from dynamic eligibility;
- platform/product maximum enforcement;
- built-in and custom categories;
- deleted-category removal from dynamic selection;
- exact valid category focus;
- stale/deleted category destination fallback;
- separation between retained persisted overrides and shortcut eligibility;
- category-management pin actions and unsupported-launcher presentation;
- Android instrumentation coverage for valid, unrelated and malformed shortcut intent payload decoding.

Android framework shortcut publication itself remains a platform/device acceptance concern rather than introducing Robolectric or another test dependency.

## Samsung / One UI acceptance still required

On the project Samsung test device, verify all of the following before this feature is treated as device-accepted:

1. Long-press the Organizer launcher icon and confirm the dynamic category shortcuts appear in persisted order, with no more than the selected platform/product limit.
2. Reorder categories, reopen/resume Organizer, and confirm launcher order updates while shortcut identities continue to target the same categories.
3. Rename a custom category and confirm its dynamic and pinned visible labels update without creating a second logical shortcut.
4. Pin both one built-in and one custom category from category management; confirm One UI displays its normal confirmation flow and creates shortcuts only after user approval.
5. Launch each pinned shortcut and confirm Organizer opens directly in the correct category context; launch an app from that context and confirm Organizer dismisses back to One UI normally.
6. Delete a custom category that has a pinned shortcut, synchronize by returning to Organizer, and confirm One UI disables/handles the stale pinned shortcut sensibly. Also verify a stale intent delivered to Organizer falls back to the full shelf with the explanatory message.
7. Remove or hide enough category contents to change the dynamic qualifying set and confirm obsolete dynamic shortcuts disappear on the next successful foreground synchronization.
8. Reboot/restore-style test as practical and confirm reopening Organizer republishes missing dynamic shortcuts.

## Agent 92 integration expectation

Agent 91 includes the minimal `MainActivity` wiring required to prove the shortcut feature end to end. Agent 92 should treat the shortcut policy/identity/platform adapter as owned by this lane and only reconcile shared composition-root conflicts after Agent 90 is merged/rebased.

In particular, Agent 92 should preserve:

- `handleShortcutIntent()` / `onNewIntent()` routing;
- foreground shortcut synchronization;
- category-management pin support/callback wiring;
- focused shelf destination clearing;
- any independent backup/import callbacks introduced by Agent 90.

Agent 92 should not create a second shortcut state store, change `CategoryId`, or move Android shortcut types into domain/application models merely to resolve a merge conflict.
