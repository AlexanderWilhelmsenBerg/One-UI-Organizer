package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts

import android.content.Context
import android.content.Intent
import io.github.alexanderwilhelmsenberg.oneuiorganizer.MainActivity
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryShortcutDestination

/** Android intent encoding kept at the platform boundary; app-owned code sees only the destination. */
object CategoryShortcutIntents {
    const val ACTION_OPEN_CATEGORY =
        "io.github.alexanderwilhelmsenberg.oneuiorganizer.action.OPEN_CATEGORY"
    const val EXTRA_CATEGORY_ID =
        "io.github.alexanderwilhelmsenberg.oneuiorganizer.extra.CATEGORY_ID"

    fun create(context: Context, destination: CategoryShortcutDestination): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_CATEGORY
            putExtra(EXTRA_CATEGORY_ID, destination.categoryId.value)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

    fun destinationFrom(intent: Intent?): CategoryShortcutDestination? {
        if (intent?.action != ACTION_OPEN_CATEGORY) {
            return null
        }
        val rawCategoryId =
            intent.getStringExtra(EXTRA_CATEGORY_ID)?.takeIf(String::isNotBlank)
                ?: return null
        return CategoryShortcutDestination(CategoryId(rawCategoryId))
    }
}
