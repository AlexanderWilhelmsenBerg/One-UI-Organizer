package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
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
                categoryAssignmentCounts = emptyMap(),
                organizerState = state,
                operationError = null
            )

        assertEquals(custom.id, result.categories.first().category.id)
        assertEquals(AppCategory.WORK.id, result.categories[1].category.id)
        assertEquals(AppCategory.TOOLS.id, result.categories[2].category.id)
        assertEquals(state.orderedCategories().map { it.id }, result.categories.map { it.category.id })
    }

    @Test
    fun `management rows consume shared effective assignment counts`() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val state = OrganizerState(customCategories = listOf(custom))

        val result =
            CategoryManagementUiStateMapper.map(
                categoryAssignmentCounts =
                    mapOf(
                        AppCategory.VIDEO.id to 7,
                        custom.id to 2
                    ),
                organizerState = state,
                operationError = null
            )
        val videoItem = result.categories.single { item -> item.category.id == AppCategory.VIDEO.id }
        val customItem = result.categories.single { item -> item.category.id == custom.id }

        assertEquals(7, videoItem.assignedAppCount)
        assertEquals(2, customItem.assignedAppCount)
    }

    @Test
    fun `operation error is presented without changing category state`() {
        val state = OrganizerState()

        val result =
            CategoryManagementUiStateMapper.map(
                categoryAssignmentCounts = emptyMap(),
                organizerState = state,
                operationError = "Could not save the category change. Try again."
            )

        assertEquals("Could not save the category change. Try again.", result.operationError)
        assertTrue(result.categories.isNotEmpty())
        assertEquals(state.orderedCategories().map { it.id }, result.categories.map { it.category.id })
    }
}
