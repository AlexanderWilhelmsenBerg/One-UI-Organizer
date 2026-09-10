package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LaunchTargetSelectorTest {
    @Test
    fun `preserves the exact selected package and component`() {
        val target =
            LaunchTargetId(
                packageName = "com.example",
                className = "com.example.GameAlias"
            )

        assertEquals(
            LaunchComponentSpec(
                packageName = "com.example",
                className = "com.example.GameAlias"
            ),
            target.toLaunchComponentSpec()
        )
    }

    @Test
    fun `rejects incomplete launch targets`() {
        assertNull(LaunchTargetId("", "com.example.MainActivity").toLaunchComponentSpec())
        assertNull(LaunchTargetId("com.example", " ").toLaunchComponentSpec())
    }
}
