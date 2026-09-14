package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.metadata

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.SupportedMetadataLookupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class FdroidMetadataProviderTest {
    @Test
    fun parsesRequestedPackageCategoriesFromIndexV2() = runBlocking {
        val provider = providerWith(INDEX, now = 1234L)

        val result = provider.lookup(setOf(AppId("org.example.reader"), AppId("org.example.missing")))

        val success = assertIs<SupportedMetadataLookupResult.Success>(result)
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

        val success = assertIs<SupportedMetadataLookupResult.Success>(result)
        assertEquals(emptyMap(), success.metadataByAppId)
        assertEquals(setOf(AppId("org.example.nocategories")), success.missingAppIds)
    }

    @Test
    fun malformedRootIsFailure() {
        runBlocking {
            val provider = providerWith("{\"repo\":{}}")

            assertIs<SupportedMetadataLookupResult.Failure>(provider.lookup(setOf(AppId("org.example.reader"))))
        }
    }

    @Test
    fun malformedPackageDataDoesNotBreakOtherRequestedPackages() = runBlocking {
        val provider = providerWith(INDEX)

        val result = provider.lookup(setOf(AppId("org.example.reader"), AppId("org.example.bad")))

        val success = assertIs<SupportedMetadataLookupResult.Success>(result)
        assertEquals(setOf(AppId("org.example.reader")), success.metadataByAppId.keys)
        assertEquals(setOf(AppId("org.example.bad")), success.missingAppIds)
    }

    @Test
    fun networkFailureDoesNotLeakException() {
        runBlocking {
            val provider = FdroidMetadataProvider(FdroidIndexDocumentSource { error("offline") })

            assertIs<SupportedMetadataLookupResult.Failure>(provider.lookup(setOf(AppId("org.example.reader"))))
        }
    }

    private fun providerWith(document: String, now: Long = 99L) = FdroidMetadataProvider(
        documentSource = FdroidIndexDocumentSource { document },
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
