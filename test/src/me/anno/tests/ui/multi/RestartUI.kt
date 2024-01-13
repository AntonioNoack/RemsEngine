package me.anno.tests.ui.multi

import me.anno.config.DefaultConfig.style
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    // make this work somehow, by properly shutting down everything, and then somehow cancelling it and restarting
    // todo test sound
    // todo bug: secondary windows have black background, why ever!!!!
    // todo we also get a weird message, because we have to recreate the OpenGL context...
    for (name in listOf("First", "Second", "Third", "Fourth", "Fifth")) {
        testUI("$name Window", TextPanel("$name Panel", style))
    }
}