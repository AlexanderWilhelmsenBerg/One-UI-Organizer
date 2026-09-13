package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.R

enum class PrimaryDestination(
    val savedValue: String,
    @StringRes val labelResId: Int
) {
    ORGANIZER(
        savedValue = "organizer",
        labelResId = R.string.primary_destination_organizer
    ),
    CATEGORIES(
        savedValue = "categories",
        labelResId = R.string.primary_destination_categories
    );

    companion object {
        fun fromSavedValue(value: String?): PrimaryDestination =
            entries.firstOrNull { destination -> destination.savedValue == value } ?: ORGANIZER

        fun forCategoryShortcut(): PrimaryDestination = ORGANIZER
    }
}

internal enum class PrimaryBackAction {
    DISMISS_BACKUP_RESTORE,
    DISMISS_CATEGORY_MANAGEMENT,
    DISMISS_HIDDEN_APPS,
    SYSTEM
}

internal fun primaryBackAction(
    showBackupRestore: Boolean,
    showCategoryManagement: Boolean,
    showHiddenApps: Boolean
): PrimaryBackAction =
    when {
        showBackupRestore -> PrimaryBackAction.DISMISS_BACKUP_RESTORE
        showCategoryManagement -> PrimaryBackAction.DISMISS_CATEGORY_MANAGEMENT
        showHiddenApps -> PrimaryBackAction.DISMISS_HIDDEN_APPS
        else -> PrimaryBackAction.SYSTEM
    }

@Composable
fun PrimaryNavigationHost(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    showHiddenApps: Boolean,
    showCategoryManagement: Boolean,
    showBackupRestore: Boolean,
    onDismissHiddenApps: () -> Unit,
    onDismissCategoryManagement: () -> Unit,
    onDismissBackupRestore: () -> Unit,
    organizerContent: @Composable (showHiddenApps: Boolean) -> Unit,
    categoriesContent: @Composable () -> Unit,
    categoryManagementContent: @Composable () -> Unit,
    backupRestoreContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryDestinationState = rememberSaveableStateHolder()
    val backAction =
        primaryBackAction(
            showBackupRestore = showBackupRestore,
            showCategoryManagement = showCategoryManagement,
            showHiddenApps = showHiddenApps
        )

    BackHandler(enabled = backAction != PrimaryBackAction.SYSTEM) {
        when (backAction) {
            PrimaryBackAction.DISMISS_BACKUP_RESTORE -> onDismissBackupRestore()
            PrimaryBackAction.DISMISS_CATEGORY_MANAGEMENT -> onDismissCategoryManagement()
            PrimaryBackAction.DISMISS_HIDDEN_APPS -> onDismissHiddenApps()
            PrimaryBackAction.SYSTEM -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            showBackupRestore -> backupRestoreContent()
            showCategoryManagement -> categoryManagementContent()
            showHiddenApps -> organizerContent(true)
            else -> {
                PrimaryNavigationShell(
                    selectedDestination = selectedDestination,
                    onDestinationSelected = onDestinationSelected
                ) { destination ->
                    primaryDestinationState.SaveableStateProvider(destination.savedValue) {
                        when (destination) {
                            PrimaryDestination.ORGANIZER -> organizerContent(false)
                            PrimaryDestination.CATEGORIES -> categoriesContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryNavigationShell(
    selectedDestination: PrimaryDestination,
    onDestinationSelected: (PrimaryDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PrimaryDestination) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxSize()
        ) {
            content(selectedDestination)
        }

        NavigationBar(modifier = Modifier.selectableGroup()) {
            PrimaryDestination.entries.forEach { destination ->
                val label = stringResource(destination.labelResId)
                NavigationBarItem(
                    selected = selectedDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                    icon = { PrimaryDestinationIcon(destination) },
                    label = { Text(label) },
                    alwaysShowLabel = true,
                    modifier =
                        Modifier.semantics {
                            contentDescription = label
                        }
                )
            }
        }
    }
}

@Composable
private fun PrimaryDestinationIcon(destination: PrimaryDestination) {
    val color = LocalContentColor.current
    Canvas(
        modifier =
            Modifier
                .size(24.dp)
                .clearAndSetSemantics { }
    ) {
        when (destination) {
            PrimaryDestination.ORGANIZER -> {
                val radius = size.minDimension * 0.105f
                val positions =
                    listOf(
                        Offset(size.width * 0.32f, size.height * 0.32f),
                        Offset(size.width * 0.68f, size.height * 0.32f),
                        Offset(size.width * 0.32f, size.height * 0.68f),
                        Offset(size.width * 0.68f, size.height * 0.68f)
                    )
                positions.forEach { center ->
                    drawCircle(color = color, radius = radius, center = center)
                }
            }

            PrimaryDestination.CATEGORIES -> {
                val strokeWidth = size.minDimension * 0.09f
                listOf(0.28f, 0.5f, 0.72f).forEach { fraction ->
                    val y = size.height * fraction
                    drawCircle(
                        color = color,
                        radius = strokeWidth * 0.65f,
                        center = Offset(size.width * 0.23f, y)
                    )
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.39f, y),
                        end = Offset(size.width * 0.78f, y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
