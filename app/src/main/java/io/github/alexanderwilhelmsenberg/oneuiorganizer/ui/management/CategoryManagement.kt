package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import io.github.alexanderwilhelmsenberg.oneuiorganizer.R
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryDeletionChoiceUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementItemUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryMoveDirectionUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerDimens

@Composable
fun CategoryManagement(
    state: CategoryManagementUiState,
    onCreateCategory: (String) -> Unit,
    onRenameCategory: (CategoryId, String) -> Unit,
    onDeleteCategory: (CategoryId, CategoryDeletionChoiceUiModel) -> Unit,
    onMoveCategory: (CategoryId, CategoryMoveDirectionUiModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    pinningSupported: Boolean = true,
    onPinCategory: (CategoryDefinition) -> Unit = {},
    onBackupRestoreRequested: (() -> Unit)? = null
) {
    var createDialogVisible by rememberSaveable { mutableStateOf(false) }
    var renameCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteCategoryId by rememberSaveable { mutableStateOf<String?>(null) }

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
            item(key = "category-management-header") {
                ManagementHeader(
                    onCreate = { createDialogVisible = true },
                    onDismiss = onDismiss,
                    pinningSupported = pinningSupported,
                    onBackupRestoreRequested = onBackupRestoreRequested
                )
            }

            state.operationError?.let { operationError ->
                item(key = "category-management-error") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = operationError,
                            modifier = Modifier.padding(OrganizerDimens.spacingLarge),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            itemsIndexed(
                items = state.categories,
                key = { _, item -> item.category.id.value }
            ) { index, item ->
                CategoryManagementRow(
                    item = item,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.categories.lastIndex,
                    pinningSupported = pinningSupported,
                    onMoveUp = {
                        onMoveCategory(item.category.id, CategoryMoveDirectionUiModel.UP)
                    },
                    onMoveDown = {
                        onMoveCategory(item.category.id, CategoryMoveDirectionUiModel.DOWN)
                    },
                    onPin = { onPinCategory(item.category) },
                    onRename = { renameCategoryId = item.category.id.value },
                    onDelete = { deleteCategoryId = item.category.id.value }
                )
            }
        }
    }

    if (createDialogVisible) {
        CategoryNameDialog(
            title = stringResource(R.string.create_category_title),
            initialName = "",
            confirmLabel = stringResource(R.string.create_category_confirm),
            externalError = state.validationError,
            onDismiss = { createDialogVisible = false },
            onConfirm = { name ->
                onCreateCategory(name)
                createDialogVisible = false
            }
        )
    }

    val renameItem = state.categories.firstOrNull { item -> item.category.id.value == renameCategoryId }
    if (renameItem != null) {
        CategoryNameDialog(
            title = stringResource(R.string.rename_category_title, renameItem.category.displayName),
            initialName = renameItem.category.displayName,
            confirmLabel = stringResource(R.string.rename_category_confirm),
            externalError = state.validationError,
            onDismiss = { renameCategoryId = null },
            onConfirm = { name ->
                onRenameCategory(renameItem.category.id, name)
                renameCategoryId = null
            }
        )
    }

    val deleteItem = state.categories.firstOrNull { item -> item.category.id.value == deleteCategoryId }
    if (deleteItem != null) {
        if (deleteItem.assignedAppCount == 0) {
            EmptyCategoryDeleteDialog(
                item = deleteItem,
                onDismiss = { deleteCategoryId = null },
                onDelete = {
                    onDeleteCategory(
                        deleteItem.category.id,
                        CategoryDeletionChoiceUiModel.AutomaticClassification
                    )
                    deleteCategoryId = null
                }
            )
        } else {
            PopulatedCategoryDeleteDialog(
                item = deleteItem,
                reassignTargets =
                    state.categories.filter { candidate ->
                        candidate.category.id != deleteItem.category.id
                    },
                onDismiss = { deleteCategoryId = null },
                onDelete = { choice ->
                    onDeleteCategory(deleteItem.category.id, choice)
                    deleteCategoryId = null
                }
            )
        }
    }
}

@Composable
private fun ManagementHeader(
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    pinningSupported: Boolean,
    onBackupRestoreRequested: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.manage_categories_title),
                style = MaterialTheme.typography.displaySmall
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.category_management_done))
            }
        }
        Text(
            text = stringResource(R.string.manage_categories_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!pinningSupported) {
            Text(
                text = stringResource(R.string.category_pinning_unsupported),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreate
        ) {
            Text(stringResource(R.string.create_category))
        }
        onBackupRestoreRequested?.let { onBackupRestore ->
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBackupRestore
            ) {
                Text(stringResource(R.string.backup_restore_title))
            }
        }
    }
}

@Composable
private fun CategoryManagementRow(
    item: CategoryManagementItemUiModel,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    pinningSupported: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryName = item.category.displayName
    val moveUpDescription = stringResource(R.string.move_category_up_content_description, categoryName)
    val moveDownDescription = stringResource(R.string.move_category_down_content_description, categoryName)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = OrganizerDimens.surfaceTonalElevation
    ) {
        Column(
            modifier = Modifier.padding(OrganizerDimens.categorySurfacePadding),
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)
                ) {
                    Text(
                        text = categoryName,
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
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.category_assigned_apps,
                                item.assignedAppCount,
                                item.assignedAppCount
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)
            ) {
                TextButton(
                    enabled = canMoveUp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { testTag = "move-up-${item.category.id.value}" },
                    onClick = onMoveUp
                ) {
                    Text(moveUpDescription)
                }
                TextButton(
                    enabled = canMoveDown,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { testTag = "move-down-${item.category.id.value}" },
                    onClick = onMoveDown
                ) {
                    Text(moveDownDescription)
                }
                if (pinningSupported) {
                    TextButton(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { testTag = "pin-${item.category.id.value}" },
                        onClick = onPin
                    ) {
                        Text(stringResource(R.string.pin_category_shortcut))
                    }
                }
                if (item.isCustom) {
                    TextButton(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { testTag = "rename-${item.category.id.value}" },
                        onClick = onRename
                    ) {
                        Text(stringResource(R.string.rename_category))
                    }
                    TextButton(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { testTag = "delete-${item.category.id.value}" },
                        onClick = onDelete
                    ) {
                        Text(stringResource(R.string.delete_category))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    externalError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    isError = externalError != null,
                    supportingText = externalError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(name) })
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun EmptyCategoryDeleteDialog(
    item: CategoryManagementItemUiModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_category_title, item.category.displayName)) },
        text = { Text(stringResource(R.string.delete_empty_category_body)) },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete_category_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun PopulatedCategoryDeleteDialog(
    item: CategoryManagementItemUiModel,
    reassignTargets: List<CategoryManagementItemUiModel>,
    onDismiss: () -> Unit,
    onDelete: (CategoryDeletionChoiceUiModel) -> Unit
) {
    var selectedMode by rememberSaveable { mutableStateOf(DeleteMode.AUTOMATIC) }
    var reassignTargetId by rememberSaveable { mutableStateOf(reassignTargets.firstOrNull()?.category?.id?.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_category_title, item.category.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
                Text(
                    pluralStringResource(
                        R.plurals.delete_populated_category_body,
                        item.assignedAppCount,
                        item.assignedAppCount
                    )
                )
                DeleteChoiceRow(
                    label = stringResource(R.string.delete_category_return_automatic),
                    selected = selectedMode == DeleteMode.AUTOMATIC,
                    onClick = { selectedMode = DeleteMode.AUTOMATIC }
                )
                if (reassignTargets.isNotEmpty()) {
                    DeleteChoiceRow(
                        label = stringResource(R.string.delete_category_reassign),
                        selected = selectedMode == DeleteMode.REASSIGN,
                        onClick = { selectedMode = DeleteMode.REASSIGN }
                    )
                    if (selectedMode == DeleteMode.REASSIGN) {
                        reassignTargets.forEach { target ->
                            DeleteChoiceRow(
                                label = target.category.displayName,
                                selected = reassignTargetId == target.category.id.value,
                                onClick = { reassignTargetId = target.category.id.value },
                                indent = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedMode != DeleteMode.REASSIGN || reassignTargetId != null,
                onClick = {
                    val choice =
                        when (selectedMode) {
                            DeleteMode.AUTOMATIC -> CategoryDeletionChoiceUiModel.AutomaticClassification
                            DeleteMode.REASSIGN -> {
                                val targetId = reassignTargetId ?: return@TextButton
                                CategoryDeletionChoiceUiModel.ReassignTo(CategoryId(targetId))
                            }
                        }
                    onDelete(choice)
                }
            ) {
                Text(stringResource(R.string.delete_category_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DeleteChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    indent: Boolean = false
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = if (indent) OrganizerDimens.spacingLarge else OrganizerDimens.spacingNone)
                .semantics(mergeDescendants = true) { testTag = "delete-choice-$label" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private enum class DeleteMode {
    AUTOMATIC,
    REASSIGN
}
