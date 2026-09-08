# Agent 10 Prompt — Android App Discovery / Launch Platform Spike

Repository:

https://github.com/AlexanderWilhelmsenBerg/One-UI-Organizer

You are the **Android platform integration owner** for One UI Organizer.

Your lane is the supported Android boundary for discovering ordinary launcher-visible apps, mapping them into app-owned models, launching the exact selected target, and proving the companion presentation on a real Samsung device.

## Start condition

**Do not start until Agent 00 / foundation scaffold has been merged to `main`.**

Then update from the latest `main` and create/use branch:

`feature/platform-apps`

## Mandatory reading

Read completely before editing:

- `AGENTS.md`
- `docs/STABLE_BASELINE.md`
- `docs/ENGINEERING_BASELINE.md`
- `docs/TECH_STACK.md`
- `docs/PLAN.md`
- `docs/ACCEPTANCE_CRITERIA.md`
- `docs/PARALLEL_DEVELOPMENT.md`

## Critical rules

- Do **not merge** the PR.
- Stay inside the Android platform lane.
- Do not redesign shared contracts merely for convenience.
- Do not implement category rules, DataStore persistence or the final organizer shelf.
- Do not change build/toolchain/library versions.
- Do not add `QUERY_ALL_PACKAGES`.
- Do not add `INTERNET`.
- Do not use Samsung private launcher databases/APIs.
- Keep warnings at zero.

## Primary ownership

Prefer changes under:

```text
app/src/main/java/**/platform/apps/**
app/src/androidTest/**/platform/apps/**
```

A tiny diagnostic spike surface may be added where necessary to prove real-device behavior, but do not build the final UI owned by Agent 40/50.

## Required behavior

Implement the frozen `InstalledAppSource` and `AppLauncher` contracts from Agent 00.

### Discovery

Use supported Android APIs and package visibility declarations to discover normal launcher activities, centered on:

```text
ACTION_MAIN + CATEGORY_LAUNCHER
```

Requirements:

- no broad package-query permission;
- exclude One UI Organizer itself from the normal result set;
- capture package + exact launch component/target identity;
- map label and Android-declared app category into app-owned models;
- preserve deterministic ordering/identity;
- deal explicitly with packages that expose aliases or multiple launch activities;
- scanning must not block the main thread;
- rescan on request/open/resume is sufficient for v0.1; do not add a permanent package observer/service.

### Launching

Implement exact target launching behind `AppLauncher`.

Requirements:

- launch the selected component rather than assuming package name is always sufficient;
- return/represent launch failure through an app-owned result/error contract if the frozen contract supports it;
- do not leak raw `Intent` or package manager objects above the platform adapter.

### Companion presentation spike

Prove a supported One UI-friendly host presentation on the target Samsung/Android 16 device:

1. preferred translucent/sheet-like activity;
2. dimmed/translucent fallback if needed;
3. normal edge-to-edge fallback if OEM/window behavior makes the preferred presentation unreliable.

Blur is optional and must not become a dependency or acceptance blocker.

Document which presentation is selected and why.

## Tests

Add tests at the cheapest meaningful layers:

- mapper/unit tests for Android metadata -> app-owned model where possible;
- instrumentation tests for real package discovery behavior where stable;
- launch construction/selection tests;
- UI Automator only if useful for cross-app validation in this lane.

Do not mock Android internals extensively just to manufacture coverage; prefer narrow test seams/fakes.

## Physical Samsung spike

This lane has mandatory device work before full platform acceptance.

On the primary Samsung device, compare discovered targets against ordinary user-launchable apps visible through One UI and test a representative sample:

- Samsung app;
- Google app;
- third-party app;
- game;
- work/productivity app;
- launcher alias/non-trivial entry if present.

Also test:

- Organizer exclusion;
- install/remove followed by reopen/rescan;
- duplicate/alias behavior;
- return/dismiss behavior to One UI.

If the coding environment cannot access the physical device, leave a precise test checklist and mark device acceptance as pending rather than guessing.

## Acceptance targets

This PR should satisfy or provide the implementation foundation for Acceptance Criteria A1-A4 and I1-I4 without crossing into other lanes.

## Final report

When finished, do not merge. Report:

1. branch and PR;
2. files changed;
3. discovery API/manifest visibility approach;
4. duplicate/alias policy;
5. launch-target model behavior;
6. tests run and results;
7. Samsung-device results or exact pending checklist;
8. chosen presentation/fallback;
9. any shared-contract change requested, with justification;
10. anything Agent 50 must know during integration.
