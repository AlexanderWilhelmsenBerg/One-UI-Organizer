package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId

data class ShelfAppUiModel(
    val launchTargetId: LaunchTargetId,
    val label: String,
    val category: AppCategory,
    val icon: ImageBitmap? = null,
    val isFavourite: Boolean = false
) {
    val stableKey: String
        get() = "${launchTargetId.packageName}/${launchTargetId.className}"
}

data class CategorySectionUiModel(val category: AppCategory, val apps: List<ShelfAppUiModel>)

enum class ShelfContentMode {
    LOADING,
    CONTENT,
    NO_RESULTS,
    EMPTY
}

data class OrganizerShelfUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val favourites: List<ShelfAppUiModel> = emptyList(),
    val categories: List<CategorySectionUiModel> = emptyList(),
    val hiddenApps: List<ShelfAppUiModel> = emptyList(),
    val availableCategories: List<AppCategory> = AppCategory.entries
) {
    val hasVisibleApps: Boolean
        get() = favourites.isNotEmpty() || categories.any { it.apps.isNotEmpty() }

    val contentMode: ShelfContentMode
        get() =
            when {
                isLoading -> ShelfContentMode.LOADING
                hasVisibleApps -> ShelfContentMode.CONTENT
                query.isNotBlank() -> ShelfContentMode.NO_RESULTS
                else -> ShelfContentMode.EMPTY
            }
}

fun AppCategory.displayName(): String = when (this) {
    AppCategory.COMMUNICATION -> "Communication"
    AppCategory.SOCIAL -> "Social"
    AppCategory.WORK -> "Work"
    AppCategory.PRODUCTIVITY -> "Productivity"
    AppCategory.SMART_HOME -> "Smart Home"
    AppCategory.HOMELAB -> "Homelab"
    AppCategory.FINANCE -> "Finance"
    AppCategory.SHOPPING -> "Shopping"
    AppCategory.TRAVEL_NAVIGATION -> "Travel & Navigation"
    AppCategory.MUSIC_AUDIO -> "Music & Audio"
    AppCategory.VIDEO -> "Video"
    AppCategory.PHOTOS -> "Photos"
    AppCategory.READING -> "Reading"
    AppCategory.DEVELOPMENT -> "Development"
    AppCategory.TOOLS -> "Tools"
    AppCategory.GAMES -> "Games"
    AppCategory.OTHER -> "Other"
    AppCategory.UNSORTED -> "Unsorted"
}
