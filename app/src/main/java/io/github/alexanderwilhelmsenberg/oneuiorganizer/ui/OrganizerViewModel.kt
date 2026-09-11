package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerShelfUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerUiStateMapper
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfErrorUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Activity-scoped presentation state holder backed by process-scoped repository state. */
class OrganizerViewModel(
    private val organizerRepository: OrganizerRepository,
    private val appLauncher: AppLauncher,
    private val scope: CoroutineScope
) {
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val error = MutableStateFlow<ShelfErrorUiModel?>(null)
    private val _showHiddenApps = MutableStateFlow(false)
    private var refreshJob: Job? = null

    private val organizerState =
        organizerRepository.organizerState.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = OrganizerState()
        )

    private val shelfInputs =
        combine(
            organizerRepository.apps,
            organizerState,
            query
        ) { apps, state, currentQuery ->
            ShelfInputs(
                apps = apps,
                organizerState = state,
                query = currentQuery
            )
        }

    val uiState =
        combine(
            shelfInputs,
            isLoading,
            error
        ) { inputs, loading, currentError ->
            OrganizerUiStateMapper.map(
                apps = inputs.apps,
                organizerState = inputs.organizerState,
                query = inputs.query,
                isLoading = loading,
                error = currentError
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = OrganizerShelfUiState(isLoading = true)
        )

    val showHiddenApps = _showHiddenApps.asStateFlow()

    fun refresh() {
        if (refreshJob?.isActive == true) {
            return
        }

        refreshJob =
            scope.launch {
                if (!uiState.value.hasAnyCurrentApps) {
                    isLoading.value = true
                }
                error.value = null
                try {
                    organizerRepository.refresh()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    error.value = ShelfErrorUiModel.SCAN_FAILED
                } finally {
                    isLoading.value = false
                }
            }
    }

    fun updateQuery(value: String) {
        query.value = value
        if (error.value == ShelfErrorUiModel.LAUNCH_FAILED) {
            error.value = null
        }
    }

    fun launch(target: LaunchTargetId): Boolean {
        val launched = appLauncher.launch(target)
        error.value = if (launched) null else ShelfErrorUiModel.LAUNCH_FAILED
        return launched
    }

    fun moveApp(target: LaunchTargetId, category: AppCategory) {
        updateOrganizerState {
            organizerRepository.setCategoryOverride(target.toAppId(), category)
        }
    }

    fun toggleFavourite(target: LaunchTargetId) {
        val appId = target.toAppId()
        val isFavourite = appId in organizerState.value.favouriteAppIds
        updateOrganizerState {
            organizerRepository.setFavourite(appId, !isFavourite)
        }
    }

    fun hideApp(target: LaunchTargetId) {
        updateOrganizerState {
            organizerRepository.setHidden(target.toAppId(), true)
        }
    }

    fun restoreApp(target: LaunchTargetId) {
        updateOrganizerState {
            organizerRepository.setHidden(target.toAppId(), false)
        }
    }

    fun showHiddenApps() {
        _showHiddenApps.value = true
    }

    fun hideHiddenApps() {
        _showHiddenApps.value = false
    }

    private fun updateOrganizerState(update: suspend () -> Unit) {
        scope.launch {
            try {
                update()
                error.value = null
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                error.value = ShelfErrorUiModel.STATE_UPDATE_FAILED
            }
        }
    }

    private fun LaunchTargetId.toAppId(): AppId = AppId(packageName)

    private data class ShelfInputs(
        val apps: List<CategorizedApp>,
        val organizerState: OrganizerState,
        val query: String
    )
}
