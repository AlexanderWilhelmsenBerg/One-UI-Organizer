package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.backup

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DefaultOrganizerBackupRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerStateStore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.backup.OrganizerBackupJsonCodec
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup.OrganizerBackupMapper
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentReadResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentStore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentWriteResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupDocumentRequest
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreNotice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BackupRestoreControllerTest {
    @Test
    fun `export requests create document and writes only after a destination is selected`() = runBlocking {
        val state = portableState()
        val documents = FakeBackupDocumentStore()
        val controller =
            BackupRestoreController(
                backupRepository = DefaultOrganizerBackupRepository(FakeOrganizerStateStore(state)),
                documentStore = documents,
                scope = this
            )
        val request = async { controller.documentRequests.first() }

        controller.requestExport()

        val create = assertIs<BackupDocumentRequest.Create>(request.await())
        assertEquals("one-ui-organizer-backup.json", create.suggestedFileName)
        assertEquals("application/json", create.mimeType)
        assertEquals(null, documents.lastWrittenContent)

        controller.onExportDocumentSelected(BackupDocumentId("content://backup/export"))
        val completed = controller.uiState.first { state -> state.notice != null }

        assertEquals(BackupRestoreNotice.EXPORTED, completed.notice)
        assertEquals(
            OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(state)),
            documents.lastWrittenContent
        )
    }

    @Test
    fun `import validates and summarizes before explicit confirmation then replaces state`() = runBlocking {
        val current = OrganizerState(favouriteAppIds = setOf(AppId("current.app")))
        val imported = portableState()
        val documents =
            FakeBackupDocumentStore(
                content = OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(imported))
            )
        val store = FakeOrganizerStateStore(current)
        val controller =
            BackupRestoreController(
                backupRepository = DefaultOrganizerBackupRepository(store),
                documentStore = documents,
                scope = this
            )
        val request = async { controller.documentRequests.first() }

        controller.requestImport()

        assertIs<BackupDocumentRequest.Open>(request.await())
        controller.onImportDocumentSelected(BackupDocumentId("content://backup/import"))
        val pending = controller.uiState.first { state -> state.pendingImportSummary != null }
        assertEquals(1, pending.pendingImportSummary?.customCategoryCount)
        assertEquals(1, pending.pendingImportSummary?.overrideCount)
        assertEquals(current.normalized(), store.state.first())

        controller.confirmImport()
        val completed = controller.uiState.first { state -> state.notice == BackupRestoreNotice.IMPORTED }

        assertEquals(BackupRestoreNotice.IMPORTED, completed.notice)
        assertEquals(imported.normalized(), store.state.first())
    }

    @Test
    fun `cancelled import confirmation leaves organizer state unchanged`() = runBlocking {
        val current = OrganizerState(favouriteAppIds = setOf(AppId("current.app")))
        val imported = portableState()
        val documents =
            FakeBackupDocumentStore(
                content = OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(imported))
            )
        val store = FakeOrganizerStateStore(current)
        val controller =
            BackupRestoreController(
                backupRepository = DefaultOrganizerBackupRepository(store),
                documentStore = documents,
                scope = this
            )

        controller.onImportDocumentSelected(BackupDocumentId("content://backup/import"))
        controller.uiState.first { state -> state.pendingImportSummary != null }
        controller.cancelImport()

        assertEquals(null, controller.uiState.value.pendingImportSummary)
        assertEquals(current.normalized(), store.state.first())
    }

    private fun portableState(): OrganizerState {
        val customId = CategoryId.custom("controller-stable")
        return OrganizerState(
            categoryOverrides = mapOf(AppId("example.uninstalled") to customId),
            favouriteAppIds = setOf(AppId("example.uninstalled")),
            hiddenAppIds = setOf(AppId("example.uninstalled")),
            customCategories = listOf(CustomCategoryDefinition(customId, "Controller")),
            categoryOrder = listOf(customId) + OrganizerState.defaultBuiltInCategoryOrder()
        )
    }

    private class FakeOrganizerStateStore(initialState: OrganizerState) : OrganizerStateStore {
        private val mutableState = MutableStateFlow(initialState.normalized())
        override val state: Flow<OrganizerState> = mutableState

        override suspend fun update(transform: (OrganizerState) -> OrganizerState): OrganizerState {
            val updated =
                transform(mutableState.value)
                    .copy(schemaVersion = OrganizerState.CURRENT_SCHEMA_VERSION)
                    .normalized()
            mutableState.value = updated
            return updated
        }
    }

    private class FakeBackupDocumentStore(private val content: String = "") : BackupDocumentStore {
        var lastWrittenContent: String? = null
            private set

        override suspend fun read(documentId: BackupDocumentId): BackupDocumentReadResult =
            BackupDocumentReadResult.Success(content)

        override suspend fun write(documentId: BackupDocumentId, content: String): BackupDocumentWriteResult {
            lastWrittenContent = content
            return BackupDocumentWriteResult.Success
        }
    }
}
