# Agent 50 Prompt — Integration / Acceptance / Performance

Repository:

https://github.com/AlexanderWilhelmsenBerg/One-UI-Organizer

You are the **v0.1 integration, acceptance, regression-repair and performance-hardening owner** for One UI Organizer.

Your job is to integrate the completed Wave-1 lanes into one coherent app, close remaining Must-have behavior, run the full quality lane, and bring the project to a state the owner can physically test on Samsung hardware.

## Start condition

Do **not** start until these four Wave-1 PRs are merged to `main`:

- Agent 10 — platform app discovery/launch;
- Agent 20 — category/search domain;
- Agent 30 — state/repository;
- Agent 40 — Compose UI/design system.

Update from latest `main` and create/use branch:

`integration/v0.1`

## Mandatory reading

Read completely before editing:

- `AGENTS.md`
- `docs/STABLE_BASELINE.md`
- `docs/ENGINEERING_BASELINE.md`
- `docs/TECH_STACK.md`
- `docs/PLAN.md`
- `docs/ACCEPTANCE_CRITERIA.md`
- `docs/MOSCOW.md`
- `docs/PARALLEL_DEVELOPMENT.md`
- all merged Wave-1 PR descriptions/review notes if available.

## Critical rules

- Do **not merge** the PR.
- Do not broaden v0.1 scope beyond Must items/acceptance criteria.
- Do not replace working architecture simply because you prefer a different pattern.
- Do not change toolchain/library versions unless a concrete compatibility blocker requires an isolated, documented baseline update.
- No alpha/beta/RC dependencies.
- No `INTERNET` or `QUERY_ALL_PACKAGES`.
- No Samsung private launcher APIs.
- Keep build/compiler/deprecation/lint/format warnings at zero.

## Responsibilities

### 1. Composition root and wiring

Wire the real implementations together using constructor injection/manual composition:

```text
InstalledAppSource
      |
      v
CategoryEngine
      |
OrganizerStateStore
      |
      v
OrganizerRepository
      |
      v
ViewModel / immutable UI state
      |
      v
Compose shelf
```

Do not create a service locator/global manager to make wiring easier.

### 2. Final v0.1 behavior

Close all Must-have integration gaps, including:

- scan/rescan on app open/resume;
- categorized app model reaching UI;
- search by app/category;
- single-tap launch;
- category override from long press;
- favourite/unfavourite;
- hide;
- restore/manage hidden apps;
- loading/error/empty/unsorted behavior;
- persistence across process recreation;
- removed apps not surfaced as dead targets;
- manual corrections remain authoritative;
- chosen platform presentation from Agent 10 hosts the real shelf cleanly.

### 3. Regression and contract repair

If merged lanes do not integrate cleanly:

- determine which documented contract owns the mismatch;
- make the smallest repair;
- preserve app-owned models and boundary rules;
- add a regression test for the mismatch when practical;
- do not flatten boundaries merely to make wiring easier.

### 4. Full quality lane

Run the project's documented verification tasks from a clean state, including as configured:

- compile/assemble;
- unit tests;
- Compose tests;
- instrumented tests;
- Android Lint;
- ktlint;
- dependency `buildHealth`;
- dependency verification;
- Gradle warning/deprecation lane;
- configuration-cache verification;
- release build where available.

No lint baseline or broad suppression to force green.

### 5. Cross-app / UI Automator testing

Use stable UI Automator where it meaningfully proves:

- Organizer opens;
- selecting a sampled app transitions to the external target;
- back/home interaction remains sane;
- package/system UI behavior can be tested reliably.

Do not create brittle system-coordinate tests where semantic/system selectors are available.

### 6. Physical Samsung acceptance

Physical Samsung acceptance is mandatory before v0.1 release readiness.

Test/prepare checklist for:

- current Samsung One UI / Android 16 device;
- discovered app-set comparison;
- Samsung/Google/third-party/game/work app launch;
- alias/non-trivial launch entry if available;
- install/remove + reopen/rescan;
- category override persistence;
- favourite persistence;
- hidden restore;
- light/dark;
- common font/display scaling;
- sheet/translucent/fallback presentation;
- no crashes/ANRs in normal flows.

If the environment cannot access the physical device, provide an exact owner test script and leave those acceptance items explicitly pending.

### 7. Performance and benchmark adoption

Do not optimize by intuition.

First measure the integrated flow. Add the stable AndroidX Benchmark/Profile tooling from `STABLE_BASELINE.md` only when the real journey exists and the stable tooling cleanly works with the baseline build stack.

Measure relevant flows such as:

- cold organizer start;
- warm start;
- initial app scan;
- resume/rescan;
- search latency;
- category scrolling/frame timing;
- organizer -> external app launch.

Use Macrobenchmark for user journeys and Microbenchmark only for isolated pure hot code.

Compare before/after if you make a performance optimization.

The acceptance target in `ACCEPTANCE_CRITERIA.md` for usable results remains authoritative.

### 8. Privacy/package audit

Before declaring testable:

- verify manifest contains no `INTERNET`;
- verify no `QUERY_ALL_PACKAGES`;
- verify no analytics/ad/account SDKs;
- verify only documented package visibility declarations exist;
- verify local state contains only required organizer behavior.

### 9. Documentation truthfulness

Update documentation only where implementation has resolved a previously-open choice or device result. Keep `main` docs truthful:

- selected presentation/fallback;
- actual stale/reinstall policy;
- actual verification commands;
- performance/device results;
- remaining acceptance gaps.

Do not rewrite planning history unnecessarily.

## Acceptance target

The PR is ready for owner testing when:

- all automatable Must/acceptance criteria pass;
- all project quality gates are green;
- there are zero unexplained warnings;
- no known integration regression remains;
- device-only steps are either passed or presented as a precise checklist;
- the PR remains unmerged for owner review/test.

## Final report

When finished, do not merge. Report:

1. branch and PR;
2. Wave-1 PRs/commits integrated;
3. architecture/wiring summary;
4. remaining repairs made and why;
5. exact verification commands and results;
6. warning/deprecation/lint status;
7. benchmark/performance measurements if run;
8. privacy/package audit result;
9. Samsung device results or exact pending owner test steps;
10. any acceptance criteria still not passed;
11. recommendation: not testable / testable / release-candidate-ready — **never merge without explicit owner instruction**.
