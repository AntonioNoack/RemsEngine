package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.export.ui.FileInclusionPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.documents

fun main() {
    testUI3("FileInclusionUI", FileInclusionPanel(listOf(documents), style))
}