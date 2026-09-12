package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerUiStateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryShortcutDestinationTest {
    @Test
    fun validCustomDestinationFocusesOnlyThatStableCategory() {
        val family = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val state =
            OrganizerState(
                customCategories = listOf(family),
                categoryOrder = listOf(family.id, AppCategory.WORK.id)
            )
        val apps =
            listOf(
                categorizedApp("family.app", family),
                categorizedApp("work.app", AppCategory.WORK)
            )

        val result =
            OrganizerUiStateMapper.map(
                apps = apps,
                organizerState = state,
                query = "",
                isLoading = false,
                error = null,
                focusedCategoryId = family.id
            )

        assertEquals(family, result.focusedCategory)
        assertFalse(result.categoryDestinationUnavailable)
        assertEquals(listOf(family.id), result.categories.map { section -> section.category.id })
        assertEquals(listOf("family.app"), result.categories.single().apps.map { app -> app.launchTargetId.packageName })
        assertTrue(result.favourites.isEmpty())
    }

    @Test
    fun builtInDestinationUsesTheSameFocusContract() {
        val result =
            OrganizerUiStateMapper.map(
                apps = listOf(categorizedApp("work.app", AppCategory.WORK)),
                organizerState = OrganizerState(),
                query = "",
                isLoading = false,
                error = null,
                focusedCategoryId = AppCategory.WORK.id
            )

        assertEquals(AppCategory.WORK, result.focusedCategory)
        assertEquals(AppCategory.WORK.id, result.categories.single().category.id)
    }

    @Test
    fun staleDeletedDestinationFallsBackToNormalShelfSafely() {
        val staleId = CategoryId.custom("deleted")
        val result =
            OrganizerUiStateMapper.map(
                apps = listOf(categorizedApp("work.app", AppCategory.WORK)),
                organizerState = OrganizerState(),
                query = "",
                isLoading = false,
                error = null,
                focusedCategoryId = staleId
            )

        assertNull(result.focusedCategory)
        assertTrue(result.categoryDestinationUnavailable)
        assertEquals(listOf(AppCategory.WORK.id), result.categories.map { section -> section.category.id })
    }

    @Test
    fun dynamicEligibilityCountsOnlyCurrentAppsNotRetainedUninstalledOverrides() {
        val family = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val missingApp = AppId("missing.app")
        val state =
            OrganizerState(
                categoryOverrides = mapOf(missingApp to family.id),
                customCategories = listOf(family)
            )

        val result =
            OrganizerUiStateMapper.map(
                apps = listOf(categorizedApp("work.app", AppCategory.WORK)),
                organizerState = state,
                query = "",
                isLoading = false,
                error = null
            )

        assertEquals(1, result.categoryAssignmentCounts[family.id])
        assertFalse(family.id in result.currentCategoryAppCounts)
        assertEquals(1, result.currentCategoryAppCounts[AppCategory.WORK.id])
    }

    private fun categorizedApp(
        packageName: String,
        category: io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
    ): CategorizedApp =
        CategorizedApp(
            app =
                InstalledApp(
                    id = AppId(packageName),
                    launchTargetId = LaunchTargetId(packageName, "$packageName.MainActivity"),
                    label = packageName
                ),
            category = category,
            source = ClassificationSource.USER_OVERRIDE
        )
}
