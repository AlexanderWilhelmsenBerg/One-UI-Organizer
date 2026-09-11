# Game Classification Result

This note records the sanitized aggregate result of the Agent 62 game-rule lane.

The private owner-device classification report remains diagnostic input only. Raw launcher rows are not committed.
This result is an Agent 62-only projection against the frozen Agent 60 evidence; sibling general and Web/PWA rule
packs are not included in these counts.

## Rule selection

The game pack adds 106 deterministic exact-package rules. It uses only the five game categories frozen by Agent 60:

- `GAME_ACTION_ADVENTURE`
- `GAME_RPG`
- `GAME_STRATEGY_SIMULATION`
- `GAME_PUZZLE_CASUAL`
- `GAME_BOARD_CARD`

`GAMES` remains the safe fallback.

Rules were added only where the game identity and broad genre were high-confidence. Mixed or unclear entries remain in
`GAMES` rather than being forced into a narrower category. Launcher entries that are primarily emulators, game
frontends, streaming clients, or compatibility tools also remain in `GAMES` because the frozen taxonomy describes game
genres, not gaming software roles.

Public publisher/store metadata was used during development to resolve unclear genre cases. The app performs no runtime
network access and no metadata lookup.

## Same-device before/after game counts

| Game category | Before | After |
| --- | ---: | ---: |
| Action & Adventure | 0 | 12 |
| RPG | 0 | 38 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 23 |
| **Total** | **129** | **129** |

The narrower rules move 106 of the 129 previously broad `Games` entries while deliberately retaining 23 safe fallback
entries.

## Classification-source impact

Against the same frozen report, and applying only this game-rule lane:

| Classification source | Before | After |
| --- | ---: | ---: |
| User override | 0 | 0 |
| Bundled known-app rule | 5 | 111 |
| Android-declared category | 336 | 230 |
| Unsorted fallback | 225 | 225 |

The 106 moved game entries change source from Android-declared category to bundled known-app rule. No `Unsorted` entry
is affected by this lane.

## Scope confirmation

This lane does not change:

- `AppCategory` or taxonomy;
- selector/index/composition infrastructure;
- Android discovery;
- persistence schema or migration behavior;
- UI;
- dependencies, SDKs, or toolchains.
