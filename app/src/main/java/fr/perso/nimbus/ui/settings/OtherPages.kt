package fr.perso.nimbus.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.perso.nimbus.R
import fr.perso.nimbus.data.BadgeStyle
import fr.perso.nimbus.data.Gesture
import fr.perso.nimbus.data.GestureAction
import fr.perso.nimbus.data.claudeTargetName
import fr.perso.nimbus.dictation.ClaudeDictation
import fr.perso.nimbus.service.SystemActions
import fr.perso.nimbus.ui.SettingsSection
import fr.perso.nimbus.ui.theme.LocalPaperUi
import fr.perso.nimbus.ui.theme.PaperColors
import fr.perso.nimbus.ui.theme.PaperParagraph
import fr.perso.nimbus.ui.theme.PaperStyle
import fr.perso.nimbus.ui.theme.runIn
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ------------------------------------------------------------------------------------------ gestes

private fun gestureIcon(gesture: Gesture): Int = when (gesture) {
    Gesture.DOUBLE_TAP -> R.drawable.ms_touch_app
    Gesture.SWIPE_UP -> R.drawable.ms_swipe_up
    Gesture.SWIPE_DOWN -> R.drawable.ms_swipe_down
    Gesture.TWO_FINGER_UP, Gesture.TWO_FINGER_DOWN -> R.drawable.ms_swipe_vertical
    Gesture.PINCH_IN -> R.drawable.ms_pinch
    Gesture.PINCH_OUT -> R.drawable.ms_open_in_full
}

private fun gestureTone(gesture: Gesture): Color = when (gesture) {
    Gesture.DOUBLE_TAP -> SettingsColors.Red
    Gesture.SWIPE_UP -> SettingsColors.Orange
    Gesture.SWIPE_DOWN -> SettingsColors.Blue
    Gesture.TWO_FINGER_UP -> SettingsColors.Teal
    Gesture.TWO_FINGER_DOWN -> SettingsColors.Purple
    Gesture.PINCH_IN -> SettingsColors.Green
    Gesture.PINCH_OUT -> SettingsColors.Cyan
}

/** Gestes et raccourcis : l'action de chaque geste de l'accueil, puis l'état du service d'accessibilité. */
@Composable
internal fun SettingsCtx.GesturesPage() {
    Page(SettingsSection.GESTURES) {
        settingsSection("Sur l'accueil")
        for (gesture in Gesture.entries) {
            val id = SettingId.of(gesture)
            setting(id) {
                val action = settings.actionFor(gesture)
                val subtitle = if (action == GestureAction.APP) {
                    "Ouvrir " + (gestureApps[gesture]?.let { apps.byKey[it]?.label } ?: "… (aucune app choisie)")
                } else {
                    action.label
                }
                SettingsEntry(gestureIcon(gesture), gestureTone(gesture), gesture.label, subtitle, chevron = true, id = id) {
                    showDialog(SettingsDialog.GestureChoice(gesture))
                }
            }
        }
        settingsSection("Service d'accessibilité", SettingsColors.Gray)
        setting(SettingId.GESTURE_SERVICE) {
            SettingsEntry(
                R.drawable.ms_accessibility_new, SettingsColors.Gray, SettingId.GESTURE_SERVICE.title,
                if (status.gestureService) "Activé" else "Désactivé : verrouillage, capture d'écran, applis récentes…",
                chevron = true, id = SettingId.GESTURE_SERVICE,
            ) { SystemActions.openAccessibilitySettings(context) }
        }
    }
}

// ------------------------------------------------------------------------------------------ écran verrouillé

/** Ce que montre la page de déverrouillage, dans l'ordre : le contenu de `UnlockFeed`. */
private val lockBlocks = listOf(
    "Météo" to "de l'appli Météo",
    "Messages" to "3 au plus",
    "Musique en cours" to "si la lecture est active",
    "Matchs des favoris" to "NBA et LoL",
    "Figure du jour" to "une nouvelle à chaque réveil",
)

/** Maquette de la page de déverrouillage : l'heure, la date, les cinq blocs de contenu, puis « Glisser vers le haut ». */
@Composable
private fun LockPagePreview() {
    val now = remember { LocalDateTime.now() }
    val paper = LocalPaperUi.current
    val accent = LocalSettingsAccent.current
    val time = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE))
    val frame = if (paper) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(28.dp)
    val ink = if (paper) PaperColors.Ink else MaterialTheme.colorScheme.onSurface
    val soft = if (paper) PaperColors.Soft else MaterialTheme.colorScheme.onSurfaceVariant
    SettingsCard {
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    if (paper) Modifier.background(accent.copy(alpha = 0.05f)).border(0.8.dp, accentDark(), frame)
                    else Modifier.background(
                        Brush.verticalGradient(
                            listOf(lerp(MaterialTheme.colorScheme.surface, accent, 0.42f), lerp(MaterialTheme.colorScheme.surfaceVariant, accent, 0.1f)),
                        ),
                    ),
                )
                .padding(22.dp),
        ) {
            Text(
                time,
                style = if (paper) PaperStyle.serif(46f, 50f) else MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp, fontWeight = FontWeight.Medium),
                color = ink,
                textAlign = if (paper) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                date,
                style = if (paper) PaperStyle.smallCaps(14f, soft) else MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = soft,
                textAlign = if (paper) TextAlign.Center else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
            )
            for ((title, hint) in lockBlocks) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .then(if (paper) Modifier else Modifier.background(ink.copy(alpha = 0.08f), RoundedCornerShape(12.dp)))
                        .heightIn(min = 34.dp)
                        .padding(horizontal = if (paper) 0.dp else 12.dp),
                ) {
                    Text(title, style = if (paper) PaperStyle.item else MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = ink)
                    Text(hint, style = if (paper) PaperStyle.note else MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = accentText())
                }
            }
            Text(
                "Glisser vers le haut pour déverrouiller",
                style = if (paper) PaperStyle.note else MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                color = accentText(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }
    }
}

/** Écran verrouillé : la page de déverrouillage et le fond d'écran animé qui la reprend sous l'écran du système. */
@Composable
internal fun SettingsCtx.LockscreenPage() {
    Page(SettingsSection.LOCKSCREEN) {
        item(key = "lock:preview") { LockPagePreview() }

        settingsSection("Page de déverrouillage")
        setting(SettingId.UNLOCK_PAGE) {
            SettingsEntry(
                R.drawable.ms_lock, SettingsColors.Teal, SettingId.UNLOCK_PAGE.title,
                "Au réveil : météo, messages, musique, matchs et figure du jour",
                checked = settings.unlockPage,
                id = SettingId.UNLOCK_PAGE,
            ) {
                val enable = !settings.unlockPage
                update { it.copy(unlockPage = enable) }
                when {
                    !enable -> Unit
                    !status.gestureService -> SystemActions.openAccessibilitySettings(context)
                    !status.lockScreenAllowed -> SystemActions.openXiaomiPermissions(context)
                }
            }
        }
        item(key = "unlock:note") {
            SettingsNote(
                "S'affiche au réveil, par-dessus l'écran verrouillé du système" +
                    if (status.gestureService) "." else " ; nécessite le service « Gestes Nimbus ».",
            )
        }
        if (settings.unlockPage && !status.gestureService) {
            item(key = "banner:gestures") {
                SettingsBanner(R.drawable.ms_accessibility_new, "Activer le service « Gestes Nimbus »") {
                    SystemActions.openAccessibilitySettings(context)
                }
            }
        }
        if (settings.unlockPage && !status.lockScreenAllowed) {
            item(key = "banner:xiaomi") {
                SettingsBanner(
                    R.drawable.ms_lock,
                    "Xiaomi : autoriser « Afficher sur l'écran de verrouillage » et « Ouvrir de nouvelles fenêtres en arrière-plan »",
                    mark = if (!status.gestureService) 2 else 1,
                    rule = status.gestureService,
                ) { SystemActions.openXiaomiPermissions(context) }
            }
        }

        settingsSection("Fond de l'écran verrouillé")
        setting(SettingId.UNLOCK_WALLPAPER) {
            SettingsEntry(
                R.drawable.ms_wallpaper, SettingsColors.Cyan, SettingId.UNLOCK_WALLPAPER.title,
                "La même page en fond, sous l'heure du téléphone",
                value = if (status.unlockWallpaper) "En place" else "Désactivé",
                chevron = true,
                id = SettingId.UNLOCK_WALLPAPER,
            ) { SystemActions.openUnlockWallpaper(context) }
        }
        item(key = "wallpaper:note") {
            SettingsNote(
                "Seule, l'empreinte et le visage déverrouillent directement ; avec la page de déverrouillage, c'est elle qu'on voit " +
                    "sous l'écran du système au moment de déverrouiller. Choisir « Écran de verrouillage », sans la grande horloge du Pixel.",
            )
        }
    }
}

// ------------------------------------------------------------------------------------------ confidentialité

/** Confidentialité : apps masquées derrière un code PIN, profil professionnel, espace privé. */
@Composable
internal fun SettingsCtx.PrivacyPage() {
    Page(SettingsSection.PRIVACY) {
        settingsSection("Apps masquées")
        setting(SettingId.HIDDEN_APPS) {
            val count = apps.hidden.size
            SettingsEntry(
                R.drawable.ms_visibility_off, SettingsColors.Teal, SettingId.HIDDEN_APPS.title,
                (if (count == 0) "Aucune" else "$count masquée${if (count > 1) "s" else ""}") + " · protégé par code PIN",
                chevron = true,
                id = SettingId.HIDDEN_APPS,
            ) { showDialog(if (status.hasPin) SettingsDialog.PinUnlock else SettingsDialog.PinCreate) }
        }
        if (status.hasPin) {
            setting(SettingId.PIN) {
                SettingsEntry(R.drawable.ms_password, SettingsColors.Orange, SettingId.PIN.title, chevron = true, id = SettingId.PIN) {
                    showDialog(SettingsDialog.PinChangeOld)
                }
            }
        }
        apps.workProfile?.let { work ->
            settingsSection("Profil professionnel")
            setting(SettingId.WORK_PROFILE) {
                SettingsEntry(
                    R.drawable.ms_work, SettingsColors.Blue, SettingId.WORK_PROFILE.title,
                    if (work.quiet) "En pause" else "Actives",
                    checked = !work.quiet,
                    id = SettingId.WORK_PROFILE,
                ) { vm.setProfileQuiet(work, !work.quiet) }
            }
        }
        apps.privateSpace?.let { private ->
            settingsSection("Espace privé", SettingsColors.Purple)
            setting(SettingId.PRIVATE_SPACE) {
                SettingsEntry(
                    if (private.quiet) R.drawable.ms_lock else R.drawable.ms_lock_open, SettingsColors.Purple, SettingId.PRIVATE_SPACE.title,
                    if (private.quiet) "Verrouillé · toucher pour déverrouiller" else "Déverrouillé · toucher pour verrouiller",
                    id = SettingId.PRIVATE_SPACE,
                ) { vm.setProfileQuiet(private, !private.quiet) }
            }
            setting(SettingId.HIDE_PRIVATE) {
                SettingsEntry(
                    R.drawable.ms_visibility_off, SettingsColors.Purple, SettingId.HIDE_PRIVATE.title,
                    "Accessible en tapant « espace privé » dans la recherche" +
                        if (private.hiddenWhenLocked) " (déjà masqué par le système)" else "",
                    enabled = !private.hiddenWhenLocked,
                    checked = settings.hidePrivateSpaceWhenLocked || private.hiddenWhenLocked,
                    id = SettingId.HIDE_PRIVATE,
                ) { update { it.copy(hidePrivateSpaceWhenLocked = !it.hidePrivateSpaceWhenLocked) } }
            }
            setting(SettingId.PRIVATE_SETTINGS) {
                SettingsEntry(R.drawable.ms_settings, SettingsColors.Gray, SettingId.PRIVATE_SETTINGS.title, chevron = true, id = SettingId.PRIVATE_SETTINGS, onClick = vm::openPrivateSpaceSettings)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ dictée

/** Dictée pour Claude : la conversation de destination, l'envoi, puis le lancement (bulle, tuile, geste). */
@Composable
internal fun SettingsCtx.DictationPage() {
    val paper = LocalPaperUi.current
    Page(SettingsSection.DICTATION) {
        item(key = "dictation:intro") {
            SettingsNote(
                "Parlez depuis n'importe quelle appli : Nimbus transcrit, ouvre la conversation Claude " +
                    "(par exemple une session Claude Code), y écrit le texte et l'envoie. Avec le service activé, " +
                    "la dictée s'ouvre dans un panneau flottant : vous pouvez changer d'appli pendant que vous parlez.",
            )
        }
        if (!status.dictationService) {
            item(key = "banner:dictation") {
                SettingsBanner(R.drawable.ms_accessibility_new, "Activer le service « Dictée Claude Nimbus »") {
                    SystemActions.openAccessibilitySettings(context)
                }
            }
        }
        setting(SettingId.DICTATION_TRY) {
            SettingsEntry(R.drawable.ms_mic, SettingsColors.Purple, SettingId.DICTATION_TRY.title, "Ouvre l'écran de dictée", id = SettingId.DICTATION_TRY) {
                ClaudeDictation.start(context)
            }
        }

        settingsSection("Conversations", SettingsColors.Blue)
        item(key = "dictation:target") {
            SettingsNote("La destination se choisit dans le panneau de dictée (actuelle : ${settings.claudeTargetName}).")
        }
        settings.claudeSessions.forEachIndexed { index, session ->
            item(key = "claude-session:$index") {
                SettingsEntry(R.drawable.ms_public, if (index % 2 == 0) SettingsColors.Blue else SettingsColors.Cyan, session.name, session.url, chevron = true) {
                    showDialog(SettingsDialog.EditClaudeSession(index))
                }
            }
        }
        setting(SettingId.DICTATION_ADD) {
            SettingsEntry(R.drawable.ms_add_box, SettingsColors.Green, SettingId.DICTATION_ADD.title, "Nom et lien de la session", id = SettingId.DICTATION_ADD) {
                showDialog(SettingsDialog.EditClaudeSession(null))
            }
        }

        settingsSection("Envoi", SettingsColors.Teal)
        setting(SettingId.DICTATION_COPY_ONLY) {
            SettingsEntry(
                R.drawable.ms_content_copy, SettingsColors.Orange, SettingId.DICTATION_COPY_ONLY.title,
                "Ne rien envoyer à Claude : le texte est copié, à coller où vous voulez",
                checked = settings.dictationCopyOnly, id = SettingId.DICTATION_COPY_ONLY,
            ) { update { it.copy(dictationCopyOnly = !it.dictationCopyOnly) } }
        }
        setting(SettingId.DICTATION_CONFIRM) {
            SettingsEntry(
                R.drawable.ms_info, SettingsColors.Teal, SettingId.DICTATION_CONFIRM.title, "Relire et corriger la transcription",
                checked = settings.dictationConfirm, id = SettingId.DICTATION_CONFIRM,
            ) { update { it.copy(dictationConfirm = !it.dictationConfirm) } }
        }
        setting(SettingId.DICTATION_RETURN) {
            SettingsEntry(
                R.drawable.ms_autorenew, SettingsColors.Cyan, SettingId.DICTATION_RETURN.title, "Après l'envoi",
                checked = settings.dictationReturn, id = SettingId.DICTATION_RETURN,
            ) { update { it.copy(dictationReturn = !it.dictationReturn) } }
        }

        settingsSection("Lancement", SettingsColors.Red)
        setting(SettingId.DICTATION_BUBBLE) {
            SettingsEntry(
                R.drawable.ms_touch_app, SettingsColors.Red, SettingId.DICTATION_BUBBLE.title,
                if (status.dictationService) "Bouton micro par-dessus les applis ; glissez-le pour le déplacer"
                else "Nécessite le service « Dictée Claude Nimbus »",
                checked = settings.dictationBubble, id = SettingId.DICTATION_BUBBLE,
            ) {
                val enable = !settings.dictationBubble
                update { it.copy(dictationBubble = enable) }
                if (enable && !status.dictationService) SystemActions.openAccessibilitySettings(context)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setting(SettingId.DICTATION_TILE) {
                SettingsEntry(
                    R.drawable.ms_add_box, SettingsColors.Orange, SettingId.DICTATION_TILE.title,
                    "Ajouter « Dictée Claude » aux réglages rapides", id = SettingId.DICTATION_TILE,
                ) { SystemActions.requestDictationTile(context) }
            }
        }
        item(key = "dictation:tip") {
            val text = "Un geste de l'accueil peut aussi lancer la dictée (Gestes › « Dictée pour Claude »)."
            if (paper) PaperParagraph(runIn("Astuce.", text)) else SettingsNote("Astuce : " + text.replaceFirstChar { it.lowercase() })
        }
    }
}

// ------------------------------------------------------------------------------------------ notifications

/** Deux pastilles d'exemple sur des icônes : le style choisi, en direct. */
@Composable
private fun BadgePreview(style: BadgeStyle) {
    val paper = LocalPaperUi.current
    val badge = if (paper) accentDark() else Color(0xFFE5484D)
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        for ((icon, tone, count) in listOf(Triple(R.drawable.ms_contacts, SettingsColors.Blue, "3"), Triple(R.drawable.ms_notifications, SettingsColors.Green, "12"))) {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .then(
                            if (paper) Modifier.background(tone.copy(alpha = 0.09f), CircleShape).border(1.2.dp, tone, CircleShape)
                            else Modifier.background(lerp(tone, Color.Black, 0.12f), RoundedCornerShape(16.dp)),
                        ),
                ) {
                    Icon(painterResource(icon), contentDescription = null, tint = if (paper) tone else Color.White, modifier = Modifier.size(26.dp))
                }
                if (style == BadgeStyle.COUNT) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 7.dp, y = (-7).dp)
                            .heightIn(min = 20.dp)
                            .background(badge, CircleShape)
                            .padding(horizontal = 6.dp),
                    ) {
                        Text(count, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 3.dp, y = (-3).dp)
                            .size(13.dp)
                            .background(badge, CircleShape),
                    )
                }
            }
        }
    }
}

/** Notifications : les pastilles sur les icônes (style en direct) et le bandeau papier. */
@Composable
internal fun SettingsCtx.NotificationsPage() {
    val paper = LocalPaperUi.current
    Page(SettingsSection.NOTIFICATIONS) {
        settingsSection("Pastilles")
        if (settings.badgesEnabled && !status.notificationAccess) {
            item(key = "banner:notifications") {
                SettingsBanner(R.drawable.ms_app_badging, "Autoriser « Pastilles de notification Nimbus »") {
                    SystemActions.openNotificationAccessSettings(context)
                }
            }
        }
        setting(SettingId.BADGES) {
            SettingsEntry(
                R.drawable.ms_app_badging, SettingsColors.Cyan, SettingId.BADGES.title, "Sur les icônes des apps qui ont des notifications",
                checked = settings.badgesEnabled, id = SettingId.BADGES,
            ) {
                val enable = !settings.badgesEnabled
                update { it.copy(badgesEnabled = enable) }
                if (enable && !status.notificationAccess) SystemActions.openNotificationAccessSettings(context)
            }
        }
        setting(SettingId.BADGE_STYLE) {
            SettingsControl(SettingId.BADGE_STYLE.title, settings.badgeStyle.label, SettingId.BADGE_STYLE) {
                BadgePreview(settings.badgeStyle)
                SettingsSegmented(
                    BadgeStyle.entries, settings.badgeStyle,
                    { if (it == BadgeStyle.COUNT) "Nombre" else "Point" },
                    { style -> update { it.copy(badgeStyle = style) } },
                )
            }
        }
        if (paper) {
            settingsSection("Bandeau papier", SettingsColors.Orange)
            setting(SettingId.PAPER_PANEL) {
                SettingsEntry(
                    R.drawable.ms_swipe_down, SettingsColors.Orange, SettingId.PAPER_PANEL.title,
                    "Glisser vers le bas sur l'accueil : réglages rapides et notifications en page d'article",
                    checked = settings.paperPanel, id = SettingId.PAPER_PANEL,
                ) {
                    val enable = !settings.paperPanel
                    update { it.copy(paperPanel = enable) }
                    if (enable && !status.notificationAccess) SystemActions.openNotificationAccessSettings(context)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ sauvegarde

/** Sauvegarde : export et import d'une configuration, sauvegarde automatique quotidienne dans un dossier. */
@Composable
internal fun SettingsCtx.BackupPage() {
    Page(SettingsSection.BACKUP) {
        settingsSection("Fichier")
        setting(SettingId.BACKUP_EXPORT) {
            SettingsEntry(
                R.drawable.ms_upload_file, SettingsColors.Cyan, SettingId.BACKUP_EXPORT.title,
                "Réglages, accueil, dock, catégories, noms, icônes, gestes (.json)", id = SettingId.BACKUP_EXPORT,
            ) { host.exportBackup() }
        }
        setting(SettingId.BACKUP_IMPORT) {
            SettingsEntry(
                R.drawable.ms_file_open, SettingsColors.Blue, SettingId.BACKUP_IMPORT.title,
                "Sauvegarde Nimbus (.json) ou Smart Launcher (.slbk)", id = SettingId.BACKUP_IMPORT,
            ) { showDialog(SettingsDialog.ConfirmImport) }
        }

        settingsSection("Automatique", SettingsColors.Green)
        setting(SettingId.BACKUP_AUTO) {
            val folder = backupFolder
            val subtitle = if (folder == null) {
                "Désactivée · choisir un dossier"
            } else {
                val last = vm.lastAutoBackup
                "Chaque jour dans « ${folderName(folder)} » (7 dernières gardées)" +
                    if (last > 0) " · dernière : ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(last))}" else ""
            }
            SettingsEntry(R.drawable.ms_autorenew, SettingsColors.Green, SettingId.BACKUP_AUTO.title, subtitle, chevron = true, id = SettingId.BACKUP_AUTO) {
                host.chooseAutoBackupFolder()
            }
        }
        if (backupFolder != null) {
            setting(SettingId.BACKUP_AUTO_OFF) {
                SettingsEntry(R.drawable.ms_close, SettingsColors.Gray, SettingId.BACKUP_AUTO_OFF.title, id = SettingId.BACKUP_AUTO_OFF) {
                    vm.setAutoBackupFolder(null)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------ à propos

/** À propos : la version, l'accueil par défaut et les crédits des icônes. */
@Composable
internal fun SettingsCtx.AboutPage() {
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        } ?: "?"
    }
    Page(SettingsSection.ABOUT) {
        item(key = "about:version") {
            SettingsEntry(R.drawable.ms_info, SettingsColors.Blue, "Nimbus $version", "Launcher personnel") {}
        }
        setting(SettingId.DEFAULT_LAUNCHER) {
            SettingsEntry(
                R.drawable.ms_home, SettingsColors.Green, SettingId.DEFAULT_LAUNCHER.title,
                if (status.isDefaultLauncher) "Nimbus est l'application d'accueil" else "Toucher pour définir Nimbus",
                chevron = !status.isDefaultLauncher,
                id = SettingId.DEFAULT_LAUNCHER,
            ) { host.requestDefaultLauncher() }
        }
        setting(SettingId.ABOUT_ICONS) {
            SettingsEntry(R.drawable.ms_star, SettingsColors.Orange, SettingId.ABOUT_ICONS.title, "Material Symbols, Google (licence Apache 2.0)", id = SettingId.ABOUT_ICONS) {}
        }
    }
}
