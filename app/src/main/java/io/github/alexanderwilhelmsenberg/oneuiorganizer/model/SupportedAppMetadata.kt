package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

enum class SupportedMetadataProvider {
    F_DROID
}

data class SupportedAppMetadata(
    val appId: AppId,
    val provider: SupportedMetadataProvider,
    val categories: Set<String>,
    val fetchedAtEpochMillis: Long
)
