package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultCategoryEngineTest {
    @Test
    fun userOverrideBeatsKnownAppRule() {
        val app = installedApp(
            packageName = "com.termux",
            platformCategory = PlatformAppCategory.GAME
        )

        val result = DefaultCategoryEngine().categorize(app, AppCategory.FINANCE)

        assertEquals(AppCategory.FINANCE, result.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, result.source)
    }

    @Test
    fun customUserOverrideBeatsKnownAppRuleAndPreservesIdentity() {
        val app = installedApp(
            packageName = "com.termux",
            platformCategory = PlatformAppCategory.GAME
        )
        val custom = CustomCategoryDefinition(CategoryId.custom("terminals"), "Terminals")

        val result = DefaultCategoryEngine().categorize(app, custom)

        assertEquals(custom, result.category)
        assertEquals(custom.id, result.category.id)
        assertEquals(ClassificationSource.USER_OVERRIDE, result.source)
    }

    @Test
    fun userOverrideBeatsAndroidCategory() {
        val app = installedApp(
            packageName = "example.social",
            platformCategory = PlatformAppCategory.SOCIAL
        )

        val result = DefaultCategoryEngine(knownAppCategory = { null }).categorize(app, AppCategory.WORK)

        assertEquals(AppCategory.WORK, result.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, result.source)
    }

    @Test
    fun knownAppRuleBeatsAndroidCategory() {
        val app = installedApp(
            packageName = "com.termux",
            platformCategory = PlatformAppCategory.GAME
        )

        val result = DefaultCategoryEngine().categorize(app, userOverride = null)

        assertEquals(AppCategory.DEVELOPMENT, result.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, result.source)
    }

    @Test
    fun androidCategoryBeatsFallback() {
        val app = installedApp(
            packageName = "example.audio",
            platformCategory = PlatformAppCategory.AUDIO
        )

        val result = DefaultCategoryEngine(knownAppCategory = { null }).categorize(app, userOverride = null)

        assertEquals(AppCategory.MUSIC_AUDIO, result.category)
        assertEquals(ClassificationSource.ANDROID_DECLARED_CATEGORY, result.source)
    }

    @Test
    fun fallbackProducesUnsortedWithoutDroppingApp() {
        val app = installedApp(
            packageName = "example.unknown",
            label = "Mystery App",
            platformCategory = PlatformAppCategory.UNDEFINED
        )

        val result = DefaultCategoryEngine(knownAppCategory = { null }).categorize(app, userOverride = null)

        assertEquals(app, result.app)
        assertEquals(AppCategory.UNSORTED, result.category)
        assertEquals(ClassificationSource.UNSORTED_FALLBACK, result.source)
    }

    @Test
    fun repeatedCallsWithSameInputsAreDeterministic() {
        val app = installedApp(
            packageName = "example.maps",
            platformCategory = PlatformAppCategory.MAPS
        )
        val engine = DefaultCategoryEngine(knownAppCategory = { null })

        val first = engine.categorize(app, userOverride = null)
        val second = engine.categorize(app, userOverride = null)

        assertEquals(first, second)
    }

    @Test
    fun allSupportedPlatformCategoriesMapDeterministically() {
        val expected = mapOf(
            PlatformAppCategory.ACCESSIBILITY to AppCategory.TOOLS,
            PlatformAppCategory.AUDIO to AppCategory.MUSIC_AUDIO,
            PlatformAppCategory.GAME to AppCategory.GAMES,
            PlatformAppCategory.IMAGE to AppCategory.PHOTOS,
            PlatformAppCategory.MAPS to AppCategory.TRAVEL_NAVIGATION,
            PlatformAppCategory.NEWS to AppCategory.READING,
            PlatformAppCategory.PRODUCTIVITY to AppCategory.PRODUCTIVITY,
            PlatformAppCategory.SOCIAL to AppCategory.SOCIAL,
            PlatformAppCategory.VIDEO to AppCategory.VIDEO
        )

        expected.forEach { (platformCategory, organizerCategory) ->
            assertEquals(organizerCategory, platformCategory.toOrganizerCategory())
        }
        assertEquals(null, PlatformAppCategory.UNDEFINED.toOrganizerCategory())
    }

    @Test
    fun displayLabelCannotTriggerKnownAppRule() {
        val app = installedApp(
            packageName = "example.not-home-assistant",
            label = "Home Assistant",
            platformCategory = PlatformAppCategory.UNDEFINED
        )

        val result = DefaultCategoryEngine().categorize(app, userOverride = null)

        assertEquals(AppCategory.UNSORTED, result.category)
        assertEquals(ClassificationSource.UNSORTED_FALLBACK, result.source)
    }

    private fun installedApp(
        packageName: String,
        label: String = "Example",
        platformCategory: PlatformAppCategory
    ): InstalledApp = InstalledApp(
        id = AppId(packageName),
        launchTargetId = LaunchTargetId(packageName, "$packageName.MainActivity"),
        label = label,
        platformCategory = platformCategory
    )
}
