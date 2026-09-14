package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider

interface SupportedAppMetadataProvider {
    val provider: SupportedMetadataProvider

    suspend fun lookup(appIds: Set<AppId>): SupportedMetadataLookupResult
}

sealed interface SupportedMetadataLookupResult {
    data class Success(val metadataByAppId: Map<AppId, SupportedAppMetadata>, val missingAppIds: Set<AppId>) :
        SupportedMetadataLookupResult

    data object Failure : SupportedMetadataLookupResult
}
