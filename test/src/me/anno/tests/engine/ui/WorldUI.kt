package me.anno.tests.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.ui.CanvasComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.ui.Window
import me.anno.ui.base.buttons.TextButton

fun main() {
    // todo ui probably should react, if it is camera-space (only scaled properly in non-editor-mode)
    // create UI in 3d
    ECSRegistry.init()
    val scene = Entity()
    scene.add(CanvasComponent().apply {
        val ui = TextButton("Test Button", style)
        windowStack.add(Window(ui, false, windowStack))
        width = 120
        height = 40
    })
    testSceneWithUI("UI in 3d", scene)
}