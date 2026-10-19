package fr.perso.nimbus.data

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import fr.perso.nimbus.widgets.WidgetHostManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Compte rendu d'un import, affiché à l'utilisateur. */
data class ImportResult(
    val success: Boolean,
    val title: String,
    val details: List<String> = emptyList(),
    /** Widgets à rajouter (les identifiants de widgets ne survivent pas à une sauvegarde). */
    val widgets: List<PendingWidget> = emptyList(),
    /** Apps dont l'icône personnalisée a changé. */
    val iconKeys: Set<String> = emptySet(),
)

/**
 * Sauvegardes de la configuration :
 * - export / import au format Nimbus (JSON : réglages, listes, icônes personnalisées, widgets) ;
 * - import d'une sauvegarde Smart Launcher (.slbk) ;
 * - sauvegarde automatique quotidienne dans un dossier choisi (7 dernières conservées).
 */
class BackupManager(context: Context, private val prefs: Prefs) {

    private val appContext = context.applicationContext

    fun export(): ByteArray {
        val root = prefs.exportJson()
        val icons = JSONObject()
        for ((key, png) in CustomIcons.all(appContext)) icons.put(key, Base64.encodeToString(png, Base64.NO_WRAP))
        root.put("customIcons", icons)
        val widgets = JSONArray()
        val host = WidgetHostManager.get(appContext)
        for (slot in prefs.widgets.value) {
            val info = host.info(slot.id) ?: continue
            widgets.put(JSONObject().put("provider", info.provider.flattenToString()).put("heightDp", slot.heightDp))
        }
        for (w in prefs.homeWidgets.value) {
            val info = host.info(w.id) ?: continue
            widgets.put(
                JSONObject().put("provider", info.provider.flattenToString())
                    .put("home", JSONArray(listOf(w.x, w.y, w.spanX, w.spanY, w.page))),
            )
        }
        root.put("widgets", widgets)
        root.put("createdAt", System.currentTimeMillis())
        return root.toString(2).toByteArray()
    }

    /** Importe une sauvegarde Nimbus (.json) ou Smart Launcher (.slbk). [availableKeys] : apps installées. */
    fun import(bytes: ByteArray, availableKeys: Collection<String>): ImportResult {
        if (SmartLauncherImport.looksLikeBackup(bytes)) return importSmartLauncher(bytes, availableKeys)

        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
        if (root == null || !prefs.importJson(root)) {
            return ImportResult(false, "Fichier non reconnu", listOf("Ni une sauvegarde Nimbus (.json), ni une sauvegarde Smart Launcher (.slbk)."))
        }

        val iconKeys = HashSet(CustomIcons.all(appContext).keys)
        CustomIcons.clear(appContext)
        var icons = 0
        root.optJSONObject("customIcons")?.let { o ->
            for (key in o.keys()) {
                val png = try {
                    Base64.decode(o.getString(key), Base64.DEFAULT)
                } catch (e: Exception) {
                    continue
                }
                if (CustomIcons.save(appContext, key, png)) {
                    iconKeys.add(key)
                    icons++
                }
            }
        }
        val widgets = ArrayList<PendingWidget>()
        root.optJSONArray("widgets")?.let { array ->
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val provider = o.optString("provider").takeIf { it.isNotBlank() } ?: continue
                val home = o.optJSONArray("home")?.takeIf { it.length() == 4 || it.length() == 5 }?.let { a ->
                    HomeWidget(0, a.optInt(0), a.optInt(1), a.optInt(2).coerceAtLeast(1), a.optInt(3).coerceAtLeast(1), a.optInt(4).coerceAtLeast(0))
                }
                widgets.add(PendingWidget(provider, heightDp = o.optInt("heightDp").takeIf { it > 0 }, home = home))
            }
        }
        val toAdd = withoutExisting(widgets)
        val details = buildList {
            add("Réglages, accueil, favoris, catégories, apps masquées et gestes restaurés")
            if (icons > 0) add(plural(icons, "icône personnalisée", "icônes personnalisées"))
            if (toAdd.isNotEmpty()) add(plural(toAdd.size, "widget à rajouter", "widgets à rajouter"))
        }
        return ImportResult(true, "Sauvegarde Nimbus restaurée", details, toAdd, iconKeys)
    }

    private fun importSmartLauncher(bytes: ByteArray, availableKeys: Collection<String>): ImportResult {
        val backup = SmartLauncherImport.read(appContext, bytes)
            ?: return ImportResult(false, "Sauvegarde illisible", listOf("Ce fichier .slbk ne contient pas de données Smart Launcher lisibles."))
        val plan = SmartLauncherImport.plan(backup, availableKeys)

        val homeRead = plan.homeIcons != null
        prefs.updateSettings(plan::applyTo)
        prefs.replaceAll(
            favorites = if (homeRead && !plan.dockVisible) emptyList() else null,
            hidden = plan.hidden,
            categoryOverrides = plan.categoryOverrides,
            usage = plan.usage,
            customLabels = plan.customLabels.takeIf { homeRead },
            homeIcons = plan.homeIcons,
        )
        val iconKeys = plan.customIcons.filter { (key, img) -> CustomIcons.save(appContext, key, img) }.keys
        val widgets = withoutExisting(plan.widgets)

        val details = buildList {
            val home = plan.homeIcons
            if (home != null) add("${plural(home.size, "icône", "icônes")} sur l'accueil (grille ${plan.homeColumns} × ${plan.homeRows})")
            else add("Accueil illisible dans cette sauvegarde : celui de Nimbus est conservé")
            val overrides = plan.categoryOverrides
            if (overrides == null) add("Tiroir illisible dans cette sauvegarde : catégories et apps masquées conservées")
            else if (overrides.isNotEmpty()) {
                val categories = overrides.values.toSet().joinToString(", ") { it.label }
                add("${plural(overrides.size, "app classée", "apps classées")} : $categories")
            }
            plan.hidden?.takeIf { it.isNotEmpty() }?.let { add(plural(it.size, "app masquée", "apps masquées")) }
            if (iconKeys.isNotEmpty()) add(plural(iconKeys.size, "icône personnalisée", "icônes personnalisées"))
            plan.usage?.takeIf { it.isNotEmpty() }?.let { add("Fréquence d'utilisation reprise pour ${plural(it.size, "app", "apps")}") }
            if (plan.duplicates > 0) add(plural(plan.duplicates, "icône en double ignorée", "icônes en double ignorées"))
            if (plan.unresolved.isNotEmpty()) {
                val names = plan.unresolved.take(4).joinToString(", ") + if (plan.unresolved.size > 4) "…" else ""
                add("${plural(plan.unresolved.size, "app absente", "apps absentes")} de ce téléphone : $names")
            }
            if (widgets.isNotEmpty()) add(plural(widgets.size, "widget à rajouter", "widgets à rajouter"))
            add("Horloge, batterie et barre de recherche masquées comme dans Smart Launcher (réactivables dans les paramètres)")
        }
        return ImportResult(true, "Configuration Smart Launcher importée", details, widgets, iconKeys)
    }

    /** Retire les widgets déjà présents sur l'accueil (import sur le même téléphone). */
    private fun withoutExisting(widgets: List<PendingWidget>): List<PendingWidget> {
        fun normalize(provider: String) = ComponentName.unflattenFromString(provider)?.flattenToString() ?: provider
        val host = WidgetHostManager.get(appContext)
        val existing = (prefs.widgets.value.map { it.id } + prefs.homeWidgets.value.map { it.id })
            .mapNotNull { host.info(it)?.provider?.flattenToString() }
            .groupingBy { it }.eachCount().toMutableMap()
        return widgets.filter { w ->
            val key = normalize(w.provider)
            val count = existing[key] ?: 0
            if (count > 0) {
                existing[key] = count - 1
                false
            } else {
                true
            }
        }
    }

    // ---------------------------------------------------------------- Sauvegarde automatique

    /** Écrit une sauvegarde dans le dossier choisi si la dernière date de plus de 24 h. */
    fun autoBackupIfDue(now: Long = System.currentTimeMillis()): Boolean {
        val folder = prefs.autoBackupFolder.value ?: return false
        if (now - prefs.lastAutoBackup < DAY_MS) return false
        return try {
            val tree = Uri.parse(folder)
            val resolver = appContext.contentResolver
            val treeId = DocumentsContract.getTreeDocumentId(tree)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
            val name = AUTO_PREFIX + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now)) + ".json"
            val doc = DocumentsContract.createDocument(resolver, parent, "application/json", name) ?: return false
            resolver.openOutputStream(doc)?.use { it.write(export()) } ?: return false
            prefs.lastAutoBackup = now
            prune(tree, treeId)
            true
        } catch (e: Exception) {
            // dossier supprimé ou autorisation retirée
            false
        }
    }

    private fun prune(tree: Uri, treeId: String) {
        val resolver = appContext.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId)
        val files = ArrayList<Pair<String, String>>()
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (name.startsWith(AUTO_PREFIX) && name.endsWith(".json")) files.add(name to c.getString(0))
            }
        }
        files.sortedByDescending { it.first }.drop(AUTO_KEEP).forEach { (_, id) ->
            DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id))
        }
    }

    private fun plural(n: Int, one: String, many: String) = "$n ${if (n > 1) many else one}"

    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val AUTO_PREFIX = "Nimbus-auto-"
        private const val AUTO_KEEP = 7
    }
}
