# Framework and Dependency Decisions

**Verified:** 2026-09-08

This project is unusually Android-specific: its core job is discovering Android launcher activities, reading package metadata, launching explicit components, integrating with Android shortcuts/widgets later, and presenting naturally over Samsung One UI. Framework choice should optimize those facts rather than hypothetical portability.

## 1. Framework choice

| Choice | Status | Advantages | Costs / risks | Decision |
|---|---|---|---|---|
| **Native Android: Kotlin + Jetpack Compose** | Current stable Android stack | Direct PackageManager/Intent/ShortcutManager access; smallest platform boundary; best Android lifecycle/window integration; excellent testing; no bridge | Android-only | **Recommended** |
| Native Android: Kotlin + XML Views | Very mature | Maximum historical Android compatibility; direct platform access | More UI boilerplate; less attractive for a new small app; no benefit for this Compose-friendly UI | Do not choose for a greenfield app |
| Flutter 3.47 | Current stable Flutter release line | Strong UI toolkit; fast iteration; cross-platform potential | Package discovery, explicit Android launching, translucent activity details, shortcuts and widgets still require Android platform-channel/native code; larger conceptual stack for an Android-only utility | Viable, not recommended |
| React Native 0.87 | Current stable RN release | Strong ecosystem; React/TypeScript development model | Core feature still needs native Android modules; JS/runtime/tooling adds another layer with no product benefit | Viable, not recommended |
| Kotlin Multiplatform / Compose Multiplatform | Modern Kotlin option | Could share domain logic later | There is no second platform requirement and almost all valuable integration is Android-specific | Do not introduce in v0.1 |

### Framework decision

Use **native Kotlin + Jetpack Compose + Material 3**.

The project should be Android-first rather than "portable by architecture ceremony." Pure category rules and models should naturally remain platform-light, but the project should not pay a multiplatform tax until a second platform actually exists.

## 2. Build-tool choices

There are two sensible stable tracks as of 2026-09-08.

### Option A — compatibility-first stable stack — **recommended**

- Kotlin / Kotlin Gradle Plugin: **2.4.20**
- Android Gradle Plugin: **9.3.1**
- Gradle: **9.5.0**
- JDK toolchain: **17**
- compileSdk: **37**
- targetSdk: **37**
- minSdk: **28**

Why this is the default:

- Kotlin 2.4.20 documents full support through AGP 9.3.1 and Gradle 9.7.0.
- AGP 9.3 requires Gradle 9.5.0.
- Compose 1.12 requires only AGP 9.1.2 or newer, so this track comfortably supports the current stable Compose release.
- It avoids starting the project on an individually-stable but not-yet-listed-as-fully-supported Kotlin/AGP pairing.

### Option B — newest individually stable Android build stack

- Kotlin: **2.4.20**
- Android Gradle Plugin: **9.4.0**
- Gradle: **9.6.0** (AGP default/minimum) or **9.7.0** (newer stable Gradle)
- JDK: **17**

Why not make this the default immediately:

AGP 9.4.0 is the newest stable Android Gradle Plugin, but Kotlin's current compatibility table lists Kotlin 2.4.20 as fully supported only through AGP 9.3.1. Kotlin notes that newer AGP versions can still work, but may produce warnings or expose unsupported-new-feature edges. There is no feature in v0.1 that needs AGP 9.4 specifically.

**Upgrade rule:** move to AGP 9.4 once Kotlin's fully supported range catches up, or earlier only if a concrete AGP 9.4 feature/fix matters to this app.

## 3. Core UI stack

### Compose BOM — **2026.08.00** — recommended

Use the stable Compose BOM rather than pinning each Compose artifact independently.

The August 2026 stable line maps the core Compose libraries to 1.12.x and gives a tested compatible set.

### Compose UI/Foundation/Runtime — **1.12.0** through BOM

Use for all primary UI.

### Material 3 — **1.4.0** through BOM

Use as the component baseline, then layer a small app-owned token system for One UI-inspired proportions/spacing.

Do not use Material 3 alpha purely for visual novelty.

### Activity Compose — **1.13.0**

Use `ComponentActivity` and Compose activity integration.

### Lifecycle — **2.11.0**

Use ViewModel/lifecycle Compose integrations as needed.

### Core — **1.19.0**

Use `androidx.core:core`; `core-ktx` is now effectively compatibility-only because Kotlin extensions have been folded into Core.

## 4. Navigation choice

### Choice A — no navigation dependency for the first spike — **recommended initially**

Milestone 1 needs essentially one diagnostic/shelf surface. Do not add a navigation framework before a second meaningful destination exists.

### Choice B — Navigation 3 **1.1.7** — **recommended when navigation becomes necessary**

Navigation 3 is stable, Compose-first, and appropriate for a greenfield Compose project.

Likely adoption point: when Settings / Hidden Apps / Category Management become real destinations.

### Choice C — Navigation 2 **2.10.0**

Stable and mature, but Navigation 3 is the better greenfield Compose direction. Choose Navigation 2 only if an integration gap is discovered during implementation.

## 5. Persistence choice

### Choice A — DataStore **1.2.1** + kotlinx.serialization **1.11.0** — **recommended for v0.1**

Store a small typed organizer-state document containing user-owned state such as:

- category overrides;
- favourites;
- hidden apps;
- category order/custom category metadata when introduced.

Benefits:

- simple;
- transactional update semantics;
- no SQL schema;
- no annotation processor;
- no KSP requirement;
- easy to test and migrate with an explicit state version.

### Choice B — Room 3 **3.0.2** — new stable, intentionally deferred

Room 3 is a strong option if the data model later becomes relational: multiple tags per app, rule editing, history, complex sorting/filtering, or large user-authored rule sets.

Do **not** add Room simply because it is available. Room 3 requires KSP and a SQLite driver, which is unnecessary for the initial state volume.

### Choice C — Preferences DataStore only

Mature and simple for a handful of independent values, but a growing graph of package overrides/categories becomes awkward. Prefer one typed state model rather than dozens of string-key conventions.

## 6. Concurrency

### kotlinx.coroutines — **1.11.0** — recommended

Use coroutines/Flow for scanner work, repository state, and ViewModel state.

Rules:

- PackageManager scanning must not block the main thread.
- Keep long-lived scopes lifecycle/repository owned.
- Do not create a background worker/service solely to keep the app list current; rescan on open/resume in v0.1.

Use `kotlinx-coroutines-test` at the same version for deterministic coroutine tests.

## 7. Dependency injection

### Choice A — manual constructor injection — **recommended**

The initial graph is tiny:

```text
PackageInstalledAppSource
CategoryEngine
OrganizerStateStore
OrganizerRepository
OrganizerViewModel
```

Wire these explicitly. This is clearer than adding a framework to save a few constructor calls.

### Choice B — Hilt

AndroidX Hilt integrations are stable at **1.4.0**. Introduce Hilt only if the object graph genuinely grows across services, workers, multiple feature modules, or many ViewModels.

If Hilt is adopted later, re-check the current compatible Dagger/Hilt processor versions at that time rather than freezing an unused processor dependency now.

### Choice C — Koin

Reasonable lightweight alternative, but still unnecessary for the v0.1 graph. Avoid adding a service locator/DI runtime without a concrete benefit.

## 8. Image/icon loading

Do **not** add Coil/Glide solely to display installed application icons.

Package icons are local Android `Drawable`s. Adapt them at the Android/UI boundary and cache only if measurements show a real scrolling cost.

If remote images ever become a product requirement, reevaluate an image-loading library then. v0.1 has no network permission anyway.

## 9. Search

Start with pure in-memory Kotlin filtering over the discovered/categorized app model.

Normalization should cover at least:

- case-insensitive matching;
- trimmed input;
- app label;
- category label.

Do not add AppSearch, SQLite FTS, or a search framework until the data volume proves it useful.

## 10. Widgets and shortcuts — later

### Android shortcuts

Use platform `ShortcutManager` APIs for dynamic/pinned category shortcuts. No third-party dependency is needed.

### Glance — **1.2.0**

If/when the app gains a home-screen widget, use stable AndroidX Glance rather than a custom RemoteViews abstraction unless a concrete Glance limitation blocks the design.

## 11. Performance tooling — later hardening

### AndroidX Benchmark — **1.4.1** stable

A newer 1.5 release candidate exists, but the project should use stable Benchmark when macrobenchmarking becomes valuable.

Add benchmark/profile tooling only after the primary flow exists; measuring an empty scaffold is impressive only to spreadsheets.

## 12. Testing package policy

Prefer first-party / Kotlin tooling where practical:

- `kotlin-test` / JUnit-compatible unit tests;
- `kotlinx-coroutines-test:1.11.0`;
- Compose UI test artifacts through the Compose BOM;
- AndroidX Test runner/core at the stable versions current when the implementation scaffold is created.

Avoid adding a second assertion framework, mocking framework, Robolectric, or snapshot-testing package until a test case demonstrates why it is needed. Fakes are preferred for `InstalledAppSource` and persistence boundaries.

## 13. Version-management policy

Use a Gradle version catalog (`gradle/libs.versions.toml`) from the first implementation commit.

Rules:

1. Pin exact stable versions; no `+` dynamic versions.
2. Use the stable Compose BOM.
3. Do not use alpha/beta/RC libraries in production code unless a documented decision names the required feature/fix.
4. Separate "latest available" from "latest fully compatible stack." Prefer the latter by default.
5. Dependency-update PRs should be isolated from feature work where practical.
6. Record intentional deferred upgrades when compatibility—not neglect—is the reason for staying one minor version back.

## 14. Recommended initial dependency footprint

For the first real implementation slice, aim for roughly this conceptual set:

```text
AndroidX Core 1.19.0
Activity Compose 1.13.0
Lifecycle 2.11.0
Compose BOM 2026.08.00
Compose UI/Foundation/Material3 (via BOM)
DataStore 1.2.1
kotlinx.coroutines 1.11.0
kotlinx.serialization 1.11.0

Tests:
Compose UI test artifacts (via BOM)
kotlinx-coroutines-test 1.11.0
AndroidX Test stable line
```

Not initially required:

```text
Room / KSP
Hilt / Koin
WorkManager
Retrofit / Ktor / OkHttp
Coil / Glide
Paging
AppSearch
Glance
Benchmark
Navigation (until a second destination exists)
```

## 15. Sources checked for this decision

- Android Gradle Plugin releases and compatibility: https://developer.android.com/build/releases/about-agp
- AGP 9.4 release notes: https://developer.android.com/build/releases/agp-9-4-0-release-notes
- Kotlin Gradle compatibility: https://kotlinlang.org/docs/gradle-configure-project.html
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- AndroidX current versions: https://developer.android.com/jetpack/androidx/versions
- Navigation 3 releases: https://developer.android.com/jetpack/androidx/releases/navigation3
- DataStore releases: https://developer.android.com/jetpack/androidx/releases/datastore
- Room 3 releases: https://developer.android.com/jetpack/androidx/releases/room3
- Flutter release archive: https://docs.flutter.dev/install/archive
- React Native releases: https://reactnative.dev/blog/

## Decision summary

Start with the **smallest fully supported native stack**:

**Kotlin 2.4.20 + AGP 9.3.1 + Gradle 9.5.0 + JDK 17 + Compose BOM 2026.08.00 + Material 3 + DataStore + coroutines, with manual DI and no navigation dependency until it is actually needed.**

This gives the app modern Android APIs and current stable Compose while deliberately avoiding a just-released AGP/Kotlin compatibility edge and unnecessary infrastructure.
