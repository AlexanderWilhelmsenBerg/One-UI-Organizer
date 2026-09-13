package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.categories.CategoriesOverview
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementItemUiModel
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.model.CategoryManagementUiState
import io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.theme.OneUiOrganizerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoriesOverviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overviewUsesExistingCategoryOrderCountsAndContextualTools() {
        var manageRequested = false
        var backupRequested = false
        val family = CustomCategoryDefinition(CategoryId.custom("family"), "Family")

        composeRule.setContent {
            OneUiOrganizerTheme(dynamicColor = false) {
                CategoriesOverview(
                    state =
                        CategoryManagementUiState(
                            categories =
                                listOf(
                                    CategoryManagementItemUiModel(AppCategory.WORK, assignedAppCount = 3),
                                    CategoryManagementItemUiModel(family, assignedAppCount = 1)
                                )
                        ),
                    onManageCategories = { manageRequested = true },
                    onBackupRestoreRequested = { backupRequested = true }
                )
            }
        }

        composeRule.onNodeWithText("Categories").assertExists()
        composeRule.onNodeWithText("Work").assertExists()
        composeRule.onNodeWithText("Built-in category").assertExists()
        composeRule.onNodeWithText("3 assigned apps").assertExists()
        composeRule.onNodeWithText("Family").assertExists()
        composeRule.onNodeWithText("Custom category").assertExists()
        composeRule.onNodeWithText("1 assigned app").assertExists()

        composeRule.onNodeWithText("Manage categories").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(manageRequested) }

        composeRule.onNodeWithText("Backup & restore").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(backupRequested) }
    }
}
