package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.BundledKnownAppRules

class DefaultCategoryEngine(private val knownAppCategory: (AppId) -> AppCategory? = BundledKnownAppRules::categoryFor) :
    CategoryEngine {
    override fun categorize(app: InstalledApp, userOverride: AppCategory?): CategorizedApp {
        if (userOverride != null) {
            return CategorizedApp(
                app = app,
                category = userOverride,
                source = ClassificationSource.USER_OVERRIDE
            )
        }

        val knownCategory = knownAppCategory(app.id)
        if (knownCategory != null) {
            return CategorizedApp(
                app = app,
                category = knownCategory,
                source = ClassificationSource.KNOWN_APP_RULE
            )
        }

        val platformCategory = app.platformCategory.toOrganizerCategory()
        if (platformCategory != null) {
            return CategorizedApp(
                app = app,
                category = platformCategory,
                source = ClassificationSource.ANDROID_DECLARED_CATEGORY
            )
        }

        return CategorizedApp(
            app = app,
            category = AppCategory.UNSORTED,
            source = ClassificationSource.UNSORTED_FALLBACK
        )
    }
}

fun PlatformAppCategory.toOrganizerCategory(): AppCategory? = when (this) {
    PlatformAppCategory.ACCESSIBILITY -> AppCategory.TOOLS
    PlatformAppCategory.AUDIO -> AppCategory.MUSIC_AUDIO
    PlatformAppCategory.GAME -> AppCategory.GAMES
    PlatformAppCategory.IMAGE -> AppCategory.PHOTOS
    PlatformAppCategory.MAPS -> AppCategory.TRAVEL_NAVIGATION
    PlatformAppCategory.NEWS -> AppCategory.READING
    PlatformAppCategory.PRODUCTIVITY -> AppCategory.PRODUCTIVITY
    PlatformAppCategory.SOCIAL -> AppCategory.SOCIAL
    PlatformAppCategory.VIDEO -> AppCategory.VIDEO
    PlatformAppCategory.UNDEFINED -> null
}
