package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.searchLabel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.displayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryLabelTest {
    @Test
    fun everyCategoryHasMatchingNonBlankSearchAndDisplayLabels() {
        AppCategory.entries.forEach { category ->
            val searchLabel = category.searchLabel()
            val displayLabel = category.displayName()

            assertTrue(searchLabel.isNotBlank(), "Missing search label for ${category.name}")
            assertTrue(displayLabel.isNotBlank(), "Missing display label for ${category.name}")
            assertEquals(displayLabel, searchLabel, "Search/display label drift for ${category.name}")
        }
    }

    @Test
    fun postV01ClassificationTaxonomyUsesFrozenAdditiveCategories() {
        assertEquals("Web Shortcuts", AppCategory.WEB_SHORTCUTS.displayName())
        assertEquals("Action & Adventure", AppCategory.GAME_ACTION_ADVENTURE.displayName())
        assertEquals("RPG", AppCategory.GAME_RPG.displayName())
        assertEquals("Strategy & Simulation", AppCategory.GAME_STRATEGY_SIMULATION.displayName())
        assertEquals("Puzzle & Casual", AppCategory.GAME_PUZZLE_CASUAL.displayName())
        assertEquals("Board & Card", AppCategory.GAME_BOARD_CARD.displayName())
        assertEquals("Games", AppCategory.GAMES.displayName())
    }
}
