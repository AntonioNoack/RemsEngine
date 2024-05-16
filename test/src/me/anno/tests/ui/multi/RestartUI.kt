package me.anno.tests.ui.multi

import me.anno.config.DefaultConfig.style
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI

fun main() {
    // make this work somehow, by properly shutting down everything, and then somehow cancelling it and restarting
    // todo test sound
    // todo it currently crashes sometimes, and at the very end...
    for (name in listOf("First", "Second", "Third", "Fourth", "Fifth")) {
        testUI("$name Window", TextPanel("$name Panel", style))
    }
}