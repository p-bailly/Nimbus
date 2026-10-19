package fr.perso.nimbus.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Xml
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

// ---------------------------------------------------------------- Contenu brut d'une sauvegarde

/** Ce que Nimbus sait lire d'une sauvegarde Smart Launcher (.slbk). */
data class SlBackup(
    /** shared_prefs/ginlemon.flower*.xml */
    val prefs: Map<String, Any>,
    val drawer: List<SlDrawerItem>,
    val home: List<SlHomeItem>,
    val widgets: List<SlWidget>,
    /** Icônes personnalisées de l'accueil : identifiant du « Launchable » -> image. */
    val customIcons: Map<Long, ByteArray>,
)

data class SlDrawerItem(
    val packageName: String,
    val activityName: String,
    val userId: Int,
    val categoryId: String,
    val hidden: Boolean,
    val active: Boolean,
    val launches: Int,
)

data class SlHomeItem(
    val launchableId: Long,
    /** null pour une icône rangée dans un dossier. */
    val screen: Int?,
    val x: Int,
    val y: Int,
    /** « paquet/classe », null si l'élément n'est pas une app (dossier, raccourci…). */
    val component: String?,
    val userId: Int,
    val label: String?,
    val customLabel: Boolean,
)

data class SlWidget(val provider: String, val userId: Int, val screen: Int, val spanY: Int)

// ---------------------------------------------------------------- Conversion vers Nimbus

/** Widget à rajouter après un import : les identifiants de widgets ne se transfèrent pas. */
data class PendingWidget(
    /** « paquet/classe » du fournisseur. */
    val provider: String,
    val heightDp: Int? = null,
    /** Hauteur en fraction de l'écran (import Smart Launcher). */
    val heightFraction: Float? = null,
)

/** Résultat de la conversion d'une sauvegarde Smart Launcher en données Nimbus. */
data class SlImportPlan(
    val homeColumns: Int,
    val homeRows: Int,
    val homeShowLabels: Boolean?,
    val drawerColumns: Int?,
    val drawerShowLabels: Boolean?,
    val autoCategories: Boolean?,
    val iconShape: IconShape?,
    val iconPack: String?,
    val themedIcons: Boolean?,
    /** Smart Launcher n'affichait pas de dock : les favoris Nimbus sont vidés. */
    val dockVisible: Boolean,
    val homeIcons: List<HomeIcon>,
    val categoryOverrides: Map<String, AppCategory>,
    val hidden: Set<String>,
    val usage: Map<String, Int>,
    val customLabels: Map<String, String>,
    /** Clé d'app -> image de l'icône personnalisée. */
    val customIcons: Map<String, ByteArray>,
    val widgets: List<PendingWidget>,
    /** Apps de la sauvegarde absentes de ce téléphone. */
    val unresolved: Set<String>,
    /** Icônes en double sur l'accueil Smart Launcher (Nimbus n'en garde qu'une par app). */
    val duplicates: Int = 0,
) {
    /**
     * Réglages repris de Smart Launcher. L'accueil Smart Launcher n'a ni horloge, ni batterie,
     * ni barre de recherche : on les masque pour retrouver la même grille.
     */
    fun applyTo(s: LauncherSettings): LauncherSettings = s.copy(
        homeColumns = homeColumns,
        homeRows = homeRows,
        homeShowLabels = homeShowLabels ?: s.homeShowLabels,
        drawerColumns = drawerColumns ?: s.drawerColumns,
        showLabels = drawerShowLabels ?: s.showLabels,
        autoCategories = autoCategories ?: s.autoCategories,
        iconShape = iconShape ?: s.iconShape,
        iconPack = iconPack ?: s.iconPack,
        themedIcons = themedIcons ?: s.themedIcons,
        showClock = false,
        showBattery = false,
        showSearchBar = false,
    )
}

object SmartLauncherImport {

    private const val MAX_ENTRY_BYTES = 32 * 1024 * 1024
    private val CUSTOM_ICON = Regex("""files/icons/home/custom/ic_(\d+)_1\.(png|webp|jpg)""")
    private val COMPONENT = Regex("""component=([^;]+)""")

    /** Une sauvegarde Smart Launcher est un zip (signature « PK »). */
    fun looksLikeBackup(bytes: ByteArray): Boolean =
        bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    // ------------------------------------------------------------ Lecture du zip

    /** Lit la sauvegarde ; null si ce n'est pas une sauvegarde Smart Launcher lisible. */
    fun read(context: Context, bytes: ByteArray): SlBackup? {
        var prefsXml: ByteArray? = null
        var db: ByteArray? = null
        var wal: ByteArray? = null
        val icons = HashMap<Long, ByteArray>()

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val name = entry.name
                    val iconMatch = CUSTOM_ICON.matchEntire(name)
                    when {
                        name.startsWith("shared_prefs/ginlemon.flower") && name.endsWith(".xml") ->
                            prefsXml = readLimited(zip) ?: return null
                        name == "databases/ginlemon.flower.db" -> db = readLimited(zip) ?: return null
                        name == "databases/ginlemon.flower.db-wal" -> wal = readLimited(zip) ?: return null
                        iconMatch != null ->
                            icons[iconMatch.groupValues[1].toLong()] = readLimited(zip) ?: return null
                    }
                }
            }
        } catch (e: Exception) {
            return null
        }

        val dbBytes = db ?: return null
        val prefs = prefsXml?.let { parsePrefs(it) } ?: emptyMap()

        // SQLite ne lit qu'un fichier : copie temporaire dans le cache, supprimée ensuite.
        val dir = File(context.cacheDir, "smart_launcher_import")
        dir.deleteRecursively()
        dir.mkdirs()
        return try {
            val dbFile = File(dir, "flower.db")
            dbFile.writeBytes(dbBytes)
            wal?.let { File(dir, "flower.db-wal").writeBytes(it) }
            SQLiteDatabase.openDatabase(
                dbFile.path, null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
            ).use { sql ->
                SlBackup(prefs, readDrawer(sql), readHome(sql), readWidgets(sql), icons)
            }
        } catch (e: Exception) {
            null
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun readLimited(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > MAX_ENTRY_BYTES) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    /** Fichier SharedPreferences Android : <string>, <int>, <long>, <boolean>, <float>. */
    private fun parsePrefs(xml: ByteArray): Map<String, Any> {
        val result = HashMap<String, Any>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(xml), "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val key = parser.getAttributeValue(null, "name")
                    if (key != null) {
                        when (parser.name) {
                            "string" -> result[key] = parser.nextText()
                            "boolean" -> result[key] = parser.getAttributeValue(null, "value") == "true"
                            "int" -> parser.getAttributeValue(null, "value")?.toIntOrNull()?.let { result[key] = it }
                            "long" -> parser.getAttributeValue(null, "value")?.toLongOrNull()?.let { result[key] = it }
                            "float" -> parser.getAttributeValue(null, "value")?.toFloatOrNull()?.let { result[key] = it }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            // préférences illisibles : on garde ce qui a été lu
        }
        return result
    }

    private inline fun <T> SQLiteDatabase.rows(query: String, map: (Cursor) -> T?): List<T> = try {
        rawQuery(query, null).use { c ->
            val list = ArrayList<T>()
            while (c.moveToNext()) map(c)?.let { list.add(it) }
            list
        }
    } catch (e: Exception) {
        // table absente ou schéma d'une autre version de Smart Launcher
        emptyList()
    }

    private fun Cursor.str(column: String): String? {
        val i = getColumnIndex(column)
        return if (i < 0 || isNull(i)) null else getString(i)
    }

    private fun Cursor.int(column: String): Int {
        val i = getColumnIndex(column)
        return if (i < 0 || isNull(i)) 0 else getInt(i)
    }

    private fun Cursor.double(column: String): Double {
        val i = getColumnIndex(column)
        return if (i < 0 || isNull(i)) 0.0 else getDouble(i)
    }

    private fun readDrawer(sql: SQLiteDatabase): List<SlDrawerItem> = sql.rows(
        "SELECT packageName, activityName, userId, categoryId, hidden, active, " +
            "counterDrawerPanel, counterSearchPanel FROM DrawerItem",
    ) { c ->
        SlDrawerItem(
            packageName = c.str("packageName") ?: return@rows null,
            activityName = c.str("activityName") ?: return@rows null,
            userId = c.int("userId"),
            categoryId = c.str("categoryId").orEmpty(),
            hidden = c.int("hidden") != 0,
            active = c.int("active") != 0,
            launches = c.int("counterDrawerPanel") + c.int("counterSearchPanel"),
        )
    }

    /** Icônes de l'accueil (action principale), y compris celles rangées dans un dossier. */
    private fun readHome(sql: SQLiteDatabase): List<SlHomeItem> = sql.rows(
        "SELECT a.idLaunchable AS id, h.screen AS screen, h.cellX AS cellX, h.cellY AS cellY, " +
            "a.intentUri AS intentUri, a.userId AS userId, a.label AS label, a.customLabel AS customLabel, " +
            "a.deepShortcutId AS deepShortcutId, l.idParentFolderLaunchable AS parent " +
            "FROM Action a JOIN Launchable l ON l.id = a.idLaunchable " +
            "LEFT JOIN HomeItem h ON h.idLaunchable = a.idLaunchable " +
            "WHERE a.actionId = 1",
    ) { c ->
        val onHome = c.str("screen") != null
        val inFolder = c.str("parent") != null
        if (!onHome && !inFolder) return@rows null
        // Les raccourcis d'app (deep shortcuts) ne sont pas des apps : ignorés.
        val component = if (c.str("deepShortcutId") == null) parseComponent(c.str("intentUri")) else null
        SlHomeItem(
            launchableId = c.int("id").toLong(),
            screen = if (onHome) c.int("screen") else null,
            x = c.double("cellX").toInt(),
            y = c.double("cellY").toInt(),
            component = component,
            userId = c.int("userId"),
            label = c.str("label"),
            customLabel = c.int("customLabel") != 0,
        )
    }

    private fun readWidgets(sql: SQLiteDatabase): List<SlWidget> = sql.rows(
        "SELECT w.provider AS provider, w.userId AS userId, h.screen AS screen, h.spanY AS spanY " +
            "FROM Widget w JOIN HomeItem h ON h.idWidget = w.id WHERE w.provider IS NOT NULL",
    ) { c ->
        SlWidget(
            provider = c.str("provider") ?: return@rows null,
            userId = c.int("userId"),
            screen = c.int("screen"),
            spanY = c.double("spanY").toInt().coerceAtLeast(1),
        )
    }

    /** « #Intent;…;component=com.pkg/.Main;end » -> « com.pkg/com.pkg.Main ». */
    fun parseComponent(intentUri: String?): String? {
        val raw = intentUri?.let { COMPONENT.find(it)?.groupValues?.get(1) } ?: return null
        val slash = raw.indexOf('/')
        if (slash <= 0 || slash == raw.length - 1) return null
        val pkg = raw.substring(0, slash)
        val cls = raw.substring(slash + 1)
        return "$pkg/" + if (cls.startsWith(".")) pkg + cls else cls
    }

    // ------------------------------------------------------------ Conversion

    /**
     * Convertit la sauvegarde pour ce téléphone. [availableKeys] : clés des apps installées
     * (« paquet/classe#profil »). Une app dont l'activité a changé est retrouvée par son paquet.
     */
    fun plan(backup: SlBackup, availableKeys: Collection<String>): SlImportPlan {
        val keys = availableKeys.toSet()
        val byPackage = availableKeys.groupBy { it.substringBefore('/') + "#" + it.substringAfterLast('#') }
        val unresolved = LinkedHashSet<String>()

        fun resolve(pkg: String, cls: String, userId: Int): String? {
            val exact = "$pkg/$cls#$userId"
            if (exact in keys) return exact
            val byPkg = byPackage["$pkg#$userId"]?.firstOrNull()
            if (byPkg == null) unresolved.add(pkg)
            return byPkg
        }

        fun resolveComponent(component: String, userId: Int): String? {
            val slash = component.indexOf('/')
            return resolve(component.substring(0, slash), component.substring(slash + 1), userId)
        }

        // Grille et tiroir : « launcher_profile_#0 » (JSON).
        val profile = (backup.prefs["launcher_profile_#0"] as? String)?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }
        val grid = profile?.optJSONObject("gridUserSettings")
        val drawerSettings = profile?.optJSONObject("drawerSettings")
        val columns = (grid?.optInt("columns", 5) ?: 5).coerceIn(3, 8)
        val rows = (grid?.optInt("rows", 6) ?: 6).coerceIn(3, 14)
        val dockVisible = grid?.optJSONObject("dockSettings")?.optBoolean("show", true) ?: true

        // Accueil : page 1 à sa place, puis le reste (autres pages, dossiers, collisions) dans les cases libres.
        val homeIcons = LinkedHashMap<String, HomeIcon>()
        var duplicates = 0
        val taken = HashSet<Pair<Int, Int>>()
        val leftovers = ArrayList<String>()
        val customLabels = LinkedHashMap<String, String>()
        val customIcons = LinkedHashMap<String, ByteArray>()
        val homeItems = backup.home.filter { it.component != null }
            .sortedWith(compareBy({ it.screen ?: Int.MAX_VALUE }, { it.y }, { it.x }))
        for (item in homeItems) {
            val key = resolveComponent(item.component!!, item.userId) ?: continue
            if (item.customLabel && !item.label.isNullOrBlank()) customLabels[key] = item.label.trim()
            backup.customIcons[item.launchableId]?.let { customIcons[key] = it }
            if (key in homeIcons || key in leftovers) {
                duplicates++
                continue
            }
            val cell = item.x to item.y
            if (item.screen == 0 && item.x in 0 until columns && item.y in 0 until rows && cell !in taken) {
                homeIcons[key] = HomeIcon(key, item.x, item.y)
                taken.add(cell)
            } else {
                leftovers.add(key)
            }
        }
        val free = (0 until rows).flatMap { y -> (0 until columns).map { x -> x to y } }.filter { it !in taken }.iterator()
        for (key in leftovers) {
            if (!free.hasNext()) break
            val (x, y) = free.next()
            homeIcons[key] = HomeIcon(key, x, y)
        }

        // Tiroir : catégories, apps masquées, compteurs d'utilisation.
        val overrides = LinkedHashMap<String, AppCategory>()
        val hidden = LinkedHashSet<String>()
        val usage = LinkedHashMap<String, Int>()
        for (item in backup.drawer) {
            if (!item.active) continue
            val key = resolve(item.packageName, item.activityName, item.userId) ?: continue
            mapCategory(item.categoryId)?.let { overrides[key] = it }
            if (item.hidden) hidden.add(key)
            if (item.launches > 0) usage[key] = (usage[key] ?: 0) + item.launches
        }

        // Apparence des icônes : « prAd:true;path:M0 512 C…;icPk:;thMd:false;… »
        val appearance = (backup.prefs["homeIconAppearanceKey"] as? String)
            ?.split(';')
            ?.mapNotNull { part -> part.indexOf(':').takeIf { it > 0 }?.let { part.substring(0, it) to part.substring(it + 1) } }
            ?.toMap()
            .orEmpty()

        return SlImportPlan(
            homeColumns = columns,
            homeRows = rows,
            homeShowLabels = grid?.takeIf { it.has("hasLabel") }?.optBoolean("hasLabel"),
            drawerColumns = drawerSettings?.takeIf { it.has("portraitColumns") }?.optInt("portraitColumns")?.coerceIn(3, 6),
            drawerShowLabels = drawerSettings?.takeIf { it.has("hasLabel") }?.optBoolean("hasLabel"),
            autoCategories = backup.prefs["enableCategory"] as? Boolean,
            iconShape = appearance["path"]?.let { shapeFromPath(it) },
            iconPack = appearance["icPk"]?.takeIf { it.isNotBlank() },
            themedIcons = appearance["thMd"]?.let { it == "true" },
            dockVisible = dockVisible,
            homeIcons = homeIcons.values.toList(),
            categoryOverrides = overrides,
            hidden = hidden,
            usage = usage,
            customLabels = customLabels,
            customIcons = customIcons,
            widgets = backup.widgets.map { PendingWidget(it.provider, heightFraction = it.spanY.toFloat() / rows) },
            unresolved = unresolved,
            duplicates = duplicates,
        )
    }

    /** Catégories Smart Launcher (identifiants internes) -> catégories Nimbus. */
    fun mapCategory(id: String): AppCategory? = when (id.lowercase()) {
        "phone", "communication" -> AppCategory.COMMUNICATION
        "social" -> AppCategory.SOCIAL
        "internet", "web" -> AppCategory.INTERNET
        "media", "multimedia", "music", "video", "entertainment" -> AppCategory.MEDIA
        "photo", "photography", "camera" -> AppCategory.PHOTO
        "games", "game" -> AppCategory.GAMES
        "productivity", "office", "work" -> AppCategory.PRODUCTIVITY
        "shopping", "finance" -> AppCategory.SHOPPING
        "travel", "maps", "navigation" -> AppCategory.TRAVEL
        "news", "reading", "books" -> AppCategory.NEWS
        "health", "fitness", "sport" -> AppCategory.HEALTH
        "utility", "utilities", "tools" -> AppCategory.TOOLS
        "settings", "system" -> AppCategory.SYSTEM
        else -> null
    }

    /** Forme approchée à partir du tracé SVG de Smart Launcher. */
    private fun shapeFromPath(path: String): IconShape = when {
        path.isBlank() -> IconShape.SYSTEM
        path.contains('A') || path.contains('a') -> IconShape.CIRCLE
        path.contains('C') || path.contains('c') || path.contains('Q') || path.contains('q') -> IconShape.SQUIRCLE
        else -> IconShape.SQUARE
    }
}
