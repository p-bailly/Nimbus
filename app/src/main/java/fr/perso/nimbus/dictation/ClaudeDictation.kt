package fr.perso.nimbus.dictation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.perso.nimbus.data.Prefs
import fr.perso.nimbus.service.DictationService
import fr.perso.nimbus.service.SystemActions

/**
 * Dictée vers Claude : on transcrit la voix, puis on ouvre l'appli Claude sur la conversation
 * choisie et le service d'accessibilité y écrit le texte et appuie sur Envoyer.
 * Aucune API : c'est l'appli Claude (et le compte de l'utilisateur) qui envoie.
 */
object ClaudeDictation {

    const val CLAUDE_PACKAGE = "com.anthropic.claude"

    /**
     * Ouvre la dictée par-dessus l'appli en cours : panneau flottant (on peut changer d'appli
     * en dictant) si le service d'accessibilité est activé, sinon écran de dictée.
     */
    fun start(context: Context) {
        DictationService.instance?.captureOrigin()
        context.startActivity(intent(context))
    }

    fun intent(context: Context): Intent {
        val screen = if (DictationService.isEnabled) DictationStarterActivity::class.java else DictationActivity::class.java
        return Intent(context, screen).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Envoie [text] dans la conversation Claude ; repli : texte copié, à coller. */
    fun send(context: Context, text: String) {
        val settings = Prefs.get(context).settings.value
        if (!openClaude(context, settings.claudeSessionUrl)) {
            copy(context, text)
            SystemActions.toast(context, "Appli Claude introuvable : texte copié")
            return
        }
        val service = DictationService.instance
        if (service == null) {
            copy(context, text)
            SystemActions.toast(context, "Texte copié : collez-le dans Claude. Activez « Dictée Claude Nimbus » pour l'envoi automatique")
            return
        }
        service.deliver(text, returnToOrigin = settings.dictationReturn)
    }

    /** La conversation enregistrée si l'appli Claude sait l'ouvrir, sinon l'appli elle-même. */
    private fun openClaude(context: Context, sessionUrl: String?): Boolean {
        if (!sessionUrl.isNullOrBlank()) {
            val link = Intent(Intent.ACTION_VIEW, Uri.parse(sessionUrl.trim()))
                .setPackage(CLAUDE_PACKAGE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(link)
                return true
            } catch (e: ActivityNotFoundException) {
                // lien non géré par l'appli : on l'ouvre simplement
            }
        }
        val launch = context.packageManager.getLaunchIntentForPackage(CLAUDE_PACKAGE) ?: return false
        return try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Copie la dictée pour la coller soi-même, où l'on veut. */
    fun copyText(context: Context, text: String) {
        copy(context, text)
        SystemActions.toast(context, "Texte copié : collez-le où vous voulez")
    }

    fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Dictée", text))
    }
}
