package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.LocalAppSearch
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState

object OrganizerUiStateMapper {
    fun map(
        apps: List<CategorizedApp>,
        organizerState: OrganizerState,
        query: String,
        isLoading: Boolean,
        error: ShelfErrorUiModel?,
        focusedCategoryId: CategoryId? = null
    ): OrganizerShelfUiState {
        val currentAppIds = apps.mapTo(mutableSetOf()) { categorizedApp -> categorizedApp.app.id }
        val currentCategoryAssignmentCounts =
            apps.groupingBy { categorizedApp -> categorizedApp.category.id }.eachCount()
        val categoryAssignmentCounts = currentCategoryAssignmentCounts.toMutableMap()
        organizerState.categoryOverrides.forEach { (appId, categoryId) ->
            if (appId !in currentAppIds) {
                categoryAssignmentCounts[categoryId] = (categoryAssignmentCounts[categoryId] ?: 0) + 1
            }
        }

        val hiddenApps =
            LocalAppSearch
                .filter(
                    apps = apps.filter { categorizedApp -> categorizedApp.app.id in organizerState.hiddenAppIds },
                    query = ""
                ).map { categorizedApp -> categorizedApp.toUiModel(organizerState) }

        val orderedCategories = organizerState.orderedCategories()
        val focusedCategory = focusedCategoryId?.let(organizerState::categoryDefinition)
        val visibleApps =
            apps.filterNot { categorizedApp -> categorizedApp.app.id in organizerState.hiddenAppIds }
        val currentCategoryAppCounts =
            visibleApps.groupingBy { categorizedApp -> categorizedApp.category.id }.eachCount()
        val categoryScopedApps =
            if (focusedCategory == null) {
                visibleApps
            } else {
                visibleApps.filter { categorizedApp -> categorizedApp.category.id == focusedCategory.id }
            }
        val filteredApps = LocalAppSearch.filter(categoryScopedApps, query)

        val favourites =
            if (focusedCategory == null) {
                filteredApps
                    .filter { categorizedApp -> categorizedApp.app.id in organizerState.favouriteAppIds }
                    .map { categorizedApp -> categorizedApp.toUiModel(organizerState) }
            } else {
                emptyList()
            }

        val categories =
            if (focusedCategory != null) {
                listOf(
                    CategorySectionUiModel(
                        category = focusedCategory,
                        apps = filteredApps.map { categorizedApp -> categorizedApp.toUiModel(organizerState) }
                    )
                )
            } else {
                orderedCategories.mapNotNull { category ->
                    val categoryApps =
                        filteredApps
                            .filter { categorizedApp -> categorizedApp.category.id == category.id }
                            .map { categorizedApp -> categorizedApp.toUiModel(organizerState) }
                    categoryApps
                        .takeIf(List<ShelfAppUiModel>::isNotEmpty)
                        ?.let { appsInCategory ->
                            CategorySectionUiModel(
                                category = category,
                                apps = appsInCategory
                            )
                        }
                }
            }

        return OrganizerShelfUiState(
            isLoading = isLoading,
            query = query,
            favourites = favourites,
            categories = categories,
            hiddenApps = hiddenApps,
            availableCategories = orderedCategories,
            categoryAssignmentCounts = categoryAssignmentCounts.toMap(),
            currentCategoryAppCounts = currentCategoryAppCounts,
            focusedCategory = focusedCategory,
            categoryDestinationUnavailable =
                focusedCategoryId != null && focusedCategory == null && !isLoading,
            error = error,
            currentAppCount = apps.size
        )
    }

    private fun CategorizedApp.toUiModel(organizerState: OrganizerState): ShelfAppUiModel = ShelfAppUiModel(
        launchTargetId = app.launchTargetId,
        label = app.label,
        category = category,
        classificationSource = source,
        isFavourite = app.id in organizerState.favouriteAppIds
    )
}
