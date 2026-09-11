package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.BundledKnownAppRules
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassificationQualityIntegrationTest {
    @Test
    fun `merged rule packs classify representative general game and web entries`() {
        val expected =
            mapOf(
                "io.homeassistant.companion.android" to AppCategory.SMART_HOME,
                "com.rockstargames.bully" to AppCategory.GAME_ACTION_ADVENTURE,
                "com.aspyr.swkotor" to AppCategory.GAME_RPG,
                "com.unciv.app" to AppCategory.GAME_STRATEGY_SIMULATION,
                "com.playdead.limbo.full" to AppCategory.GAME_PUZZLE_CASUAL,
                "com.playstack.balatro.android" to AppCategory.GAME_BOARD_CARD,
                "org.chromium.webapk.integration_fixture" to AppCategory.WEB_SHORTCUTS
            )

        expected.forEach { (packageName, category) ->
            assertEquals(category, BundledKnownAppRules.categoryFor(installedApp(packageName)))
        }
    }

    @Test
    fun `user override remains authoritative after merged rule expansion`() {
        val engine = DefaultCategoryEngine()
        val app =
            installedApp(
                packageName = "com.aspyr.swkotor",
                platformCategory = PlatformAppCategory.PRODUCTIVITY
            )

        val categorized = engine.categorize(app, userOverride = AppCategory.WORK)

        assertEquals(AppCategory.WORK, categorized.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, categorized.source)
    }

    @Test
    fun `bundled rule remains above android declared category`() {
        val engine = DefaultCategoryEngine()
        val app =
            installedApp(
                packageName = "com.aspyr.swkotor",
                platformCategory = PlatformAppCategory.PRODUCTIVITY
            )

        val categorized = engine.categorize(app, userOverride = null)

        assertEquals(AppCategory.GAME_RPG, categorized.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, categorized.source)
    }

    @Test
    fun `android game category remains the broad fallback for unmatched games`() {
        val categorized =
            DefaultCategoryEngine().categorize(
                app =
                    installedApp(
                        packageName = "example.unmatched.game",
                        platformCategory = PlatformAppCategory.GAME
                    ),
                userOverride = null
            )

        assertEquals(AppCategory.GAMES, categorized.category)
        assertEquals(ClassificationSource.ANDROID_DECLARED_CATEGORY, categorized.source)
    }

    @Test
    fun `unmatched undefined entry remains unsorted fallback`() {
        val categorized =
            DefaultCategoryEngine().categorize(
                app = installedApp("example.unmatched.app"),
                userOverride = null
            )

        assertEquals(AppCategory.UNSORTED, categorized.category)
        assertEquals(ClassificationSource.UNSORTED_FALLBACK, categorized.source)
    }

    private fun installedApp(
        packageName: String,
        platformCategory: PlatformAppCategory = PlatformAppCategory.UNDEFINED
    ): InstalledApp = InstalledApp(
        id = AppId(packageName),
        launchTargetId = LaunchTargetId(packageName, "$packageName.MainActivity"),
        label = "Integration fixture",
        platformCategory = platformCategory
    )
}
