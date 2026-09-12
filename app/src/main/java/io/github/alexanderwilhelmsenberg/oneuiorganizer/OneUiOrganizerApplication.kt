package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.app.Application
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.CategoryManagementRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DataStoreOrganizerStateStore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DefaultOrganizerBackupRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DefaultOrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerBackupRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.DefaultCategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.AndroidUiPlatformCapabilities
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.UiPlatformCapabilities
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidAppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidInstalledAppSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts.AndroidCategoryShortcutManager
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts.CategoryShortcutManager
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.OrganizerViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Process-scoped composition root for the small application dependency graph. */
class OneUiOrganizerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var organizerRepository: OrganizerRepository
    private lateinit var categoryManagementRepository: CategoryManagementRepository
    private lateinit var appLauncher: AppLauncher

    lateinit var organizerBackupRepository: OrganizerBackupRepository
        private set

    lateinit var categoryShortcutManager: CategoryShortcutManager
        internal set

    lateinit var uiPlatformCapabilities: UiPlatformCapabilities
        private set

    override fun onCreate() {
        super.onCreate()

        val installedAppSource = AndroidInstalledAppSource(this)
        val organizerStateStore =
            DataStoreOrganizerStateStore.create(
                file = File(filesDir, ORGANIZER_STATE_FILE_NAME),
                scope = applicationScope
            )
        val defaultOrganizerRepository =
            DefaultOrganizerRepository(
                installedAppSource = installedAppSource,
                organizerStateStore = organizerStateStore,
                categoryEngine = DefaultCategoryEngine()
            )

        organizerRepository = defaultOrganizerRepository
        categoryManagementRepository = defaultOrganizerRepository
        organizerBackupRepository = DefaultOrganizerBackupRepository(organizerStateStore)
        appLauncher = AndroidAppLauncher(this)
        categoryShortcutManager = AndroidCategoryShortcutManager(this)
        uiPlatformCapabilities = AndroidUiPlatformCapabilities.current()
    }

    fun createOrganizerViewModel(scope: CoroutineScope): OrganizerViewModel = OrganizerViewModel(
        organizerRepository = organizerRepository,
        categoryManagementRepository = categoryManagementRepository,
        appLauncher = appLauncher,
        scope = scope
    )

    private companion object {
        const val ORGANIZER_STATE_FILE_NAME = "organizer-state.json"
    }
}
