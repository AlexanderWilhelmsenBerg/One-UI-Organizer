package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryShortcutDestination

/** Stable platform shortcut identity derived only from the app-owned durable category identity. */
object CategoryShortcutIdentity {
    const val ID_PREFIX = "category:"

    fun shortcutId(categoryId: CategoryId): String = "$ID_PREFIX${categoryId.value}"

    fun isCategoryShortcutId(shortcutId: String): Boolean = shortcutId.startsWith(ID_PREFIX)
}

data class CategoryShortcutDescriptor(
    val shortcutId: String,
    val label: String,
    val destination: CategoryShortcutDestination
) {
    companion object {
        fun from(category: CategoryDefinition): CategoryShortcutDescriptor = CategoryShortcutDescriptor(
            shortcutId = CategoryShortcutIdentity.shortcutId(category.id),
            label = category.displayName,
            destination = CategoryShortcutDestination(category.id)
        )
    }
}

/**
 * Chooses a small useful subset of current normal categories for dynamic launcher shortcuts.
 *
 * Persisted category order wins, empty current categories are skipped, and the result is capped by
 * both Android's device-reported maximum and the launcher-menu size recommended by Android.
 */
object DynamicCategoryShortcutPolicy {
    const val RECOMMENDED_LAUNCHER_MENU_LIMIT = 4

    fun select(
        orderedCategories: List<CategoryDefinition>,
        currentCategoryAppCounts: Map<CategoryId, Int>,
        platformMaximum: Int
    ): List<CategoryDefinition> {
        if (platformMaximum <= 0) {
            return emptyList()
        }

        val limit = minOf(platformMaximum, RECOMMENDED_LAUNCHER_MENU_LIMIT)
        return orderedCategories
            .asSequence()
            .filter { category -> (currentCategoryAppCounts[category.id] ?: 0) > 0 }
            .take(limit)
            .toList()
    }
}
