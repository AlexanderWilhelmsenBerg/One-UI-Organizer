package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import java.util.UUID

fun interface CustomCategoryIdGenerator {
    fun generate(): CategoryId
}

object UuidCustomCategoryIdGenerator : CustomCategoryIdGenerator {
    override fun generate(): CategoryId = CategoryId.custom(UUID.randomUUID().toString())
}

sealed interface CategoryDeletionPolicy {
    data object ReturnToAutomatic : CategoryDeletionPolicy

    data class Reassign(val destinationCategoryId: CategoryId) : CategoryDeletionPolicy
}

sealed interface CategoryManagementResult<out T> {
    data class Success<T>(val value: T) : CategoryManagementResult<T>

    data class Failure(val error: CategoryManagementError) : CategoryManagementResult<Nothing>
}

sealed interface CategoryManagementError {
    data object BlankName : CategoryManagementError

    data class NameTooLong(val maximumCodePoints: Int) : CategoryManagementError

    data class DuplicateName(val conflictingCategoryId: CategoryId, val displayName: String) : CategoryManagementError

    data class CategoryNotFound(val categoryId: CategoryId) : CategoryManagementError

    data class BuiltInCategoryImmutable(val categoryId: CategoryId) : CategoryManagementError

    data class InvalidGeneratedCategoryId(val categoryId: CategoryId) : CategoryManagementError

    data class CategoryIdAlreadyExists(val categoryId: CategoryId) : CategoryManagementError

    data class InvalidReassignmentDestination(val categoryId: CategoryId) : CategoryManagementError

    data class InvalidOrder(val problem: CategoryOrderProblem, val categoryId: CategoryId) : CategoryManagementError

    data object PersistenceFailure : CategoryManagementError
}

enum class CategoryOrderProblem {
    DUPLICATE_ID,
    UNKNOWN_ID,
    MISSING_ID
}

object CategoryNamePolicy {
    const val MAX_CODE_POINTS: Int = 48

    fun sanitized(displayName: String): String = displayName.trim()

    fun validate(
        displayName: String,
        existingCategories: Iterable<CategoryDefinition>,
        ignoredCategoryId: CategoryId? = null
    ): CategoryManagementError? {
        val sanitizedName = sanitized(displayName)
        if (sanitizedName.isBlank()) {
            return CategoryManagementError.BlankName
        }
        if (sanitizedName.codePointCount(0, sanitizedName.length) > MAX_CODE_POINTS) {
            return CategoryManagementError.NameTooLong(MAX_CODE_POINTS)
        }

        val comparisonKey = comparisonKey(sanitizedName)
        val duplicate =
            existingCategories.firstOrNull { category ->
                category.id != ignoredCategoryId && comparisonKey(category.displayName) == comparisonKey
            }
        return duplicate?.let { category ->
            CategoryManagementError.DuplicateName(
                conflictingCategoryId = category.id,
                displayName = sanitizedName
            )
        }
    }

    private fun comparisonKey(value: String): String = sanitized(value).uppercase().lowercase()
}
