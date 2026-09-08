# Framework and Dependency Decisions

**Verified:** 2026-09-08

This document records **why** the project uses its chosen technologies.

- Exact current versions: [`STABLE_BASELINE.md`](STABLE_BASELINE.md)
- Toolchain, testing, benchmarking, coding-boundary and upgrade rules: [`ENGINEERING_BASELINE.md`](ENGINEERING_BASELINE.md)

Do not duplicate version pins into feature documentation. The stable baseline is the single version inventory.

## 1. Framework decision

Use **native Android with Kotlin + Jetpack Compose + Material 3**.

This app is intentionally Android-specific. Its useful work depends on Android platform APIs such as PackageManager, launcher activities, intents, shortcuts, widgets, profile behavior, and Samsung/One UI window integration. Native Kotlin avoids introducing a platform bridge around the app's core purpose.

Flutter remains a good UI framework, but for this product it would add a Dart/native boundary around functionality that is naturally Kotlin. React Native and other cross-platform stacks have the same disadvantage without giving the project a meaningful second platform.

Do not introduce Kotlin Multiplatform merely for theoretical portability. Pure domain code should remain platform-light where natural, so extraction remains possible later.

## 2. Stable-only development policy

One UI Organizer is a greenfield project and should not begin life with avoidable dependency debt.

The rules are:

- stable releases only;
- no alpha, beta, RC, Canary-only package, EAP, snapshot, milestone, nightly, or dynamic `+` dependency in normal development;
- use the newest stable **compatible** build-tool set rather than mixing individually newest releases outside vendor compatibility matrices;
- pin exact versions centrally;
- use stable BOMs where a vendor supplies one;
- treat build, compiler, deprecation and lint warnings as work to resolve, not permanent background noise;
- document every intentionally deferred stable upgrade and the compatibility reason blocking it;
- verify versions before the initial scaffold and before release milestones.

## 3. JVM/toolchain strategy

The project deliberately separates the JDK that runs the build from the Java bytecode level used by Android code.

- **Gradle daemon:** current supported stable JDK line (currently Temurin JDK 26).
- **Android compile/test toolchain:** explicit Java 17.
- **Java source compatibility:** 17.
- **Java target compatibility:** 17.
- **Kotlin JVM target:** 17.

The build runtime JDK may therefore move forward when Gradle supports it without silently raising Android's language/bytecode contract.

The repository will commit Gradle Daemon JVM criteria and use Gradle Java toolchains. `JAVA_HOME` is bootstrap information, not the project's source of truth.

## 4. Build-system direction

Use:

- Gradle Wrapper;
- Android Gradle Plugin with built-in Kotlin support;
- `org.jetbrains.kotlin.plugin.compose` matching the Kotlin release;
- Kotlin DSL only;
- one Gradle version catalog;
- centralized repositories;
- dependency verification;
- configuration-cache-compatible plugins;
- AGP-managed Build Tools unless a documented issue requires an explicit pin.

Do not depend on internal AGP task/classes or custom build hacks when a supported public DSL exists.

## 5. UI stack

Use Jetpack Compose + Material 3.

Compose is allowed to be a direct dependency of UI code because it is the selected UI platform. It must not leak into domain/repository contracts.

For One UI-inspired presentation, define project-owned theme/design tokens around intentional colors, shapes, dimensions, motion and typography instead of scattering raw Material defaults and magic numbers across screens. This provides one migration point when Material APIs or visual requirements change.

## 6. Persistence

Use **DataStore** for v0.1.

The initial persistent model is small: user category overrides, favourites, hidden apps, and later category metadata/order. A typed DataStore-backed state is sufficient and avoids SQL/KSP/schema machinery prematurely.

Persist **app-owned versioned DTO/state**, not DataStore/framework-specific types. `OrganizerStateStore` is the application boundary. A future move to Room must not require category logic or UI state to be rewritten.

Room is intentionally deferred until the product develops relational requirements such as many-to-many tags, large editable rulesets, history, or complex queries.

## 7. Concurrency

Use Kotlin coroutines and Flow as the app's asynchronous contract.

Rules:

- PackageManager scanning does not block the main thread;
- use lifecycle/repository-owned scopes;
- expose `suspend`, `Flow` and immutable app-owned state across boundaries;
- adapt callbacks/futures/other reactive types at integration boundaries rather than leaking them through the app;
- do not add a background service merely to keep the app list current;
- rescan on open/resume initially unless measurement proves another design necessary.

## 8. Dependency injection

Use **manual constructor injection** initially.

The expected graph is deliberately small. Dependencies are assembled in a narrow composition root; no service-locator globals.

If Hilt/Koin is introduced later, domain/application objects must remain ordinary constructor-injected Kotlin classes so the DI framework does not become the architecture.

## 9. Android platform boundaries

Android framework types remain in Android/platform adapters wherever practical.

The domain/application model should use app-owned types such as:

```text
AppId
InstalledApp
AppCategory
OrganizerState
```

rather than exposing types such as `ResolveInfo`, `ApplicationInfo`, `Intent`, `Drawable`, or package-manager objects throughout the project.

Likely narrow boundaries include:

```text
InstalledAppSource
AppLauncher
OrganizerStateStore
PinnedCategoryShortcutManager (later)
UsageSignalSource (later)
```

Avoid a giant generic `AndroidManager` abstraction.

## 10. Navigation

Do not add navigation to the initial one-surface platform spike.

Adopt the then-current stable Navigation 3 line only after the app has a second meaningful destination such as Settings, Hidden Apps, or Category Management.

## 11. App icons and images

Installed app icons come from Android. Do not add Coil/Glide merely to display local package icons.

Convert/adapt platform drawable/icon data at the Android/UI boundary. Introduce caching only if profiling identifies a real scrolling or decode cost.

The v0.1 app has no Internet permission.

## 12. Search

Use simple in-memory Kotlin filtering for the initial few-hundred-app data set.

Search covers app labels and category labels with deterministic normalization. Do not introduce AppSearch, SQLite FTS, or another indexing engine before measurement or product scope requires it.

## 13. Testing direction

Use the cheapest layer capable of proving each behavior:

- pure Kotlin unit tests for categorization, state/migrations, search and repository logic;
- Compose semantics tests for user-visible UI behavior;
- Android instrumentation for actual platform integration;
- UI Automator for cross-app/system/launcher flows;
- real Samsung acceptance for One UI-specific behavior.

Tests target app-owned contracts, not internal library object graphs. Prefer fakes over mocking frameworks.

Exact stable test tool versions and the device matrix live in the two baseline documents linked above.

## 14. Performance direction

Use Macrobenchmark for real end-to-end performance journeys and Microbenchmark only for isolated hotspots.

Performance work is measurement-driven. Do not add profile/benchmark infrastructure merely because it exists.

Baseline Profile tooling is adopted only when a **stable** plugin line cleanly supports the selected AGP generation; pre-release tooling does not get a special exemption from the stable-only policy.

## 15. Dependency introduction rule

Before adding any library/plugin, establish:

1. the concrete requirement it solves;
2. the latest stable release;
3. compatibility with the current Kotlin/AGP/Gradle/JDK line;
4. configuration-cache support for build plugins;
5. runtime/build/permission/processor cost;
6. what external types would enter our code;
7. the narrow adapter or boundary that contains those types;
8. how the dependency could be replaced later;
9. tests that prove our behavior independently of that library.

Do not add packages speculatively.

## 16. Decision summary

Start with a **current, warning-free Kotlin + Compose Android stack** and keep it current through small isolated upgrades.

The key long-term rule is more important than any one version number: **build tools move forward independently, application contracts remain app-owned, external dependencies live behind controlled boundaries, and deprecations are fixed while they are small.**
