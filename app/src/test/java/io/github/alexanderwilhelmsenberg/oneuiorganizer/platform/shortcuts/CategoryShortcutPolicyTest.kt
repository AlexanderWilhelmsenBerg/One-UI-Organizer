package io.github.alexanderwilhelmsenberg.oneuiorganizer.platform.shortcuts

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CategoryId
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.CustomCategoryDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryShortcutPolicyTest {
    @Test
    fun shortcutIdentityUsesStableCategoryIdInsteadOfLabelOrPosition() {
        val id = CategoryId.custom("opaque-family-id")
        val beforeRename = CustomCategoryDefinition(id, "Family")
        val afterRename = CustomCategoryDefinition(id, "Household")

        assertEquals(
            CategoryShortcutIdentity.shortcutId(beforeRename.id),
            CategoryShortcutIdentity.shortcutId(afterRename.id)
        )
        assertEquals("category:custom:opaque-family-id", CategoryShortcutIdentity.shortcutId(id))
        assertTrue(
            CategoryShortcutIdentity.isCategoryShortcutId(
                CategoryShortcutIdentity.shortcutId(id)
            )
        )
        assertFalse(CategoryShortcutIdentity.isCategoryShortcutId("unrelated"))
    }

    @Test
    fun reorderChangesSelectionOrderWithoutChangingIndividualIdentities() {
        val firstOrder = listOf(AppCategory.WORK, AppCategory.TOOLS)
        val secondOrder = listOf(AppCategory.TOOLS, AppCategory.WORK)
        val counts = mapOf(AppCategory.WORK.id to 2, AppCategory.TOOLS.id to 3)

        val first = DynamicCategoryShortcutPolicy.select(firstOrder, counts, platformMaximum = 4)
        val second = DynamicCategoryShortcutPolicy.select(secondOrder, counts, platformMaximum = 4)

        assertEquals(listOf(AppCategory.WORK, AppCategory.TOOLS), first)
        assertEquals(listOf(AppCategory.TOOLS, AppCategory.WORK), second)
        assertEquals(
            "category:${AppCategory.WORK.id.value}",
            CategoryShortcutIdentity.shortcutId(AppCategory.WORK.id)
        )
    }

    @Test
    fun selectionSkipsEmptyCategoriesAndSupportsBuiltInAndCustomCategories() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val ordered = listOf(AppCategory.SOCIAL, custom, AppCategory.WORK)
        val counts = mapOf(custom.id to 1, AppCategory.WORK.id to 2)

        val selected = DynamicCategoryShortcutPolicy.select(ordered, counts, platformMaximum = 4)

        assertEquals(listOf(custom, AppCategory.WORK), selected)
    }

    @Test
    fun selectionRespectsBothProductAndPlatformMaximums() {
        val ordered =
            listOf(
                AppCategory.COMMUNICATION,
                AppCategory.SOCIAL,
                AppCategory.WORK,
                AppCategory.PRODUCTIVITY,
                AppCategory.TOOLS
            )
        val counts = ordered.associate { category -> category.id to 1 }

        assertEquals(
            ordered.take(DynamicCategoryShortcutPolicy.RECOMMENDED_LAUNCHER_MENU_LIMIT),
            DynamicCategoryShortcutPolicy.select(ordered, counts, platformMaximum = 10)
        )
        assertEquals(
            ordered.take(2),
            DynamicCategoryShortcutPolicy.select(ordered, counts, platformMaximum = 2)
        )
        assertTrue(DynamicCategoryShortcutPolicy.select(ordered, counts, platformMaximum = 0).isEmpty())
    }

    @Test
    fun deletedCategoryDisappearsWhenItIsNoLongerInAuthoritativeOrder() {
        val custom = CustomCategoryDefinition(CategoryId.custom("family"), "Family")
        val beforeDelete = listOf(custom, AppCategory.WORK)
        val afterDelete = listOf(AppCategory.WORK)
        val counts = mapOf(custom.id to 2, AppCategory.WORK.id to 1)

        assertEquals(
            listOf(custom, AppCategory.WORK),
            DynamicCategoryShortcutPolicy.select(beforeDelete, counts, platformMaximum = 4)
        )
        assertEquals(
            listOf(AppCategory.WORK),
            DynamicCategoryShortcutPolicy.select(afterDelete, counts, platformMaximum = 4)
        )
    }
}
