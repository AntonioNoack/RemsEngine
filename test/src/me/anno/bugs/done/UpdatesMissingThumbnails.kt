package me.anno.bugs.done

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.ECSFileExplorer
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.pictures

fun main() {
    // bug: file explorer entries aren't really redrawing themselves well, and text panels sometimes neither...
    //  reproduce: refresh all resources (Ctrl+F5), wait
    testUI3("Thumbnail Updates Missing", ECSFileExplorer(pictures.getChild("Anime"), style))
}