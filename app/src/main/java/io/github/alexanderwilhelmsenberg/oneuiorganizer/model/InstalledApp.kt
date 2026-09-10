package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

data class InstalledApp(
    val id: AppId,
    val launchTargetId: LaunchTargetId,
    val label: String,
    val platformCategory: PlatformAppCategory = PlatformAppCategory.UNDEFINED
)
