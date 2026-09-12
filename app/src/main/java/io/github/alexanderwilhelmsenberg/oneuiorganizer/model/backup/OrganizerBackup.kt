package io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState

@JvmInline
value class OrganizerBackupFormatVersion(val value: Int) {
    init {
        require(value > 0) { "Backup format version must be positive." }
    }

    companion object {
        val V1 = OrganizerBackupFormatVersion(1)
        val CURRENT = V1
    }
}

data class OrganizerBackupCategoryOverride(val appId: AppId, val categoryId: CategoryId)

data class OrganizerBackupCustomCategory(val categoryId: CategoryId, val displayName: String)

data class OrganizerBackupDocument(
    val formatVersion: OrganizerBackupFormatVersion,
    val categoryOverrides: List<OrganizerBackupCategoryOverride>,
    val favouriteAppIds: List<AppId>,
    val hiddenAppIds: List<AppId>,
    val customCategories: List<OrganizerBackupCustomCategory>,
    val categoryOrder: List<CategoryId>
)

data class OrganizerBackupSummary(
    val customCategoryCount: Int,
    val overrideCount: Int,
    val favouriteCount: Int,
    val hiddenCount: Int
)

data class OrganizerBackupExport(
    val content: String,
    val summary: OrganizerBackupSummary,
    val suggestedFileName: String = DEFAULT_FILE_NAME,
    val mimeType: String = MIME_TYPE
) {
    companion object {
        const val DEFAULT_FILE_NAME = "one-ui-organizer-backup.json"
        const val MIME_TYPE = "application/json"
    }
}

class PreparedOrganizerBackupImport internal constructor(
    internal val organizerState: OrganizerState,
    val summary: OrganizerBackupSummary
)

sealed interface OrganizerBackupResult<out T> {
    data class Success<T>(val value: T) : OrganizerBackupResult<T>

    data class Failure(val error: OrganizerBackupError) : OrganizerBackupResult<Nothing>
}

sealed interface OrganizerBackupError {
    data object MalformedDocument : OrganizerBackupError

    data class UnsupportedFormatVersion(val version: Int) : OrganizerBackupError

    data class DuplicateCustomCategoryId(val categoryId: CategoryId) : OrganizerBackupError

    data class InvalidCustomCategoryId(val categoryId: CategoryId) : OrganizerBackupError

    data class InvalidCustomCategoryName(val categoryId: CategoryId) : OrganizerBackupError

    data class DuplicateCategoryOverride(val appId: AppId) : OrganizerBackupError

    data class DuplicateFavouriteAppId(val appId: AppId) : OrganizerBackupError

    data class DuplicateHiddenAppId(val appId: AppId) : OrganizerBackupError

    data class InvalidAppId(val field: OrganizerBackupAppIdField, val index: Int) : OrganizerBackupError

    data class InvalidCategoryReference(val categoryId: CategoryId) : OrganizerBackupError

    data class InvalidCategoryOrder(val problem: OrganizerBackupOrderProblem, val categoryId: CategoryId? = null) :
        OrganizerBackupError

    data object PersistenceFailure : OrganizerBackupError
}

enum class OrganizerBackupAppIdField {
    OVERRIDE,
    FAVOURITE,
    HIDDEN
}

enum class OrganizerBackupOrderProblem {
    DUPLICATE_ID,
    UNKNOWN_ID,
    MISSING_ID
}
