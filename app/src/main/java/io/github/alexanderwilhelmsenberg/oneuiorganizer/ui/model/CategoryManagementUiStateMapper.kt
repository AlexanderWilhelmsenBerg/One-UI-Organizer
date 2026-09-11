package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState

object CategoryManagementUiStateMapper {
    fun map(
        apps: List<CategorizedApp>,
        organizerState: OrganizerState,
        operationError: String?
    ): CategoryManagementUiState {
        val currentAppIds = apps.mapTo(mutableSetOf()) { categorizedApp -> categorizedApp.app.id }
        val assignedCounts = apps.groupingBy { categorizedApp -> categorizedApp.category.id }.eachCount().toMutableMap()

        organizerState.categoryOverrides.forEach { (appId, categoryId) ->
            if (appId !in currentAppIds) {
                assignedCounts[categoryId] = (assignedCounts[categoryId] ?: 0) + 1
            }
        }

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
