# One UI Organizer

A small, local-first Android companion for automatically organizing launchable apps into useful categories **without replacing Samsung One UI Home**.

The app is intended to act as a fast categorized app shelf: open it from One UI, search or browse categories, launch an app, and return to the existing home screen. It does not attempt to rewrite Samsung's launcher database or create native One UI folders.

## Status

**Planning complete / implementation ready.**

The product scope, acceptance criteria, MoSCoW priorities, stable toolchain, engineering/upgrade policy, and parallel coding plan are now defined on `main`.

Implementation starts with one foundation/scaffold PR. After that PR is merged, four feature agents can work in parallel on Android platform integration, pure categorization/search, persistence/repository, and Compose UI. A final integration agent then closes v0.1 acceptance and performance work.

No production Organizer feature code has been implemented yet.

## Product principles

- **Companion, not launcher.** One UI Home remains the default launcher.
- **Local first.** No account, cloud service, analytics, or network dependency is required.
- **Automatic by default, correctable by the user.** Classification is deterministic and manual corrections always win.
- **Tiny on purpose.** Avoid services, heavy frameworks, and dependencies until they solve a demonstrated need.
- **Supported Android APIs first.** Do not depend on Samsung private launcher storage or unsupported launcher manipulation.
- **Play-policy friendly.** The initial design avoids `QUERY_ALL_PACKAGES` and discovers launchable apps through declared launcher-intent visibility.
- **Current without becoming fragile.** Use the latest stable, fully compatible toolchain/library versions and keep upgrades warning-free.
- **Replaceable dependencies.** Keep app-owned models and narrow platform/library adapters so future upgrades do not ripple through unrelated features.
- **Parallel by ownership, not duplication.** Coding agents work in explicit lanes against frozen app-owned contracts rather than inventing competing implementations.

## Planned v0.1 experience

1. Tap One UI Organizer from the Samsung home screen.
2. A One UI-inspired categorized shelf opens.
3. Apps are grouped automatically using user overrides, known-app rules, Android's declared app category, then `Unsorted` as the fallback.
4. Search filters apps and categories immediately.
5. Tap an app to launch it.
6. Long-press an app to change its category, favourite it, or hide it.
7. Corrections persist locally and take priority on future scans.

## Implementation sequence

```text
Agent 00 — Foundation / scaffold / CI
                 |
                 v
        merge foundation PR
                 |
     +-----------+-----------+-----------+-----------+
     |           |           |           |           |
Agent 10     Agent 20    Agent 30    Agent 40
Platform      Domain       Data          UI
     |           |           |           |
     +-----------+-----------+-----------+
                 |
                 v
Agent 50 — Integration / acceptance / performance
                 |
                 v
        Samsung device acceptance
                 |
                 v
                v0.1
```

See [Parallel development plan](docs/PARALLEL_DEVELOPMENT.md) for ownership and merge rules, and [Coding agent prompts](docs/agents/README.md) for copy/paste briefs.

## Documentation

### Product and scope

- [Product and delivery plan](docs/PLAN.md)
- [Acceptance criteria](docs/ACCEPTANCE_CRITERIA.md)
- [MoSCoW scope](docs/MOSCOW.md)

### Engineering

- [Framework and dependency decisions](docs/TECH_STACK.md)
- [Authoritative stable version baseline](docs/STABLE_BASELINE.md)
- [Engineering, toolchain, testing and upgrade policy](docs/ENGINEERING_BASELINE.md)
- [Repository-wide coding-agent rules](AGENTS.md)

### Parallel implementation

- [Parallel development plan](docs/PARALLEL_DEVELOPMENT.md)
- [Coding agent prompt index](docs/agents/README.md)

## Initial technical direction

The stack is native Android with Kotlin and Jetpack Compose. The project targets modern Android while keeping `minSdk 28` as the practical lower bound.

Build infrastructure and Android compilation are deliberately separated: the current supported stable Gradle runtime JDK can move forward independently, while Android source/bytecode compatibility is pinned through explicit Java/Kotlin toolchains. Exact versions are maintained in `docs/STABLE_BASELINE.md`.

The initial architecture deliberately avoids a database and dependency-injection framework. A small scanner boundary, categorization engine, repository, typed local state store, and Compose UI are enough for v0.1. Room, Hilt/Koin, widgets, usage statistics, and background monitoring are deferred until requirements justify them.

All implementation work follows `docs/ENGINEERING_BASELINE.md`: platform and third-party types are kept at controlled boundaries, persisted state is app-owned and versioned, warnings are not accumulated, and dependency/toolchain upgrades are isolated and fully tested.

## Non-goals for v0.1

- Replacing One UI Home.
- Editing Samsung's native app drawer, pages, or folders.
- Becoming a default launcher.
- Cloud/AI classification.
- Usage-history monitoring.
- Work-profile/device-policy management.
- Internet access or telemetry.

## License

No license has been selected yet. Choose one before accepting external contributions or publishing a release.
