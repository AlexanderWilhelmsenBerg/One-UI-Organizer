package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId

internal data class LaunchComponentSpec(val packageName: String, val className: String)

internal fun LaunchTargetId.toLaunchComponentSpec(): LaunchComponentSpec? {
    if (packageName.isBlank() || className.isBlank()) {
        return null
    }

    return LaunchComponentSpec(
        packageName = packageName,
        className = className
    )
}
