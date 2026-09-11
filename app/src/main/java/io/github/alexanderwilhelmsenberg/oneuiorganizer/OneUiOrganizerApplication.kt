package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.app.Application
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DataStoreOrganizerStateStore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DefaultOrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.DefaultCategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.AndroidUiPlatformCapabilities
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.UiPlatformCapabilities
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidAppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidInstalledAppSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.OrganizerViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Process-scoped composition root for the small v0.1 dependency graph. */
class OneUiOrganizerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var organizerRepository: OrganizerRepository
    private lateinit var appLauncher: AppLauncher

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

        organizerRepository =
            DefaultOrganizerRepository(
                installedAppSource = installedAppSource,
                organizerStateStore = organizerStateStore,
                categoryEngine = DefaultCategoryEngine()
            )
        appLauncher = AndroidAppLauncher(this)
        uiPlatformCapabilities = AndroidUiPlatformCapabilities.current()
    }

    fun createOrganizerViewModel(scope: CoroutineScope): OrganizerViewModel = OrganizerViewModel(
        organizerRepository = organizerRepository,
        appLauncher = appLauncher,
        scope = scope
    )

    private companion object {
        const val ORGANIZER_STATE_FILE_NAME = "organizer-state.json"
    }
}
