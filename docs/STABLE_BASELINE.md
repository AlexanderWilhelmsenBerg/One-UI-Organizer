# Stable Development Baseline

**Policy date:** 2026-09-08

This file is the **authoritative exact-version inventory** for One UI Organizer.

For the rules governing how these tools and libraries are used, isolated, tested, benchmarked, and upgraded, see [ENGINEERING_BASELINE.md](ENGINEERING_BASELINE.md).

## Policy

The project uses **latest stable releases only**. Alpha, beta, RC, milestone, EAP, preview, nightly, snapshot, and dynamic `+` versions are not allowed in normal development.

One qualification is intentional: mutually dependent build tools must also be inside their vendors' documented compatibility ranges. If the individually newest stable versions are not yet documented as fully compatible, use the newest fully supported combination and record the newer stable release as deferred.

## 1. Build and JVM toolchains

| Component | Baseline | Reason / status |
|---|---:|---|
| Android Studio | **Quail 4 / 2026.1.4** | Current stable Android Studio at policy date |
| Gradle daemon JDK vendor | **Eclipse Temurin / Adoptium** | Reproducible OpenJDK distribution for local and CI |
| Gradle daemon JDK | **26.0.2.1+1** | Latest stable Temurin JDK 26 security update; Gradle supports running on JVM through 26 |
| Android compile/test Java toolchain | **Java 17** | Explicit Android language/bytecode baseline; independent from daemon JDK |
| Foojay Toolchains Resolver Convention | **1.0.0** | Latest stable; provisions required JDK toolchains |
| Kotlin | **2.4.20** | Current stable Kotlin |
| Compose Compiler Gradle plugin | **2.4.20** | Match Kotlin version |
| Android Gradle Plugin | **9.3.1** | Highest AGP fully supported by Kotlin 2.4.20 |
| Gradle Wrapper | **9.7.0** | Highest Gradle fully supported by Kotlin 2.4.20 |
| compileSdk | **37** | Current compile API required by stable Compose line |
| targetSdk | **36 initially** | Raise to 37 after Android 17 final + acceptance testing |
| minSdk | **28** | Product compatibility decision |
| Android SDK Build Tools | **AGP-managed** | Do not pin unless a concrete build issue requires it |
| NDK | **Not used** | No native code requirement |

### The JDK split is deliberate

Do not treat "the project's JDK" as one setting.

- **JDK 26.0.2.1+1** runs Gradle/build tooling.
- **Java 17 toolchain** compiles Android production code and local JVM tests.
- `sourceCompatibility`, `targetCompatibility`, and Kotlin JVM target are explicitly **17**.
- The repository must commit Gradle Daemon JVM criteria so CI and developers do not silently inherit whatever `JAVA_HOME` happens to point at.

This lets the development/build environment stay current without accidentally producing Android code against Java 26 APIs or bytecode.

## 2. Core Kotlin / Compose dependencies

| Dependency | Stable baseline |
|---|---:|
| Compose BOM | **2026.08.00** |
| Compose UI | **1.12.0** via BOM |
| Compose Foundation | **1.12.0** via BOM |
| Compose Runtime | **1.12.0** via BOM |
| Material 3 | **1.4.0** via BOM |
| Activity / activity-compose | **1.13.0** |
| Lifecycle | **2.11.0** |
| AndroidX Core | **1.19.0** |
| DataStore | **1.2.1** |
| kotlinx.coroutines | **1.11.0** |
| kotlinx.serialization | **1.11.0** |

Use the stable Compose BOM rather than independently pinning Compose artifacts.

## 3. Test toolchain

| Dependency / tool | Stable baseline | Intended use |
|---|---:|---|
| Kotlin test APIs | **2.4.20** | Pure unit assertions |
| kotlinx-coroutines-test | **1.11.0** | Deterministic coroutine/Flow tests |
| Compose UI test artifacts | **BOM 2026.08.00** | Compose semantics/interaction tests |
| AndroidX Test Core | **1.7.0** | Instrumentation support |
| AndroidX Test Runner | **1.7.0** | Instrumentation runner |
| AndroidX Test Rules | **1.7.0** | Android test rules |
| AndroidX ext.junit | **1.3.0** | JUnit integration |
| Espresso | **3.7.0** | In-app instrumentation UI tests where useful |
| UI Automator | **2.4.0** | Cross-app, system UI, launcher/app-launch tests |
| Android Test Orchestrator | **1.6.1** | Optional isolation if instrumentation suite needs it |

Prefer fakes over a mocking framework. MockK/Mockito/Robolectric are not baseline dependencies; add one only to solve a demonstrated test gap.

## 4. Formatting, lint and dependency health

| Concern | Stable tool | Version / policy |
|---|---|---:|
| Android semantic/static analysis | Android Lint | **Bundled with AGP 9.3.1** |
| Kotlin formatting/lint engine | ktlint | **1.8.0** |
| ktlint Gradle integration | `org.jlleitschuh.gradle.ktlint` | **14.2.0** |
| Dependency update discovery | `io.github.ben-manes.versions.settings` | **0.61.0** |
| Dependency usage analysis | `com.autonomousapps.dependency-analysis` | **3.19.1** |
| Detekt | Deferred | **Do not add until Detekt 2.x reaches stable** |

Greenfield quality policy:

- Kotlin/compiler warnings fail CI;
- Gradle deprecations fail the dedicated warning check;
- Android Lint warnings/errors fail CI;
- no lint baseline initially;
- no global warning suppressions;
- `ktlintCheck` is a PR gate;
- dependency `buildHealth` is a PR gate;
- dependency update reports reject prerelease candidates.

## 5. Benchmark and performance tooling

| Feature | Stable baseline | Adoption |
|---|---:|---|
| AndroidX Macrobenchmark | **1.4.1** | Add dedicated benchmark module when primary flow exists |
| AndroidX Microbenchmark | **1.4.1** | Only for isolated hot code where useful |
| ProfileInstaller | **1.4.1** | Use when baseline/profile flow is adopted |
| Baseline Profile Gradle plugin | **1.4.1 stable exists, but deferred** | Do not adopt until a stable line cleanly supports the selected AGP 9/new-DSL setup |
| R8 | **AGP-managed** | Enabled/optimized for release builds |

The newer Benchmark 1.5 line is release-candidate/pre-release on the policy date and therefore is not allowed under the stable-only rule.

Performance acceptance will focus on real user journeys: cold/warm shelf startup, installed-app scanning, search latency, category scrolling/frame timing, and shelf-to-external-app launch.

## 6. Stable libraries reserved for later product features

These are **not initial dependencies**. Re-check stable versions again at adoption time.

| Feature | Current stable reference |
|---|---:|
| Home-screen widgets | AndroidX Glance **1.2.0** |
| Navigation | Navigation 3 **1.1.7**; add only when a second real destination exists |
| Database | Room 3 **3.0.2**; add only if DataStore no longer fits |
| DI | Manual constructor injection initially; re-check Hilt/Dagger if graph complexity justifies it |

## 7. CI/build infrastructure baseline

| Tool | Stable line / policy |
|---|---:|
| GitHub checkout action | `actions/checkout` **7.0.1** |
| Gradle GitHub Action | `gradle/actions/setup-gradle` **6.2.0** |
| GitHub Actions dependencies | Pin to **immutable commit SHA**, annotate release tag in comments |
| Gradle dependency verification | **SHA-256 metadata committed** |
| Gradle configuration cache | **Required compatible** |
| Gradle build cache | **Enabled where appropriate** |
| Build scripts | **Kotlin DSL only** |
| Version management | **`gradle/libs.versions.toml`** |

The exact `setup-java` action version is not part of the authoritative toolchain: CI Java setup is bootstrap plumbing. Committed Gradle Daemon JVM criteria plus Gradle Java toolchains remain the source of truth.

## 8. Known newer/pre-release versions intentionally not selected

### AGP 9.4.0

AGP **9.4.0** is stable, but Kotlin 2.4.20 currently documents full AGP compatibility only through **9.3.1**. Upgrade when a stable Kotlin line expands that compatibility range.

### Gradle 9.7.1

Gradle **9.7.1** is a newer stable patch, but Kotlin 2.4.20's published fully-compatible maximum is **9.7.0**. Stay on 9.7.0 until Kotlin's compatibility matrix catches up or a newer stable Kotlin release supports the newer Gradle line.

### JDK 27

JDK 27 is not yet a supported Gradle runtime in the current stable Gradle compatibility table. Do not move until both a GA JDK 27 exists and the pinned stable Gradle/Kotlin/AGP stack supports it.

### Benchmark 1.5.x

Current 1.5 releases are pre-release/RC. Stay on **1.4.1** until 1.5 reaches stable and passes the project compatibility lane.

### Detekt 2.x

Detekt 2.x is pre-release. Do not introduce old Detekt 1.x just to have a second analyzer in a new project; re-evaluate when 2.x is stable.

## 9. Version policy for future development

Every dependency/tool change follows these rules:

1. **Stable only.** No prerelease or dynamic versions.
2. **Newest stable compatible.** Compatibility matrices outrank version-number vanity.
3. **Exact central pins.** Versions live in the catalog/build policy, not scattered through modules.
4. **No silent debt.** Every deliberately deferred stable upgrade has a recorded reason.
5. **Built-in Kotlin.** AGP 9+ built-in Kotlin is the default; do not reintroduce `org.jetbrains.kotlin.android` without a documented reason.
6. **Compose compiler matches Kotlin.** `org.jetbrains.kotlin.plugin.compose` uses the same version as Kotlin.
7. **No warning baseline.** Deprecation/compiler/lint warnings are fixed, not accumulated.
8. **Upgrade PRs are isolated.** Prefer dependency/toolchain upgrades separate from product features.
9. **Re-check before scaffolding and every release milestone.** This file is a living baseline, not a time capsule.
10. **Code for replacement.** Follow [ENGINEERING_BASELINE.md](ENGINEERING_BASELINE.md) so external library/platform types remain at controlled boundaries.

## 10. Initial implementation footprint

The first real implementation slice should remain deliberately small:

```text
Build/runtime
- Temurin JDK 26.0.2.1+1 for Gradle
- Java 17 Android compile/test toolchain
- Foojay resolver 1.0.0
- Kotlin 2.4.20
- Compose compiler plugin 2.4.20
- AGP 9.3.1
- Gradle 9.7.0
- compileSdk 37 / targetSdk 36 / minSdk 28

Runtime/UI
- AndroidX Core 1.19.0
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- Compose BOM 2026.08.00
- Material 3 1.4.0 via BOM
- DataStore 1.2.1
- kotlinx.coroutines 1.11.0
- kotlinx.serialization 1.11.0

Quality/tests
- ktlint 1.8.0 + Gradle plugin 14.2.0
- Android Lint from AGP 9.3.1
- Dependency Analysis 3.19.1
- Versions settings plugin 0.61.0
- AndroidX Test Core/Runner/Rules 1.7.0
- ext.junit 1.3.0
- Espresso 3.7.0
- UI Automator 2.4.0
- Compose UI tests via BOM
- kotlinx-coroutines-test 1.11.0
```

Not initially required:

```text
Room / KSP
Hilt / Koin
WorkManager
Retrofit / Ktor / OkHttp
Coil / Glide
Paging / AppSearch
Robolectric / MockK / Mockito
Detekt
Navigation
Glance
Baseline Profile plugin
```

## 11. Upstream references

- Kotlin Gradle/AGP compatibility: https://kotlinlang.org/docs/gradle-configure-project.html
- Kotlin 2.4.20: https://kotlinlang.org/docs/whatsnew2420.html
- Android Java/JDK configuration: https://developer.android.com/build/jdks
- Gradle Java compatibility: https://docs.gradle.org/current/userguide/compatibility.html
- Gradle Daemon JVM criteria/toolchains: https://docs.gradle.org/current/userguide/gradle_daemon.html and https://docs.gradle.org/current/userguide/toolchains.html
- Eclipse Temurin releases: https://adoptium.net/
- AGP releases: https://developer.android.com/build/releases/about-agp
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- AndroidX Test: https://developer.android.com/jetpack/androidx/releases/test
- UI Automator: https://developer.android.com/jetpack/androidx/releases/test-uiautomator
- AndroidX Benchmark: https://developer.android.com/jetpack/androidx/releases/benchmark
- Foojay toolchain resolver: https://plugins.gradle.org/plugin/org.gradle.toolchains.foojay-resolver-convention
- ktlint: https://github.com/ktlint/ktlint/releases
- ktlint Gradle plugin: https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint
- Dependency Analysis: https://plugins.gradle.org/plugin/com.autonomousapps.dependency-analysis
- Versions plugin: https://plugins.gradle.org/plugin/io.github.ben-manes.versions.settings
