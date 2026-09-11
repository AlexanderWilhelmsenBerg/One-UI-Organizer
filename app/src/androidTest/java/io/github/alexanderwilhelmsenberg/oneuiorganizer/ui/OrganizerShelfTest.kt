package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategorySectionUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerShelfUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfAppUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerShelf
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrganizerShelfTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun categoriesAndFavouritesRenderFromSuppliedState() {
        val favourite = app("Home Assistant", AppCategory.SMART_HOME, isFavourite = true)
        val signal = app("Signal", AppCategory.COMMUNICATION)
        val state =
            OrganizerShelfUiState(
                favourites = listOf(favourite),
                categories =
                    listOf(
                        CategorySectionUiModel(AppCategory.SMART_HOME, listOf(favourite)),
                        CategorySectionUiModel(AppCategory.COMMUNICATION, listOf(signal))
                    )
            )

        composeRule.setShelfContent(state = state)

        composeRule.onNodeWithText("Favourites").assertExists()
        composeRule.onNodeWithText("Smart Home").assertExists()
        composeRule.onNodeWithText("Communication").assertExists()
        composeRule.onAllNodesWithText("Home Assistant").assertCountEquals(2)
        composeRule.onNodeWithText("Signal").assertExists()
    }

    @Test
    fun searchFieldReportsInputAndClearActions() {
        var state by mutableStateOf(OrganizerShelfUiState())
        var observedQuery = ""
        composeRule.setShelfContent(
            stateProvider = { state },
            onQueryChange = {
                observedQuery = it
                state = state.copy(query = it)
            }
        )

        composeRule.onNodeWithContentDescription("Search apps and categories").performTextInput("home")
        composeRule.runOnIdle {
            assertEquals("home", observedQuery)
        }
        composeRule.onNodeWithContentDescription("Clear search").performClick()
        composeRule.runOnIdle {
            assertEquals("", observedQuery)
        }
    }

    @Test
    fun queryWithNoSuppliedMatchesShowsNoResultsState() {
        composeRule.setShelfContent(state = OrganizerShelfUiState(query = "nothing"))

        composeRule.onNodeWithText("No results").assertExists()
        composeRule.onNodeWithText("Nothing visible matches", substring = true).assertExists()
    }

    @Test
    fun classificationExplanationUsesSourceForEveryClassificationPath() {
        var state by mutableStateOf(OrganizerShelfUiState())
        composeRule.setShelfContent(stateProvider = { state })
        val cases =
            listOf(
                ClassificationSource.USER_OVERRIDE to "Your category",
                ClassificationSource.KNOWN_APP_RULE to "Known app rule",
                ClassificationSource.ANDROID_DECLARED_CATEGORY to "Android category",
                ClassificationSource.UNSORTED_FALLBACK to "Needs sorting"
            )

        cases.forEach { (source, expectedLabel) ->
            val category =
                if (source == ClassificationSource.UNSORTED_FALLBACK) {
                    AppCategory.UNSORTED
                } else {
                    AppCategory.TOOLS
                }
            val sourceApp = app("Source app", category, classificationSource = source)
            composeRule.runOnIdle {
                state =
                    OrganizerShelfUiState(
                        categories = listOf(CategorySectionUiModel(category, listOf(sourceApp)))
                    )
            }
            composeRule.waitForIdle()

            composeRule.onNodeWithContentDescription("Open Source app")
                .performSemanticsAction(SemanticsActions.OnLongClick)
            composeRule.onNodeWithContentDescription("Classification: $expectedLabel").assertExists()
            composeRule.onNodeWithText("Move category").performClick()
            composeRule.onNodeWithText("Cancel").performClick()
        }
    }

    @Test
    fun unsortedCorrectionIsDirectAndBecomesUserOverrideAfterMove() {
        val unsorted =
            app(
                "Mystery",
                AppCategory.UNSORTED,
                classificationSource = ClassificationSource.UNSORTED_FALLBACK
            )
        var state by
            mutableStateOf(
                OrganizerShelfUiState(
                    categories = listOf(CategorySectionUiModel(AppCategory.UNSORTED, listOf(unsorted)))
                )
            )
        var moved: Pair<LaunchTargetId, CategoryDefinition>? = null
        composeRule.setShelfContent(
            stateProvider = { state },
            onMoveApp = { target, category ->
                moved = target to category
                val corrected =
                    unsorted.copy(
                        category = category,
                        classificationSource = ClassificationSource.USER_OVERRIDE
                    )
                state =
                    OrganizerShelfUiState(
                        categories = listOf(CategorySectionUiModel(category, listOf(corrected)))
                    )
            }
        )

        composeRule.onNodeWithContentDescription("Sort Mystery into a category")
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("Sort Mystery").assertExists()
        composeRule.onNodeWithContentDescription("Classification: Needs sorting").assertExists()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.runOnIdle {
            assertEquals(unsorted.launchTargetId to AppCategory.TOOLS, moved)
        }

        composeRule.onNodeWithContentDescription("Sort Mystery into a category").assertDoesNotExist()
        composeRule.onNodeWithText("Tools").assertExists()
        composeRule.onNodeWithContentDescription("Open Mystery")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithContentDescription("Classification: Your category").assertExists()
    }

    @Test
    fun longPressSemanticsExposeOrganizationActionsAndCallbacks() {
        val signal = app("Signal", AppCategory.COMMUNICATION)
        var launched: LaunchTargetId? = null
        var moved: Pair<LaunchTargetId, CategoryDefinition>? = null
        var toggled: LaunchTargetId? = null
        var hidden: LaunchTargetId? = null
        composeRule.setShelfContent(
            state =
                OrganizerShelfUiState(
                    categories =
                        listOf(
                            CategorySectionUiModel(AppCategory.COMMUNICATION, listOf(signal))
                        )
                ),
            onLaunchApp = { launched = it },
            onMoveApp = { target, category -> moved = target to category },
            onToggleFavourite = { toggled = it },
            onHideApp = { hidden = it }
        )

        composeRule.onNodeWithContentDescription("Open Signal")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(signal.launchTargetId, launched)
        }

        composeRule.onNodeWithContentDescription("Open Signal")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Add to favourites").assertExists()
        composeRule.onNodeWithText("Move category").assertExists()
        composeRule.onNodeWithText("Hide").assertExists()
        composeRule.onNodeWithText("Move category").performClick()
        composeRule.onNodeWithText("Tools").performClick()
        composeRule.runOnIdle {
            assertEquals(signal.launchTargetId to AppCategory.TOOLS, moved)
        }

        composeRule.onNodeWithContentDescription("Open Signal")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Add to favourites").performClick()
        composeRule.runOnIdle {
            assertEquals(signal.launchTargetId, toggled)
        }

        composeRule.onNodeWithContentDescription("Open Signal")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Hide").performClick()
        composeRule.runOnIdle {
            assertEquals(signal.launchTargetId, hidden)
        }
    }

    @Test
    fun favouriteActionExposesUnfavouriteContract() {
        val favourite = app("Clock", AppCategory.TOOLS, isFavourite = true)
        var toggled: LaunchTargetId? = null
        composeRule.setShelfContent(
            state = OrganizerShelfUiState(favourites = listOf(favourite)),
            onToggleFavourite = { toggled = it }
        )

        composeRule.onNodeWithContentDescription("Open Clock")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Remove from favourites").performClick()
        composeRule.runOnIdle {
            assertEquals(favourite.launchTargetId, toggled)
        }
    }

    @Test
    fun classificationReportRequestRemainsExplicitAndHostControlled() {
        val signal = app("Signal", AppCategory.COMMUNICATION)
        var requested = false
        composeRule.setShelfContent(
            state =
                OrganizerShelfUiState(
                    categories =
                        listOf(
                            CategorySectionUiModel(AppCategory.COMMUNICATION, listOf(signal))
                        )
                ),
            onClassificationReportRequested = { requested = true }
        )

        composeRule.onNodeWithText("Share classification report")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(true, requested)
        }
    }

    @Test
    fun hiddenManagementRequestAndDismissRemainHostControlled() {
        val hiddenApp = app("Authenticator", AppCategory.TOOLS)
        var showHiddenApps by mutableStateOf(false)
        var requested = false
        var dismissed = false
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                OrganizerShelf(
                    state = OrganizerShelfUiState(hiddenApps = listOf(hiddenApp)),
                    showHiddenApps = showHiddenApps,
                    onQueryChange = {},
                    onLaunchApp = {},
                    onMoveApp = { _, _ -> },
                    onToggleFavourite = {},
                    onHideApp = {},
                    onRestoreApp = {},
                    onClassificationReportRequested = {},
                    onHiddenAppsRequested = {
                        requested = true
                        showHiddenApps = true
                    },
                    onHiddenAppsDismissed = {
                        dismissed = true
                        showHiddenApps = false
                    }
                )
            }
        }

        composeRule.onNodeWithText("Manage hidden apps (1)").performClick()
        composeRule.onNodeWithText("Hidden apps").assertExists()
        composeRule.runOnIdle {
            assertEquals(true, requested)
        }
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithText("Apps").assertExists()
        composeRule.runOnIdle {
            assertEquals(true, dismissed)
        }
    }

    @Test
    fun hiddenManagementRestoresAppsThroughExplicitCallback() {
        val hiddenApp = app("Authenticator", AppCategory.TOOLS)
        var restored: LaunchTargetId? = null
        composeRule.setShelfContent(
            state = OrganizerShelfUiState(hiddenApps = listOf(hiddenApp)),
            showHiddenApps = true,
            onRestoreApp = { restored = it }
        )

        composeRule.onNodeWithText("Hidden apps").assertExists()
        composeRule.onNodeWithContentDescription("Restore Authenticator")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(hiddenApp.launchTargetId, restored)
        }
    }

    @Test
    fun lightAndDarkThemeModesKeepCriticalControlsSemantic() {
        var themeMode by mutableStateOf(OrganizerThemeMode.LIGHT)
        val signal = app("Signal", AppCategory.COMMUNICATION)
        composeRule.setContent {
            OneUiOrganizerTheme(
                themeMode = themeMode,
                dynamicColor = false
            ) {
                OrganizerShelf(
                    state =
                        OrganizerShelfUiState(
                            categories =
                                listOf(
                                    CategorySectionUiModel(
                                        AppCategory.COMMUNICATION,
                                        listOf(signal)
                                    )
                                )
                        ),
                    showHiddenApps = false,
                    onQueryChange = {},
                    onLaunchApp = {},
                    onMoveApp = { _, _ -> },
                    onToggleFavourite = {},
                    onHideApp = {},
                    onRestoreApp = {},
                    onClassificationReportRequested = {},
                    onHiddenAppsRequested = {},
                    onHiddenAppsDismissed = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search apps and categories").assertExists()
        composeRule.onNodeWithContentDescription("Open Signal").assertHasClickAction()
        composeRule.runOnIdle {
            themeMode = OrganizerThemeMode.DARK
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Search apps and categories").assertExists()
        composeRule.onNodeWithContentDescription("Open Signal").assertHasClickAction()
    }

    private fun app(
        label: String,
        category: AppCategory,
        isFavourite: Boolean = false,
        classificationSource: ClassificationSource = ClassificationSource.ANDROID_DECLARED_CATEGORY
    ): ShelfAppUiModel {
        val slug = label.lowercase().replace(" ", "")
        return ShelfAppUiModel(
            launchTargetId = LaunchTargetId("com.example.$slug", "com.example.$slug.Main"),
            label = label,
            category = category,
            classificationSource = classificationSource,
            isFavourite = isFavourite
        )
    }
}

private fun ComposeContentTestRule.setShelfContent(
    state: OrganizerShelfUiState? = null,
    stateProvider: (() -> OrganizerShelfUiState)? = null,
    showHiddenApps: Boolean = false,
    onQueryChange: (String) -> Unit = {},
    onLaunchApp: (LaunchTargetId) -> Unit = {},
    onMoveApp: (LaunchTargetId, CategoryDefinition) -> Unit = { _, _ -> },
    onToggleFavourite: (LaunchTargetId) -> Unit = {},
    onHideApp: (LaunchTargetId) -> Unit = {},
    onRestoreApp: (LaunchTargetId) -> Unit = {},
    onClassificationReportRequested: () -> Unit = {}
) {
    setContent {
        OneUiOrganizerTheme(dynamicColor = false) {
            OrganizerShelf(
                state = stateProvider?.invoke() ?: requireNotNull(state),
                showHiddenApps = showHiddenApps,
                onQueryChange = onQueryChange,
                onLaunchApp = onLaunchApp,
                onMoveApp = onMoveApp,
                onToggleFavourite = onToggleFavourite,
                onHideApp = onHideApp,
                onRestoreApp = onRestoreApp,
                onClassificationReportRequested = onClassificationReportRequested,
                onHiddenAppsRequested = {},
                onHiddenAppsDismissed = {}
            )
        }
    }
}
