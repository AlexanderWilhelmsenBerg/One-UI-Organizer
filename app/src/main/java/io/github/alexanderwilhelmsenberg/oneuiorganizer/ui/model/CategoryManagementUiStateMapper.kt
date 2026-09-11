package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState

object CategoryManagementUiStateMapper {
    fun map(
        organizerState: OrganizerState,
        validationError: CategoryManagementErrorUiModel?,
        operationError: CategoryManagementErrorUiModel?
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
            validationError = validationError,
            operationError = operationError
        )
    }
}
