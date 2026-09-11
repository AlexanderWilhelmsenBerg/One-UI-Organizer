package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState

object CategoryManagementUiStateMapper {
    fun map(
        categoryAssignmentCounts: Map<CategoryId, Int>,
        organizerState: OrganizerState,
        operationError: String?
    ): CategoryManagementUiState =
        CategoryManagementUiState(
            categories =
                organizerState.orderedCategories().map { category ->
                    CategoryManagementItemUiModel(
                        category = category,
                        assignedAppCount = categoryAssignmentCounts[category.id] ?: 0
                    )
                },
            operationError = operationError
        )
}
