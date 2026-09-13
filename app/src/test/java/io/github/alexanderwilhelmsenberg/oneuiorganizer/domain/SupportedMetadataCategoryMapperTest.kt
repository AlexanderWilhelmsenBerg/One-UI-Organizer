package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupportedMetadataCategoryMapperTest {
    @Test
    fun mapsOnlyExplicitFdroidCategories() {
        val expected =
            mapOf(
                "Development" to AppCategory.DEVELOPMENT,
                "Games" to AppCategory.GAMES,
                "Messaging" to AppCategory.COMMUNICATION,
                "Forum" to AppCategory.SOCIAL,
                "Finance Manager" to AppCategory.FINANCE,
                "Shopping" to AppCategory.SHOPPING,
                "Navigation" to AppCategory.TRAVEL_NAVIGATION,
                "Podcast" to AppCategory.MUSIC_AUDIO,
                "Gallery" to AppCategory.PHOTOS,
                "Ebook Reader" to AppCategory.READING,
                "Calendar & Agenda" to AppCategory.PRODUCTIVITY,
                "Firewall" to AppCategory.TOOLS
            )

        expected.forEach { (source, category) ->
            assertEquals(category, SupportedMetadataCategoryMapper.categoryForFdroid(setOf(source)))
        }
    }

    @Test
    fun ignoresUnknownOrAmbiguousCategories() {
        assertNull(SupportedMetadataCategoryMapper.categoryForFdroid(setOf("Internet", "Multimedia", "Unknown")))
    }

    @Test
    fun multipleMappedCategoriesUseStableAppOwnedPriority() {
        val first = SupportedMetadataCategoryMapper.categoryForFdroid(setOf("Note", "Games", "Firewall"))
        val second = SupportedMetadataCategoryMapper.categoryForFdroid(setOf("Firewall", "Games", "Note"))

        assertEquals(AppCategory.GAMES, first)
        assertEquals(first, second)
    }
}
