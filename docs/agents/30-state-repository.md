# Agent 30 Prompt — Persistence / State / Repository

Repository:

https://github.com/AlexanderWilhelmsenBerg/One-UI-Organizer

You are the **local persistence, schema/migration and repository-state owner** for One UI Organizer.

Your lane persists user-owned organizer state and combines that state with discovered apps through app-owned contracts. It must not know about Compose UI details and must not implement Android package discovery itself.

## Start condition

**Do not start until Agent 00 / foundation scaffold has been merged to `main`.**

Then update from latest `main` and create/use branch:

`feature/state-repository`

## Mandatory reading

Read completely:

- `AGENTS.md`
- `docs/STABLE_BASELINE.md`
- `docs/ENGINEERING_BASELINE.md`
- `docs/TECH_STACK.md`
- `docs/PLAN.md`
- `docs/ACCEPTANCE_CRITERIA.md`
- `docs/PARALLEL_DEVELOPMENT.md`

## Critical rules

- Do **not merge** the PR.
- Stay inside data/state/repository ownership.
- Do not implement PackageManager scanning.
- Do not implement category algorithm rules owned by Agent 20; consume the frozen categorization contract where repository integration needs it.
- Do not build final Compose UI.
- Do not change toolchain/library versions.
- Use the already-selected stable DataStore/serialization stack only; do not introduce Room/KSP.
- Keep warnings at zero.

## Primary ownership

Prefer changes under:

```text
app/src/main/java/**/data/**
app/src/test/**/data/**
```

Use shared app-owned models/contracts from Agent 00 rather than defining parallel equivalents.

## Persistent state requirements

Implement `OrganizerStateStore` using the baseline DataStore + Kotlin serialization approach documented in the repository.

State is app-owned and begins at explicit **schema version 1**.

Persist only user-owned organizer behavior needed by v0.1, including as contracts allow:

- category overrides;
- favourite state;
- hidden state;
- any minimal identity metadata needed to associate those choices with launch targets/packages deterministically.

Do **not** persist app icons or package labels as long-lived authoritative state.

## Schema/migration rules

- serialized DTO/state belongs to the app, not to DataStore's public surface;
- `schemaVersion` is explicit from version 1;
- keep serializer/persistence implementation types below `OrganizerStateStore`;
- migration logic must be deterministic;
- before any future schema version 2 exists, migration tests from v1 become mandatory;
- corrupt/unreadable state must fail safely according to a documented policy rather than crashing the ordinary organizer flow.

For this initial schema, add tests establishing v1 round-trip and default/empty-state behavior so later migrations have a baseline contract.

## Repository responsibilities

Implement the frozen `OrganizerRepository` contract or the data-side implementation expected by Agent 00.

The repository is the source of truth that combines:

- apps supplied by `InstalledAppSource`;
- user-owned persisted state from `OrganizerStateStore`;
- categorization supplied by the domain categorization contract.

Requirements:

- UI must not have to merge package scan + persistence itself;
- no Compose types in repository contracts;
- use immutable app-owned state exposed via `suspend` / `Flow` / `StateFlow` only as the frozen contracts require;
- favourite/unfavourite persists;
- hide/unhide persists;
- category override persists;
- rescans merge cleanly with stored user state;
- uninstalled apps do not appear as dead entries in the ordinary shelf;
- stale persisted state policy is explicit and tested;
- reinstall behavior is deterministic and documented.

Do not introduce an always-running background service.

## Stale/uninstall policy

Choose and document a conservative v0.1 policy consistent with acceptance criteria. A reasonable starting point is retaining user-owned package/category state for a bounded/defined reason or ignoring stale entries in current UI while preserving them for reinstall. Whatever you choose:

- normal shelf must never surface a dead launch target;
- behavior must be deterministic;
- tests must prove it;
- do not silently discard user corrections without a documented cleanup rule.

## Tests

At minimum add deterministic tests for:

- empty/default schema v1;
- state round-trip;
- override persistence;
- favourite persistence;
- hidden persistence;
- state update concurrency/atomicity as appropriate;
- repository merge of installed apps + persisted state;
- removal/uninstall handling;
- reinstall identity behavior according to selected policy;
- category override surviving automatic rule/category changes at the repository merge level;
- process recreation equivalent via store reload;
- corrupt/default recovery policy where practical.

Prefer fakes for `InstalledAppSource` and categorizer boundaries. Do not add a mocking framework unless the documented dependency process is followed and a concrete gap exists.

## Contract changes

Do not redesign shared contracts casually. If the frozen state/repository contract cannot represent a required acceptance behavior, make the smallest app-owned change and call it out prominently so sibling agents can rebase.

Never leak DataStore-specific types into domain/UI contracts to avoid a contract change.

## Acceptance targets

This PR should satisfy or establish the implementation for Acceptance Criteria C1-C3 and G3, plus the persistence side of B3/B4.

## Final report

When finished, do not merge. Report:

1. branch and PR;
2. files changed;
3. schema-v1 shape and versioning strategy;
4. stale/uninstall/reinstall policy;
5. repository merge semantics;
6. tests run/results;
7. warnings/deprecations encountered;
8. any shared-contract change and sibling impact;
9. anything Agent 50 must know during integration.
