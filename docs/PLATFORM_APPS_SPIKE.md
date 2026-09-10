# Android platform apps spike

**Lane:** Agent 10 / `feature/platform-apps`

**Rebase status:** Rebased onto the current integrated `main` baseline on 2026-09-10 after the sibling domain, persistence, UI, baseline/dependency and workflow changes landed. The frozen `InstalledAppSource` / `AppLauncher` contracts remain unchanged.

This note records the platform decisions made by the Android launcher-app discovery and launch spike. It does not define the final organizer UI.

## Discovery

The implementation queries `PackageManager` with `ACTION_MAIN` + `CATEGORY_LAUNCHER`.

The manifest declares the matching `<queries><intent>...</intent></queries>` visibility signature. The app does not request `QUERY_ALL_PACKAGES` or `INTERNET`.

`AndroidInstalledAppSource` executes the package scan on `Dispatchers.IO` and maps Android metadata into the frozen app-owned `InstalledApp` model. Organizer's own package is removed from the result. The diagnostic host requests a fresh scan from `onResume`; there is no package observer or background service.

Android application categories are translated at the adapter boundary into `PlatformAppCategory`; Android framework types do not leave the platform adapter.

A physical Samsung pass initially raised concern that some system/helper-looking entries might be false positives. The owner then checked those examples against One UI and confirmed that Disk and Assistant are in fact launcher apps, and could not identify any discovered entry that was absent from the One UI app drawer. Representative expected Samsung, Google, third-party, game, and work/productivity apps were also present. Discovery is therefore accepted for the primary Samsung device.

Do not add blanket system-app filtering. Legitimate user-facing Samsung/Google apps can be system applications and are required by product acceptance. Likewise, do not read Samsung launcher databases or use undocumented Samsung APIs.

## Launcher aliases and multiple activities

The launch target identity is the frozen `LaunchTargetId(packageName, className)`.

Policy:

- different launcher components from one package remain separate launch targets;
- launcher aliases therefore remain addressable rather than being collapsed by package name;
- exact duplicate `ResolveInfo` rows for the same package/class component are collapsed;
- ordering is deterministic by case-normalized label, original label, package name, then component class name;
- `AppId` remains package-scoped, so higher layers may treat multiple launch entries from one package as one app for organizer-owned state while still launching the selected component exactly.

The diagnostic calculates multiple-target packages automatically. A compact section at the top reports how many packages expose more than one launcher target and lists each package together with every discovered launcher component. The owner does not need to manually search the full launcher list to identify alias/multiple-activity candidates.

On the primary Samsung device the 566 targets across 564 packages are fully explained by two legitimate multi-target packages:

- Tasker exposes its normal launcher target plus Tasker Secondary;
- Daijisho exposes its normal launcher target plus DaiRescue.

Both secondary entries were launched successfully on-device and opened their intended targets. This closes the alias/multiple-launcher-entry acceptance case.

## Launching

`AndroidAppLauncher` validates the frozen target through a small pure `LaunchComponentSpec` seam, then builds a main-launcher intent for that exact `ComponentName` and adds `FLAG_ACTIVITY_NEW_TASK` because the adapter is intentionally created with application context. It returns `false` for blank target identity, missing activities, or security rejection.

The initial Samsung device pass exposed the missing `FLAG_ACTIVITY_NEW_TASK`: every target tap crashed before the external app could open. After the flag was added, the owner retested on-device and confirmed that tapping diagnostic targets opens the selected apps successfully.

The adapter does not expose `Intent`, `ComponentName`, `ResolveInfo`, `ApplicationInfo`, or `PackageManager` through the app-owned contracts.

The diagnostic host calls `finish()` after a successful external launch. Agent 50 should preserve that host-level behavior if direct return to One UI Home after leaving the launched app remains the desired product interaction; it should not be moved into `AppLauncher`, because task/host lifecycle is a presentation concern.

## Companion presentation

The selected spike presentation is a translucent, dim-behind activity containing a bottom sheet-shaped Compose diagnostic surface. It uses no blur API and adds no dependency.

The activity-specific `Theme.OneUIOrganizer.Translucent` sets:

- transparent window background;
- `windowIsTranslucent = true`;
- background dimming with a modest dim amount.

The host calls the existing Activity edge-to-edge API. `Theme.OneUIOrganizer` remains an ordinary non-translucent theme and is the normal edge-to-edge/fullscreen fallback if Samsung/Android window behavior proves the translucent host unreliable.

The diagnostic Compose screen is intentionally temporary. For physical validation it shows the multiple-target package summary first, followed by the entire lazily rendered target list with exact package/component identity under each label. Agent 40/50 can replace its contents without changing the platform source/launcher adapters or the host theme decision.

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

Physical acceptance on the owner's primary Samsung device is complete.

Confirmed:

- debug APK installs without changing the default HOME app;
- companion activity opens successfully from One UI Home;
- launcher scan completes off the main thread;
- translucent/dimmed presentation is usable and stable;
- scan reports **566 launcher targets across 564 distinct packages**;
- no discovered entry absent from the One UI app drawer could be identified;
- representative expected Samsung, Google, third-party, game, and work/productivity apps are present;
- One UI Organizer itself is excluded from the discovered list;
- representative apps launch successfully through their exact components;
- Tasker/Tasker Secondary and Daijisho/DaiRescue account for the two additional launcher targets and both secondary targets work correctly;
- install/remove followed by reopen/resume refreshes the discovered set without a package observer/background service;
- repeated dismiss returns directly to One UI Home;
- after launching an external app, the finished Organizer host does not remain as a stale foreground surface;
- rotate/display-mode testing leaves the selected presentation usable.

## Samsung / Android 16 acceptance checklist

1. Install the debug APK without changing launcher/default-HOME settings. **Passed.**
2. Open One UI Organizer from One UI Home and confirm the translucent/dimmed sheet presentation is visually acceptable. **Passed.**
3. Compare the full diagnostic list against ordinary user-launchable apps visible in One UI, checking both false positives and representative omissions. **Passed.**
4. Confirm One UI Organizer itself does not appear in the discovered target list. **Passed.**
5. Launch at least one Samsung app, one Google app, one third-party app, one game, and one work/productivity app from the diagnostic list. **Passed.**
6. Test reported alias/multiple-launcher-entry candidates and confirm exact targets open as intended. **Passed:** Tasker Secondary and DaiRescue both work.
7. Install a small test app, resume or reopen Organizer, and confirm it appears without a package observer/background service. **Passed.**
8. Remove that test app, resume or reopen Organizer, and confirm it disappears. **Passed.**
9. Repeat open/dismiss several times and confirm dismiss returns directly to One UI Home. **Passed.**
10. Launch a sampled app, navigate back/exit it, and confirm the finished Organizer host does not unexpectedly remain as a stale foreground surface. **Passed.**
11. Rotate/change display mode if applicable and confirm the translucent host remains usable; otherwise select the normal fallback. **Passed.**
12. Record device result, discovered target count, sampled behavior, alias result, presentation choice, and deviations before merge. **Passed in this document and PR.**

**Milestone-1 platform spike physical-device acceptance is complete for PR #3.**
