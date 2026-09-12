package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId

/** App-owned boundary for launcher shortcut publication and user-requested pinning. */
interface CategoryShortcutManager {
    val isPinningSupported: Boolean

    suspend fun synchronize(
        orderedCategories: List<CategoryDefinition>,
        currentCategoryAppCounts: Map<CategoryId, Int>
    )

    fun requestPinShortcut(category: CategoryDefinition): Boolean

    fun reportShortcutUsed(categoryId: CategoryId)
}
