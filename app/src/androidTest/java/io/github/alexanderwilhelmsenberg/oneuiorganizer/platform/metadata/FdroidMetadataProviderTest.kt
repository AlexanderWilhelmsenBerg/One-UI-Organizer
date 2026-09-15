package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.metadata

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.SupportedMetadataLookupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import java.io.Reader
import java.io.StringReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FdroidMetadataProviderTest {
    @Test
    fun parsesRequestedPackageCategoriesFromIndexV2() = runBlocking {
        val provider = providerWith(INDEX, now = 1234L)

        val result = provider.lookup(setOf(AppId("org.example.reader"), AppId("org.example.missing")))

        val success = result.requireSuccess()
        val metadata = success.metadataByAppId.getValue(AppId("org.example.reader"))
        assertEquals(SupportedMetadataProvider.F_DROID, metadata.provider)
        assertEquals(setOf("Reading", "News"), metadata.categories)
        assertEquals(1234L, metadata.fetchedAtEpochMillis)
        assertEquals(setOf(AppId("org.example.missing")), success.missingAppIds)
    }

    @Test
    fun missingCategoriesAreRepresentedAsMiss() = runBlocking {
        val provider = providerWith(INDEX)

        val result = provider.lookup(setOf(AppId("org.example.nocategories")))

        val success = result.requireSuccess()
        assertTrue(success.metadataByAppId.isEmpty())
        assertEquals(setOf(AppId("org.example.nocategories")), success.missingAppIds)
    }

    @Test
    fun malformedRootIsFailure() = runBlocking {
        val provider = providerWith("{\"repo\":{}}")

        assertTrue(
            provider.lookup(setOf(AppId("org.example.reader"))) is SupportedMetadataLookupResult.Failure
        )
    }

    @Test
    fun malformedPackageDataDoesNotBreakOtherRequestedPackages() = runBlocking {
        val provider = providerWith(INDEX)

        val result = provider.lookup(setOf(AppId("org.example.reader"), AppId("org.example.bad")))

        val success = result.requireSuccess()
        assertEquals(setOf(AppId("org.example.reader")), success.metadataByAppId.keys)
        assertEquals(setOf(AppId("org.example.bad")), success.missingAppIds)
    }

    @Test
    fun networkFailureDoesNotLeakException() = runBlocking {
        val provider =
            FdroidMetadataProvider(
                object : FdroidIndexDocumentSource {
                    override suspend fun <T> readIndexV2(block: (Reader) -> T): T = error("offline")
                }
            )

        assertTrue(
            provider.lookup(setOf(AppId("org.example.reader"))) is SupportedMetadataLookupResult.Failure
        )
    }

    private fun SupportedMetadataLookupResult.requireSuccess(): SupportedMetadataLookupResult.Success {
        assertTrue(this is SupportedMetadataLookupResult.Success)
        return this as SupportedMetadataLookupResult.Success
    }

    private fun providerWith(document: String, now: Long = 99L) =
        FdroidMetadataProvider(
            documentSource =
                object : FdroidIndexDocumentSource {
                    override suspend fun <T> readIndexV2(block: (Reader) -> T): T =
                        StringReader(document).use(block)
                },
            nowEpochMillis = { now }
        )

    private companion object {
        val INDEX =
            """
            {
              "packages": {
                "org.example.reader": {
                  "metadata": {"categories": ["Reading", "News"]},
                  "versions": {}
                },
                "org.example.nocategories": {
                  "metadata": {},
                  "versions": {}
                },
                "org.example.bad": {
                  "metadata": {"categories": [3, null]},
                  "versions": {}
                },
                "org.example.unrequested": {
                  "metadata": {"categories": ["Games"]},
                  "versions": {}
                }
              }
            }
            """.trimIndent()
    }
}
