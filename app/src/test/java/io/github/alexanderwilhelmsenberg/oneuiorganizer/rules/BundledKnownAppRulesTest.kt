package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BundledKnownAppRulesTest {
    @Test
    fun starterRulesContainNoDuplicateAppIds() {
        val appIds = BundledKnownAppRules.entries.map(KnownAppRule::appId)

        assertEquals(appIds.size, appIds.distinct().size)
    }

    @Test
    fun starterRulesCoverCategoriesThatPlatformHintsCannotExpressWell() {
        val expected = mapOf(
            AppId("io.homeassistant.companion.android") to AppCategory.SMART_HOME,
            AppId("com.termux") to AppCategory.DEVELOPMENT,
            AppId("com.github.android") to AppCategory.DEVELOPMENT,
            AppId("com.microsoft.teams") to AppCategory.WORK,
            AppId("com.amazon.mShop.android.shopping") to AppCategory.SHOPPING,
            AppId("com.google.android.apps.walletnfcrel") to AppCategory.FINANCE
        )

        expected.forEach { (appId, category) ->
            assertEquals(category, BundledKnownAppRules.categoryFor(appId))
        }
    }

    @Test
    fun unknownPackageDoesNotMatchStarterRules() {
        assertNull(BundledKnownAppRules.categoryFor(AppId("example.unknown")))
    }
}
