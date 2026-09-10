package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.alexanderwilhelmsenberg.oneuiorganizer.R
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfAppUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerDimens

@Composable
fun HiddenAppsManagement(
    apps: List<ShelfAppUiModel>,
    onRestoreApp: (ShelfAppUiModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding =
                PaddingValues(
                    start = OrganizerDimens.screenHorizontalPadding,
                    top = OrganizerDimens.screenTopPadding,
                    end = OrganizerDimens.screenHorizontalPadding,
                    bottom = OrganizerDimens.screenBottomPadding
                ),
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingMedium)
        ) {
            item(key = "hidden-header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)
                ) {
                    Text(
                        text = stringResource(R.string.hidden_apps_title),
                        style = MaterialTheme.typography.displaySmall
                    )
                    Text(
                        text = stringResource(R.string.hidden_apps_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.hidden_apps_done))
                    }
                }
            }

            if (apps.isEmpty()) {
                item(key = "hidden-empty") {
                    EmptyHiddenAppsState()
                }
            } else {
                items(
                    items = apps,
                    key = { it.stableKey }
                ) { app ->
                    HiddenAppRow(
                        app = app,
                        onRestore = { onRestoreApp(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAppRow(app: ShelfAppUiModel, onRestore: () -> Unit) {
    val restoreDescription = stringResource(R.string.restore_app_content_description, app.label)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(OrganizerDimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                app = app,
                size = OrganizerDimens.hiddenAppIconSize
            )
            Spacer(modifier = Modifier.width(OrganizerDimens.spacingMedium))
            Text(
                text = app.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(
                modifier =
                    Modifier.semantics {
                        contentDescription = restoreDescription
                    },
                onClick = onRestore
            ) {
                Text(stringResource(R.string.restore_app))
            }
        }
    }
}

@Composable
private fun EmptyHiddenAppsState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = OrganizerDimens.surfaceTonalElevation
    ) {
        Column(
            modifier = Modifier.padding(OrganizerDimens.spacingExtraLarge),
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)
        ) {
            Text(
                text = stringResource(R.string.hidden_apps_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.hidden_apps_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
