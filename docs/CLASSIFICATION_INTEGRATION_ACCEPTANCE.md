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

## Same-device before/after result

The original classification evidence contained 566 launcher targets on the primary Samsung device. After the integrated build was installed and exercised successfully, the owner generated a fresh classification report on the same device.

The fresh report exactly matches the deterministic integrated projection at the aggregate level. Only sanitized aggregate counts are recorded here.

| Category | Before | Fresh Samsung result |
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
| RPG | 0 | 38 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 23 |
| Other | 0 | 0 |
| Unsorted | **225** | **124** |
| **Total** | **566** | **566** |

The integrated result reduces `Unsorted` by 101 targets (44.9%) while retaining uncertain entries there, and narrows 106 of the 129 broad game entries while retaining 23 in `Games`.

Fresh classification-source counts are:

| Classification source | Before | Fresh Samsung result |
| --- | ---: | ---: |
| User override | 0 | 0 |
| Bundled known-app rule | 5 | 235 |
| Android-declared category | 336 | 207 |
| Unsorted fallback | 225 | 124 |
| **Total** | **566** | **566** |

The largest useful changes are the general exact-package pack (100 previously `Unsorted` entries classified), the game pack (106 broad games narrowed), and the deliberately narrow WebAPK rule (one confirmed Web shortcut without broad browser/TWA guessing).

## Web/PWA decision

`Web Shortcuts` is implemented only for generated Chromium WebAPK package identities under `org.chromium.webapk.`.

Ordinary Chrome/Samsung Internet packages, arbitrary TWA wrappers, labels that look like websites, URLs and launcher class-name heuristics are not used for classification. No deterministic Samsung Internet or general TWA signature was established in the reviewed evidence, so those remain on the normal classification path.

The fresh Samsung report contains exactly one `Web Shortcuts` entry, matching the expected narrow rule behavior.

## Game decision

The five permanent narrow game buckets are:

- Action & Adventure
- RPG
- Strategy & Simulation
- Puzzle & Casual
- Board & Card

`Sports & Racing` was not added because the evidence did not justify a permanent bucket. `Other Games` was not added because `Games` already serves that safe fallback role.

The fresh Samsung report matches the projected game distribution exactly: 12 Action & Adventure, 38 RPG, 29 Strategy & Simulation, 23 Puzzle & Casual, 4 Board & Card and 23 broad `Games` fallback entries.

## Persisted-state / migration decision

The taxonomy change is additive. No existing `AppCategory` enum value was renamed or removed and organizer state remains schema version 1.

The integration lane includes a regression test that reads a literal pre-classification-wave schema-v1 payload and verifies that an existing manual category override, favourite and hidden state all survive unchanged. This is the repository-level migration proof for the additive taxonomy.

The owner reports that the updated application works correctly on the primary Samsung device. The supplied fresh classification report has `USER_OVERRIDE = 0`, however, and the report format does not expose favourite or hidden state. Therefore this report cannot independently prove the strict physical persisted-state test for those three user-owned state types. No migration failure has been observed, but that narrow acceptance item remains unproven by device evidence unless separately exercised.

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

Final repository CI is green. No benchmark/profile infrastructure is added because no measured regression justified it.

## Samsung acceptance

The owner installed and exercised the integrated application on the primary Samsung device and reports that the application works. A fresh classification report was then generated from the running build.

Confirmed from the fresh report:

- target count remains 566;
- every category aggregate matches the integration projection exactly;
- every classification-source aggregate matches the integration projection exactly;
- `Unsorted` is 124 rather than the original 225;
- broad `Games` is 23 rather than the original 129;
- all five new game buckets contain the expected counts;
- `Web Shortcuts` contains exactly one entry;
- no raw launcher inventory has been committed.

No classification integration discrepancy was found between the frozen evidence calculation and the fresh device result.

The only remaining strict acceptance caveat is the persisted-user-state device test described above: the fresh report contains no user override and cannot report favourites or hidden state.

## Manual false-positive review

The integration review sampled exact-package identities across general categories and all five game buckets, plus the WebAPK boundary. The rule architecture contains no selector collision and no integration-level false positive was identified from the reviewed repository evidence.

The fresh device aggregates exactly matching the projection provide additional confidence that no unexpected rule-composition behavior occurred. `Unsorted` and broad `Games` intentionally remain available rather than forcing uncertain entries into narrow categories.

## Remaining classification limitations

Classification is intentionally conservative:

- 124 rows remain `Unsorted`;
- 23 games remain in broad `Games`;
- standard TWA wrappers and Samsung Internet/other browser-created shortcuts are not generically classified;
- known-app rules require maintenance as package identities change;
- one primary category per app remains the product model;
- no cloud, network, label-guessing or probabilistic classifier is introduced.

## Recommended next development wave

The next coherent wave should be **user-owned category management**:

1. custom categories with stable app-owned category identifiers;
2. category reorder with persisted order;
3. richer category-management UI, including efficient bulk/manual organization where it earns its complexity.

This should begin with a small persistence/domain foundation because custom category identity and ordering are persisted-state changes and must have explicit migration tests.

Local backup/export/import should follow once that representation and migration contract are stable; otherwise the export format would be versioned around a moving schema. Dynamic/pinned shortcuts should follow stable category identity so shortcuts target durable categories. Presentation polish can proceed afterward or in a low-conflict UI lane. Performance tooling remains measurement-driven and should not be added unless measurements expose a real regression.
