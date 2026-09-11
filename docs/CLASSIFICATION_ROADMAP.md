# Classification Quality Roadmap and Result

This document records the post-v0.1 classification-quality wave from evidence through merged implementation. The wave is no longer future planning: the foundation, general rules, game rules, WebAPK rule and triage/explanation UI are merged. Agent 70 owns final integration/acceptance.

See [`CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`](CLASSIFICATION_INTEGRATION_ACCEPTANCE.md) for the integrated acceptance record and remaining Samsung-device steps.

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

The evidence showed that several small/empty organizer categories were under-populated because deterministic package rules were sparse, not because the categories were inherently useless. Existing persisted category values were therefore retained.

The evidence also established a generated Chromium WebAPK package under the stable `org.chromium.webapk.` namespace. TWA-style wrappers were present without one equally safe universal identity rule.

## 2. Foundation decision — merged in PR #9

Agent 60 established the shared classification foundation:

- explicit local classification reporting;
- deterministic aggregate category/source counts;
- separate general/game/Web rule packs;
- additive taxonomy expansion;
- deterministic selector/index infrastructure;
- tests preserving outer classification precedence;
- privacy rule that raw device inventory never enters the repository.

## 3. Final taxonomy decision

The classification wave added:

- `WEB_SHORTCUTS` — Web Shortcuts;
- `GAME_ACTION_ADVENTURE` — Action & Adventure;
- `GAME_RPG` — RPG;
- `GAME_STRATEGY_SIMULATION` — Strategy & Simulation;
- `GAME_PUZZLE_CASUAL` — Puzzle & Casual;
- `GAME_BOARD_CARD` — Board & Card.

`GAMES` remains the broad safe fallback.

`Sports & Racing` was not added because the reviewed inventory did not justify another permanent bucket. `Other Games` was not added because `Games` already fills that role. Strategy and Simulation remain deliberately combined.

Existing zero/low-count categories were retained. No existing enum value was renamed or removed.

## 4. Rule architecture — frozen and retained

Known-app rules are pure Kotlin and Android-framework-free.

Supported selectors are:

- exact launch component;
- exact package;
- package namespace prefix for narrowly evidence-backed generated identities.

Selector precedence inside the bundled-rule tier is:

1. exact component;
2. exact package;
3. package prefix.

Outer classification precedence is:

1. user override;
2. bundled rule;
3. Android-declared category;
4. `Unsorted`.

Duplicate selectors fail fast. Overlapping package prefixes fail fast. Prefixes require a trailing `.` package-segment boundary. Every bundled selector reports `ClassificationSource.KNOWN_APP_RULE`.

No generic rule DSL, display-label guessing, URL parsing or probabilistic classifier was introduced.

## 5. General-rule lane — merged in PR #12

The general lane expanded stable exact-package rules across communication, social, work, productivity, smart home, homelab, finance, shopping, travel/navigation, music/audio, video, photos, reading, development and tools.

Against the frozen evidence, the general lane alone reduced `Unsorted` from 225 to 125, a 100-target improvement without touching game/Web ownership.

All general production selectors are exact package identities.

## 6. Game lane — merged in PR #13

The game pack adds 106 high-confidence exact-package rules across the five frozen buckets.

Against the same frozen game population:

| Game category | Before | After game lane |
| --- | ---: | ---: |
| Action & Adventure | 0 | 12 |
| RPG | 0 | 38 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 23 |
| **Total** | **129** | **129** |

Ambiguous/mixed titles and gaming utilities remain in `Games`. The lane does not mechanically force every game into a genre.

## 7. Web/PWA lane — merged in PR #10

The production Web shortcut rule is intentionally narrow:

```text
org.chromium.webapk.* -> Web Shortcuts
```

The implemented selector is the segment-bounded prefix `org.chromium.webapk.`.

It does not classify based on labels, URLs, activity-name fragments, browser names or Samsung launcher internals. Standard TWAs remain out of scope because their package identities are developer-controlled. No generic Samsung Internet/other-browser shortcut signature was established.

Against the frozen evidence, exactly one previously `Unsorted` target moves into `Web Shortcuts`.

## 8. Triage/explanation lane — merged in PR #11

The UI now carries the real `ClassificationSource` into presentation state and explains classification as:

- `Your category`;
- `Known app rule`;
- `Android category`;
- `Needs sorting`.

Automatic `UNSORTED_FALLBACK` entries receive a direct `Sort` affordance. A deliberate user override to `Unsorted` remains distinguishable and does not masquerade as automatic fallback.

The existing move/favourite/hide/restore behavior remains available through the normal organization flow.

## 9. Integrated frozen-evidence projection

Because the general lane excluded game/Web rows, the game lane only subdivides the original 129-game population, and the WebAPK lane affects one previously `Unsorted` row, their sanitized effects can be combined deterministically against the original frozen 566-target evidence set.

| Category | Integrated projection |
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
| RPG | 38 |
| Strategy & Simulation | 29 |
| Puzzle & Casual | 23 |
| Board & Card | 4 |
| Games fallback | 23 |
| Other | 0 |
| Unsorted | **124** |
| **Total** | **566** |

Classification-source projection:

- `USER_OVERRIDE`: 0;
- `KNOWN_APP_RULE`: 235;
- `ANDROID_DECLARED_CATEGORY`: 207;
- `UNSORTED_FALLBACK`: 124.

This is a projection against the **same frozen report**, not a fresh current-device capture. Agent 70 acceptance still requires generating a new report from the same Samsung device and recording only sanitized aggregates/generalized findings.

## 10. Migration decision

The taxonomy expansion is additive only:

- no existing `AppCategory` value was renamed or removed;
- persisted override enum names remain valid;
- organizer-state schema remains version 1;
- no data migration is required.

Agent 70 adds a regression test that decodes a literal pre-wave schema-v1 payload and verifies an existing category override, favourite and hidden state remain intact.

The physical upgrade check must still install over existing owner-device data without clearing it.

## 11. Integration quality decision

The merged rule packs construct one `KnownAppRuleSet`, so duplicate selectors or overlapping prefixes fail at the shared composition boundary rather than depending on list order.

Agent 70 adds representative cross-pack integration tests and re-proves:

- user override wins;
- bundled rules beat Android category;
- unmatched Android games retain `Games` fallback;
- unmatched/undefined apps retain `Unsorted` fallback.

The permanent CI lane is also extended to compile the `androidTest` APK. This catches broken Compose/instrumentation test sources but does not pretend to execute device tests in headless CI.

## 12. Acceptance principle

The goal is not `Unsorted = 0`.

The successful result is:

- materially better useful grouping;
- low false-positive risk;
- deterministic local rules;
- preserved user corrections;
- understandable classification source;
- easy manual correction for the remainder.

The frozen-evidence result meets that direction: `Unsorted` projects from 225 to 124 and broad `Games` from 129 to 23 without broad inference.

## 13. Remaining limitations

- A substantial `Unsorted` fallback remains intentionally.
- 23 game entries remain broad `Games` in the frozen evidence.
- Standard TWA and non-Chromium browser shortcuts lack a universal safe rule.
- Exact known-app identities need maintenance as package names/products change.
- Automatic classification remains one primary category per app.

These limitations should not be solved by weakening determinism or adding network/cloud classification.

## 14. After Agent 70

The recommended next coherent product wave is user-owned category management:

- custom categories with stable app-owned identifiers;
- persisted category order;
- richer manual/category management UI.

That wave should start with the persistence/domain contract and migration tests. Local backup/export/import should follow once custom-category identity/order are stable. Pinned/dynamic shortcuts should also follow stable category identity. Performance work remains measurement-driven.
