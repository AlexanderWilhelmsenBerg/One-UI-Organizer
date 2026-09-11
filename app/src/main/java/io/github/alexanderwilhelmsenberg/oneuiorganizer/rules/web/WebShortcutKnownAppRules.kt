package io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.web

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppRule
import io.github.alexanderwilhelmsenberg.oneuiorganizer.rules.KnownAppSelector

/** Evidence-backed browser-created launcher entries with stable Android package identities. */
object WebShortcutKnownAppRules {
    val entries: List<KnownAppRule> =
        listOf(
            KnownAppRule(
                selector = KnownAppSelector.PackagePrefix("org.chromium.webapk."),
                category = AppCategory.WEB_SHORTCUTS
            )
        )
}
