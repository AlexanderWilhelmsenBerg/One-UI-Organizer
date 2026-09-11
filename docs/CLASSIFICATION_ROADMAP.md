# Classification Quality Roadmap

This document records the classification-quality wave after v0.1.

It is intentionally separate from PR #7's implementation scope. The v0.1 acceptance contract remains unchanged: unknown apps stay usable in `Unsorted`, and `Games` remains a valid safe fallback.

## Reviewed owner-device evidence

The Agent 60 local report was generated on the primary Samsung owner device and reviewed privately. Raw launcher rows remain private and are not committed.

Sanitized same-device aggregates:

- launcher targets: 566;
- `Unsorted`: 225;
- `Games`: 129;
- Android-declared category: 336;
- bundled known-app rule: 5;
- user override: 0;
- `Unsorted` fallback: 225.

Several existing categories had zero or one entry. The detailed report showed that this is primarily a rule-coverage problem rather than proof that those categories are useless: apps that naturally fit smart-home, finance, shopping, utility/tooling and other existing buckets still appear in `Unsorted` when Android declares no useful category. Existing persisted category values are therefore retained rather than consolidated solely from current counts.

The report also contained a generated Chromium WebAPK package under the stable `org.chromium.webapk.` namespace. Separate TWA-style wrappers were present without one equally safe global identity rule, so WebAPK identity is accepted as a narrow deterministic matcher seam while arbitrary TWA/activity-name inference is not.

These observations are product-quality evidence, not a discovery or v0.1 correctness failure.

## Agent 60 — classification foundation and evidence

Agent 60 owns the shared foundation before classification rules expand in parallel.

The foundation provides:

1. an explicit, user-initiated local classification report containing label, package, exact component/class, app-owned platform category, organizer category and `ClassificationSource`;
2. deterministic aggregate counts by organizer category and classification source;
3. stable rule composition with exact package, exact component and narrowly scoped package-prefix selectors;
4. separate ownership seams for general, game and Web/PWA rule packs;
5. regression tests that preserve user override > bundled rule > Android-declared category > `Unsorted`;
6. an evidence-reviewed additive taxonomy with no persisted-state migration.

The raw owner-device report is private diagnostic data. Do not commit it, attach it to a pull request, or turn it into test fixtures. Commit only generalized decisions, aggregate before/after counts where useful, reusable deterministic rules, tests and sanitized documentation.

## Taxonomy frozen by Agent 60

The post-v0.1 classification wave adds these organizer categories:

- `WEB_SHORTCUTS` — display/search label `Web Shortcuts`;
- `GAME_ACTION_ADVENTURE` — `Action & Adventure`;
- `GAME_RPG` — `RPG`;
- `GAME_STRATEGY_SIMULATION` — `Strategy & Simulation`;
- `GAME_PUZZLE_CASUAL` — `Puzzle & Casual`;
- `GAME_BOARD_CARD` — `Board & Card`.

`GAMES` remains the safe fallback for Android-declared games and for game titles without a reliable narrower bundled rule.

The roadmap candidates `Sports & Racing` and `Other Games` are not added in this wave. The owner report did not show enough sports/racing density to justify another permanent category, while `Other Games` would duplicate the role already served by `Games`. Strategy and Simulation are deliberately combined because the installed set contains substantial overlap and the product benefits more from one broad useful bucket than two sparse/ambiguous ones.

Existing zero/low-count categories are retained. Removing or renaming them would require migration of persisted override enum names, while the report shows plausible uncategorized apps that can populate several of those buckets once the general rule lane expands.

This taxonomy change is additive only. Organizer state remains schema version 1; no existing override enum name changes and no migration is required.

## Rule architecture frozen by Agent 60

Known-app rules remain pure Kotlin and Android-framework-free.

Supported deterministic selectors are:

- exact launch component identity;
- exact package identity;
- package namespace prefix, only for evidence-backed namespaces where generated package identities make exact-package enumeration inappropriate.

Within the bundled-rule tier the selector precedence is:

1. exact component;
2. exact package;
3. package prefix.

The outer classification precedence remains unchanged:

1. user override;
2. bundled deterministic rule;
3. Android-declared category;
4. `Unsorted`.

Duplicate selectors fail fast. Overlapping package-prefix selectors also fail fast so rule result cannot depend on list order. Prefix selectors require a trailing `.` package-segment boundary. `ClassificationSource.KNOWN_APP_RULE` remains the source for every bundled selector type.

The reviewed WebAPK evidence justifies the prefix matcher seam; it does **not** authorize label guessing, class-name heuristics, generic URL parsing, or broad TWA inference. The final Web/PWA lane owns the actual reusable rule pack.

No generic rule DSL exists.

## Parallel rule lanes after Agent 60 merges

These lanes start from the merged Agent 60 foundation and must not change shared rule infrastructure independently.

| Lane | Reserved production file | Scope |
|---|---|---|
| General known apps | `rules/general/GeneralKnownAppRules.kt` | Evidence-backed non-game package/component rules. |
| Games | `rules/games/GameKnownAppRules.kt` | Evidence-backed exact package/component rules targeting only the frozen game taxonomy; leave uncertain games in `Games`. |
| Web/PWA shortcuts | `rules/web/WebShortcutKnownAppRules.kt` | Evidence-backed WebAPK/package/component rules targeting `WEB_SHORTCUTS`; do not infer from labels or arbitrary TWA activity names. |

Each lane owns corresponding tests under its matching `app/src/test/**/rules/` package. Shared selector/index/composition files remain foundation-owned and should not be edited by parallel rule lanes unless a real evidence-backed contract change is coordinated first.

## Follow-on rule priorities from the report

The largest quality opportunity remains `Unsorted`, but it should be reduced with reusable exact identities rather than guesses. General-rule work should prioritize obvious, stable package identities represented in the owner report across categories such as communication, smart home, finance, shopping, travel, media, reading, development and tools.

The game lane should not classify all 129 titles mechanically. Add exact reusable rules where genre is clear, use the five frozen subcategories above, and keep `Games` for ambiguous/mixed titles.

The Web/PWA lane should start with the evidence-backed generated WebAPK namespace. TWA wrappers require separate evidence because a TWA launch component can front an app that users reasonably think of as an ordinary branded app rather than a generic web shortcut.

Each lane should report sanitized before/after aggregate effects from the same owner-device report where practical. Raw report rows remain private.

## Acceptance direction

The classification-quality wave should provide before/after category counts from the same owner device, identify deterministic rules responsible for meaningful improvements, and retain `Unsorted` as a safe fallback rather than forcing uncertain matches.

A useful result is not "zero Unsorted at any cost". A useful result is substantially better automatic grouping with low false-positive risk, deterministic tests, and easy manual correction for the remainder.
