package fr.perso.nimbus.ui

import android.appwidget.AppWidgetProviderInfo

/** Actions qui ont besoin de l'activité (résultats d'activité, permissions, barres système). */
interface LauncherHost {
    /** Lie le widget (en demandant l'autorisation si besoin), puis lance sa configuration. */
    fun addWidget(info: AppWidgetProviderInfo)

    fun requestContactsPermission()

    /** Demande à devenir le launcher par défaut (RoleManager, sinon réglages Accueil). */
    fun requestDefaultLauncher()

    /** Icônes sombres dans les barres système (true) ou claires (false). */
    fun setLightSystemBars(light: Boolean)
}
