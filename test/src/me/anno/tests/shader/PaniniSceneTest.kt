package me.anno.tests.shader

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.visual.render.effects.PaniniProjectionSettings
import me.anno.tests.engine.material.createMetallicScene

fun main() {
    OfficialExtensions.initForTests()
    val scene = createMetallicScene()
        .add(PaniniProjectionSettings())
    testSceneWithUI("Panini", scene, RenderMode.PANINI)
}