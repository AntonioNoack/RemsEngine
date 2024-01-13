package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.ui.Panel
import me.anno.ui.debug.TestStudio.Companion.testUI

fun main() {
    GFX.someWindow.addProgressBar("Test", "", 1.0)
    testUI("Progressbar before UI", Panel(style))
}