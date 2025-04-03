package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.engine.DefaultAssets.flatCube
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.PropertyInspector

fun main() {
    // create a set of inputs
    // todo see whether everything can be reached using tabs
    //  - search bar is broken: accepts tab and prints space
    //  - button below is broken: jumps to item above
    // todo fix tab controls
    // todo make that test automated
    disableRenderDoc()
    val sample = flatCube
    val inspector = PropertyInspector({ sample }, style, Unit)
    testUI3("Tab Controls", inspector)
}