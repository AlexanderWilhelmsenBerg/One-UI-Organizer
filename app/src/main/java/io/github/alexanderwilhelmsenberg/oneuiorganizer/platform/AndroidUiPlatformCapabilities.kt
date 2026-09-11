package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform

import android.os.Build

data class UiPlatformCapabilities(val supportsDynamicColor: Boolean)

object AndroidUiPlatformCapabilities {
    fun current(): UiPlatformCapabilities = UiPlatformCapabilities(
        supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    )
}
