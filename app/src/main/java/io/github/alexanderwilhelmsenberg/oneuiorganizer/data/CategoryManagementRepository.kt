package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryDeletionPolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition

interface CategoryManagementRepository {
    suspend fun createCustomCategory(displayName: String): CategoryManagementResult<CustomCategoryDefinition>

    suspend fun renameCustomCategory(
        categoryId: CategoryId,
        displayName: String
    ): CategoryManagementResult<CustomCategoryDefinition>

    suspend fun deleteCustomCategory(
        categoryId: CategoryId,
        policy: CategoryDeletionPolicy
    ): CategoryManagementResult<Unit>

    suspend fun reorderCategories(categoryIds: List<CategoryId>): CategoryManagementResult<List<CategoryId>>
}
