package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource

object ClassificationReportFormatter {
    fun format(apps: List<CategorizedApp>): String {
        val categoryCounts = apps.groupingBy { categorizedApp -> categorizedApp.category.id }.eachCount()
        val sourceCounts = apps.groupingBy(CategorizedApp::source).eachCount()
        val customCategories =
            apps.asSequence()
                .map(CategorizedApp::category)
                .filterNot { category -> category is AppCategory }
                .distinctBy(CategoryDefinition::id)
                .sortedBy { category -> category.id.value }
                .toList()
        val orderedApps =
            apps.sortedWith(
                compareBy<CategorizedApp>(
                    { categorizedApp -> categorizedApp.app.id.packageName },
                    { categorizedApp -> categorizedApp.app.launchTargetId.className },
                    { categorizedApp -> categorizedApp.app.label }
                )
            )

        return buildString {
            appendLine("One UI Organizer classification report")
            appendLine("formatVersion=1")
            appendLine("targetCount=${apps.size}")
            appendLine()
            appendLine("[organizerCategoryCounts]")
            AppCategory.entries.forEach { category ->
                appendLine("${category.name}\t${categoryCounts[category.id] ?: 0}")
            }
            customCategories.forEach { category ->
                appendLine("${category.reportLabel()}\t${categoryCounts[category.id] ?: 0}")
            }
            appendLine()
            appendLine("[classificationSourceCounts]")
            ClassificationSource.entries.forEach { source ->
                appendLine("${source.name}\t${sourceCounts[source] ?: 0}")
            }
            appendLine()
            appendLine("[targets]")
            appendLine(
                "label\tpackageName\tclassName\tplatformCategory\torganizerCategory\tclassificationSource"
            )
            orderedApps.forEach { categorizedApp ->
                val app = categorizedApp.app
                appendLine(
                    listOf(
                        sanitizeField(app.label),
                        sanitizeField(app.id.packageName),
                        sanitizeField(app.launchTargetId.className),
                        app.platformCategory.name,
                        categorizedApp.category.reportLabel(),
                        categorizedApp.source.name
                    ).joinToString(separator = "\t")
                )
            }
        }
    }

    private fun CategoryDefinition.reportLabel(): String = if (this is AppCategory) {
        name
    } else {
        "${id.value} (${sanitizeField(displayName)})"
    }

    private fun sanitizeField(value: String): String = value
        .replace('\t', ' ')
        .replace('\r', ' ')
        .replace('\n', ' ')
}
