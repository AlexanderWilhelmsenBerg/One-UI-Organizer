package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategorySectionUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerShelfUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfAppUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfContentMode
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfErrorUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerDimens

@Composable
fun OrganizerShelf(
    state: OrganizerShelfUiState,
    showHiddenApps: Boolean,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (LaunchTargetId) -> Unit,
    onMoveApp: (LaunchTargetId, CategoryDefinition) -> Unit,
    onToggleFavourite: (LaunchTargetId) -> Unit,
    onHideApp: (LaunchTargetId) -> Unit,
    onRestoreApp: (LaunchTargetId) -> Unit,
    onClassificationReportRequested: () -> Unit,
    onHiddenAppsRequested: () -> Unit,
    onHiddenAppsDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showHiddenApps) {
        HiddenAppsManagement(
            apps = state.hiddenApps,
            onRestoreApp = { onRestoreApp(it.launchTargetId) },
            onDismiss = onHiddenAppsDismissed,
            modifier = modifier
        )
        return
    }

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
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingLarge)
        ) {
            item(key = "shelf-header") {
                ShelfHeader()
            }
            item(key = "search") {
                OrganizerSearchField(
                    query = state.query,
                    onQueryChange = onQueryChange
                )
            }

            state.error?.let { currentError ->
                item(key = "error-${currentError.name}") {
                    ErrorState(currentError)
                }
            }

            when (state.contentMode) {
                ShelfContentMode.LOADING -> {
                    item(key = "loading") {
                        LoadingState()
                    }
                }

                ShelfContentMode.ERROR -> Unit

                ShelfContentMode.NO_RESULTS -> {
                    item(key = "no-results") {
                        NoResultsState(query = state.query)
                    }
                }

                ShelfContentMode.EMPTY -> {
                    item(key = "empty") {
                        EmptyShelfState()
                    }
                }

                ShelfContentMode.CONTENT -> {
                    if (state.favourites.isNotEmpty()) {
                        item(key = "favourites") {
                            AppSection(
                                title = stringResource(R.string.favourites_title),
                                apps = state.favourites,
                                availableCategories = state.availableCategories,
                                onLaunchApp = onLaunchApp,
                                onMoveApp = onMoveApp,
                                onToggleFavourite = onToggleFavourite,
                                onHideApp = onHideApp
                            )
                        }
                    }

                    state.categories.forEach { section ->
                        item(key = "category-${section.category.id.value}") {
                            CategorySection(
                                section = section,
                                availableCategories = state.availableCategories,
                                onLaunchApp = onLaunchApp,
                                onMoveApp = onMoveApp,
                                onToggleFavourite = onToggleFavourite,
                                onHideApp = onHideApp
                            )
                        }
                    }

                    if (state.categories.none { section -> section.category.id == AppCategory.UNSORTED.id }) {
                        item(key = "unsorted-empty") {
                            UnsortedEmptyState()
                        }
                    }
                }
            }

            item(key = "classification-report") {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.hasAnyCurrentApps,
                    onClick = onClassificationReportRequested
                ) {
                    Text(stringResource(R.string.share_classification_report))
                }
            }

            item(key = "hidden-apps-management") {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onHiddenAppsRequested
                ) {
                    Text(
                        if (state.hiddenApps.isEmpty()) {
                            stringResource(R.string.manage_hidden_apps)
                        } else {
                            stringResource(R.string.manage_hidden_apps_count, state.hiddenApps.size)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)) {
        Text(
            text = stringResource(R.string.shelf_title),
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = stringResource(R.string.shelf_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OrganizerSearchField(query: String, onQueryChange: (String) -> Unit) {
    val searchDescription = stringResource(R.string.search_content_description)
    val clearDescription = stringResource(R.string.clear_search)

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = OrganizerDimens.searchMinHeight)
                .semantics {
                    contentDescription = searchDescription
                },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    modifier =
                        Modifier.semantics {
                            contentDescription = clearDescription
                        },
                    onClick = { onQueryChange("") }
                ) {
                    Text("×", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    )
}

@Composable
private fun CategorySection(
    section: CategorySectionUiModel,
    availableCategories: List<CategoryDefinition>,
    onLaunchApp: (LaunchTargetId) -> Unit,
    onMoveApp: (LaunchTargetId, CategoryDefinition) -> Unit,
    onToggleFavourite: (LaunchTargetId) -> Unit,
    onHideApp: (LaunchTargetId) -> Unit
) {
    AppSection(
        title = section.category.displayName,
        apps = section.apps,
        availableCategories = availableCategories,
        onLaunchApp = onLaunchApp,
        onMoveApp = onMoveApp,
        onToggleFavourite = onToggleFavourite,
        onHideApp = onHideApp,
        emptyMessage =
            if (section.category.id == AppCategory.UNSORTED.id) {
                stringResource(R.string.unsorted_empty_body)
            } else {
                null
            }
    )
}

@Composable
private fun AppSection(
    title: String,
    apps: List<ShelfAppUiModel>,
    availableCategories: List<CategoryDefinition>,
    onLaunchApp: (LaunchTargetId) -> Unit,
    onMoveApp: (LaunchTargetId, CategoryDefinition) -> Unit,
    onToggleFavourite: (LaunchTargetId) -> Unit,
    onHideApp: (LaunchTargetId) -> Unit,
    emptyMessage: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = OrganizerDimens.surfaceTonalElevation
    ) {
        Column(
            modifier = Modifier.padding(vertical = OrganizerDimens.categorySurfacePadding),
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingMedium)
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = OrganizerDimens.categoryHeaderMinHeight)
                        .padding(horizontal = OrganizerDimens.categorySurfacePadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (apps.isNotEmpty()) {
                    Text(
                        text = apps.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (apps.isEmpty()) {
                if (emptyMessage != null) {
                    Text(
                        text = emptyMessage,
                        modifier = Modifier.padding(horizontal = OrganizerDimens.categorySurfacePadding),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = OrganizerDimens.categorySurfacePadding),
                    horizontalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingMedium)
                ) {
                    items(
                        items = apps,
                        key = { it.stableKey }
                    ) { app ->
                        AppTile(
                            app = app,
                            availableCategories = availableCategories,
                            onLaunch = { onLaunchApp(it.launchTargetId) },
                            onMove = { target, category -> onMoveApp(target.launchTargetId, category) },
                            onToggleFavourite = { onToggleFavourite(it.launchTargetId) },
                            onHide = { onHideApp(it.launchTargetId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    StateCard {
        CircularProgressIndicator(modifier = Modifier.size(OrganizerDimens.loadingIndicatorSize))
        Spacer(modifier = Modifier.size(OrganizerDimens.spacingMedium))
        Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)) {
            Text(
                text = stringResource(R.string.loading_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.loading_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(error: ShelfErrorUiModel) {
    val title =
        stringResource(
            when (error) {
                ShelfErrorUiModel.SCAN_FAILED -> R.string.scan_error_title
                ShelfErrorUiModel.LAUNCH_FAILED -> R.string.launch_error_title
                ShelfErrorUiModel.STATE_UPDATE_FAILED -> R.string.state_error_title
            }
        )
    val body =
        stringResource(
            when (error) {
                ShelfErrorUiModel.SCAN_FAILED -> R.string.scan_error_body
                ShelfErrorUiModel.LAUNCH_FAILED -> R.string.launch_error_body
                ShelfErrorUiModel.STATE_UPDATE_FAILED -> R.string.state_error_body
            }
        )

    StateCard {
        Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoResultsState(query: String) {
    StateCard {
        Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
            Text(
                text = stringResource(R.string.no_results_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.no_results_body, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyShelfState() {
    StateCard {
        Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
            Text(
                text = stringResource(R.string.empty_shelf_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.empty_shelf_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnsortedEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(OrganizerDimens.spacingExtraLarge),
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)
        ) {
            Text(
                text = AppCategory.UNSORTED.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.unsorted_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StateCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = OrganizerDimens.surfaceTonalElevation
    ) {
        Row(
            modifier = Modifier.padding(OrganizerDimens.spacingExtraLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}
