package fr.perso.nimbus.data

import java.text.Normalizer

/**
 * Motifs et couleurs par défaut de quelques applis, d'après leur paquet ou leur nom : celles dont la
 * silhouette ne convient pas (icône pleine, mauvais calque à thème), les applis Google en couleur, et
 * les couleurs qui ne se devinent pas de l'icône. Le choix de l'utilisateur (Plus › Motif) passe devant.
 */
object AppBrands {

    /** Bleu France de l'État (Marianne). */
    private const val BLEU_FRANCE = 0xFF000091

    /** « IGN » comme mot (Cartes IGN, IGNrando'), pas dans « Signal ». */
    private val ignWord = Regex("\\bign")

    private val revanced = setOf("app.revanced.android.youtube", "app.rvx.android.youtube")

    fun glyph(app: AppEntry): GlyphChoice? {
        val pkg = app.packageName
        val label = normalize(app.label)
        return when {
            pkg in revanced -> GlyphChoice.Logo(PaperLogo.YOUTUBE)
            pkg.startsWith("fr.banquepopulaire") || "banque populaire" in label -> GlyphChoice.Logo(PaperLogo.PLUS_X)
            "prof de maths" in label -> GlyphChoice.Logo(PaperLogo.PI_R)
            pkg == "fr.perso.managingapps" || label == "mes applis" -> GlyphChoice.Logo(PaperLogo.MES_APPLIS)
            pkg.startsWith("fr.ign") || ignWord.containsMatchIn(label) -> GlyphChoice.Logo(PaperLogo.IGN)
            pkg.startsWith("com.comuto") || "blablacar" in label -> GlyphChoice.Detoured
            pkg.startsWith("com.vsct") || pkg.startsWith("fr.sncf") || "sncf" in label -> GlyphChoice.Logo(PaperLogo.SNCF)
            isGoogle(pkg) -> GlyphChoice.Colored
            else -> null
        }
    }

    /** Couleur imposée dans le sommaire (ARGB), ou null : celle de l'icône. */
    fun color(app: AppEntry): Long? {
        val label = normalize(app.label)
        return when {
            "france identite" in label || "franceidentite" in app.packageName -> BLEU_FRANCE
            else -> null
        }
    }

    /** Applis Google, Chrome et le Play Store compris (paquets « com.android. » hérités). */
    private fun isGoogle(pkg: String) =
        pkg.startsWith("com.google.") || pkg == "com.android.vending" || pkg == "com.android.chrome" || pkg.startsWith("com.chrome.")

    private val marks = Regex("\\p{Mn}+")

    private fun normalize(text: String): String =
        marks.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")
}
