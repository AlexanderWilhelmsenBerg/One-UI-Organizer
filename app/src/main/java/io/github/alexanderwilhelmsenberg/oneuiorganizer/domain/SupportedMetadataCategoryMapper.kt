package io.github.alexanderwilhelmsenberg.oneuiorganizer.domain

import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.AppCategory
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedAppMetadata
import io.github.alexanderwilhelmsenberg.oneuiorganizer.model.SupportedMetadataProvider
import java.util.Locale

object SupportedMetadataCategoryMapper {
    fun categoryFor(metadata: SupportedAppMetadata): AppCategory? = when (metadata.provider) {
        SupportedMetadataProvider.F_DROID -> categoryForFdroid(metadata.categories)
    }

    internal fun categoryForFdroid(categories: Set<String>): AppCategory? {
        val mapped = categories.mapNotNull { category -> FDROID_CATEGORY_MAPPING[category.normalized()] }.toSet()
        return CATEGORY_PRIORITY.firstOrNull(mapped::contains)
    }

    private fun String.normalized(): String = trim().lowercase(Locale.ROOT)

    private val CATEGORY_PRIORITY =
        listOf(
            AppCategory.DEVELOPMENT,
            AppCategory.GAMES,
            AppCategory.COMMUNICATION,
            AppCategory.SOCIAL,
            AppCategory.FINANCE,
            AppCategory.SHOPPING,
            AppCategory.TRAVEL_NAVIGATION,
            AppCategory.MUSIC_AUDIO,
            AppCategory.PHOTOS,
            AppCategory.READING,
            AppCategory.PRODUCTIVITY,
            AppCategory.TOOLS
        )

    private val FDROID_CATEGORY_MAPPING =
        mapOf(
            "development" to AppCategory.DEVELOPMENT,
            "games" to AppCategory.GAMES,
            "educational game" to AppCategory.GAMES,
            "email" to AppCategory.COMMUNICATION,
            "messaging" to AppCategory.COMMUNICATION,
            "phone & sms" to AppCategory.COMMUNICATION,
            "contact" to AppCategory.COMMUNICATION,
            "forum" to AppCategory.SOCIAL,
            "finance manager" to AppCategory.FINANCE,
            "money" to AppCategory.FINANCE,
            "shopping" to AppCategory.SHOPPING,
            "navigation" to AppCategory.TRAVEL_NAVIGATION,
            "public transport" to AppCategory.TRAVEL_NAVIGATION,
            "location tracker & sharer" to AppCategory.TRAVEL_NAVIGATION,
            "music practice tool" to AppCategory.MUSIC_AUDIO,
            "podcast" to AppCategory.MUSIC_AUDIO,
            "radio" to AppCategory.MUSIC_AUDIO,
            "gallery" to AppCategory.PHOTOS,
            "reading" to AppCategory.READING,
            "ebook reader" to AppCategory.READING,
            "news" to AppCategory.READING,
            "bookmark" to AppCategory.READING,
            "calendar & agenda" to AppCategory.PRODUCTIVITY,
            "note" to AppCategory.PRODUCTIVITY,
            "task" to AppCategory.PRODUCTIVITY,
            "writing" to AppCategory.PRODUCTIVITY,
            "calculator" to AppCategory.PRODUCTIVITY,
            "habit tracker" to AppCategory.PRODUCTIVITY,
            "cloud storage & file sync" to AppCategory.PRODUCTIVITY,
            "app manager" to AppCategory.TOOLS,
            "app store & updater" to AppCategory.TOOLS,
            "battery" to AppCategory.TOOLS,
            "clock" to AppCategory.TOOLS,
            "connectivity" to AppCategory.TOOLS,
            "dns & hosts" to AppCategory.TOOLS,
            "file encryption & vault" to AppCategory.TOOLS,
            "file transfer" to AppCategory.TOOLS,
            "firewall" to AppCategory.TOOLS,
            "flashlight" to AppCategory.TOOLS,
            "keyboard & ime" to AppCategory.TOOLS,
            "network analyzer" to AppCategory.TOOLS,
            "password & 2fa" to AppCategory.TOOLS,
            "system" to AppCategory.TOOLS
        )
}
