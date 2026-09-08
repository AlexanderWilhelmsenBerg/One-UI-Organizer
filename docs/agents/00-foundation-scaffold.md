# Agent 00 Prompt — Foundation / Scaffold / CI

Repository:

https://github.com/AlexanderWilhelmsenBerg/One-UI-Organizer

You are the **foundation, build-system, toolchain, CI and shared-contract owner** for One UI Organizer.

Your job is to create the clean Android scaffold that all later coding agents will branch from. This is a greenfield repository with planning documentation already on `main`.

## Mandatory reading before editing

Read completely:

- `AGENTS.md`
- `docs/STABLE_BASELINE.md`
- `docs/ENGINEERING_BASELINE.md`
- `docs/TECH_STACK.md`
- `docs/PLAN.md`
- `docs/ACCEPTANCE_CRITERIA.md`
- `docs/MOSCOW.md`
- `docs/PARALLEL_DEVELOPMENT.md`

Treat `docs/STABLE_BASELINE.md` as the authoritative exact-version inventory. **Do not substitute newer-looking versions** if they are outside the documented compatible stack.

## Critical rules

- Do **not merge** the PR.
- Do not implement the Organizer feature beyond minimal scaffold/smoke wiring and shared contract shells.
- Do not add alpha/beta/RC/EAP/preview/nightly/snapshot dependencies.
- Do not introduce Room, Hilt/Koin, WorkManager, networking, Coil/Glide, Navigation, Glance, Detekt, Robolectric, MockK or Mockito.
- Do not request `INTERNET` or `QUERY_ALL_PACKAGES`.
- Keep all build/compiler/deprecation/lint/format warnings at zero.

## Branch

Create/use:

`foundation/scaffold`

from the latest `main`.

## Required toolchain baseline

Follow `docs/STABLE_BASELINE.md`, including the current verified baseline:

- Gradle daemon: Temurin/Adoptium JDK 26.0.2.1+1
- Android Java compile/test toolchain: Java 17
- Kotlin 2.4.20
- Compose compiler plugin 2.4.20
- AGP 9.3.1
- Gradle wrapper 9.7.0
- compileSdk 37
- targetSdk 36
- minSdk 28
- stable Compose BOM / AndroidX versions exactly as documented

The JDK roles must remain separate. Do not let the daemon JDK implicitly set Android bytecode/JVM target.

## Deliverables

Create a minimal, production-grade Android project scaffold with:

1. Gradle wrapper.
2. `settings.gradle.kts` and Kotlin DSL build scripts only.
3. `gradle/libs.versions.toml` as the central version catalog.
4. `gradle/gradle-daemon-jvm.properties` / Daemon JVM criteria generated/configured for JDK 26 and documented vendor criteria.
5. Foojay stable resolver as specified by the baseline if needed for toolchain provisioning.
6. Explicit Java 17 toolchain, source/target compatibility and Kotlin JVM target.
7. AGP built-in Kotlin support; do not reintroduce obsolete `org.jetbrains.kotlin.android` unless the baseline explicitly changes.
8. Compose compiler plugin matching Kotlin.
9. Single `:app` module.
10. Minimal Compose smoke activity/theme sufficient to install and launch.
11. Central package namespace suitable for the project.
12. Dependency verification metadata using SHA-256.
13. Centralized repositories with subprojects prevented from adding arbitrary repositories.
14. Configuration-cache-compatible build.
15. ktlint, Android Lint and dependency-analysis tooling from the baseline.
16. Stable dependency update reporting using the current non-deprecated plugin ID documented in the baseline.
17. GitHub Actions CI pinned to immutable action SHAs, covering the core verification lane.
18. `.gitignore` and other normal greenfield Android repository hygiene.

## Shared contracts to freeze for Wave 1

Create only the smallest app-owned model/contracts needed for the four parallel lanes. Keep them implementation-free or minimally implemented.

Expected responsibilities/names are approximately:

```text
model/
  AppId
  LaunchTargetId
  InstalledApp
  AppCategory
  ClassificationSource
  CategorizedApp
  OrganizerState

platform/apps/
  InstalledAppSource
  AppLauncher

domain/
  CategoryEngine or AppCategorizer contract

data/
  OrganizerStateStore
  OrganizerRepository contract
```

Use good Kotlin naming if an adjustment is warranted, but preserve the boundaries from `docs/PARALLEL_DEVELOPMENT.md`.

Contract rules:

- no `ApplicationInfo`, `ResolveInfo`, `Intent`, `Drawable`, DataStore, serialization or Compose types in domain/application models;
- no Android framework objects in pure domain contracts;
- no persistence implementation types in repository/domain contracts;
- `OrganizerState` has an explicit schema version starting at 1;
- no generic god-manager abstraction;
- dependencies remain constructor-injectable ordinary Kotlin types.

## CI / verification tasks

Create a clear verification path that can later be used by all agents. It should cover, as applicable:

- clean assemble/compile;
- unit tests;
- Android Lint;
- ktlint;
- dependency `buildHealth`;
- Gradle deprecation/warning check consistent with the engineering baseline;
- configuration-cache smoke verification.

Do not create a lint baseline or broad warning suppressions.

## Tests

Add only tests that prove the scaffold/contracts themselves where meaningful. Do not pre-implement feature behavior owned by Agents 10/20/30/40.

## Documentation update

If implementation reveals a genuine mismatch in the documented baseline, do not silently work around it. Update the relevant documentation in the same PR with evidence and explain the change in the PR body.

Do not change a version simply because a higher version exists; the stable-compatible policy applies.

## Exit criteria

This PR is ready for review when:

- clean checkout can build using the documented toolchains;
- CI configuration is present and warning-free locally where runnable;
- exact dependencies match `STABLE_BASELINE.md`;
- shared contracts compile and do not leak external types;
- app installs/launches to a minimal smoke surface;
- no Organizer feature lane has been implemented prematurely;
- later agents can branch from this PR after merge without editing common build files for their normal work.

## Final report

When finished, do not merge. Report:

1. branch and PR number/link;
2. files/directories created;
3. exact toolchain versions actually resolved;
4. verification commands run and results;
5. all warnings encountered and how they were removed;
6. any baseline/document mismatch discovered;
7. shared contracts created/frozen;
8. anything that later agents must know before starting.
