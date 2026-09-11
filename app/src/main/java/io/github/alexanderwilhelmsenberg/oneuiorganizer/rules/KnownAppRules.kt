package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId

sealed interface KnownAppSelector {
    data class ExactPackage(val appId: AppId) : KnownAppSelector

    data class ExactComponent(val launchTargetId: LaunchTargetId) : KnownAppSelector
}

data class KnownAppRule(val selector: KnownAppSelector, val category: AppCategory)

class KnownAppRuleSet(rules: List<KnownAppRule>) {
    val entries: List<KnownAppRule> = rules.toList()

    init {
        val duplicateSelectors =
            entries
                .groupingBy(KnownAppRule::selector)
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
        check(duplicateSelectors.isEmpty()) {
            "Known-app rules contain duplicate exact selectors: ${duplicateSelectors.joinToString()}"
        }
    }

    private val componentCategories: Map<LaunchTargetId, AppCategory> =
        entries.mapNotNull { rule ->
            val selector = rule.selector as? KnownAppSelector.ExactComponent ?: return@mapNotNull null
            selector.launchTargetId to rule.category
        }.toMap()

    private val packageCategories: Map<AppId, AppCategory> =
        entries.mapNotNull { rule ->
            val selector = rule.selector as? KnownAppSelector.ExactPackage ?: return@mapNotNull null
            selector.appId to rule.category
        }.toMap()

    fun categoryFor(app: InstalledApp): AppCategory? =
        componentCategories[app.launchTargetId] ?: packageCategories[app.id]
}
