package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

enum class CategoryKind {
    BUILT_IN,
    CUSTOM
}

sealed interface CategoryDefinition {
    val id: CategoryId
    val displayName: String
    val kind: CategoryKind
}

data class CustomCategoryDefinition(override val id: CategoryId, override val displayName: String) :
    CategoryDefinition {
    init {
        require(id.isCustom) { "Custom category identities must use the custom namespace." }
        require(displayName.isNotBlank()) { "Custom category name must not be blank." }
    }

    override val kind: CategoryKind = CategoryKind.CUSTOM
}
