# Development Instructions for One UI Organizer

These rules apply to **all implementation, refactoring, testing, build, CI, dependency and parallel-agent work** in this repository.

Before making code/build changes, read:

1. `docs/STABLE_BASELINE.md` — authoritative exact versions.
2. `docs/ENGINEERING_BASELINE.md` — toolchain, testing, benchmarking, dependency and coding-boundary rules.
3. `docs/TECH_STACK.md` — architectural technology decisions.
4. `docs/ACCEPTANCE_CRITERIA.md` — product acceptance contract.
5. `docs/PLAN.md` and `docs/MOSCOW.md` — current scope.
6. `docs/PARALLEL_DEVELOPMENT.md` — agent ownership, sequencing and merge rules.
7. The lane-specific prompt under `docs/agents/` when working as one of the parallel coding agents.

## Non-negotiable engineering rules

- Use **stable releases only**. Do not introduce alpha, beta, RC, EAP, preview, nightly, snapshot, milestone, or dynamic `+` versions unless the project owner explicitly changes the policy.
- Use the **newest stable version inside documented compatibility ranges**. A higher version number is not an upgrade if it creates unsupported combinations or warnings.
- Check upstream release and compatibility documentation before adding/upgrading any build tool, plugin, package, or library.
- Exact versions belong in the central version/build policy; do not scatter version strings through modules.
- Do not silently change the Gradle daemon JDK, Android Java toolchain, JVM target, `compileSdk`, `targetSdk`, or `minSdk`.
- Keep build/runtime JDK and Android compile/bytecode target separate. Follow the exact current values in `STABLE_BASELINE.md`.
- The project starts warning-free. Do not normalize compiler, Gradle deprecation, Android Lint, or dependency warnings with broad suppressions/baselines.
- Do not add a library merely for convenience. Every new dependency must satisfy the introduction checklist in `ENGINEERING_BASELINE.md`.
- Prefer Android/AndroidX/Kotlin first-party stable APIs when they solve the requirement cleanly.
- Do not rely on transitive dependencies. Code that imports an artifact must have a direct declared dependency.
- Keep configuration cache compatibility.
- Keep Gradle build scripts in Kotlin DSL.
- Keep dependency verification metadata current when dependencies change.

## Upgrade-friendly code boundaries

- Domain/application models are **app-owned**. Do not expose Android framework, DataStore, serialization, database, networking, DI, or other library-specific types through domain contracts.
- Android framework/package APIs live behind narrow adapters such as `InstalledAppSource` and `AppLauncher`.
- Persistence is accessed through `OrganizerStateStore`; persisted DTO/state is app-owned and explicitly versioned.
- Compose/Material are UI dependencies and must not leak into repository/domain contracts.
- Use app-owned design tokens/theme abstractions for intentional One UI-inspired customization instead of scattering magic dimensions/colors or raw Material defaults.
- Coroutines (`suspend`, `Flow`, immutable state) are the application async contract. Adapt callbacks/futures/other reactive types at boundaries.
- Dependencies are constructor-injected. Do not create service-locator globals.
- Do not create generic god abstractions such as `AndroidManager` or `DataManager`.
- Isolate Android API-level/version checks in platform adapters rather than spreading `Build.VERSION` branches across features.

## Parallel-development rules

Parallel work is organized in two waves. Follow `docs/PARALLEL_DEVELOPMENT.md` exactly.

### Foundation gate

Agent 00 owns the initial scaffold/build/CI/common-contract PR. Agents 10/20/30/40 must not begin from an independently invented scaffold; they start from `main` **after Agent 00 is merged**.

### Wave-1 ownership

- Agent 10 owns Android platform app discovery/launch integration.
- Agent 20 owns pure categorization/search domain logic.
- Agent 30 owns persistence/state/repository behavior.
- Agent 40 owns Compose UI/design-system behavior against app-owned/fake state.
- Agent 50 owns final cross-layer wiring, acceptance and performance hardening after the Wave-1 PRs merge.

### Shared contracts

Agent 00 freezes the smallest app-owned shared contracts/models needed by the Wave-1 lanes.

Feature agents:

- consume shared models/contracts rather than creating lane-local duplicates;
- stay inside their documented file ownership whenever practical;
- do not casually edit common Gradle/toolchain policy;
- delay cross-layer composition-root wiring to Agent 50 unless the lane needs minimal wiring to prove its own behavior;
- if a shared contract is genuinely insufficient, make the smallest change and call it out so affected sibling branches rebase.

Do not solve parallel merge pressure by creating two scanners, two state models, two repositories, two category engines, or adapter bypasses.

### PR control

Coding agents do **not** merge their own PRs unless the project owner explicitly says to merge. Each agent reports tests, warnings, unresolved device steps and any sibling-impacting contract change.

## Testing requirements

Choose the cheapest layer that proves the behavior:

- pure Kotlin unit tests for domain/category/search/state/migration behavior;
- Compose semantics tests for UI behavior;
- instrumentation tests for Android integration;
- UI Automator for cross-app/system/launcher behavior;
- real Samsung/One UI testing for product-critical platform behavior.

Prefer fakes over adding a mocking framework.

Any persisted-state format change requires migration tests proving existing user overrides/settings survive.

Bug fixes require a regression test at the appropriate layer whenever practical.

Each parallel agent owns the tests for its lane. Agent 50 owns the final cross-layer acceptance/regression lane.

## Performance requirements

Do not optimize from intuition alone.

Use stable AndroidX Macrobenchmark for real user-flow performance once the integrated flow exists. Use Microbenchmark only for isolated hot code. Do not introduce pre-release benchmark/profile tooling to bypass the stable-only rule.

Performance-sensitive changes should compare before/after measurements for the relevant user journey.

Feature agents should avoid speculative performance infrastructure; Agent 50 owns the first integrated performance pass.

## Dependency/toolchain upgrades

Keep upgrades separate from feature work where practical.

An upgrade is not complete until:

1. release/migration notes were reviewed;
2. compatibility is documented;
3. central versions are updated;
4. dependency verification metadata is updated;
5. build/compiler/lint output is warning-free;
6. formatting, lint, dependency health and unit tests pass;
7. instrumentation/device tests pass where relevant;
8. benchmark checks pass where relevant;
9. Samsung device acceptance is performed for Android/Compose/platform changes;
10. `docs/STABLE_BASELINE.md` is updated when the authoritative baseline changed.

Never fix an upgrade problem by broadly suppressing the warning unless the suppression is narrow, documented, temporary, and there is no supported alternative.

## Scope discipline

One UI Organizer is a **companion**, not a replacement launcher.

Do not broaden v0.1 into Samsung private APIs, launcher database mutation, `QUERY_ALL_PACKAGES`, cloud classification, analytics, background monitoring, work-profile management, or unrelated infrastructure without an explicit scope decision.

Parallel development is a delivery technique, not permission to expand scope.
