package me.anno.tests.bugs.done

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

// engine would allocate cubemap textures constantly without realising
fun main() {
    OfficialExtensions.initForTests()
    testSceneWithUI("OOM?!?", downloads.getChild("3d/TheJunkShop/The Junk Shop.blend"))
}