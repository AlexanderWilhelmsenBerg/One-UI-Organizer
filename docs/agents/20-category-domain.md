# Agent 20 Prompt — Categorization / Domain / Search

Repository:

https://github.com/AlexanderWilhelmsenBerg/One-UI-Organizer

You are the **pure Kotlin categorization and search-domain owner** for One UI Organizer.

Your lane contains deterministic category assignment, bundled known-app rules, classification-source reporting, and local search normalization/filtering. This lane must remain Android-framework-free and persistence-implementation-free.

## Start condition

**Do not start until Agent 00 / foundation scaffold has been merged to `main`.**

Then update from latest `main` and create/use branch:

`feature/category-engine`

## Mandatory reading

Read completely:

- `AGENTS.md`
- `docs/STABLE_BASELINE.md`
- `docs/ENGINEERING_BASELINE.md`
- `docs/TECH_STACK.md`
- `docs/PLAN.md`
- `docs/ACCEPTANCE_CRITERIA.md`
- `docs/MOSCOW.md`
- `docs/PARALLEL_DEVELOPMENT.md`

## Critical rules

- Do **not merge** the PR.
- Stay inside the pure domain/rules lane.
- Do not touch PackageManager/platform discovery implementation.
- Do not implement DataStore.
- Do not build final Compose screens.
- Do not change dependency/toolchain versions.
- Do not add libraries unless a documented requirement cannot be met with Kotlin/stdlib/coroutines already present; any proposed addition must satisfy `ENGINEERING_BASELINE.md` first.
- Keep warnings at zero.

## Primary ownership

Prefer changes under:

```text
app/src/main/java/**/domain/**
app/src/main/java/**/rules/**
app/src/test/**/domain/**
app/src/test/**/rules/**
```

## Required categorization behavior

Implement the frozen categorization contract from Agent 00 using app-owned models only.

Exact precedence is mandatory:

1. user override;
2. bundled known-app rule;
3. mapped Android application category supplied as app-owned metadata;
4. `Unsorted` fallback.

Requirements:

- deterministic: same inputs + same rules -> same output;
- manual override always wins;
- known-rule updates can never override an existing user override;
- unknown/unclassified apps remain usable and become `Unsorted`;
- emit/retain `ClassificationSource` so later UI/debugging can explain why an app landed in a category;
- no Android framework types in the engine.

## Known-app rules

Create a small, maintainable bundled starter ruleset only where it provides value beyond Android's declared categories.

Rules should be:

- explicit and deterministic;
- keyed on app-owned identity such as package name, not localized display labels where avoidable;
- easy to update without rewriting engine logic;
- tested for collisions/precedence;
- not presented as a complete global app database.

Do not build network-backed rule updates or AI classification.

## Android category mapping

The platform lane will provide an app-owned representation of Android-declared category metadata. Map those supported values to Organizer categories in pure Kotlin.

The domain layer must not import `ApplicationInfo` just to interpret constants.

## Search

Implement the v0.1 local search behavior against app-owned categorized models.

Minimum behavior:

- trim query;
- case-insensitive app-label match;
- category-label match that surfaces apps in that category;
- empty query returns the ordinary visible result set;
- hidden-state handling is not invented here if it belongs to repository state; expose pure helpers only where contracts support it;
- deterministic ordering;
- no network and no external search dependency.

Do not introduce fuzzy-search/indexing packages in this slice.

## Tests

Unit tests are the primary deliverable.

At minimum prove:

- each precedence boundary independently;
- user override beats known rule;
- user override beats Android category;
- known rule beats Android category;
- Android category beats fallback;
- fallback produces `Unsorted` without dropping app;
- classification source is correct;
- deterministic repeated calls;
- starter-rule matching and no accidental label-based behavior;
- search trim/case handling;
- search by app label;
- search by category;
- empty query;
- representative Norwegian/international labels if normalization assumptions matter.

Prefer parameterized/data-driven pure tests where useful.

## Contract changes

Do not independently redesign shared models/contracts while sibling agents are working.

If a frozen contract is genuinely insufficient:

1. document the exact missing capability;
2. make the smallest compatible change possible;
3. call it out prominently in the PR so Agents 10/30/40 can rebase if needed;
4. never duplicate the shared model inside your lane.

## Acceptance targets

This PR should satisfy or establish implementation for Acceptance Criteria B1-B4 and D1-D3 at the pure-domain level.

## Final report

When finished, do not merge. Report:

1. branch and PR;
2. files changed;
3. exact categorization precedence implementation;
4. starter rules included and rationale;
5. search behavior;
6. tests run/results;
7. warnings/deprecations encountered;
8. any shared-contract change and impact on sibling lanes;
9. anything Agent 50 must know during integration.
