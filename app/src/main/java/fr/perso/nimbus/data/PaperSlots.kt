package fr.perso.nimbus.data

import java.text.Normalizer

/** Familles des accueils « papier » : chacune a sa couleur d'encre. */
enum class PaperFamily(val label: String, val argb: Long) {
    ECHANGES("Échanges", 0xFF2F5BD3),
    ECRAN("Écran", 0xFFC23A6B),
    TRAJETS("Trajets", 0xFF0D7F63),
    OUTILS("Outils", 0xFF6B46C1),
    JEUX("Jeux & perso", 0xFFA85F0A),
}

/**
 * Les 27 places des accueils « papier », dans l'ordre des figures (6 échanges, 6 écran,
 * 3 trajets, 9 outils, 3 jeux) : les mathématiques des pages reposent sur ces effectifs.
 * Chaque place reçoit l'app choisie par l'utilisateur, sinon la première installée parmi
 * [packages], sinon une app dont le nom contient l'un des [keywords].
 */
enum class PaperSlot(
    val title: String,
    val family: PaperFamily,
    val packages: List<String>,
    val keywords: List<String> = emptyList(),
) {
    WHATSAPP("WhatsApp", PaperFamily.ECHANGES, listOf("com.whatsapp", "com.whatsapp.w4b"), listOf("whatsapp")),
    MESSAGES(
        "Messages", PaperFamily.ECHANGES,
        listOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.messaging"),
        listOf("messages"),
    ),
    DISCORD("Discord", PaperFamily.ECHANGES, listOf("com.discord"), listOf("discord")),
    REDDIT("Reddit", PaperFamily.ECHANGES, listOf("com.reddit.frontpage"), listOf("reddit")),
    TELEPHONE(
        "Téléphone", PaperFamily.ECHANGES,
        listOf("com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer"),
        listOf("telephone", "phone"),
    ),
    GMAIL("Gmail", PaperFamily.ECHANGES, listOf("com.google.android.gm"), listOf("gmail")),
    YOUTUBE("YouTube", PaperFamily.ECRAN, listOf("com.google.android.youtube", "app.revanced.android.youtube"), listOf("youtube")),
    TWITCH("Twitch", PaperFamily.ECRAN, listOf("tv.twitch.android.app"), listOf("twitch")),
    STREMIO("Stremio", PaperFamily.ECRAN, listOf("com.stremio.one"), listOf("stremio")),
    SPOTIFY("Spotify", PaperFamily.ECRAN, listOf("com.spotify.music"), listOf("spotify")),
    PHOTOS("Photos", PaperFamily.ECRAN, listOf("com.google.android.apps.photos"), listOf("photos")),
    CAMERA(
        "Appareil photo", PaperFamily.ECRAN,
        listOf("com.google.android.GoogleCamera", "com.android.camera", "com.android.camera2", "com.sec.android.app.camera"),
        listOf("appareil photo", "camera"),
    ),
    WAZE("Waze", PaperFamily.TRAJETS, listOf("com.waze"), listOf("waze")),
    MAPS("Maps", PaperFamily.TRAJETS, listOf("com.google.android.apps.maps"), listOf("maps")),
    METEO("Météo", PaperFamily.TRAJETS, listOf("fr.meteo", "com.google.android.apps.weather"), listOf("meteo", "weather")),
    CLAUDE("Claude", PaperFamily.OUTILS, listOf("com.anthropic.claude"), listOf("claude")),
    GITHUB("GitHub", PaperFamily.OUTILS, listOf("com.github.android"), listOf("github")),
    DRIVE("Drive", PaperFamily.OUTILS, listOf("com.google.android.apps.docs"), listOf("drive")),
    FICHIERS(
        "Fichiers", PaperFamily.OUTILS,
        listOf("com.google.android.apps.nbu.files", "com.google.android.documentsui", "com.android.documentsui"),
        listOf("fichiers", "files"),
    ),
    AUTHENTICATOR(
        "Authenticator", PaperFamily.OUTILS,
        listOf("com.google.android.apps.authenticator2", "com.azure.authenticator"),
        listOf("authenticator"),
    ),
    BANQUE("Banque Populaire", PaperFamily.OUTILS, listOf("fr.banquepopulaire.cyberplus"), listOf("banque populaire", "+x")),
    PLAY("Play Store", PaperFamily.OUTILS, listOf("com.android.vending"), listOf("play store")),
    AMAZON("Amazon", PaperFamily.OUTILS, listOf("com.amazon.mShop.android.shopping"), listOf("amazon")),
    PRONOTE("Pronote", PaperFamily.OUTILS, listOf("com.IndexEducation.Pronote", "xyz.getpapillon.app"), listOf("pronote", "papillon")),
    LICHESS("Lichess", PaperFamily.JEUX, listOf("org.lichess.mobileV2", "org.lichess.mobileapp"), listOf("lichess")),
    BASKET("Basket", PaperFamily.JEUX, emptyList(), listOf("basket", "nba")),
    PIR("πr", PaperFamily.JEUX, emptyList(), listOf("πr", "pir", "pi r")),
    ;

    companion object {
        /** Les places d'une famille, dans l'ordre des figures. */
        fun of(family: PaperFamily): List<PaperSlot> = entries.filter { it.family == family }

        /**
         * App de chaque place : choix de l'utilisateur ([chosen], s'il est encore installé), puis
         * paquets connus, puis nom. Une app n'occupe qu'une place ; une place sans app reste à null.
         */
        fun resolve(apps: List<AppEntry>, chosen: Map<PaperSlot, String>, byKey: Map<String, AppEntry>): Map<PaperSlot, AppEntry?> {
            val result = LinkedHashMap<PaperSlot, AppEntry?>()
            val used = HashSet<String>()
            for ((slot, key) in chosen) {
                val app = byKey[key] ?: continue
                result[slot] = app
                used += app.key
            }
            // Profil principal d'abord : la version pro d'une app ne passe qu'à défaut.
            val ordered = apps.sortedBy { it.profile != ProfileType.MAIN }
            for (slot in entries) {
                if (slot in result) continue
                val app = slot.packages.firstNotNullOfOrNull { pkg ->
                    ordered.firstOrNull { it.packageName == pkg && it.key !in used }
                } ?: ordered.firstOrNull { app ->
                    app.key !in used && normalize(app.label).let { label -> slot.keywords.any { label.contains(normalize(it)) } }
                }
                result[slot] = app
                if (app != null) used += app.key
            }
            return entries.associateWith { result[it] }
        }

        private val marks = Regex("\\p{Mn}+")

        private fun normalize(text: String): String =
            marks.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")
    }
}
