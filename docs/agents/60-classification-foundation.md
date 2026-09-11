# Agent 60 — Classification Foundation / Evidence

## Role

Own the smallest shared post-v0.1 classification foundation before rule expansion starts in parallel.

Use branch:

`feature/classification-foundation`

Do not merge the PR.

## Mandatory reading

Read root `AGENTS.md` and current:

- `docs/STABLE_BASELINE.md`
- `docs/ENGINEERING_BASELINE.md`
- `docs/TECH_STACK.md`
- `docs/PLAN.md`
- `docs/MOSCOW.md`
- `docs/ACCEPTANCE_CRITERIA.md`
- `docs/PARALLEL_DEVELOPMENT.md`
- `docs/CLASSIFICATION_ROADMAP.md`
- `docs/PLATFORM_APPS_SPIKE.md`

Repository state is authoritative.

## Foundation responsibilities

1. Provide an explicit, local-only classification report for every current launcher target with label, package, exact class/component, app-owned platform category, resulting organizer category and `ClassificationSource`.
2. Include deterministic aggregate counts by organizer category and classification source.
3. Preserve precedence exactly: user override > bundled deterministic rule > Android-declared category > `Unsorted`.
4. Keep known-rule selectors pure Kotlin, app-owned and Android-framework-free. The evidence-reviewed contract is exact component > exact package > non-overlapping package prefix within the bundled-rule tier.
5. Keep separate general/game/Web-PWA ownership seams. Do not add a rule DSL or label guessing.
6. Treat the raw owner-device report as private diagnostic data. Commit only aggregate evidence, generalized decisions, reusable rules/tests and sanitized documentation.
7. Treat category rename/removal as persisted-state migration work because overrides store enum names.
8. Run the full permanent quality lane and leave the PR unmerged.

## Reviewed evidence and frozen decisions

The private primary-Samsung report contained 566 launcher targets: 225 `Unsorted`, 129 `Games`, 336 Android-declared classifications, 5 installed known-rule matches and no user overrides in that snapshot.

Agent 60 freezes these additive categories:

- `WEB_SHORTCUTS`;
- `GAME_ACTION_ADVENTURE`;
- `GAME_RPG`;
- `GAME_STRATEGY_SIMULATION`;
- `GAME_PUZZLE_CASUAL`;
- `GAME_BOARD_CARD`.

`GAMES` remains the safe game fallback. `Sports & Racing` is deferred for lack of enough owner-device density, and `Other Games` is unnecessary because `Games` already provides that fallback.

The report established generated Chromium WebAPK packages under `org.chromium.webapk.` as a repeatable non-label identity signal. Package-prefix matching is therefore accepted as the one additional matcher seam. Prefixes must end at a package-segment boundary, duplicate/overlapping prefixes fail fast, and arbitrary TWA/activity-name inference remains out of scope.

Existing low-count category enum values remain unchanged. The report indicates that several are under-populated because rules are sparse rather than because the taxonomy is invalid. The taxonomy change is additive only, so persisted organizer state remains schema version 1 and no migration is required.

## Scope exclusions

Do not perform the large general rule expansion, classify the game inventory one-by-one, implement the final Web/PWA rule pack, redesign the shelf, add dependencies, or change toolchain versions.

Do not add `INTERNET`, `QUERY_ALL_PACKAGES`, telemetry, analytics, cloud classification, background collection, label/title guessing, or speculative matchers.

See `docs/CLASSIFICATION_ROADMAP.md` and `docs/PARALLEL_DEVELOPMENT.md` for the frozen post-Agent-60 ownership model.
