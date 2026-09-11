package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp

object LocalAppSearch {
    fun filter(apps: List<CategorizedApp>, query: String): List<CategorizedApp> {
        val normalizedQuery = normalize(query)

        return apps
            .asSequence()
            .filter { categorizedApp ->
                normalizedQuery.isEmpty() ||
                    normalize(categorizedApp.app.label).contains(normalizedQuery) ||
                    normalize(categorizedApp.category.displayName).contains(normalizedQuery)
            }
            .sortedWith(
                compareBy<CategorizedApp>(
                    { categorizedApp -> normalize(categorizedApp.app.label) },
                    { categorizedApp -> categorizedApp.app.id.packageName },
                    { categorizedApp -> categorizedApp.app.launchTargetId.className }
                )
            )
            .toList()
    }

    private fun normalize(value: String): String = value.trim().uppercase().lowercase()
}

fun AppCategory.searchLabel(): String = displayName
