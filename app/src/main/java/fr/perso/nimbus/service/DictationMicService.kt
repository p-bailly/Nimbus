package fr.perso.nimbus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import fr.perso.nimbus.R

/**
 * Service de premier plan « micro » tant que le panneau de dictée est ouvert : Android ne
 * laisse écouter qu'une appli visible, ou qui a démarré un tel service quand elle l'était.
 * C'est ce qui permet de continuer à dicter en passant d'une appli à l'autre.
 */
class DictationMicService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            DictationService.instance?.closePanel()
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
        } catch (e: Exception) {
            // démarrage refusé (Nimbus n'était pas au premier plan) : l'écoute marchera tant qu'il l'est
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Dictée pour Claude", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, DictationMicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ms_mic)
            .setContentTitle("Dictée pour Claude")
            .setContentText("Panneau ouvert : vous pouvez changer d'appli")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Fermer", stop).build())
            .build()
    }

    companion object {
        private const val CHANNEL = "dictation"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_STOP = "fr.perso.nimbus.STOP_DICTATION"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, DictationMicService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DictationMicService::class.java))
        }
    }
}
