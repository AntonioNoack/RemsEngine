package me.anno.tests.bugs.todo

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSFileExplorer
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.pictures

fun main() {
    // todo bug: images are flipped sometimes... why???
    // disableRenderDoc()
    OfficialExtensions.initForTests()
    testUI3("Thumbnails Flipped", ECSFileExplorer(pictures.getChild("Test"), style))
}