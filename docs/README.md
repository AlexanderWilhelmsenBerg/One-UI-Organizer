# One UI Organizer Documentation Index

This directory is the project source of truth for product scope, engineering policy and parallel implementation.

## Read order for implementation

1. [`../AGENTS.md`](../AGENTS.md) — repository-wide mandatory coding rules.
2. [`STABLE_BASELINE.md`](STABLE_BASELINE.md) — authoritative exact tool/library versions.
3. [`ENGINEERING_BASELINE.md`](ENGINEERING_BASELINE.md) — toolchains, build, testing, benchmarking, dependency and upgrade rules.
4. [`TECH_STACK.md`](TECH_STACK.md) — architecture/framework decisions and dependency boundaries.
5. [`PLAN.md`](PLAN.md) — product architecture and delivery milestones.
6. [`ACCEPTANCE_CRITERIA.md`](ACCEPTANCE_CRITERIA.md) — final product/quality acceptance contract.
7. [`MOSCOW.md`](MOSCOW.md) — v0.1 priority/scope boundary.
8. [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md) — coding-agent ownership, sequencing and merge plan.
9. [`agents/README.md`](agents/README.md) — copy/paste prompts for each coding lane.

## Authority map

When documents overlap, use this precedence:

| Question | Authoritative document |
|---|---|
| Which exact JDK/Kotlin/AGP/Gradle/library version? | `STABLE_BASELINE.md` |
| How must tools/libraries be configured and isolated? | `ENGINEERING_BASELINE.md` |
| Why Kotlin/Compose/DataStore/manual DI? | `TECH_STACK.md` |
| What product are we building and in what milestones? | `PLAN.md` |
| What must pass before v0.1 is accepted? | `ACCEPTANCE_CRITERIA.md` |
| What is Must/Should/Could/Won't? | `MOSCOW.md` |
| Which coding agent owns which files and when can it start? | `PARALLEL_DEVELOPMENT.md` |
| What should a specific coding agent be told? | `agents/<lane>.md` |
| What rules apply to every coding agent? | root `AGENTS.md` |

If an implementation discovers a contradiction, do not silently choose whichever document is convenient. Resolve the contradiction in documentation in the same PR and explain why.

## Current state

As of 2026-09-08:

- planning and architecture are complete enough to begin implementation;
- the toolchain/library baseline has been upstream-verified;
- no feature implementation exists yet;
- Agent 00 is the first required implementation lane;
- Agents 10/20/30/40 can start in parallel only after Agent 00 merges;
- Agent 50 starts after the Wave-1 lanes merge;
- physical Samsung acceptance remains required before v0.1 release readiness.

## Coding agent prompts

- [`agents/00-foundation-scaffold.md`](agents/00-foundation-scaffold.md)
- [`agents/10-platform-apps.md`](agents/10-platform-apps.md)
- [`agents/20-category-domain.md`](agents/20-category-domain.md)
- [`agents/30-state-repository.md`](agents/30-state-repository.md)
- [`agents/40-compose-ui.md`](agents/40-compose-ui.md)
- [`agents/50-integration-acceptance.md`](agents/50-integration-acceptance.md)

Coding agents must leave PRs unmerged unless the project owner explicitly instructs otherwise.
