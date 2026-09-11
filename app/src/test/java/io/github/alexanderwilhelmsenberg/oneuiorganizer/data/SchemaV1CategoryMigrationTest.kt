package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaV1CategoryMigrationTest {
    @Test
    fun `literal schema v1 names map to every stable built in identity`() {
        val migrated = OrganizerStateJsonCodec.decode(schemaV1AllCategories)

        val expected =
            mapOf(
                AppId("legacy.communication") to AppCategory.COMMUNICATION.id,
                AppId("legacy.social") to AppCategory.SOCIAL.id,
                AppId("legacy.work") to AppCategory.WORK.id,
                AppId("legacy.productivity") to AppCategory.PRODUCTIVITY.id,
                AppId("legacy.smart-home") to AppCategory.SMART_HOME.id,
                AppId("legacy.homelab") to AppCategory.HOMELAB.id,
                AppId("legacy.finance") to AppCategory.FINANCE.id,
                AppId("legacy.shopping") to AppCategory.SHOPPING.id,
                AppId("legacy.travel") to AppCategory.TRAVEL_NAVIGATION.id,
                AppId("legacy.audio") to AppCategory.MUSIC_AUDIO.id,
                AppId("legacy.video") to AppCategory.VIDEO.id,
                AppId("legacy.photos") to AppCategory.PHOTOS.id,
                AppId("legacy.reading") to AppCategory.READING.id,
                AppId("legacy.web") to AppCategory.WEB_SHORTCUTS.id,
                AppId("legacy.development") to AppCategory.DEVELOPMENT.id,
                AppId("legacy.tools") to AppCategory.TOOLS.id,
                AppId("legacy.emulators") to AppCategory.EMULATORS.id,
                AppId("legacy.action") to AppCategory.GAME_ACTION_ADVENTURE.id,
                AppId("legacy.rpg") to AppCategory.GAME_RPG.id,
                AppId("legacy.strategy") to AppCategory.GAME_STRATEGY_SIMULATION.id,
                AppId("legacy.puzzle") to AppCategory.GAME_PUZZLE_CASUAL.id,
                AppId("legacy.board") to AppCategory.GAME_BOARD_CARD.id,
                AppId("legacy.games") to AppCategory.GAMES.id,
                AppId("legacy.other") to AppCategory.OTHER.id,
                AppId("legacy.unsorted") to AppCategory.UNSORTED.id
            )

        assertEquals(expected, migrated.categoryOverrides)
    }

    private companion object {
        val schemaV1AllCategories =
            """
            {
              "schemaVersion": 1,
              "categoryOverrides": {
                "legacy.communication": "COMMUNICATION",
                "legacy.social": "SOCIAL",
                "legacy.work": "WORK",
                "legacy.productivity": "PRODUCTIVITY",
                "legacy.smart-home": "SMART_HOME",
                "legacy.homelab": "HOMELAB",
                "legacy.finance": "FINANCE",
                "legacy.shopping": "SHOPPING",
                "legacy.travel": "TRAVEL_NAVIGATION",
                "legacy.audio": "MUSIC_AUDIO",
                "legacy.video": "VIDEO",
                "legacy.photos": "PHOTOS",
                "legacy.reading": "READING",
                "legacy.web": "WEB_SHORTCUTS",
                "legacy.development": "DEVELOPMENT",
                "legacy.tools": "TOOLS",
                "legacy.emulators": "EMULATORS",
                "legacy.action": "GAME_ACTION_ADVENTURE",
                "legacy.rpg": "GAME_RPG",
                "legacy.strategy": "GAME_STRATEGY_SIMULATION",
                "legacy.puzzle": "GAME_PUZZLE_CASUAL",
                "legacy.board": "GAME_BOARD_CARD",
                "legacy.games": "GAMES",
                "legacy.other": "OTHER",
                "legacy.unsorted": "UNSORTED"
              },
              "favouriteAppIds": [],
              "hiddenAppIds": []
            }
            """.trimIndent()
    }
}
