package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.ClassificationSource
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategorySectionUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.OrganizerShelfUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.ShelfAppUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.shelf.OrganizerShelf
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrganizerCategoryManagementTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun managementEntryIsClearButHostControlled() {
        var requested = false
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                OrganizerShelf(
                    state = OrganizerShelfUiState(),
                    showHiddenApps = false,
                    onQueryChange = {},
                    onLaunchApp = {},
                    onMoveApp = { _, _ -> },
                    onToggleFavourite = {},
                    onHideApp = {},
                    onRestoreApp = {},
                    onClassificationReportRequested = {},
                    onHiddenAppsRequested = {},
                    onHiddenAppsDismissed = {},
                    onCategoryManagementRequested = { requested = true }
                )
            }
        }

        composeRule.onNodeWithText("Manage categories").assertHasClickAction().performClick()
        composeRule.runOnIdle {
            assertEquals(true, requested)
        }
    }

    @Test
    fun customCategoryFromSuppliedOrderAppearsInExistingMovePicker() {
        val family = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val signal =
            ShelfAppUiModel(
                launchTargetId = LaunchTargetId("org.thoughtcrime.securesms", "org.thoughtcrime.securesms.Main"),
                label = "Signal",
                category = AppCategory.COMMUNICATION,
                classificationSource = ClassificationSource.ANDROID_DECLARED_CATEGORY
            )
        var movedCategoryId: CategoryId? = null
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                OrganizerShelf(
                    state =
                        OrganizerShelfUiState(
                            categories =
                                listOf(
                                    CategorySectionUiModel(
                                        category = AppCategory.COMMUNICATION,
                                        apps = listOf(signal)
                                    )
                                ),
                            availableCategories =
                                listOf(
                                    AppCategory.WORK,
                                    family,
                                    AppCategory.COMMUNICATION,
                                    AppCategory.TOOLS
                                )
                        ),
                    showHiddenApps = false,
                    onQueryChange = {},
                    onLaunchApp = {},
                    onMoveApp = { _, category -> movedCategoryId = category.id },
                    onToggleFavourite = {},
                    onHideApp = {},
                    onRestoreApp = {},
                    onClassificationReportRequested = {},
                    onHiddenAppsRequested = {},
                    onHiddenAppsDismissed = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Open Signal")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Add to favourites").assertExists()
        composeRule.onNodeWithText("Hide").assertExists()
        composeRule.onNodeWithText("Move category").performClick()
        composeRule.onNodeWithText("Family").assertHasClickAction().performClick()

        composeRule.runOnIdle {
            assertEquals(family.id, movedCategoryId)
        }
    }
}
