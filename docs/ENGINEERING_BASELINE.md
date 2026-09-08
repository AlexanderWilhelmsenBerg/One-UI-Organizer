# Engineering Baseline

**Policy date:** 2026-09-08

This document defines how One UI Organizer is built, tested, analyzed, benchmarked and kept upgradeable. Exact versions live in [`STABLE_BASELINE.md`](STABLE_BASELINE.md); if a version here ever differs, `STABLE_BASELINE.md` wins.

## 1. Engineering goals

The engineering baseline exists to make upgrades boring rather than heroic.

Principles:

1. **Separate runtime roles.** Gradle's JVM, Android bytecode level, device API level and Kotlin language/compiler version are different concerns.
2. **Prefer official stable tooling.** Do not solve ordinary Android problems with preview toolchains or abandoned plugins.
3. **Pin what changes the build.** Toolchains and direct dependencies must be reproducible.
4. **Own boundaries, not frameworks.** Domain/application contracts belong to this app; external libraries stay behind narrow adapters.
5. **Tests follow boundaries.** Pure logic is tested without Android; Android integration is tested only where Android behavior matters.
6. **Warnings are migration signals.** Greenfield warnings are fixed, not normalized.
7. **Performance is measured.** Add benchmark machinery when there is a stable flow worth measuring.

## 2. JDK and JVM toolchains

### 2.1 Gradle daemon JDK

Run Gradle itself on **Eclipse Temurin / Adoptium JDK 26.0.2.1+1**.

Commit Gradle Daemon JVM criteria so a clean checkout does not silently use whatever JDK happens to launch the wrapper:

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

`JAVA_HOME` may bootstrap Gradle, but the committed Daemon JVM criteria and explicit compile/tool execution toolchains define the build.

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

Use ktlint engine **1.8.0**.

Stable `org.jlleitschuh.gradle.ktlint` **14.2.0** remains the tracked plugin version, but the initial scaffold does not apply it. Agent 00 verified that the plugin launches ktlint in a process-isolated worker using the Gradle daemon JVM and exposes no Java-launcher selector. With the required JDK 26 daemon, ktlint 1.8.0's embedded compiler emits terminal `sun.misc.Unsafe::objectFieldOffset` deprecation warnings.

Instead, use ktlint's documented custom Gradle `JavaExec` integration and set that task's `javaLauncher` to the existing Java 17 toolchain. This keeps Gradle itself on JDK 26, keeps the exact stable ktlint engine, and avoids a global JVM warning-suppression flag.

`.editorconfig` is the formatting contract. Formatting rules must not be duplicated in IDE-only settings.

CI runs `:app:ktlintCheck`; developers may use `:app:ktlintFormat` locally. Re-evaluate the plugin when a stable release can select the worker JVM or no longer emits the warning on JDK 26.

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
- stale-app cleanup rules.

### 8.2 Android-local tests

Avoid Robolectric by default. Android framework behavior that cannot be represented by a fake should usually move to instrumentation instead of simulating the whole framework locally.

### 8.3 Compose UI tests

Use Compose's official test APIs. Keep UI state and event handlers injectable so most shelf behavior can be tested without real package scanning or persistence.

### 8.4 Instrumentation tests

Use AndroidX Test + UI Automator on an emulator/real device where Android behavior matters.

UI Automator is particularly appropriate for validating that tapping a shelf item leaves Organizer and opens the intended external app.

Use Android Test Orchestrator only if isolation becomes useful; do not add it to the initial graph by default.

### 8.5 Test execution discipline

Every PR should run the cheap verification lane:

```text
unit tests
Android Lint
ktlint
buildHealth
Gradle warning/deprecation check
configuration-cache smoke
```

Run instrumentation when a PR touches Android integration/UI behavior.

Run physical-device acceptance when a PR changes:

- package visibility/discovery;
- launching external apps;
- window/translucency behavior;
- API-level behavior that the emulator cannot represent reliably.

## 9. Benchmarking and performance

### 9.1 Do not benchmark an empty app

Do not add a benchmark module merely because modern Android projects often have one. A benchmark without a stable user journey measures scaffolding noise.

### 9.2 Adopt stable AndroidX Benchmark when the flow exists

When primary flows exist, add:

- Macrobenchmark **1.4.1**;
- Microbenchmark **1.4.1** only where isolated hot code needs it;
- ProfileInstaller **1.4.1**;
- UI Automator **2.4.0** for system-boundary benchmark setup where helpful.

Keep benchmark modules isolated from app/domain code. Benchmarking is a consumer of public app behavior, not an excuse to add benchmark hooks into production logic.

### 9.3 Baseline Profiles

AndroidX Baseline Profile Gradle plugin **1.4.1** is stable, but do not add it in the initial scaffold.

The current Baseline Profile plugin stable line does not yet integrate cleanly enough with the selected AGP 9 generation/new DSL to justify workarounds in a greenfield repository.

Re-evaluate when a stable plugin line supports the selected AGP generation without deprecated DSL or warning debt.

### 9.4 Performance gates

Do not invent microsecond thresholds before measurement. Establish a baseline on the primary Samsung device first.

Candidate journeys:

1. cold start to first usable cached shelf;
2. cold start to first categorized scan;
3. rescan after app install/remove;
4. search filtering latency;
5. category expansion/collapse;
6. long-press action latency;
7. shelf-to-external-app launch latency;
8. scroll frame timing for large app sets.

Track regressions relative to the measured baseline. A noisy benchmark is advisory; a stable repeated regression can become a gate.

## 10. Dependency and API design for upgrades

The goal is to make dependency upgrades local.

### 10.1 App-owned models at boundaries

Do not expose external implementation types in domain/application contracts.

Examples:

- scanner adapter converts `ApplicationInfo`/`ResolveInfo` to `InstalledApp`;
- persistence adapter converts DataStore/serialization DTOs to `OrganizerState`;
- UI converts app-owned models to Compose presentation state;
- external app launch accepts `LaunchTargetId`, not a raw `Intent`.

### 10.2 Constructor injection

Use ordinary constructor injection and small interfaces. Do not introduce Hilt/Koin solely to avoid writing constructors.

A future DI framework should replace only composition wiring, not application contracts.

### 10.3 No transitive-dependency coding

If code imports a type from a library, that library should normally be declared directly in the module where the import occurs.

Do not rely on one dependency pulling another into the classpath by accident.

### 10.4 Isolate framework adapters

Keep Android/package scanning, persistence, and presentation-specific APIs behind small adapters. This allows independent upgrades of AndroidX, DataStore, Compose or future persistence choices.

### 10.5 Experimental APIs

Do not opt into experimental APIs globally. If a future stable feature requires an experimental API, isolate the opt-in to the smallest possible file/class and document why the product needs it.

## 11. Automated dependency maintenance

Use update automation only as discovery and PR creation, not as policy.

Recommended split:

- Kotlin / AGP / Gradle / Compose compiler upgrades in dedicated toolchain PRs;
- Compose BOM upgrades separately;
- AndroidX library upgrades grouped conservatively;
- testing-tool upgrades separately;
- GitHub Actions upgrades separately.

Every update PR must pass the same build/test/Lint/ktlint/dependency-health gates as feature work.

## 12. What not to add yet

Do **not** add:

- Detekt 2.x alpha;
- stable Detekt 1.x just to have a second analyzer;
- KSP unless a stable dependency actually requires code generation;
- Room without a measured persistence need;
- Hilt/Koin without DI graph complexity;
- Retrofit/Ktor/OkHttp without a network requirement;
- baseline profile plugin until its stable line cleanly supports the selected AGP generation;
- benchmarking modules before the primary product flow exists;
- mocking frameworks by default;
- experimental Compose libraries simply because they are new.

## 13. Upgrade checklist

When upgrading a library/toolchain:

1. Confirm the candidate release is stable.
2. Check upstream compatibility ranges, not only release date.
3. Update central version declarations only.
4. Regenerate dependency verification metadata.
5. Run clean compile and unit tests.
6. Run Android Lint with warnings treated as errors.
7. Run ktlint.
8. Run dependency `buildHealth`.
9. Run Gradle with deprecation warnings failing.
10. Verify configuration-cache reuse.
11. Run instrumentation/device tests when the affected library crosses Android/UI/system boundaries.
12. Check generated APK/release behavior if packaging changes.
13. Record any remaining warning/deprecation before merge; greenfield changes should normally have none.

## 14. Useful verification commands

The scaffold should expose tasks so the normal local lane is approximately:

```text
./gradlew clean assembleDebug testDebugUnitTest lintDebug :app:ktlintCheck buildHealth --warning-mode=fail
./gradlew dependencyUpdates
./gradlew dependencies
./gradlew javaToolchains
./gradlew --version
```

The exact task names may change slightly when the scaffold is implemented; keep this section aligned with reality.
