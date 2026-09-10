package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherActivityMapperTest {
    @Test
    fun `maps launcher metadata into app-owned identity`() {
        val apps =
            LauncherActivityMapper.map(
                listOf(
                    LauncherActivityMetadata(
                        packageName = "com.example.notes",
                        className = "com.example.notes.MainActivity",
                        label = "Notes",
                        platformCategory = PlatformAppCategory.PRODUCTIVITY
                    )
                )
            )

        val app = apps.single()

        assertEquals(AppId("com.example.notes"), app.id)
        assertEquals(
            LaunchTargetId(
                packageName = "com.example.notes",
                className = "com.example.notes.MainActivity"
            ),
            app.launchTargetId
        )
        assertEquals("Notes", app.label)
        assertEquals(PlatformAppCategory.PRODUCTIVITY, app.platformCategory)
    }

    @Test
    fun `collapses duplicate rows but preserves distinct aliases`() {
        val apps =
            LauncherActivityMapper.map(
                listOf(
                    metadata(className = "com.example.MainActivity", label = "Example"),
                    metadata(className = "com.example.MainActivity", label = "Example"),
                    metadata(className = "com.example.GameAlias", label = "Example Game")
                )
            )

        assertEquals(
            listOf(
                LaunchTargetId("com.example", "com.example.MainActivity"),
                LaunchTargetId("com.example", "com.example.GameAlias")
            ),
            apps.map { it.launchTargetId }
        )
    }

    @Test
    fun `ordering is deterministic by label package and component`() {
        val apps =
            LauncherActivityMapper.map(
                listOf(
                    metadata(
                        packageName = "com.example.zulu",
                        className = "com.example.zulu.Main",
                        label = "Zulu"
                    ),
                    metadata(
                        packageName = "com.example.beta",
                        className = "com.example.beta.Main",
                        label = "alpha"
                    ),
                    metadata(
                        packageName = "com.example.alpha",
                        className = "com.example.alpha.Main",
                        label = "Alpha"
                    )
                )
            )

        assertEquals(
            listOf(
                LaunchTargetId("com.example.alpha", "com.example.alpha.Main"),
                LaunchTargetId("com.example.beta", "com.example.beta.Main"),
                LaunchTargetId("com.example.zulu", "com.example.zulu.Main")
            ),
            apps.map { it.launchTargetId }
        )
    }

    private fun metadata(packageName: String = "com.example", className: String, label: String) =
        LauncherActivityMetadata(
            packageName = packageName,
            className = className,
            label = label,
            platformCategory = PlatformAppCategory.UNDEFINED
        )
}
