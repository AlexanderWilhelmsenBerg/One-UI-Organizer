# Android platform apps spike

**Lane:** Agent 10 / `feature/platform-apps`

**Rebase status:** Rebased onto the current integrated `main` baseline on 2026-09-10 after the sibling domain, persistence, UI, baseline/dependency and workflow changes landed. The frozen `InstalledAppSource` / `AppLauncher` contracts remain unchanged.

This note records the platform decisions made by the Android launcher-app discovery and launch spike. It does not define the final organizer UI.

## Discovery

The implementation queries `PackageManager` with `ACTION_MAIN` + `CATEGORY_LAUNCHER`.

The manifest declares the matching `<queries><intent>...</intent></queries>` visibility signature. The app does not request `QUERY_ALL_PACKAGES` or `INTERNET`.

`AndroidInstalledAppSource` executes the package scan on `Dispatchers.IO` and maps Android metadata into the frozen app-owned `InstalledApp` model. Organizer's own package is removed from the result. The diagnostic host requests a fresh scan from `onResume`; there is no package observer or background service.

Android application categories are translated at the adapter boundary into `PlatformAppCategory`; Android framework types do not leave the platform adapter.

The diagnostic surface reports both launcher-target count and distinct package count. The distinction matters because a package may legitimately expose multiple launcher activities or aliases.

## Launcher aliases and multiple activities

The launch target identity is the frozen `LaunchTargetId(packageName, className)`.

Policy:

- different launcher components from one package remain separate launch targets;
- launcher aliases therefore remain addressable rather than being collapsed by package name;
- exact duplicate `ResolveInfo` rows for the same package/class component are collapsed;
- ordering is deterministic by case-normalized label, original label, package name, then component class name;
- `AppId` remains package-scoped, so higher layers may treat multiple launch entries from one package as one app for organizer-owned state while still launching the selected component exactly.

## Launching

`AndroidAppLauncher` validates the frozen target through a small pure `LaunchComponentSpec` seam, then builds a main-launcher intent for that exact `ComponentName`. Because the adapter is deliberately created from the application context, the intent includes `FLAG_ACTIVITY_NEW_TASK`, matching Android's requirement for launching an activity from a non-Activity context and the task behavior expected from a launcher-style surface. It returns `false` for blank target identity, missing activities, or security rejection.

The adapter does not expose `Intent`, `ComponentName`, `ResolveInfo`, `ApplicationInfo`, or `PackageManager` through the app-owned contracts.

The diagnostic host calls `finish()` after a successful external launch. Agent 50 should preserve that host-level behavior if direct return to One UI Home after leaving the launched app remains the desired product interaction; it should not be moved into `AppLauncher`, because task/host lifecycle is a presentation concern.

## Companion presentation

The selected spike presentation is a translucent, dim-behind activity containing a bottom sheet-shaped Compose diagnostic surface. It uses no blur API and adds no dependency.

The activity-specific `Theme.OneUIOrganizer.Translucent` sets:

- transparent window background;
- `windowIsTranslucent = true`;
- background dimming with a modest dim amount.

The host calls the existing Activity edge-to-edge API. `Theme.OneUIOrganizer` remains an ordinary non-translucent theme and is the normal edge-to-edge/fullscreen fallback if Samsung/Android window behavior proves the translucent host unreliable.

The diagnostic Compose screen is intentionally temporary. Agent 40/50 can replace its contents without changing the platform source/launcher adapters or the host theme decision.

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

## Physical-device observations

Initial owner testing on 2026-09-10 established that the companion activity opens and the diagnostic scan completes. That build reported 566 launcher targets and displayed the first 12 diagnostic entries. Tapping any entry crashed the Organizer process.

The launch crash was traced to the adapter using an application context with an intent from `Intent.makeMainActivity()` that lacked `FLAG_ACTIVITY_NEW_TASK`. Android requires that flag when `startActivity()` is called outside an Activity context. The adapter now adds the flag explicitly. CI run #98 is green with the correction; device retest of exact target launch remains pending.

The reported 566 launcher targets are not yet accepted as the expected app set. The diagnostic now also reports distinct package count so the next device pass can distinguish legitimate multi-component/alias expansion from unexpected over-inclusion.

## Samsung / Android 16 acceptance checklist

Run this checklist on the primary Samsung device before declaring I1-I4 complete:

1. Install the debug APK without changing launcher/default-HOME settings.
2. Open One UI Organizer from One UI Home and confirm the translucent/dimmed sheet presentation is visually acceptable. If it is clipped, incorrectly sized, opaque, or unstable across repeated opens, switch the activity to the normal `Theme.OneUIOrganizer` fallback and record the reason.
3. Compare the diagnostic launcher-target count and distinct-package count against ordinary user-launchable apps visible in One UI. Record any material omissions or over-inclusion and whether they are explained by Android package visibility/profile/alias behavior.
4. Confirm One UI Organizer itself does not appear in the discovered target list.
5. Launch at least one Samsung app, one Google app, one third-party app, one game, and one work/productivity app from the diagnostic list.
6. If the device contains a package with a launcher alias or multiple launcher activities, confirm the entries are deterministic and each selected entry launches its exact intended component.
7. Install a small test app, resume or reopen Organizer, and confirm it appears without a package observer/background service.
8. Remove that test app, resume or reopen Organizer, and confirm it disappears.
9. Repeat open/dismiss several times and confirm dismiss returns directly to One UI Home.
10. Launch a sampled app, navigate back/exit it, and confirm the finished Organizer host does not unexpectedly remain as a stale foreground surface.
11. Rotate/change display mode if applicable and confirm the translucent host remains usable; otherwise select the normal fallback.
12. Record device model, Android version, One UI version, discovered target count, distinct package count, sampled apps, alias result, presentation choice, and any deviations in the PR before merge.

Until this checklist is completed, physical-device acceptance is explicitly **pending**.
