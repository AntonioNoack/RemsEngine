package me.anno.tests.ui.input

import me.anno.engine.Events.addEvent
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing

fun main() {
    disableRenderDoc()
    addEvent { showFPS = false }
    testDrawing("Show in focus") {
        it.showIsInFocus()
    }
}