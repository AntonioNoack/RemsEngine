package me.anno.tests.engine.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube

fun main() {
    // sky was missing motion vectors
    testSceneWithUI("Motion Vectors", flatCube.front) {
        it.renderer.renderMode = RenderMode.MOTION_VECTORS
    }
}