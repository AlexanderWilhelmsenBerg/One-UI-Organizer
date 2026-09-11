# Classification Quality Roadmap and Result

This document records the post-v0.1 classification-quality wave from evidence through integration acceptance. The foundation, general rules, game rules, WebAPK rule and triage/explanation UI are merged. Agent 70 owns final integration and owner-device acceptance on `integration/classification-quality`.

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

The evidence also established one generated Chromium WebAPK and a substantial emulator population that became clearer during row-level acceptance review.

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

The classification wave includes:

- `WEB_SHORTCUTS` — Web Shortcuts;
- `EMULATORS` — Emulators;
- `GAME_ACTION_ADVENTURE` — Action & Adventure;
- `GAME_RPG` — RPG;
- `GAME_STRATEGY_SIMULATION` — Strategy & Simulation;
- `GAME_PUZZLE_CASUAL` — Puzzle & Casual;
- `GAME_BOARD_CARD` — Board & Card.

`EMULATORS` is a software-role category rather than a game genre. `GAMES` remains the broad safe fallback. `UNSORTED` remains a normal fallback. Existing enum values are not renamed or removed.

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

Duplicate selectors and overlapping prefixes fail fast. Prefixes require a package-segment boundary. No display-label guessing or probabilistic classifier is introduced.

## 5. General-rule lane — merged in PR #12

The general lane expanded stable exact-package rules across communication, social, work, productivity, smart home, homelab, finance, shopping, travel/navigation, music/audio, video, photos, reading, development and tools.

Against the frozen evidence, the general lane alone reduced `Unsorted` from 225 to 125, a 100-target improvement without touching game/Web ownership.

## 6. Game lane — merged in PR #13, corrected by Agent 70

PR #13 initially added 106 narrow exact-package game rules. The first fresh Samsung acceptance report exposed one false positive: an `Eden Optimized` emulator variant used package identity `com.miHoYo.Yuanshen` while launching an `org.yuzu.yuzu_emu` activity. The package rule incorrectly forced it into RPG.

Agent 70 removed that ambiguous package game rule and added regression coverage. The narrow game pack now contains 105 genre rules.

Before the emulator pack is applied, the corrected game-only distribution is:

| Game category | Before | Corrected game lane |
| --- | ---: | ---: |
| Action & Adventure | 0 | 12 |
| RPG | 0 | 37 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 24 |
| **Total** | **129** | **129** |

## 7. Emulator integration correction — Agent 70

Owner review established that emulators form a useful and sufficiently large category distinct from game genres.

The emulator pack adds:

- 14 exact-package rules for evidence-backed emulator identities;
- one exact-component rule for the observed Eden/Yuzu launcher identity.

The exact-component rule is important because the Eden emulator variant shares `com.miHoYo.Yuanshen` with software that must not be assumed to be an emulator based on package identity alone.

The evidence-backed package rules cover DraStic, Flycast, DuckStation, RetroArch, Cemu, Azahar, RPCSX, Citra, citron, Dolphin, PPSSPP, ScummVM, Sudachi and NetherSX2.

Gaming frontends, streaming clients and controller utilities are not automatically treated as emulators.

## 8. Web/PWA lane — merged in PR #10

The production Web shortcut rule remains intentionally narrow:

```text
org.chromium.webapk.* -> Web Shortcuts
```

It does not classify based on labels, URLs, activity-name fragments, browser names or Samsung launcher internals. Standard TWAs remain outside generic automatic classification.

## 9. Triage/explanation lane — merged in PR #11

The UI carries the real `ClassificationSource` into presentation state and explains classification as:

- `Your category`;
- `Known app rule`;
- `Android category`;
- `Needs sorting`.

Automatic `UNSORTED_FALLBACK` entries receive a direct `Sort` affordance. Deliberate user overrides remain distinguishable.

## 10. Final integrated device-confirmed result

The final Samsung report from the emulator-category build confirms the aggregate result exactly against the same 566-target population:

| Category | Device confirmed |
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
| Emulators | **15** |
| Action & Adventure | 12 |
| RPG | **37** |
| Strategy & Simulation | 29 |
| Puzzle & Casual | 23 |
| Board & Card | 4 |
| Games fallback | **10** |
| Other | 0 |
| Unsorted | **123** |
| **Total** | **566** |

Device-confirmed source counts:

- `USER_OVERRIDE`: 0;
- `KNOWN_APP_RULE`: **249**;
- `ANDROID_DECLARED_CATEGORY`: **194**;
- `UNSORTED_FALLBACK`: **123**.

Compared with the first fresh integrated report, 13 broad Android game entries, the Eden/Yuzu component and one previously `Unsorted` Citra entry move to `Emulators`.

Manual review confirms all 15 emulator rows are actual emulator software/components. The 10 remaining broad `Games` rows are MonsterFactory, Magic Timer, Xbox Game Pass, Moonlight, Artemis, Prado, Better xCloud, Winlator, GameHub and ES-DE. Several are frontends, streaming or compatibility software; the remainder lack enough evidence for a narrower permanent bucket.

## 11. Migration decision

The taxonomy expansion is additive only:

- no existing `AppCategory` value is renamed or removed;
- persisted override enum names remain valid;
- organizer-state schema remains version 1;
- no data migration is required.

Agent 70 includes a regression test that decodes a literal pre-wave schema-v1 payload and verifies category override, favourite and hidden state remain intact.

The final Samsung report contains `USER_OVERRIDE = 0` and does not expose favourites/hidden state, so strict physical persisted-state proof remains a separate acceptance item if required literally. Repository migration coverage is green.

## 12. Optional metadata enrichment direction

The owner permits future Internet-backed category/tag enrichment.

Google Play categories/tags are useful evidence, but Google's documented Developer APIs do not provide a general arbitrary-package catalog API for this product. Unofficial Play scraping is therefore not accepted as a production dependency.

F-Droid publishes documented indexes and package metadata with categories and is a viable future provider for the subset it covers.

A future provider must be local-first, cached, best-effort, mapped explicitly into app-owned categories and lower precedence than user overrides and bundled high-confidence rules. `INTERNET` should be added only together with the supported provider implementation.

## 13. Integration quality

Agent 70 re-proves:

- all rule packs compose through one `KnownAppRuleSet`;
- user override wins;
- bundled rules beat Android category;
- exact-component evidence can disambiguate a shared package identity;
- unmatched Android games retain `Games` fallback;
- unmatched/undefined apps retain `Unsorted` fallback.

The permanent CI lane compiles the `androidTest` APK in addition to the existing unit/lint/format/dependency/configuration-cache gates.

## 14. Acceptance principle

The goal is not `Unsorted = 0` or `Games = 0`.

The successful result is materially better grouping with low false-positive risk, preserved user corrections, understandable classification source and easy manual correction.

The Eden correction demonstrates why exact-component evidence can be preferable to package-only assumptions. The emulator category demonstrates that the organizer taxonomy should serve useful personal organization rather than mirror a storefront taxonomy mechanically.

## 15. Remaining limitations

- 123 entries remain `Unsorted`.
- 10 entries remain broad `Games`.
- gaming frontends/streaming clients are not automatically classified as emulators.
- standard TWA and non-Chromium browser shortcuts lack a universal safe rule.
- exact known-app identities require maintenance.
- optional network metadata enrichment is not yet implemented.
- automatic classification remains one primary category per app.

## 16. After Agent 70

The classification-quality device gate is complete. The recommended next coherent wave is user-owned category management:

- custom categories with stable app-owned identifiers;
- persisted category order;
- richer manual/category management UI.

A metadata-enrichment investigation may run as a separate architecture lane. Local backup/export/import and pinned/dynamic shortcuts should follow stable category identity. Performance work remains measurement-driven.
