package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.backup.OrganizerBackupJsonCodec
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup.OrganizerBackupMapper
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultOrganizerBackupRepositoryTest {
    @Test
    fun `export reports sanitized summary and deterministic portable content`() = runBlocking {
        val expected = portableState()
        val repository = DefaultOrganizerBackupRepository(FakeOrganizerStateStore(expected))

        val first = assertSuccess(repository.exportOrganizerBackup())
        val second = assertSuccess(repository.exportOrganizerBackup())

        assertEquals(first.content, second.content)
        assertEquals("one-ui-organizer-backup.json", first.suggestedFileName)
        assertEquals("application/json", first.mimeType)
        assertEquals(1, first.summary.customCategoryCount)
        assertEquals(2, first.summary.overrideCount)
        assertEquals(2, first.summary.favouriteCount)
        assertEquals(1, first.summary.hiddenCount)
        assertEquals(
            expected.normalized(),
            assertSuccess(
                io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.backup.OrganizerBackupValidator.prepare(
                    assertSuccess(OrganizerBackupJsonCodec.decode(first.content))
                )
            ).organizerState
        )
    }

    @Test
    fun `validated import atomically replaces non empty organizer state`() = runBlocking {
        val current =
            OrganizerState(
                categoryOverrides = mapOf(AppId("current.app") to AppCategory.GAMES.id),
                favouriteAppIds = setOf(AppId("current.favourite")),
                hiddenAppIds = setOf(AppId("current.hidden"))
            )
        val expected = portableState()
        val store = FakeOrganizerStateStore(current)
        val repository = DefaultOrganizerBackupRepository(store)
        val encoded = OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(expected))
        val prepared = assertSuccess(repository.prepareOrganizerBackupImport(encoded))

        val summary = assertSuccess(repository.importOrganizerBackup(prepared))

        assertEquals(1, store.updateAttempts)
        assertEquals(expected.normalized(), store.state.first())
        assertEquals(expected.customCategories.size, summary.customCategoryCount)
        assertEquals(expected.categoryOverrides.size, summary.overrideCount)
        assertTrue(AppId("current.app") !in store.state.first().categoryOverrides)
    }

    @Test
    fun `persistence failure leaves current state unchanged`() = runBlocking {
        val current =
            OrganizerState(
                categoryOverrides = mapOf(AppId("current.app") to AppCategory.READING.id),
                favouriteAppIds = setOf(AppId("current.favourite"))
            )
        val store = FailingOrganizerStateStore(current)
        val repository = DefaultOrganizerBackupRepository(store)
        val encoded = OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(portableState()))
        val prepared = assertSuccess(repository.prepareOrganizerBackupImport(encoded))

        val error = assertFailure(repository.importOrganizerBackup(prepared))

        assertEquals(OrganizerBackupError.PersistenceFailure, error)
        assertEquals(current.normalized(), store.state.first())
    }

    @Test
    fun `successful import survives process recreation with retained uninstalled state`() = runBlocking {
        val directory = Files.createTempDirectory("organizer-backup-recreation")
        val file = directory.resolve("organizer-state.json").toFile()
        val expected = portableState()
        val encoded = OrganizerBackupJsonCodec.encode(OrganizerBackupMapper.fromState(expected))
        val firstJob = SupervisorJob()
        val firstScope = CoroutineScope(Dispatchers.IO + firstJob)
        try {
            val firstStore = DataStoreOrganizerStateStore.create(file = file, scope = firstScope)
            val firstRepository = DefaultOrganizerBackupRepository(firstStore)
            val prepared = assertSuccess(firstRepository.prepareOrganizerBackupImport(encoded))
            assertSuccess(firstRepository.importOrganizerBackup(prepared))
        } finally {
            firstJob.cancelAndJoin()
        }

        val secondJob = SupervisorJob()
        val secondScope = CoroutineScope(Dispatchers.IO + secondJob)
        try {
            val recreated = DataStoreOrganizerStateStore.create(file = file, scope = secondScope).state.first()
            assertEquals(expected.normalized(), recreated)
            val customId = expected.customCategories.single().id
            assertEquals(customId, recreated.categoryOverrides[AppId("example.uninstalled")])
            assertEquals(customId, recreated.customCategories.single().id)
            assertEquals(customId, recreated.categoryOrder.first())
        } finally {
            secondJob.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
    }

    private fun portableState(): OrganizerState {
        val customId = CategoryId.custom("stable-portable-id")
        return OrganizerState(
            categoryOverrides =
                mapOf(
                    AppId("example.installed") to AppCategory.WORK.id,
                    AppId("example.uninstalled") to customId
                ),
            favouriteAppIds = setOf(AppId("example.installed"), AppId("example.uninstalled")),
            hiddenAppIds = setOf(AppId("example.uninstalled")),
            customCategories = listOf(CustomCategoryDefinition(customId, "Portable")),
            categoryOrder = listOf(customId) + OrganizerState.defaultBuiltInCategoryOrder()
        )
    }

    private fun <T> assertSuccess(result: OrganizerBackupResult<T>): T = when (result) {
        is OrganizerBackupResult.Success -> result.value
        is OrganizerBackupResult.Failure -> fail("Expected success but got ${result.error}.")
    }

    private fun assertFailure(result: OrganizerBackupResult<*>): OrganizerBackupError = when (result) {
        is OrganizerBackupResult.Failure -> result.error
        is OrganizerBackupResult.Success -> fail("Expected failure but got ${result.value}.")
    }

    private class FakeOrganizerStateStore(initialState: OrganizerState) : OrganizerStateStore {
        private val mutableState = MutableStateFlow(initialState.normalized())
        private val mutex = Mutex()
        var updateAttempts: Int = 0
            private set

        override val state: Flow<OrganizerState> = mutableState

        override suspend fun update(transform: (OrganizerState) -> OrganizerState): OrganizerState = mutex.withLock {
            updateAttempts += 1
            val updated =
                transform(mutableState.value)
                    .copy(schemaVersion = OrganizerState.CURRENT_SCHEMA_VERSION)
                    .normalized()
            mutableState.value = updated
            updated
        }
    }

    private class FailingOrganizerStateStore(initialState: OrganizerState) : OrganizerStateStore {
        private val mutableState = MutableStateFlow(initialState.normalized())
        override val state: Flow<OrganizerState> = mutableState

        override suspend fun update(transform: (OrganizerState) -> OrganizerState): OrganizerState {
            transform(mutableState.value)
            throw IllegalStateException("simulated persistence failure")
        }
    }
}
