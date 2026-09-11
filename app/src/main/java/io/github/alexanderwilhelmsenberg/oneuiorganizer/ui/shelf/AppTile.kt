package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.alexanderwilhelmsenberg.oneuiorganizer.R
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfAppUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.displayName
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerDimens

@Composable
internal fun AppTile(
    app: ShelfAppUiModel,
    availableCategories: List<CategoryDefinition>,
    onLaunch: (ShelfAppUiModel) -> Unit,
    onMove: (ShelfAppUiModel, CategoryDefinition) -> Unit,
    onToggleFavourite: (ShelfAppUiModel) -> Unit,
    onHide: (ShelfAppUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var actionMenuVisible by rememberSaveable(app.stableKey) { mutableStateOf(false) }
    var moveDialogVisible by rememberSaveable(app.stableKey) { mutableStateOf(false) }
    val openLabel = stringResource(R.string.open_app, app.label)
    val organizeLabel = stringResource(R.string.organize_app, app.label)
    val classificationLabel = app.classificationSource.displayName()
    val classificationDescription =
        stringResource(R.string.classification_reason_content_description, classificationLabel)

    Box(modifier = modifier.width(OrganizerDimens.appTileWidth)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = OrganizerDimens.appTileMinHeight)
                        .combinedClickable(
                            onClickLabel = openLabel,
                            onLongClickLabel = organizeLabel,
                            onClick = { onLaunch(app) },
                            onLongClick = { actionMenuVisible = true }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = openLabel
                        }
                        .padding(vertical = OrganizerDimens.spacingSmall),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIcon(app = app)
                Spacer(modifier = Modifier.size(OrganizerDimens.spacingSmall))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (app.classificationSource == ClassificationSource.UNSORTED_FALLBACK) {
                val sortDescription = stringResource(R.string.sort_app_content_description, app.label)
                TextButton(
                    onClick = { moveDialogVisible = true },
                    modifier =
                        Modifier.semantics {
                            contentDescription = sortDescription
                        }
                ) {
                    Text(stringResource(R.string.sort_app))
                }
            }
        }

        DropdownMenu(
            expanded = actionMenuVisible,
            onDismissRequest = { actionMenuVisible = false }
        ) {
            Text(
                text = classificationLabel,
                modifier =
                    Modifier
                        .padding(
                            horizontal = OrganizerDimens.spacingMedium,
                            vertical = OrganizerDimens.spacingSmall
                        ).semantics {
                            contentDescription = classificationDescription
                        },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (app.isFavourite) {
                                R.string.remove_from_favourites
                            } else {
                                R.string.add_to_favourites
                            }
                        )
                    )
                },
                onClick = {
                    actionMenuVisible = false
                    onToggleFavourite(app)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.move_category)) },
                onClick = {
                    actionMenuVisible = false
                    moveDialogVisible = true
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.hide_app)) },
                onClick = {
                    actionMenuVisible = false
                    onHide(app)
                }
            )
        }
    }

    if (moveDialogVisible) {
        MoveCategoryDialog(
            app = app,
            categories = availableCategories,
            onDismiss = { moveDialogVisible = false },
            onMove = { category ->
                moveDialogVisible = false
                onMove(app, category)
            }
        )
    }
}

@Composable
internal fun AppIcon(
    app: ShelfAppUiModel,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = OrganizerDimens.appIconSize
) {
    val monogram = app.label.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()

    Box(modifier = modifier.size(size)) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(OrganizerDimens.appIconCornerRadius))
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(OrganizerDimens.appIconCornerRadius),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = monogram,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (app.isFavourite) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(OrganizerDimens.favouriteBadgeSize),
                shape = RoundedCornerShape(OrganizerDimens.favouriteBadgeCornerRadius),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "★", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MoveCategoryDialog(
    app: ShelfAppUiModel,
    categories: List<CategoryDefinition>,
    onDismiss: () -> Unit,
    onMove: (CategoryDefinition) -> Unit
) {
    val classificationLabel = app.classificationSource.displayName()
    val classificationDescription =
        stringResource(R.string.classification_reason_content_description, classificationLabel)
    val title =
        stringResource(
            if (app.classificationSource == ClassificationSource.UNSORTED_FALLBACK) {
                R.string.sort_app_title
            } else {
                R.string.move_app_title
            },
            app.label
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = OrganizerDimens.moveDialogMaxHeight)
                        .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = classificationLabel,
                    modifier =
                        Modifier.semantics {
                            contentDescription = classificationDescription
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(OrganizerDimens.spacingSmall))
                categories
                    .filterNot { category -> category.id == app.category.id }
                    .forEach { category ->
                        TextButton(
                            onClick = { onMove(category) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(category.displayName)
                            }
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
