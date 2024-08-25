package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.ECSFileExplorer
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.documents

fun main() {
    // todo drawing scrollbars is broken in ECSFileExplorer
    //  - size window down vertically
    //  - scroll favourites
    //  -> scrollbar is drawn multiple times
    // RemsEngine().run()
    testUI3("ScrollbarBroken", ECSFileExplorer(documents, style))
}