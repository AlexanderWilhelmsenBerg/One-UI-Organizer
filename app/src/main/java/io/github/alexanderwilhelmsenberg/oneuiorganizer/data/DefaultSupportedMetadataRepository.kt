package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultSupportedMetadataRepository(
    private val provider: SupportedAppMetadataProvider,
    private val cache: SupportedMetadataCache,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
    private val refreshIntervalMillis: Long = DEFAULT_REFRESH_INTERVAL_MILLIS
) : SupportedMetadataRepository {
    private val mutex = Mutex()
    private val currentMetadata = MutableStateFlow<Map<AppId, SupportedAppMetadata>>(emptyMap())
    private var cacheLoaded = false
    private var snapshot: SupportedMetadataCacheSnapshot? = null

    override val metadata: Flow<Map<AppId, SupportedAppMetadata>> = currentMetadata.asStateFlow()

    override suspend fun refreshIfNeeded(appIds: Set<AppId>) {
        if (appIds.isEmpty()) return

        mutex.withLock {
            loadCacheIfNeeded()
            val currentSnapshot = snapshot
            if (currentSnapshot != null && currentSnapshot.isFreshFor(appIds)) return

            when (val result = provider.lookup(appIds)) {
                SupportedMetadataLookupResult.Failure -> Unit
                is SupportedMetadataLookupResult.Success -> applySuccessfulRefresh(appIds, result)
            }
        }
    }

    private suspend fun loadCacheIfNeeded() {
        if (cacheLoaded) return
        cacheLoaded = true

        val cached = try {
            cache.read()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }

        if (cached?.provider == provider.provider) {
            snapshot = cached
            currentMetadata.value = cached.metadataByAppId
        }
    }

    private suspend fun applySuccessfulRefresh(
        requestedAppIds: Set<AppId>,
        result: SupportedMetadataLookupResult.Success
    ) {
        val refreshedMetadata =
            result.metadataByAppId
                .filterKeys { appId -> appId in requestedAppIds }
                .filterValues { metadata -> metadata.provider == provider.provider }
                .toSortedMap(compareBy(AppId::packageName))

        val refreshed =
            SupportedMetadataCacheSnapshot(
                provider = provider.provider,
                refreshedAtEpochMillis = nowEpochMillis(),
                checkedAppIds = requestedAppIds.toSortedSet(compareBy(AppId::packageName)),
                metadataByAppId = refreshedMetadata
            )

        snapshot = refreshed
        currentMetadata.value = refreshedMetadata

        try {
            cache.write(refreshed)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // The in-memory refresh remains useful even when derived cache persistence fails.
        }
    }

    private fun SupportedMetadataCacheSnapshot.isFreshFor(appIds: Set<AppId>): Boolean {
        if (!checkedAppIds.containsAll(appIds)) return false
        val ageMillis = nowEpochMillis() - refreshedAtEpochMillis
        return ageMillis in 0 until refreshIntervalMillis
    }

    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MILLIS: Long = 14L * 24L * 60L * 60L * 1_000L
    }
}
