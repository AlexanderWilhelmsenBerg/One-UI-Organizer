package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.management.CategoryManagement
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementItemUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryShortcutManagementTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinActionReturnsTheStableBuiltInCategoryDefinition() {
        var pinned: CategoryDefinition? = null
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                CategoryManagement(
                    state =
                        CategoryManagementUiState(
                            categories = listOf(CategoryManagementItemUiModel(AppCategory.WORK))
                        ),
                    onCreateCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> },
                    onMoveCategory = { _, _ -> },
                    onDismiss = {},
                    pinningSupported = true,
                    onPinCategory = { pinned = it }
                )
            }
        }

        composeRule.onNodeWithText("Pin to Home screen").performClick()
        composeRule.runOnIdle {
            assertEquals(AppCategory.WORK.id, pinned?.id)
        }
    }

    @Test
    fun pinActionSupportsCustomCategoryWithoutUsingItsDisplayNameAsIdentity() {
        val custom = CustomCategoryDefinition(CategoryId.custom("opaque-family"), "Family")
        var pinned: CategoryDefinition? = null
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                CategoryManagement(
                    state =
                        CategoryManagementUiState(
                            categories = listOf(CategoryManagementItemUiModel(custom))
                        ),
                    onCreateCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> },
                    onMoveCategory = { _, _ -> },
                    onDismiss = {},
                    pinningSupported = true,
                    onPinCategory = { pinned = it }
                )
            }
        }

        composeRule.onNodeWithText("Pin to Home screen").performClick()
        composeRule.runOnIdle {
            assertEquals(CategoryId.custom("opaque-family"), pinned?.id)
        }
    }

    @Test
    fun unsupportedLauncherExplainsAndDisablesPinning() {
        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                CategoryManagement(
                    state =
                        CategoryManagementUiState(
                            categories = listOf(CategoryManagementItemUiModel(AppCategory.WORK))
                        ),
                    onCreateCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> },
                    onMoveCategory = { _, _ -> },
                    onDismiss = {},
                    pinningSupported = false
                )
            }
        }

        composeRule.onNodeWithText("Pin to Home screen").assertIsNotEnabled()
        composeRule
            .onNodeWithText("The current launcher does not support in-app Home screen shortcut pinning.")
            .assertExists()
    }
}
