package fr.perso.nimbus.data

import androidx.annotation.DrawableRes
import fr.perso.nimbus.R
import java.text.Normalizer

/**
 * Logos dessinés au crayon pour la table des matières (sources : design/logos-crayon/, un SVG par
 * logo, tous à la même taille de dessin ; PNG de 192 px sur fond transparent dans drawable-nodpi).
 * Reconnus d'après le paquet de l'app, sinon d'après son nom tel qu'il s'affiche.
 */
object CrayonLogos {

    private val byPackage: Map<String, Int> = mapOf(
        "com.google.android.gm" to R.drawable.crayon_gmail,
        "com.reddit.frontpage" to R.drawable.crayon_reddit,
        "com.whatsapp" to R.drawable.crayon_whatsapp,
        "com.whatsapp.w4b" to R.drawable.crayon_whatsapp,
        "com.discord" to R.drawable.crayon_discord,
        "com.twitter.android" to R.drawable.crayon_x,
        "com.google.android.dialer" to R.drawable.crayon_telephone,
        "com.google.android.apps.messaging" to R.drawable.crayon_messages,
        "com.google.android.contacts" to R.drawable.crayon_contacts,
        "com.android.chrome" to R.drawable.crayon_chrome,
        "com.android.vending" to R.drawable.crayon_play,
        "com.amazon.mShop.android.shopping" to R.drawable.crayon_amazon,
        "fr.banquepopulaire.cyberplus" to R.drawable.crayon_banquepop,
        "com.google.android.apps.maps" to R.drawable.crayon_maps,
        "com.lidl.eci.lidlplus" to R.drawable.crayon_lidl,
        "com.waze" to R.drawable.crayon_waze,
        "com.google.android.googlequicksearchbox" to R.drawable.crayon_google,
        "com.paypal.android.p2pmobile" to R.drawable.crayon_paypal,
        "com.google.android.apps.walletnfcrel" to R.drawable.crayon_wallet,
        "app.revanced.android.youtube" to R.drawable.crayon_youtube,
        "app.rvx.android.youtube" to R.drawable.crayon_youtube,
        "com.google.android.youtube" to R.drawable.crayon_youtube,
        "tv.twitch.android.app" to R.drawable.crayon_twitch,
        "com.google.android.apps.photos" to R.drawable.crayon_photos,
        "com.spotify.music" to R.drawable.crayon_spotify,
        "com.stremio.one" to R.drawable.crayon_stremio,
        "org.lichess.mobileV2" to R.drawable.crayon_lichess,
        "org.lichess.mobileapp" to R.drawable.crayon_lichess,
        "com.anthropic.claude" to R.drawable.crayon_claude,
        "io.github.pbailly.meteo" to R.drawable.crayon_meteo,
        "com.github.android" to R.drawable.crayon_github,
        "com.google.android.GoogleCamera" to R.drawable.crayon_camera,
        "com.google.android.apps.docs" to R.drawable.crayon_drive,
        "com.google.android.calendar" to R.drawable.crayon_agenda,
        "com.google.android.calculator" to R.drawable.crayon_calculatrice,
        "com.google.android.deskclock" to R.drawable.crayon_horloge,
        "com.google.android.keep" to R.drawable.crayon_keep,
        "fr.perso.managingapps" to R.drawable.crayon_mesapplis,
        "com.android.settings" to R.drawable.crayon_parametres,
        "com.google.android.apps.nbu.files" to R.drawable.crayon_files,
        "app.revanced.manager.flutter" to R.drawable.crayon_revanced,
        "com.google.android.apps.authenticator2" to R.drawable.crayon_authenticator,
        "com.IndexEducation.Pronote" to R.drawable.crayon_pronote,
        "xyz.getpapillon.app" to R.drawable.crayon_pronote,
    )

    /** Noms affichés (minuscules, sans accents) : les applis sans paquet connu, et les variantes. */
    private val byLabel: Map<String, Int> = mapOf(
        "gmail" to R.drawable.crayon_gmail,
        "reddit" to R.drawable.crayon_reddit,
        "whatsapp" to R.drawable.crayon_whatsapp,
        "discord" to R.drawable.crayon_discord,
        "x" to R.drawable.crayon_x,
        "telephone" to R.drawable.crayon_telephone,
        "messages" to R.drawable.crayon_messages,
        "contacts" to R.drawable.crayon_contacts,
        "chrome" to R.drawable.crayon_chrome,
        "play store" to R.drawable.crayon_play,
        "boutique amazon" to R.drawable.crayon_amazon,
        "amazon" to R.drawable.crayon_amazon,
        "banque populaire" to R.drawable.crayon_banquepop,
        "maps" to R.drawable.crayon_maps,
        "lidl plus" to R.drawable.crayon_lidl,
        "waze" to R.drawable.crayon_waze,
        "google" to R.drawable.crayon_google,
        "paypal" to R.drawable.crayon_paypal,
        "red & moi" to R.drawable.crayon_redmoi,
        "cartes ign" to R.drawable.crayon_ign,
        "wallet" to R.drawable.crayon_wallet,
        "youtube" to R.drawable.crayon_youtube,
        "twitch" to R.drawable.crayon_twitch,
        "photos" to R.drawable.crayon_photos,
        "spotify" to R.drawable.crayon_spotify,
        "stremio" to R.drawable.crayon_stremio,
        "basket live" to R.drawable.crayon_basket,
        "lichess" to R.drawable.crayon_lichess,
        "bg stats" to R.drawable.crayon_bgstats,
        "claude" to R.drawable.crayon_claude,
        "meteo" to R.drawable.crayon_meteo,
        "github" to R.drawable.crayon_github,
        "appareil photo" to R.drawable.crayon_camera,
        "drive" to R.drawable.crayon_drive,
        "agenda" to R.drawable.crayon_agenda,
        "blablacar" to R.drawable.crayon_blablacar,
        "france identite" to R.drawable.crayon_franceid,
        "calculatrice" to R.drawable.crayon_calculatrice,
        "horloge" to R.drawable.crayon_horloge,
        "notes keep" to R.drawable.crayon_keep,
        "keep" to R.drawable.crayon_keep,
        "sncf connect" to R.drawable.crayon_sncf,
        "mes applis" to R.drawable.crayon_mesapplis,
        "parametres" to R.drawable.crayon_parametres,
        "files" to R.drawable.crayon_files,
        "revanced manager" to R.drawable.crayon_revanced,
        "authenticator" to R.drawable.crayon_authenticator,
        "parametres de microg" to R.drawable.crayon_microg,
        "gestionnaire de mots de passe" to R.drawable.crayon_motsdepasse,
        "pronote" to R.drawable.crayon_pronote,
        "prof maths" to R.drawable.crayon_profmaths,
        "prof de maths" to R.drawable.crayon_profmaths,
        "scan" to R.drawable.crayon_scan,
    )

    /** Logo au crayon de [app], ou null s'il n'en a pas. */
    @DrawableRes
    fun of(app: AppEntry): Int? {
        // Le nom d'abord : « Paramètres de microG » partage le paquet des services Google, et une appli
        // renommée garde le logo de son nom.
        byLabel[normalize(app.label)]?.let { return it }
        byPackage[app.packageName]?.let { return it }
        val pkg = app.packageName
        return when {
            pkg.startsWith("com.comuto") -> R.drawable.crayon_blablacar
            pkg.startsWith("com.vsct") || pkg.startsWith("fr.sncf") -> R.drawable.crayon_sncf
            pkg.startsWith("fr.ign") -> R.drawable.crayon_ign
            "franceidentite" in pkg -> R.drawable.crayon_franceid
            else -> null
        }
    }

    private val marks = Regex("\\p{Mn}+")

    private fun normalize(text: String): String =
        marks.replace(Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD), "")
}
