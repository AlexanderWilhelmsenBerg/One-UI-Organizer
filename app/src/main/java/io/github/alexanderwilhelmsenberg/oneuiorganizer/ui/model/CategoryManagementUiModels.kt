package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryKind

data class CategoryManagementItemUiModel(val category: CategoryDefinition, val assignedAppCount: Int = 0) {
    val isCustom: Boolean
        get() = category.kind == CategoryKind.CUSTOM
}

data class CategoryManagementUiState(
    val categories: List<CategoryManagementItemUiModel> = emptyList(),
    val validationError: String? = null,
    val operationError: String? = null
)

sealed interface CategoryDeletionChoiceUiModel {
    data object AutomaticClassification : CategoryDeletionChoiceUiModel

    data class Reassign(val targetCategoryId: CategoryId) : CategoryDeletionChoiceUiModel
}

enum class CategoryMoveDirectionUiModel {
    UP,
    DOWN
}
