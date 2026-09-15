# Engineering Baseline

## 1. Purpose

This document defines how One UI Organizer is built, tested, analyzed, benchmarked and kept upgradeable. Exact versions live in [`STABLE_BASELINE.md`](STABLE_BASELINE.md); if a version here ever differs, `STABLE_BASELINE.md` wins.

The engineering baseline exists to make upgrades boring rather than heroic.

Core principles:

1. **Compatibility beats novelty.** Use the newest stable version inside documented compatibility ranges.
2. **Centralize versions.** Exact versions belong in the version catalog, wrapper or committed toolchain criteria.
3. **One owner per concern.** Avoid parallel state/config implementations.
4. **Adapters contain volatility.** Android/platform/provider details stay at the edge.
5. **Tests prove behavior at the cheapest valid layer.**
6. **Warnings are migration signals.** Greenfield warnings are fixed, not normalized.
7. **Performance is measured.** Add benchmark machinery when there is a stable flow worth measuring.

## 2. JDK and JVM toolchains

### 2.1 Gradle daemon JDK

Run Gradle with JDK **26**.

Commit Gradle Daemon JVM criteria so a clean checkout does not silently use whatever JDK happens to launch the wrapper:

```text
gradle/gradle-daemon-jvm.properties
```

Prefer Eclipse Temurin / Adoptium for reproducibility.

Use stable Foojay Toolchains Resolver Convention **1.0.0** in `settings.gradle.kts` to allow Gradle to provision missing JDK toolchains.

### 2.2 Android compile/test toolchain

Use an explicit **Java 17 compile/test toolchain** for Android production code and local JVM tests until Android officially documents and supports a newer Java bytecode/language baseline for the app's supported API range.

The build therefore intentionally separates:

```text
Gradle daemon: JDK 26
Android/Kotlin compilation: Java 17
JVM unit tests: Java 17
ktlint CLI: Java 17
```

Do not “simplify” this by moving everything to JDK 26 unless the Android/Kotlin/AGP compatibility evidence changes.

### 2.3 Kotlin JVM target

Use JVM target **17**.

```text
Java toolchain: 17
Kotlin jvmTarget: 17
```

### 2.4 JAVA_HOME

`JAVA_HOME` may bootstrap Gradle, but the committed Daemon JVM criteria and explicit compile/tool execution toolchains define the build.

## 3. Build system baseline

### 3.1 Gradle / AGP / Kotlin

Use the exact versions from `STABLE_BASELINE.md`.

The supported combination is intentionally conservative around compatibility rather than choosing the individually newest stable component in isolation.

Kotlin 2.4.20 documents full compatibility through Gradle 9.7.0 and AGP 9.3.1. AGP 9.4.0 is stable but remains outside that fully-supported Kotlin range on the policy date, so 9.3.1 is the correct baseline under this project's compatibility rule.

### 3.2 Android SDK policy

`minSdk`, `targetSdk`, and compile SDK are product/toolchain decisions, not cleanup knobs.

For the current baseline, stable Compose BOM **2026.08.00 / Compose 1.12** requires API 37, while Android API **37.0** is still distributed as the **Cinnamon Bun Preview** SDK. CI therefore uses stable Android command-line tools build **15859902** and its non-deprecated `android` CLI to install only `platforms/android-37.0@2.0.0` from the beta SDK channel. AGP 9.3 models the actual SDK minor version with `compileSdk = 37` plus `compileSdkMinor = 0`.

Rules:

- preview Android SDK platforms are allowed only when a selected latest-stable AndroidX/Compose release requires that compile API and no final SDK exists;
- the exception is compile-time only;
- do not extend the exception to preview Kotlin, AGP, Gradle, Android Studio, Maven libraries, plugins, emulator images, or other runtime/tooling inputs.

### 3.3 Gradle configuration cache

The project must remain configuration-cache compatible.

CI runs with:

```text
--configuration-cache
--configuration-cache-problems=fail
```

and explicitly verifies reuse.

### 3.4 Build cache

Use Gradle build caching where appropriate. Do not add custom remote caches without a separate infrastructure decision.

### 3.5 Repositories

Use only:

- Google Maven;
- Maven Central;
- Gradle Plugin Portal for plugins.

Do not add JitPack or custom repositories unless a documented dependency decision justifies them.

### 4.3 Dependency verification

Enable Gradle dependency verification and commit `gradle/verification-metadata.xml` once the initial dependency graph is established.

Use SHA-256 verification for downloaded artifacts. Updating a dependency must update verification metadata in the same dependency-upgrade change.

## 5. Compiler and warning policy

### Kotlin

Kotlin compiler warnings are errors in CI.

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

`NewerVersionAvailable` is the one deliberate CI-policy exception: dependency freshness is time-dependent external state, not a property of the PR source. It is excluded from deterministic Android Lint and is instead reported by the scheduled/manual `Dependency freshness` workflow using the stable-only `dependencyUpdates` policy. Actual upgrades remain dedicated reviewed changes and must still satisfy the full upgrade checklist.

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

Every PR runs the deterministic verification lane:

```text
debug + release assembly
unit tests
Android Lint (source/semantic checks; dependency freshness is separate)
ktlint
buildHealth
Gradle warning/deprecation check
configuration-cache reuse
built-APK merged-manifest/privacy audit
```

The application now has stable Compose/navigation/backup/shortcut flows, so permanent PR CI also executes the existing Android instrumentation suite on a stable API-36 emulator. Compiling `assembleDebugAndroidTest` is not acceptance by itself. Instrumentation artifacts are retained for diagnosis. Physical Samsung acceptance remains required where One UI or real package/launcher behavior matters.

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

### 10.1 Prefer official stable APIs

Prefer AndroidX/platform APIs over third-party wrappers when the official API is adequate.

### 10.2 Stable interfaces at boundaries

Domain and application layers should depend on app-owned contracts rather than Android or third-party types.

### 10.3 No transitive-dependency coding

Do not rely on one dependency pulling another into the classpath by accident.

### 10.4 Keep volatility at the edge

Keep Android/package scanning, persistence, and presentation-specific APIs behind small adapters. This allows independent upgrades of AndroidX, DataStore, Compose or future persistence choices.

### 10.5 Experimental APIs

Do not opt into experimental APIs globally. If a future stable feature requires an experimental API, isolate the opt-in to the smallest possible file/class and document why the product needs it.

The preview API-37.0 compile platform is not an experimental-API opt-in. Product code remains on the existing runtime/target contract until a separate change approves otherwise.

## 11. Automated dependency maintenance

Use update automation only as discovery and PR creation, not as policy.

Recommended split:

- Kotlin / AGP / Gradle / Compose compiler upgrades in dedicated toolchain PRs;
- Compose BOM upgrades separately;
- AndroidX library upgrades grouped conservatively;
- testing-tool upgrades separately;
- GitHub Actions upgrades separately.

Every update PR must pass the same build/test/Lint/ktlint/dependency-health gates as feature work.

Dependency discovery itself runs separately on a schedule and on demand. The discovery workflow is advisory and must not be a required PR check: publishing a new upstream version must not turn an unchanged feature branch red. The report identifies maintenance work; adoption still follows the dedicated upgrade procedure and compatibility rules.

## 12. What not to add yet

Do **not** add:

- preview SDK packages other than the explicitly approved API-37.0 compile platform;
- Detekt 2.x alpha;
- stable Detekt 1.x just to have a second analyzer;
- KSP unless a stable dependency actually requires code generation;
- Room without a measured persistence need;
- Hilt/Koin without DI graph complexity;
- a second JSON library while `kotlinx.serialization` is sufficient;
- baseline profile plugin until its stable line cleanly supports the selected AGP generation;
- benchmarking modules before the primary product flow exists;
- screenshot test frameworks before the product has stable visual acceptance cases;
- mocking frameworks unless a demonstrated test gap cannot be solved cleanly with fakes.

## 13. Upgrade checklist

When upgrading a library/toolchain:

1. Verify release notes and migration notes.
2. Confirm compatibility with Gradle, AGP, Kotlin, Compose and JDK/toolchains.
3. Update the version catalog or other central version source.
4. Regenerate dependency verification metadata.
5. Run a clean build.
6. Run Android Lint with warnings treated as errors.
7. Run ktlint.
8. Run dependency `buildHealth`.
9. Run Gradle with deprecation warnings failing.
10. Verify configuration-cache reuse.
11. Run instrumentation/device tests when the affected library crosses Android/UI/system boundaries.
12. Compare relevant benchmark evidence for performance-sensitive changes.
13. Record any remaining warning/deprecation before merge; greenfield changes should normally have none.
14. Update `STABLE_BASELINE.md` when the authoritative baseline changes.

## 14. Useful verification commands

```bash
./gradlew clean :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck buildHealth --warning-mode=fail --dependency-verification=strict --configuration-cache --configuration-cache-problems=fail
./gradlew connectedDebugAndroidTest --warning-mode=fail --dependency-verification=strict
./gradlew dependencyUpdates --warning-mode=fail --dependency-verification=strict
```

The permanent CI additionally provisions the approved API-37.0 compile platform from the Android SDK beta channel before running Gradle. Keep this section aligned with reality.
