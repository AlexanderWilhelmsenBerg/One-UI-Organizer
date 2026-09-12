package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts.CategoryShortcutIntents
import io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts.CategoryShortcutManager
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.OrganizerViewModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.management.CategoryManagement
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfErrorUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerSheetHost
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerShelf
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var presentationScope: CoroutineScope
    private lateinit var organizerViewModel: OrganizerViewModel
    private lateinit var categoryShortcutManager: CategoryShortcutManager
    private var categoryDestinationCleared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val organizerApplication = application as OneUiOrganizerApplication
        presentationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        organizerViewModel = organizerApplication.createOrganizerViewModel(presentationScope)
        categoryShortcutManager = organizerApplication.categoryShortcutManager
        val supportsDynamicColor = organizerApplication.uiPlatformCapabilities.supportsDynamicColor

        categoryDestinationCleared = savedInstanceState?.getBoolean(STATE_CATEGORY_DESTINATION_CLEARED) ?: false
        if (!categoryDestinationCleared) {
            handleShortcutIntent(intent)
        }
        observeShortcutSynchronization()

        setContent {
            val state by organizerViewModel.uiState.collectAsState()
            val showHiddenApps by organizerViewModel.showHiddenApps.collectAsState()
            val showCategoryManagement by organizerViewModel.showCategoryManagement.collectAsState()
            val categoryManagementState by organizerViewModel.categoryManagementUiState.collectAsState()

            BackHandler(enabled = showCategoryManagement) {
                organizerViewModel.hideCategoryManagement()
            }

            OneUiOrganizerTheme(supportsDynamicColor = supportsDynamicColor) {
                OrganizerSheetHost {
                    if (showCategoryManagement) {
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
                            }
                        )
                    } else {
                        OrganizerShelf(
                            state = state,
                            showHiddenApps = showHiddenApps,
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
                            onCategoryManagementRequested = organizerViewModel::showCategoryManagement,
                            onCategoryDestinationCleared = ::clearCategoryDestination
                        )
                    }
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
        organizerViewModel.openCategoryDestination(destination)
        categoryShortcutManager.reportShortcutUsed(destination.categoryId)
    }

    private fun clearCategoryDestination() {
        organizerViewModel.clearCategoryDestination()
        categoryDestinationCleared = true
        setIntent(Intent(this, MainActivity::class.java))
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
    }
}
