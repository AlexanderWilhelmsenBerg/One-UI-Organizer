package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId

data class KnownAppRule(val appId: AppId, val category: AppCategory)

object BundledKnownAppRules {
    val entries: List<KnownAppRule> = listOf(
        KnownAppRule(AppId("io.homeassistant.companion.android"), AppCategory.SMART_HOME),
        KnownAppRule(AppId("com.termux"), AppCategory.DEVELOPMENT),
        KnownAppRule(AppId("com.github.android"), AppCategory.DEVELOPMENT),
        KnownAppRule(AppId("com.microsoft.teams"), AppCategory.WORK),
        KnownAppRule(AppId("com.amazon.mShop.android.shopping"), AppCategory.SHOPPING),
        KnownAppRule(AppId("com.google.android.apps.walletnfcrel"), AppCategory.FINANCE)
    )

    private val categoriesByAppId: Map<AppId, AppCategory> = entries
        .associate { rule -> rule.appId to rule.category }
        .also { indexedRules ->
            check(indexedRules.size == entries.size) { "Bundled known-app rules contain duplicate app IDs." }
        }

    fun categoryFor(appId: AppId): AppCategory? = categoriesByAppId[appId]
}
