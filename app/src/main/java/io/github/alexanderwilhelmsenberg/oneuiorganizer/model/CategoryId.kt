package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

@JvmInline
value class CategoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "Category identity must not be blank." }
    }

    val isBuiltIn: Boolean
        get() = value.startsWith(BUILT_IN_PREFIX)

    val isCustom: Boolean
        get() = value.startsWith(CUSTOM_PREFIX)

    companion object {
        private const val BUILT_IN_PREFIX = "builtin:"
        private const val CUSTOM_PREFIX = "custom:"

        fun custom(opaqueId: String): CategoryId {
            require(opaqueId.isNotBlank()) { "Custom category identity must not be blank." }
            return CategoryId("$CUSTOM_PREFIX$opaqueId")
        }
    }
}
