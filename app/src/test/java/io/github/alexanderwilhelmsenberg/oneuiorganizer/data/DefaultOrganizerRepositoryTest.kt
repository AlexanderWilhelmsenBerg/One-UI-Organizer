package io.github.alexanderwilhelmsenberg.oneuiorganizer.data

import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.CategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategorizedApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.OrganizerState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.InstalledAppSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DefaultOrganizerRepositoryTest {
    @Test
    fun `refresh merges installed apps with persisted category override`() = runBlocking {
        val overriddenId = AppId("example.overridden")
        val automaticId = AppId("example.automatic")
        val source =
            FakeInstalledAppSource(
                mutableListOf(
                    installedApp(overriddenId),
                    installedApp(automaticId)
                )
            )
        val store =
            FakeOrganizerStateStore(
                OrganizerState(categoryOverrides = mapOf(overriddenId to AppCategory.WORK))
            )
        val categoryEngine =
            FakeCategoryEngine(
                automaticCategories =
                    mutableMapOf(
                        overriddenId to AppCategory.GAMES,
                        automaticId to AppCategory.TOOLS
                    )
            )
        val repository = DefaultOrganizerRepository(source, store, categoryEngine)

        repository.refresh()

        val apps = repository.apps.first()
        assertEquals(AppCategory.WORK, apps.single { it.app.id == overriddenId }.category)
        assertEquals(
            ClassificationSource.USER_OVERRIDE,
            apps.single { it.app.id == overriddenId }.source
        )
        assertEquals(AppCategory.TOOLS, apps.single { it.app.id == automaticId }.category)
    }

    @Test
    fun `repository mutations persist override favourite and hidden state`() = runBlocking {
        val appId = AppId("example.mutable")
        val store = FakeOrganizerStateStore()
        val repository =
            DefaultOrganizerRepository(
                installedAppSource = FakeInstalledAppSource(mutableListOf(installedApp(appId))),
                organizerStateStore = store,
                categoryEngine = FakeCategoryEngine()
            )

        repository.setCategoryOverride(appId, AppCategory.PRODUCTIVITY)
        repository.setFavourite(appId, true)
        repository.setHidden(appId, true)

        val persisted = repository.organizerState.first()
        assertEquals(AppCategory.PRODUCTIVITY, persisted.categoryOverrides[appId])
        assertTrue(appId in persisted.favouriteAppIds)
        assertTrue(appId in persisted.hiddenAppIds)

        repository.setCategoryOverride(appId, null)
        repository.setFavourite(appId, false)
        repository.setHidden(appId, false)

        val cleared = repository.organizerState.first()
        assertFalse(appId in cleared.categoryOverrides)
        assertFalse(appId in cleared.favouriteAppIds)
        assertFalse(appId in cleared.hiddenAppIds)
    }

    @Test
    fun `uninstalled apps disappear while stale user state is retained`() = runBlocking {
        val appId = AppId("example.stale")
        val source = FakeInstalledAppSource(mutableListOf(installedApp(appId)))
        val retainedState =
            OrganizerState(
                categoryOverrides = mapOf(appId to AppCategory.READING),
                favouriteAppIds = setOf(appId),
                hiddenAppIds = setOf(appId)
            )
        val store = FakeOrganizerStateStore(retainedState)
        val repository =
            DefaultOrganizerRepository(
                installedAppSource = source,
                organizerStateStore = store,
                categoryEngine = FakeCategoryEngine()
            )

        repository.refresh()
        assertEquals(1, repository.apps.first().size)

        source.apps.clear()
        repository.refresh()

        assertTrue(repository.apps.first().isEmpty())
        assertEquals(retainedState, repository.organizerState.first())
    }

    @Test
    fun `reinstall with same package identity restores retained user state`() = runBlocking {
        val appId = AppId("example.reinstall")
        val source = FakeInstalledAppSource(mutableListOf<InstalledApp>())
        val store =
            FakeOrganizerStateStore(
                OrganizerState(
                    categoryOverrides = mapOf(appId to AppCategory.SMART_HOME),
                    favouriteAppIds = setOf(appId),
                    hiddenAppIds = setOf(appId)
                )
            )
        val repository =
            DefaultOrganizerRepository(
                installedAppSource = source,
                organizerStateStore = store,
                categoryEngine = FakeCategoryEngine()
            )

        repository.refresh()
        assertTrue(repository.apps.first().isEmpty())

        source.apps += installedApp(appId)
        repository.refresh()

        val restored = repository.apps.first().single()
        assertEquals(AppCategory.SMART_HOME, restored.category)
        assertEquals(ClassificationSource.USER_OVERRIDE, restored.source)
        val state = repository.organizerState.first()
        assertTrue(appId in state.favouriteAppIds)
        assertTrue(appId in state.hiddenAppIds)
    }

    @Test
    fun `category override survives automatic category changes`() = runBlocking {
        val appId = AppId("example.rule-change")
        val source = FakeInstalledAppSource(mutableListOf(installedApp(appId)))
        val store =
            FakeOrganizerStateStore(
                OrganizerState(categoryOverrides = mapOf(appId to AppCategory.FINANCE))
            )
        val categoryEngine =
            FakeCategoryEngine(
                automaticCategories = mutableMapOf(appId to AppCategory.SHOPPING)
            )
        val repository = DefaultOrganizerRepository(source, store, categoryEngine)

        repository.refresh()
        assertEquals(AppCategory.FINANCE, repository.apps.first().single().category)

        categoryEngine.automaticCategories[appId] = AppCategory.GAMES
        repository.refresh()

        assertEquals(AppCategory.FINANCE, repository.apps.first().single().category)
    }

    private fun installedApp(appId: AppId): InstalledApp = InstalledApp(
        id = appId,
        launchTargetId =
            LaunchTargetId(
                packageName = appId.packageName,
                className = "${appId.packageName}.MainActivity"
            ),
        label = appId.packageName
    )

    private class FakeInstalledAppSource(val apps: MutableList<InstalledApp>) : InstalledAppSource {
        override suspend fun loadInstalledApps(): List<InstalledApp> = apps.toList()
    }

    private class FakeOrganizerStateStore(initialState: OrganizerState = OrganizerState()) : OrganizerStateStore {
        private val mutableState = MutableStateFlow(initialState)

        override val state: Flow<OrganizerState> = mutableState

        override suspend fun update(transform: (OrganizerState) -> OrganizerState): OrganizerState {
            val updated = transform(mutableState.value)
            mutableState.value = updated
            return updated
        }
    }

    private class FakeCategoryEngine(val automaticCategories: MutableMap<AppId, AppCategory> = mutableMapOf()) :
        CategoryEngine {
        override fun categorize(app: InstalledApp, userOverride: AppCategory?): CategorizedApp =
            if (userOverride != null) {
                CategorizedApp(
                    app = app,
                    category = userOverride,
                    source = ClassificationSource.USER_OVERRIDE
                )
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
