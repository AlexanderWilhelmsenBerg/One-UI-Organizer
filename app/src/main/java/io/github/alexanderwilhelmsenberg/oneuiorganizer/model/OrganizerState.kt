package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

data class OrganizerState(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val categoryOverrides: Map<AppId, AppCategory> = emptyMap(),
    val favouriteAppIds: Set<AppId> = emptySet(),
    val hiddenAppIds: Set<AppId> = emptySet()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
