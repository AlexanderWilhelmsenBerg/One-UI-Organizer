package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.navigation.PrimaryDestination
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.navigation.PrimaryNavigationHost
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.navigation.PrimaryNavigationShell
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import org.junit.Rule
import org.junit.Test

class PrimaryNavigationShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun organizerIsInitialDestinationAndTabsSwitchExplicitly() {
        var selected by mutableStateOf(PrimaryDestination.ORGANIZER)
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                PrimaryNavigationShell(
                    selectedDestination = selected,
                    onDestinationSelected = { selected = it }
                ) { destination ->
                    Text("${destination.savedValue} root")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Organizer").assertIsSelected()
        composeRule.onNodeWithContentDescription("Categories").assertIsNotSelected().performClick()
        composeRule.onNodeWithContentDescription("Categories").assertIsSelected()
        composeRule.onNodeWithText("categories root").assertExists()
        composeRule.onNodeWithContentDescription("Organizer").performClick().assertIsSelected()
        composeRule.onNodeWithText("organizer root").assertExists()
    }

    @Test
    fun organizerSaveablePresentationStateSurvivesPrimaryTabSwitches() {
        var selected by mutableStateOf(PrimaryDestination.ORGANIZER)
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                PrimaryNavigationHost(
                    selectedDestination = selected,
                    onDestinationSelected = { selected = it },
                    showHiddenApps = false,
                    showCategoryManagement = false,
                    showBackupRestore = false,
                    onDismissHiddenApps = {},
                    onDismissCategoryManagement = {},
                    onDismissBackupRestore = {},
                    organizerContent = {
                        var query by rememberSaveable { mutableStateOf("") }
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .semantics {
                                        contentDescription = "Organizer state test"
                                    }
                        )
                    },
                    categoriesContent = { Text("Categories root") },
                    categoryManagementContent = { Text("Category management") },
                    backupRestoreContent = { Text("Backup restore") }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Organizer state test").performTextInput("maps")
        composeRule.onNodeWithContentDescription("Categories").performClick()
        composeRule.onNodeWithText("Categories root").assertExists()
        composeRule.onNodeWithContentDescription("Organizer").performClick()
        composeRule.onNodeWithContentDescription("Organizer state test").assertTextContains("maps")
    }

    @Test
    fun labelsRemainAvailableAtLargeFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                OneUiOrganizerTheme(dynamicColor = false) {
                    PrimaryNavigationShell(
                        selectedDestination = PrimaryDestination.ORGANIZER,
                        onDestinationSelected = {}
                    ) { Text("Organizer root") }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Organizer").assertExists()
        composeRule.onNodeWithContentDescription("Categories").assertExists()
        composeRule.onNodeWithText("Organizer").assertExists()
        composeRule.onNodeWithText("Categories").assertExists()
    }
}
