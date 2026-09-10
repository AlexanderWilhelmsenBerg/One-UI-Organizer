package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DataStoreOrganizerStateStoreTest {
    @Test
    fun `empty store starts at schema version one`() = runBlocking {
        withStore { store ->
            assertEquals(OrganizerState(), store.state.first())
        }
    }

    @Test
    fun `category override persists across store recreation`() = runBlocking {
        val appId = AppId("example.override")
        withRecreatedStore(
            update = { store ->
                store.update { state ->
                    state.copy(categoryOverrides = mapOf(appId to AppCategory.WORK))
                }
            },
            verify = { state ->
                assertEquals(AppCategory.WORK, state.categoryOverrides[appId])
            }
        )
    }

    @Test
    fun `favourite persists across store recreation`() = runBlocking {
        val appId = AppId("example.favourite")
        withRecreatedStore(
            update = { store ->
                store.update { state ->
                    state.copy(favouriteAppIds = setOf(appId))
                }
            },
            verify = { state ->
                assertTrue(appId in state.favouriteAppIds)
            }
        )
    }

    @Test
    fun `hidden state persists across store recreation`() = runBlocking {
        val appId = AppId("example.hidden")
        withRecreatedStore(
            update = { store ->
                store.update { state ->
                    state.copy(hiddenAppIds = setOf(appId))
                }
            },
            verify = { state ->
                assertTrue(appId in state.hiddenAppIds)
            }
        )
    }

    @Test
    fun `all schema v1 state round trips across store recreation`() = runBlocking {
        val overrideId = AppId("example.override")
        val favouriteId = AppId("example.favourite")
        val hiddenId = AppId("example.hidden")
        val expected =
            OrganizerState(
                categoryOverrides = mapOf(overrideId to AppCategory.PRODUCTIVITY),
                favouriteAppIds = setOf(favouriteId),
                hiddenAppIds = setOf(hiddenId)
            )

        withRecreatedStore(
            update = { store -> store.update { expected } },
            verify = { state -> assertEquals(expected, state) }
        )
    }

    @Test
    fun `concurrent updates are atomic`() = runBlocking {
        withStore { store ->
            coroutineScope {
                repeat(40) { index ->
                    launch(Dispatchers.Default) {
                        val appId = AppId("example.concurrent.$index")
                        store.update { state ->
                            state.copy(favouriteAppIds = state.favouriteAppIds + appId)
                        }
                    }
                }
            }

            val state = store.state.first()
            assertEquals(40, state.favouriteAppIds.size)
        }
    }

    @Test
    fun `corrupt state recovers to default and remains writable`() = runBlocking {
        val directory = Files.createTempDirectory("organizer-state-corrupt")
        val file = directory.resolve("organizer-state.json").toFile()
        file.writeText("{ definitely-not-valid-json")

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        try {
            val store = DataStoreOrganizerStateStore.create(file = file, scope = scope)
            assertEquals(OrganizerState(), store.state.first())

            val appId = AppId("example.after-recovery")
            store.update { state -> state.copy(hiddenAppIds = setOf(appId)) }

            assertTrue(appId in store.state.first().hiddenAppIds)
            assertFalse(file.readText().contains("definitely-not-valid-json"))
        } finally {
            job.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
    }

    private suspend fun withRecreatedStore(
        update: suspend (DataStoreOrganizerStateStore) -> Unit,
        verify: (OrganizerState) -> Unit
    ) {
        val directory = Files.createTempDirectory("organizer-state-recreated")
        val file = directory.resolve("organizer-state.json").toFile()

        val firstJob = SupervisorJob()
        val firstScope = CoroutineScope(Dispatchers.IO + firstJob)
        try {
            val firstStore = DataStoreOrganizerStateStore.create(file = file, scope = firstScope)
            update(firstStore)
        } finally {
            firstJob.cancelAndJoin()
        }

        val secondJob = SupervisorJob()
        val secondScope = CoroutineScope(Dispatchers.IO + secondJob)
        try {
            val secondStore = DataStoreOrganizerStateStore.create(file = file, scope = secondScope)
            verify(secondStore.state.first())
        } finally {
            secondJob.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
    }

    private suspend fun withStore(block: suspend (DataStoreOrganizerStateStore) -> Unit) {
        val directory = Files.createTempDirectory("organizer-state")
        val file = directory.resolve("organizer-state.json").toFile()
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        try {
            val store = DataStoreOrganizerStateStore.create(file = file, scope = scope)
            block(store)
        } finally {
            job.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
    }
}
