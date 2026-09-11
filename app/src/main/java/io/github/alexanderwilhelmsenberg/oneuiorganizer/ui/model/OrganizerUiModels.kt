package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId

data class ShelfAppUiModel(
    val launchTargetId: LaunchTargetId,
    val label: String,
    val category: CategoryDefinition,
    val classificationSource: ClassificationSource,
    val icon: ImageBitmap? = null,
    val isFavourite: Boolean = false
) {
    val stableKey: String
        get() = "${launchTargetId.packageName}/${launchTargetId.className}"
}

data class CategorySectionUiModel(val category: CategoryDefinition, val apps: List<ShelfAppUiModel>)

enum class ShelfErrorUiModel {
    SCAN_FAILED,
    LAUNCH_FAILED,
    STATE_UPDATE_FAILED
}

enum class ShelfContentMode {
    LOADING,
    CONTENT,
    ERROR,
    NO_RESULTS,
    EMPTY
}

data class OrganizerShelfUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val favourites: List<ShelfAppUiModel> = emptyList(),
    val categories: List<CategorySectionUiModel> = emptyList(),
    val hiddenApps: List<ShelfAppUiModel> = emptyList(),
    val availableCategories: List<CategoryDefinition> = emptyList(),
    val categoryAssignmentCounts: Map<CategoryId, Int> = emptyMap(),
    val error: ShelfErrorUiModel? = null,
    val currentAppCount: Int = 0
) {
    val hasVisibleApps: Boolean
        get() = favourites.isNotEmpty() || categories.any { it.apps.isNotEmpty() }

    val hasAnyCurrentApps: Boolean
        get() = currentAppCount > 0

    val contentMode: ShelfContentMode
        get() =
            when {
                isLoading -> ShelfContentMode.LOADING
                hasVisibleApps -> ShelfContentMode.CONTENT
                error != null -> ShelfContentMode.ERROR
                query.isNotBlank() -> ShelfContentMode.NO_RESULTS
                else -> ShelfContentMode.EMPTY
            }
}

fun AppCategory.displayName(): String = displayName

fun ClassificationSource.displayName(): String = when (this) {
    ClassificationSource.USER_OVERRIDE -> "Your category"
    ClassificationSource.KNOWN_APP_RULE -> "Known app rule"
    ClassificationSource.ANDROID_DECLARED_CATEGORY -> "Android category"
    ClassificationSource.UNSORTED_FALLBACK -> "Needs sorting"
}
