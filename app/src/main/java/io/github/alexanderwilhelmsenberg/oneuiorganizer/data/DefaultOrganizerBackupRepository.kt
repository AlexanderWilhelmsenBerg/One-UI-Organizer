package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.backup.OrganizerBackupJsonCodec
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup.OrganizerBackupMapper
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup.OrganizerBackupValidator
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupExport
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupSummary
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.PreparedOrganizerBackupImport
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Portability boundary for organizer-owned state.
 *
 * This repository has no independent state cache. Integration must give it the same [OrganizerStateStore] used by
 * the organizer repository so a successful import becomes the normal persisted source of truth immediately.
 */
class DefaultOrganizerBackupRepository(
    private val organizerStateStore: OrganizerStateStore
) : OrganizerBackupRepository {
    override suspend fun exportOrganizerBackup(): OrganizerBackupResult<OrganizerBackupExport> = try {
        val state = organizerStateStore.state.first()
        val document = OrganizerBackupMapper.fromState(state)
        OrganizerBackupResult.Success(
            OrganizerBackupExport(
                content = OrganizerBackupJsonCodec.encode(document),
                summary = OrganizerBackupMapper.summary(state)
            )
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        OrganizerBackupResult.Failure(OrganizerBackupError.PersistenceFailure)
    }

    override fun prepareOrganizerBackupImport(
        encodedDocument: String
    ): OrganizerBackupResult<PreparedOrganizerBackupImport> =
        when (val decoded = OrganizerBackupJsonCodec.decode(encodedDocument)) {
            is OrganizerBackupResult.Failure -> decoded
            is OrganizerBackupResult.Success -> OrganizerBackupValidator.prepare(decoded.value)
        }

    override suspend fun importOrganizerBackup(
        preparedImport: PreparedOrganizerBackupImport
    ): OrganizerBackupResult<OrganizerBackupSummary> = try {
        organizerStateStore.update { preparedImport.organizerState }
        OrganizerBackupResult.Success(preparedImport.summary)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        OrganizerBackupResult.Failure(OrganizerBackupError.PersistenceFailure)
    }
}
