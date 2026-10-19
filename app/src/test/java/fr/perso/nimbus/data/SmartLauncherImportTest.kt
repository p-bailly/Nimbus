package fr.perso.nimbus.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SmartLauncherImportTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun parseComponent_expandsRelativeClassNames() {
        assertEquals(
            "com.google.android.dialer/com.google.android.dialer.extensions.GoogleDialtactsActivity",
            SmartLauncherImport.parseComponent(
                "#Intent;action=android.intent.action.MAIN;component=com.google.android.dialer/.extensions.GoogleDialtactsActivity;end",
            ),
        )
        assertEquals(
            "com.reddit.frontpage/launcher.default",
            SmartLauncherImport.parseComponent("#Intent;launchFlags=0x10000000;component=com.reddit.frontpage/launcher.default;end"),
        )
        assertEquals(null, SmartLauncherImport.parseComponent("#Intent;action=android.intent.action.MAIN;end"))
    }

    @Test
    fun mapCategory_coversSmartLauncherDefaults() {
        assertEquals(AppCategory.COMMUNICATION, SmartLauncherImport.mapCategory("phone"))
        assertEquals(AppCategory.INTERNET, SmartLauncherImport.mapCategory("internet"))
        assertEquals(AppCategory.GAMES, SmartLauncherImport.mapCategory("games"))
        assertEquals(AppCategory.MEDIA, SmartLauncherImport.mapCategory("media"))
        assertEquals(AppCategory.TOOLS, SmartLauncherImport.mapCategory("utility"))
        assertEquals(AppCategory.SYSTEM, SmartLauncherImport.mapCategory("settings"))
        assertEquals(null, SmartLauncherImport.mapCategory("ma_categorie_perso"))
    }

    @Test
    fun syntheticBackup_isConvertedToNimbus() {
        val bytes = buildBackup()
        assertTrue(SmartLauncherImport.looksLikeBackup(bytes))
        val backup = SmartLauncherImport.read(context, bytes)
        assertNotNull(backup)
        backup!!

        val installed = listOf(
            "com.whatsapp/com.whatsapp.Main#0",
            // Activité renommée depuis la sauvegarde : retrouvée par le paquet.
            "com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity#0",
            "com.google.android.apps.weather/com.google.android.apps.weather.home.HomeActivity#0",
            "com.android.settings/com.android.settings.Settings#0",
        )
        val plan = SmartLauncherImport.plan(backup, installed)

        assertEquals(6, plan.homeColumns)
        assertEquals(12, plan.homeRows)
        assertEquals(false, plan.homeShowLabels)
        assertEquals(4, plan.drawerColumns)
        assertEquals(IconShape.SQUIRCLE, plan.iconShape)
        assertFalse(plan.dockVisible)

        val home = plan.homeIcons.associateBy { it.key }
        // Doublon : la première icône dans l'ordre de lecture (ligne 0) est gardée.
        assertEquals(HomeIcon("com.whatsapp/com.whatsapp.Main#0", 0, 0), home["com.whatsapp/com.whatsapp.Main#0"])
        assertNotNull(home["com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity#0"])
        // Icône en double et app absente.
        assertEquals(1, plan.duplicates)
        assertTrue("com.absente" in plan.unresolved)

        assertEquals(AppCategory.COMMUNICATION, plan.categoryOverrides["com.whatsapp/com.whatsapp.Main#0"])
        assertEquals(AppCategory.SYSTEM, plan.categoryOverrides["com.android.settings/com.android.settings.Settings#0"])
        assertEquals(setOf("com.google.android.apps.weather/com.google.android.apps.weather.home.HomeActivity#0"), plan.hidden)
        assertEquals(44, plan.usage["com.whatsapp/com.whatsapp.Main#0"])
        assertEquals("Ouaf", plan.customLabels["com.whatsapp/com.whatsapp.Main#0"])
        assertTrue(plan.customIcons.containsKey("com.whatsapp/com.whatsapp.Main#0"))
        assertEquals(listOf("com.google.android.apps.photos/com.google.android.apps.photos.widget.WidgetProvider"), plan.widgets.map { it.provider })

        val settings = plan.applyTo(LauncherSettings())
        assertEquals(6, settings.homeColumns)
        assertFalse(settings.showClock)
        assertFalse(settings.showSearchBar)
    }

    /** Vérification sur une vraie sauvegarde : NIMBUS_SLBK=/chemin/Backup.slbk ./gradlew testDebugUnitTest */
    @Test
    fun realBackup_ifProvided() {
        val path = System.getenv("NIMBUS_SLBK")
        assumeTrue(path != null && File(path).isFile)
        val backup = SmartLauncherImport.read(context, File(path!!).readBytes())
        assertNotNull(backup)
        backup!!
        // On simule un téléphone qui a exactement les apps du tiroir Smart Launcher.
        val installed = backup.drawer.filter { it.active }.map { "${it.packageName}/${it.activityName}#${it.userId}" }
        val plan = SmartLauncherImport.plan(backup, installed)
        println("Grille ${plan.homeColumns}×${plan.homeRows}, libellés accueil=${plan.homeShowLabels}, dock=${plan.dockVisible}")
        println("Forme=${plan.iconShape}, pack=${plan.iconPack}, thème=${plan.themedIcons}, colonnes tiroir=${plan.drawerColumns}")
        println("Accueil : ${plan.homeIcons.size} icônes, ${plan.duplicates} doublons, absentes=${plan.unresolved}")
        plan.homeIcons.sortedWith(compareBy({ it.y }, { it.x })).forEach { println("  (${it.x},${it.y}) ${it.key}") }
        println("Catégories : ${plan.categoryOverrides.values.groupingBy { it }.eachCount()}")
        println("Masquées : ${plan.hidden}")
        println("Usage (top 5) : ${plan.usage.entries.sortedByDescending { it.value }.take(5)}")
        println("Icônes perso : ${plan.customIcons.mapValues { it.value.size }}")
        println("Widgets : ${plan.widgets}")
        assertTrue(plan.homeIcons.isNotEmpty())
        assertTrue(plan.categoryOverrides.isNotEmpty())
    }

    // ---------------------------------------------------------------- Sauvegarde factice

    private fun buildBackup(): ByteArray {
        val dbFile = File(context.cacheDir, "test-flower.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("CREATE TABLE Launchable (id INTEGER PRIMARY KEY, iconGroupId INTEGER, idParentFolderLaunchable INTEGER, position INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE Widget (id INTEGER PRIMARY KEY, type INTEGER NOT NULL, appWidgetId INTEGER, parentId INTEGER, positionIndex INTEGER, provider TEXT, userId INTEGER NOT NULL)")
            db.execSQL(
                "CREATE TABLE HomeItem (id INTEGER PRIMARY KEY, idLaunchable INTEGER, idWidget INTEGER, screen INTEGER NOT NULL, " +
                    "configId INTEGER NOT NULL, cellX REAL NOT NULL, cellY REAL NOT NULL, spanX REAL NOT NULL, spanY REAL NOT NULL, zIndex INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE Action (idLaunchable INTEGER NOT NULL, actionId INTEGER NOT NULL, type INTEGER NOT NULL, intentUri TEXT, " +
                    "userId INTEGER NOT NULL, label TEXT, customLabel INTEGER NOT NULL, deepShortcutId TEXT, originalIcon INTEGER NOT NULL, " +
                    "color INTEGER, folderColor INTEGER, customIconProps TEXT, PRIMARY KEY(idLaunchable, actionId))",
            )
            db.execSQL(
                "CREATE TABLE DrawerItem (id INTEGER PRIMARY KEY, parentId INTEGER, label TEXT NOT NULL, labelNormalized TEXT NOT NULL, " +
                    "pegiLevel INTEGER, categoryId TEXT NOT NULL, active INTEGER NOT NULL, hidden INTEGER NOT NULL, timestampInstallation INTEGER NOT NULL, " +
                    "counterDrawerPanel INTEGER NOT NULL, counterSearchPanel INTEGER NOT NULL, position INTEGER NOT NULL, flags INTEGER NOT NULL, " +
                    "packageName TEXT, activityName TEXT, userId INTEGER, intent TEXT, deepShortcutId TEXT, dominantColor INTEGER, folderColor INTEGER, " +
                    "customIconProps TEXT, showBadge INTEGER NOT NULL)",
            )
            fun app(id: Int, x: Int, y: Int, component: String, label: String, customLabel: Boolean = false) {
                db.execSQL("INSERT INTO Launchable VALUES ($id, NULL, NULL, -1)")
                db.execSQL("INSERT INTO HomeItem VALUES (${id + 100}, $id, NULL, 0, 0, $x.0, $y.0, 1.0, 1.0, 1)")
                db.execSQL(
                    "INSERT INTO Action VALUES ($id, 1, 7, '#Intent;action=android.intent.action.MAIN;component=$component;end', 0, " +
                        "'$label', ${if (customLabel) 1 else 0}, NULL, 0, NULL, NULL, NULL)",
                )
            }
            app(1, 3, 11, "com.whatsapp/.Main", "Ouaf", customLabel = true)
            app(2, 5, 11, "com.google.android.apps.messaging/.main.MainActivity", "Messages")
            app(3, 0, 0, "com.whatsapp/.Main", "WhatsApp bis")
            app(4, 1, 0, "com.absente/.Main", "Absente")
            db.execSQL("INSERT INTO Widget VALUES (7, 1, 9, NULL, NULL, 'com.google.android.apps.photos/com.google.android.apps.photos.widget.WidgetProvider', 0)")
            db.execSQL("INSERT INTO HomeItem VALUES (46, NULL, 7, 1, 0, 0.0, 0.0, 6.0, 4.0, 0)")
            fun drawer(id: Int, pkg: String, cls: String, cat: String, hidden: Boolean, launches: Int) = db.execSQL(
                "INSERT INTO DrawerItem VALUES ($id, NULL, 'x', 'x', NULL, '$cat', 1, ${if (hidden) 1 else 0}, 0, $launches, 0, $id, 4, " +
                    "'$pkg', '$cls', 0, NULL, NULL, NULL, NULL, NULL, 0)",
            )
            drawer(1, "com.whatsapp", "com.whatsapp.Main", "phone", false, 44)
            drawer(2, "com.google.android.apps.weather", "com.google.android.apps.weather.home.HomeActivity", "utility", true, 1)
            drawer(3, "com.android.settings", "com.android.settings.Settings", "settings", false, 16)
        }
        val prefs = """
            <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
            <map>
                <boolean name="enableCategory" value="true" />
                <string name="launcher_profile_#0">{&quot;id&quot;:0,&quot;gridUserSettings&quot;:{&quot;columns&quot;:6,&quot;rows&quot;:12,&quot;hasLabel&quot;:false,&quot;dockSettings&quot;:{&quot;show&quot;:false}},&quot;drawerSettings&quot;:{&quot;portraitColumns&quot;:4,&quot;hasLabel&quot;:true}}</string>
                <string name="homeIconAppearanceKey">prAd:true;path:M0 512 C0 286 2 192 85 99Z;icPk:;thMd:false;</string>
                <string name="last_position">0;0;fused;0</string>
            </map>
        """.trimIndent()
        // Petite image PNG valide (1 × 1) pour l'icône personnalisée de l'élément 1.
        val png = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888).let { bmp ->
            ByteArrayOutputStream().also { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        }
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, data: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
            put("shared_prefs/ginlemon.flowerfree.xml", prefs.toByteArray())
            put("databases/ginlemon.flower.db", dbFile.readBytes())
            put("files/icons/home/custom/ic_1_1.png", png)
            put("files/metadata/info", """{"slDataVersion":63}""".toByteArray())
        }
        dbFile.delete()
        return out.toByteArray()
    }
}
