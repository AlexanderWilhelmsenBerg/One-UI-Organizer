package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

/** Android/Material adapter for the API-gated dynamic-colour implementation. */
object AndroidDynamicColorScheme {
    fun create(context: Context, darkTheme: Boolean): ColorScheme? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

        return if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }
}
