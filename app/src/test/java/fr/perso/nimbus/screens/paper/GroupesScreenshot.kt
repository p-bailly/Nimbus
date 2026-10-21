package fr.perso.nimbus.screens.paper

import fr.perso.nimbus.ui.paper.layouts.GroupesPage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class GroupesScreenshot {
    @Test
    fun page() = PaperFixture.shoot("h1_groupes") { GroupesPage() }

    /** Aux couleurs des applis (réglage par défaut) : pastilles cerclées de la famille, motif à la couleur de l'appli. */
    @Test
    fun ownColors() = PaperFixture.shoot("h1_groupes_couleurs", ownColors = true) { GroupesPage() }
}
