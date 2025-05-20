package me.anno.tests.gfx

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    testSceneWithUI("FSR1", downloads.getChild("3d/DamagedHelmet.glb"), RenderMode.FSR_X4)
}