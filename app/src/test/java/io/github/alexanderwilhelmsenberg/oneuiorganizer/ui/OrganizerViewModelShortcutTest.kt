package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.CategoryManagementRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryDeletionPolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryShortcutDestination
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AppLauncher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class OrganizerViewModelShortcutTest {
    @Test
    fun `shortcut destination focuses exact category and can return to full shelf`() {
        val family = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val organizerState = OrganizerState(customCategories = listOf(family))
        val apps =
            listOf(
                categorizedApp("family.app", family.id, family),
                categorizedApp("work.app", AppCategory.WORK.id, AppCategory.WORK)
            )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = viewModel(scope, organizerState, apps)

        try {
            viewModel.openCategoryDestination(CategoryShortcutDestination(family.id))

            assertEquals(family.id, viewModel.uiState.value.focusedCategory?.id)
            assertEquals(listOf(family.id), viewModel.uiState.value.categories.map { it.category.id })
            assertFalse(viewModel.uiState.value.categoryDestinationUnavailable)

            viewModel.clearCategoryDestination()

            assertNull(viewModel.uiState.value.focusedCategory)
            assertTrue(viewModel.uiState.value.categories.any { it.category.id == AppCategory.WORK.id })
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `deleted category shortcut never mutates repository state and falls back safely`() {
        val repository = ShortcutOrganizerRepository(OrganizerState(), listOf(categorizedApp("work.app", AppCategory.WORK.id, AppCategory.WORK)))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel =
            OrganizerViewModel(
                organizerRepository = repository,
                categoryManagementRepository = ShortcutCategoryManagementRepository(),
                appLauncher = SuccessfulShortcutAppLauncher,
                scope = scope
            )

        try {
            viewModel.openCategoryDestination(CategoryShortcutDestination(CategoryId.custom("deleted")))

            assertTrue(viewModel.uiState.value.categoryDestinationUnavailable)
            assertNull(viewModel.uiState.value.focusedCategory)
            assertEquals(0, repository.mutationCount)
        } finally {
            scope.cancel()
        }
    }

    private fun viewModel(
        scope: CoroutineScope,
        organizerState: OrganizerState,
        apps: List<CategorizedApp>
    ): OrganizerViewModel =
        OrganizerViewModel(
            organizerRepository = ShortcutOrganizerRepository(organizerState, apps),
            categoryManagementRepository = ShortcutCategoryManagementRepository(),
            appLauncher = SuccessfulShortcutAppLauncher,
            scope = scope
        )

    private fun categorizedApp(
        packageName: String,
        categoryId: CategoryId,
        category: io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
    ): CategorizedApp =
        CategorizedApp(
            app = InstalledApp(AppId(packageName), LaunchTargetId(packageName, "MainActivity"), packageName),
            category = category,
            source = if (categoryId == AppCategory.WORK.id) ClassificationSource.KNOWN_APP_RULE else ClassificationSource.USER_OVERRIDE
        )
}

private class ShortcutOrganizerRepository(
    initialState: OrganizerState,
    initialApps: List<CategorizedApp>
) : OrganizerRepository {
    override val apps: Flow<List<CategorizedApp>> = MutableStateFlow(initialApps)
    override val organizerState: Flow<OrganizerState> = MutableStateFlow(initialState)
    var mutationCount = 0
        private set

    override suspend fun refresh() = Unit

    override suspend fun setCategoryOverride(appId: AppId, categoryId: CategoryId?) {
        mutationCount += 1
    }

    override suspend fun setFavourite(appId: AppId, isFavourite: Boolean) {
        mutationCount += 1
    }

    override suspend fun setHidden(appId: AppId, isHidden: Boolean) {
        mutationCount += 1
    }
}

private class ShortcutCategoryManagementRepository : CategoryManagementRepository {
    override suspend fun createCustomCategory(displayName: String): CategoryManagementResult<CustomCategoryDefinition> =
        error("Shortcut routing must not create categories")

    override suspend fun renameCustomCategory(
        categoryId: CategoryId,
        displayName: String
    ): CategoryManagementResult<CustomCategoryDefinition> = error("Shortcut routing must not rename categories")

    override suspend fun deleteCustomCategory(
        categoryId: CategoryId,
        policy: CategoryDeletionPolicy
    ): CategoryManagementResult<Unit> = error("Shortcut routing must not delete categories")

    override suspend fun reorderCategories(categoryIds: List<CategoryId>): CategoryManagementResult<List<CategoryId>> =
        error("Shortcut routing must not reorder categories")
}

private object SuccessfulShortcutAppLauncher : AppLauncher {
    override fun launch(target: LaunchTargetId): Boolean = true
}
