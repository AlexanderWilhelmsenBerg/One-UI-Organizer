package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import androidx.compose.runtime.Immutable
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId

@Immutable
data class OrganizerAppUiModel(
    val appId: AppId,
    val launchTargetId: LaunchTargetId,
    val label: String,
    val categoryId: CategoryId,
    val categoryName: String,
    val classificationSource: ClassificationSource,
    val classificationSourceLabel: String,
    val isFavourite: Boolean,
    val isHidden: Boolean
)

@Immutable
data class OrganizerCategoryUiModel(
    val categoryId: CategoryId,
    val displayName: String,
    val apps: List<OrganizerAppUiModel>
)

@Immutable
data class OrganizerUiState(
    val query: String = "",
    val categories: List<OrganizerCategoryUiModel> = emptyList(),
    val hiddenApps: List<OrganizerAppUiModel> = emptyList(),
    val unsortedCount: Int = 0,
    val selectedCategoryId: CategoryId? = null,
    val categoryManagement: CategoryManagementUiState = CategoryManagementUiState()
) {
    val isSearching: Boolean
        get() = query.isNotBlank()

    val selectedCategory: OrganizerCategoryUiModel?
        get() = selectedCategoryId?.let { id -> categories.firstOrNull { it.categoryId == id } }

    val selectedVisibleApps: List<OrganizerAppUiModel>
        get() = selectedCategory?.apps.orEmpty()

    val isShowingAllApps: Boolean
        get() = selectedCategoryId == null
}

fun CategoryDefinition.uiDisplayName(): String = when (this) {
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
    AppCategory.WEB_SHORTCUTS -> "Web Shortcuts"
    AppCategory.DEVELOPMENT -> "Development"
    AppCategory.TOOLS -> "Tools"
    AppCategory.EMULATORS -> "Emulators"
    AppCategory.GAME_ACTION_ADVENTURE -> "Action & Adventure"
    AppCategory.GAME_RPG -> "RPG"
    AppCategory.GAME_STRATEGY_SIMULATION -> "Strategy & Simulation"
    AppCategory.GAME_PUZZLE_CASUAL -> "Puzzle & Casual"
    AppCategory.GAME_BOARD_CARD -> "Board & Card"
    AppCategory.GAMES -> "Games"
    AppCategory.OTHER -> "Other"
    AppCategory.UNSORTED -> "Unsorted"
    else -> displayName
}

fun ClassificationSource.uiDisplayName(): String = when (this) {
    ClassificationSource.USER_OVERRIDE -> "Your category"
    ClassificationSource.KNOWN_APP_RULE -> "Known app rule"
    ClassificationSource.ANDROID_DECLARED_CATEGORY -> "Android category"
    ClassificationSource.SUPPORTED_METADATA -> "Supported metadata"
    ClassificationSource.UNSORTED_FALLBACK -> "Needs sorting"
}
