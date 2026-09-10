package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object OrganizerDimens {
    val spacingExtraSmall = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 12.dp
    val spacingLarge = 16.dp
    val spacingExtraLarge = 24.dp
    val screenHorizontalPadding = 20.dp
    val screenTopPadding = 32.dp
    val screenBottomPadding = 48.dp
    val searchMinHeight = 56.dp
    val categoryHeaderMinHeight = 44.dp
    val categorySurfacePadding = 16.dp
    val appIconSize = 56.dp
    val appIconCornerRadius = 18.dp
    val appTileWidth = 88.dp
    val appTileMinHeight = 104.dp
    val hiddenAppIconSize = 44.dp
    val surfaceTonalElevation = 2.dp
}

val OrganizerShapes =
    Shapes(
        extraSmall = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp),
        extraLarge = RoundedCornerShape(40.dp)
    )

val OrganizerTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.SemiBold
            ),
        titleLarge =
            TextStyle(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold
            ),
        titleMedium =
            TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold
            ),
        bodyLarge =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal
            ),
        bodyMedium =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            ),
        labelLarge =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
    )
