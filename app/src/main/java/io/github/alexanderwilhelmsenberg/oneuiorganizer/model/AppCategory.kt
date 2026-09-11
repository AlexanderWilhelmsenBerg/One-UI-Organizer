package io.github.alexanderwilhelmsenberg.oneuiorganizer.model

enum class AppCategory(
    val id: CategoryId,
    val displayName: String
) {
    COMMUNICATION(CategoryId("builtin:communication"), "Communication"),
    SOCIAL(CategoryId("builtin:social"), "Social"),
    WORK(CategoryId("builtin:work"), "Work"),
    PRODUCTIVITY(CategoryId("builtin:productivity"), "Productivity"),
    SMART_HOME(CategoryId("builtin:smart-home"), "Smart Home"),
    HOMELAB(CategoryId("builtin:homelab"), "Homelab"),
    FINANCE(CategoryId("builtin:finance"), "Finance"),
    SHOPPING(CategoryId("builtin:shopping"), "Shopping"),
    TRAVEL_NAVIGATION(CategoryId("builtin:travel-navigation"), "Travel & Navigation"),
    MUSIC_AUDIO(CategoryId("builtin:music-audio"), "Music & Audio"),
    VIDEO(CategoryId("builtin:video"), "Video"),
    PHOTOS(CategoryId("builtin:photos"), "Photos"),
    READING(CategoryId("builtin:reading"), "Reading"),
    WEB_SHORTCUTS(CategoryId("builtin:web-shortcuts"), "Web Shortcuts"),
    DEVELOPMENT(CategoryId("builtin:development"), "Development"),
    TOOLS(CategoryId("builtin:tools"), "Tools"),
    EMULATORS(CategoryId("builtin:emulators"), "Emulators"),
    GAME_ACTION_ADVENTURE(CategoryId("builtin:game-action-adventure"), "Action & Adventure"),
    GAME_RPG(CategoryId("builtin:game-rpg"), "RPG"),
    GAME_STRATEGY_SIMULATION(CategoryId("builtin:game-strategy-simulation"), "Strategy & Simulation"),
    GAME_PUZZLE_CASUAL(CategoryId("builtin:game-puzzle-casual"), "Puzzle & Casual"),
    GAME_BOARD_CARD(CategoryId("builtin:game-board-card"), "Board & Card"),
    GAMES(CategoryId("builtin:games"), "Games"),
    OTHER(CategoryId("builtin:other"), "Other"),
    UNSORTED(CategoryId("builtin:unsorted"), "Unsorted");

    companion object {
        fun fromId(id: CategoryId): AppCategory? = entries.firstOrNull { category -> category.id == id }
    }
}
