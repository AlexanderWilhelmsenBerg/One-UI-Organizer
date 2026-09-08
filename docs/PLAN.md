# Product and Delivery Plan

## 1. Product definition

**One UI Organizer** is a small Android companion app that provides an automatically categorized app shelf while leaving Samsung One UI Home untouched as the system launcher.

The problem it solves is simple: One UI can sort apps alphabetically and lets the user create folders manually, but it does not maintain a useful category system automatically. One UI Organizer provides that organized view without taking ownership of the home screen, widgets, gestures, or Samsung launcher state.

## 2. Core constraints

1. One UI Home remains the default launcher.
2. The app must not write to Samsung private launcher databases or rely on undocumented Samsung APIs.
3. The app should not request `QUERY_ALL_PACKAGES` for v0.1.
4. v0.1 requires no Internet permission, account, telemetry, analytics, or backend.
5. Manual user classification always overrides automatic classification.
6. Scanning must not require a persistent service.
7. New dependencies must earn their place; the first version should remain structurally small.
8. The primary physical-device target is a current Samsung phone running modern One UI / Android 16, while the planned minimum Android version is API 28 (Android 9 / first-generation One UI era).

## 3. User experience

### Primary flow

- User taps the Organizer icon from One UI Home.
- Organizer opens as a fast One UI-inspired activity/sheet.
- A search control and categorized app sections are immediately available.
- User taps an app and the selected launchable activity opens.
- Back/dismiss returns directly to the existing Samsung home experience.

### Category model

v0.1 uses one **primary category** per app plus independent favourite/hidden state. Multi-category tagging is deliberately deferred.

Classification priority:

1. User override.
2. Bundled known-app rule.
3. Android `ApplicationInfo.category` mapping where useful.
4. `Unsorted` fallback.

Classification must be deterministic. The same installed-app metadata and rule set must produce the same result.

### Planned initial categories

The exact defaults can change during implementation, but the baseline taxonomy is:

- Favourites (virtual section)
- Communication
- Social
- Work
- Productivity
- Smart Home
- Homelab
- Finance
- Shopping
- Travel & Navigation
- Music & Audio
- Video
- Photos
- Reading
- Development
- Tools
- Games
- Other / Unsorted

Categories should not pretend to be authoritative. The UI must make correction easy.

## 4. Architecture

Keep v0.1 as a single Android application module unless a concrete reason appears to split it.

Suggested logical boundaries:

```text
Android PackageManager
        |
        v
InstalledAppSource
        |
        v
CategoryEngine <--- bundled known-app rules
        |
        v
OrganizerRepository <--- local user state
        |
        v
ViewModel / UI state
        |
        v
Jetpack Compose UI
```

### InstalledAppSource

Responsibilities:

- discover launcher-visible activities through supported Android APIs;
- expose package name, component, label, icon handle/loader, and declared Android category;
- exclude the Organizer itself from normal app results;
- deduplicate or explicitly represent packages that expose more than one launcher activity;
- rescan when the Organizer is opened/resumed rather than maintaining a permanent observer in v0.1.

Implementation should prefer an `ACTION_MAIN` + `CATEGORY_LAUNCHER` query declared in manifest package visibility over broad package visibility.

### CategoryEngine

Pure Kotlin logic with no Android UI dependency.

Inputs:

- installed-app metadata;
- bundled known-app rules;
- user overrides.

Outputs:

- primary category;
- source/reason for classification, useful for debugging and future UI explanation.

Suggested classification-source enum:

- `USER_OVERRIDE`
- `KNOWN_RULE`
- `ANDROID_CATEGORY`
- `FALLBACK`

### OrganizerRepository

Single source of truth combining discovered apps with persisted organizer state.

Persist only what must survive reinstall scans, for example:

- package/component identity as required;
- category overrides;
- favourite state;
- hidden state;
- category order/custom category definitions when those features are introduced.

Do not persist app icons or labels as authoritative data. Read them from the installed package when possible.

### Persistence

Start with typed DataStore state plus Kotlin serialization rather than Room.

Reasoning:

- the initial state is small;
- there are no relational queries that justify SQLite;
- avoiding Room also avoids KSP and generated database code in the first slice;
- Room 3 remains an available migration path if custom categories, multiple tags, history, rule editing, or complex queries materially grow the model.

### UI

Use Jetpack Compose + Material 3 with a small One UI-inspired design layer:

- large, thumb-reachable controls;
- generous top spacing where appropriate;
- rounded surfaces;
- system dynamic colour where it works, while keeping a predictable fallback theme;
- edge-to-edge support;
- translucent/scrim presentation when reliable on Samsung devices.

Do not copy Samsung proprietary assets. "One UI-inspired" describes interaction and proportion, not pixel-for-pixel reproduction.

## 5. Biggest technical risks to prove first

### Risk A — package visibility

Before building the product UI, prove that the intended package-query declaration returns the complete set of ordinary launchable apps expected on the Samsung test device without `QUERY_ALL_PACKAGES`.

### Risk B — reliable launching

Prove that an explicitly selected launcher component can be opened reliably, including apps with unusual launcher aliases.

### Risk C — sheet/translucent presentation

Prove the companion can open over One UI in a visually acceptable way on Android 16. A dimmed/translucent activity is acceptable. Blur is optional and must not become a dependency or compatibility requirement.

### Risk D — duplicate launcher activities

Some packages expose aliases or multiple launcher activities. Define deterministic behaviour before the main UI assumes package name alone is a unique launch target.

## 6. Delivery sequence

### Milestone 0 — planning baseline

Deliverables:

- README;
- plan;
- acceptance criteria;
- MoSCoW scope;
- technology decision matrix.

Exit condition: implementation scope is explicit and the first technical spike is defined.

### Milestone 1 — Android integration spike

Build only enough UI to prove the platform boundary.

Deliverables:

- Android project scaffold;
- launchable-app query;
- diagnostic list of label/package/component/category;
- app-launch action;
- translucent/sheet host experiment;
- tests for scanner mapping where practical.

Device test:

- current Samsung device on Android 16;
- compare Organizer's discovered set with the ordinary user-launchable apps visible through One UI;
- open a representative set including Samsung, Google, third-party, game, work, and aliased apps.

Do **not** implement the full organizer until this milestone passes.

### Milestone 2 — category engine and persistence

Deliverables:

- deterministic category model;
- Android-category mapping;
- bundled known-app rules;
- user override model;
- typed DataStore persistence;
- favourites and hidden state;
- unit tests covering precedence and persistence behaviour.

Exit condition: the repository can expose a categorized app model independent of final UI styling.

### Milestone 3 — usable v0.1 UI

Deliverables:

- categorized sections;
- search;
- app launch;
- long-press actions;
- move to category;
- favourite/unfavourite;
- hide/unhide management;
- empty/unsorted states;
- accessibility semantics;
- dark/light/system theme.

Exit condition: the app can replace manual browsing for normal daily app launching without replacing One UI Home.

### Milestone 4 — hardening and release candidate

Deliverables:

- unit and Compose UI coverage for critical flows;
- lint clean or documented suppressions;
- build/release workflow;
- package visibility regression test strategy;
- startup/scan performance check on the Samsung target device;
- backup/restore behaviour documented;
- privacy statement;
- release notes.

## 7. Post-v0.1 roadmap

### Near-term candidates

- editable/custom categories;
- reorder categories;
- pinned category shortcuts through Android ShortcutManager;
- category-specific launcher shortcuts;
- export/import organizer rules;
- richer known-app rules;
- optional app-widget surface.

### Later candidates

- Glance home-screen widget;
- multiple category tags per app;
- work-profile support;
- optional usage-based suggestions with explicit user consent;
- context/time-based favourite suggestions;
- ruleset sharing/import without requiring a cloud account.

## 8. Explicit non-goals

The project should resist scope creep into these areas unless a later decision changes the product:

- modifying native One UI folders/pages;
- becoming the default launcher;
- managing Samsung Home settings;
- always-running background service;
- cloud AI classification;
- ad/analytics SDKs;
- broad package visibility merely for convenience;
- iOS or desktop portability.

## 9. Testing strategy

### Unit tests

Prioritize pure tests for:

- category precedence;
- Android category mapping;
- known rule matching;
- fallback behaviour;
- search normalization;
- persisted override merge behaviour;
- uninstall/reinstall identity edge cases.

### Instrumented / Compose tests

Cover:

- search input and filtering;
- category rendering;
- long-press menu actions;
- favourite and hidden-state UI;
- launch intent construction at the Android boundary where possible.

### Physical-device acceptance

A Samsung physical-device pass is mandatory for v0.1 because the value proposition depends on interaction with One UI. Emulator-only acceptance is insufficient.

## 10. Definition of done for v0.1

v0.1 is done when all Must-have items in `MOSCOW.md` and all v0.1 criteria in `ACCEPTANCE_CRITERIA.md` pass, including the physical Samsung device test, without granting Internet access or `QUERY_ALL_PACKAGES`.
