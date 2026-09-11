package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
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
    fun `empty store starts at current schema`() = runBlocking {
        withStore { store ->
            assertEquals(OrganizerState(), store.state.first())
            assertEquals(2, store.state.first().schemaVersion)
        }
    }

    @Test
    fun `category override persists by durable identity across store recreation`() = runBlocking {
        val appId = AppId("example.override")
        withRecreatedStore(
            update = { store ->
                store.update { state ->
                    state.copy(categoryOverrides = mapOf(appId to AppCategory.WORK.id))
                }
            },
            verify = { state ->
                assertEquals(AppCategory.WORK.id, state.categoryOverrides[appId])
            }
        )
    }

    @Test
    fun `custom categories and order round trip`() = runBlocking {
        val custom = CustomCategoryDefinition(CategoryId.custom("reading-list"), "Reading List")
        val order = listOf(custom.id, AppCategory.WORK.id) +
            OrganizerState.defaultBuiltInCategoryOrder().filterNot { id -> id == AppCategory.WORK.id }
        val expected =
            OrganizerState(
                categoryOverrides = mapOf(AppId("example.custom") to custom.id),
                customCategories = listOf(custom),
                categoryOrder = order
            )

        withRecreatedStore(
            update = { store -> store.update { expected } },
            verify = { state -> assertEquals(expected, state) }
        )
    }

    @Test
    fun `favourite and hidden state persist across store recreation`() = runBlocking {
        val favouriteId = AppId("example.favourite")
        val hiddenId = AppId("example.hidden")
        withRecreatedStore(
            update = { store ->
                store.update { state ->
                    state.copy(
                        favouriteAppIds = setOf(favouriteId),
                        hiddenAppIds = setOf(hiddenId)
                    )
                }
            },
            verify = { state ->
                assertTrue(favouriteId in state.favouriteAppIds)
                assertTrue(hiddenId in state.hiddenAppIds)
            }
        )
    }

    @Test
    fun `literal schema v1 payload migrates without user state loss`() = runBlocking {
        val directory = Files.createTempDirectory("organizer-state-v1-migration")
        val file = directory.resolve("organizer-state.json").toFile()
        file.writeText(
            """
            {
              "schemaVersion": 1,
              "categoryOverrides": {
                "example.work": "WORK",
                "example.reading": "READING"
              },
              "favouriteAppIds": ["example.favourite"],
              "hiddenAppIds": ["example.hidden"]
            }
            """.trimIndent()
        )

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        try {
            val store = DataStoreOrganizerStateStore.create(file = file, scope = scope)
            val migrated = store.state.first()

            assertEquals(OrganizerState.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
            assertEquals(AppCategory.WORK.id, migrated.categoryOverrides[AppId("example.work")])
            assertEquals(AppCategory.READING.id, migrated.categoryOverrides[AppId("example.reading")])
            assertTrue(AppId("example.favourite") in migrated.favouriteAppIds)
            assertTrue(AppId("example.hidden") in migrated.hiddenAppIds)
            assertTrue(migrated.customCategories.isEmpty())
            assertEquals(OrganizerState.defaultBuiltInCategoryOrder(), migrated.categoryOrder)

            val postMigrationFavourite = AppId("example.after-migration")
            store.update { state ->
                state.copy(favouriteAppIds = state.favouriteAppIds + postMigrationFavourite)
            }
            val rewritten = file.readText()
            assertTrue(rewritten.contains("\"schemaVersion\":2"))
            assertTrue(rewritten.contains("\"example.work\":\"builtin:work\""))
            assertTrue(rewritten.contains("\"customCategories\":[]"))
            assertTrue(rewritten.contains("\"categoryOrder\""))
            assertTrue(postMigrationFavourite in store.state.first().favouriteAppIds)
            assertTrue(AppId("example.favourite") in store.state.first().favouriteAppIds)
            assertTrue(AppId("example.hidden") in store.state.first().hiddenAppIds)
        } finally {
            job.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `schema v2 duplicate and stale order ids normalize deterministically`() = runBlocking {
        val directory = Files.createTempDirectory("organizer-state-order-normalization")
        val file = directory.resolve("organizer-state.json").toFile()
        file.writeText(
            """
            {
              "schemaVersion": 2,
              "categoryOverrides": {},
              "favouriteAppIds": [],
              "hiddenAppIds": [],
              "customCategories": [{"id":"custom:alpha","name":"Alpha"}],
              "categoryOrder": [
                "custom:alpha",
                "builtin:work",
                "custom:alpha",
                "custom:stale",
                "builtin:work"
              ]
            }
            """.trimIndent()
        )

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        try {
            val state = DataStoreOrganizerStateStore.create(file = file, scope = scope).state.first()
            val expectedPrefix = listOf(CategoryId.custom("alpha"), AppCategory.WORK.id)

            assertEquals(expectedPrefix, state.categoryOrder.take(expectedPrefix.size))
            assertEquals(state.categoryOrder.distinct(), state.categoryOrder)
            assertFalse(CategoryId.custom("stale") in state.categoryOrder)
            assertEquals(
                OrganizerState.defaultBuiltInCategoryOrder().toSet() + CategoryId.custom("alpha"),
                state.categoryOrder.toSet()
            )
        } finally {
            job.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
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
