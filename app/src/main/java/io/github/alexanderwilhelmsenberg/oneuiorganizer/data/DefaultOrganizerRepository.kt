package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.InstalledAppSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Source of truth for the current installed-app scan combined with user-owned organizer state.
 *
 * v0.1 retains state for packages that are not currently installed. Stale entries never create [CategorizedApp]
 * values because only the current [InstalledAppSource] result is categorized. If the same package is installed again,
 * its retained override, favourite, and hidden state becomes active again. Cleanup is deferred until the product has
 * an explicit user-visible or bounded retention policy, avoiding silent loss of user corrections.
 *
 * User state is keyed by [AppId], which is package identity in the frozen v0.1 contract. Multiple launcher targets in
 * one package therefore intentionally share category, favourite, and hidden state.
 */
class DefaultOrganizerRepository(
    private val installedAppSource: InstalledAppSource,
    private val organizerStateStore: OrganizerStateStore,
    private val categoryEngine: CategoryEngine
) : OrganizerRepository {
    private val refreshMutex = Mutex()
    private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())

    override val organizerState: Flow<OrganizerState> = organizerStateStore.state

    override val apps: Flow<List<CategorizedApp>> =
        combine(installedApps, organizerStateStore.state) { currentApps, state ->
            currentApps.map { app ->
                categoryEngine.categorize(app, state.categoryOverrides[app.id])
            }
        }

    override suspend fun refresh() {
        refreshMutex.withLock {
            installedApps.value = installedAppSource.loadInstalledApps().toList()
        }
    }

    override suspend fun setCategoryOverride(appId: AppId, category: AppCategory?) {
        organizerStateStore.update { state ->
            val overrides =
                if (category == null) {
                    state.categoryOverrides - appId
                } else {
                    state.categoryOverrides + (appId to category)
                }
            state.copy(categoryOverrides = overrides)
        }
    }

    override suspend fun setFavourite(appId: AppId, isFavourite: Boolean) {
        organizerStateStore.update { state ->
            state.copy(
                favouriteAppIds =
                    state.favouriteAppIds.updatedMembership(
                        appId = appId,
                        included = isFavourite
                    )
            )
        }
    }

    override suspend fun setHidden(appId: AppId, isHidden: Boolean) {
        organizerStateStore.update { state ->
            state.copy(
                hiddenAppIds =
                    state.hiddenAppIds.updatedMembership(
                        appId = appId,
                        included = isHidden
                    )
            )
        }
    }

    private fun Set<AppId>.updatedMembership(appId: AppId, included: Boolean): Set<AppId> =
        if (included) this + appId else this - appId
}
