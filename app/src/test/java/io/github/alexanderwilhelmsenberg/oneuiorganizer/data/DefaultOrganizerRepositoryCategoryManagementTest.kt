package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryDeletionPolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementError
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryManagementResult
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryNamePolicy
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryOrderProblem
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CustomCategoryIdGenerator
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.LocalAppSearch
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.InstalledAppSource
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultOrganizerRepositoryCategoryManagementTest {
    @Test
    fun `create trims name generates durable identity appends order and uses one atomic update`() = runBlocking {
        val generatedId = CategoryId.custom("created-once")
        val store = FakeOrganizerStateStore()
        val repository = repository(store = store, generatedIds = listOf(generatedId))

        val created = assertSuccess(repository.createCustomCategory("  Travel Plans  "))

        assertEquals(generatedId, created.id)
        assertEquals("Travel Plans", created.displayName)
        assertEquals(1, store.updateAttempts)
        val state = store.state.first()
        assertEquals(listOf(created), state.customCategories)
        assertEquals(generatedId, state.categoryOrder.last())
        assertEquals(
            OrganizerState.defaultBuiltInCategoryOrder() + generatedId,
            state.categoryOrder
        )
    }

    @Test
    fun `create rejects blank long duplicate and built in names without changing state`() = runBlocking {
        val existing = CustomCategoryDefinition(CategoryId.custom("weekend"), "Straße")
        val initial = OrganizerState(customCategories = listOf(existing))
        val store = FakeOrganizerStateStore(initial)
        val repository =
            repository(
                store = store,
                generatedIds = listOf(
                    CategoryId.custom("blank"),
                    CategoryId.custom("long"),
                    CategoryId.custom("duplicate"),
                    CategoryId.custom("built-in-collision")
                )
            )

        assertEquals(
            CategoryManagementError.BlankName,
            assertFailure(repository.createCustomCategory("   "))
        )
        assertEquals(
            CategoryManagementError.NameTooLong(CategoryNamePolicy.MAX_CODE_POINTS),
            assertFailure(repository.createCustomCategory("x".repeat(CategoryNamePolicy.MAX_CODE_POINTS + 1)))
        )
        assertEquals(
            CategoryManagementError.DuplicateName(existing.id, "STRASSE"),
            assertFailure(repository.createCustomCategory("STRASSE"))
        )
        assertEquals(
            CategoryManagementError.DuplicateName(AppCategory.WORK.id, "work"),
            assertFailure(repository.createCustomCategory(" work "))
        )
        assertEquals(initial.normalized(), store.state.first())
    }

    @Test
    fun `create rejects non custom or already referenced generated identity`() = runBlocking {
        val existing = CustomCategoryDefinition(CategoryId.custom("existing"), "Existing")
        val staleReferenced = CategoryId.custom("stale-reference")
        val initial =
            OrganizerState(
                categoryOverrides = mapOf(AppId("example.stale") to staleReferenced),
                customCategories = listOf(existing)
            )
        val repository =
            repository(
                store = FakeOrganizerStateStore(initial),
                generatedIds = listOf(AppCategory.WORK.id, existing.id, staleReferenced)
            )

        assertEquals(
            CategoryManagementError.InvalidGeneratedCategoryId(AppCategory.WORK.id),
            assertFailure(repository.createCustomCategory("One"))
        )
        assertEquals(
            CategoryManagementError.CategoryIdAlreadyExists(existing.id),
            assertFailure(repository.createCustomCategory("Two"))
        )
        assertEquals(
            CategoryManagementError.CategoryIdAlreadyExists(staleReferenced),
            assertFailure(repository.createCustomCategory("Three"))
        )
    }

    @Test
    fun `rename changes display metadata only and preserves override favourite hidden and order`() = runBlocking {
        val appId = AppId("example.renamed")
        val automaticId = AppId("example.automatic")
        val custom = CustomCategoryDefinition(CategoryId.custom("stable-id"), "Old Name")
        val initialOrder = listOf(custom.id) + OrganizerState.defaultBuiltInCategoryOrder()
        val initial =
            OrganizerState(
                categoryOverrides = mapOf(appId to custom.id),
                favouriteAppIds = setOf(appId),
                hiddenAppIds = setOf(appId),
                customCategories = listOf(custom),
                categoryOrder = initialOrder
            )
        val store = FakeOrganizerStateStore(initial)
        val repository =
            repository(
                store = store,
                installedApps = mutableListOf(installedApp(appId), installedApp(automaticId)),
                automaticCategories =
                    mutableMapOf(
                        appId to AppCategory.GAMES,
                        automaticId to AppCategory.READING
                    )
            )
        repository.refresh()

        val renamed = assertSuccess(repository.renameCustomCategory(custom.id, "  New Name  "))

        assertEquals(custom.id, renamed.id)
        assertEquals("New Name", renamed.displayName)
        val state = store.state.first()
        assertEquals(custom.id, state.categoryOverrides[appId])
        assertTrue(appId in state.favouriteAppIds)
        assertTrue(appId in state.hiddenAppIds)
        assertEquals(initialOrder, state.categoryOrder)
        assertEquals(listOf(renamed), state.customCategories)

        val categorized = repository.apps.first()
        assertEquals(renamed, categorized.single { it.app.id == appId }.category)
        assertEquals(
            ClassificationSource.USER_OVERRIDE,
            categorized.single { it.app.id == appId }.source
        )
        assertEquals(AppCategory.READING, categorized.single { it.app.id == automaticId }.category)
    }

    @Test
    fun `built in categories cannot be renamed or deleted`() = runBlocking {
        val store = FakeOrganizerStateStore()
        val repository = repository(store = store)

        assertEquals(
            CategoryManagementError.BuiltInCategoryImmutable(AppCategory.WORK.id),
            assertFailure(repository.renameCustomCategory(AppCategory.WORK.id, "Career"))
        )
        assertEquals(
            CategoryManagementError.BuiltInCategoryImmutable(AppCategory.WORK.id),
            assertFailure(
                repository.deleteCustomCategory(
                    AppCategory.WORK.id,
                    CategoryDeletionPolicy.ReturnToAutomatic
                )
            )
        )
        assertEquals(OrganizerState(), store.state.first())
    }

    @Test
    fun `rename and delete return not found for unknown custom identity`() = runBlocking {
        val unknown = CategoryId.custom("missing")
        val repository = repository(store = FakeOrganizerStateStore())

        assertEquals(
            CategoryManagementError.CategoryNotFound(unknown),
            assertFailure(repository.renameCustomCategory(unknown, "Missing"))
        )
        assertEquals(
            CategoryManagementError.CategoryNotFound(unknown),
            assertFailure(
                repository.deleteCustomCategory(
                    unknown,
                    CategoryDeletionPolicy.ReturnToAutomatic
                )
            )
        )
    }

    @Test
    fun `delete empty custom category removes definition and order atomically`() = runBlocking {
        val custom = CustomCategoryDefinition(CategoryId.custom("empty"), "Empty")
        val initial = OrganizerState(customCategories = listOf(custom))
        val store = FakeOrganizerStateStore(initial)
        val repository = repository(store = store)

        assertSuccess(
            repository.deleteCustomCategory(
                custom.id,
                CategoryDeletionPolicy.ReturnToAutomatic
            )
        )

        val state = store.state.first()
        assertTrue(state.customCategories.isEmpty())
        assertFalse(custom.id in state.categoryOrder)
        assertEquals(1, store.updateAttempts)
    }

    @Test
    fun `delete populated category can reassign overrides and preserves unrelated user state`() = runBlocking {
        val firstApp = AppId("example.first")
        val secondApp = AppId("example.second")
        val unrelatedApp = AppId("example.unrelated")
        val custom = CustomCategoryDefinition(CategoryId.custom("source"), "Source")
        val initial =
            OrganizerState(
                categoryOverrides =
                    mapOf(
                        firstApp to custom.id,
                        secondApp to custom.id,
                        unrelatedApp to AppCategory.READING.id
                    ),
                favouriteAppIds = setOf(firstApp),
                hiddenAppIds = setOf(secondApp),
                customCategories = listOf(custom)
            )
        val store = FakeOrganizerStateStore(initial)
        val repository = repository(store = store)

        assertSuccess(
            repository.deleteCustomCategory(
                custom.id,
                CategoryDeletionPolicy.Reassign(AppCategory.WORK.id)
            )
        )

        val state = store.state.first()
        assertEquals(AppCategory.WORK.id, state.categoryOverrides[firstApp])
        assertEquals(AppCategory.WORK.id, state.categoryOverrides[secondApp])
        assertEquals(AppCategory.READING.id, state.categoryOverrides[unrelatedApp])
        assertTrue(firstApp in state.favouriteAppIds)
        assertTrue(secondApp in state.hiddenAppIds)
        assertTrue(state.customCategories.isEmpty())
        assertFalse(custom.id in state.categoryOrder)
    }

    @Test
    fun `delete populated category can return apps to automatic classification`() = runBlocking {
        val appId = AppId("example.automatic-again")
        val custom = CustomCategoryDefinition(CategoryId.custom("temporary"), "Temporary")
        val store =
            FakeOrganizerStateStore(
                OrganizerState(
                    categoryOverrides = mapOf(appId to custom.id),
                    favouriteAppIds = setOf(appId),
                    hiddenAppIds = setOf(appId),
                    customCategories = listOf(custom)
                )
            )
        val repository =
            repository(
                store = store,
                installedApps = mutableListOf(installedApp(appId)),
                automaticCategories = mutableMapOf(appId to AppCategory.READING)
            )
        repository.refresh()
        assertEquals(custom, repository.apps.first().single().category)

        assertSuccess(
            repository.deleteCustomCategory(
                custom.id,
                CategoryDeletionPolicy.ReturnToAutomatic
            )
        )

        val state = store.state.first()
        assertFalse(appId in state.categoryOverrides)
        assertTrue(appId in state.favouriteAppIds)
        assertTrue(appId in state.hiddenAppIds)
        val categorized = repository.apps.first().single()
        assertEquals(AppCategory.READING, categorized.category)
        assertEquals(ClassificationSource.KNOWN_APP_RULE, categorized.source)
    }

    @Test
    fun `delete rejects self and unknown reassignment destinations without partial mutation`() = runBlocking {
        val appId = AppId("example.source")
        val custom = CustomCategoryDefinition(CategoryId.custom("source"), "Source")
        val initial =
            OrganizerState(
                categoryOverrides = mapOf(appId to custom.id),
                customCategories = listOf(custom)
            ).normalized()
        val store = FakeOrganizerStateStore(initial)
        val repository = repository(store = store)

        assertEquals(
            CategoryManagementError.InvalidReassignmentDestination(custom.id),
            assertFailure(
                repository.deleteCustomCategory(
                    custom.id,
                    CategoryDeletionPolicy.Reassign(custom.id)
                )
            )
        )
        val unknown = CategoryId.custom("unknown-destination")
        assertEquals(
            CategoryManagementError.InvalidReassignmentDestination(unknown),
            assertFailure(
                repository.deleteCustomCategory(
                    custom.id,
                    CategoryDeletionPolicy.Reassign(unknown)
                )
            )
        )
        assertEquals(initial, store.state.first())
    }

    @Test
    fun `reorder accepts exact built in and custom set and persists requested order`() = runBlocking {
        val first = CustomCategoryDefinition(CategoryId.custom("first"), "First")
        val second = CustomCategoryDefinition(CategoryId.custom("second"), "Second")
        val store = FakeOrganizerStateStore(OrganizerState(customCategories = listOf(first, second)))
        val repository = repository(store = store)
        val currentIds = store.state.first().orderedCategories().map { category -> category.id }
        val requested = listOf(second.id, AppCategory.WORK.id) +
            currentIds.filterNot { categoryId -> categoryId == second.id || categoryId == AppCategory.WORK.id }

        val result = assertSuccess(repository.reorderCategories(requested))

        assertEquals(requested, result)
        assertEquals(requested, store.state.first().categoryOrder)
        assertEquals(currentIds.toSet(), requested.toSet())
    }

    @Test
    fun `reorder rejects duplicate unknown and missing ids deterministically`() = runBlocking {
        val custom = CustomCategoryDefinition(CategoryId.custom("custom"), "Custom")
        val initial = OrganizerState(customCategories = listOf(custom)).normalized()
        val store = FakeOrganizerStateStore(initial)
        val repository = repository(store = store)
        val valid = initial.orderedCategories().map { category -> category.id }

        assertEquals(
            CategoryManagementError.InvalidOrder(CategoryOrderProblem.DUPLICATE_ID, valid.first()),
            assertFailure(repository.reorderCategories(valid + valid.first()))
        )

        val unknown = CategoryId.custom("unknown")
        assertEquals(
            CategoryManagementError.InvalidOrder(CategoryOrderProblem.UNKNOWN_ID, unknown),
            assertFailure(repository.reorderCategories(listOf(unknown) + valid.drop(1)))
        )

        assertEquals(
            CategoryManagementError.InvalidOrder(CategoryOrderProblem.MISSING_ID, valid.last()),
            assertFailure(repository.reorderCategories(valid.dropLast(1)))
        )
        assertEquals(initial, store.state.first())
    }

    @Test
    fun `custom override outranks automatic classification and custom display name is searchable`() = runBlocking {
        val appId = AppId("example.custom-search")
        val store = FakeOrganizerStateStore()
        val repository =
            repository(
                store = store,
                generatedIds = listOf(CategoryId.custom("weekend")),
                installedApps = mutableListOf(installedApp(appId)),
                automaticCategories = mutableMapOf(appId to AppCategory.WORK)
            )
        val custom = assertSuccess(repository.createCustomCategory("Weekend Stuff"))
        repository.setCategoryOverride(appId, custom.id)
        repository.refresh()

        val categorized = repository.apps.first().single()
        assertEquals(custom, categorized.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, categorized.source)
        assertEquals(
            listOf(categorized),
            LocalAppSearch.filter(listOf(categorized), "weekend")
        )
    }

    @Test
    fun `create rename and reorder survive datastore recreation with stable identity`() = runBlocking {
        val directory = Files.createTempDirectory("category-management-recreation")
        val file = directory.resolve("organizer-state.json").toFile()
        val generatedId = CategoryId.custom("persisted-stable-id")
        val appId = AppId("example.persisted")

        val firstJob = SupervisorJob()
        val firstScope = CoroutineScope(Dispatchers.IO + firstJob)
        try {
            val firstStore = DataStoreOrganizerStateStore.create(file = file, scope = firstScope)
            val firstRepository =
                DefaultOrganizerRepository(
                    installedAppSource = FakeInstalledAppSource(mutableListOf()),
                    organizerStateStore = firstStore,
                    categoryEngine = FakeCategoryEngine(),
                    customCategoryIdGenerator = CustomCategoryIdGenerator { generatedId }
                )
            val created = assertSuccess(firstRepository.createCustomCategory("Original"))
            assertEquals(generatedId, created.id)
            firstRepository.setFavourite(appId, true)
            firstRepository.setHidden(appId, true)
            val renamed = assertSuccess(firstRepository.renameCustomCategory(generatedId, "Renamed"))
            assertEquals(generatedId, renamed.id)
            val current = firstStore.state.first().orderedCategories().map { category -> category.id }
            val reordered = listOf(generatedId) + current.filterNot { categoryId -> categoryId == generatedId }
            assertSuccess(firstRepository.reorderCategories(reordered))
        } finally {
            firstJob.cancelAndJoin()
        }

        val secondJob = SupervisorJob()
        val secondScope = CoroutineScope(Dispatchers.IO + secondJob)
        try {
            val recreated = DataStoreOrganizerStateStore.create(file = file, scope = secondScope).state.first()
            assertEquals(
                CustomCategoryDefinition(generatedId, "Renamed"),
                recreated.customCategories.single()
            )
            assertEquals(generatedId, recreated.categoryOrder.first())
            assertTrue(appId in recreated.favouriteAppIds)
            assertTrue(appId in recreated.hiddenAppIds)
        } finally {
            secondJob.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `concurrent duplicate creates commit one category and return one domain failure`() = runBlocking {
        val directory = Files.createTempDirectory("category-management-concurrency")
        val file = directory.resolve("organizer-state.json").toFile()
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        try {
            val counter = AtomicInteger()
            val store = DataStoreOrganizerStateStore.create(file = file, scope = scope)
            val repository =
                DefaultOrganizerRepository(
                    installedAppSource = FakeInstalledAppSource(mutableListOf()),
                    organizerStateStore = store,
                    categoryEngine = FakeCategoryEngine(),
                    customCategoryIdGenerator =
                        CustomCategoryIdGenerator {
                            CategoryId.custom("parallel-${counter.incrementAndGet()}")
                        }
                )

            val results =
                coroutineScope {
                    listOf(
                        async(Dispatchers.Default) { repository.createCustomCategory("Parallel") },
                        async(Dispatchers.Default) { repository.createCustomCategory("parallel") }
                    ).map { deferred -> deferred.await() }
                }

            assertEquals(1, results.count { result -> result is CategoryManagementResult.Success })
            val failure = results.single { result -> result is CategoryManagementResult.Failure }
            assertIs<CategoryManagementError.DuplicateName>(assertFailure(failure))
            val state = store.state.first()
            assertEquals(1, state.customCategories.size)
            assertEquals(1, state.categoryOrder.count { categoryId -> categoryId.isCustom })
        } finally {
            job.cancelAndJoin()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `storage exception is mapped to app owned persistence failure`() = runBlocking {
        val repository =
            DefaultOrganizerRepository(
                installedAppSource = FakeInstalledAppSource(mutableListOf()),
                organizerStateStore = FailingOrganizerStateStore(),
                categoryEngine = FakeCategoryEngine(),
                customCategoryIdGenerator = CustomCategoryIdGenerator { CategoryId.custom("storage") }
            )

        assertEquals(
            CategoryManagementError.PersistenceFailure,
            assertFailure(repository.createCustomCategory("Storage"))
        )
    }

    private fun repository(
        store: OrganizerStateStore,
        generatedIds: List<CategoryId> = emptyList(),
        installedApps: MutableList<InstalledApp> = mutableListOf(),
        automaticCategories: MutableMap<AppId, AppCategory> = mutableMapOf()
    ): DefaultOrganizerRepository {
        val generatedIdIterator = generatedIds.iterator()
        return DefaultOrganizerRepository(
            installedAppSource = FakeInstalledAppSource(installedApps),
            organizerStateStore = store,
            categoryEngine = FakeCategoryEngine(automaticCategories),
            customCategoryIdGenerator =
                if (generatedIds.isEmpty()) {
                    CustomCategoryIdGenerator { CategoryId.custom("unused") }
                } else {
                    CustomCategoryIdGenerator { generatedIdIterator.next() }
                }
        )
    }

    private fun installedApp(appId: AppId): InstalledApp = InstalledApp(
        id = appId,
        launchTargetId = LaunchTargetId(appId.packageName, "${appId.packageName}.MainActivity"),
        label = appId.packageName
    )

    private fun <T> assertSuccess(result: CategoryManagementResult<T>): T = when (result) {
        is CategoryManagementResult.Success -> result.value
        is CategoryManagementResult.Failure -> fail("Expected success but got ${result.error}.")
    }

    private fun assertFailure(result: CategoryManagementResult<*>): CategoryManagementError = when (result) {
        is CategoryManagementResult.Failure -> result.error
        is CategoryManagementResult.Success -> fail("Expected failure but got ${result.value}.")
    }

    private class FakeInstalledAppSource(val apps: MutableList<InstalledApp>) : InstalledAppSource {
        override suspend fun loadInstalledApps(): List<InstalledApp> = apps.toList()
    }

    private class FakeOrganizerStateStore(initialState: OrganizerState = OrganizerState()) : OrganizerStateStore {
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

    private class FailingOrganizerStateStore : OrganizerStateStore {
        override val state: Flow<OrganizerState> = MutableStateFlow(OrganizerState())

        override suspend fun update(transform: (OrganizerState) -> OrganizerState): OrganizerState =
            throw IllegalStateException("simulated persistence failure")
    }

    private class FakeCategoryEngine(private val automaticCategories: MutableMap<AppId, AppCategory> = mutableMapOf()) :
        CategoryEngine {
        override fun categorize(app: InstalledApp, userOverride: CategoryDefinition?): CategorizedApp =
            if (userOverride != null) {
                CategorizedApp(app, userOverride, ClassificationSource.USER_OVERRIDE)
            } else {
                val automaticCategory = automaticCategories[app.id]
                CategorizedApp(
                    app = app,
                    category = automaticCategory ?: AppCategory.UNSORTED,
                    source =
                        if (automaticCategory == null) {
                            ClassificationSource.UNSORTED_FALLBACK
                        } else {
                            ClassificationSource.KNOWN_APP_RULE
                        }
                )
            }
    }
}
