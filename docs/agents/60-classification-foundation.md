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
4. Evolve known rules only enough to support exact package and exact component selectors and separate general/game/Web-PWA ownership seams.
5. Keep the rule system pure Kotlin, app-owned, Android-framework-free, deterministic and small. Do not add a rule DSL or label guessing.
6. Review the private owner-device report before freezing new taxonomy values. Raw report rows must never be committed.
7. Treat category rename/removal as persisted-state migration work because overrides store enum names.
8. Run the full permanent quality lane and leave the PR unmerged.

## Scope exclusions

Do not perform the large general rule expansion, classify the game inventory one-by-one, implement the final Web/PWA rule pack, redesign the shelf, add dependencies, or change toolchain versions.

Do not add `INTERNET`, `QUERY_ALL_PACKAGES`, telemetry, analytics, cloud classification, background collection, or speculative matchers.

## Evidence gate

If the agent cannot access the physical Samsung device, implement the report and provide the owner an exact one-pass collection procedure. The PR may be code-review-ready, but it is not merge-ready until the private report is reviewed and taxonomy/migration decisions are recorded in sanitized documentation.

See `docs/CLASSIFICATION_ROADMAP.md` and `docs/PARALLEL_DEVELOPMENT.md` for the frozen post-Agent-60 ownership model.
