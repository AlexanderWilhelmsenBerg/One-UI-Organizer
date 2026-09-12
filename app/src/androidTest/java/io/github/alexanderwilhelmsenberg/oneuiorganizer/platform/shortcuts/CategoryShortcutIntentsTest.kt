package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts

import android.content.Intent
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryShortcutIntentsTest {
    @Test
    fun validAndroidPayloadResolvesToAppOwnedCategoryDestination() {
        val intent =
            Intent().apply {
                action = CategoryShortcutIntents.ACTION_OPEN_CATEGORY
                putExtra(
                    CategoryShortcutIntents.EXTRA_CATEGORY_ID,
                    AppCategory.WORK.id.value
                )
            }

        val destination = CategoryShortcutIntents.destinationFrom(intent)

        assertEquals(AppCategory.WORK.id, destination?.categoryId)
    }

    @Test
    fun unrelatedOrMalformedAndroidPayloadDoesNotResolve() {
        val unrelatedIntent =
            Intent().apply {
                action = Intent.ACTION_MAIN
                putExtra(
                    CategoryShortcutIntents.EXTRA_CATEGORY_ID,
                    AppCategory.WORK.id.value
                )
            }
        val missingCategoryIntent =
            Intent().apply {
                action = CategoryShortcutIntents.ACTION_OPEN_CATEGORY
            }

        assertNull(CategoryShortcutIntents.destinationFrom(unrelatedIntent))
        assertNull(CategoryShortcutIntents.destinationFrom(missingCategoryIntent))
    }
}
