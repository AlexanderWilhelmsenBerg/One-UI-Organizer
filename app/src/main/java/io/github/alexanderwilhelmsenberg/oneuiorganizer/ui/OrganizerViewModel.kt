package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.CategoryManagementRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryDeletionPolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.ClassificationReportFormatter
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryDeletionChoiceUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiStateMapper
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryMoveDirectionUiModel
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
    private val categoryManagementRepository: CategoryManagementRepository,
    private val appLauncher: AppLauncher,
    private val scope: CoroutineScope
) {
    private val query = MutableStateFlow("")
    private val isLoading = MutableStateFlow(true)
    private val error = MutableStateFlow<ShelfErrorUiModel?>(null)
    private val _showHiddenApps = MutableStateFlow(false)
    private val _showCategoryManagement = MutableStateFlow(false)
    private val categoryManagementError = MutableStateFlow<String?>(null)
    private var refreshJob: Job? = null

    private val organizerState =
        organizerRepository.organizerState.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = OrganizerState()
        )

    private val categorizedApps =
        organizerRepository.apps.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val shelfInputs =
        combine(
            categorizedApps,
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

    val categoryManagementUiState =
        combine(
            organizerState,
            categoryManagementError
        ) { state, operationError ->
            CategoryManagementUiStateMapper.map(
                organizerState = state,
                operationError = operationError
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = CategoryManagementUiState()
        )

    val showHiddenApps = _showHiddenApps.asStateFlow()
    val showCategoryManagement = _showCategoryManagement.asStateFlow()

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

    fun moveApp(target: LaunchTargetId, category: CategoryDefinition) {
        updateOrganizerState {
            organizerRepository.setCategoryOverride(target.toAppId(), category.id)
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
        _showCategoryManagement.value = false
        _showHiddenApps.value = true
    }

    fun hideHiddenApps() {
        _showHiddenApps.value = false
    }

    fun showCategoryManagement() {
        _showHiddenApps.value = false
        clearCategoryManagementError()
        _showCategoryManagement.value = true
    }

    fun hideCategoryManagement() {
        _showCategoryManagement.value = false
        clearCategoryManagementError()
    }

    fun createCustomCategory(displayName: String) {
        updateCategoryManagement {
            categoryManagementRepository.createCustomCategory(displayName)
        }
    }

    fun renameCustomCategory(categoryId: CategoryId, displayName: String) {
        updateCategoryManagement {
            categoryManagementRepository.renameCustomCategory(categoryId, displayName)
        }
    }

    fun deleteCustomCategory(categoryId: CategoryId, choice: CategoryDeletionChoiceUiModel) {
        val policy =
            when (choice) {
                CategoryDeletionChoiceUiModel.AutomaticClassification -> CategoryDeletionPolicy.ReturnToAutomatic
                is CategoryDeletionChoiceUiModel.Reassign -> CategoryDeletionPolicy.Reassign(choice.targetCategoryId)
            }
        updateCategoryManagement {
            categoryManagementRepository.deleteCustomCategory(categoryId, policy)
        }
    }

    fun moveCategory(categoryId: CategoryId, direction: CategoryMoveDirectionUiModel) {
        val orderedIds = organizerState.value.orderedCategories().map { category -> category.id }.toMutableList()
        val currentIndex = orderedIds.indexOf(categoryId)
        if (currentIndex < 0) {
            categoryManagementError.value = CATEGORY_UNAVAILABLE_MESSAGE
            return
        }

        val destinationIndex =
            when (direction) {
                CategoryMoveDirectionUiModel.UP -> currentIndex - 1
                CategoryMoveDirectionUiModel.DOWN -> currentIndex + 1
            }
        if (destinationIndex !in orderedIds.indices) {
            return
        }

        val movedCategoryId = orderedIds.removeAt(currentIndex)
        orderedIds.add(destinationIndex, movedCategoryId)
        updateCategoryManagement {
            categoryManagementRepository.reorderCategories(orderedIds)
        }
    }

    fun classificationReport(): String = ClassificationReportFormatter.format(categorizedApps.value)

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

    private fun <T> updateCategoryManagement(update: suspend () -> CategoryManagementResult<T>) {
        scope.launch {
            clearCategoryManagementError()
            val result =
                try {
                    update()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    CategoryManagementResult.Failure(CategoryManagementError.PersistenceFailure)
                }

            categoryManagementError.value =
                when (result) {
                    is CategoryManagementResult.Success -> null
                    is CategoryManagementResult.Failure -> result.error.userMessage()
                }
        }
    }

    private fun clearCategoryManagementError() {
        categoryManagementError.value = null
    }

    private fun CategoryManagementError.userMessage(): String = when (this) {
        CategoryManagementError.BlankName -> "Enter a category name."

        is CategoryManagementError.NameTooLong ->
            "Category names can be at most $maximumCodePoints characters."

        is CategoryManagementError.DuplicateName -> "A category with that name already exists."

        is CategoryManagementError.CategoryNotFound -> CATEGORY_UNAVAILABLE_MESSAGE

        is CategoryManagementError.BuiltInCategoryImmutable ->
            "Built-in categories cannot be renamed or deleted."

        is CategoryManagementError.InvalidReassignmentDestination ->
            "That reassignment destination is no longer available. Choose another category."

        is CategoryManagementError.InvalidOrder ->
            "The category order changed. Try moving the category again."

        CategoryManagementError.PersistenceFailure,
        is CategoryManagementError.InvalidGeneratedCategoryId,
        is CategoryManagementError.CategoryIdAlreadyExists ->
            "Could not save the category change. Try again."
    }

    private fun LaunchTargetId.toAppId(): AppId = AppId(packageName)

    private data class ShelfInputs(
        val apps: List<CategorizedApp>,
        val organizerState: OrganizerState,
        val query: String
    )

    private companion object {
        const val CATEGORY_UNAVAILABLE_MESSAGE = "That category is no longer available. Try again."
    }
}
