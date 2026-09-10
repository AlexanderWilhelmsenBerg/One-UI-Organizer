package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import kotlinx.coroutines.flow.Flow

interface OrganizerStateStore {
    val state: Flow<OrganizerState>

    suspend fun update(transform: (OrganizerState) -> OrganizerState): OrganizerState
}
