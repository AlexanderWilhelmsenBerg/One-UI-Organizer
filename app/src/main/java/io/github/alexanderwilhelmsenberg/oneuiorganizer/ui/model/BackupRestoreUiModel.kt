package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupSummary

data class BackupRestoreUiState(
    val isBusy: Boolean = false,
    val pendingImportSummary: OrganizerBackupSummary? = null,
    val problem: BackupRestoreProblem? = null,
    val notice: BackupRestoreNotice? = null
)

enum class BackupRestoreProblem {
    INVALID_BACKUP,
    UNSUPPORTED_BACKUP_VERSION,
    READ_FAILED,
    WRITE_FAILED,
    PERSISTENCE_FAILED
}

enum class BackupRestoreNotice {
    EXPORTED,
    IMPORTED
}

sealed interface BackupDocumentRequest {
    data class Create(val suggestedFileName: String, val mimeType: String) : BackupDocumentRequest

    data class Open(val mimeTypes: List<String>) : BackupDocumentRequest
}
