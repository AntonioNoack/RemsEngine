package me.anno.tests.gfx.textures

import me.anno.Build
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    ECSRegistry.init()
    Build.isDebug = false
    testSceneWithUI(".blend with materials", downloads.getChild("The Junk Shop.blend"))
}
