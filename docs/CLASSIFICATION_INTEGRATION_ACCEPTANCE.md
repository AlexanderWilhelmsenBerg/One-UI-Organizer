# Classification Quality Integration / Acceptance

**Integration lane:** Agent 70 / `integration/classification-quality`

This document records the integrated post-v0.1 classification-quality result and owner-device acceptance evidence. Raw Samsung launcher rows remain private diagnostic input and are not committed.

## Integrated inputs

The integration starts from `main` after:

- PR #9 — classification foundation and private evidence report;
- PR #12 — general known-app rules;
- PR #13 — game classification rules;
- PR #10 — Chromium WebAPK classification;
- PR #11 — `Unsorted` triage and classification explanations.

Agent 70 also owns integration corrections discovered during owner-device review.

## Final taxonomy

The organizer taxonomy is:

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
- Web Shortcuts
- Development
- Tools
- Emulators
- Action & Adventure
- RPG
- Strategy & Simulation
- Puzzle & Casual
- Board & Card
- Games
- Other
- Unsorted

`Emulators` describes software whose primary role is running software for another platform or game system. Gaming frontends, streaming clients and controller utilities are not automatically folded into it.

`Games` remains the safe broad fallback for games and gaming software that does not have a justified narrower organizer category. `Unsorted` remains a normal usable fallback.

## Rule architecture and precedence

Bundled rules remain local, pure Kotlin and deterministic.

Inside the bundled-rule tier selector precedence is:

1. exact launch component;
2. exact package;
3. package prefix.

The outer classification precedence remains:

1. user override;
2. bundled known-app rule;
3. Android-declared category;
4. `Unsorted`.

Duplicate selectors and overlapping package prefixes fail fast. The only package-prefix rule currently shipped is the evidence-backed Chromium WebAPK namespace `org.chromium.webapk.`.

The emulator pack is separate from game-genre rules. It contains 14 exact-package rules plus one exact-component rule for the observed Eden/Yuzu launcher identity.

## Samsung report and integration corrections

The original evidence contained 566 launcher targets. The first integrated Samsung report matched the then-projected aggregates but row-level review found a real false positive: `Eden Optimized` reused package identity `com.miHoYo.Yuanshen` while launching an `org.yuzu.yuzu_emu` activity. A package-only RPG rule therefore misclassified an emulator variant.

Agent 70 first removed the ambiguous package RPG rule. Owner review then approved a first-class `Emulators` category. The final implementation uses an exact component selector for the observed Eden/Yuzu launcher, so the emulator variant can be classified without treating every installation sharing the package name as an emulator.

The emulator pack also classifies the evidence-backed emulator packages for DraStic, Flycast, DuckStation, RetroArch, Cemu, Azahar, RPCSX, Citra, citron, Dolphin, PPSSPP, ScummVM, Sudachi and NetherSX2.

The final owner-device report from the emulator-category build contained the same 566 launch targets and matched the predicted aggregate result exactly. Manual review confirmed all 15 `Emulators` rows are emulator software/components, including the corrected Eden/Yuzu launcher and Citra.

The 10 remaining broad `Games` rows are intentionally not forced into emulator or genre buckets. They include MonsterFactory, Magic Timer, Xbox Game Pass, Moonlight, Artemis, Prado, Better xCloud, Winlator, GameHub and ES-DE. Several are clearly frontends, streaming or compatibility software; the remainder lack enough evidence for a narrower permanent game bucket and therefore stay safely broad.

## Final device-confirmed same-device aggregate result

On the same 566-target population the current branch produces:

| Category | Before | Device confirmed |
| --- | ---: | ---: |
| Communication | 0 | 6 |
| Social | 24 | 27 |
| Work | 1 | 2 |
| Productivity | 150 | 129 |
| Smart Home | 1 | 12 |
| Homelab | 0 | 6 |
| Finance | 1 | 13 |
| Shopping | 0 | 15 |
| Travel & Navigation | 4 | 21 |
| Music & Audio | 12 | 14 |
| Video | 12 | 28 |
| Photos | 4 | 7 |
| Reading | 1 | 12 |
| Web Shortcuts | 0 | 1 |
| Development | 2 | 3 |
| Tools | 0 | 17 |
| Emulators | 0 | **15** |
| Action & Adventure | 0 | 12 |
| RPG | 0 | **37** |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | **10** |
| Other | 0 | 0 |
| Unsorted | **225** | **123** |
| **Total** | **566** | **566** |

Device-confirmed classification-source counts are:

| Classification source | Before | Device confirmed |
| --- | ---: | ---: |
| User override | 0 | 0 |
| Bundled known-app rule | 5 | **249** |
| Android-declared category | 336 | **194** |
| Unsorted fallback | 225 | **123** |
| **Total** | **566** | **566** |

Compared with the first fresh integrated report, the emulator pack moves 13 broad Android `Games` entries, the Eden/Yuzu component, and one previously `Unsorted` Citra entry into `Emulators`.

The classification improvement is therefore device-confirmed rather than projected: `Unsorted` falls from 225 to 123 while preserving conservative fallbacks, and all 566 launch targets remain accounted for.

## Game decision

The permanent narrow game genres remain:

- Action & Adventure
- RPG
- Strategy & Simulation
- Puzzle & Casual
- Board & Card

`Sports & Racing` is not added because the evidence does not justify it. `Other Games` is unnecessary because `Games` is the broad fallback.

Emulators are now deliberately outside the genre taxonomy. Gaming frontends and streaming tools remain in their existing category/fallback unless separately justified.

## Web/PWA decision

`Web Shortcuts` remains limited to generated Chromium WebAPK package identities under `org.chromium.webapk.`. Ordinary browsers, arbitrary TWA wrappers, labels, URLs and class-name guessing are not used for generic Web classification.

## Persisted-state / migration decision

The taxonomy changes are additive. No existing `AppCategory` value is renamed or removed and organizer state remains schema version 1.

The integration lane includes a regression that reads a literal pre-wave schema-v1 payload and verifies an existing manual category override, favourite and hidden state survive unchanged.

The final device report has `USER_OVERRIDE = 0` and does not expose favourite or hidden state, so strict physical proof for those state types remains a separate acceptance item if required literally. No migration failure has been observed.

## Optional network metadata enrichment decision

The owner has approved Internet access in principle for useful category/tag metadata. This PR does **not** add `INTERNET`, a networking dependency or runtime metadata lookup yet.

The reason is architectural rather than policy resistance: Google Play's documented Developer APIs manage a developer's own apps and do not provide a general supported API for querying arbitrary installed-package categories/tags. Unofficial Play scraping is therefore not accepted as a production dependency.

F-Droid publishes documented repository indexes and package metadata with categories, so it is a viable future provider for the subset of installed apps present there. A future metadata-enrichment slice should:

- define an app-owned metadata/provider contract;
- keep provider/network types at an adapter boundary;
- cache metadata locally and never block the normal package scan on network availability;
- map provider-specific categories/tags into the organizer taxonomy explicitly;
- add a distinct classification source if external metadata can decide category;
- preserve user override precedence;
- disclose network behavior and add `INTERNET` only in the same change that implements a supported provider;
- keep bundled deterministic rules as the high-confidence local tier.

The current build therefore remains offline by implementation, while the product scope now permits a future supported metadata provider.

## Quality lane

The permanent CI lane covers debug assembly, instrumentation-test APK compilation, JVM tests, Android Lint, ktlint, dependency `buildHealth`, warning-mode failure, strict dependency verification, configuration-cache creation/reuse and forbidden-permission checks.

`assembleDebugAndroidTest` remains a compile gate, not a claim of device/emulator instrumentation execution.

CI run #192 passed the full repository quality lane for the emulator-category implementation before the final documentation-only acceptance update.

## Samsung acceptance status

Confirmed from the final owner-device pass:

- the current emulator-category build installs and runs on the primary Samsung device;
- target count remains 566;
- aggregate category counts match the predicted result exactly;
- aggregate classification-source counts match the predicted result exactly;
- `Emulators = 15`, broad `Games = 10`, RPG = 37 and `Unsorted = 123`;
- Eden/Yuzu is correctly classified as `Emulators` through its exact launch component;
- Citra is correctly classified as `Emulators` instead of `Unsorted`;
- all reviewed emulator rows are actual emulator software/components;
- ambiguous gaming frontends/streaming/compatibility software remains safely broad instead of being over-classified;
- no raw launcher inventory is committed.

The classification-quality device gate is complete.

If strict physical persisted-state acceptance is required literally, manual override/favourite/hidden survival remains a separate narrow check because the classification report cannot observe those state types. Repository migration coverage for them is green.

## Remaining limitations

- 123 entries remain `Unsorted` by design rather than being guessed into categories.
- 10 entries remain broad `Games` by design.
- gaming frontends/streaming clients are not automatically treated as emulators.
- standard TWA and non-Chromium browser shortcuts lack a universal safe rule.
- known-app rules require maintenance as package identities change.
- external metadata enrichment is approved in scope but not yet implemented.
- one primary category per app remains the current product model.

## Recommended next development wave

The next coherent product wave remains **user-owned category management**:

1. stable custom-category identifiers;
2. persisted category order;
3. create/rename/delete/reorder behavior;
4. richer category-management UI and Samsung migration acceptance.

A small optional metadata-enrichment investigation can run as a separate low-conflict architecture lane, but it should not destabilize the custom-category persistence foundation. Local backup/export/import and dynamic/pinned shortcuts should follow stable category identity.
