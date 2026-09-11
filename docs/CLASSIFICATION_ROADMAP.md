# Classification Quality Roadmap and Result

This document records the post-v0.1 classification-quality wave from evidence through integration acceptance. The foundation, general rules, game rules, WebAPK rule and triage/explanation UI are merged. Agent 70 owns final integration and acceptance on `integration/classification-quality`.

See [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md) for the detailed acceptance record.

## 1. Original owner-device evidence

Agent 60 generated and privately reviewed a local classification report on the primary Samsung device. Raw launcher rows remain private and are not committed.

Sanitized baseline aggregates were:

- launcher targets: 566;
- `Unsorted`: 225;
- `Games`: 129;
- `ANDROID_DECLARED_CATEGORY`: 336;
- `KNOWN_APP_RULE`: 5;
- `USER_OVERRIDE`: 0;
- `UNSORTED_FALLBACK`: 225.

The evidence showed that deterministic package rules were sparse and established one generated Chromium WebAPK under the stable `org.chromium.webapk.` namespace. TWA-style wrappers were present without an equally safe universal identity rule.

## 2. Foundation decision — merged in PR #9

Agent 60 established:

- explicit local classification reporting;
- deterministic aggregate category/source counts;
- additive Web/game taxonomy;
- exact-component, exact-package and narrowly scoped package-prefix selectors;
- separate general/game/Web rule packs;
- tests preserving classification precedence;
- privacy rules keeping raw device inventory out of the repository.

## 3. Final taxonomy

The classification wave added:

- `WEB_SHORTCUTS` — Web Shortcuts;
- `GAME_ACTION_ADVENTURE` — Action & Adventure;
- `GAME_RPG` — RPG;
- `GAME_STRATEGY_SIMULATION` — Strategy & Simulation;
- `GAME_PUZZLE_CASUAL` — Puzzle & Casual;
- `GAME_BOARD_CARD` — Board & Card.

`GAMES` remains the broad safe fallback. `UNSORTED` remains a normal fallback. Existing enum values were not renamed or removed.

## 4. Rule architecture

Known-app rules remain pure Kotlin and Android-framework-free.

Selector precedence inside the bundled-rule tier is:

1. exact component;
2. exact package;
3. package prefix.

Outer classification precedence is:

1. user override;
2. bundled rule;
3. Android-declared category;
4. `Unsorted`.

Duplicate selectors and overlapping prefixes fail fast. Prefixes require a package-segment boundary. No label guessing, URL inference, cloud classification or generic rule DSL was introduced.

## 5. General-rule lane — merged in PR #12

The general lane expanded stable exact-package rules across communication, social, work, productivity, smart home, homelab, finance, shopping, travel/navigation, music/audio, video, photos, reading, development and tools.

Against the frozen evidence, the general lane alone reduced `Unsorted` from 225 to 125, a 100-target improvement without touching game/Web ownership.

## 6. Game lane — merged in PR #13, corrected by Agent 70

PR #13 initially added 106 narrow exact-package game rules. The first fresh Samsung acceptance report exposed one false positive: an `Eden Optimized` emulator variant used package identity `com.miHoYo.Yuanshen` while launching an `org.yuzu.yuzu_emu` activity. The package rule incorrectly forced it into RPG.

Agent 70 removed that ambiguous exact-package rule and added a regression. The final game pack contains 105 narrow rules.

Final expected same-device game distribution:

| Game category | Before | Final expected |
| --- | ---: | ---: |
| Action & Adventure | 0 | 12 |
| RPG | 0 | 37 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 24 |
| **Total** | **129** | **129** |

This correction follows the acceptance principle: an emulator or ambiguous gaming utility remains in broad `Games` rather than being forced into a genre.

## 7. Web/PWA lane — merged in PR #10

The production Web shortcut rule is intentionally narrow:

```text
org.chromium.webapk.* -> Web Shortcuts
```

The implemented selector is the segment-bounded prefix `org.chromium.webapk.`. It does not classify based on labels, URLs, activity-name fragments, browser names or Samsung launcher internals. Standard TWAs remain outside generic automatic classification.

Exactly one reviewed target moves into `Web Shortcuts`.

## 8. Triage/explanation lane — merged in PR #11

The UI carries the real `ClassificationSource` into presentation state and explains classification as:

- `Your category`;
- `Known app rule`;
- `Android category`;
- `Needs sorting`.

Automatic `UNSORTED_FALLBACK` entries receive a direct `Sort` affordance. Deliberate user overrides remain distinguishable.

## 9. Final integrated expected result

After the Agent 70 false-positive correction, the expected aggregate result against the same 566-target population is:

| Category | Final expected |
| --- | ---: |
| Communication | 6 |
| Social | 27 |
| Work | 2 |
| Productivity | 129 |
| Smart Home | 12 |
| Homelab | 6 |
| Finance | 13 |
| Shopping | 15 |
| Travel & Navigation | 21 |
| Music & Audio | 14 |
| Video | 28 |
| Photos | 7 |
| Reading | 12 |
| Web Shortcuts | 1 |
| Development | 3 |
| Tools | 17 |
| Action & Adventure | 12 |
| RPG | 37 |
| Strategy & Simulation | 29 |
| Puzzle & Casual | 23 |
| Board & Card | 4 |
| Games fallback | 24 |
| Other | 0 |
| Unsorted | **124** |
| **Total** | **566** |

Final expected source counts:

- `USER_OVERRIDE`: 0;
- `KNOWN_APP_RULE`: 234;
- `ANDROID_DECLARED_CATEGORY`: 208;
- `UNSORTED_FALLBACK`: 124.

`Unsorted` remains improved from 225 to 124. The game correction changes only one entry from narrow RPG back to broad `Games` and from known-rule source back to Android-declared source.

A second fresh report from the corrected build is required before these final figures are called device-confirmed.

## 10. Migration decision

The taxonomy expansion is additive only:

- no existing `AppCategory` value was renamed or removed;
- persisted override enum names remain valid;
- organizer-state schema remains version 1;
- no data migration is required.

Agent 70 includes a regression test that decodes a literal pre-wave schema-v1 payload and verifies category override, favourite and hidden state remain intact.

The supplied Samsung report contains `USER_OVERRIDE = 0` and does not expose favourites/hidden state, so strict physical persisted-state proof remains a separate acceptance item if required literally.

## 11. Integration quality

Agent 70 re-proves:

- all rule packs compose through one `KnownAppRuleSet`;
- user override wins;
- bundled rules beat Android category;
- unmatched Android games retain `Games` fallback;
- unmatched/undefined apps retain `Unsorted` fallback;
- the ambiguous Eden/Yuzu evidence remains broad `Games`.

The permanent CI lane is extended to compile the `androidTest` APK. This catches broken Compose/instrumentation test sources without pretending to execute device tests in headless CI.

## 12. Acceptance principle

The goal is not `Unsorted = 0` or `Games = 0`.

The successful result is:

- materially better useful grouping;
- low false-positive risk;
- deterministic local rules;
- preserved user corrections;
- understandable classification source;
- easy manual correction for the remainder.

The Eden correction is a concrete example of preferring a safe broad fallback over an aggressive narrow match.

## 13. Remaining limitations

- 124 entries remain `Unsorted`.
- 24 game entries remain broad `Games` after the false-positive correction.
- Standard TWA and non-Chromium browser shortcuts lack a universal safe rule.
- Exact package identities require maintenance and can be ambiguous for modified/repacked software.
- Automatic classification remains one primary category per app.

These limitations should not be solved by weakening determinism or adding network/cloud classification.

## 14. After Agent 70

The recommended next coherent wave is user-owned category management:

- custom categories with stable app-owned identifiers;
- persisted category order;
- richer manual/category management UI.

That wave should start with the persistence/domain contract and migration tests. Local backup/export/import and pinned/dynamic shortcuts should follow stable category identity. Performance work remains measurement-driven.
