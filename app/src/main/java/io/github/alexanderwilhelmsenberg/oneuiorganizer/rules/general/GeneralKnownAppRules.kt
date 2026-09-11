package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.general

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppRule
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppSelector

object GeneralKnownAppRules {
    val entries: List<KnownAppRule> =
        listOf(
            packageRule("io.homeassistant.companion.android", AppCategory.SMART_HOME),
            packageRule("com.termux", AppCategory.DEVELOPMENT),
            packageRule("com.github.android", AppCategory.DEVELOPMENT),
            packageRule("com.microsoft.teams", AppCategory.WORK),
            packageRule("com.amazon.mShop.android.shopping", AppCategory.SHOPPING),
            packageRule("com.google.android.apps.walletnfcrel", AppCategory.FINANCE)
        )

    private fun packageRule(packageName: String, category: AppCategory): KnownAppRule =
        KnownAppRule(
            selector = KnownAppSelector.ExactPackage(AppId(packageName)),
            category = category
        )
}
