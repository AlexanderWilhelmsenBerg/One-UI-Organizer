package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassificationReportFormatterTest {
    @Test
    fun reportOrderingAndAggregateCountsAreDeterministic() {
        val apps =
            listOf(
                categorizedApp(
                    packageName = "z.example",
                    className = "z.example.MainActivity",
                    label = "Zulu",
                    platformCategory = PlatformAppCategory.GAME,
                    category = AppCategory.GAMES,
                    source = ClassificationSource.ANDROID_DECLARED_CATEGORY
                ),
                categorizedApp(
                    packageName = "a.example",
                    className = "a.example.SecondActivity",
                    label = "Second",
                    platformCategory = PlatformAppCategory.UNDEFINED,
                    category = AppCategory.UNSORTED,
                    source = ClassificationSource.UNSORTED_FALLBACK
                ),
                categorizedApp(
                    packageName = "a.example",
                    className = "a.example.MainActivity",
                    label = "Main",
                    platformCategory = PlatformAppCategory.PRODUCTIVITY,
                    category = AppCategory.WORK,
                    source = ClassificationSource.USER_OVERRIDE
                )
            )

        val first = ClassificationReportFormatter.format(apps)
        val second = ClassificationReportFormatter.format(apps.reversed())

        assertEquals(first, second)
        assertTrue(first.contains("targetCount=3"))
        assertTrue(first.contains("GAMES\t1"))
        assertTrue(first.contains("UNSORTED\t1"))
        assertTrue(first.contains("USER_OVERRIDE\t1"))
        assertTrue(first.contains("ANDROID_DECLARED_CATEGORY\t1"))
        assertTrue(first.contains("UNSORTED_FALLBACK\t1"))

        val targetRows = first.substringAfter("[targets]\n").lines().filter(String::isNotBlank).drop(1)
        assertEquals(
            listOf(
                "a.example.MainActivity",
                "a.example.SecondActivity",
                "z.example.MainActivity"
            ),
            targetRows.map { row -> row.split('\t')[2] }
        )
    }

    @Test
    fun reportContainsRequiredFieldsAndSanitizesControlCharacters() {
        val app =
            categorizedApp(
                packageName = "example.app",
                className = "example.app.MainActivity",
                label = "Example\tApp\nName",
                platformCategory = PlatformAppCategory.SOCIAL,
                category = AppCategory.SOCIAL,
                source = ClassificationSource.KNOWN_APP_RULE
            )

        val report = ClassificationReportFormatter.format(listOf(app))

        assertTrue(
            report.contains(
                "label\tpackageName\tclassName\tplatformCategory\torganizerCategory\tclassificationSource"
            )
        )
        assertTrue(
            report.contains(
                "Example App Name\texample.app\texample.app.MainActivity\tSOCIAL\tSOCIAL\tKNOWN_APP_RULE"
            )
        )
    }

    @Test
    fun reportIncludesZeroCountsForEveryKnownCategoryAndSource() {
        val report = ClassificationReportFormatter.format(emptyList())

        AppCategory.entries.forEach { category ->
            assertTrue(report.contains("${category.name}\t0"))
        }
        ClassificationSource.entries.forEach { source ->
            assertTrue(report.contains("${source.name}\t0"))
        }
    }

    private fun categorizedApp(
        packageName: String,
        className: String,
        label: String,
        platformCategory: PlatformAppCategory,
        category: AppCategory,
        source: ClassificationSource
    ): CategorizedApp =
        CategorizedApp(
            app =
                InstalledApp(
                    id = AppId(packageName),
                    launchTargetId = LaunchTargetId(packageName, className),
                    label = label,
                    platformCategory = platformCategory
                ),
            category = category,
            source = source
        )
}
