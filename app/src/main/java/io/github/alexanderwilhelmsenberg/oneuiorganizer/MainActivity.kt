package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.AndroidBackupDocumentStore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.backup.BackupDocumentPicker
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts.CategoryShortcutIntents
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts.CategoryShortcutManager
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.OrganizerViewModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.backup.BackupRestore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.backup.BackupRestoreController
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.categories.CategoriesOverview
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.management.CategoryManagement
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupDocumentRequest
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreNotice
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfErrorUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.navigation.PrimaryDestination
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.navigation.PrimaryNavigationHost
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerSheetHost
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerShelf
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var presentationScope: CoroutineScope
    private lateinit var organizerViewModel: OrganizerViewModel
    private lateinit var categoryShortcutManager: CategoryShortcutManager
    private lateinit var backupRestoreController: BackupRestoreController
    private var categoryDestinationCleared = false
    private var primaryDestination by mutableStateOf(PrimaryDestination.ORGANIZER)
    private var showBackupRestore by mutableStateOf(false)

    private val createBackupDocumentLauncher =
        registerForActivityResult(BackupDocumentPicker.createDocumentContract()) { uri ->
            backupRestoreController.onExportDocumentSelected(
                uri?.let { BackupDocumentId(it.toString()) }
            )
        }

    private val openBackupDocumentLauncher =
        registerForActivityResult(BackupDocumentPicker.openDocumentContract()) { uri ->
            backupRestoreController.onImportDocumentSelected(
                uri?.let { BackupDocumentId(it.toString()) }
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val organizerApplication = application as OneUiOrganizerApplication
        presentationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        organizerViewModel = organizerApplication.createOrganizerViewModel(presentationScope)
        categoryShortcutManager = organizerApplication.categoryShortcutManager
        backupRestoreController =
            BackupRestoreController(
                backupRepository = organizerApplication.organizerBackupRepository,
                documentStore = AndroidBackupDocumentStore(contentResolver),
                scope = presentationScope
            )
        val supportsDynamicColor = organizerApplication.uiPlatformCapabilities.supportsDynamicColor

        primaryDestination =
            PrimaryDestination.fromSavedValue(
                savedInstanceState?.getString(STATE_PRIMARY_DESTINATION)
            )
        categoryDestinationCleared = savedInstanceState?.getBoolean(STATE_CATEGORY_DESTINATION_CLEARED) ?: false
        if (!categoryDestinationCleared) {
            handleShortcutIntent(intent)
        }
        observeShortcutSynchronization()
        observeBackupDocumentRequests()
        observeSuccessfulImport()

        setContent {
            val state by organizerViewModel.uiState.collectAsState()
            val showHiddenApps by organizerViewModel.showHiddenApps.collectAsState()
            val showCategoryManagement by organizerViewModel.showCategoryManagement.collectAsState()
            val categoryManagementState by organizerViewModel.categoryManagementUiState.collectAsState()
            val backupRestoreState by backupRestoreController.uiState.collectAsState()

            OneUiOrganizerTheme(supportsDynamicColor = supportsDynamicColor) {
                OrganizerSheetHost {
                    PrimaryNavigationHost(
                        selectedDestination = primaryDestination,
                        onDestinationSelected = { destination ->
                            primaryDestination = destination
                        },
                        showHiddenApps = showHiddenApps,
                        showCategoryManagement = showCategoryManagement,
                        showBackupRestore = showBackupRestore,
                        onDismissHiddenApps = organizerViewModel::hideHiddenApps,
                        onDismissCategoryManagement = organizerViewModel::hideCategoryManagement,
                        onDismissBackupRestore = ::dismissBackupRestore,
                        organizerContent = { hiddenAppsVisible ->
                            OrganizerShelf(
                                state = state,
                                showHiddenApps = hiddenAppsVisible,
                                onQueryChange = organizerViewModel::updateQuery,
                                onLaunchApp = { target ->
                                    if (organizerViewModel.launch(target)) {
                                        finish()
                                    }
                                },
                                onMoveApp = organizerViewModel::moveApp,
                                onToggleFavourite = organizerViewModel::toggleFavourite,
                                onHideApp = organizerViewModel::hideApp,
                                onRestoreApp = organizerViewModel::restoreApp,
                                onClassificationReportRequested = ::shareClassificationReport,
                                onHiddenAppsRequested = organizerViewModel::showHiddenApps,
                                onHiddenAppsDismissed = organizerViewModel::hideHiddenApps,
                                onCategoryManagementRequested = ::openCategoryManagement,
                                onCategoryDestinationCleared = ::clearCategoryDestination
                            )
                        },
                        categoriesContent = {
                            CategoriesOverview(
                                state = categoryManagementState,
                                onManageCategories = ::openCategoryManagement,
                                onBackupRestoreRequested = ::openBackupRestore
                            )
                        },
                        categoryManagementContent = {
                            CategoryManagement(
                                state = categoryManagementState,
                                onCreateCategory = organizerViewModel::createCustomCategory,
                                onRenameCategory = organizerViewModel::renameCustomCategory,
                                onDeleteCategory = organizerViewModel::deleteCustomCategory,
                                onMoveCategory = organizerViewModel::moveCategory,
                                onDismiss = organizerViewModel::hideCategoryManagement,
                                pinningSupported = categoryShortcutManager.isPinningSupported,
                                onPinCategory = { category ->
                                    categoryShortcutManager.requestPinShortcut(category)
                                },
                                onBackupRestoreRequested = ::openBackupRestore
                            )
                        },
                        backupRestoreContent = {
                            BackupRestore(
                                state = backupRestoreState,
                                onExportRequested = backupRestoreController::requestExport,
                                onImportRequested = backupRestoreController::requestImport,
                                onConfirmImport = backupRestoreController::confirmImport,
                                onCancelImport = backupRestoreController::cancelImport,
                                onDismiss = ::dismissBackupRestore
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        categoryDestinationCleared = false
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_CATEGORY_DESTINATION_CLEARED, categoryDestinationCleared)
        outState.putString(STATE_PRIMARY_DESTINATION, primaryDestination.savedValue)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        organizerViewModel.refresh()
    }

    override fun onDestroy() {
        presentationScope.cancel()
        super.onDestroy()
    }

    private fun handleShortcutIntent(sourceIntent: Intent?) {
        val destination = CategoryShortcutIntents.destinationFrom(sourceIntent) ?: return
        showBackupRestore = false
        primaryDestination = PrimaryDestination.forCategoryShortcut()
        organizerViewModel.openCategoryDestination(destination)
        categoryShortcutManager.reportShortcutUsed(destination.categoryId)
    }

    private fun openCategoryManagement() {
        primaryDestination = PrimaryDestination.CATEGORIES
        organizerViewModel.showCategoryManagement()
    }

    private fun openBackupRestore() {
        primaryDestination = PrimaryDestination.CATEGORIES
        showBackupRestore = true
    }

    private fun clearCategoryDestination() {
        organizerViewModel.clearCategoryDestination()
        categoryDestinationCleared = true
        setIntent(Intent(this, MainActivity::class.java))
    }

    private fun dismissBackupRestore() {
        backupRestoreController.cancelImport()
        showBackupRestore = false
    }

    private fun observeBackupDocumentRequests() {
        presentationScope.launch {
            backupRestoreController.documentRequests.collect { request ->
                when (request) {
                    is BackupDocumentRequest.Create ->
                        createBackupDocumentLauncher.launch(request.suggestedFileName)

                    is BackupDocumentRequest.Open ->
                        openBackupDocumentLauncher.launch(request.mimeTypes.toTypedArray())
                }
            }
        }
    }

    private fun observeSuccessfulImport() {
        presentationScope.launch {
            backupRestoreController.uiState
                .map { state -> state.notice }
                .distinctUntilChanged()
                .collectLatest { notice ->
                    if (notice == BackupRestoreNotice.IMPORTED) {
                        organizerViewModel.refresh()
                    }
                }
        }
    }

    private fun observeShortcutSynchronization() {
        presentationScope.launch {
            organizerViewModel.uiState
                .map { state ->
                    CategoryShortcutSyncSnapshot(
                        canSynchronize = !state.isLoading && state.error != ShelfErrorUiModel.SCAN_FAILED,
                        orderedCategories = state.availableCategories,
                        currentCategoryAppCounts = state.currentCategoryAppCounts
                    )
                }.distinctUntilChanged()
                .collectLatest { snapshot ->
                    if (snapshot.canSynchronize) {
                        categoryShortcutManager.synchronize(
                            orderedCategories = snapshot.orderedCategories,
                            currentCategoryAppCounts = snapshot.currentCategoryAppCounts
                        )
                    }
                }
        }
    }

    private fun shareClassificationReport() {
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.classification_report_subject))
                putExtra(Intent.EXTRA_TEXT, organizerViewModel.classificationReport())
            }
        startActivity(
            Intent.createChooser(
                sendIntent,
                getString(R.string.classification_report_chooser_title)
            )
        )
    }

    private data class CategoryShortcutSyncSnapshot(
        val canSynchronize: Boolean,
        val orderedCategories: List<CategoryDefinition>,
        val currentCategoryAppCounts: Map<CategoryId, Int>
    )

    private companion object {
        const val STATE_CATEGORY_DESTINATION_CLEARED = "category-destination-cleared"
        const val STATE_PRIMARY_DESTINATION = "primary-destination"
    }
}
