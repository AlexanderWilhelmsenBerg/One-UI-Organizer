package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.R
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementItemUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerDimens

@Composable
fun CategoriesOverview(
    state: CategoryManagementUiState,
    onManageCategories: () -> Unit,
    onBackupRestoreRequested: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingLarge)
        ) {
            item(key = "categories-overview-header") {
                CategoriesHeader()
            }

            item(key = "categories-tools") {
                CategoryTools(
                    onManageCategories = onManageCategories,
                    onBackupRestoreRequested = onBackupRestoreRequested
                )
            }

            if (state.categories.isEmpty()) {
                item(key = "categories-overview-empty") {
                    Text(
                        text = stringResource(R.string.categories_overview_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = state.categories,
                    key = { item -> item.category.id.value }
                ) { item ->
                    CategoryOverviewRow(item)
                }
            }
        }
    }
}

@Composable
private fun CategoriesHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)) {
        Text(
            text = stringResource(R.string.manage_categories_title),
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = stringResource(R.string.categories_overview_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryTools(
    onManageCategories: () -> Unit,
    onBackupRestoreRequested: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = OrganizerDimens.surfaceTonalElevation
    ) {
        Column(
            modifier = Modifier.padding(OrganizerDimens.categorySurfacePadding),
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)
        ) {
            Text(
                text = stringResource(R.string.category_tools_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.category_tools_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onManageCategories
            ) {
                Text(stringResource(R.string.manage_categories))
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBackupRestoreRequested
            ) {
                Text(stringResource(R.string.backup_restore_title))
            }
        }
    }
}

@Composable
private fun CategoryOverviewRow(item: CategoryManagementItemUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = OrganizerDimens.surfaceTonalElevation
    ) {
        Row(
            modifier = Modifier.padding(OrganizerDimens.categorySurfacePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingMedium)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)
            ) {
                Text(
                    text = item.category.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text =
                        stringResource(
                            if (item.isCustom) {
                                R.string.custom_category_label
                            } else {
                                R.string.built_in_category_label
                            }
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text =
                    pluralStringResource(
                        R.plurals.category_assigned_apps,
                        item.assignedAppCount,
                        item.assignedAppCount
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
