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
    private val THUNDERBIRD = MessageApp("Thunderbird", 0xFF1F6FE5)

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
        "net.thunderbird.android" to THUNDERBIRD,
        "net.thunderbird.android.beta" to THUNDERBIRD,
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

    /** Valeurs de `Notification.VISIBILITY_*` (recopiées : ce fichier n'utilise pas Android, pour ses essais). */
    const val VISIBILITY_SECRET = -1
    const val VISIBILITY_PRIVATE = 0
    const val VISIBILITY_PUBLIC = 1

    /** Ce que montre un message privé quand le contenu sensible est masqué et que l'appli ne donne pas de version publique. */
    const val HIDDEN_TEXT = "Contenu masqué"

    /**
     * Les notifications à montrer, les plus récentes d'abord : celles des applis de messages, effaçables
     * (pas une sauvegarde ou une session en cours), avec un titre ou un texte.
     *
     * La page et le fond animé sont posés sur l'écran verrouillé : ils suivent ses règles. Rien si
     * [showNotifications] est faux (« ne pas afficher les notifications » sur l'écran verrouillé) ; jamais une
     * notification SECRET ; une notification PRIVATE (le cas par défaut) en entier seulement si [showPrivate]
     * (« afficher le contenu sensible » du système), sinon sa version publique, ou l'appli seule et
     * [HIDDEN_TEXT].
     */
    fun notes(
        all: List<Candidate>,
        showPrivate: Boolean = false,
        showNotifications: Boolean = true,
    ): List<UnlockNote> = if (!showNotifications) emptyList() else all
        .asSequence()
        .filter { it.clearable && (it.title.isNotBlank() || it.text.isNotBlank()) && it.lockVisibility != VISIBILITY_SECRET }
        .mapNotNull { c ->
            val app = of(c.packageName) ?: return@mapNotNull null
            val full = c.lockVisibility == VISIBILITY_PUBLIC || showPrivate
            val (title, text) = when {
                full -> c.title.trim() to c.text.trim()
                c.publicTitle.isNotBlank() || c.publicText.isNotBlank() -> c.publicTitle.trim() to c.publicText.trim()
                else -> "" to HIDDEN_TEXT
            }
            UnlockNote(c.key, c.packageName, app, title, text, c.time)
        }
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
        /** Visibilité sur l'écran verrouillé ([VISIBILITY_PUBLIC] dans les essais qui ne s'en occupent pas). */
        val lockVisibility: Int = VISIBILITY_PUBLIC,
        val publicTitle: String = "",
        val publicText: String = "",
    )
}
