package io.github.alexanderwilhelmsenberg.oneuiorganizer.data.backup

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupCategoryOverride
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupCustomCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupDocument
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupFormatVersion
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object OrganizerBackupJsonCodec {
    fun encode(document: OrganizerBackupDocument): String {
        require(document.formatVersion == OrganizerBackupFormatVersion.CURRENT) {
            "Only the current organizer backup format can be written."
        }

        val root =
            JsonObject(
                linkedMapOf(
                    FORMAT_VERSION to JsonPrimitive(document.formatVersion.value),
                    CATEGORY_OVERRIDES to
                        JsonArray(
                            document.categoryOverrides.map { override ->
                                JsonObject(
                                    linkedMapOf(
                                        APP_ID to JsonPrimitive(override.appId.packageName),
                                        CATEGORY_ID to JsonPrimitive(override.categoryId.value)
                                    )
                                )
                            }
                        ),
                    FAVOURITE_APP_IDS to
                        JsonArray(
                            document.favouriteAppIds.map { appId -> JsonPrimitive(appId.packageName) }
                        ),
                    HIDDEN_APP_IDS to
                        JsonArray(
                            document.hiddenAppIds.map { appId -> JsonPrimitive(appId.packageName) }
                        ),
                    CUSTOM_CATEGORIES to
                        JsonArray(
                            document.customCategories.map { category ->
                                JsonObject(
                                    linkedMapOf(
                                        CATEGORY_ID to JsonPrimitive(category.categoryId.value),
                                        CATEGORY_NAME to JsonPrimitive(category.displayName)
                                    )
                                )
                            }
                        ),
                    CATEGORY_ORDER to
                        JsonArray(
                            document.categoryOrder.map { categoryId -> JsonPrimitive(categoryId.value) }
                        )
                )
            )
        return root.toString()
    }

    fun decode(encoded: String): OrganizerBackupResult<OrganizerBackupDocument> = try {
        require(encoded.isNotBlank()) { "Backup document is empty." }
        val root = Json.parseToJsonElement(encoded) as? JsonObject
            ?: throw IllegalArgumentException("Backup root must be a JSON object.")
        val versionPrimitive = root.requiredPrimitive(FORMAT_VERSION)
        require(!versionPrimitive.isString) { "$FORMAT_VERSION must be numeric." }
        val version = versionPrimitive.content.toIntOrNull()
            ?: throw IllegalArgumentException("$FORMAT_VERSION must be an integer.")

        OrganizerBackupResult.Success(
            OrganizerBackupDocument(
                formatVersion = OrganizerBackupFormatVersion(version),
                categoryOverrides = root.decodeOverrides(),
                favouriteAppIds = root.decodeAppIds(FAVOURITE_APP_IDS),
                hiddenAppIds = root.decodeAppIds(HIDDEN_APP_IDS),
                customCategories = root.decodeCustomCategories(),
                categoryOrder = root.decodeCategoryOrder()
            )
        )
    } catch (_: Exception) {
        OrganizerBackupResult.Failure(OrganizerBackupError.MalformedDocument)
    }

    private fun JsonObject.decodeOverrides(): List<OrganizerBackupCategoryOverride> =
        requiredArray(CATEGORY_OVERRIDES).map { element ->
            val value = element as? JsonObject
                ?: throw IllegalArgumentException("$CATEGORY_OVERRIDES entries must be objects.")
            OrganizerBackupCategoryOverride(
                appId = AppId(value.requiredString(APP_ID)),
                categoryId = CategoryId(value.requiredString(CATEGORY_ID))
            )
        }

    private fun JsonObject.decodeAppIds(key: String): List<AppId> =
        requiredArray(key).map { element -> AppId(element.requiredStringValue(key)) }

    private fun JsonObject.decodeCustomCategories(): List<OrganizerBackupCustomCategory> =
        requiredArray(CUSTOM_CATEGORIES).map { element ->
            val value = element as? JsonObject
                ?: throw IllegalArgumentException("$CUSTOM_CATEGORIES entries must be objects.")
            OrganizerBackupCustomCategory(
                categoryId = CategoryId(value.requiredString(CATEGORY_ID)),
                displayName = value.requiredString(CATEGORY_NAME)
            )
        }

    private fun JsonObject.decodeCategoryOrder(): List<CategoryId> =
        requiredArray(CATEGORY_ORDER).map { element ->
            CategoryId(element.requiredStringValue(CATEGORY_ORDER))
        }

    private fun JsonObject.requiredArray(key: String): JsonArray =
        this[key] as? JsonArray ?: throw IllegalArgumentException("Missing or invalid $key.")

    private fun JsonObject.requiredString(key: String): String {
        val primitive = requiredPrimitive(key)
        require(primitive.isString) { "$key must be a string." }
        return primitive.content
    }

    private fun JsonObject.requiredPrimitive(key: String): JsonPrimitive =
        this[key] as? JsonPrimitive ?: throw IllegalArgumentException("Missing or invalid $key.")

    private fun JsonElement.requiredStringValue(field: String): String {
        val primitive = this as? JsonPrimitive
            ?: throw IllegalArgumentException("$field entries must be strings.")
        require(primitive.isString) { "$field entries must be strings." }
        return primitive.content
    }

    private const val FORMAT_VERSION = "formatVersion"
    private const val CATEGORY_OVERRIDES = "categoryOverrides"
    private const val FAVOURITE_APP_IDS = "favouriteAppIds"
    private const val HIDDEN_APP_IDS = "hiddenAppIds"
    private const val CUSTOM_CATEGORIES = "customCategories"
    private const val CATEGORY_ORDER = "categoryOrder"
    private const val APP_ID = "appId"
    private const val CATEGORY_ID = "categoryId"
    private const val CATEGORY_NAME = "name"
}
