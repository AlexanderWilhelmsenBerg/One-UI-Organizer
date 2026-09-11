package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalAppSearchTest {
    @Test
    fun trimsQueryAndMatchesAppLabelWithoutCaseSensitivity() {
        val apps = listOf(
            categorizedApp("example.maps", "Åpne Kart", AppCategory.TRAVEL_NAVIGATION),
            categorizedApp("example.music", "Musikk", AppCategory.MUSIC_AUDIO)
        )

        val result = LocalAppSearch.filter(apps, "  åPNE  ")

        assertEquals(listOf("Åpne Kart"), result.map { categorizedApp -> categorizedApp.app.label })
    }

    @Test
    fun matchesInternationalAppLabelsUsingUnicodeCaseNormalization() {
        val apps = listOf(
            categorizedApp("example.reader", "École Reader", AppCategory.READING),
            categorizedApp("example.work", "Arbeid", AppCategory.WORK)
        )

        val result = LocalAppSearch.filter(apps, "éCOLE")

        assertEquals(listOf("École Reader"), result.map { categorizedApp -> categorizedApp.app.label })
    }

    @Test
    fun matchesCaseMappingsThatExpandDuringUnicodeNormalization() {
        val apps = listOf(
            categorizedApp("example.street", "Straße", AppCategory.TRAVEL_NAVIGATION),
            categorizedApp("example.work", "Arbeid", AppCategory.WORK)
        )

        val result = LocalAppSearch.filter(apps, "STRASSE")

        assertEquals(listOf("Straße"), result.map { categorizedApp -> categorizedApp.app.label })
    }

    @Test
    fun appLabelMatchSurfacesOnlyMatchingApps() {
        val apps = listOf(
            categorizedApp("example.home", "Home Assistant", AppCategory.SMART_HOME),
            categorizedApp("example.term", "Termux", AppCategory.DEVELOPMENT)
        )

        val result = LocalAppSearch.filter(apps, "term")

        assertEquals(listOf("Termux"), result.map { categorizedApp -> categorizedApp.app.label })
    }

    @Test
    fun categoryLabelMatchSurfacesAppsInThatCategory() {
        val apps = listOf(
            categorizedApp("example.maps", "Kart", AppCategory.TRAVEL_NAVIGATION),
            categorizedApp("example.music", "Musikk", AppCategory.MUSIC_AUDIO),
            categorizedApp("example.video", "Film", AppCategory.VIDEO)
        )

        val result = LocalAppSearch.filter(apps, "travel")

        assertEquals(listOf("Kart"), result.map { categorizedApp -> categorizedApp.app.label })
    }

    @Test
    fun emulatorCategoryLabelIsSearchable() {
        val apps = listOf(
            categorizedApp("example.dolphin", "Dolphin", AppCategory.EMULATORS),
            categorizedApp("example.game", "Game", AppCategory.GAMES)
        )

        val result = LocalAppSearch.filter(apps, "emulators")

        assertEquals(listOf("Dolphin"), result.map { categorizedApp -> categorizedApp.app.label })
    }

    @Test
    fun emptyQueryReturnsAllVisibleAppsInDeterministicOrder() {
        val apps = listOf(
            categorizedApp("example.zulu", "Zulu", AppCategory.TOOLS),
            categorizedApp("example.alpha", "Alpha", AppCategory.TOOLS)
        )

        val result = LocalAppSearch.filter(apps, "   ")

        assertEquals(listOf("Alpha", "Zulu"), result.map { categorizedApp -> categorizedApp.app.label })
    }

    @Test
    fun repeatedSearchCallsAreDeterministic() {
        val apps = listOf(
            categorizedApp("example.two", "Same", AppCategory.WORK, className = "example.two.Second"),
            categorizedApp("example.one", "Same", AppCategory.WORK, className = "example.one.First"),
            categorizedApp("example.two", "Same", AppCategory.WORK, className = "example.two.First")
        )

        val first = LocalAppSearch.filter(apps, "work")
        val second = LocalAppSearch.filter(apps.reversed(), "WORK")

        assertEquals(first, second)
        assertEquals(
            listOf(
                "example.one.First",
                "example.two.First",
                "example.two.Second"
            ),
            first.map { categorizedApp -> categorizedApp.app.launchTargetId.className }
        )
    }

    private fun categorizedApp(
        packageName: String,
        label: String,
        category: AppCategory,
        className: String = "$packageName.MainActivity"
    ): CategorizedApp = CategorizedApp(
        app = InstalledApp(
            id = AppId(packageName),
            launchTargetId = LaunchTargetId(packageName, className),
            label = label
        ),
        category = category,
        source = ClassificationSource.UNSORTED_FALLBACK
    )
}
