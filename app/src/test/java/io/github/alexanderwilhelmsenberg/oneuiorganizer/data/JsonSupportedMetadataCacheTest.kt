package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class JsonSupportedMetadataCacheTest {
    @Test
    fun roundTripsDerivedMetadataCache() = runBlocking {
        val directory = Files.createTempDirectory("supported-metadata-cache").toFile()
        val cache = JsonSupportedMetadataCache(directory.resolve("cache.json"))
        val snapshot = snapshot()

        assertTrue(cache.write(snapshot))

        assertEquals(snapshot, cache.read())
    }

    @Test
    fun malformedCacheIsIgnored() = runBlocking {
        val directory = Files.createTempDirectory("supported-metadata-cache-bad").toFile()
        val file = directory.resolve("cache.json").apply { writeText("{not-json") }
        val cache = JsonSupportedMetadataCache(file)

        assertNull(cache.read())
    }

    @Test
    fun encodingIsDeterministicRegardlessOfInputOrder() {
        val directory = Files.createTempDirectory("supported-metadata-cache-order").toFile()
        val cache = JsonSupportedMetadataCache(directory.resolve("cache.json"))
        val first = snapshot()
        val second =
            first.copy(
                checkedAppIds = first.checkedAppIds.reversed().toSet(),
                metadataByAppId = first.metadataByAppId.entries.reversed().associate { it.toPair() }
            )

        assertEquals(cache.encode(first), cache.encode(second))
    }

    private fun snapshot(): SupportedMetadataCacheSnapshot {
        val a = AppId("org.example.a")
        val b = AppId("org.example.b")
        return SupportedMetadataCacheSnapshot(
            provider = SupportedMetadataProvider.F_DROID,
            refreshedAtEpochMillis = 5_000L,
            checkedAppIds = linkedSetOf(b, a),
            metadataByAppId =
                linkedMapOf(
                    b to metadata(b, setOf("News", "Reading"), 4_000L),
                    a to metadata(a, setOf("Games"), 4_000L)
                )
        )
    }

    private fun metadata(appId: AppId, categories: Set<String>, fetchedAt: Long) =
        SupportedAppMetadata(
            appId = appId,
            provider = SupportedMetadataProvider.F_DROID,
            categories = categories,
            fetchedAtEpochMillis = fetchedAt
        )
}
