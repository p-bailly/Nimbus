package fr.perso.nimbus.screens.paper

import fr.perso.nimbus.ui.paper.layouts.EnsemblesPage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h916dp-xxhdpi")
class EnsemblesScreenshot {
    @Test
    fun page() = PaperFixture.shoot("h3_ensembles") { EnsemblesPage() }
}
