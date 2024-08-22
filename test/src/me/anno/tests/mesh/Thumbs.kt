package me.anno.tests.mesh

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSFileExplorer
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.res

fun main() {
    OfficialExtensions.initForTests()
    testUI3("Thumbs", ECSFileExplorer(res.getChild("icon.obj"), style))
}