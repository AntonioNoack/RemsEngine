package me.anno.tests.sdf

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.SDFRegistry
import me.anno.sdf.shapes.SDFHeightMap
import me.anno.utils.OS

fun main() {
    // todo define a sample with bricks (SDFArray2)
    testSceneWithUI("SDFHeightMap", SDFHeightMap().apply {
        maxSteps = 50
        source = OS.pictures.getChild("Maps/Bricks.png")
        maxHeight = 0.1f
    }) {
        it.renderer.renderMode = SDFRegistry.NumStepsRenderMode
    }
}