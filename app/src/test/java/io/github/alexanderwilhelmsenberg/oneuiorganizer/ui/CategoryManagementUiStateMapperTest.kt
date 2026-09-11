package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiStateMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryManagementUiStateMapperTest {
    @Test
    fun `management categories follow persisted normalized order`() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val state =
            OrganizerState(
                customCategories = listOf(custom),
                categoryOrder =
                    listOf(
                        custom.id,
                        AppCategory.WORK.id,
                        AppCategory.TOOLS.id
                    )
            )

        val result =
            CategoryManagementUiStateMapper.map(
                apps = emptyList(),
                organizerState = state,
                operationError = null
            )

        assertEquals(custom.id, result.categories.first().category.id)
        assertEquals(AppCategory.WORK.id, result.categories[1].category.id)
        assertEquals(AppCategory.TOOLS.id, result.categories[2].category.id)
        assertEquals(state.orderedCategories().map { it.id }, result.categories.map { it.category.id })
    }

    @Test
    fun `assigned counts use effective categories and retain uninstalled overrides`() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val automaticApp = AppId("example.automatic")
        val hiddenApp = AppId("example.hidden")
        val retainedApp = AppId("example.retained")
        val state =
            OrganizerState(
                categoryOverrides =
                    mapOf(
                        hiddenApp to custom.id,
                        retainedApp to custom.id
                    ),
                hiddenAppIds = setOf(hiddenApp),
                customCategories = listOf(custom)
            )
        val apps =
            listOf(
                categorizedApp(
                    appId = automaticApp,
                    category = AppCategory.VIDEO,
                    source = ClassificationSource.KNOWN_APP_RULE
                ),
                categorizedApp(
                    appId = hiddenApp,
                    category = custom,
                    source = ClassificationSource.USER_OVERRIDE
                )
            )

        val result =
            CategoryManagementUiStateMapper.map(
                apps = apps,
                organizerState = state,
                operationError = null
            )
        val videoItem = result.categories.single { item -> item.category.id == AppCategory.VIDEO.id }
        val customItem = result.categories.single { item -> item.category.id == custom.id }

        assertEquals(1, videoItem.assignedAppCount)
        assertEquals(2, customItem.assignedAppCount)
    }

    @Test
    fun `operation error is presented without changing category state`() {
        val state = OrganizerState()

        val result =
            CategoryManagementUiStateMapper.map(
                apps = emptyList(),
                organizerState = state,
                operationError = "Could not save the category change. Try again."
            )

        assertEquals("Could not save the category change. Try again.", result.operationError)
        assertTrue(result.categories.isNotEmpty())
        assertEquals(state.orderedCategories().map { it.id }, result.categories.map { it.category.id })
    }

    private fun categorizedApp(
        appId: AppId,
        category: CategoryDefinition,
        source: ClassificationSource
    ): CategorizedApp =
        CategorizedApp(
            app =
                InstalledApp(
                    id = appId,
                    launchTargetId = LaunchTargetId(appId.packageName, "MainActivity"),
                    label = appId.packageName
                ),
            category = category,
            source = source
        )
}
