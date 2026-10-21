package fr.perso.nimbus.service

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

/** État des réglages du bandeau papier ; null : inconnu (ou illisible sans autorisation). */
data class ToggleState(
    val wifi: Boolean? = null,
    val bluetooth: Boolean? = null,
    val airplane: Boolean = false,
    val rotation: Boolean = false,
    /** Prochaine alarme (ms), null s'il n'y en a pas. */
    val nextAlarm: Long? = null,
)

/**
 * Réglages rapides du bandeau papier. Une appli ne peut plus allumer le Wi-Fi, le Bluetooth ou le
 * mode avion elle-même : un appui ouvre le panneau du système (Wi-Fi, Internet) ou la page des réglages ;
 * la lampe et la rotation automatique (avec l'autorisation « Modifier les paramètres système ») se
 * changent directement.
 */
object QuickToggles {

    fun read(context: Context): ToggleState {
        val wifi = runCatching { context.getSystemService(WifiManager::class.java)?.isWifiEnabled }.getOrNull()
        val bluetooth = runCatching { context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled }.getOrNull()
        val resolver = context.contentResolver
        val airplane = Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        val rotation = Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        val alarm = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
        return ToggleState(wifi, bluetooth, airplane, rotation, alarm)
    }

    private fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    fun wifi(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && open(context, Intent(Settings.Panel.ACTION_WIFI))) return
        open(context, Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun mobileData(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && open(context, Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))) return
        open(context, Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
    }

    fun bluetooth(context: Context) {
        open(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    fun airplane(context: Context) {
        open(context, Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    }

    /** Portefeuille (Google Wallet), sinon les paiements sans contact. */
    fun wallet(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.walletnfcrel")
        if (intent != null && open(context, intent)) return
        open(context, Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS))
    }

    /** La caméra arrière qui a un flash, null s'il n'y en a pas. */
    fun torchCamera(context: Context): String? {
        val camera = context.getSystemService(CameraManager::class.java) ?: return null
        return runCatching {
            camera.cameraIdList.firstOrNull { id ->
                val c = camera.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }

    fun setTorch(context: Context, on: Boolean): Boolean {
        val camera = context.getSystemService(CameraManager::class.java) ?: return false
        val id = torchCamera(context) ?: return false
        return runCatching { camera.setTorchMode(id, on) }.isSuccess
    }

    /**
     * Rotation automatique : changée directement avec l'autorisation « Modifier les paramètres système »,
     * sinon la page qui la donne s'ouvre. Rend le nouvel état, ou null si rien n'a changé.
     */
    fun toggleRotation(context: Context, current: Boolean): Boolean? {
        if (!Settings.System.canWrite(context)) {
            open(context, Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")))
            return null
        }
        val ok = runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (current) 0 else 1)
        }.getOrDefault(false)
        return if (ok) !current else null
    }

    /**
     * Ouvre une notification depuis le lanceur (au premier plan) : sur Android 14+, l'envoi doit dire
     * qu'il autorise l'appli à passer devant.
     */
    fun send(context: Context, intent: PendingIntent): Boolean = try {
        if (Build.VERSION.SDK_INT >= 34) {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            intent.send(context, 0, null, null, null, null, options.toBundle())
        } else {
            intent.send()
        }
        true
    } catch (e: Exception) {
        false
    }
}
