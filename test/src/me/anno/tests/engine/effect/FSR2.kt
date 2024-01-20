package me.anno.tests.engine.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc
import me.anno.io.files.Reference.getReference

fun main() {
    // at least it renders again...
    // todo this looks very broken at the moment, why???
    RenderDoc.forceLoadRenderDoc()
    testSceneWithUI("FSR2", getReference("res://icon.obj")) {
        it.renderer.renderMode = RenderMode.FSR2_X2
    }
}