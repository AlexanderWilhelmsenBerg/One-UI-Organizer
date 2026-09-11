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
    val validationError: CategoryManagementErrorUiModel? = null,
    val operationError: CategoryManagementErrorUiModel? = null
)

sealed interface CategoryManagementErrorUiModel {
    data object NameRequired : CategoryManagementErrorUiModel

    data class NameTooLong(val maximumCodePoints: Int) : CategoryManagementErrorUiModel

    data object DuplicateName : CategoryManagementErrorUiModel

    data object CategoryUnavailable : CategoryManagementErrorUiModel

    data object BuiltInCategoryImmutable : CategoryManagementErrorUiModel

    data object InvalidReassignmentDestination : CategoryManagementErrorUiModel

    data object InvalidOrder : CategoryManagementErrorUiModel

    data object SaveFailed : CategoryManagementErrorUiModel
}

sealed interface CategoryDeletionChoiceUiModel {
    data object AutomaticClassification : CategoryDeletionChoiceUiModel

    data class Reassign(val targetCategoryId: CategoryId) : CategoryDeletionChoiceUiModel
}

enum class CategoryMoveDirectionUiModel {
    UP,
    DOWN
}
