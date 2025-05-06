package me.anno.tests.gfx.forwardplus

import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.tests.engine.light.createLightTypesScene

/**
 * Forward+ rendering (very WIP)
 * */
fun main() {
    val scene = createLightTypesScene()
    testSceneWithUI("Forward+", scene) {
        (it.editControls as DraggingControls).settings.renderMode = RenderMode.FORWARD_PLUS
    }
}