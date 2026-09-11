package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.web

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.DefaultCategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebShortcutKnownAppRulesTest {
    private val ruleSet = KnownAppRuleSet(WebShortcutKnownAppRules.entries)

    @Test
    fun generatedChromiumWebApkPackageIsClassifiedAsWebShortcut() {
        assertEquals(
            AppCategory.WEB_SHORTCUTS,
            ruleSet.categoryFor(
                installedApp(
                    packageName = "org.chromium.webapk.generated_hash_v2",
                    className = "org.chromium.webapk.shell_apk.h2o.H2OOpaqueMainActivity",
                    label = "Installed site"
                )
            )
        )
    }

    @Test
    fun packagePrefixNearMissesDoNotMatch() {
        listOf(
            "org.chromium.webapk",
            "org.chromium.webapknot.generated",
            "org.chromium.webapk_like.generated"
        ).forEach { packageName ->
            assertNull(ruleSet.categoryFor(installedApp(packageName)))
        }
    }

    @Test
    fun ordinaryBrowserAppsAreNotClassifiedAsWebShortcuts() {
        listOf(
            "com.android.chrome",
            "com.sec.android.app.sbrowser"
        ).forEach { packageName ->
            assertNull(ruleSet.categoryFor(installedApp(packageName)))
        }
    }

    @Test
    fun webLookingLabelDoesNotCauseClassification() {
        assertNull(
            ruleSet.categoryFor(
                installedApp(
                    packageName = "com.example.nativeapp",
                    label = "https://example.com"
                )
            )
        )
    }

    @Test
    fun twaLikeComponentDoesNotCauseClassification() {
        assertNull(
            ruleSet.categoryFor(
                installedApp(
                    packageName = "com.example.brandedapp",
                    className = "com.example.brandedapp.TrustedWebActivityLauncher",
                    label = "Example"
                )
            )
        )
    }

    @Test
    fun webApkClassificationIsDeterministic() {
        val app = installedApp("org.chromium.webapk.generated_hash")

        val results = List(20) { ruleSet.categoryFor(app) }

        assertEquals(setOf(AppCategory.WEB_SHORTCUTS), results.toSet())
    }

    @Test
    fun categoryEngineReportsBundledRuleSourceForWebApk() {
        val result =
            DefaultCategoryEngine().categorize(
                app = installedApp("org.chromium.webapk.generated_hash"),
                userOverride = null
            )

        assertEquals(AppCategory.WEB_SHORTCUTS, result.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, result.source)
    }

    @Test
    fun manualOverrideRemainsAuthoritativeForWebApk() {
        val result =
            DefaultCategoryEngine().categorize(
                app = installedApp("org.chromium.webapk.generated_hash"),
                userOverride = AppCategory.WORK
            )

        assertEquals(AppCategory.WORK, result.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, result.source)
    }

    private fun installedApp(
        packageName: String,
        className: String = "$packageName.MainActivity",
        label: String = "Example"
    ): InstalledApp =
        InstalledApp(
            id = AppId(packageName),
            launchTargetId = LaunchTargetId(packageName, className),
            label = label
        )
}
