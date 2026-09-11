package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.management.CategoryManagement
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryDeletionChoiceUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementItemUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryMoveDirectionUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OrganizerThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryManagementTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun builtInAndCustomCategoriesHaveDifferentEditingAffordances() {
        val family = customCategory("family", "Family")
        composeRule.setManagementContent(
            state =
                CategoryManagementUiState(
                    categories =
                        listOf(
                            CategoryManagementItemUiModel(AppCategory.WORK, assignedAppCount = 5),
                            CategoryManagementItemUiModel(family, assignedAppCount = 2)
                        )
                )
        )

        composeRule.onNodeWithText("Work").assertExists()
        composeRule.onNodeWithText("Built-in category").assertExists()
        composeRule.onNodeWithText("Family").assertExists()
        composeRule.onNodeWithText("Custom category").assertExists()
        composeRule.onNodeWithText("Rename").assertExists()
        composeRule.onNodeWithText("Delete").assertExists()
    }

    @Test
    fun builtInOnlyStateDoesNotOfferRenameOrDelete() {
        composeRule.setManagementContent(
            state =
                CategoryManagementUiState(
                    categories = listOf(CategoryManagementItemUiModel(AppCategory.WORK))
                )
        )

        composeRule.onNodeWithText("Rename").assertDoesNotExist()
        composeRule.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test
    fun createValidatesBlankNameAndReportsTrimmedDraft() {
        var createdName: String? = null
        composeRule.setManagementContent(
            state = CategoryManagementUiState(),
            onCreateCategory = { createdName = it }
        )

        composeRule.onNodeWithText("Create category").performClick()
        composeRule.onNodeWithText("Create", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Enter a category name.").assertExists()
        composeRule.onNodeWithContentDescription("Category name").performTextInput("  Family  ")
        composeRule.onNodeWithText("Create", useUnmergedTree = true).performClick()

        composeRule.runOnIdle {
            assertEquals("Family", createdName)
        }
    }

    @Test
    fun renameReportsStableCategoryIdAndNewDisplayName() {
        val family = customCategory("family", "Family")
        var renamedId: CategoryId? = null
        var renamedName: String? = null
        composeRule.setManagementContent(
            state =
                CategoryManagementUiState(
                    categories = listOf(CategoryManagementItemUiModel(family))
                ),
            onRenameCategory = { id, name ->
                renamedId = id
                renamedName = name
            }
        )

        composeRule.onNodeWithText("Rename").performClick()
        composeRule.onNodeWithContentDescription("Category name").performTextReplacement("Household")
        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(family.id, renamedId)
            assertEquals("Household", renamedName)
        }
    }

    @Test
    fun deletingEmptyCustomCategoryUsesSimpleExplicitConfirmation() {
        val family = customCategory("family", "Family")
        var deleted: Pair<CategoryId, CategoryDeletionChoiceUiModel>? = null
        composeRule.setManagementContent(
            state =
                CategoryManagementUiState(
                    categories = listOf(CategoryManagementItemUiModel(family, assignedAppCount = 0))
                ),
            onDeleteCategory = { id, choice -> deleted = id to choice }
        )

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("This custom category has no assigned apps.", substring = true).assertExists()
        composeRule.onNodeWithText("Delete category").performClick()

        composeRule.runOnIdle {
            assertEquals(family.id, deleted?.first)
            assertTrue(deleted?.second is CategoryDeletionChoiceUiModel.AutomaticClassification)
        }
    }

    @Test
    fun deletingPopulatedCategoryOffersAutomaticOrExplicitReassignment() {
        val family = customCategory("family", "Family")
        var deleted: Pair<CategoryId, CategoryDeletionChoiceUiModel>? = null
        composeRule.setManagementContent(
            state =
                CategoryManagementUiState(
                    categories =
                        listOf(
                            CategoryManagementItemUiModel(family, assignedAppCount = 3),
                            CategoryManagementItemUiModel(AppCategory.WORK, assignedAppCount = 5)
                        )
                ),
            onDeleteCategory = { id, choice -> deleted = id to choice }
        )

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("3 apps are assigned", substring = true).assertExists()
        composeRule.onNodeWithText("Return to automatic classification").assertExists()
        composeRule.onNodeWithText("Move to Work").performClick()
        composeRule.onNodeWithText("Delete category").performClick()

        composeRule.runOnIdle {
            assertEquals(family.id, deleted?.first)
            assertEquals(
                CategoryDeletionChoiceUiModel.Reassign(AppCategory.WORK.id),
                deleted?.second
            )
        }
    }

    @Test
    fun populatedDeleteDefaultsToAutomaticClassificationWhenChosen() {
        val family = customCategory("family", "Family")
        var deleted: Pair<CategoryId, CategoryDeletionChoiceUiModel>? = null
        composeRule.setManagementContent(
            state =
                CategoryManagementUiState(
                    categories =
                        listOf(
                            CategoryManagementItemUiModel(family, assignedAppCount = 1),
                            CategoryManagementItemUiModel(AppCategory.WORK)
                        )
                ),
            onDeleteCategory = { id, choice -> deleted = id to choice }
        )

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Return to automatic classification").performClick()
        composeRule.onNodeWithText("Delete category").performClick()

        composeRule.runOnIdle {
            assertEquals(family.id, deleted?.first)
            assertTrue(deleted?.second is CategoryDeletionChoiceUiModel.AutomaticClassification)
        }
    }

    @Test
    fun reorderUsesDeterministicAccessibleMoveControls() {
        val family = customCategory("family", "Family")
        var moved: Pair<CategoryId, CategoryMoveDirectionUiModel>? = null
        composeRule.setManagementContent(
            state =
                CategoryManagementUiState(
                    categories =
                        listOf(
                            CategoryManagementItemUiModel(AppCategory.WORK),
                            CategoryManagementItemUiModel(family),
                            CategoryManagementItemUiModel(AppCategory.TOOLS)
                        )
                ),
            onMoveCategory = { id, direction -> moved = id to direction }
        )

        composeRule.onNodeWithContentDescription("Move Work up").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Move Family up").assertIsEnabled().performClick()
        composeRule.runOnIdle {
            assertEquals(family.id to CategoryMoveDirectionUiModel.UP, moved)
        }
        composeRule.onNodeWithContentDescription("Move Tools down").assertIsNotEnabled()
    }

    @Test
    fun externalErrorsArePresentedWithoutOwningRepositoryBehavior() {
        composeRule.setManagementContent(
            state = CategoryManagementUiState(operationError = "Could not save category change.")
        )

        composeRule.onNodeWithText("Could not save category change.").assertExists()
    }

    @Test
    fun lightAndDarkThemesKeepManagementControlsAvailable() {
        var themeMode by mutableStateOf(OrganizerThemeMode.LIGHT)
        val family = customCategory("family", "Family")
        composeRule.setContent {
            OneUiOrganizerTheme(
                themeMode = themeMode,
                dynamicColor = false
            ) {
                CategoryManagement(
                    state =
                        CategoryManagementUiState(
                            categories = listOf(CategoryManagementItemUiModel(family))
                        ),
                    onCreateCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> },
                    onMoveCategory = { _, _ -> },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Categories").assertExists()
        composeRule.onNodeWithText("Create category").assertExists()
        composeRule.runOnIdle {
            themeMode = OrganizerThemeMode.DARK
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Categories").assertExists()
        composeRule.onNodeWithText("Create category").assertExists()
    }

    private fun customCategory(opaqueId: String, name: String): CustomCategoryDefinition =
        CustomCategoryDefinition(CategoryId.custom(opaqueId), name)
}

private fun ComposeContentTestRule.setManagementContent(
    state: CategoryManagementUiState,
    onCreateCategory: (String) -> Unit = {},
    onRenameCategory: (CategoryId, String) -> Unit = { _, _ -> },
    onDeleteCategory: (CategoryId, CategoryDeletionChoiceUiModel) -> Unit = { _, _ -> },
    onMoveCategory: (CategoryId, CategoryMoveDirectionUiModel) -> Unit = { _, _ -> }
) {
    setContent {
        OneUiOrganizerTheme(dynamicColor = false) {
            CategoryManagement(
                state = state,
                onCreateCategory = onCreateCategory,
                onRenameCategory = onRenameCategory,
                onDeleteCategory = onDeleteCategory,
                onMoveCategory = onMoveCategory,
                onDismiss = {}
            )
        }
    }
}
