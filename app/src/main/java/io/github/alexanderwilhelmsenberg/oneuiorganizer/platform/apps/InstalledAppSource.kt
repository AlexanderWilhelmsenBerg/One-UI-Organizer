package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp

interface InstalledAppSource {
    suspend fun loadInstalledApps(): List<InstalledApp>
}
