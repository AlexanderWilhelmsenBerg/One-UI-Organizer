package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

enum class CategoryKind {
    BUILT_IN,
    CUSTOM
}

data class CategoryDefinition(
    val id: CategoryId,
    val name: String,
    val kind: CategoryKind
) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank." }
    }
}

data class CustomCategoryDefinition(
    val id: CategoryId,
    val name: String
) {
    init {
        require(id.isCustom) { "Custom category identities must use the custom namespace." }
        require(name.isNotBlank()) { "Custom category name must not be blank." }
    }

    fun toCategoryDefinition(): CategoryDefinition =
        CategoryDefinition(
            id = id,
            name = name,
            kind = CategoryKind.CUSTOM
        )
}

fun AppCategory.toCategoryDefinition(): CategoryDefinition =
    CategoryDefinition(
        id = id,
        name = displayName,
        kind = CategoryKind.BUILT_IN
    )
