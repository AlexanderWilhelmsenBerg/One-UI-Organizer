package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryNamePolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupAppIdField
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupCategoryOverride
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupCustomCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupDocument
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupFormatVersion
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupOrderProblem
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupSummary
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.PreparedOrganizerBackupImport

object OrganizerBackupMapper {
    fun fromState(state: OrganizerState): OrganizerBackupDocument {
        val normalizedState = state.normalized()
        return OrganizerBackupDocument(
            formatVersion = OrganizerBackupFormatVersion.CURRENT,
            categoryOverrides =
                normalizedState.categoryOverrides.entries
                    .sortedBy { entry -> entry.key.packageName }
                    .map { (appId, categoryId) -> OrganizerBackupCategoryOverride(appId, categoryId) },
            favouriteAppIds = normalizedState.favouriteAppIds.sortedBy(AppId::packageName),
            hiddenAppIds = normalizedState.hiddenAppIds.sortedBy(AppId::packageName),
            customCategories =
                normalizedState.customCategories.map { category ->
                    OrganizerBackupCustomCategory(
                        categoryId = category.id,
                        displayName = category.displayName
                    )
                },
            categoryOrder = normalizedState.categoryOrder.toList()
        )
    }

    fun summary(state: OrganizerState): OrganizerBackupSummary = OrganizerBackupSummary(
        customCategoryCount = state.customCategories.size,
        overrideCount = state.categoryOverrides.size,
        favouriteCount = state.favouriteAppIds.size,
        hiddenCount = state.hiddenAppIds.size
    )
}

object OrganizerBackupValidator {
    fun prepare(document: OrganizerBackupDocument): OrganizerBackupResult<PreparedOrganizerBackupImport> {
        if (document.formatVersion != OrganizerBackupFormatVersion.CURRENT) {
            return OrganizerBackupResult.Failure(
                OrganizerBackupError.UnsupportedFormatVersion(document.formatVersion.value)
            )
        }

        val acceptedCategories = mutableListOf<CategoryDefinition>().apply {
            addAll(AppCategory.entries)
        }
        val validatedCustomCategories = mutableListOf<CustomCategoryDefinition>()
        val seenCustomIds = mutableSetOf<CategoryId>()
        document.customCategories.forEach { category ->
            if (!category.categoryId.isValidPortableCustomId()) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.InvalidCustomCategoryId(category.categoryId)
                )
            }
            if (!seenCustomIds.add(category.categoryId)) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.DuplicateCustomCategoryId(category.categoryId)
                )
            }
            if (
                CategoryNamePolicy.sanitized(category.displayName) != category.displayName ||
                CategoryNamePolicy.validate(category.displayName, acceptedCategories) != null
            ) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.InvalidCustomCategoryName(category.categoryId)
                )
            }
            val validated =
                CustomCategoryDefinition(
                    id = category.categoryId,
                    displayName = category.displayName
                )
            validatedCustomCategories += validated
            acceptedCategories += validated
        }

        val knownCategoryIds = acceptedCategories.mapTo(linkedSetOf()) { category -> category.id }
        val overrideAppIds = mutableSetOf<AppId>()
        document.categoryOverrides.forEachIndexed { index, override ->
            if (!override.appId.isValidPortableAppId()) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.InvalidAppId(OrganizerBackupAppIdField.OVERRIDE, index)
                )
            }
            if (!overrideAppIds.add(override.appId)) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.DuplicateCategoryOverride(override.appId)
                )
            }
            if (override.categoryId !in knownCategoryIds) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.InvalidCategoryReference(override.categoryId)
                )
            }
        }

        validateAppIdList(
            values = document.favouriteAppIds,
            field = OrganizerBackupAppIdField.FAVOURITE,
            duplicateError = OrganizerBackupError::DuplicateFavouriteAppId
        )?.let { error -> return OrganizerBackupResult.Failure(error) }
        validateAppIdList(
            values = document.hiddenAppIds,
            field = OrganizerBackupAppIdField.HIDDEN,
            duplicateError = OrganizerBackupError::DuplicateHiddenAppId
        )?.let { error -> return OrganizerBackupResult.Failure(error) }

        val seenOrderIds = mutableSetOf<CategoryId>()
        document.categoryOrder.forEach { categoryId ->
            if (!seenOrderIds.add(categoryId)) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.InvalidCategoryOrder(
                        problem = OrganizerBackupOrderProblem.DUPLICATE_ID,
                        categoryId = categoryId
                    )
                )
            }
            if (categoryId !in knownCategoryIds) {
                return OrganizerBackupResult.Failure(
                    OrganizerBackupError.InvalidCategoryOrder(
                        problem = OrganizerBackupOrderProblem.UNKNOWN_ID,
                        categoryId = categoryId
                    )
                )
            }
        }
        knownCategoryIds.firstOrNull { categoryId -> categoryId !in seenOrderIds }?.let { categoryId ->
            return OrganizerBackupResult.Failure(
                OrganizerBackupError.InvalidCategoryOrder(
                    problem = OrganizerBackupOrderProblem.MISSING_ID,
                    categoryId = categoryId
                )
            )
        }

        val organizerState =
            OrganizerState(
                schemaVersion = OrganizerState.CURRENT_SCHEMA_VERSION,
                categoryOverrides = document.categoryOverrides.associate { override ->
                    override.appId to override.categoryId
                },
                favouriteAppIds = document.favouriteAppIds.toSet(),
                hiddenAppIds = document.hiddenAppIds.toSet(),
                customCategories = validatedCustomCategories,
                categoryOrder = document.categoryOrder.toList()
            )
        return OrganizerBackupResult.Success(
            PreparedOrganizerBackupImport(
                organizerState = organizerState,
                summary = OrganizerBackupMapper.summary(organizerState)
            )
        )
    }

    private fun validateAppIdList(
        values: List<AppId>,
        field: OrganizerBackupAppIdField,
        duplicateError: (AppId) -> OrganizerBackupError
    ): OrganizerBackupError? {
        val seen = mutableSetOf<AppId>()
        values.forEachIndexed { index, appId ->
            if (!appId.isValidPortableAppId()) {
                return OrganizerBackupError.InvalidAppId(field, index)
            }
            if (!seen.add(appId)) {
                return duplicateError(appId)
            }
        }
        return null
    }

    private fun AppId.isValidPortableAppId(): Boolean = packageName.isNotBlank() && packageName == packageName.trim()

    private fun CategoryId.isValidPortableCustomId(): Boolean =
        isCustom && value.removePrefix(CUSTOM_PREFIX).let { opaqueId ->
            opaqueId.isNotBlank() && opaqueId == opaqueId.trim()
        }

    private const val CUSTOM_PREFIX = "custom:"
}
