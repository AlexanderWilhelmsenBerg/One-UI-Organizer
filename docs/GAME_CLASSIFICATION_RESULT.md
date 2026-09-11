# Game Classification Result

This note records the sanitized aggregate result of the Agent 62 game-genre lane plus the Agent 70 false-positive correction. The later Agent 70 emulator pack is documented separately in the integrated acceptance record.

The private owner-device classification report remains diagnostic input only. Raw launcher rows are not committed.

## Rule selection

The final game-genre pack contains 105 deterministic exact-package rules. It uses only the five genre categories frozen by Agent 60:

- `GAME_ACTION_ADVENTURE`
- `GAME_RPG`
- `GAME_STRATEGY_SIMULATION`
- `GAME_PUZZLE_CASUAL`
- `GAME_BOARD_CARD`

`GAMES` remains the safe broad fallback for this game-genre lane.

Rules are retained only where the game identity and broad genre are high-confidence. Mixed or unclear games stay in `GAMES` rather than being forced into a narrower genre.

## Agent 70 false-positive correction

The first fresh Samsung acceptance report exposed one false positive that aggregate-only review could not reveal. A launcher entry labeled `Eden Optimized` used package identity `com.miHoYo.Yuanshen` but launched through an `org.yuzu.yuzu_emu` activity. The original exact-package game rule therefore classified an emulator variant as RPG.

Agent 70 removed `com.miHoYo.Yuanshen` from the narrow RPG pack and added regression coverage.

The owner then approved a first-class `Emulators` category. Emulator classification now lives in a separate rule pack, including an exact-component selector for the observed Eden/Yuzu launcher. This keeps emulator/software-role classification separate from the game-genre pack and avoids package-only ambiguity.

## Corrected game-lane-only projection

Before the separate emulator pack is applied, the corrected game-genre lane produces:

| Game category | Before | Corrected game lane |
| --- | ---: | ---: |
| Action & Adventure | 0 | 12 |
| RPG | 0 | 37 |
| Strategy & Simulation | 0 | 29 |
| Puzzle & Casual | 0 | 23 |
| Board & Card | 0 | 4 |
| Games fallback | 129 | 24 |
| **Total** | **129** | **129** |

The 105 genre rules move 105 of the original 129 broad `Games` entries to a specific genre.

The integrated emulator pack subsequently moves evidence-backed emulator software out of broad `Games` into `Emulators`; therefore this table is intentionally a game-lane-only result, not the final integrated category distribution.

## Classification-source impact

Applying only the corrected game-genre lane against the original frozen report:

| Classification source | Before | Corrected game lane |
| --- | ---: | ---: |
| User override | 0 | 0 |
| Bundled known-app rule | 5 | 110 |
| Android-declared category | 336 | 231 |
| Unsorted fallback | 225 | 225 |

The integrated emulator pack has its own additional source-count effect documented in `CLASSIFICATION_INTEGRATION_ACCEPTANCE.md`.

## Scope confirmation

The game-genre lane itself does not own emulator taxonomy or emulator selectors. Agent 70's owner-approved emulator integration is a separate bundled rule pack composed through the same `KnownAppRuleSet`.
