package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.backup.OrganizerBackupSummary
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.backup.BackupRestore
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.BackupRestoreUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BackupRestoreTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exportAndImportActionsUseInjectedCallbacks() {
        var exportRequested = false
        var importRequested = false
        composeRule.setContent {
            OneUiOrganizerTheme {
                BackupRestore(
                    state = BackupRestoreUiState(),
                    onExportRequested = { exportRequested = true },
                    onImportRequested = { importRequested = true },
                    onConfirmImport = {},
                    onCancelImport = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Export backup").performClick()
        composeRule.onNodeWithText("Choose backup to import").performClick()

        assertTrue(exportRequested)
        assertTrue(importRequested)
    }

    @Test
    fun pendingImportShowsSanitizedSummaryAndRequiresExplicitConfirmation() {
        var confirmed = false
        var cancelled = false
        composeRule.setContent {
            OneUiOrganizerTheme {
                BackupRestore(
                    state =
                        BackupRestoreUiState(
                            pendingImportSummary =
                                OrganizerBackupSummary(
                                    customCategoryCount = 2,
                                    overrideCount = 3,
                                    favouriteCount = 4,
                                    hiddenCount = 5
                                )
                        ),
                    onExportRequested = {},
                    onImportRequested = {},
                    onConfirmImport = { confirmed = true },
                    onCancelImport = { cancelled = true },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Replace organizer state?").assertIsDisplayed()
        composeRule.onNodeWithText("2 custom categories").assertIsDisplayed()
        composeRule.onNodeWithText("3 category overrides").assertIsDisplayed()
        composeRule.onNodeWithText("4 favourites").assertIsDisplayed()
        composeRule.onNodeWithText("5 hidden apps").assertIsDisplayed()
        composeRule.onNodeWithText("Import and replace").performClick()

        assertTrue(confirmed)
        assertTrue(!cancelled)
    }
}
