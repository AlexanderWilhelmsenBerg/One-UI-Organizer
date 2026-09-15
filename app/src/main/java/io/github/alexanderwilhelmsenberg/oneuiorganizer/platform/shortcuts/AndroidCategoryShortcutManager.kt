package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts

import android.content.Context
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import io.github.alexanderwilhelmsenberg.oneuiorganizer.R
import io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics.AppEventLog
import io.github.alexanderwilhelmsenberg.oneuiorganizer.diagnostics.NoOpAppEventLog
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryDefinition
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCategoryShortcutManager(
    context: Context,
    private val eventLog: AppEventLog = NoOpAppEventLog
) : CategoryShortcutManager {
    private val appContext = context.applicationContext
    private val shortcutManager: ShortcutManager =
        requireNotNull(appContext.getSystemService(ShortcutManager::class.java))

    override val isPinningSupported: Boolean
        get() = try {
            shortcutManager.isRequestPinShortcutSupported
        } catch (exception: RuntimeException) {
            eventLog.record("Shortcut pinning capability query failed", exception)
            false
        }

    override suspend fun synchronize(
        orderedCategories: List<CategoryDefinition>,
        currentCategoryAppCounts: Map<CategoryId, Int>
    ) {
        withContext(Dispatchers.IO) {
            try {
                synchronizeOnWorker(orderedCategories, currentCategoryAppCounts)
            } catch (exception: RuntimeException) {
                // Shortcut publication is best-effort. Device launchers can reject framework calls
                // while locked or during launcher state transitions; retry on the next foreground sync.
                eventLog.record("Category shortcut synchronization failed", exception)
            }
        }
    }

    override fun requestPinShortcut(category: CategoryDefinition): Boolean {
        if (!isPinningSupported) {
            return false
        }
        return try {
            shortcutManager.requestPinShortcut(buildShortcut(category), null)
        } catch (exception: RuntimeException) {
            eventLog.record("Category shortcut pin request failed", exception)
            false
        }
    }

    override fun reportShortcutUsed(categoryId: CategoryId) {
        try {
            shortcutManager.reportShortcutUsed(CategoryShortcutIdentity.shortcutId(categoryId))
        } catch (exception: RuntimeException) {
            // Usage reporting is advisory and must never make launching a category fail.
            eventLog.record("Category shortcut usage reporting failed", exception)
        }
    }

    private fun synchronizeOnWorker(
        orderedCategories: List<CategoryDefinition>,
        currentCategoryAppCounts: Map<CategoryId, Int>
    ) {
        val categoriesByShortcutId =
            orderedCategories.associateBy { category -> CategoryShortcutIdentity.shortcutId(category.id) }
        val categoryPinnedShortcuts =
            shortcutManager.pinnedShortcuts.filter { shortcut ->
                CategoryShortcutIdentity.isCategoryShortcutId(shortcut.id)
            }

        val stalePinnedIds =
            categoryPinnedShortcuts
                .filter { shortcut -> shortcut.isEnabled && shortcut.id !in categoriesByShortcutId }
                .map(ShortcutInfo::getId)
        if (stalePinnedIds.isNotEmpty()) {
            shortcutManager.disableShortcuts(
                stalePinnedIds,
                appContext.getString(R.string.shortcut_category_deleted_disabled_message)
            )
        }

        if (shortcutManager.isRateLimitingActive) {
            return
        }

        val dynamicCategories =
            DynamicCategoryShortcutPolicy.select(
                orderedCategories = orderedCategories,
                currentCategoryAppCounts = currentCategoryAppCounts,
                platformMaximum = shortcutManager.maxShortcutCountPerActivity
            )
        val desiredDynamicShortcuts =
            dynamicCategories.mapIndexed { index, category -> buildShortcut(category, rank = index) }
        val existingDynamicShortcuts = shortcutManager.dynamicShortcuts
        if (!dynamicShortcutsMatch(existingDynamicShortcuts, desiredDynamicShortcuts)) {
            shortcutManager.setDynamicShortcuts(desiredDynamicShortcuts)
        }

        val dynamicIds = desiredDynamicShortcuts.mapTo(mutableSetOf(), ShortcutInfo::getId)
        val pinnedLabelUpdates =
            categoryPinnedShortcuts.mapNotNull { pinnedShortcut ->
                if (!pinnedShortcut.isEnabled || pinnedShortcut.id in dynamicIds) {
                    return@mapNotNull null
                }
                val category = categoriesByShortcutId[pinnedShortcut.id] ?: return@mapNotNull null
                if (pinnedShortcut.shortLabel.toString() == category.displayName) {
                    return@mapNotNull null
                }
                buildShortcut(category)
            }
        if (pinnedLabelUpdates.isNotEmpty()) {
            shortcutManager.updateShortcuts(pinnedLabelUpdates)
        }
    }

    private fun buildShortcut(category: CategoryDefinition, rank: Int? = null): ShortcutInfo {
        val descriptor = CategoryShortcutDescriptor.from(category)
        val builder =
            ShortcutInfo.Builder(appContext, descriptor.shortcutId)
                .setShortLabel(descriptor.label)
                .setLongLabel(
                    appContext.getString(
                        R.string.shortcut_category_long_label,
                        descriptor.label
                    )
                ).setIcon(Icon.createWithResource(appContext, R.drawable.ic_launcher))
                .setIntent(CategoryShortcutIntents.create(appContext, descriptor.destination))
        if (rank != null) {
            builder.setRank(rank)
        }
        return builder.build()
    }

    private fun dynamicShortcutsMatch(existing: List<ShortcutInfo>, desired: List<ShortcutInfo>): Boolean {
        if (existing.size != desired.size) {
            return false
        }
        val existingById = existing.associateBy(ShortcutInfo::getId)
        return desired.all { desiredShortcut ->
            val existingShortcut = existingById[desiredShortcut.id] ?: return@all false
            existingShortcut.isEnabled &&
                existingShortcut.rank == desiredShortcut.rank &&
                existingShortcut.shortLabel.toString() == desiredShortcut.shortLabel.toString()
        }
    }
}
