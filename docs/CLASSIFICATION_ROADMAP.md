# Classification Quality Roadmap

This document records the classification-quality wave after v0.1.

It is intentionally separate from PR #7's implementation scope. The v0.1 acceptance contract remains unchanged: unknown apps stay usable in `Unsorted`, and `Games` remains a valid safe fallback.

## Current evidence

Owner testing on the primary Samsung device showed that the integrated app and category shelf work, but the starter taxonomy is still coarse:

- `Unsorted`: approximately 225 launcher entries;
- `Games`: approximately 129 launcher entries;
- some existing categories contain only one or two entries;
- browser-created/PWA-style launcher shortcuts appear among ordinary launcher entries.

The platform acceptance pass separately recorded 566 launcher targets across 564 packages. These counts are product-quality feedback, not a failure of discovery or the v0.1 categorization contract.

## Agent 60 — classification foundation and evidence

Agent 60 owns the shared foundation before classification rules expand in parallel.

The foundation provides:

1. an explicit, user-initiated local classification report containing label, package, exact component/class, app-owned platform category, organizer category and `ClassificationSource`;
2. deterministic aggregate counts by organizer category and classification source;
3. stable rule composition with exact package and exact component selectors;
4. separate ownership seams for general, game and Web/PWA rule packs;
5. regression tests that preserve user override > bundled rule > Android-declared category > `Unsorted`;
6. evidence and migration review before taxonomy values are added, renamed or removed.

The raw owner-device report is private diagnostic data. Do not commit it, attach it to a pull request, or turn it into test fixtures. Commit only generalized decisions, aggregate before/after counts where useful, reusable deterministic rules, tests and sanitized documentation.

### Evidence gate

The v0.1 aggregate observations above are not sufficient to infer a reliable Web/PWA signature or a useful game split. Until the new owner-device report is reviewed:

- keep the current `AppCategory` enum unchanged;
- keep `Games` as the Android-declared game fallback;
- do not add `Web Shortcuts` merely because browser-created entries were observed;
- do not rename/remove tiny categories merely because their current counts are small;
- do not add label/title guessing or another matcher.

This deliberately leaves Agent 60's taxonomy exit gate pending until the owner report is reviewed. Adding a category remains low-risk, but renaming/removing a persisted enum value requires an explicit state migration and migration tests because category overrides store enum names.

## Rule architecture frozen by Agent 60

Known-app rules remain pure Kotlin and Android-framework-free.

Supported deterministic selectors are:

- exact package identity;
- exact launch component identity.

Within the bundled-rule tier, an exact component rule takes precedence over an exact package rule for the same installed target. Globally duplicated exact selectors fail fast when the composed rule set is built. `ClassificationSource.KNOWN_APP_RULE` remains the source for either selector type.

No generic rule DSL exists. A third matcher may be introduced only if the real report demonstrates a repeatable identity signal that cannot be represented safely by exact package/component matching. Labels are not primary rule identity.

## Parallel rule lanes after Agent 60 merges

These lanes start from the merged Agent 60 foundation and must not change shared rule infrastructure independently.

| Lane | Reserved production file | Scope |
|---|---|---|
| General known apps | `rules/general/GeneralKnownAppRules.kt` | Evidence-backed non-game package/component rules. |
| Games | `rules/games/GameKnownAppRules.kt` | Evidence-backed game rules using only the frozen taxonomy. |
| Web/PWA shortcuts | `rules/web/WebShortcutKnownAppRules.kt` | Only deterministic Web/PWA rules supported by reviewed evidence. |

Each lane owns corresponding tests under its matching `app/src/test/**/rules/` package. Shared selector/index/composition files remain foundation-owned and should not be edited by parallel rule lanes unless a real evidence-backed contract change is coordinated first.

## Taxonomy to evaluate from the private report

Do not treat this list as final until the report is reviewed. Candidate broad game buckets are:

- Action & Adventure
- RPG
- Strategy
- Simulation
- Puzzle & Casual
- Sports & Racing
- Board & Card
- Other Games

Prefer fewer categories with good coverage over many one-app categories. `Games` should remain available as the safe fallback for Android-declared games that cannot be classified reliably into a narrower bucket unless a later evidence-backed migration decision explicitly changes that contract.

Evaluate `Web Shortcuts` only if supported launcher metadata provides a deterministic, maintainable signature. Review one- and two-entry categories for overlap, but weigh any consolidation against the migration cost of changing persisted override values.

## Evidence review checklist

Before Agent 60 is merge-ready, review the private report and record only sanitized conclusions:

1. same-device aggregate counts by category and `ClassificationSource`;
2. representative package/component patterns responsible for the largest `Unsorted` and `Games` buckets;
3. whether browser/PWA entries have a stable supported identity signal;
4. the smallest game taxonomy that gives useful coverage without forcing uncertain matches;
5. whether any tiny category should be retained, supplemented, or consolidated;
6. whether the accepted taxonomy is additive only or requires persisted-state migration.

If taxonomy changes are additive only, schema version 1 can remain unchanged because existing enum names still decode. If any existing enum name is renamed or removed, the state schema/migration must be changed in the same PR and existing overrides must be proven to survive.

## Acceptance direction

The classification-quality wave should provide before/after category counts from the same owner device, identify the deterministic rules responsible for meaningful improvements, and retain `Unsorted` as a safe fallback rather than forcing uncertain matches.

A useful result is not "zero Unsorted at any cost". A useful result is substantially better automatic grouping with low false-positive risk, deterministic tests, and easy manual correction for the remainder.
