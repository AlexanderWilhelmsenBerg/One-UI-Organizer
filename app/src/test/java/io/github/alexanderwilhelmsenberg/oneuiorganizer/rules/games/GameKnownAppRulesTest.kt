package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.games

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.DefaultCategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppRuleSet
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppSelector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameKnownAppRulesTest {
    private val ruleSet = KnownAppRuleSet(GameKnownAppRules.entries)
    private val categoryEngine = DefaultCategoryEngine(ruleSet::categoryFor)

    @Test
    fun gameRulePackUsesOnlyExactPackagesAndFrozenGameCategories() {
        val frozenGameCategories =
            setOf(
                AppCategory.GAME_ACTION_ADVENTURE,
                AppCategory.GAME_RPG,
                AppCategory.GAME_STRATEGY_SIMULATION,
                AppCategory.GAME_PUZZLE_CASUAL,
                AppCategory.GAME_BOARD_CARD
            )

        assertEquals(106, GameKnownAppRules.entries.size)
        assertTrue(GameKnownAppRules.entries.all { rule -> rule.selector is KnownAppSelector.ExactPackage })
        assertEquals(frozenGameCategories, GameKnownAppRules.entries.map { rule -> rule.category }.toSet())
    }

    @Test
    fun representativeExactPackageMappingsCoverEveryGameCategory() {
        val expected =
            mapOf(
                "com.bandainamcoent.ultimateninjastorm" to AppCategory.GAME_ACTION_ADVENTURE,
                "com.aspyr.swkotor" to AppCategory.GAME_RPG,
                "com.ExabyteGames.FinalOutpost" to AppCategory.GAME_STRATEGY_SIMULATION,
                "com.denysdmytro.Oberty" to AppCategory.GAME_PUZZLE_CASUAL,
                "com.playstack.balatro.android" to AppCategory.GAME_BOARD_CARD
            )

        expected.forEach { (packageName, category) ->
            assertEquals(category, ruleSet.categoryFor(installedGame(packageName)))
        }
    }

    @Test
    fun gameKnownRuleOverridesAndroidGameFallback() {
        val categorized = categoryEngine.categorize(installedGame("com.aspyr.swkotor"), userOverride = null)

        assertEquals(AppCategory.GAME_RPG, categorized.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, categorized.source)
    }

    @Test
    fun userOverrideStillWinsOverGameKnownRule() {
        val categorized =
            categoryEngine.categorize(
                app = installedGame("com.playstack.balatro.android"),
                userOverride = AppCategory.OTHER
            )

        assertEquals(AppCategory.OTHER, categorized.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, categorized.source)
    }

    @Test
    fun unknownGameStaysInSafeGamesFallback() {
        val categorized = categoryEngine.categorize(installedGame("example.unknown.game"), userOverride = null)

        assertEquals(AppCategory.GAMES, categorized.category)
        assertEquals(ClassificationSource.ANDROID_DECLARED_CATEGORY, categorized.source)
    }

    @Test
    fun gameRulesDoNotInferFromLocalizedOrDisplayTitle() {
        val categorized =
            categoryEngine.categorize(
                app = installedGame(packageName = "example.unrelated", label = "Baldur's Gate"),
                userOverride = null
            )

        assertEquals(AppCategory.GAMES, categorized.category)
        assertEquals(ClassificationSource.ANDROID_DECLARED_CATEGORY, categorized.source)
    }

    @Test
    fun duplicateGameSelectorFailsFast() {
        assertFailsWith<IllegalStateException> {
            KnownAppRuleSet(GameKnownAppRules.entries + GameKnownAppRules.entries.first())
        }
    }

    @Test
    fun gameRuleResultsAreDeterministic() {
        val packages =
            GameKnownAppRules.entries.map { rule ->
                (rule.selector as KnownAppSelector.ExactPackage).appId.packageName
            }
        val expected = packages.map { packageName -> ruleSet.categoryFor(installedGame(packageName)) }

        repeat(10) {
            assertEquals(
                expected,
                packages.map { packageName -> ruleSet.categoryFor(installedGame(packageName)) }
            )
        }
    }

    private fun installedGame(packageName: String, label: String = "Game"): InstalledApp = InstalledApp(
        id = AppId(packageName),
        launchTargetId = LaunchTargetId(packageName, "$packageName.MainActivity"),
        label = label,
        platformCategory = PlatformAppCategory.GAME
    )
}
