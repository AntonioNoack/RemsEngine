package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    // make this work somehow, by properly shutting down everything, and then somehow cancelling it and restarting
    for (name in listOf("First", "Second", "Third", "Fourth", "Fifth")) {
        testUI("$name Window", TextPanel("$name Panel", style))
    }
}