package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.metadata

import android.util.JsonReader
import android.util.JsonToken
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.SupportedAppMetadataProvider
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.SupportedMetadataLookupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics.AppEventLog
import io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics.NoOpAppEventLog
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FdroidIndexDocumentSource {
    suspend fun <T> readIndexV2(block: (Reader) -> T): T
}

class HttpFdroidIndexDocumentSource(
    private val indexUrl: URL = URL(DEFAULT_INDEX_URL),
    private val maxResponseBytes: Long = DEFAULT_MAX_RESPONSE_BYTES
) : FdroidIndexDocumentSource {
    override suspend fun <T> readIndexV2(block: (Reader) -> T): T = withContext(Dispatchers.IO) {
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
            val contentLength = connection.contentLengthLong
            if (contentLength > maxResponseBytes) {
                throw IOException("F-Droid index exceeded the configured response limit")
            }
            connection.inputStream.use { input ->
                InputStreamReader(
                    LimitedInputStream(input, maxResponseBytes),
                    StandardCharsets.UTF_8
                ).use(block)
            }
        } finally {
            connection.disconnect()
        }
    }

    private class LimitedInputStream(
        input: InputStream,
        private val maximumBytes: Long
    ) : FilterInputStream(input) {
        private var bytesRead = 0L

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) {
                countBytes(1)
            }
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = super.read(buffer, offset, length)
            if (count > 0) {
                countBytes(count.toLong())
            }
            return count
        }

        private fun countBytes(count: Long) {
            bytesRead += count
            if (bytesRead > maximumBytes) {
                throw IOException("F-Droid index exceeded the configured response limit")
            }
        }
    }

    private companion object {
        const val DEFAULT_INDEX_URL = "https://f-droid.org/repo/index-v2.json"
        const val DEFAULT_MAX_RESPONSE_BYTES = 64L * 1024L * 1024L
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 60_000
        const val USER_AGENT = "OneUIOrganizer/0.1"
    }
}

class FdroidMetadataProvider(
    private val documentSource: FdroidIndexDocumentSource,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val eventLog: AppEventLog = NoOpAppEventLog
) : SupportedAppMetadataProvider {
    override val provider: SupportedMetadataProvider = SupportedMetadataProvider.F_DROID

    override suspend fun lookup(appIds: Set<AppId>): SupportedMetadataLookupResult {
        if (appIds.isEmpty()) {
            return SupportedMetadataLookupResult.Success(emptyMap(), emptySet())
        }

        eventLog.record("Supported metadata refresh started for ${appIds.size} installed apps")
        return try {
            val result = documentSource.readIndexV2 { reader -> parseLookup(reader, appIds) }
            if (result is SupportedMetadataLookupResult.Success) {
                eventLog.record(
                    "Supported metadata refresh completed: ${result.metadataByAppId.size} matched, " +
                        "${result.missingAppIds.size} unmatched"
                )
            } else {
                eventLog.record("Supported metadata refresh failed while parsing provider data")
            }
            result
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            eventLog.record("Supported metadata refresh failed", exception)
            SupportedMetadataLookupResult.Failure
        }
    }

    internal fun parseLookup(reader: Reader, appIds: Set<AppId>): SupportedMetadataLookupResult {
        val requestedByPackage = appIds.associateBy(AppId::packageName)
        val categoriesByAppId = linkedMapOf<AppId, Set<String>>()
        var packagesFound = false

        JsonReader(reader).use { jsonReader ->
            if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                return SupportedMetadataLookupResult.Failure
            }
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                when (jsonReader.nextName()) {
                    "packages" -> {
                        if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
                            jsonReader.skipValue()
                            return SupportedMetadataLookupResult.Failure
                        }
                        packagesFound = true
                        readRequestedPackages(jsonReader, requestedByPackage, categoriesByAppId)
                    }

                    else -> jsonReader.skipValue()
                }
            }
            jsonReader.endObject()
        }

        if (!packagesFound) {
            return SupportedMetadataLookupResult.Failure
        }

        val fetchedAt = nowEpochMillis()
        val metadataByAppId = linkedMapOf<AppId, SupportedAppMetadata>()
        val missingAppIds = linkedSetOf<AppId>()
        appIds.sortedBy(AppId::packageName).forEach { appId ->
            val categories = categoriesByAppId[appId].orEmpty()
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

    private fun readRequestedPackages(
        reader: JsonReader,
        requestedByPackage: Map<String, AppId>,
        categoriesByAppId: MutableMap<AppId, Set<String>>
    ) {
        reader.beginObject()
        while (reader.hasNext()) {
            val packageName = reader.nextName()
            val appId = requestedByPackage[packageName]
            if (appId == null) {
                reader.skipValue()
            } else {
                categoriesByAppId[appId] = readPackageCategories(reader)
            }
        }
        reader.endObject()
    }

    private fun readPackageCategories(reader: JsonReader): Set<String> {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return emptySet()
        }

        val categories = sortedSetOf<String>()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "metadata" -> readMetadataCategories(reader, categories)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return categories
    }

    private fun readMetadataCategories(reader: JsonReader, categories: MutableSet<String>) {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return
        }
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "categories" -> readCategories(reader, categories)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    private fun readCategories(reader: JsonReader, categories: MutableSet<String>) {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return
        }
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() == JsonToken.STRING) {
                reader.nextString().trim().takeIf(String::isNotEmpty)?.let(categories::add)
            } else {
                reader.skipValue()
            }
        }
        reader.endArray()
    }
}
