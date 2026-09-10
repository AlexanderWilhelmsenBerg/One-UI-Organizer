# Android platform apps spike

**Lane:** Agent 10 / `feature/platform-apps`

**Rebase status:** Rebased onto the current integrated `main` baseline on 2026-09-10 after the sibling domain, persistence, UI, baseline/dependency and workflow changes landed. The frozen `InstalledAppSource` / `AppLauncher` contracts remain unchanged.

This note records the platform decisions made by the Android launcher-app discovery and launch spike. It does not define the final organizer UI.

## Discovery

The implementation queries `PackageManager` with `ACTION_MAIN` + `CATEGORY_LAUNCHER`.

The manifest declares the matching `<queries><intent>...</intent></queries>` visibility signature. The app does not request `QUERY_ALL_PACKAGES` or `INTERNET`.

`AndroidInstalledAppSource` executes the package scan on `Dispatchers.IO` and maps Android metadata into the frozen app-owned `InstalledApp` model. Organizer's own package is removed from the result. The diagnostic host requests a fresh scan from `onResume`; there is no package observer or background service.

Android application categories are translated at the adapter boundary into `PlatformAppCategory`; Android framework types do not leave the platform adapter.

A physical Samsung pass initially raised concern that some system/helper-looking entries might be false positives. The owner then checked those examples against One UI and confirmed that Disk and Assistant are in fact launcher apps, and could not identify any discovered entry that was absent from the One UI app drawer. The supported `ACTION_MAIN` + `CATEGORY_LAUNCHER` query is therefore accepted for the primary Samsung device.

Do not add blanket system-app filtering. Legitimate user-facing Samsung/Google apps can be system applications and are required by product acceptance. Likewise, do not read Samsung launcher databases or use undocumented Samsung APIs.

## Launcher aliases and multiple activities

The launch target identity is the frozen `LaunchTargetId(packageName, className)`.

Policy:

- different launcher components from one package remain separate launch targets;
- launcher aliases therefore remain addressable rather than being collapsed by package name;
- exact duplicate `ResolveInfo` rows for the same package/class component are collapsed;
- ordering is deterministic by case-normalized label, original label, package name, then component class name;
- `AppId` remains package-scoped, so higher layers may treat multiple launch entries from one package as one app for organizer-owned state while still launching the selected component exactly.

## Launching

`AndroidAppLauncher` validates the frozen target through a small pure `LaunchComponentSpec` seam, then builds a main-launcher intent for that exact `ComponentName` and adds `FLAG_ACTIVITY_NEW_TASK` because the adapter is intentionally created with application context. It returns `false` for blank target identity, missing activities, or security rejection.

The initial Samsung device pass exposed the missing `FLAG_ACTIVITY_NEW_TASK`: every target tap crashed before the external app could open. After the flag was added, the owner retested on-device and confirmed that tapping a diagnostic target opens the selected app successfully.

The adapter does not expose `Intent`, `ComponentName`, `ResolveInfo`, `ApplicationInfo`, or `PackageManager` through the app-owned contracts.

The diagnostic host calls `finish()` after a successful external launch. Agent 50 should preserve that host-level behavior if direct return to One UI Home after leaving the launched app remains the desired product interaction; it should not be moved into `AppLauncher`, because task/host lifecycle is a presentation concern.

## Companion presentation

The selected spike presentation is a translucent, dim-behind activity containing a bottom sheet-shaped Compose diagnostic surface. It uses no blur API and adds no dependency.

The activity-specific `Theme.OneUIOrganizer.Translucent` sets:

- transparent window background;
- `windowIsTranslucent = true`;
- background dimming with a modest dim amount.

The host calls the existing Activity edge-to-edge API. `Theme.OneUIOrganizer` remains an ordinary non-translucent theme and is the normal edge-to-edge/fullscreen fallback if Samsung/Android window behavior proves the translucent host unreliable.

The diagnostic Compose screen is intentionally temporary. For physical validation it shows the entire lazily rendered target list and displays exact package/component identity under each label. Agent 40/50 can replace its contents without changing the platform source/launcher adapters or the host theme decision.

## Automated coverage

Pure JVM tests cover:

- Android-independent launcher metadata -> app-owned model mapping;
- exact target identity preservation;
- duplicate component collapse;
- alias preservation;
- deterministic ordering;
- exact package/component launch selection;
- rejection of incomplete launch targets.

The platform adapter itself requires real Android behavior to prove package visibility, `PackageManager` results, component start success, and One UI window behavior. The repository CI has no connected Android device, so those checks remain in the mandatory Samsung acceptance checklist rather than adding an unexecuted instrumentation dependency graph to this lane.

## Samsung device observations — 2026-09-10

Confirmed on the owner's Samsung device:

- the companion activity opens successfully;
- the launcher scan completes;
- the translucent/dimmed diagnostic presentation is usable;
- the corrected exact-component launch path opens selected apps successfully;
- the scan reports **566 launcher targets across 564 distinct packages**;
- the two-target difference shows that aliases/multiple launcher activities contribute only two additional targets, so the large package count is not caused by alias inflation;
- the original diagnostic displayed only the first 12 alphabetically sorted entries, which all began with `A`; that cap has now been removed;
- entries that initially looked suspicious, including Disk and Assistant, were verified to be real One UI app-drawer entries;
- after inspecting the full result, the owner could not identify a discovered entry that was absent from the One UI app drawer.

**Discovery acceptance is passed for the primary Samsung device.** The public launcher query is a practically complete/clean match for the expected One UI launchable app set without `QUERY_ALL_PACKAGES`.

## Samsung / Android 16 acceptance checklist

Run this checklist on the primary Samsung device before declaring I1-I4 complete:

1. Install the debug APK without changing launcher/default-HOME settings.
2. Open One UI Organizer from One UI Home and confirm the translucent/dimmed sheet presentation is visually acceptable. **Passed:** presentation is usable on the primary Samsung device.
3. Compare the full diagnostic list against ordinary user-launchable apps visible in One UI. **Passed:** no false-positive entry could be identified; Disk and Assistant were confirmed as real One UI launcher apps.
4. Confirm One UI Organizer itself does not appear in the discovered target list.
5. Launch at least one Samsung app, one Google app, one third-party app, one game, and one work/productivity app from the diagnostic list.
6. If the device contains a package with a launcher alias or multiple launcher activities, confirm the entries are deterministic and each selected entry launches its exact intended component.
7. Install a small test app, resume or reopen Organizer, and confirm it appears without a package observer/background service.
8. Remove that test app, resume or reopen Organizer, and confirm it disappears.
9. Repeat open/dismiss several times and confirm dismiss returns directly to One UI Home.
10. Launch a sampled app, navigate back/exit it, and confirm the finished Organizer host does not unexpectedly remain as a stale foreground surface.
11. Rotate/change display mode if applicable and confirm the translucent host remains usable; otherwise select the normal fallback.
12. Record device model, Android version, One UI version, discovered target count, sampled apps, alias result, presentation choice, and any deviations in the PR before merge.

Discovery, launch-path correctness, and basic companion presentation have now been demonstrated on the primary Samsung device. The remaining checklist items still require completion or explicit owner acceptance before the full physical-device gate is closed.
