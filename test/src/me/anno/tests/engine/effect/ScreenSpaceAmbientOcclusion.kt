package me.anno.tests.engine.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents

fun main() {
    testSceneWithUI("SSAO", documents.getChild("Assets School Classroom.blend")) {
        it.renderer.renderMode = RenderMode.SSAO
    }
}