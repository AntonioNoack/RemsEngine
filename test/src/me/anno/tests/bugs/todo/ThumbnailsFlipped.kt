package me.anno.tests.bugs.todo

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSFileExplorer
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.pictures

fun main() {
    // to do bug: images are flipped sometimes... why???
    //  now it has become hard to reproduce... maybe we fixed it???
    //  -> can no longer reproduce it ...
    // fixed: generating image thumbnails still is very heavy...
    //    it should be 100% async and not cause any lag at all
    //    -> we scaled and saved them on the gfx thread, which is obviously quite heavy
    // disableRenderDoc()
    OfficialExtensions.initForTests()
    testUI3("Thumbnails Flipped", ECSFileExplorer(pictures.getChild("Test"), style))
}