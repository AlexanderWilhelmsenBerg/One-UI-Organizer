package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider

data class SupportedMetadataCacheSnapshot(
    val provider: SupportedMetadataProvider,
    val refreshedAtEpochMillis: Long,
    val checkedAppIds: Set<AppId>,
    val metadataByAppId: Map<AppId, SupportedAppMetadata>
)

interface SupportedMetadataCache {
    suspend fun read(): SupportedMetadataCacheSnapshot?

    suspend fun write(snapshot: SupportedMetadataCacheSnapshot): Boolean
}
