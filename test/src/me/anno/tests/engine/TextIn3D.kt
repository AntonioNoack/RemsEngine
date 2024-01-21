package me.anno.tests.engine

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.ecs.components.text.TextMeshComponent
import me.anno.ecs.components.text.TextTextureComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment

/**
 * Shows different ways to draw text in 3d
 * */
fun main() {

    val scene = Entity("Scene")
    fun place(component: Component, pos: Double) {
        Entity(scene)
            .setPosition(0.0, pos, 0.0)
            .add(component)
    }

    val font = Font("Verdana", 40f)
    place(TextTextureComponent("Texture Text g", font, AxisAlignment.MIN, -1), 0.0)
    place(SDFTextComponent("SDF Text g", font, AxisAlignment.MAX), 0.0)
    place(TextMeshComponent("Mesh Text g", font, AxisAlignment.CENTER, -1), -2.0)

    testSceneWithUI("Text in 3d", scene)
}