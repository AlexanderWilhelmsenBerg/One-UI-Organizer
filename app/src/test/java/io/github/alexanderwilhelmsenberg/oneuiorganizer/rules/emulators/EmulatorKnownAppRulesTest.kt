package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.emulators

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
import kotlin.test.assertTrue

class EmulatorKnownAppRulesTest {
    private val ruleSet = KnownAppRuleSet(EmulatorKnownAppRules.entries)
    private val categoryEngine = DefaultCategoryEngine(ruleSet::categoryFor)

    @Test
    fun emulatorRulePackUsesEvidenceBackedExactSelectors() {
        assertEquals(15, EmulatorKnownAppRules.entries.size)
        assertEquals(
            setOf(AppCategory.EMULATORS),
            EmulatorKnownAppRules.entries.map { rule -> rule.category }.toSet()
        )
        assertEquals(
            14,
            EmulatorKnownAppRules.entries.count { rule -> rule.selector is KnownAppSelector.ExactPackage }
        )
        assertEquals(
            1,
            EmulatorKnownAppRules.entries.count { rule -> rule.selector is KnownAppSelector.ExactComponent }
        )
    }

    @Test
    fun representativeEmulatorsMapToEmulatorCategory() {
        val packages =
            listOf(
                "com.dsemu.drastic",
                "com.github.stenzek.duckstation",
                "info.cemu.cemu",
                "org.dolphinemu.dolphinemu",
                "org.ppsspp.ppsspp",
                "xyz.aethersx2.android"
            )

        packages.forEach { packageName ->
            assertEquals(AppCategory.EMULATORS, ruleSet.categoryFor(installedApp(packageName)))
        }
    }

    @Test
    fun undefinedPlatformCategoryCanStillBecomeEmulatorFromKnownRule() {
        val categorized =
            categoryEngine.categorize(
                installedApp(
                    packageName = "org.citra.citra_emu",
                    platformCategory = PlatformAppCategory.UNDEFINED
                ),
                userOverride = null
            )

        assertEquals(AppCategory.EMULATORS, categorized.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, categorized.source)
    }

    @Test
    fun edenYuzuLauncherUsesExactComponentRule() {
        val categorized =
            categoryEngine.categorize(
                installedApp(
                    packageName = "com.miHoYo.Yuanshen",
                    className = "org.yuzu.yuzu_emu.ui.main.MainActivity"
                ),
                userOverride = null
            )

        assertEquals(AppCategory.EMULATORS, categorized.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, categorized.source)
    }

    @Test
    fun samePackageWithDifferentComponentIsNotAssumedToBeEmulator() {
        val categorized =
            categoryEngine.categorize(
                installedApp(
                    packageName = "com.miHoYo.Yuanshen",
                    className = "com.miHoYo.GetMobileInfo.MainActivity"
                ),
                userOverride = null
            )

        assertEquals(AppCategory.GAMES, categorized.category)
        assertEquals(ClassificationSource.ANDROID_DECLARED_CATEGORY, categorized.source)
    }

    @Test
    fun allPackageSelectorsAreUnique() {
        val packageSelectors =
            EmulatorKnownAppRules.entries.mapNotNull { rule ->
                (rule.selector as? KnownAppSelector.ExactPackage)?.appId
            }

        assertEquals(packageSelectors.size, packageSelectors.toSet().size)
        assertTrue(packageSelectors.isNotEmpty())
    }

    private fun installedApp(
        packageName: String,
        className: String = "$packageName.MainActivity",
        platformCategory: PlatformAppCategory = PlatformAppCategory.GAME
    ): InstalledApp = InstalledApp(
        id = AppId(packageName),
        launchTargetId = LaunchTargetId(packageName, className),
        label = "Emulator fixture",
        platformCategory = platformCategory
    )
}
