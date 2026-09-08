# Framework and Dependency Decisions

**Verified:** 2026-09-08

The authoritative exact-version list is [`STABLE_BASELINE.md`](STABLE_BASELINE.md). This document explains the architectural choices behind that baseline.

## 1. Framework decision

Use **native Android with Kotlin + Jetpack Compose + Material 3**.

This app is intentionally Android-specific. Its useful work depends on Android platform APIs such as PackageManager, launcher activities, intents, shortcuts, widgets, profile behavior, and Samsung/One UI window integration. Native Kotlin avoids introducing a platform bridge around the app's core purpose.

Flutter remains a good UI framework, but for this product it would add a Dart/native boundary around functionality that is naturally Kotlin. React Native and other cross-platform stacks have the same disadvantage without giving the project a meaningful second platform.

Do not introduce Kotlin Multiplatform merely for theoretical portability. Pure domain code should remain platform-light where natural, so extraction remains possible later.

## 2. Stable-only development policy

One UI Organizer is a greenfield project and should not begin life with avoidable dependency debt.

The rules are:

- stable releases only;
- no alpha, beta, RC, Canary-only package, EAP, snapshot, milestone, or dynamic `+` dependency in normal application development;
- use the newest stable **compatible** build-tool set rather than mixing individually newest releases outside vendor compatibility matrices;
- pin exact versions in a Gradle version catalog;
- use the stable Compose BOM;
- treat new build and deprecation warnings as work to resolve, not permanent background noise;
- document any intentionally deferred stable upgrade and the compatibility reason blocking it;
- verify versions before the initial scaffold and before release milestones.

See [`STABLE_BASELINE.md`](STABLE_BASELINE.md) for the current exact versions.

## 3. Current build baseline

As of 2026-09-08:

```text
Android Studio: Quail 4 / 2026.1.4 stable
Gradle runtime JDK: JDK 26
Kotlin: 2.4.20
Compose compiler plugin: 2.4.20
Android Gradle Plugin: 9.3.1
Gradle Wrapper: 9.7.0
compileSdk: 37
targetSdk: 36
minSdk: 28
```

### Why JDK 26?

The project must not confuse Android's source/bytecode compatibility level with the JDK used to run Gradle.

Gradle 9.7 supports running on JDK 26. Android's build documentation also recommends explicitly selecting a project toolchain rather than accidentally inheriting the machine's ambient JDK.

Therefore the development/build environment uses **JDK 26**, while Android bytecode compatibility remains an explicit independent setting appropriate for the Android platform.

This avoids needlessly freezing the whole development environment on JDK 17.

### Why not AGP 9.4.0 yet?

AGP 9.4.0 is individually stable and current, but Kotlin 2.4.20 currently documents full AGP compatibility only through 9.3.1. Because this project explicitly wants a clean compatibility baseline, AGP 9.4 is tracked as a pending stable upgrade rather than adopted ahead of the compatibility matrix.

### Why Gradle 9.7.0 rather than 9.7.1?

Gradle 9.7.1 is the current stable patch and Gradle recommends it, but Kotlin 2.4.20's currently published fully-supported range explicitly tops out at 9.7.0. The project pins 9.7.0 until that compatibility statement catches up, then upgrades immediately.

## 4. Core UI stack

Use:

```text
Compose BOM 2026.08.00
Compose UI/Foundation/Runtime 1.12.0 via BOM
Material 3 1.4.0 via BOM
Activity Compose 1.13.0
Lifecycle 2.11.0
AndroidX Core 1.19.0
```

Compose 1.12 requires compileSdk 37 and AGP 9+, which the baseline satisfies.

Use `org.jetbrains.kotlin.plugin.compose` at exactly the Kotlin version. Since AGP 9+ has built-in Kotlin support, do not apply the obsolete `org.jetbrains.kotlin.android` plugin unless a documented compatibility reason requires opting out of built-in Kotlin.

## 5. Persistence

Use **DataStore 1.2.1** for v0.1.

The first persistent model is small: user category overrides, favourites, hidden apps, and later category metadata/order. A typed DataStore state is sufficient and avoids introducing SQL, KSP, and schema machinery prematurely.

Use **kotlinx.serialization 1.11.0** where typed serialization is needed.

Room is intentionally deferred until the product actually develops relational requirements such as many-to-many tags, large editable rulesets, history, or complex queries.

## 6. Concurrency

Use **kotlinx.coroutines 1.11.0** and the matching **kotlinx-coroutines-test 1.11.0**.

Rules:

- PackageManager scanning must not block the main thread;
- use lifecycle/repository-owned scopes;
- use Flow/StateFlow for observable organizer state where useful;
- do not add a background service merely to keep the app list current;
- rescan on open/resume in the first version unless measurement proves another design necessary.

## 7. Dependency injection

Use **manual constructor injection** initially.

The expected object graph is small:

```text
PackageInstalledAppSource
CategoryEngine
OrganizerStateStore
OrganizerRepository
OrganizerViewModel
```

Do not add Hilt, Dagger, or Koin until the object graph becomes large enough that a framework clearly reduces complexity rather than adding it.

## 8. Navigation

Do not add a navigation dependency to the initial platform spike.

Add Navigation 3 only when the app has a second meaningful destination such as Settings, Hidden Apps, or Category Management. At the time it is introduced, re-check the then-current stable Navigation 3 release rather than carrying an unused pinned dependency from project creation.

## 9. App icons and images

Installed application icons are local Android Drawables supplied by PackageManager.

Do not add Coil or Glide merely to display them. Adapt platform drawables at the UI boundary and add caching only if profiling shows it is needed.

The v0.1 app should have **no Internet permission**.

## 10. Search

Use simple in-memory Kotlin filtering over the discovered app model.

Search should cover at least:

- case-insensitive app labels;
- category labels;
- trimmed input.

Do not add AppSearch, SQLite FTS, or another indexing system for a data set of a few hundred applications.

## 11. Widgets and shortcuts

Use Android's platform ShortcutManager APIs for dynamic and pinned shortcuts.

When widgets become part of scope, the current stable planned library is **AndroidX Glance 1.2.0**, but Glance is not an initial dependency.

## 12. Testing

Use stable first-party/Kotlin tools where practical:

```text
AndroidX Test Core 1.7.0
AndroidX Test ext.junit 1.3.0
Espresso 3.7.0
UI Automator 2.4.0
Compose UI tests via Compose BOM 2026.08.00
kotlinx-coroutines-test 1.11.0
```

Prefer fakes over a mocking framework for package-source, category, and persistence boundaries. Add Robolectric, mocking, snapshots, or other test frameworks only when a specific test cannot be expressed cleanly without them.

## 13. Later performance tooling

When startup/scroll performance becomes worth measuring, use stable AndroidX Benchmark. At this policy date the stable line is **1.4.1**.

Do not add benchmark/profile modules before there is a real flow to measure.

## 14. Initial dependency footprint

The first implementation slice should be intentionally small:

```text
AndroidX Core
Activity Compose
Lifecycle
Compose BOM
Compose UI/Foundation/Runtime
Material 3
DataStore
kotlinx.coroutines
kotlinx.serialization

Tests:
Compose UI test artifacts
kotlinx-coroutines-test
AndroidX Test / Espresso as required
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
Navigation
```

## Decision summary

Start One UI Organizer on a **modern JDK 26 development environment**, Kotlin + Compose, and the newest stable dependency versions that form a vendor-supported combination.

The project explicitly prefers **continuous small upgrades** over letting the build become frozen around an old JDK or library generation. The exact current pins and upgrade rules live in [`STABLE_BASELINE.md`](STABLE_BASELINE.md).
