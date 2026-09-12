package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupSummary
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreNotice
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreProblem
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerDimens

@Composable
fun BackupRestore(
    state: BackupRestoreUiState,
    onExportRequested: () -> Unit,
    onImportRequested: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingLarge)
        ) {
            item(key = "backup-restore-header") {
                BackupRestoreHeader(onDismiss = onDismiss)
            }

            state.problem?.let { problem ->
                item(key = "backup-restore-problem") {
                    FeedbackSurface(
                        message = backupProblemMessage(problem),
                        isError = true
                    )
                }
            }

            state.notice?.let { notice ->
                item(key = "backup-restore-notice") {
                    FeedbackSurface(
                        message = backupNoticeMessage(notice),
                        isError = false
                    )
                }
            }

            item(key = "backup-export") {
                BackupActionCard(
                    title = stringResource(R.string.backup_export_title),
                    body = stringResource(R.string.backup_export_body),
                    actionLabel = stringResource(R.string.backup_export_action),
                    enabled = !state.isBusy,
                    onClick = onExportRequested
                )
            }

            item(key = "backup-import") {
                BackupActionCard(
                    title = stringResource(R.string.backup_import_title),
                    body = stringResource(R.string.backup_import_body),
                    actionLabel = stringResource(R.string.backup_import_action),
                    enabled = !state.isBusy,
                    onClick = onImportRequested
                )
            }

            if (state.isBusy) {
                item(key = "backup-restore-progress") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    state.pendingImportSummary?.let { summary ->
        ImportConfirmationDialog(
            summary = summary,
            onConfirm = onConfirmImport,
            onDismiss = onCancelImport
        )
    }
}

@Composable
private fun BackupRestoreHeader(onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.backup_restore_title),
                style = MaterialTheme.typography.displaySmall
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.backup_restore_done))
            }
        }
        Text(
            text = stringResource(R.string.backup_restore_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BackupActionCard(title: String, body: String, actionLabel: String, enabled: Boolean, onClick: () -> Unit) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun FeedbackSurface(message: String, isError: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color =
            if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(OrganizerDimens.spacingLarge),
            color =
                if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ImportConfirmationDialog(summary: OrganizerBackupSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OrganizerDimens.spacingSmall)) {
                Text(stringResource(R.string.backup_confirm_body))
                Text(
                    pluralStringResource(
                        R.plurals.backup_summary_custom_categories,
                        summary.customCategoryCount,
                        summary.customCategoryCount
                    )
                )
                Text(
                    pluralStringResource(
                        R.plurals.backup_summary_overrides,
                        summary.overrideCount,
                        summary.overrideCount
                    )
                )
                Text(
                    pluralStringResource(
                        R.plurals.backup_summary_favourites,
                        summary.favouriteCount,
                        summary.favouriteCount
                    )
                )
                Text(
                    pluralStringResource(
                        R.plurals.backup_summary_hidden,
                        summary.hiddenCount,
                        summary.hiddenCount
                    )
                )
                Text(
                    text = stringResource(R.string.backup_confirm_warning),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.backup_confirm_action))
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
private fun backupProblemMessage(problem: BackupRestoreProblem): String = stringResource(
    when (problem) {
        BackupRestoreProblem.INVALID_BACKUP -> R.string.backup_problem_invalid
        BackupRestoreProblem.UNSUPPORTED_BACKUP_VERSION -> R.string.backup_problem_unsupported
        BackupRestoreProblem.READ_FAILED -> R.string.backup_problem_read
        BackupRestoreProblem.WRITE_FAILED -> R.string.backup_problem_write
        BackupRestoreProblem.PERSISTENCE_FAILED -> R.string.backup_problem_persistence
    }
)

@Composable
private fun backupNoticeMessage(notice: BackupRestoreNotice): String = stringResource(
    when (notice) {
        BackupRestoreNotice.EXPORTED -> R.string.backup_notice_exported
        BackupRestoreNotice.IMPORTED -> R.string.backup_notice_imported
    }
)
