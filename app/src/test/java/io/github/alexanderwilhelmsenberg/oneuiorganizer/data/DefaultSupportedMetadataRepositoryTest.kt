package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DefaultSupportedMetadataRepositoryTest {
    @Test
    fun freshCacheCoveringRequestedPackagesSkipsProvider() = runBlocking {
        val appId = AppId("org.example.cached")
        val cached = snapshot(now = 1_000L, appId = appId, category = "Reading")
        val provider = FakeProvider(SupportedMetadataLookupResult.Failure)
        val repository = repository(provider, FakeCache(cached), now = 2_000L, interval = 10_000L)

        repository.refreshIfNeeded(setOf(appId))

        assertEquals(0, provider.calls)
        assertEquals(setOf(appId), repository.metadata.first().keys)
    }

    @Test
    fun staleCacheRemainsUsefulWhenRefreshFails() = runBlocking {
        val appId = AppId("org.example.stale")
        val cached = snapshot(now = 1_000L, appId = appId, category = "Reading")
        val provider = FakeProvider(SupportedMetadataLookupResult.Failure)
        val repository = repository(provider, FakeCache(cached), now = 20_000L, interval = 5_000L)

        repository.refreshIfNeeded(setOf(appId))

        assertEquals(1, provider.calls)
        assertEquals("Reading", repository.metadata.first().getValue(appId).categories.single())
    }

    @Test
    fun freshCacheRefreshesWhenNewPackageWasNeverChecked() = runBlocking {
        val cachedId = AppId("org.example.cached")
        val newId = AppId("org.example.new")
        val cached = snapshot(now = 1_000L, appId = cachedId, category = "Reading")
        val freshMetadata = metadata(newId, "Games", fetchedAt = 2_000L)
        val provider =
            FakeProvider(
                SupportedMetadataLookupResult.Success(
                    metadataByAppId = mapOf(newId to freshMetadata),
                    missingAppIds = setOf(cachedId)
                )
            )
        val cache = FakeCache(cached)
        val repository = repository(provider, cache, now = 2_000L, interval = 10_000L)

        repository.refreshIfNeeded(setOf(cachedId, newId))

        assertEquals(1, provider.calls)
        assertEquals(setOf(newId), repository.metadata.first().keys)
        assertEquals(setOf(cachedId, newId), cache.written?.checkedAppIds)
    }

    @Test
    fun successfulRefreshDeterministicallyReplacesOldDerivedMetadata() = runBlocking {
        val oldId = AppId("org.example.old")
        val currentId = AppId("org.example.current")
        val cached = snapshot(now = 1_000L, appId = oldId, category = "Reading")
        val fresh = metadata(currentId, "Games", fetchedAt = 20_000L)
        val provider =
            FakeProvider(
                SupportedMetadataLookupResult.Success(
                    metadataByAppId = mapOf(currentId to fresh),
                    missingAppIds = emptySet()
                )
            )
        val cache = FakeCache(cached)
        val repository = repository(provider, cache, now = 20_000L, interval = 5_000L)

        repository.refreshIfNeeded(setOf(currentId))

        assertEquals(mapOf(currentId to fresh), repository.metadata.first())
        assertEquals(mapOf(currentId to fresh), cache.written?.metadataByAppId)
    }

    private fun repository(
        provider: FakeProvider,
        cache: FakeCache,
        now: Long,
        interval: Long
    ) = DefaultSupportedMetadataRepository(
        provider = provider,
        cache = cache,
        nowEpochMillis = { now },
        refreshIntervalMillis = interval
    )

    private fun snapshot(now: Long, appId: AppId, category: String) =
        SupportedMetadataCacheSnapshot(
            provider = SupportedMetadataProvider.F_DROID,
            refreshedAtEpochMillis = now,
            checkedAppIds = setOf(appId),
            metadataByAppId = mapOf(appId to metadata(appId, category, now))
        )

    private fun metadata(appId: AppId, category: String, fetchedAt: Long) =
        SupportedAppMetadata(
            appId = appId,
            provider = SupportedMetadataProvider.F_DROID,
            categories = setOf(category),
            fetchedAtEpochMillis = fetchedAt
        )

    private class FakeProvider(
        private val result: SupportedMetadataLookupResult
    ) : SupportedAppMetadataProvider {
        override val provider = SupportedMetadataProvider.F_DROID
        var calls = 0

        override suspend fun lookup(appIds: Set<AppId>): SupportedMetadataLookupResult {
            calls += 1
            return result
        }
    }

    private class FakeCache(
        private val initial: SupportedMetadataCacheSnapshot?
    ) : SupportedMetadataCache {
        var written: SupportedMetadataCacheSnapshot? = null

        override suspend fun read(): SupportedMetadataCacheSnapshot? = initial

        override suspend fun write(snapshot: SupportedMetadataCacheSnapshot): Boolean {
            written = snapshot
            return true
        }
    }
}
