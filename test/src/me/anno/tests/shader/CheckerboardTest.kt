package me.anno.tests.shader

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.WindowManagement
import me.anno.utils.OS.documents

fun main() {
    val scene = documents.getChild("RemsEngine/StuckUnderground/Rooms/Room3x4.json")
    // todo bug: f11 is not working inside input fields, and the scene view, why? scene view is especially important
    testSceneWithUI("Checkerboard", scene, RenderMode.CHECKERBOARD)
}