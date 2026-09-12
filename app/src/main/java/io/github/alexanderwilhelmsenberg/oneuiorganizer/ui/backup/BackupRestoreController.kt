package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.backup

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerBackupRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupExport
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.PreparedOrganizerBackupImport
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentIoError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentReadResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentStore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentWriteResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupDocumentRequest
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreNotice
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreProblem
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreUiState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Activity-scoped backup/restore presentation controller.
 *
 * Pending export content and validated import state are intentionally ephemeral. The repository/store remains the
 * persistent source of truth, and Android URI/file handling stays behind [BackupDocumentStore].
 */
class BackupRestoreController(
    private val backupRepository: OrganizerBackupRepository,
    private val documentStore: BackupDocumentStore,
    private val scope: CoroutineScope
) {
    private val mutableUiState = MutableStateFlow(BackupRestoreUiState())
    private val documentRequestChannel = Channel<BackupDocumentRequest>(capacity = Channel.BUFFERED)
    private var pendingExport: OrganizerBackupExport? = null
    private var pendingImport: PreparedOrganizerBackupImport? = null

    val uiState = mutableUiState.asStateFlow()
    val documentRequests: Flow<BackupDocumentRequest> = documentRequestChannel.receiveAsFlow()

    fun requestExport() {
        scope.launch {
            beginOperation()
            when (val result = backupRepository.exportOrganizerBackup()) {
                is OrganizerBackupResult.Failure -> finishWithBackupError(result.error)
                is OrganizerBackupResult.Success -> {
                    pendingExport = result.value
                    mutableUiState.value = BackupRestoreUiState()
                    documentRequestChannel.send(
                        BackupDocumentRequest.Create(
                            suggestedFileName = result.value.suggestedFileName,
                            mimeType = result.value.mimeType
                        )
                    )
                }
            }
        }
    }

    fun onExportDocumentSelected(documentId: BackupDocumentId?) {
        val export = pendingExport ?: return
        if (documentId == null) {
            pendingExport = null
            mutableUiState.value = BackupRestoreUiState()
            return
        }

        scope.launch {
            mutableUiState.value = BackupRestoreUiState(isBusy = true)
            when (val result = documentStore.write(documentId, export.content)) {
                BackupDocumentWriteResult.Success -> {
                    pendingExport = null
                    mutableUiState.value = BackupRestoreUiState(notice = BackupRestoreNotice.EXPORTED)
                }

                is BackupDocumentWriteResult.Failure -> {
                    pendingExport = null
                    mutableUiState.value =
                        BackupRestoreUiState(problem = result.error.toWriteProblem())
                }
            }
        }
    }

    fun requestImport() {
        pendingImport = null
        mutableUiState.value = BackupRestoreUiState()
        scope.launch {
            documentRequestChannel.send(
                BackupDocumentRequest.Open(
                    mimeTypes = listOf(OrganizerBackupExport.MIME_TYPE, JSON_TEXT_MIME_TYPE)
                )
            )
        }
    }

    fun onImportDocumentSelected(documentId: BackupDocumentId?) {
        if (documentId == null) {
            return
        }

        scope.launch {
            mutableUiState.value = BackupRestoreUiState(isBusy = true)
            when (val readResult = documentStore.read(documentId)) {
                is BackupDocumentReadResult.Failure -> {
                    mutableUiState.value =
                        BackupRestoreUiState(problem = readResult.error.toReadProblem())
                }

                is BackupDocumentReadResult.Success -> {
                    when (val prepared = backupRepository.prepareOrganizerBackupImport(readResult.content)) {
                        is OrganizerBackupResult.Failure -> finishWithBackupError(prepared.error)
                        is OrganizerBackupResult.Success -> {
                            pendingImport = prepared.value
                            mutableUiState.value =
                                BackupRestoreUiState(
                                    pendingImportSummary = prepared.value.summary
                                )
                        }
                    }
                }
            }
        }
    }

    fun confirmImport() {
        val prepared = pendingImport ?: return
        scope.launch {
            mutableUiState.value = BackupRestoreUiState(isBusy = true)
            when (val result = backupRepository.importOrganizerBackup(prepared)) {
                is OrganizerBackupResult.Failure -> finishWithBackupError(result.error)
                is OrganizerBackupResult.Success -> {
                    pendingImport = null
                    mutableUiState.value = BackupRestoreUiState(notice = BackupRestoreNotice.IMPORTED)
                }
            }
        }
    }

    fun cancelImport() {
        pendingImport = null
        mutableUiState.value = BackupRestoreUiState()
    }

    fun clearFeedback() {
        mutableUiState.value = mutableUiState.value.copy(problem = null, notice = null)
    }

    private fun beginOperation() {
        pendingExport = null
        mutableUiState.value = BackupRestoreUiState(isBusy = true)
    }

    private fun finishWithBackupError(error: OrganizerBackupError) {
        mutableUiState.value = BackupRestoreUiState(problem = error.toProblem())
    }

    private fun OrganizerBackupError.toProblem(): BackupRestoreProblem =
        when (this) {
            is OrganizerBackupError.UnsupportedFormatVersion -> BackupRestoreProblem.UNSUPPORTED_BACKUP_VERSION
            OrganizerBackupError.PersistenceFailure -> BackupRestoreProblem.PERSISTENCE_FAILED
            else -> BackupRestoreProblem.INVALID_BACKUP
        }

    private fun BackupDocumentIoError.toReadProblem(): BackupRestoreProblem = BackupRestoreProblem.READ_FAILED

    private fun BackupDocumentIoError.toWriteProblem(): BackupRestoreProblem = BackupRestoreProblem.WRITE_FAILED

    private companion object {
        const val JSON_TEXT_MIME_TYPE = "text/json"
    }
}
