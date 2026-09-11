package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState

object CategoryManagementUiStateMapper {
    fun map(
        organizerState: OrganizerState,
        operationError: String?
    ): CategoryManagementUiState {
        val assignedCounts = organizerState.categoryOverrides.values.groupingBy { categoryId -> categoryId }.eachCount()

        return CategoryManagementUiState(
            categories =
                organizerState.orderedCategories().map { category ->
                    CategoryManagementItemUiModel(
                        category = category,
                        assignedAppCount = assignedCounts[category.id] ?: 0
                    )
                },
            operationError = operationError
        )
    }
}
