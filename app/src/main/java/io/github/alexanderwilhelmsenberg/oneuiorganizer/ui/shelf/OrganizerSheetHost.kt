package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerDimens

@Composable
fun OrganizerSheetHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = OrganizerDimens.sheetMaxHeight),
            shape =
                RoundedCornerShape(
                    topStart = OrganizerDimens.sheetTopCornerRadius,
                    topEnd = OrganizerDimens.sheetTopCornerRadius
                ),
            tonalElevation = OrganizerDimens.sheetTonalElevation,
            content = content
        )
    }
}
