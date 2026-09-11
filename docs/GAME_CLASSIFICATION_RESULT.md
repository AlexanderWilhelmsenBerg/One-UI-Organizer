# Game Classification Result

This note records the sanitized aggregate result of the Agent 62 game-rule lane plus the Agent 70 integration correction discovered from the fresh Samsung acceptance report.

The private owner-device classification report remains diagnostic input only. Raw launcher rows are not committed.

## Rule selection

The final game pack contains 105 deterministic exact-package rules. It uses only the five game categories frozen by Agent 60:

- `GAME_ACTION_ADVENTURE`
- `GAME_RPG`
- `GAME_STRATEGY_SIMULATION`
- `GAME_PUZZLE_CASUAL`
- `GAME_BOARD_CARD`

`GAMES` remains the safe fallback.

Rules are retained only where the game identity and broad genre are high-confidence. Mixed or unclear entries remain in `GAMES` rather than being forced into a narrower category. Launcher entries that are primarily emulators, game frontends, streaming clients or compatibility tools also remain in `GAMES` because the frozen taxonomy describes game genres, not gaming software roles.

## Agent 70 false-positive correction

The fresh Samsung acceptance report exposed one false positive that aggregate-only review could not reveal. A launcher entry labeled `Eden Optimized` used package identity `com.miHoYo.Yuanshen` but launched through an `org.yuzu.yuzu_emu` activity. The exact-package rule therefore classified an emulator variant as RPG.

Agent 70 removed `com.miHoYo.Yuanshen` from the narrow RPG rule pack and added a regression proving that this owner-device evidence stays in broad `Games` through the Android-declared `GAME` fallback.

The correction intentionally does not attempt to distinguish an official game installation from emulator variants sharing that package identity. The low-false-positive policy prefers the broad fallback when package identity is demonstrably ambiguous in the accepted device evidence.

## Final same-device game projection

After the Agent 70 correction, the expected game distribution on the same 129-entry game population is:

| Game category | Before | Final expected |
| --- | ---: | ---: |
| Action & Adventure | 0 | 12 |
| RPG | 0 | 37 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 24 |
| **Total** | **129** | **129** |

The narrow rules therefore move 105 of the 129 broad `Games` entries while deliberately retaining 24 safe fallback entries.

## Classification-source impact

Applying only the final game-rule lane against the original frozen report:

| Classification source | Before | Final expected |
| --- | ---: | ---: |
| User override | 0 | 0 |
| Bundled known-app rule | 5 | 110 |
| Android-declared category | 336 | 231 |
| Unsorted fallback | 225 | 225 |

The 105 narrowed game entries move from Android-declared category to bundled known-app rule. No `Unsorted` entry is affected by this lane.

## Scope confirmation

This lane does not change:

- `AppCategory` or taxonomy;
- selector/index/composition infrastructure;
- Android discovery;
- persistence schema or migration behavior;
- UI;
- dependencies, SDKs or toolchains.
