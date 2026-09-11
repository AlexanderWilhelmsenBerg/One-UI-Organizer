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
}
