package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SupportedMetadataRepository {
    val metadata: Flow<Map<AppId, SupportedAppMetadata>>

    suspend fun refreshIfNeeded(appIds: Set<AppId>)
}

object EmptySupportedMetadataRepository : SupportedMetadataRepository {
    override val metadata: Flow<Map<AppId, SupportedAppMetadata>> = flowOf(emptyMap())

    override suspend fun refreshIfNeeded(appIds: Set<AppId>) = Unit
}
