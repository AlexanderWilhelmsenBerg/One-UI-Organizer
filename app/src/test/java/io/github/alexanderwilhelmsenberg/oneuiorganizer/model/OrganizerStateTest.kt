package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OrganizerStateTest {
    @Test
    fun defaultStateStartsAtSchemaVersionTwoWithBuiltInOrder() {
        val state = OrganizerState()

        assertEquals(2, state.schemaVersion)
        assertEquals(OrganizerState.CURRENT_SCHEMA_VERSION, state.schemaVersion)
        assertTrue(state.categoryOverrides.isEmpty())
        assertTrue(state.favouriteAppIds.isEmpty())
        assertTrue(state.hiddenAppIds.isEmpty())
        assertTrue(state.customCategories.isEmpty())
        assertEquals(AppCategory.entries.map(AppCategory::id), state.categoryOrder)
    }

    @Test
    fun everyBuiltInCategoryHasUniqueStableIdentity() {
        val ids = AppCategory.entries.map(AppCategory::id)

        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.all(CategoryId::isBuiltIn))
        AppCategory.entries.forEach { category ->
            assertEquals(category, AppCategory.fromId(category.id))
        }
    }

    @Test
    fun customCategoryIdentitySurvivesDisplayNameChange() {
        val id = CategoryId.custom("durable-identity")
        val original = CustomCategoryDefinition(id = id, displayName = "Original")
        val renamed = original.copy(displayName = "Renamed")

        assertEquals(original.id, renamed.id)
        assertNotEquals(original.displayName, renamed.displayName)
    }

    @Test
    fun normalizedOrderKeepsFirstValidOccurrenceAndAppendsMissingCategories() {
        val alpha = CustomCategoryDefinition(CategoryId.custom("alpha"), "Alpha")
        val state =
            OrganizerState(
                customCategories = listOf(alpha),
                categoryOrder =
                    listOf(
                        alpha.id,
                        AppCategory.WORK.id,
                        alpha.id,
                        CategoryId.custom("missing"),
                        AppCategory.WORK.id
                    )
            )

        val normalized = state.normalizedCategoryOrder()

        assertEquals(listOf(alpha.id, AppCategory.WORK.id), normalized.take(2))
        assertEquals(normalized.distinct(), normalized)
        assertTrue(CategoryId.custom("missing") !in normalized)
        assertEquals(
            AppCategory.entries.map(AppCategory::id).toSet() + alpha.id,
            normalized.toSet()
        )
    }

    @Test
    fun favouritesAreVirtualAndNeverPartOfCategoryOrder() {
        val state = OrganizerState(favouriteAppIds = setOf(AppId("example.favourite")))

        assertEquals(AppCategory.entries.size, state.orderedCategories().size)
        assertTrue(state.orderedCategories().none { category -> category.id.value.contains("favourite") })
    }

    @Test
    fun launchTargetIdentityDistinguishesActivitiesWithinOnePackage() {
        val first = LaunchTargetId("example.package", "example.package.FirstActivity")
        val second = LaunchTargetId("example.package", "example.package.SecondActivity")

        assertTrue(first != second)
    }
}
