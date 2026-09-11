package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.LocalAppSearch
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState

object OrganizerUiStateMapper {
    fun map(
        apps: List<CategorizedApp>,
        organizerState: OrganizerState,
        query: String,
        isLoading: Boolean,
        error: ShelfErrorUiModel?
    ): OrganizerShelfUiState {
        val currentAppIds = apps.mapTo(mutableSetOf()) { categorizedApp -> categorizedApp.app.id }
        val categoryAssignmentCounts =
            apps.groupingBy { categorizedApp -> categorizedApp.category.id }.eachCount().toMutableMap()
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

        val visibleApps =
            apps.filterNot { categorizedApp -> categorizedApp.app.id in organizerState.hiddenAppIds }
        val filteredApps = LocalAppSearch.filter(visibleApps, query)

        val favourites =
            filteredApps
                .filter { categorizedApp -> categorizedApp.app.id in organizerState.favouriteAppIds }
                .map { categorizedApp -> categorizedApp.toUiModel(organizerState) }

        val orderedCategories = organizerState.orderedCategories()
        val categories =
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

        return OrganizerShelfUiState(
            isLoading = isLoading,
            query = query,
            favourites = favourites,
            categories = categories,
            hiddenApps = hiddenApps,
            availableCategories = orderedCategories,
            categoryAssignmentCounts = categoryAssignmentCounts.toMap(),
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
