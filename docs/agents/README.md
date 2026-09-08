# Coding Agent Prompt Index

Use these prompts as copy/paste task briefs for separate coding agents.

## Sequence

### Start first

- [`00-foundation-scaffold.md`](00-foundation-scaffold.md) — scaffold, toolchains, CI, shared contracts.

**Merge Agent 00 before starting the next four agents.**

### Start together after Agent 00 merges

- [`10-platform-apps.md`](10-platform-apps.md) — Android app discovery/launch platform spike.
- [`20-category-domain.md`](20-category-domain.md) — category engine, rules and search domain logic.
- [`30-state-repository.md`](30-state-repository.md) — DataStore state, migrations and repository merge logic.
- [`40-compose-ui.md`](40-compose-ui.md) — Compose design system and organizer shelf against fake data.

These four lanes are intentionally separated by file ownership and app-owned contracts.

### Start after Wave 1 merges

- [`50-integration-acceptance.md`](50-integration-acceptance.md) — wire real layers, close v0.1 behavior, device acceptance, cross-app tests and performance hardening.

## Rules common to every prompt

- Do not merge your PR.
- Do not broaden scope.
- Read root `AGENTS.md` before editing.
- Use the exact versions in `docs/STABLE_BASELINE.md`.
- Follow `docs/ENGINEERING_BASELINE.md` for toolchains, boundaries, tests and upgrade behavior.
- Use app-owned models/contracts; do not leak framework/library types across boundaries.
- Keep build/compiler/lint/format warnings at zero.
- Rebase/update from `main` at the point required by the prompt.
- Report changed files, tests run, failures, warnings and any physical-device work still required.

See [`../PARALLEL_DEVELOPMENT.md`](../PARALLEL_DEVELOPMENT.md) for merge order and ownership rules.
