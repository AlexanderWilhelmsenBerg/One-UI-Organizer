# Classification Quality Roadmap

This document records the next classification-quality slice after the v0.1 integration PR.

It is intentionally **not part of PR #7's implementation scope**. The current v0.1 acceptance contract remains unchanged: unknown apps may remain usable in `Unsorted`, and `Games` is a valid broad category.

## Why this is next

Owner testing on the primary Samsung device showed that the integrated app and category shelf work, but the starter taxonomy is still coarse:

- `Unsorted`: 225 launcher entries;
- `Games`: 129 launcher entries;
- some existing categories contain only one or two entries;
- browser-created/PWA-style launcher shortcuts appear among ordinary launcher entries.

These counts are product-quality feedback, not a failure of the current v0.1 discovery/categorization contract.

## Next-PR scope

The next classification PR should:

1. collect enough local diagnostic information to tune rules from real launcher data, preferably label, package, exact launch component, Android-declared category, current category and classification source;
2. keep any diagnostic/export mechanism local-only, explicit, and temporary or clearly scoped—no telemetry, analytics, network permission, or background collection;
3. expand bundled known-app rules only from evidence, with deterministic package/component matching and pure Kotlin regression tests;
4. add a `Web Shortcuts` category only if browser-created/PWA launcher entries expose a deterministic and maintainable signature through supported Android-visible metadata;
5. split the broad `Games` bucket into a small number of useful, stable game categories only where deterministic classification is practical;
6. review categories with only one or two apps and consolidate overlapping taxonomy where doing so makes browsing clearer;
7. preserve the classification precedence contract: user override > bundled rule > Android-declared category > `Unsorted`;
8. preserve existing user overrides as authoritative when automatic rules or taxonomy improve;
9. avoid title/label heuristics that are likely to create false positives unless a narrowly justified, tested rule proves reliable;
10. document any persisted-state migration required by category renames/removals before changing existing serialized category values.

## Provisional game taxonomy to evaluate

Do not treat this list as final until the real device report is reviewed. Candidate broad buckets are:

- Action & Adventure
- RPG
- Strategy
- Simulation
- Puzzle & Casual
- Sports & Racing
- Board & Card
- Other Games

Prefer fewer categories with good coverage over many one-app categories.

## Acceptance direction for the next PR

The next PR should provide before/after category counts from the same owner device, identify which rules caused the largest improvements, and keep `Unsorted` as a safe fallback rather than forcing uncertain matches.

A useful result is not "zero Unsorted at any cost". A useful result is substantially better automatic grouping with low false-positive risk, deterministic tests, and easy manual correction for the remainder.
