package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupExport
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupSummary
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.PreparedOrganizerBackupImport

interface OrganizerBackupRepository {
    suspend fun exportOrganizerBackup(): OrganizerBackupResult<OrganizerBackupExport>

    fun prepareOrganizerBackupImport(encodedDocument: String): OrganizerBackupResult<PreparedOrganizerBackupImport>

    suspend fun importOrganizerBackup(
        preparedImport: PreparedOrganizerBackupImport
    ): OrganizerBackupResult<OrganizerBackupSummary>
}
