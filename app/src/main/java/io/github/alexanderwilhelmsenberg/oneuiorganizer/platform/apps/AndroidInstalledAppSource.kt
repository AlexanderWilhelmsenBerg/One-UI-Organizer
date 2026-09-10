package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.InstalledApp
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.PlatformAppCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidInstalledAppSource(
    context: Context,
    private val packageManager: PackageManager = context.packageManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : InstalledAppSource {
    private val organizerPackageName = context.packageName

    override suspend fun loadInstalledApps(): List<InstalledApp> = withContext(ioDispatcher) {
        val launcherIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

        val metadata =
            queryLauncherActivities(launcherIntent)
                .mapNotNull(::toMetadata)
                .filterNot { it.packageName == organizerPackageName }

        LauncherActivityMapper.map(metadata)
    }

    private fun toMetadata(resolveInfo: ResolveInfo): LauncherActivityMetadata? {
        val activityInfo = resolveInfo.activityInfo ?: return null
        val packageName = activityInfo.packageName.takeIf(String::isNotBlank) ?: return null
        val className = activityInfo.name.takeIf(String::isNotBlank) ?: return null
        val label =
            resolveInfo
                .loadLabel(packageManager)
                .toString()
                .trim()
                .ifBlank { packageName }

        return LauncherActivityMetadata(
            packageName = packageName,
            className = className,
            label = label,
            platformCategory = activityInfo.applicationInfo.toPlatformCategory()
        )
    }

    private fun queryLauncherActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            queryLauncherActivitiesLegacy(intent)
        }

    // Min SDK 28 requires the deprecated PackageManager overload before API 33.
    @Suppress("DEPRECATION")
    private fun queryLauncherActivitiesLegacy(intent: Intent): List<ResolveInfo> =
        packageManager.queryIntentActivities(intent, 0)
}

private fun ApplicationInfo.toPlatformCategory(): PlatformAppCategory = when (category) {
    ApplicationInfo.CATEGORY_ACCESSIBILITY -> PlatformAppCategory.ACCESSIBILITY
    ApplicationInfo.CATEGORY_AUDIO -> PlatformAppCategory.AUDIO
    ApplicationInfo.CATEGORY_GAME -> PlatformAppCategory.GAME
    ApplicationInfo.CATEGORY_IMAGE -> PlatformAppCategory.IMAGE
    ApplicationInfo.CATEGORY_MAPS -> PlatformAppCategory.MAPS
    ApplicationInfo.CATEGORY_NEWS -> PlatformAppCategory.NEWS
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> PlatformAppCategory.PRODUCTIVITY
    ApplicationInfo.CATEGORY_SOCIAL -> PlatformAppCategory.SOCIAL
    ApplicationInfo.CATEGORY_VIDEO -> PlatformAppCategory.VIDEO
    else -> PlatformAppCategory.UNDEFINED
}
