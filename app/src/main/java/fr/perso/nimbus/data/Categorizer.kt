package fr.perso.nimbus.data

import android.content.pm.ApplicationInfo

/**
 * Classement automatique des apps par catégorie :
 * 1) table de paquets connus, 2) mots-clés dans le nom du paquet,
 * 3) catégorie déclarée par le développeur, 4) apps système -> Outils.
 */
object Categorizer {

    private val exact = mapOf(
        "com.google.android.gm" to AppCategory.COMMUNICATION,
        "com.google.android.apps.messaging" to AppCategory.COMMUNICATION,
        "com.android.vending" to AppCategory.TOOLS,
        "com.google.android.googlequicksearchbox" to AppCategory.INTERNET,
        "com.google.android.apps.maps" to AppCategory.TRAVEL,
        "com.google.android.apps.photos" to AppCategory.PHOTO,
        "com.google.android.calendar" to AppCategory.PRODUCTIVITY,
        "com.google.android.apps.walletnfcrel" to AppCategory.SHOPPING,
    )

    // L'ordre compte : la première catégorie dont un mot-clé correspond l'emporte.
    private val keywords: List<Pair<AppCategory, List<String>>> = listOf(
        AppCategory.COMMUNICATION to listOf(
            "whatsapp", "telegram", "signal", "messag", "mms", "sms", "dialer", "contacts",
            "outlook", "skype", "viber", "teams", "zoom", "thunderbird", "protonmail", "k9", "email",
        ),
        AppCategory.SOCIAL to listOf(
            "facebook", "instagram", "twitter", "tiktok", "musically", "snapchat", "reddit",
            "linkedin", "pinterest", "mastodon", "bsky", "bluesky", "discord", "tumblr", "bereal",
        ),
        AppCategory.INTERNET to listOf(
            "chrome", "firefox", "browser", "opera", "brave", "duckduckgo", "emmx", "vivaldi",
        ),
        AppCategory.MEDIA to listOf(
            "youtube", "spotify", "deezer", "netflix", "primevideo", "avod", "disney", "music",
            "podcast", "vlc", "twitch", "soundcloud", "canalplus", "molotov", "plex", "radio",
            "player", "video", "audible",
        ),
        AppCategory.PHOTO to listOf(
            "camera", "gallery", "photo", "snapseed", "lightroom", "picsart", "gcam",
        ),
        AppCategory.GAMES to listOf(
            "game", "supercell", "rovio", "nintendo", "chess", "puzzle", "king.",
        ),
        AppCategory.TRAVEL to listOf(
            "maps", "waze", "uber", "bolt", "sncf", "ouigo", "trainline", "blablacar", "airbnb",
            "booking", "citymapper", "ratp", "tripadvisor", "expedia", "flight", "airfrance",
            "navigation", "gps",
        ),
        AppCategory.SHOPPING to listOf(
            "amazon.mshop", "ebay", "vinted", "leboncoin", "aliexpress", "shein", "temu", "shop",
            "cdiscount", "fnac", "paypal", "revolut", "lydia", "boursorama", "bank", "banque", "bnp",
            "creditagricole", "societegenerale", "lcl", "fortuneo", "n26", "wallet", "crypto",
        ),
        AppCategory.NEWS to listOf(
            "news", "lemonde", "lefigaro", "liberation", "ouestfrance", "20minutes", "franceinfo",
            "flipboard", "feedly", "kindle", "medium", "wikipedia", "book", "reader",
        ),
        AppCategory.HEALTH to listOf(
            "health", "fitness", "fitbit", "strava", "workout", "doctolib", "ameli", "meditat",
            "yoga", "garmin", "decathlon", "runtastic", "sleep",
        ),
        AppCategory.TOOLS to listOf(
            "settings", "calculator", "calc", "clock", "files", "filemanager", "documentsui",
            "flashlight", "torch", "weather", "meteo", "vpn", "password", "bitwarden",
            "authenticator", "keyboard", "inputmethod", "compass", "recorder", "backup",
            "cleaner", "launcher", "bluetooth",
        ),
        AppCategory.PRODUCTIVITY to listOf(
            "docs", "sheets", "slides", "office", "word", "excel", "powerpoint", "drive",
            "dropbox", "calendar", "notes", "keep", "notion", "todo", "evernote", "slack",
            "trello", "onenote", "pdf", "scanner", "translate", "tasks", "obsidian",
        ),
    )

    fun categorize(info: ApplicationInfo): AppCategory {
        val pkg = info.packageName.lowercase()
        exact[pkg]?.let { return it }
        for ((category, words) in keywords) {
            if (words.any { pkg.contains(it) }) return category
        }
        return when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> AppCategory.MEDIA
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.PHOTO
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.TRAVEL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            else ->
                if (info.flags and ApplicationInfo.FLAG_SYSTEM != 0) AppCategory.TOOLS
                else AppCategory.OTHER
        }
    }
}
