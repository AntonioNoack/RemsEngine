package me.anno.tests.ui.multi

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.ECSFileExplorer
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.music

fun main() {
    // make this work somehow, by properly shutting down everything, and then somehow cancelling it and restarting
    // todo test sound -> is broken in the second window :/
    // todo background somehow is missing???
    disableRenderDoc()
    for (name in listOf("First", "Second", "Third", "Fourth", "Fifth")) {
        testUI3("$name Window", ECSFileExplorer(music, true, style))
    }
}