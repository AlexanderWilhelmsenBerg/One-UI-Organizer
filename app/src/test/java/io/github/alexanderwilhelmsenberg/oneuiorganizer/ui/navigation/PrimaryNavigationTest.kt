package io.github.alexanderwilhelmsenberg.oneuiorganizer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class PrimaryNavigationTest {
    @Test
    fun organizerIsInitialAndFallbackDestination() {
        assertEquals(PrimaryDestination.ORGANIZER, PrimaryDestination.fromSavedValue(null))
        assertEquals(PrimaryDestination.ORGANIZER, PrimaryDestination.fromSavedValue("unknown"))
    }

    @Test
    fun categoriesDestinationRestoresFromSavedPresentationValue() {
        assertEquals(
            PrimaryDestination.CATEGORIES,
            PrimaryDestination.fromSavedValue(PrimaryDestination.CATEGORIES.savedValue)
        )
    }

    @Test
    fun categoryShortcutAlwaysTargetsOrganizerPrimaryDestination() {
        assertEquals(PrimaryDestination.ORGANIZER, PrimaryDestination.forCategoryShortcut())
    }

    @Test
    fun contextualCategoryFlowsReturnToCategoriesPrimaryDestination() {
        assertEquals(PrimaryDestination.CATEGORIES, PrimaryDestination.forCategoryManagement())
        assertEquals(PrimaryDestination.CATEGORIES, PrimaryDestination.forBackupRestore())
    }

    @Test
    fun backPriorityClosesNestedPresentationBeforeSystemBack() {
        assertEquals(
            PrimaryBackAction.DISMISS_BACKUP_RESTORE,
            primaryBackAction(
                showBackupRestore = true,
                showCategoryManagement = true,
                showHiddenApps = true
            )
        )
        assertEquals(
            PrimaryBackAction.DISMISS_CATEGORY_MANAGEMENT,
            primaryBackAction(
                showBackupRestore = false,
                showCategoryManagement = true,
                showHiddenApps = true
            )
        )
        assertEquals(
            PrimaryBackAction.DISMISS_HIDDEN_APPS,
            primaryBackAction(
                showBackupRestore = false,
                showCategoryManagement = false,
                showHiddenApps = true
            )
        )
    }

    @Test
    fun primaryRootsUseSystemBackInsteadOfSyntheticTabHistory() {
        assertEquals(
            PrimaryBackAction.SYSTEM,
            primaryBackAction(
                showBackupRestore = false,
                showCategoryManagement = false,
                showHiddenApps = false
            )
        )
    }
}
