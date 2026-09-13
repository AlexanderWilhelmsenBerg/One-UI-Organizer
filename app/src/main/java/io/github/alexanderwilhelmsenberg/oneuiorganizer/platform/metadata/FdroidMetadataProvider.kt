package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.metadata

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.SupportedAppMetadataProvider
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.SupportedMetadataLookupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun interface FdroidIndexDocumentSource {
    suspend fun loadIndexV2(): String
}

class HttpFdroidIndexDocumentSource(
    private val indexUrl: URL = URL(DEFAULT_INDEX_URL),
    private val maxResponseBytes: Int = DEFAULT_MAX_RESPONSE_BYTES
) : FdroidIndexDocumentSource {
    override suspend fun loadIndexV2(): String = withContext(Dispatchers.IO) {
        val connection = (indexUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", USER_AGENT)
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("F-Droid index request failed with HTTP $responseCode")
            }
            connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER_SIZE_BYTES)
                var totalBytes = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    totalBytes += count
                    if (totalBytes > maxResponseBytes) {
                        throw IOException("F-Droid index exceeded the configured response limit")
                    }
                    output.write(buffer, 0, count)
                }
                output.toString(StandardCharsets.UTF_8.name())
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val DEFAULT_INDEX_URL = "https://f-droid.org/repo/index-v2.json"
        const val DEFAULT_MAX_RESPONSE_BYTES = 64 * 1024 * 1024
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 60_000
        const val BUFFER_SIZE_BYTES = 16 * 1024
        const val USER_AGENT = "OneUIOrganizer/0.1"
    }
}

class FdroidMetadataProvider(
    private val documentSource: FdroidIndexDocumentSource,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis
) : SupportedAppMetadataProvider {
    override val provider: SupportedMetadataProvider = SupportedMetadataProvider.F_DROID

    override suspend fun lookup(appIds: Set<AppId>): SupportedMetadataLookupResult {
        if (appIds.isEmpty()) {
            return SupportedMetadataLookupResult.Success(emptyMap(), emptySet())
        }

        return try {
            parseLookup(documentSource.loadIndexV2(), appIds)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            SupportedMetadataLookupResult.Failure
        }
    }

    internal fun parseLookup(document: String, appIds: Set<AppId>): SupportedMetadataLookupResult {
        val root = Json.parseToJsonElement(document) as? JsonObject
            ?: return SupportedMetadataLookupResult.Failure
        val packages = root["packages"] as? JsonObject
            ?: return SupportedMetadataLookupResult.Failure
        val fetchedAt = nowEpochMillis()
        val metadataByAppId = linkedMapOf<AppId, SupportedAppMetadata>()
        val missingAppIds = linkedSetOf<AppId>()

        appIds.sortedBy(AppId::packageName).forEach { appId ->
            val packageObject = packages[appId.packageName] as? JsonObject
            val metadataObject = packageObject?.get("metadata") as? JsonObject
            val categoriesArray = metadataObject?.get("categories") as? JsonArray
            val categories =
                categoriesArray
                    ?.mapNotNull { element ->
                        val primitive = element as? JsonPrimitive
                        primitive
                            ?.takeIf { it.isString }
                            ?.content
                            ?.trim()
                            ?.takeIf(String::isNotEmpty)
                    }
                    ?.toSortedSet()
                    .orEmpty()

            if (categories.isEmpty()) {
                missingAppIds += appId
            } else {
                metadataByAppId[appId] =
                    SupportedAppMetadata(
                        appId = appId,
                        provider = provider,
                        categories = categories,
                        fetchedAtEpochMillis = fetchedAt
                    )
            }
        }

        return SupportedMetadataLookupResult.Success(
            metadataByAppId = metadataByAppId,
            missingAppIds = missingAppIds
        )
    }
}
