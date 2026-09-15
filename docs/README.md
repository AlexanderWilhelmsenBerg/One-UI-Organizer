# One UI Organizer Documentation Index

This directory is the project source of truth for product scope, engineering policy and parallel implementation.

## Read order for implementation

1. [`../AGENTS.md`](../AGENTS.md) — repository-wide mandatory coding rules.
2. [`STABLE_BASELINE.md`](STABLE_BASELINE.md) — authoritative exact tool/library versions.
3. [`ENGINEERING_BASELINE.md`](ENGINEERING_BASELINE.md) — toolchains, build, testing, benchmarking, dependency and upgrade rules.
4. [`TECH_STACK.md`](TECH_STACK.md) — architecture/framework decisions and dependency boundaries.
5. [`PLAN.md`](PLAN.md) — product architecture and delivery milestones.
6. [`ACCEPTANCE_CRITERIA.md`](ACCEPTANCE_CRITERIA.md) — final product/quality acceptance contract.
7. [`MOSCOW.md`](MOSCOW.md) — priority/scope boundary.
8. [`PARALLEL_DEVELOPMENT.md`](PARALLEL_DEVELOPMENT.md) — coding-agent ownership, sequencing and merge plan.
9. [`METADATA_PRESENTATION_INTEGRATION_ACCEPTANCE.md`](METADATA_PRESENTATION_INTEGRATION_ACCEPTANCE.md) — current Agent-102 combined metadata/presentation acceptance record.
10. [`agents/README.md`](agents/README.md) — copy/paste prompts for coding lanes.

## Authority map

When documents overlap, use this precedence:

| Question | Authoritative document |
|---|---|
| Which exact JDK/Kotlin/AGP/Gradle/library version? | `STABLE_BASELINE.md` |
| How must tools/libraries be configured and isolated? | `ENGINEERING_BASELINE.md` |
| Why Kotlin/Compose/DataStore/manual DI? | `TECH_STACK.md` |
| What product are we building and in what milestones? | `PLAN.md` |
| What must pass before a wave is accepted? | `ACCEPTANCE_CRITERIA.md` |
| What is Must/Should/Could/Won't? | `MOSCOW.md` |
| Which coding agent owns which files and when can it start? | `PARALLEL_DEVELOPMENT.md` |
| What should a specific coding agent be told? | `agents/<lane>.md` or the current owner-provided lane prompt |
| What rules apply to every coding agent? | root `AGENTS.md` |

If an implementation discovers a contradiction, do not silently choose whichever document is convenient. Resolve the contradiction in documentation in the same PR and explain why.

## Current state

As of 2026-09-14:

- v0.1 and the classification/category-management waves are merged;
- Agent 92 / PR #21 portability + category-shortcut integration is merged and owner Samsung-accepted;
- Agent 101 / PR #22 adds the two-destination `Organizer` / `Categories` presentation shell;
- Agent 100 / PR #23 adds supported best-effort F-Droid metadata enrichment;
- Agent 102 is the current integration/acceptance lane on `integration/metadata-presentation`;
- physical Samsung online/offline acceptance remains required before the metadata/presentation wave is accepted.

## Coding agent prompts

- [`agents/00-foundation-scaffold.md`](agents/00-foundation-scaffold.md)
- [`agents/10-platform-apps.md`](agents/10-platform-apps.md)
- [`agents/20-category-domain.md`](agents/20-category-domain.md)
- [`agents/30-state-repository.md`](agents/30-state-repository.md)
- [`agents/40-compose-ui.md`](agents/40-compose-ui.md)
- [`agents/50-integration-acceptance.md`](agents/50-integration-acceptance.md)

Later waves may be driven by explicit owner prompts even when a dedicated `docs/agents/` file has not been added. Repository state and `PARALLEL_DEVELOPMENT.md` remain authoritative.

Coding agents must leave PRs unmerged unless the project owner explicitly instructs otherwise.
