# Metadata + Presentation Integration Acceptance

## Status

Agent 102 owns combined acceptance for the merged Agent-101 presentation/navigation lane (PR #22) and Agent-100 supported-metadata lane (PR #23) on top of the accepted Agent-92 portability/shortcut baseline (PR #21).

Start baseline:

- accepted PR #21 portability/shortcut integration, including owner Samsung acceptance;
- PR #22 merged presentation/navigation;
- PR #23 merged F-Droid metadata enrichment;
- `main` at Agent-102 start: `8d34cc88106bb346baac00c012f9ad12c7a9e50f`;
- integration branch: `integration/metadata-presentation`.

Repository state is authoritative. This document must not claim physical-device results that were not actually observed.

## Integrated architecture

The accepted dependency direction remains:

```text
supported provider/cache/classification
               |
               v
       app-owned effective state
               |
               v
 presentation / Organizer / Categories
```

The primary shell contains exactly two destinations:

- `Organizer` — existing shelf/search/category-focus and app-launch experience;
- `Categories` — category overview and contextual management entry points.

Backup & restore, category management, diagnostics and similar utilities are contextual surfaces. They are not additional primary destinations.

Primary destination selection is presentation/navigation state only. It is not an `OrganizerState` field, does not own classification/category data and is not part of backup/export. The shell hosts existing feature screens rather than moving feature state into navigation.

On phones the current shell uses bottom navigation. Organizer/Categories content is passed into the shell as independent feature content so a later Galaxy Fold/tablet treatment can substitute a navigation rail or other adaptive chrome without rewriting those feature screens.

## Automated acceptance matrix

Agent 102 must keep or add automated proof for the following seams:

| Area | Required proof |
| --- | --- |
| Primary destinations | exactly `Organizer` and `Categories`; restore mapping remains deterministic |
| Search/state | primary switching does not reset destination-local saveable presentation state; real Organizer query remains outside navigation ownership |
| Back | nested Backup/category-management/Hidden Apps close before system Back; root tabs do not accumulate synthetic history |
| Shortcuts | shortcut routing selects `Organizer`; existing ViewModel tests prove exact stable-category focus and stale/deleted fallback |
| Backup/import | existing atomic import/process-recreation tests preserve custom `CategoryId` and shortcut identity |
| Metadata repository | supported metadata maps through the real repository/category engine while user override stays authoritative |
| Metadata presentation | `SUPPORTED_METADATA` survives normal UI-state mapping/explanation data |
| Offline/provider failure | Agent-100 provider/cache tests retain local/stale behavior and convert ordinary network/provider failure safely |
| Permissions | CI requires only `INTERNET` and rejects any other declared Android permission, with `QUERY_ALL_PACKAGES` explicitly forbidden |

Permanent CI must pass on the final Agent-102 head: debug assembly, Android-test APK assembly, JVM tests, Android Lint, ktlint, dependency health, strict dependency verification, warning failure, configuration-cache creation/reuse and permission audit.

## Shortcut regression contract

For both built-in and custom category shortcuts:

1. launcher shortcut entry selects `Organizer`;
2. the stable requested `CategoryId` is resolved/focused;
3. `Categories` management is not selected accidentally;
4. launching an app still finishes Organizer normally;
5. Back/dismiss remains normal;
6. deleted/stale category destinations fall back safely with the existing explanatory state.

Renaming or reordering a custom category must not change shortcut identity. Restored backup state must preserve the same category identity used by shortcut resolution.

## Backup/import regression contract

From `Categories`:

1. open contextual Backup & restore;
2. perform a real export through Android SAF;
3. return to Organizer normally;
4. import a representative valid backup;
5. explicitly confirm replacement;
6. verify UI/category counts refresh from the shared state store;
7. switch `Organizer` / `Categories` and verify restored custom categories/order;
8. verify restored custom IDs still resolve from shortcuts;
9. verify an invalid import remains atomic and leaves current state unchanged.

The navigation shell must not replace the activity-result/SAF boundary and must not add another imported-state owner.

## Metadata acceptance contract

The merged F-Droid provider remains a best-effort enrichment path:

- installed-app scanning/local classification publishes without waiting for network I/O;
- precedence remains user override > bundled rule > Android-declared category > supported metadata > `Unsorted`;
- metadata cannot create/rename custom categories or modify category order;
- provider/network/JSON types remain behind adapters;
- the derived cache remains separate from user-owned organizer state and backup/export;
- stale useful cache remains usable when refresh fails;
- provider/network failure leaves shelf/search/launch usable;
- a metadata-derived classification exposes `ClassificationSource.SUPPORTED_METADATA` through the normal app-owned UI model;
- metadata details/explanations remain contextual and never create a third primary destination.

Agent 102 should capture a fresh private classification report on the Samsung owner device and record generalized before/after counts for formerly `Unsorted` entries without committing the owner's installed-app inventory.

## Privacy / permission audit

Expected manifest state:

- `android.permission.INTERNET` — present only for the supported F-Droid metadata provider;
- `android.permission.QUERY_ALL_PACKAGES` — absent;
- broad storage permissions — absent;
- telemetry/analytics/ads — absent;
- account/cloud-sync behavior — absent;
- background service / WorkManager metadata refresh — absent.

Backup remains local/user-selected via SAF. Search text, favourites, hidden state, manual category assignments, custom category definitions and backups are not sent to the metadata provider.

## Physical Samsung acceptance — pending final Agent-102 APK

Use the repository-signed debug APK produced from the exact final Agent-102 head. Record the exact commit, workflow run, artifact name/digest and generalized observations below.

### Primary navigation

- cold launch lands correctly;
- switch repeatedly `Organizer` ↔ `Categories`;
- selected state is visually clear;
- bottom navigation touch targets are comfortable;
- no utility appears as a third primary destination.

### State preservation

- enter an Organizer search, switch tabs and return;
- exercise shortcut-requested category focus, switch tabs and return;
- background/foreground the app;
- recreate/kill/relaunch as practical;
- verify selected primary destination restores where normal activity state restoration applies.

### Back / nested flows

- root Back from Organizer;
- root Back from Categories;
- create/rename/delete confirmation dialogs;
- category-management subflows;
- Backup & restore;
- Android file picker return;
- other nested sheets/dialogs.

Nested UI closes first. Category-management and Backup flows return to `Categories`. Primary tab switching must not create a deep Back history. Root companion dismissal remains normal.

### Category lifecycle

- create custom category;
- rename while preserving identity/assignment;
- reorder;
- delete with reassignment;
- delete/return-to-automatic;
- verify assignment counts and shelf results remain correct.

### Shortcuts

- dynamic category shortcut;
- pinned built-in shortcut;
- pinned custom shortcut;
- renamed custom category label;
- correct `Organizer` landing/category focus;
- stale/deleted custom category safeguard.

### Backup

- enter/leave Backup & restore;
- perform at least one real export;
- import/restore representative state where practical;
- verify navigation remains healthy afterward;
- verify restored custom category IDs continue to resolve from shortcuts.

### Metadata / offline

- confirm first useful local shelf is not held behind metadata network work;
- allow one online refresh and identify at least one covered formerly-`Unsorted` package if available;
- confirm its explanation/source is supported metadata when metadata decides;
- restart/use cached metadata;
- test airplane mode or otherwise unavailable provider;
- confirm local shelf/search/launch and existing manual/rule/Android classifications remain usable;
- note practical network/memory impact of the F-Droid bulk index.

### Accessibility / scaling

- enlarged font/display scale;
- both destination labels remain understandable;
- category actions remain usable without catastrophic clipping;
- TalkBack labels/selected destination semantics where practical.

### Orientation / larger screens

- orientation change where supported;
- Galaxy Fold/large-screen sanity pass if a target is available;
- confirm feature screens still function without phone-only state assumptions.

A full navigation-rail implementation is not required in this wave.

## Evidence record

Until actual device observations are supplied, the physical result remains **PENDING**.

| Evidence | Result |
| --- | --- |
| Final Agent-102 HEAD | pending |
| Agent-102 PR | pending |
| Permanent CI | pending |
| Signed debug APK run/artifact | pending |
| Samsung primary navigation | pending |
| Search/state preservation | pending |
| Back/nested flows | pending |
| Category lifecycle | pending |
| Shortcut regression | pending |
| Backup/import | pending |
| Metadata online/offline | pending |
| Accessibility/scaling | pending |
| Orientation/large-screen sanity | pending |
| Privacy/permission audit | pending final-head CI confirmation |
