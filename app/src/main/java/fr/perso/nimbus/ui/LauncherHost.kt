package fr.perso.nimbus.ui

import android.appwidget.AppWidgetProviderInfo
import fr.perso.nimbus.data.AppEntry
import fr.perso.nimbus.data.PendingWidget

/** Actions qui ont besoin de l'activité (résultats d'activité, permissions, fichiers, barres système). */
interface LauncherHost {
    /**
     * Lie le widget (en demandant l'autorisation si besoin), lance sa configuration, puis
     * le pose sur la grille de l'accueil.
     */
    fun addWidget(info: AppWidgetProviderInfo)

    /** Rajoute des widgets un par un (après un import). */
    fun addWidgets(widgets: List<PendingWidget>)

    fun requestContactsPermission()

    /** Accès aux fichiers pour la recherche (« Accès à tous les fichiers » sur Android 11+). */
    fun requestFilesPermission()

    /** Demande à devenir le launcher par défaut (RoleManager, sinon réglages Accueil). */
    fun requestDefaultLauncher()

    /**
     * Icônes sombres (true) ou claires (false) de la barre d'état et des boutons de navigation.
     * Des boutons de la couleur du fond sont quasi invisibles, mais toujours utilisables.
     */
    fun setSystemBars(statusLight: Boolean, navigationLight: Boolean)

    /** Enregistre la configuration dans un fichier choisi par l'utilisateur. */
    fun exportBackup()

    /** Ouvre une sauvegarde Nimbus (.json) ou Smart Launcher (.slbk). */
    fun importBackup()

    /** Choisit le dossier des sauvegardes automatiques. */
    fun chooseAutoBackupFolder()

    /** Choisit une image de la galerie comme icône de l'app. */
    fun pickCustomIcon(app: AppEntry)
}
