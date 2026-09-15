# Stable Baseline

## 1. Policy

This file is the **authoritative exact-version inventory** for One UI Organizer.

The project uses **latest stable releases only** for Maven artifacts, libraries, Kotlin, AGP, Gradle, Android Studio, Gradle plugins and other normal build/runtime dependencies. Alpha, beta, RC, milestone, EAP, preview, nightly, snapshot, and dynamic `+` versions are not allowed in those dependency/tool categories.

One qualification is intentional: mutually dependent stable build tools must also be inside their vendors' documented compatibility ranges. If the individually newest stable versions are not yet documented as fully compatible, use the newest fully supported combination and record the newer stable release as deferred.

A preview **Android SDK platform** may be installed only when a selected latest-stable AndroidX/Compose release requires that compile API and no final SDK platform is available yet. This is a compile-time platform exception, not a general preview-dependency exception.

The current approved exception is **Android API 37.0 / Cinnamon Bun Preview**, package `platforms/android-37.0@2.0.0`, because stable Compose BOM **2026.09.00 / Compose 1.12.1** requires API 37 while the platform is still distributed through the Android SDK beta channel. CI installs only that SDK platform using stable Android command-line tools and the current non-deprecated `android` CLI.

## 2. Core toolchain

| Concern | Exact baseline | Notes |
|---|---:|---|
| Gradle | **9.7.0** | Wrapper distribution pinned with SHA-256 |
| Gradle daemon JDK | **26** | Committed daemon JVM criteria |
| Gradle daemon JDK vendor | **Eclipse Temurin / Adoptium** | Reproducible OpenJDK distribution for local and CI |
| Android compile/test Java toolchain | **17** | Explicit Gradle Java toolchain |
| Kotlin JVM target | **17** | Matches Android/Java baseline |
| Kotlin | **2.4.20** | Current stable, fully supported with AGP 9.3.1 / Gradle 9.7.0 |
| Android Gradle Plugin | **9.3.1** | Newest stable inside Kotlin 2.4.20's fully-supported AGP range |
| Compose Compiler Gradle plugin | **2.4.20** | Match Kotlin version |
| Android Studio | **Otter 4 Feature Drop 2026.2.4** | Current stable IDE line |
| Android command-line tools | **15859902** | Stable tools archive used by CI/API-37 provisioning |
| Android SDK platform | **37.0 / Cinnamon Bun Preview (`platforms/android-37.0@2.0.0`)** | Compile-only exception required by current stable Compose |
| Android platform-tools | **37.0.1** | Current stable adb/platform tools |
| minSdk | **28** | Product support decision |
| targetSdk | **36** | Current product target decision |
| compileSdk | **37.0** | AGP `compileSdk = 37`, `compileSdkMinor = 0` |

Notes:

- Gradle itself runs on JDK 26.
- Android/Kotlin compilation and JVM unit tests use Java 17.
- ktlint's CLI process also uses the provisioned **Java 17 toolchain**; this keeps the JDK 26 daemon current without globally suppressing the terminal `sun.misc.Unsafe` warning emitted by ktlint 1.8.0's embedded compiler on JDK 26.
- The repository commits Gradle Daemon JVM criteria so CI and developers do not silently inherit whatever `JAVA_HOME` happens to point at.

## 3. Stable libraries and testing tools

| Library / tool | Version | Role |
|---|---:|---|
| Compose BOM | **2026.09.00** | Compose dependency alignment |
| Compose runtime/UI/foundation/material3 | **BOM-managed** | UI implementation |
| AndroidX Activity | **1.13.0** | Activity integration |
| AndroidX Lifecycle | **2.11.0** | Lifecycle integration/reference baseline |
| AndroidX Core | **1.19.0** | Core AndroidX baseline/reference |
| AndroidX DataStore | **1.2.1** | Local persisted state |
| Kotlin Coroutines | **1.11.0** | Async/state flows |
| Kotlin Serialization | **1.11.0** | App-owned persistence/backup JSON |
| Kotlin test APIs | **2.4.20** | Pure unit assertions |
| kotlinx-coroutines-test | **1.11.0** | Deterministic coroutine/Flow tests |
| Compose UI test artifacts | **BOM 2026.09.00** | Compose semantics/interaction tests |
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
| Kotlin formatting/lint engine | ktlint CLI | **1.8.0**, executed with the Java 17 toolchain |
| ktlint Gradle plugin | `org.jlleitschuh.gradle.ktlint` | **14.2.0 stable, tracked but not applied while its worker cannot select Java 17 under the JDK 26 daemon** |
| Dependency update discovery | `io.github.ben-manes.versions.settings` | **0.61.0** |
| Dependency usage analysis | `com.autonomousapps.dependency-analysis` | **3.19.1** |
| Detekt | Deferred | **Do not add until Detekt 2.x reaches stable** |

The initial scaffold verified a compatibility mismatch in the otherwise-stable ktlint pair: plugin 14.2.0 launches ktlint in a process-isolated worker using the Gradle daemon JVM and exposes no worker Java-launcher selector. Under JDK 26, ktlint 1.8.0's embedded Kotlin compiler emits terminal `sun.misc.Unsafe::objectFieldOffset` deprecation warnings. The project therefore uses ktlint's documented custom Gradle/JavaExec integration with the same stable 1.8.0 engine and the already-required Java 17 toolchain. This is an execution-isolation change, not a dependency downgrade. Re-evaluate the plugin when a stable version can choose the worker JVM or no longer emits the warning on JDK 26.

Greenfield quality policy:

- Kotlin/compiler warnings fail CI;
- Gradle deprecations fail the dedicated warning check;
- Android Lint warnings/errors fail deterministic PR CI;
- no lint baseline initially;
- no global warning suppressions;
- `NewerVersionAvailable` is excluded narrowly from deterministic Lint because stable dependency discovery runs in the separate scheduled/manual `Dependency freshness` workflow;
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
| GitHub artifact upload action | `actions/upload-artifact` **7.0.1**; direct single-file APK uploads use `archive: false` |
| Android emulator action | `ReactiveCircus/android-emulator-runner` **2.38.0**, pinned to immutable commit SHA |
| GitHub Actions dependencies | Pin to **immutable commit SHA**, annotate release tag in comments |
| Gradle dependency verification | **SHA-256 metadata committed** |
| Gradle configuration cache | **Required compatible** |
| Gradle build cache | **Enabled where appropriate** |
| Build scripts | **Kotlin DSL only** |
| Version management | **`gradle/libs.versions.toml`** |

The exact `setup-java` action version is not part of the authoritative toolchain: CI Java setup is bootstrap plumbing. Committed Gradle Daemon JVM criteria plus Gradle Java toolchains remain the source of truth.

Permanent PR CI executes debug/release assembly, JVM tests, static/format/dependency-health gates, strict dependency verification, configuration-cache reuse, a merged built-APK manifest/privacy audit, and the Android instrumentation suite on a stable API-36 emulator. Dependency freshness is a separate scheduled/manual advisory workflow so upstream publication timing cannot make an unchanged PR fail. Repository-signed APK and physical Samsung acceptance remain separate release/integration evidence.

## 8. Known newer/pre-release versions intentionally not selected

The preview compile-SDK exception above is the only approved preview input. It is an SDK platform, not a Maven/library/plugin dependency.

### AGP 9.4.0

AGP **9.4.0** is stable, but Kotlin 2.4.20 currently documents full AGP compatibility only through **9.3.1**. Upgrade when a stable Kotlin line expands that compatibility range.

### Kotlin 2.5.0-Beta1

Kotlin **2.5.0-Beta1** exists but is a beta and is therefore excluded.

### Compose BOM / libraries

No newer pre-release Compose BOM or library line is allowed in the normal dependency graph.

### Benchmark 1.5.x

Benchmark 1.5.x is not stable on the policy date, so the project remains on 1.4.1 when benchmark tooling is adopted.

## 9. Upgrade procedure

When intentionally upgrading any baseline component:

1. verify the candidate is stable upstream, except for the narrowly approved compile-SDK platform rule;
2. verify compatibility with dependent build tools;
3. change the version catalog / wrapper / Daemon JVM criteria in a dedicated upgrade change;
4. regenerate dependency verification metadata;
5. run unit, instrumentation, Lint, ktlint and dependency-health gates;
6. run Gradle with deprecation warnings enabled/failing;
7. verify configuration-cache reuse;
8. verify debug APK install/launch on the primary Samsung device when platform/UI behavior may be affected;
9. record any deferred incompatibility rather than silently accepting warnings.

The point of this policy is not to chase version numbers. It is to keep the project on the newest stable **compatible** stack, with the one documented compile-SDK platform exception, and keep upgrade work small and deliberate.
