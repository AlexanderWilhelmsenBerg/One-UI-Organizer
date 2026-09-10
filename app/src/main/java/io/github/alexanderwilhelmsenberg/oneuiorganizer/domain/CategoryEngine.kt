package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp

interface CategoryEngine {
    fun categorize(app: InstalledApp, userOverride: AppCategory?): CategorizedApp
}
