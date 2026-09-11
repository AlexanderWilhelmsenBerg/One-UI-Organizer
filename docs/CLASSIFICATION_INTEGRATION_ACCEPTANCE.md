# Classification Quality Integration / Acceptance

**Integration lane:** Agent 70 / `integration/classification-quality`

This document records the integrated result of the post-v0.1 classification-quality wave and the Samsung acceptance evidence supplied by the owner.

The raw Samsung launcher inventory remains private diagnostic input and is not committed.

## Integrated inputs

The integration starts from `main` after these classification PRs were merged:

- PR #9 — classification foundation and private evidence report;
- PR #12 — general known-app rules;
- PR #13 — game classification rules;
- PR #10 — Chromium WebAPK classification;
- PR #11 — `Unsorted` triage and classification explanations.

## Final taxonomy

The organizer taxonomy after the classification wave is:

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
- Action & Adventure
- RPG
- Strategy & Simulation
- Puzzle & Casual
- Board & Card
- Games
- Other
- Unsorted

`Games` remains the safe broad fallback. `Unsorted` remains a normal, usable fallback rather than a classification failure.

## Rule architecture and precedence

Bundled rules remain local, pure Kotlin and deterministic.

Inside the bundled-rule tier the selector precedence is:

1. exact launch component;
2. exact package;
3. package prefix.

The only package-prefix rule currently shipped is the evidence-backed, segment-bounded Chromium WebAPK namespace `org.chromium.webapk.`.

The outer classification precedence remains:

1. user override;
2. bundled known-app rule;
3. Android-declared category;
4. `Unsorted`.

Duplicate selectors and overlapping package prefixes fail fast. The integrated regression lane exercises representative rules from the general, game and Web shortcut packs together and re-proves the outer precedence contract.

## Samsung report and integration correction

The original classification evidence contained 566 launcher targets on the primary Samsung device. After the first integrated build was installed and exercised successfully, the owner generated a fresh classification report on the same device.

The aggregate result matched the original integration projection exactly:

- `Unsorted`: 124;
- broad `Games`: 23;
- RPG: 38;
- bundled known-app source: 235;
- Android-declared source: 207.

Manual inspection of the raw private report then found one genuine false positive that aggregate counts could not reveal. A launcher entry labeled `Eden Optimized` used package identity `com.miHoYo.Yuanshen` but launched through an `org.yuzu.yuzu_emu` activity. The exact-package game rule therefore forced an emulator variant into RPG.

Agent 70 removed `com.miHoYo.Yuanshen` from the RPG rule pack and added a regression proving that this evidence remains in broad `Games` through the Android `GAME` fallback.

The correction deliberately favors false-positive avoidance over trying to distinguish an official game installation from emulator variants sharing that package identity.

## Final expected same-device aggregate result

After the false-positive correction, the final expected result on the same 566-target population is:

| Category | Before | Final expected |
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
| Action & Adventure | 0 | 12 |
| RPG | 0 | 37 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 24 |
| Other | 0 | 0 |
| Unsorted | **225** | **124** |
| **Total** | **566** | **566** |

The integrated result still reduces `Unsorted` by 101 targets (44.9%). The final narrow game pack moves 105 of 129 broad game entries while deliberately retaining 24 in `Games`.

Final expected classification-source counts are:

| Classification source | Before | Final expected |
| --- | ---: | ---: |
| User override | 0 | 0 |
| Bundled known-app rule | 5 | 234 |
| Android-declared category | 336 | 208 |
| Unsorted fallback | 225 | 124 |
| **Total** | **566** | **566** |

The largest useful changes remain the general exact-package pack (100 previously `Unsorted` entries classified), the game pack (105 broad games narrowed after the false-positive correction), and the deliberately narrow WebAPK rule (one confirmed Web shortcut without broad browser/TWA guessing).

A second fresh Samsung report from the corrected build is required before these final expected figures are called device-confirmed.

## Web/PWA decision

`Web Shortcuts` is implemented only for generated Chromium WebAPK package identities under `org.chromium.webapk.`.

Ordinary Chrome/Samsung Internet packages, arbitrary TWA wrappers, labels that look like websites, URLs and launcher class-name heuristics are not used for classification. No deterministic Samsung Internet or general TWA signature was established in the reviewed evidence, so those remain on the normal classification path.

The first fresh Samsung report contained exactly one `Web Shortcuts` entry, matching the expected narrow rule behavior.

## Game decision

The five permanent narrow game buckets are:

- Action & Adventure
- RPG
- Strategy & Simulation
- Puzzle & Casual
- Board & Card

`Sports & Racing` was not added because the evidence did not justify a permanent bucket. `Other Games` was not added because `Games` already serves that safe fallback role.

The final corrected expected game distribution is 12 Action & Adventure, 37 RPG, 29 Strategy & Simulation, 23 Puzzle & Casual, 4 Board & Card and 24 broad `Games` fallback entries.

## Persisted-state / migration decision

The taxonomy change is additive. No existing `AppCategory` enum value was renamed or removed and organizer state remains schema version 1.

The integration lane includes a regression test that reads a literal pre-classification-wave schema-v1 payload and verifies that an existing manual category override, favourite and hidden state all survive unchanged. This is the repository-level migration proof for the additive taxonomy.

The owner reports that the updated application works correctly on the primary Samsung device. The supplied fresh classification report has `USER_OVERRIDE = 0`, however, and the report format does not expose favourite or hidden state. Therefore that report cannot independently prove the strict physical persisted-state test for those three user-owned state types. No migration failure has been observed, but that narrow acceptance item remains unproven by device evidence unless separately exercised.

## UI explanation / manual correction

The UI carries the actual app-owned `ClassificationSource` from categorized state into presentation state. It displays:

- `Your category`
- `Known app rule`
- `Android category`
- `Needs sorting`

Only automatic `Unsorted` fallback receives the direct triage `Sort` affordance. A deliberate user override to `Unsorted` remains distinguishable from automatic fallback and still uses the ordinary move action.

The owner reports the integrated application working on the primary Samsung device after installation of the classification-quality build.

## Quality lane

The permanent CI lane covers:

- debug assembly;
- instrumentation-test APK compilation;
- JVM tests;
- Android Lint;
- ktlint;
- dependency `buildHealth`;
- Gradle warning-mode failure;
- strict dependency verification;
- configuration-cache creation and reuse;
- forbidden `INTERNET` / `QUERY_ALL_PACKAGES` permission checks.

The classification integration lane deliberately adds `assembleDebugAndroidTest` because the triage Compose tests existed under `androidTest` but were not previously compiled by CI. This proves those tests stay buildable. It does **not** claim that device/emulator instrumentation execution occurred in CI.

No benchmark/profile infrastructure is added because no measured regression justified it.

## Samsung acceptance status

Confirmed from the first fresh device pass:

- the app installs and runs on the primary Samsung device;
- target count remains 566;
- `Unsorted` improved from 225 to 124;
- the five game buckets and Web shortcut rule rendered in the real report;
- the report mechanism works on the integrated build;
- manual review found one real false positive and led to a conservative correction;
- no raw launcher inventory has been committed.

Still required before final owner acceptance:

- install/run the corrected build;
- generate a second fresh report and confirm RPG = 37, broad `Games` = 24, `KNOWN_APP_RULE` = 234 and `ANDROID_DECLARED_CATEGORY` = 208 while total and `Unsorted` remain unchanged;
- if strict physical persisted-state acceptance is required, separately prove manual override/favourite/hidden survival because the report format cannot do that.

## Remaining classification limitations

Classification is intentionally conservative:

- 124 rows remain `Unsorted`;
- 24 games remain in broad `Games` after the false-positive correction;
- standard TWA wrappers and Samsung Internet/other browser-created shortcuts are not generically classified;
- known-app rules require maintenance as package identities change;
- exact package identity can be ambiguous for modified/repacked software, as demonstrated by the Eden emulator case;
- one primary category per app remains the product model;
- no cloud, network, label-guessing or probabilistic classifier is introduced.

## Recommended next development wave

The next coherent wave should be **user-owned category management**:

1. custom categories with stable app-owned category identifiers;
2. category reorder with persisted order;
3. richer category-management UI, including efficient bulk/manual organization where it earns its complexity.

This should begin with a small persistence/domain foundation because custom category identity and ordering are persisted-state changes and must have explicit migration tests.

Local backup/export/import should follow once that representation and migration contract are stable. Dynamic/pinned shortcuts should follow stable category identity. Presentation polish can proceed afterward or in a low-conflict UI lane. Performance tooling remains measurement-driven and should not be added unless measurements expose a real regression.
