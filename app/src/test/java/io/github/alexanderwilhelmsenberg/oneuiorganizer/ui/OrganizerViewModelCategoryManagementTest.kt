package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.CategoryManagementRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryDeletionPolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryDeletionChoiceUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryMoveDirectionUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class OrganizerViewModelCategoryManagementTest {
    @Test
    fun `management visibility is mutually exclusive with hidden apps and dismissible`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = viewModel(scope = scope)

        try {
            viewModel.showHiddenApps()
            assertTrue(viewModel.showHiddenApps.value)

            viewModel.showCategoryManagement()
            assertFalse(viewModel.showHiddenApps.value)
            assertTrue(viewModel.showCategoryManagement.value)

            viewModel.hideCategoryManagement()
            assertFalse(viewModel.showCategoryManagement.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `duplicate name failure is sanitized for presentation`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val managementRepository = FakeCategoryManagementRepository()
        managementRepository.createResult =
            CategoryManagementResult.Failure(
                CategoryManagementError.DuplicateName(
                    conflictingCategoryId = AppCategory.WORK.id,
                    displayName = "work"
                )
            )
        val viewModel = viewModel(scope = scope, categoryManagementRepository = managementRepository)

        try {
            viewModel.createCustomCategory("work")

            assertEquals(
                "A category with that name already exists.",
                viewModel.categoryManagementUiState.value.operationError
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `move category submits exact current order with one adjacent swap`() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val initialState =
            OrganizerState(
                customCategories = listOf(custom),
                categoryOrder = OrganizerState.defaultBuiltInCategoryOrder() + custom.id
            )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val managementRepository = FakeCategoryManagementRepository()
        val viewModel =
            viewModel(
                scope = scope,
                organizerState = initialState,
                categoryManagementRepository = managementRepository
            )

        try {
            val expected = initialState.orderedCategories().map { category -> category.id }.toMutableList()
            val originalIndex = expected.indexOf(custom.id)
            val moved = expected.removeAt(originalIndex)
            expected.add(originalIndex - 1, moved)

            viewModel.moveCategory(custom.id, CategoryMoveDirectionUiModel.UP)

            assertEquals(expected, managementRepository.lastRequestedOrder)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `delete reassign choice maps to domain policy without changing identity`() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val managementRepository = FakeCategoryManagementRepository()
        val viewModel = viewModel(scope = scope, categoryManagementRepository = managementRepository)

        try {
            viewModel.deleteCustomCategory(
                custom.id,
                CategoryDeletionChoiceUiModel.Reassign(AppCategory.WORK.id)
            )

            assertEquals(custom.id, managementRepository.lastDeletedCategoryId)
            assertEquals(
                CategoryDeletionPolicy.Reassign(AppCategory.WORK.id),
                managementRepository.lastDeletionPolicy
            )
        } finally {
            scope.cancel()
        }
    }

    private fun viewModel(
        scope: CoroutineScope,
        organizerState: OrganizerState = OrganizerState(),
        categoryManagementRepository: CategoryManagementRepository = FakeCategoryManagementRepository()
    ): OrganizerViewModel = OrganizerViewModel(
        organizerRepository = FakeOrganizerRepository(organizerState),
        categoryManagementRepository = categoryManagementRepository,
        appLauncher =
            object : AppLauncher {
                override fun launch(target: LaunchTargetId): Boolean = true
            },
        scope = scope
    )
}

private class FakeOrganizerRepository(initialState: OrganizerState) : OrganizerRepository {
    override val apps: Flow<List<CategorizedApp>> = MutableStateFlow(emptyList())
    override val organizerState: Flow<OrganizerState> = MutableStateFlow(initialState)

    override suspend fun refresh() = Unit

    override suspend fun setCategoryOverride(appId: AppId, categoryId: CategoryId?) = Unit

    override suspend fun setFavourite(appId: AppId, isFavourite: Boolean) = Unit

    override suspend fun setHidden(appId: AppId, isHidden: Boolean) = Unit
}

private class FakeCategoryManagementRepository : CategoryManagementRepository {
    var createResult: CategoryManagementResult<CustomCategoryDefinition> =
        CategoryManagementResult.Success(
            CustomCategoryDefinition(CategoryId.custom("created"), "Created")
        )
    var lastRequestedOrder: List<CategoryId>? = null
    var lastDeletedCategoryId: CategoryId? = null
    var lastDeletionPolicy: CategoryDeletionPolicy? = null

    override suspend fun createCustomCategory(displayName: String): CategoryManagementResult<CustomCategoryDefinition> =
        createResult

    override suspend fun renameCustomCategory(
        categoryId: CategoryId,
        displayName: String
    ): CategoryManagementResult<CustomCategoryDefinition> =
        CategoryManagementResult.Success(CustomCategoryDefinition(categoryId, displayName))

    override suspend fun deleteCustomCategory(
        categoryId: CategoryId,
        policy: CategoryDeletionPolicy
    ): CategoryManagementResult<Unit> {
        lastDeletedCategoryId = categoryId
        lastDeletionPolicy = policy
        return CategoryManagementResult.Success(Unit)
    }

    override suspend fun reorderCategories(categoryIds: List<CategoryId>): CategoryManagementResult<List<CategoryId>> {
        lastRequestedOrder = categoryIds
        return CategoryManagementResult.Success(categoryIds)
    }
}
