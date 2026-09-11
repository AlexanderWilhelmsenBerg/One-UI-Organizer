package io.github.alexanderwilhelmsenberg.oneuiorganizer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.OrganizerViewModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerSheetHost
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerShelf
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private lateinit var presentationScope: CoroutineScope
    private lateinit var organizerViewModel: OrganizerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val organizerApplication = application as OneUiOrganizerApplication
        presentationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        organizerViewModel = organizerApplication.createOrganizerViewModel(presentationScope)
        val supportsDynamicColor = organizerApplication.uiPlatformCapabilities.supportsDynamicColor

        setContent {
            val state by organizerViewModel.uiState.collectAsState()
            val showHiddenApps by organizerViewModel.showHiddenApps.collectAsState()

            OneUiOrganizerTheme(supportsDynamicColor = supportsDynamicColor) {
                OrganizerSheetHost {
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
                        onHiddenAppsDismissed = organizerViewModel::hideHiddenApps
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        organizerViewModel.refresh()
    }

    override fun onDestroy() {
        presentationScope.cancel()
        super.onDestroy()
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
}
