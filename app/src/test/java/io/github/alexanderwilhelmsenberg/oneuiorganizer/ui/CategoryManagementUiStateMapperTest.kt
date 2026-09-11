package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
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
                organizerState = state,
                operationError = null
            )

        assertEquals(custom.id, result.categories.first().category.id)
        assertEquals(AppCategory.WORK.id, result.categories[1].category.id)
        assertEquals(AppCategory.TOOLS.id, result.categories[2].category.id)
        assertEquals(state.orderedCategories().map { it.id }, result.categories.map { it.category.id })
    }

    @Test
    fun `assigned counts include retained overrides for hidden and currently uninstalled apps`() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
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

        val result =
            CategoryManagementUiStateMapper.map(
                organizerState = state,
                operationError = null
            )
        val customItem = result.categories.single { item -> item.category.id == custom.id }

        assertEquals(2, customItem.assignedAppCount)
    }

    @Test
    fun `operation error is presented without changing category state`() {
        val state = OrganizerState()

        val result =
            CategoryManagementUiStateMapper.map(
                organizerState = state,
                operationError = "Could not save the category change. Try again."
            )

        assertEquals("Could not save the category change. Try again.", result.operationError)
        assertTrue(result.categories.isNotEmpty())
        assertEquals(state.orderedCategories().map { it.id }, result.categories.map { it.category.id })
    }
}
