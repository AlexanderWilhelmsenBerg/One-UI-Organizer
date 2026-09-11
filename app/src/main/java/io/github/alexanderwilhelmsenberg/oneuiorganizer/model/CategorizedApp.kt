package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

data class CategorizedApp(
    val app: InstalledApp,
    val category: CategoryDefinition,
    val source: ClassificationSource
)
