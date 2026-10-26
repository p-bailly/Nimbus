package fr.perso.nimbus.data

// Applis de messages dont la page de déverrouillage montre les notifications, chacune dans un bandeau
// colorié à sa couleur : WhatsApp, Discord, Messages, Claude, Signal, Telegram, Scrabble…

/** Une appli de messages : son nom affiché et sa couleur (ARGB). */
data class MessageApp(val name: String, val color: Long)

/** Une notification pour la page de déverrouillage. */
data class UnlockNote(
    val key: String,
    val packageName: String,
    val app: MessageApp,
    val title: String,
    val text: String,
    /** Heure de la notification (ms). */
    val time: Long,
)

object MessageApps {

    private val WHATSAPP = MessageApp("WhatsApp", 0xFF25D366)
    private val MESSAGES = MessageApp("Messages", 0xFF1A73E8)

    private val byPackage: Map<String, MessageApp> = mapOf(
        "com.whatsapp" to WHATSAPP,
        "com.whatsapp.w4b" to WHATSAPP,
        "com.discord" to MessageApp("Discord", 0xFF5865F2),
        "com.google.android.apps.messaging" to MESSAGES,
        "com.samsung.android.messaging" to MESSAGES,
        "com.android.mms" to MESSAGES,
        "com.anthropic.claude" to MessageApp("Claude", 0xFFD97757),
        "org.thoughtcrime.securesms" to MessageApp("Signal", 0xFF3A76F0),
        "org.telegram.messenger" to MessageApp("Telegram", 0xFF2AABEE),
        "com.facebook.orca" to MessageApp("Messenger", 0xFF0084FF),
        "com.instagram.android" to MessageApp("Instagram", 0xFFE1306C),
        "com.snapchat.android" to MessageApp("Snapchat", 0xFFF7C600),
        "com.google.android.gm" to MessageApp("Gmail", 0xFFEA4335),
        "com.Slack" to MessageApp("Slack", 0xFF611F69),
        "com.microsoft.teams" to MessageApp("Teams", 0xFF5B5FC7),
        "com.google.android.dialer" to MessageApp("Téléphone", 0xFF188038),
        "com.github.android" to MessageApp("GitHub", 0xFF24292F),
        // Le violet de son crayon : une partie à deux, une invitation, c'est à son tour.
        "fr.perso.scrabble" to MessageApp("Scrabble", 0xFF7A5BB8),
    )

    /** Au plus trois bandeaux : la page reste lisible. */
    const val MAX_NOTES = 3

    /** L'appli de messages de [packageName], ou null : ses notifications ne vont pas sur la page. */
    fun of(packageName: String): MessageApp? = byPackage[packageName]

    /**
     * Les notifications à montrer, les plus récentes d'abord : celles des applis de messages, effaçables
     * (pas une sauvegarde ou une session en cours), avec un titre ou un texte.
     */
    fun notes(
        all: List<Candidate>,
    ): List<UnlockNote> = all
        .asSequence()
        .filter { it.clearable && (it.title.isNotBlank() || it.text.isNotBlank()) }
        .mapNotNull { c -> of(c.packageName)?.let { UnlockNote(c.key, c.packageName, it, c.title.trim(), c.text.trim(), c.time) } }
        .sortedByDescending { it.time }
        .toList()

    /** Ce qu'il faut savoir d'une notification du système pour la trier. */
    data class Candidate(
        val key: String,
        val packageName: String,
        val title: String,
        val text: String,
        val time: Long,
        val clearable: Boolean,
    )
}
