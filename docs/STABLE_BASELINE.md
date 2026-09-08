# Stable Development Baseline

**Policy date:** 2026-09-08

This file is the authoritative version baseline for One UI Organizer.

The project uses **latest stable releases only**. Alpha, beta, RC, milestone, EAP, preview libraries, and dynamic `+` versions are not allowed in normal development.

There is one deliberate qualification: build tools must also be inside the vendors' documented compatibility ranges. If the individually newest stable AGP/Gradle/Kotlin combination is not yet documented as fully compatible, use the newest stable compatible combination and record the newer release as pending. Avoiding compatibility warnings is more important than winning a version-number race.

## 1. Baseline build environment

| Component | Baseline | Status / reason |
|---|---:|---|
| Android Studio | **Quail 4 / 2026.1.4** | Current stable Android Studio |
| JDK used to run Gradle | **JDK 26** | Current stable Java release; Gradle 9.7 supports JVM 17–26 |
| JDK patch | **26.0.2.1** where this distribution/version is available | Current Oracle JDK 26 maintenance release at policy date; other reputable OpenJDK 26 distributions are acceptable |
| Kotlin | **2.4.20** | Current stable Kotlin, released 2026-09-07 |
| Compose Compiler Gradle plugin | **2.4.20** | Must match Kotlin; Compose compiler now ships with Kotlin |
| Android Gradle Plugin | **9.3.1** | Highest AGP Kotlin 2.4.20 currently documents as fully supported |
| Gradle Wrapper | **9.7.0** | Highest Gradle Kotlin 2.4.20 currently documents as fully supported |
| compileSdk | **37** | Required by stable Compose 1.12 |
| targetSdk | **36** initially | Target the latest fully released Android OS; raise to 37 when Android 17 final is released and device tests pass |
| minSdk | **28** | Product compatibility decision, not a dependency-version constraint |
| Android SDK Build Tools | **AGP-managed default (36.0.0 for AGP 9.3)** | Do not pin unnecessarily |
| NDK | **Not installed/pinned unless needed** | No native code planned |

### Why JDK 26 but not Java-26 Android bytecode?

Three versions are easy to conflate:

1. **Gradle runtime JDK** — the JDK that runs Gradle and build plugins. Use **JDK 26**.
2. **Java/Kotlin toolchain** — compiler used for source and tests. Keep this explicitly configured rather than inheriting whatever the machine happens to have.
3. **Android bytecode/language target** — what D8/R8 and Android must consume. Do not automatically raise this merely because Gradle runs on JDK 26.

For this Kotlin-first app, JDK 26 removes the need to keep the development environment artificially frozen on JDK 17 while allowing Android bytecode compatibility to be chosen independently.

The implementation scaffold must explicitly configure the toolchain and compatibility settings. It must not rely on Android Studio's ambient JDK or `JAVA_HOME` by accident.

## 2. Kotlin / Compose UI baseline

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

Use the Compose BOM instead of independently versioning Compose artifacts.

Do not add legacy XML/AppCompat simply as a default. Add a View-system dependency only if an actual platform integration requires one.

## 3. State and concurrency baseline

| Dependency | Stable baseline | Use |
|---|---:|---|
| DataStore | **1.2.1** | Organizer preferences/state |
| kotlinx.coroutines | **1.11.0** | Async scan/repository/ViewModel work |
| kotlinx.coroutines-test | **1.11.0** | Coroutine tests |
| kotlinx.serialization | **1.11.0** | Typed persisted state/export if required |

Do not add Room, KSP, Hilt, Koin, WorkManager, Retrofit, Ktor, OkHttp, Coil, Glide, Paging, or AppSearch until there is a concrete requirement.

## 4. Test baseline

Use stable test libraries only:

| Dependency | Stable baseline |
|---|---:|
| AndroidX Test Core | **1.7.0** |
| AndroidX Test ext.junit | **1.3.0** |
| Espresso | **3.7.0** |
| UI Automator | **2.4.0** |
| Compose UI testing | **Compose BOM 2026.08.00** |
| kotlinx-coroutines-test | **1.11.0** |

Prefer `kotlin.test`, fakes, and small deterministic tests over introducing a mocking framework by default.

## 5. Stable libraries reserved for later features

These are not initial dependencies. Their versions are listed so future work does not accidentally select an alpha/RC line.

| Feature | Stable library at policy date |
|---|---:|
| Home-screen widgets | AndroidX Glance **1.2.0** |
| Macrobenchmarking | AndroidX Benchmark **1.4.1** |
| Navigation | Add only when a second real destination exists; re-check latest stable Navigation 3 at adoption time |
| Database | Add only if DataStore stops fitting; re-check latest stable Room at adoption time |
| DI | Manual injection initially; re-check stable Hilt/Dagger only if the graph becomes large enough to justify it |

## 6. Known newer stable releases intentionally not selected

### AGP 9.4.0

AGP **9.4.0** is the individually newest stable Android Gradle Plugin as of 2026-09-08. It is **not** the baseline yet because Kotlin 2.4.20 currently documents full AGP compatibility only through **9.3.1**.

Upgrade as soon as JetBrains expands Kotlin 2.4.20's supported AGP range, or a newer stable Kotlin release supports AGP 9.4+.

### Gradle 9.7.1

Gradle **9.7.1** is the newest stable patch and Gradle recommends it over 9.7.0. Kotlin 2.4.20's published compatibility range currently names **9.7.0** as its fully supported maximum. For a zero-warning baseline we therefore pin **9.7.0** until the compatibility statement catches up.

### JDK 27

JDK 27 is not stable as of this policy date. Gradle's current compatibility table supports running Gradle through **JDK 26** and explicitly says JVM 27+ is not yet supported. Do not move to JDK 27 until both conditions are true:

- JDK 27 is GA/stable; and
- the pinned stable Gradle release supports running on JDK 27.

## 7. Version policy for all future development

Every implementation or dependency-changing PR must follow these rules:

1. **Stable only.** No alpha, beta, RC, Canary-only dependency, EAP, snapshot, milestone, or `+` version.
2. **Newest stable compatible version.** Check upstream compatibility matrices before pinning build-tool upgrades.
3. **No silent version debt.** If a newer stable version is intentionally deferred, document exactly what compatibility condition blocks it.
4. **Exact pins.** Use a Gradle version catalog and exact versions/BOMs.
5. **Built-in Kotlin.** Because AGP 9+ provides built-in Kotlin, do not apply the obsolete `org.jetbrains.kotlin.android` plugin unless a documented compatibility reason requires opting out of built-in Kotlin.
6. **Compose compiler follows Kotlin.** Apply `org.jetbrains.kotlin.plugin.compose` at the same version as Kotlin.
7. **No warning baseline.** A clean project should not normalize build/deprecation warnings. New warnings are treated as work items, not wallpaper.
8. **Dependency updates are first-class maintenance.** Prefer small isolated upgrade PRs instead of accumulating several years of upgrades into one migration.
9. **Re-check at scaffold time.** Before the first source code is generated, verify this file against current upstream stable releases again.
10. **Re-check periodically.** At minimum, check stable tool/library versions before each release milestone and whenever Android Studio flags a stable update.

## 8. Initial dependency footprint

The first implementation should need roughly:

```text
Build
- JDK 26
- Kotlin 2.4.20
- Compose compiler plugin 2.4.20
- AGP 9.3.1
- Gradle 9.7.0
- compileSdk 37
- targetSdk 36

Runtime/UI
- AndroidX Core 1.19.0
- Activity Compose 1.13.0
- Lifecycle 2.11.0
- Compose BOM 2026.08.00
- Compose UI/Foundation/Runtime via BOM
- Material 3 1.4.0 via BOM
- DataStore 1.2.1
- kotlinx.coroutines 1.11.0
- kotlinx.serialization 1.11.0

Tests
- Kotlin test APIs
- kotlinx-coroutines-test 1.11.0
- Compose UI test artifacts via BOM
- AndroidX Test 1.7.0 line
- Espresso 3.7.0
```

That is deliberately modern without being dependency-heavy.

## 9. Upstream references

- Kotlin 2.4.20 / Gradle and AGP compatibility: https://kotlinlang.org/docs/gradle-configure-project.html
- Compose compiler plugin: https://kotlinlang.org/docs/compose-compiler-migration-guide.html
- Gradle Java compatibility: https://docs.gradle.org/current/userguide/compatibility.html
- Gradle releases: https://gradle.org/releases/
- AGP releases: https://developer.android.com/build/releases/about-agp
- AGP 9.3: https://developer.android.com/build/releases/agp-9-3-0-release-notes
- AGP 9.4: https://developer.android.com/build/releases/agp-9-4-0-release-notes
- Android Java/JDK configuration: https://developer.android.com/build/jdks
- Compose releases/BOM: https://developer.android.com/develop/ui/compose/bom
- AndroidX current versions: https://developer.android.com/jetpack/androidx/versions
- Oracle Java downloads/release status: https://www.oracle.com/java/technologies/downloads/
