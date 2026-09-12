package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.management

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
    onPinCategory: (CategoryDefinition) -> Unit = {}
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
                    pinningSupported = pinningSupported
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
private fun ManagementHeader(onCreate: () -> Unit, onDismiss: () -> Unit, pinningSupported: Boolean) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        enabled = canMoveUp,
                        modifier =
                            Modifier.semantics {
                                contentDescription = moveUpDescription
                            },
                        onClick = onMoveUp
                    ) {
                        Text("↑")
                    }
                    TextButton(
                        enabled = canMoveDown,
                        modifier =
                            Modifier.semantics {
                                contentDescription = moveDownDescription
                            },
                        onClick = onMoveDown
                    ) {
                        Text("↓")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    enabled = pinningSupported,
                    onClick = onPin
                ) {
                    Text(stringResource(R.string.pin_category_to_home_screen))
                }
                if (item.isCustom) {
                    TextButton(onClick = onRename) {
                        Text(stringResource(R.string.rename_category))
                    }
                    TextButton(onClick = onDelete) {
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
    var draft by rememberSaveable(title, initialName) { mutableStateOf(initialName) }
    var localError by rememberSaveable(title, initialName) { mutableStateOf<String?>(null) }
    val emptyNameError = stringResource(R.string.category_name_required)
    val fieldDescription = stringResource(R.string.category_name_content_description)
    val submit = {
        val normalized = draft.trim()
        if (normalized.isEmpty()) {
            localError = emptyNameError
        } else {
            localError = null
            onConfirm(normalized)
        }
    }
    val displayedError = localError ?: externalError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    localError = null
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = fieldDescription
                        },
                singleLine = true,
                label = { Text(stringResource(R.string.category_name_label)) },
                isError = displayedError != null,
                supportingText = displayedError?.let { error -> { Text(error) } },
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                keyboardActions = KeyboardActions(onDone = { submit() })
            )
        },
        confirmButton = {
            TextButton(onClick = submit) {
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
    var reassignTargetId by rememberSaveable(item.category.id.value) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_category_title, item.category.displayName)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = OrganizerDimens.moveDialogMaxHeight)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)
            ) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.delete_populated_category_body,
                            item.assignedAppCount,
                            item.assignedAppCount
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
                DeleteChoiceRow(
                    selected = reassignTargetId == null,
                    label = stringResource(R.string.delete_category_automatic_option),
                    description = stringResource(R.string.delete_category_automatic_body),
                    onClick = { reassignTargetId = null }
                )

                if (reassignTargets.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(OrganizerDimens.spacingExtraSmall))
                    Text(
                        text = stringResource(R.string.delete_category_reassign_heading),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    reassignTargets.forEach { target ->
                        DeleteChoiceRow(
                            selected = reassignTargetId == target.category.id.value,
                            label =
                                stringResource(
                                    R.string.delete_category_reassign_option,
                                    target.category.displayName
                                ),
                            description =
                                stringResource(
                                    R.string.delete_category_reassign_body,
                                    target.category.displayName
                                ),
                            onClick = { reassignTargetId = target.category.id.value }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetId = reassignTargetId
                    onDelete(
                        if (targetId == null) {
                            CategoryDeletionChoiceUiModel.AutomaticClassification
                        } else {
                            CategoryDeletionChoiceUiModel.Reassign(CategoryId(targetId))
                        }
                    )
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
private fun DeleteChoiceRow(selected: Boolean, label: String, description: String, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton
                ).padding(vertical = OrganizerDimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.size(OrganizerDimens.spacingSmall))
        Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingExtraSmall)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
