# Engineering Baseline and Upgrade Policy

**Policy date:** 2026-09-08

This document defines how One UI Organizer is built, tested, benchmarked, and structured so that toolchain and dependency upgrades remain routine instead of becoming migration projects.

The companion version inventory lives in [STABLE_BASELINE.md](STABLE_BASELINE.md). This document defines **how those tools and libraries must be used**.

## 1. Core policy

1. Use the **latest stable release that is inside the vendors' documented compatibility ranges**.
2. Do not use alpha, beta, RC, milestone, EAP, nightly, snapshot, or dynamic `+` versions in normal development.
3. Keep build/runtime JDK, compile toolchain, bytecode target, Android SDK level, and device runtime as separate explicit choices.
4. Pin versions centrally in `gradle/libs.versions.toml` or another single build-policy location; feature modules must not invent versions.
5. Keep the build warning-free from the first commit. Do not create warning baselines for a greenfield project.
6. Third-party APIs must not spread through the codebase merely because they are convenient. Library-specific types stay at integration boundaries unless the dependency is itself the platform contract (for example AndroidX Compose in UI code).
7. Upgrades are their own changes. Avoid mixing framework/toolchain upgrades with product features unless the feature requires the upgrade.
8. Every dependency must have an owner, a purpose, and an exit path.

## 2. JVM and Android toolchain model

One UI Organizer intentionally uses **two JDK roles**.

### 2.1 Gradle daemon / build runtime

Use **JDK 26** to run Gradle and Android build tooling.

Gradle 9.7 supports running on JVM 17 through 26. JDK 27 is not supported by that Gradle line, so do not jump to 27 merely because it exists.

The repository must commit `gradle/gradle-daemon-jvm.properties`, generated with Gradle's `updateDaemonJvm` task, so local machines and CI agree on the required daemon JVM instead of silently inheriting `JAVA_HOME`.

Preferred vendor: **Eclipse Temurin / Adoptium** for repeatable local and CI provisioning.

Conceptual generation command:

```text
./gradlew updateDaemonJvm --jvm-version=26 --jvm-vendor=adoptium
```

Use stable Foojay Toolchains Resolver Convention **1.0.0** in `settings.gradle.kts` to allow Gradle to provision missing JDK toolchains.

### 2.2 Android compile/test toolchain

Use an explicit **Java 17 compile/test toolchain** for Android production code and local JVM tests until Android officially documents and supports a newer Java bytecode/language baseline for the app's supported API range.

This is deliberately independent of the JDK 26 Gradle daemon.

Why:

- it prevents Android code from accidentally compiling against newer JDK APIs unavailable on Android;
- it keeps Java/Kotlin bytecode expectations explicit;
- it prevents machine-specific JDK defaults from changing output;
- moving Android bytecode from 17 to a later level becomes a single deliberate migration rather than a side effect of installing a new JDK.

Configure all of the following from one central convention:

```text
Java toolchain: 17
sourceCompatibility: 17
targetCompatibility: 17
Kotlin JVM target: 17
```

Kotlin 2.4.20 and AGP built-in Kotlin must not be allowed to infer a JVM target from the daemon JDK.

### 2.3 Toolchain rule

Never use `JAVA_HOME` as the project's source of truth.

`JAVA_HOME` may bootstrap Gradle, but the committed Daemon JVM criteria and explicit compile toolchain define the build.

## 3. Build system baseline

Use:

- Kotlin **2.4.20**;
- Compose compiler plugin **2.4.20**;
- Android Gradle Plugin **9.3.1**;
- Gradle Wrapper **9.7.0**;
- JDK 26 daemon;
- Java 17 Android compilation target;
- `compileSdk 37`;
- `targetSdk 36` until Android 17/API 37 is final and device acceptance is complete;
- AGP-managed Build Tools rather than unnecessary manual Build Tools pinning;
- no NDK unless native code becomes a real requirement.

Kotlin 2.4.20 documents full compatibility through Gradle 9.7.0 and AGP 9.3.1. AGP 9.4.0 is stable but remains outside that fully-supported Kotlin range on the policy date, so 9.3.1 is the correct baseline under this project's compatibility rule.

## 4. Build configuration principles

### 4.1 Kotlin DSL only

Use `settings.gradle.kts` and `build.gradle.kts`. Do not mix Groovy and Kotlin build scripts.

### 4.2 Version catalog

Use `gradle/libs.versions.toml` from the first implementation commit.

Rules:

- exact versions only;
- group related first-party artifacts with BOMs when the vendor provides a stable BOM;
- Compose uses the stable Compose BOM;
- aliases describe the library role, not implementation trivia;
- no version strings in feature source sets or module build files unless a Gradle API requires it;
- plugin versions also live centrally where Gradle permits it.

### 4.3 Dependency verification

Enable Gradle dependency verification and commit `gradle/verification-metadata.xml` once the initial dependency graph is established.

Use SHA-256 verification for downloaded artifacts. Updating a dependency must update verification metadata in the same dependency-upgrade change.

### 4.4 Repositories

Repository declarations belong in `settings.gradle.kts` with repository mode that prevents subprojects from adding arbitrary repositories.

Expected normal repositories:

- Google Maven;
- Maven Central;
- Gradle Plugin Portal for plugins.

Do not add JitPack or custom repositories unless a documented dependency decision justifies them.

### 4.5 Configuration/build cache

Keep Gradle configuration cache and build cache compatible from the beginning. A plugin that prevents configuration-cache use needs a documented reason or a replacement.

Do not depend on task names/internal AGP classes from feature code or ad-hoc Gradle scripts.

## 5. Compiler and warning policy

### Kotlin

Project Kotlin compiler warnings are errors in CI.

Do not suppress warnings globally. A local suppression must:

- be as narrow as possible;
- include a comment when the reason is not obvious;
- be removed when the underlying API is updated.

Do not opt into experimental Kotlin/compiler features merely because they are present in the current compiler.

### Java

Compile with the explicit Java 17 source/target settings. Avoid Java source unless it materially improves an Android integration; Kotlin is the project language.

### Gradle

CI runs an additional build/configuration check with Gradle deprecation warnings treated as failures (`--warning-mode=fail`). Local builds should show all warnings rather than hide them.

This intentionally catches plugin/build-script deprecations while there are still few enough to fix immediately.

## 6. Static analysis and formatting

### 6.1 Android Lint — primary semantic Android analysis

Use the Lint version bundled with the selected AGP.

Policy:

- `abortOnError = true`;
- warnings are treated as errors in CI;
- no checked-in lint baseline for a greenfield project;
- do not disable checks globally to make CI green;
- suppress a check only at the narrowest valid scope with a reason.

A lint baseline may only be introduced later by an explicit debt decision if importing legacy/generated code makes immediate cleanup impractical.

### 6.2 ktlint

Use:

- ktlint engine **1.8.0**;
- `org.jlleitschuh.gradle.ktlint` **14.2.0**.

Pin the ktlint engine explicitly; do not rely on the Gradle plugin's changing default engine version.

`.editorconfig` is the formatting contract. Formatting rules must not be duplicated in IDE-only settings.

CI runs `ktlintCheck`; developers may use `ktlintFormat` locally.

### 6.3 Detekt

**Do not add Detekt yet.**

The newest Detekt 2.x line is still alpha on the policy date. Stable Detekt 1.23.8 predates the current Kotlin/AGP generation and adding it now would violate the intent of using a clean modern toolchain.

Re-evaluate Detekt when 2.0 reaches stable. Until then, Kotlin compiler diagnostics + Android Lint + ktlint provide the static-analysis baseline without introducing an old or pre-release analyzer.

## 7. Dependency health and update tooling

### 7.1 Dependency update discovery

Use stable `io.github.ben-manes.versions.settings` **0.61.0**.

Configure it to report **stable releases only**. Alpha/beta/RC milestones must be rejected by the update filter.

The report is advisory: it may say a newer stable version exists, but the compatibility matrix still decides whether the project can adopt it.

### 7.2 Dependency usage analysis

Use `com.autonomousapps.dependency-analysis` **3.19.1**.

`buildHealth` is part of the engineering gate once the scaffold exists.

The project should fail or require explicit documented exceptions for:

- unused direct dependencies;
- used transitive dependencies that should be direct;
- dependencies on the wrong Gradle configuration;
- duplicate classes;
- unnecessary processors/plugins.

This reinforces the rule that code must not accidentally depend on implementation details pulled in transitively.

### 7.3 Automated update PRs

Use Dependabot or Renovate for dependency and GitHub Actions updates, but configure it to:

- ignore pre-release versions by default;
- separate build-tool/framework upgrades from normal library upgrades where practical;
- avoid bundling unrelated major upgrades into one PR;
- never auto-merge major framework/toolchain changes;
- run the complete verification lane on every update PR.

## 8. Testing toolchain

The test pyramid should use the cheapest layer capable of proving the behavior.

### 8.1 Pure unit tests

Use:

- Kotlin test assertions from Kotlin **2.4.20**;
- `kotlinx-coroutines-test` **1.11.0**;
- JUnit-compatible Gradle test execution supplied by the Kotlin/Android stack.

Prefer fakes over mocking frameworks. Do not add Mockito/MockK by default.

Unit-test targets include:

- categorization precedence;
- deterministic known-app rules;
- user override persistence behavior;
- search normalization/filtering;
- sorting;
- category model migrations;
- repository state transitions.

Tests must not need Android framework types for domain behavior.

### 8.2 Compose UI tests

Use Compose UI test artifacts from Compose BOM **2026.08.00**.

Compose tests should assert semantics and user-visible behavior rather than implementation node hierarchy wherever possible.

Do not couple tests to internal composable function structure merely because it is easy.

### 8.3 Android instrumentation

Stable AndroidX test baseline:

- Test Core **1.7.0**;
- Test Runner **1.7.0**;
- Test Rules **1.7.0**;
- ext.junit **1.3.0**;
- Espresso **3.7.0** where Espresso is useful;
- UI Automator **2.4.0** for app-launch/system/cross-app behavior.

UI Automator 2.4's newer `uiAutomator` / `onElement` API is preferred over writing new tests against legacy selectors when the modern API fits.

Instrumentation tests cover things unit/Compose tests cannot prove, especially:

- discovering real launcher activities;
- launching external apps;
- package aliases/duplicate launcher entries;
- system permission/visibility behavior;
- returning to One UI Organizer after external launches;
- Samsung-specific sheet/window behavior.

### 8.4 Robolectric

Do not add Robolectric initially. If an Android framework behavior can be covered cheaply with a fake boundary or on-device test, prefer that. Add Robolectric only for a specific test gap.

### 8.5 Device matrix

At minimum before a release candidate:

- the primary current Samsung/One UI device running Android 16;
- one AOSP emulator/Gradle Managed Device on the minimum supported API (28) for compatibility smoke testing;
- one current AOSP API device for platform behavior independent of Samsung;
- API 37 testing once Android 17 is final and `targetSdk` is raised.

Samsung device acceptance remains mandatory because the product is intentionally One UI-adjacent.

## 9. Performance and benchmark toolchain

### 9.1 Macrobenchmark

Use AndroidX Macrobenchmark **1.4.1** in a dedicated `:benchmark` or `:baselineprofile` `com.android.test` module when performance measurement is introduced.

Primary measurements:

- cold startup to usable shelf;
- warm startup;
- initial installed-app scan;
- cached/resumed scan;
- search response on a realistically-sized app catalog;
- category-list scroll/frame performance;
- open shelf → launch external app path.

Benchmarks are measured on device, not inferred from local JVM test time.

### 9.2 Microbenchmark

Use AndroidX Benchmark **1.4.1** only for isolated hot code where Macrobenchmark cannot explain a regression, for example classification of a synthetic 1,000-app catalog.

Do not microbenchmark ordinary repository/UI code for sport.

### 9.3 Baseline Profiles

Macrobenchmark/ProfileInstaller stable components are currently **1.4.1**.

However, the stable Baseline Profile Gradle plugin **1.4.1** has compatibility friction with the AGP 9 new DSL, while the newer 1.5 line is still pre-release. Under this project's stable-only rule:

- do **not** adopt the pre-release 1.5 plugin;
- do **not** disable AGP's modern DSL just to retain an old profile plugin;
- start with Macrobenchmark 1.4.1;
- add Baseline Profile generation when a stable plugin line cleanly supports the selected AGP generation.

This is a deliberate compatibility deferral, not forgotten performance work.

When Baseline Profiles are enabled later, use ProfileInstaller **1.4.1 or the then-current stable version**, generate profiles for real critical user journeys, and benchmark before/after rather than assuming the profile improved performance.

### 9.4 Release optimization

Release builds use R8 optimization/minification and optimized resource shrinking. Keep rules live in AGP's modern keep-rule source-set/optimization DSL rather than accumulating one giant historical ProGuard file.

Use AGP's R8 Configuration Analyzer when keep rules become non-trivial.

## 10. CI verification lanes

The eventual GitHub Actions workflow should have distinct lanes rather than one opaque `build` job.

### Fast PR lane

Run on every PR:

1. Gradle wrapper validation / setup;
2. build configuration with deprecations failing;
3. ktlint check;
4. Android Lint;
5. dependency `buildHealth`;
6. unit tests;
7. debug compilation/assembly;
8. Compose/instrumentation tests that can run reliably on CI-managed devices.

### Release-quality lane

Additionally run before release/tagging:

1. release build with R8;
2. instrumentation/device acceptance matrix;
3. Macrobenchmark smoke/regression checks once benchmarks exist;
4. artifact inspection (version, min/target SDK, signing mode, size);
5. dependency verification must pass with no regenerated metadata;
6. no build/lint/compiler warnings.

### Upgrade compatibility lane

For toolchain-update PRs, explicitly print and archive:

```text
java -version
./gradlew --version
Android Gradle Plugin version
Kotlin version
compileSdk / targetSdk / minSdk
```

This turns future toolchain debugging into comparison rather than archaeology.

## 11. GitHub Actions policy

Use stable official actions only. Pin production workflow actions to immutable commit SHAs while annotating the human-readable release tag in comments.

At the policy date, stable lines include:

- `actions/checkout` **7.0.1**;
- `gradle/actions/setup-gradle` **6.2.0**.

`actions/setup-java` is allowed only as a bootstrap/CI convenience; the repository's Gradle Daemon JVM criteria and Java toolchains remain authoritative.

Do not duplicate Gradle caches with both `setup-java` cache and `setup-gradle` cache. Prefer Gradle's dedicated action for Gradle caching/wrapper validation.

Dependabot/Renovate should update action SHAs and annotated versions.

## 12. Upgrade-friendly coding rules

These rules are mandatory for implementation work.

### 12.1 Keep domain models app-owned

Domain/model packages must not expose:

- `PackageInfo`, `ResolveInfo`, `ApplicationInfo`, `Drawable`, `Intent`;
- DataStore classes;
- serialization library internals;
- Material/Compose UI state classes;
- future database/network library classes.

Instead map external data into small app-owned models such as `InstalledApp`, `AppId`, `AppCategory`, and `OrganizerState`.

### 12.2 Platform APIs behind narrow boundaries

Android integration belongs behind interfaces sized around what the app needs, not around everything Android can do.

Example conceptual boundaries:

```text
InstalledAppSource
AppLauncher
OrganizerStateStore
PinnedCategoryShortcutManager (later)
UsageSignalSource (later)
```

Do not create a giant `AndroidManager` abstraction.

### 12.3 External-library types stop at adapters

When a third-party library is added, its types may exist inside its adapter/integration package, but should not become the app's domain vocabulary.

Example: if storage later moves from DataStore to Room, category logic and ViewModels should not need rewriting because they depend on `OrganizerStateStore`, not DataStore/Room objects.

### 12.4 Compose is allowed in UI, not domain

Compose/Material are the chosen UI platform, so composables naturally depend on them. Domain/repository layers do not.

Keep an app-owned design-token/theme layer around Material 3 values that are intentionally customized for the One UI-inspired appearance. Screens should prefer project theme/tokens rather than scattering raw Material defaults and magic dimensions.

This makes Material upgrades and design changes centralized.

### 12.5 Coroutines are the app async contract

Use Kotlin `suspend`, `Flow`, and immutable state as asynchronous boundaries where appropriate.

Do not expose third-party callback/future/reactive types across layers. If a future library uses RxJava, Guava futures, callbacks, etc., adapt it at the boundary.

### 12.6 Persistence format is versioned and app-owned

Persist app-owned DTO/state with an explicit schema/version field and migration tests.

Do not serialize arbitrary framework/library classes directly. A library upgrade must not silently redefine the on-disk format.

### 12.7 No service locator globals

Dependencies are constructor-injected and assembled in a small composition root. Manual DI remains the default while the graph is small.

If Hilt/Koin is ever introduced, application/domain objects still use ordinary constructors so the DI framework can be replaced without rewriting business logic.

### 12.8 Do not rely on transitive dependencies

If code imports an artifact, declare it directly. `buildHealth` enforces this.

Never code against a dependency merely because another dependency currently brings it in.

### 12.9 One library per concern

Avoid simultaneous competing libraries for the same job (for example two JSON libraries or two image loaders) unless a migration is actively in progress.

### 12.10 Prefer first-party stable APIs

For Android/system behavior, prefer Android platform and AndroidX APIs over convenience wrappers when the wrapper adds little value. Every extra wrapper is another compatibility calendar.

### 12.11 Isolate experimental platform APIs

If Android introduces a new API needed by the app, isolate API-level checks and compatibility fallbacks in the platform adapter. Do not scatter `Build.VERSION` branches through UI/domain code.

### 12.12 Migration tests are mandatory for stored state

Any change to persisted organizer state must include tests proving:

- old state loads;
- migration is deterministic;
- user overrides are not lost;
- unknown/new fields have defined behavior;
- downgrade behavior is documented if downgrades are unsupported.

## 13. Dependency introduction checklist

Before adding any package/library/plugin, answer:

1. What product/test/build requirement does it solve?
2. Is there a stable first-party/platform solution already available?
3. What is the latest stable version?
4. Is that version explicitly compatible with our Kotlin/AGP/Gradle/JDK line?
5. Does it support configuration cache if it is a Gradle plugin?
6. What permissions, runtime size, startup cost, or processors does it add?
7. What library-specific types would enter our source code?
8. Can those types stay behind a narrow adapter?
9. How would we replace this dependency later?
10. What tests prove our behavior independently of the library implementation?

If these questions do not have good answers, do not add the dependency yet.

## 14. Upgrade procedure

For every significant toolchain/library upgrade:

1. read release notes and migration notes;
2. verify the new stable version against upstream compatibility tables;
3. update the central version catalog/baseline only;
4. update dependency verification metadata;
5. compile with zero warnings;
6. run formatting + lint + build health;
7. run unit and instrumentation tests;
8. run benchmark/regression lane when the change can affect performance;
9. test on the Samsung device for Android/Compose/AGP/API-level changes;
10. update `STABLE_BASELINE.md` with the new verified date and any deliberate deferrals.

A version upgrade is not complete while deprecation warnings are merely tolerated.

## 15. Current tooling inventory

| Concern | Stable choice | Version / status |
|---|---|---:|
| Gradle runtime JDK | Eclipse Temurin/Adoptium | **26** |
| Android compile/test JDK | explicit Java toolchain | **17** |
| Toolchain auto-provisioning | Foojay resolver convention | **1.0.0** |
| Gradle | Wrapper | **9.7.0** |
| Kotlin | Kotlin / KGP-compatible compiler | **2.4.20** |
| Android build | AGP | **9.3.1** |
| Formatting | ktlint | **1.8.0** |
| ktlint Gradle integration | ktlint-gradle | **14.2.0** |
| Android semantic analysis | AGP Android Lint | bundled with **9.3.1** |
| Dependency update report | Ben Manes settings plugin | **0.61.0** |
| Dependency usage health | Autonomous Apps Dependency Analysis | **3.19.1** |
| Coroutine testing | kotlinx-coroutines-test | **1.11.0** |
| Android Test Core/Runner/Rules | AndroidX Test | **1.7.0** |
| Android JUnit extension | AndroidX ext.junit | **1.3.0** |
| Espresso | AndroidX Espresso | **3.7.0** |
| Cross-app/system UI tests | UI Automator | **2.4.0** |
| Compose UI tests | Compose BOM | **2026.08.00** |
| Macro/micro benchmarks | AndroidX Benchmark | **1.4.1** |
| Profile installer | AndroidX ProfileInstaller | **1.4.1** |
| Baseline Profile plugin | stable **1.4.1**, but deferred for AGP 9 new-DSL compatibility | **do not add yet** |
| Detekt | 2.x is pre-release; 1.x deliberately not introduced | **deferred** |
| GitHub checkout action | actions/checkout | **7.0.1** |
| GitHub Gradle action | gradle/actions/setup-gradle | **6.2.0** |

## 16. Sources used for this policy

Primary vendor documentation:

- Kotlin/Gradle/AGP compatibility: https://kotlinlang.org/docs/gradle-configure-project.html
- Kotlin 2.4.20: https://kotlinlang.org/docs/whatsnew2420.html
- Android JDK/toolchain guidance: https://developer.android.com/build/jdks
- AGP releases/compatibility: https://developer.android.com/build/releases/about-agp
- AGP 9.3 release notes: https://developer.android.com/build/releases/agp-9-3-0-release-notes
- Gradle JVM compatibility: https://docs.gradle.org/current/userguide/compatibility.html
- Gradle Daemon JVM criteria: https://docs.gradle.org/current/userguide/gradle_daemon.html
- Gradle JVM toolchains: https://docs.gradle.org/current/userguide/toolchains.html
- Foojay resolver: https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
- AndroidX Test releases: https://developer.android.com/jetpack/androidx/releases/test
- UI Automator releases: https://developer.android.com/jetpack/androidx/releases/test-uiautomator
- AndroidX Benchmark releases: https://developer.android.com/jetpack/androidx/releases/benchmark
- Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
- Android Lint: https://developer.android.com/studio/write/lint
- ktlint: https://github.com/ktlint/ktlint/releases
- ktlint Gradle plugin: https://github.com/JLLeitschuh/ktlint-gradle/releases
- Detekt status: https://detekt.dev/changelog-2.0.0/
- Dependency Analysis plugin: https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis
- Ben Manes Versions plugin: https://plugins.gradle.org/plugin/io.github.ben-manes.versions.settings
- Gradle GitHub Actions: https://github.com/gradle/actions
- GitHub checkout action: https://github.com/actions/checkout/releases
