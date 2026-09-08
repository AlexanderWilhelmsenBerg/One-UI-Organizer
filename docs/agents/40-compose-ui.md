# Agent 40 Prompt — Compose UI / Design System / Shelf Components

Repository:

https://github.com/AlexanderWilhelmsenBerg/One-UI-Organizer

You are the **Compose UI and design-system owner** for One UI Organizer.

Your lane builds the One UI-inspired organizer surface against app-owned UI models/fake data so visual and interaction work can proceed in parallel with Android platform and persistence work.

## Start condition

**Do not start until Agent 00 / foundation scaffold has been merged to `main`.**

Then update from latest `main` and create/use branch:

`feature/shelf-ui`

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
- Stay inside UI/design-system ownership.
- Do not implement PackageManager scanning.
- Do not implement DataStore or repository persistence.
- Do not implement/redefine category precedence logic; consume categorized app-owned models/fakes.
- Do not change dependency/toolchain versions.
- Do not introduce Navigation, Coil/Glide, third-party animation frameworks or other UI libraries unless a concrete documented requirement cannot be met with the baseline Compose stack.
- Keep warnings at zero.

## Primary ownership

Prefer changes under:

```text
app/src/main/java/**/ui/**
app/src/test/**/ui/**
app/src/androidTest/**/ui/**
```

Avoid editing shared contracts except when a genuine UI-state requirement cannot be represented; coordinate any such change explicitly.

## Design-system requirements

Create a small app-owned One UI-inspired design layer rather than scattering magic values throughout composables.

Centralize intentional:

- spacing;
- shapes/corner radii;
- typography choices;
- app surface/elevation treatment;
- motion durations/easing where custom motion is needed;
- icon/app-tile sizing;
- category header proportions;
- thumb-reachable layout behavior;
- light/dark/system theme behavior.

Use Material 3 and system dynamic color where appropriate, with a deterministic fallback theme.

Do not copy Samsung proprietary artwork/assets. One UI-inspired means proportions, interaction feel and reachability, not pixel-perfect cloning.

## Required v0.1 UI components

Build composables/UI state handling against fake/demo app-owned data for:

- companion organizer/shelf root surface;
- search field;
- favourites section/representation;
- category sections;
- app tiles with icon + label;
- `Unsorted` section/empty state;
- loading state;
- empty/no-results search state;
- long-press action surface containing at least move category, favourite/unfavourite and hide actions;
- hidden-app management surface or reusable component sufficient for Agent 50 to wire it into the final flow;
- light/dark/system presentation;
- accessibility semantics/content descriptions where icon-only controls would be ambiguous.

The UI must remain testable with fakes and must not instantiate repositories, DataStore or PackageManager internally.

## Presentation

Agent 10 owns the actual platform/window spike. Build the shelf root so Agent 50 can host it in whichever supported presentation Agent 10 proves:

1. sheet/translucent preferred;
2. dimmed/translucent fallback;
3. edge-to-edge activity fallback.

Do not hardwire UI architecture to blur or a specific Samsung-only window effect.

## Interaction design

Design for normal daily app launching:

- useful content immediately after state is available;
- search always easy to reach;
- category browsing should require minimal taps;
- app launch is a single tap;
- organization actions are discoverable by long press;
- touch targets remain accessible;
- common font/display scaling must not break the core flow.

Use stable Compose APIs only.

## Tests

Add UI tests at the cheapest useful layer using fake state.

At minimum cover:

- categories render from state;
- favourites render/update callback contract;
- search input/callback behavior;
- no-results state;
- long-press menu opens and exposes required actions;
- hide/restore management callbacks;
- accessibility semantics for critical controls;
- light/dark screenshot testing is **not** required unless a stable in-repo approach already exists; prefer semantics/behavior tests rather than introducing a snapshot dependency.

Do not add Robolectric or a mocking framework just to test Compose.

## Performance-conscious UI

Design lists/grids with appropriate lazy containers and stable keys from app-owned IDs.

Avoid premature custom caches. Keep recomposition-sensitive models immutable/stable where natural, but do not spread Compose annotations into domain/data contracts.

Leave actual macrobenchmarking to Agent 50 after real integration.

## Acceptance targets

This PR should satisfy or establish the UI implementation for Acceptance Criteria E3-E6 and the UI side of C1-C2/D1-D2. Platform presentation acceptance E1/E2 is finalized during Agent 10/50 integration.

## Final report

When finished, do not merge. Report:

1. branch and PR;
2. files changed;
3. design tokens/theme structure;
4. components/screens delivered;
5. fake UI-state contract used;
6. Compose/UI tests run and results;
7. accessibility work completed;
8. warnings/deprecations encountered;
9. any shared-contract change and sibling impact;
10. anything Agent 50 must know during integration.
