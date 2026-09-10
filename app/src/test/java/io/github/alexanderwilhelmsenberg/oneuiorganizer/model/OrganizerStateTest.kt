package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrganizerStateTest {
    @Test
    fun defaultStateStartsAtSchemaVersionOneAndContainsNoUserState() {
        val state = OrganizerState()

        assertEquals(1, state.schemaVersion)
        assertEquals(OrganizerState.CURRENT_SCHEMA_VERSION, state.schemaVersion)
        assertTrue(state.categoryOverrides.isEmpty())
        assertTrue(state.favouriteAppIds.isEmpty())
        assertTrue(state.hiddenAppIds.isEmpty())
    }

    @Test
    fun launchTargetIdentityDistinguishesActivitiesWithinOnePackage() {
        val first = LaunchTargetId("example.package", "example.package.FirstActivity")
        val second = LaunchTargetId("example.package", "example.package.SecondActivity")

        assertTrue(first != second)
    }
}
