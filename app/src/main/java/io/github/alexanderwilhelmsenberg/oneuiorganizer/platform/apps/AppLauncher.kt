package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId

interface AppLauncher {
    fun launch(target: LaunchTargetId): Boolean
}
