package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import kotlinx.coroutines.flow.Flow

interface OrganizerRepository {
    val apps: Flow<List<CategorizedApp>>
    val organizerState: Flow<OrganizerState>

    suspend fun refresh()

    suspend fun setCategoryOverride(
        appId: AppId,
        category: AppCategory?,
    )

    suspend fun setFavourite(
        appId: AppId,
        isFavourite: Boolean,
    )

    suspend fun setHidden(
        appId: AppId,
        isHidden: Boolean,
    )
}
