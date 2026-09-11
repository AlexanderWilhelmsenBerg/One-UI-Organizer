package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategorySectionUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerShelfUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfAppUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfContentMode
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfErrorUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.displayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrganizerUiStateTest {
    @Test
    fun `loading state takes precedence over visible content`() {
        val state =
            OrganizerShelfUiState(
                isLoading = true,
                categories = listOf(CategorySectionUiModel(AppCategory.TOOLS, listOf(app())))
            )

        assertEquals(ShelfContentMode.LOADING, state.contentMode)
        assertTrue(state.hasVisibleApps)
    }

    @Test
    fun `non-empty query without visible apps is no results`() {
        val state = OrganizerShelfUiState(query = "home")

        assertEquals(ShelfContentMode.NO_RESULTS, state.contentMode)
        assertFalse(state.hasVisibleApps)
    }

    @Test
    fun `empty query without visible apps is empty shelf`() {
        val state = OrganizerShelfUiState()

        assertEquals(ShelfContentMode.EMPTY, state.contentMode)
    }

    @Test
    fun `empty shelf with error reports error mode`() {
        val state = OrganizerShelfUiState(error = ShelfErrorUiModel.SCAN_FAILED)

        assertEquals(ShelfContentMode.ERROR, state.contentMode)
    }

    @Test
    fun `stable key preserves exact launcher component identity`() {
        val app =
            app(
                launchTargetId = LaunchTargetId("com.example", "com.example.SecondLauncher")
            )

        assertEquals("com.example/com.example.SecondLauncher", app.stableKey)
    }

    @Test
    fun `category labels are presentation only and human readable`() {
        assertEquals("Smart Home", AppCategory.SMART_HOME.displayName())
        assertEquals("Travel & Navigation", AppCategory.TRAVEL_NAVIGATION.displayName())
        assertEquals("Unsorted", AppCategory.UNSORTED.displayName())
    }

    private fun app(launchTargetId: LaunchTargetId = LaunchTargetId("com.example", "com.example.Main")) =
        ShelfAppUiModel(
            launchTargetId = launchTargetId,
            label = "Example",
            category = AppCategory.TOOLS
        )
}
