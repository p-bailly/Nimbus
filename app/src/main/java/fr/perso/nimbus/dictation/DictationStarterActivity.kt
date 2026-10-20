package fr.perso.nimbus.dictation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import fr.perso.nimbus.service.DictationMicService
import fr.perso.nimbus.service.DictationService
import fr.perso.nimbus.service.SystemActions

/**
 * Activité invisible, le temps d'un instant : demande le micro si besoin et démarre le service
 * « micro » pendant que Nimbus est au premier plan, puis ouvre le panneau flottant et se ferme.
 */
class DictationStarterActivity : ComponentActivity() {

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openPanel() else {
            SystemActions.toast(this, "Autorisez le micro pour dicter")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            openPanel()
        }
    }

    private fun openPanel() {
        if (isFinishing) return
        DictationMicService.start(this)
        DictationService.instance?.showPanel()
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
