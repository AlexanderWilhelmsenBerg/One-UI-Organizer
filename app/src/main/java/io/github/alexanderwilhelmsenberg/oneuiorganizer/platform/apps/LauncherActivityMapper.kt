package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.LaunchTargetId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import java.util.Locale

internal data class LauncherActivityMetadata(
    val packageName: String,
    val className: String,
    val label: String,
    val platformCategory: PlatformAppCategory
)

internal object LauncherActivityMapper {
    private val metadataComparator =
        compareBy<LauncherActivityMetadata>(
            { it.label.lowercase(Locale.ROOT) },
            { it.label },
            { it.packageName },
            { it.className }
        )

    fun map(metadata: Iterable<LauncherActivityMetadata>): List<InstalledApp> = metadata
        .sortedWith(metadataComparator)
        .distinctBy { LaunchTargetId(it.packageName, it.className) }
        .map { item ->
            InstalledApp(
                id = AppId(item.packageName),
                launchTargetId = LaunchTargetId(item.packageName, item.className),
                label = item.label,
                platformCategory = item.platformCategory
            )
        }
}
