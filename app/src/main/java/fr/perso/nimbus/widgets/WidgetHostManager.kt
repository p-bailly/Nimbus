package fr.perso.nimbus.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle

/** Hébergement des widgets Android (AppWidgetHost). */
class WidgetHostManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val host: AppWidgetHost = AppWidgetHost(appContext, HOST_ID)

    fun startListening() {
        try {
            host.startListening()
        } catch (e: Exception) {
            // certains appareils lèvent une exception si trop de widgets
        }
    }

    fun stopListening() {
        try {
            host.stopListening()
        } catch (e: Exception) {
            // ignoré
        }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(id: Int) {
        try {
            host.deleteAppWidgetId(id)
        } catch (e: Exception) {
            // ignoré
        }
    }

    fun providers(): List<AppWidgetProviderInfo> = try {
        manager.installedProviders
    } catch (e: Exception) {
        emptyList()
    }

    fun bindIfAllowed(id: Int, info: AppWidgetProviderInfo): Boolean = try {
        manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)
    } catch (e: Exception) {
        false
    }

    fun info(id: Int): AppWidgetProviderInfo? = try {
        manager.getAppWidgetInfo(id)
    } catch (e: Exception) {
        null
    }

    fun createView(context: Context, id: Int): AppWidgetHostView? {
        val info = info(id) ?: return null
        return try {
            host.createView(context, id, info)
        } catch (e: Exception) {
            null
        }
    }

    fun updateSize(id: Int, widthDp: Int, heightDp: Int) {
        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
        }
        try {
            manager.updateAppWidgetOptions(id, options)
        } catch (e: Exception) {
            // ignoré
        }
    }

    companion object {
        const val HOST_ID = 1024

        @Volatile
        private var instance: WidgetHostManager? = null

        fun get(context: Context): WidgetHostManager =
            instance ?: synchronized(this) {
                instance ?: WidgetHostManager(context).also { instance = it }
            }
    }
}
