package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.CategoryManagementRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DataStoreOrganizerStateStore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DefaultOrganizerBackupRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DefaultOrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.DefaultSupportedMetadataRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.JsonSupportedMetadataCache
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerBackupRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.data.OrganizerRepository
import io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics.AppEventLog
import io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics.FileAppEventLog
import io.github.alexanderwilhelmsenberg.oneuiorganizer.domain.DefaultCategoryEngine
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.AndroidUiPlatformCapabilities
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.UiPlatformCapabilities
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidAppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AndroidInstalledAppSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps.AppLauncher
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.metadata.FdroidMetadataProvider
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.metadata.HttpFdroidIndexDocumentSource
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

    lateinit var eventLog: AppEventLog
        private set

    override fun onCreate() {
        super.onCreate()

        eventLog = FileAppEventLog(File(filesDir, EVENT_LOG_FILE_NAME))
        installCrashLogging()
        recordPreviousProcessExit()
        eventLog.record("Process started")

        val installedAppSource = AndroidInstalledAppSource(this)
        val organizerStateStore =
            DataStoreOrganizerStateStore.create(
                file = File(filesDir, ORGANIZER_STATE_FILE_NAME),
                scope = applicationScope
            )
        val supportedMetadataRepository =
            DefaultSupportedMetadataRepository(
                provider =
                    FdroidMetadataProvider(
                        documentSource = HttpFdroidIndexDocumentSource(),
                        eventLog = eventLog
                    ),
                cache = JsonSupportedMetadataCache(File(filesDir, SUPPORTED_METADATA_CACHE_FILE_NAME))
            )
        val defaultOrganizerRepository =
            DefaultOrganizerRepository(
                installedAppSource = installedAppSource,
                organizerStateStore = organizerStateStore,
                categoryEngine = DefaultCategoryEngine(),
                supportedMetadataRepository = supportedMetadataRepository,
                metadataRefreshScope = applicationScope
            )

        organizerRepository = defaultOrganizerRepository
        categoryManagementRepository = defaultOrganizerRepository
        organizerBackupRepository = DefaultOrganizerBackupRepository(organizerStateStore)
        appLauncher = AndroidAppLauncher(this)
        categoryShortcutManager = AndroidCategoryShortcutManager(this, eventLog)
        uiPlatformCapabilities = AndroidUiPlatformCapabilities.current()
    }

    fun createOrganizerViewModel(scope: CoroutineScope): OrganizerViewModel =
        OrganizerViewModel(
            organizerRepository = organizerRepository,
            categoryManagementRepository = categoryManagementRepository,
            appLauncher = appLauncher,
            scope = scope
        )

    private fun installCrashLogging() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            eventLog.record("Uncaught exception on thread ${thread.name}", throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun recordPreviousProcessExit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        try {
            val activityManager = getSystemService(ActivityManager::class.java)
            val latestExit =
                activityManager
                    ?.getHistoricalProcessExitReasons(packageName, 0, 1)
                    ?.firstOrNull()
                    ?: return
            val preferences = getSharedPreferences(DIAGNOSTICS_PREFERENCES_NAME, MODE_PRIVATE)
            val lastRecordedTimestamp = preferences.getLong(LAST_EXIT_TIMESTAMP_KEY, 0L)
            if (latestExit.timestamp <= lastRecordedTimestamp) {
                return
            }
            eventLog.record(
                "Previous process exit reported by Android: " +
                    "reason=${latestExit.reason}, status=${latestExit.status}, importance=${latestExit.importance}"
            )
            preferences.edit().putLong(LAST_EXIT_TIMESTAMP_KEY, latestExit.timestamp).apply()
        } catch (exception: RuntimeException) {
            eventLog.record("Unable to read previous Android process exit information", exception)
        }
    }

    private companion object {
        const val ORGANIZER_STATE_FILE_NAME = "organizer-state.json"
        const val SUPPORTED_METADATA_CACHE_FILE_NAME = "supported-metadata-cache.json"
        const val EVENT_LOG_FILE_NAME = "event-log.txt"
        const val DIAGNOSTICS_PREFERENCES_NAME = "diagnostics"
        const val LAST_EXIT_TIMESTAMP_KEY = "last-exit-timestamp"
    }
}
