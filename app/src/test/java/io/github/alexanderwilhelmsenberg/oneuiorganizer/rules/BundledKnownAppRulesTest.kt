package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.games.GameKnownAppRules
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.general.GeneralKnownAppRules
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.web.WebShortcutKnownAppRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BundledKnownAppRulesTest {
    @Test
    fun bundledRulesComposeOwnedRulePacksInStableOrder() {
        assertEquals(
            GeneralKnownAppRules.entries + GameKnownAppRules.entries + WebShortcutKnownAppRules.entries,
            BundledKnownAppRules.entries
        )
    }

    @Test
    fun originalSixStarterRulesContinueToMatchByExactPackage() {
        val expected =
            mapOf(
                "io.homeassistant.companion.android" to AppCategory.SMART_HOME,
                "com.termux" to AppCategory.DEVELOPMENT,
                "com.github.android" to AppCategory.DEVELOPMENT,
                "com.microsoft.teams" to AppCategory.WORK,
                "com.amazon.mShop.android.shopping" to AppCategory.SHOPPING,
                "com.google.android.apps.walletnfcrel" to AppCategory.FINANCE
            )

        expected.forEach { (packageName, category) ->
            assertEquals(category, BundledKnownAppRules.categoryFor(installedApp(packageName)))
        }
    }

    @Test
    fun exactPackageRuleMatchesAnyComponentInThatPackage() {
        val ruleSet =
            KnownAppRuleSet(
                listOf(
                    KnownAppRule(
                        KnownAppSelector.ExactPackage(AppId("example.package")),
                        AppCategory.TOOLS
                    )
                )
            )

        assertEquals(
            AppCategory.TOOLS,
            ruleSet.categoryFor(installedApp("example.package", "example.package.AlternateActivity"))
        )
    }

    @Test
    fun exactComponentRuleWinsOverExactPackageRuleInsideKnownRuleTier() {
        val packageName = "example.multi"
        val specialTarget = LaunchTargetId(packageName, "$packageName.SpecialActivity")
        val ruleSet =
            KnownAppRuleSet(
                listOf(
                    KnownAppRule(
                        KnownAppSelector.ExactPackage(AppId(packageName)),
                        AppCategory.TOOLS
                    ),
                    KnownAppRule(
                        KnownAppSelector.ExactComponent(specialTarget),
                        AppCategory.WORK
                    )
                )
            )

        assertEquals(AppCategory.WORK, ruleSet.categoryFor(installedApp(packageName, specialTarget.className)))
        assertEquals(AppCategory.TOOLS, ruleSet.categoryFor(installedApp(packageName, "$packageName.MainActivity")))
    }

    @Test
    fun duplicateExactPackageSelectorFailsFast() {
        val selector = KnownAppSelector.ExactPackage(AppId("example.duplicate"))

        assertFailsWith<IllegalStateException> {
            KnownAppRuleSet(
                listOf(
                    KnownAppRule(selector, AppCategory.TOOLS),
                    KnownAppRule(selector, AppCategory.WORK)
                )
            )
        }
    }

    @Test
    fun duplicateExactComponentSelectorFailsFast() {
        val selector =
            KnownAppSelector.ExactComponent(
                LaunchTargetId("example.duplicate", "example.duplicate.MainActivity")
            )

        assertFailsWith<IllegalStateException> {
            KnownAppRuleSet(
                listOf(
                    KnownAppRule(selector, AppCategory.TOOLS),
                    KnownAppRule(selector, AppCategory.WORK)
                )
            )
        }
    }

    @Test
    fun unknownPackageDoesNotMatchBundledRules() {
        assertNull(BundledKnownAppRules.categoryFor(installedApp("example.unknown")))
    }

    private fun installedApp(
        packageName: String,
        className: String = "$packageName.MainActivity"
    ): InstalledApp =
        InstalledApp(
            id = AppId(packageName),
            launchTargetId = LaunchTargetId(packageName, className),
            label = "Example"
        )
}
