package me.anno.tests.shader

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase

fun main() {
    // sky was missing motion vectors
    testSceneWithUI("Motion Vectors", flatCube.front) {
        StudioBase.instance!!.enableVSync = true
        it.renderer.renderMode = RenderMode.MOTION_VECTORS
    }
}