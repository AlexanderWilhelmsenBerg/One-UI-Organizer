package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryDeletionPolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryNamePolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryOrderProblem
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CustomCategoryIdGenerator
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.UuidCustomCategoryIdGenerator
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.InstalledAppSource
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Source of truth for the current installed-app scan combined with user-owned organizer state.
 *
 * State for packages that are not currently installed is retained. Stale entries never create [CategorizedApp]
 * values because only the current [InstalledAppSource] result is categorized. If the same package is installed again,
 * its retained override, favourite, and hidden state becomes active again.
 *
 * User state is keyed by [AppId], which is package identity. Multiple launcher targets in one package therefore
 * intentionally share category, favourite, and hidden state.
 */
class DefaultOrganizerRepository(
    private val installedAppSource: InstalledAppSource,
    private val organizerStateStore: OrganizerStateStore,
    private val categoryEngine: CategoryEngine,
    private val customCategoryIdGenerator: CustomCategoryIdGenerator = UuidCustomCategoryIdGenerator
) : OrganizerRepository,
    CategoryManagementRepository {
    private val refreshMutex = Mutex()
    private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())

    override val organizerState: Flow<OrganizerState> = organizerStateStore.state

    override val apps: Flow<List<CategorizedApp>> =
        combine(installedApps, organizerStateStore.state) { currentApps, state ->
            currentApps.map { app ->
                val override = state.categoryOverrides[app.id]?.let(state::categoryDefinition)
                categoryEngine.categorize(app, override)
            }
        }

    override suspend fun refresh() {
        refreshMutex.withLock {
            installedApps.value = installedAppSource.loadInstalledApps().toList()
        }
    }

    override suspend fun setCategoryOverride(appId: AppId, categoryId: CategoryId?) {
        organizerStateStore.update { state ->
            if (categoryId != null) {
                requireNotNull(state.categoryDefinition(categoryId)) {
                    "Category override must target a known built-in or custom category."
                }
            }
            val overrides =
                if (categoryId == null) {
                    state.categoryOverrides - appId
                } else {
                    state.categoryOverrides + (appId to categoryId)
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

    override suspend fun createCustomCategory(
        displayName: String
    ): CategoryManagementResult<CustomCategoryDefinition> {
        val generatedId = customCategoryIdGenerator.generate()
        val sanitizedName = CategoryNamePolicy.sanitized(displayName)
        val mutation =
            mutateCategoryState { state ->
                CategoryNamePolicy.validate(
                    displayName = displayName,
                    existingCategories = state.orderedCategories()
                )?.let(::rejectCategoryMutation)
                if (!generatedId.isCustom) {
                    rejectCategoryMutation(CategoryManagementError.InvalidGeneratedCategoryId(generatedId))
                }
                if (state.referencesCategoryId(generatedId)) {
                    rejectCategoryMutation(CategoryManagementError.CategoryIdAlreadyExists(generatedId))
                }

                val created = CustomCategoryDefinition(id = generatedId, displayName = sanitizedName)
                state.copy(
                    customCategories = state.customCategories + created,
                    categoryOrder = state.normalizedCategoryOrder() + generatedId
                )
            }

        return mutation.mapSuccess {
            CustomCategoryDefinition(id = generatedId, displayName = sanitizedName)
        }
    }

    override suspend fun renameCustomCategory(
        categoryId: CategoryId,
        displayName: String
    ): CategoryManagementResult<CustomCategoryDefinition> {
        val sanitizedName = CategoryNamePolicy.sanitized(displayName)
        val mutation =
            mutateCategoryState { state ->
                if (AppCategory.fromId(categoryId) != null) {
                    rejectCategoryMutation(CategoryManagementError.BuiltInCategoryImmutable(categoryId))
                }
                val existingIndex = state.customCategories.indexOfFirst { category -> category.id == categoryId }
                if (existingIndex < 0) {
                    rejectCategoryMutation(CategoryManagementError.CategoryNotFound(categoryId))
                }
                CategoryNamePolicy.validate(
                    displayName = displayName,
                    existingCategories = state.orderedCategories(),
                    ignoredCategoryId = categoryId
                )?.let(::rejectCategoryMutation)

                val renamed = CustomCategoryDefinition(id = categoryId, displayName = sanitizedName)
                state.copy(
                    customCategories =
                        state.customCategories.toMutableList().apply {
                            this[existingIndex] = renamed
                        }
                )
            }

        return mutation.mapSuccess {
            CustomCategoryDefinition(id = categoryId, displayName = sanitizedName)
        }
    }

    override suspend fun deleteCustomCategory(
        categoryId: CategoryId,
        policy: CategoryDeletionPolicy
    ): CategoryManagementResult<Unit> =
        mutateCategoryState { state ->
            if (AppCategory.fromId(categoryId) != null) {
                rejectCategoryMutation(CategoryManagementError.BuiltInCategoryImmutable(categoryId))
            }
            if (state.customCategories.none { category -> category.id == categoryId }) {
                rejectCategoryMutation(CategoryManagementError.CategoryNotFound(categoryId))
            }

            val updatedOverrides =
                when (policy) {
                    CategoryDeletionPolicy.ReturnToAutomatic ->
                        state.categoryOverrides.filterValues { overrideId -> overrideId != categoryId }

                    is CategoryDeletionPolicy.Reassign -> {
                        val destinationId = policy.destinationCategoryId
                        if (destinationId == categoryId || state.categoryDefinition(destinationId) == null) {
                            rejectCategoryMutation(
                                CategoryManagementError.InvalidReassignmentDestination(destinationId)
                            )
                        }
                        state.categoryOverrides.mapValues { (_, overrideId) ->
                            if (overrideId == categoryId) destinationId else overrideId
                        }
                    }
                }

            state.copy(
                categoryOverrides = updatedOverrides,
                customCategories = state.customCategories.filterNot { category -> category.id == categoryId },
                categoryOrder = state.categoryOrder.filterNot { id -> id == categoryId }
            )
        }

    override suspend fun reorderCategories(
        categoryIds: List<CategoryId>
    ): CategoryManagementResult<List<CategoryId>> {
        val requestedOrder = categoryIds.toList()
        val mutation =
            mutateCategoryState { state ->
                val seen = mutableSetOf<CategoryId>()
                requestedOrder.forEach { categoryId ->
                    if (!seen.add(categoryId)) {
                        rejectCategoryMutation(
                            CategoryManagementError.InvalidOrder(
                                problem = CategoryOrderProblem.DUPLICATE_ID,
                                categoryId = categoryId
                            )
                        )
                    }
                }

                val validIds = state.orderedCategories().map { category -> category.id }
                val validIdSet = validIds.toSet()
                requestedOrder.firstOrNull { categoryId -> categoryId !in validIdSet }?.let { categoryId ->
                    rejectCategoryMutation(
                        CategoryManagementError.InvalidOrder(
                            problem = CategoryOrderProblem.UNKNOWN_ID,
                            categoryId = categoryId
                        )
                    )
                }
                validIds.firstOrNull { categoryId -> categoryId !in seen }?.let { categoryId ->
                    rejectCategoryMutation(
                        CategoryManagementError.InvalidOrder(
                            problem = CategoryOrderProblem.MISSING_ID,
                            categoryId = categoryId
                        )
                    )
                }

                state.copy(categoryOrder = requestedOrder)
            }

        return mutation.mapSuccess { requestedOrder }
    }

    private suspend fun mutateCategoryState(
        transform: (OrganizerState) -> OrganizerState
    ): CategoryManagementResult<Unit> =
        try {
            organizerStateStore.update(transform)
            CategoryManagementResult.Success(Unit)
        } catch (rejected: CategoryMutationRejectedException) {
            CategoryManagementResult.Failure(rejected.error)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            CategoryManagementResult.Failure(CategoryManagementError.PersistenceFailure)
        }

    private fun OrganizerState.referencesCategoryId(categoryId: CategoryId): Boolean =
        categoryDefinition(categoryId) != null ||
            categoryId in categoryOrder ||
            categoryId in categoryOverrides.values

    private fun Set<AppId>.updatedMembership(appId: AppId, included: Boolean): Set<AppId> =
        if (included) this + appId else this - appId
}

private class CategoryMutationRejectedException(val error: CategoryManagementError) : RuntimeException()

private fun rejectCategoryMutation(error: CategoryManagementError): Nothing =
    throw CategoryMutationRejectedException(error)

private inline fun <T, R> CategoryManagementResult<T>.mapSuccess(
    transform: (T) -> R
): CategoryManagementResult<R> =
    when (this) {
        is CategoryManagementResult.Failure -> this
        is CategoryManagementResult.Success -> CategoryManagementResult.Success(transform(value))
    }
