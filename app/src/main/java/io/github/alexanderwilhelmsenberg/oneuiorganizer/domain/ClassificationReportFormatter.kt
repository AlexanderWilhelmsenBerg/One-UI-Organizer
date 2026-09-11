package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource

object ClassificationReportFormatter {
    fun format(apps: List<CategorizedApp>): String {
        val categoryCounts = apps.groupingBy(CategorizedApp::category).eachCount()
        val sourceCounts = apps.groupingBy(CategorizedApp::source).eachCount()
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
                appendLine("${category.name}\t${categoryCounts[category] ?: 0}")
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
                        categorizedApp.category.name,
                        categorizedApp.source.name
                    ).joinToString(separator = "\t")
                )
            }
        }
    }

    private fun sanitizeField(value: String): String =
        value
            .replace('\t', ' ')
            .replace('\r', ' ')
            .replace('\n', ' ')
}
