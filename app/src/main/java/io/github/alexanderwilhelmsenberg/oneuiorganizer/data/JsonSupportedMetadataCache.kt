package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

class JsonSupportedMetadataCache(private val file: File) : SupportedMetadataCache {
    override suspend fun read(): SupportedMetadataCacheSnapshot? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        try {
            decode(file.readText(StandardCharsets.UTF_8))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun write(snapshot: SupportedMetadataCacheSnapshot): Boolean = withContext(Dispatchers.IO) {
        try {
            file.parentFile?.mkdirs()
            val tempFile = File(file.parentFile, "${file.name}.tmp")
            tempFile.writeText(encode(snapshot), StandardCharsets.UTF_8)
            try {
                Files.move(
                    tempFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: Exception) {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }
    }

    internal fun encode(snapshot: SupportedMetadataCacheSnapshot): String {
        val metadataEntries =
            snapshot.metadataByAppId.values
                .sortedBy { metadata -> metadata.appId.packageName }
                .map { metadata ->
                    JsonObject(
                        mapOf(
                            "packageName" to JsonPrimitive(metadata.appId.packageName),
                            "categories" to JsonArray(metadata.categories.sorted().map(::JsonPrimitive)),
                            "fetchedAtEpochMillis" to JsonPrimitive(metadata.fetchedAtEpochMillis)
                        )
                    )
                }
        val root =
            JsonObject(
                mapOf(
                    "formatVersion" to JsonPrimitive(FORMAT_VERSION),
                    "provider" to JsonPrimitive(snapshot.provider.name),
                    "refreshedAtEpochMillis" to JsonPrimitive(snapshot.refreshedAtEpochMillis),
                    "checkedPackages" to
                        JsonArray(snapshot.checkedAppIds.map { it.packageName }.sorted().map(::JsonPrimitive)),
                    "metadata" to JsonArray(metadataEntries)
                )
            )
        return root.toString()
    }

    internal fun decode(document: String): SupportedMetadataCacheSnapshot? {
        val root = Json.parseToJsonElement(document) as? JsonObject ?: return null
        val formatVersion = root.long("formatVersion") ?: return null
        if (formatVersion != FORMAT_VERSION.toLong()) return null

        val providerName = root.string("provider") ?: return null
        val provider = SupportedMetadataProvider.entries.firstOrNull { it.name == providerName } ?: return null
        val refreshedAtEpochMillis = root.long("refreshedAtEpochMillis") ?: return null
        if (refreshedAtEpochMillis < 0L) return null

        val checkedPackages = root["checkedPackages"] as? JsonArray ?: return null
        val checkedAppIds =
            checkedPackages.mapNotNull(::stringValue)
                .filter(::isValidPackageName)
                .map(::AppId)
                .toSet()
        if (checkedAppIds.size != checkedPackages.size) return null

        val metadataArray = root["metadata"] as? JsonArray ?: return null
        val metadataByAppId = linkedMapOf<AppId, SupportedAppMetadata>()
        metadataArray.forEach { element ->
            val metadataObject = element as? JsonObject ?: return null
            val packageName = metadataObject.string("packageName") ?: return null
            if (!isValidPackageName(packageName)) return null
            val appId = AppId(packageName)
            if (appId !in checkedAppIds || appId in metadataByAppId) return null

            val fetchedAtEpochMillis = metadataObject.long("fetchedAtEpochMillis") ?: return null
            if (fetchedAtEpochMillis < 0L) return null
            val categoriesArray = metadataObject["categories"] as? JsonArray ?: return null
            val categories =
                categoriesArray.mapNotNull(::stringValue)
                    .map(String::trim)
                    .filter(String::isNotEmpty)
                    .toSortedSet()
            if (categories.isEmpty() || categories.size != categoriesArray.size) return null

            metadataByAppId[appId] =
                SupportedAppMetadata(
                    appId = appId,
                    provider = provider,
                    categories = categories,
                    fetchedAtEpochMillis = fetchedAtEpochMillis
                )
        }

        return SupportedMetadataCacheSnapshot(
            provider = provider,
            refreshedAtEpochMillis = refreshedAtEpochMillis,
            checkedAppIds = checkedAppIds,
            metadataByAppId = metadataByAppId
        )
    }

    private fun JsonObject.string(name: String): String? = stringValue(this[name])

    private fun JsonObject.long(name: String): Long? = (this[name] as? JsonPrimitive)?.longOrNull

    private fun stringValue(element: JsonElement?): String? =
        (element as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun isValidPackageName(packageName: String): Boolean =
        packageName.isNotBlank() && packageName.length <= MAX_PACKAGE_NAME_LENGTH

    private companion object {
        const val FORMAT_VERSION = 1
        const val MAX_PACKAGE_NAME_LENGTH = 255
    }
}
