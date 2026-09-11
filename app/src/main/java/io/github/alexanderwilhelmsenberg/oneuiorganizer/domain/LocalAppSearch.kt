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
                    normalize(categorizedApp.category.searchLabel()).contains(normalizedQuery)
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

fun AppCategory.searchLabel(): String = when (this) {
    AppCategory.COMMUNICATION -> "Communication"
    AppCategory.SOCIAL -> "Social"
    AppCategory.WORK -> "Work"
    AppCategory.PRODUCTIVITY -> "Productivity"
    AppCategory.SMART_HOME -> "Smart Home"
    AppCategory.HOMELAB -> "Homelab"
    AppCategory.FINANCE -> "Finance"
    AppCategory.SHOPPING -> "Shopping"
    AppCategory.TRAVEL_NAVIGATION -> "Travel & Navigation"
    AppCategory.MUSIC_AUDIO -> "Music & Audio"
    AppCategory.VIDEO -> "Video"
    AppCategory.PHOTOS -> "Photos"
    AppCategory.READING -> "Reading"
    AppCategory.DEVELOPMENT -> "Development"
    AppCategory.TOOLS -> "Tools"
    AppCategory.GAMES -> "Games"
    AppCategory.OTHER -> "Other"
    AppCategory.UNSORTED -> "Unsorted"
}
