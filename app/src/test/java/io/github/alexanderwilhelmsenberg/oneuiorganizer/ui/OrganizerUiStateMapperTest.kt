package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerUiStateMapper
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfErrorUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrganizerUiStateMapperTest {
    @Test
    fun `hidden apps stay out of ordinary sections and remain restorable`() {
        val visible = categorizedApp("example.visible", "Visible", AppCategory.TOOLS)
        val hidden = categorizedApp("example.hidden", "Hidden", AppCategory.GAMES)
        val state = OrganizerState(hiddenAppIds = setOf(hidden.app.id))

        val result = OrganizerUiStateMapper.map(
            apps = listOf(hidden, visible),
            organizerState = state,
            query = "",
            isLoading = false,
            error = null
        )

        assertEquals(listOf("Hidden"), result.hiddenApps.map { it.label })
        assertEquals(listOf("Visible"), result.categories.flatMap { it.apps }.map { it.label })
        assertFalse(result.categories.flatMap { it.apps }.any { it.label == "Hidden" })
    }

    @Test
    fun `category search feeds the real shelf`() {
        val apps = listOf(
            categorizedApp("example.maps", "Kart", AppCategory.TRAVEL_NAVIGATION),
            categorizedApp("example.music", "Musikk", AppCategory.MUSIC_AUDIO)
        )

        val result = OrganizerUiStateMapper.map(
            apps = apps,
            organizerState = OrganizerState(),
            query = "travel",
            isLoading = false,
            error = null
        )

        assertEquals(listOf("Kart"), result.categories.flatMap { it.apps }.map { it.label })
    }

    @Test
    fun `package scoped favourite applies to every exact launcher target`() {
        val packageName = "example.multi"
        val first = categorizedApp(packageName, "First", AppCategory.TOOLS, "$packageName.First")
        val second = categorizedApp(packageName, "Second", AppCategory.TOOLS, "$packageName.Second")

        val result = OrganizerUiStateMapper.map(
            apps = listOf(first, second),
            organizerState = OrganizerState(favouriteAppIds = setOf(AppId(packageName))),
            query = "",
            isLoading = false,
            error = null
        )

        assertEquals(2, result.favourites.size)
        assertTrue(result.favourites.all { it.isFavourite })
        assertEquals(
            setOf("$packageName.First", "$packageName.Second"),
            result.favourites.map { it.launchTargetId.className }.toSet()
        )
    }

    @Test
    fun `scan failure is preserved for an empty shelf`() {
        val result = OrganizerUiStateMapper.map(
            apps = emptyList(),
            organizerState = OrganizerState(),
            query = "",
            isLoading = false,
            error = ShelfErrorUiModel.SCAN_FAILED
        )

        assertEquals(ShelfErrorUiModel.SCAN_FAILED, result.error)
        assertFalse(result.hasAnyCurrentApps)
    }

    private fun categorizedApp(
        packageName: String,
        label: String,
        category: AppCategory,
        className: String = "$packageName.MainActivity"
    ): CategorizedApp = CategorizedApp(
        app =
            InstalledApp(
                id = AppId(packageName),
                launchTargetId = LaunchTargetId(packageName, className),
                label = label
            ),
        category = category,
        source = ClassificationSource.UNSORTED_FALLBACK
    )
}
