package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * DataStore-backed organizer state.
 *
 * Callers must create at most one active instance for a given file. Corrupt or unsupported persisted data is
 * replaced with an empty schema-v1 state so the organizer remains usable; this recovery resets only organizer
 * preferences, never Android-installed-app data.
 */
class DataStoreOrganizerStateStore private constructor(private val dataStore: DataStore<OrganizerState>) :
    OrganizerStateStore {
    override val state: Flow<OrganizerState> = dataStore.data

    override suspend fun update(transform: (OrganizerState) -> OrganizerState): OrganizerState =
        dataStore.updateData { current ->
            transform(current).copy(schemaVersion = OrganizerState.CURRENT_SCHEMA_VERSION)
        }

    companion object {
        fun create(file: File, scope: CoroutineScope): DataStoreOrganizerStateStore {
            val dataStore =
                DataStoreFactory.create(
                    serializer = OrganizerStateSerializer,
                    corruptionHandler = ReplaceFileCorruptionHandler { OrganizerState() },
                    scope = scope,
                    produceFile = { file }
                )
            return DataStoreOrganizerStateStore(dataStore)
        }
    }
}

internal object OrganizerStateSerializer : Serializer<OrganizerState> {
    override val defaultValue: OrganizerState = OrganizerState()

    override suspend fun readFrom(input: InputStream): OrganizerState {
        val encoded = String(input.readBytes(), Charsets.UTF_8)
        return try {
            OrganizerStateJsonCodec.decode(encoded)
        } catch (exception: IllegalArgumentException) {
            throw CorruptionException("Organizer state is corrupt or uses an unsupported schema.", exception)
        }
    }

    override suspend fun writeTo(t: OrganizerState, output: OutputStream) {
        output.write(OrganizerStateJsonCodec.encode(t).toByteArray(Charsets.UTF_8))
    }
}

internal object OrganizerStateJsonCodec {
    fun encode(state: OrganizerState): String {
        require(state.schemaVersion == OrganizerState.CURRENT_SCHEMA_VERSION) {
            "Only organizer-state schema ${OrganizerState.CURRENT_SCHEMA_VERSION} can be written."
        }

        val overrides =
            state.categoryOverrides.entries
                .sortedBy { it.key.packageName }
                .associate { (appId, category) ->
                    appId.packageName to JsonPrimitive(category.name)
                }

        val root =
            JsonObject(
                linkedMapOf(
                    SCHEMA_VERSION to JsonPrimitive(state.schemaVersion),
                    CATEGORY_OVERRIDES to JsonObject(overrides),
                    FAVOURITE_APP_IDS to JsonArray(state.favouriteAppIds.toJsonAppIdList()),
                    HIDDEN_APP_IDS to JsonArray(state.hiddenAppIds.toJsonAppIdList())
                )
            )
        return root.toString()
    }

    fun decode(encoded: String): OrganizerState {
        require(encoded.isNotBlank()) { "Organizer state is empty." }
        val root = Json.parseToJsonElement(encoded) as? JsonObject
            ?: throw IllegalArgumentException("Organizer state root must be a JSON object.")

        val schemaPrimitive = root.requiredPrimitive(SCHEMA_VERSION)
        require(!schemaPrimitive.isString) { "Organizer-state schema version must be numeric." }
        val schemaVersion = schemaPrimitive.content.toIntOrNull()
            ?: throw IllegalArgumentException("Organizer-state schema version must be an integer.")
        require(schemaVersion == OrganizerState.CURRENT_SCHEMA_VERSION) {
            "Unsupported organizer-state schema version: $schemaVersion."
        }

        return OrganizerState(
            schemaVersion = schemaVersion,
            categoryOverrides = root.decodeOverrides(),
            favouriteAppIds = root.decodeAppIdSet(FAVOURITE_APP_IDS),
            hiddenAppIds = root.decodeAppIdSet(HIDDEN_APP_IDS)
        )
    }

    private fun JsonObject.decodeOverrides(): Map<AppId, AppCategory> {
        val overrides = this[CATEGORY_OVERRIDES] ?: return emptyMap()
        val objectValue = overrides as? JsonObject
            ?: throw IllegalArgumentException("$CATEGORY_OVERRIDES must be a JSON object.")

        return objectValue.entries.associate { (packageName, categoryElement) ->
            require(packageName.isNotBlank()) { "Organizer-state package names must not be blank." }
            val categoryPrimitive = categoryElement as? JsonPrimitive
                ?: throw IllegalArgumentException("Category override must be a string.")
            require(categoryPrimitive.isString) { "Category override must be a string." }
            AppId(packageName) to AppCategory.valueOf(categoryPrimitive.content)
        }
    }

    private fun JsonObject.decodeAppIdSet(key: String): Set<AppId> {
        val ids = this[key] ?: return emptySet()
        val array = ids as? JsonArray ?: throw IllegalArgumentException("$key must be a JSON array.")
        return array.mapTo(linkedSetOf()) { element ->
            val packagePrimitive = element as? JsonPrimitive
                ?: throw IllegalArgumentException("$key entries must be strings.")
            require(packagePrimitive.isString) { "$key entries must be strings." }
            require(packagePrimitive.content.isNotBlank()) { "Organizer-state package names must not be blank." }
            AppId(packagePrimitive.content)
        }
    }

    private fun JsonObject.requiredPrimitive(key: String): JsonPrimitive =
        this[key] as? JsonPrimitive ?: throw IllegalArgumentException("Missing or invalid $key.")

    private fun Set<AppId>.toJsonAppIdList(): List<JsonPrimitive> = asSequence()
        .map(AppId::packageName)
        .sorted()
        .map(::JsonPrimitive)
        .toList()

    private const val SCHEMA_VERSION = "schemaVersion"
    private const val CATEGORY_OVERRIDES = "categoryOverrides"
    private const val FAVOURITE_APP_IDS = "favouriteAppIds"
    private const val HIDDEN_APP_IDS = "hiddenAppIds"
}
