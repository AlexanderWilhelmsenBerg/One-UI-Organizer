package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.games.GameKnownAppRules
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.general.GeneralKnownAppRules
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.web.WebShortcutKnownAppRules

object BundledKnownAppRules {
    val entries: List<KnownAppRule> =
        GeneralKnownAppRules.entries +
            GameKnownAppRules.entries +
            WebShortcutKnownAppRules.entries

    private val ruleSet = KnownAppRuleSet(entries)

    fun categoryFor(app: InstalledApp): AppCategory? = ruleSet.categoryFor(app)
}
