package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

data class OrganizerState(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val categoryOverrides: Map<AppId, CategoryId> = emptyMap(),
    val favouriteAppIds: Set<AppId> = emptySet(),
    val hiddenAppIds: Set<AppId> = emptySet(),
    val customCategories: List<CustomCategoryDefinition> = emptyList(),
    val categoryOrder: List<CategoryId> = defaultBuiltInCategoryOrder()
) {
    init {
        require(customCategories.map(CustomCategoryDefinition::id).distinct().size == customCategories.size) {
            "Custom category identities must be unique."
        }
    }

    fun categoryDefinition(id: CategoryId): CategoryDefinition? =
        AppCategory.fromId(id)?.toCategoryDefinition()
            ?: customCategories.firstOrNull { category -> category.id == id }?.toCategoryDefinition()

    fun normalizedCategoryOrder(): List<CategoryId> {
        val knownIds =
            buildSet {
                AppCategory.entries.forEach { category -> add(category.id) }
                customCategories.forEach { category -> add(category.id) }
            }
        val seen = mutableSetOf<CategoryId>()
        val normalized = mutableListOf<CategoryId>()

        categoryOrder.forEach { id ->
            if (id in knownIds && seen.add(id)) {
                normalized += id
            }
        }
        AppCategory.entries.forEach { category ->
            if (seen.add(category.id)) {
                normalized += category.id
            }
        }
        customCategories.forEach { category ->
            if (seen.add(category.id)) {
                normalized += category.id
            }
        }
        return normalized
    }

    fun orderedCategories(): List<CategoryDefinition> =
        normalizedCategoryOrder().mapNotNull(::categoryDefinition)

    fun normalized(): OrganizerState = copy(categoryOrder = normalizedCategoryOrder())

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2

        fun defaultBuiltInCategoryOrder(): List<CategoryId> = AppCategory.entries.map(AppCategory::id)
    }
}

private fun defaultBuiltInCategoryOrder(): List<CategoryId> = OrganizerState.defaultBuiltInCategoryOrder()
