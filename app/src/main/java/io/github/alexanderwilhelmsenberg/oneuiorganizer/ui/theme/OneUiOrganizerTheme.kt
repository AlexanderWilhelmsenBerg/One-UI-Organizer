package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class OrganizerThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val OrganizerLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF3F5F90),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E3FF),
        onPrimaryContainer = Color(0xFF001B3D),
        secondaryContainer = Color(0xFFDCE2F0),
        onSecondaryContainer = Color(0xFF171C26),
        surface = Color(0xFFF9F9FC)
    )

private val OrganizerDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFA9C7FF),
        onPrimary = Color(0xFF0A315F),
        primaryContainer = Color(0xFF274777),
        onPrimaryContainer = Color(0xFFD6E3FF),
        secondaryContainer = Color(0xFF414752),
        onSecondaryContainer = Color(0xFFDCE2F0),
        surface = Color(0xFF111318)
    )

@Composable
fun OneUiOrganizerTheme(
    themeMode: OrganizerThemeMode = OrganizerThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    supportsDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme =
        when (themeMode) {
            OrganizerThemeMode.SYSTEM -> isSystemInDarkTheme()
            OrganizerThemeMode.LIGHT -> false
            OrganizerThemeMode.DARK -> true
        }
    val context = LocalContext.current
    val dynamicColorScheme =
        if (dynamicColor && supportsDynamicColor) {
            AndroidDynamicColorScheme.create(context, darkTheme)
        } else {
            null
        }
    val colorScheme =
        dynamicColorScheme ?: if (darkTheme) OrganizerDarkColorScheme else OrganizerLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OrganizerTypography,
        shapes = OrganizerShapes,
        content = content
    )
}
