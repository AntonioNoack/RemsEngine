package me.anno.tests.engine

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.text.MeshTextComponent
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.ecs.components.text.TextAlignmentY
import me.anno.ecs.components.text.TextComponent
import me.anno.ecs.components.text.TextureTextComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes.showDebugLine
import me.anno.engine.ui.control.DraggingControlSettings
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.fonts.Font
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.black
import org.joml.Vector3d

/**
 * Shows different ways to draw text in 3d
 * */
fun main() {

    OfficialExtensions.initForTests()

    val scene = Entity("Scene")
    fun add(component: TextComponent, alignY: TextAlignmentY, px: Double, py: Double, pz: Double = 0.0) {
        Entity("${component.text}-$alignY", scene)
            .setPosition(px, py, pz)
            .add(component as Component)
    }

    val font = Font("Verdana", 50f)

    for (alignY in TextAlignmentY.entries) {
        for (alignX in AxisAlignment.entries) {
            if (alignX == AxisAlignment.FILL) continue

            val text = alignX.name
            val px = alignX.getOffset(10, 0).toDouble()
            val py = when (alignY) {
                TextAlignmentY.MIN -> -1.0
                TextAlignmentY.CENTER -> 0.0
                TextAlignmentY.BASELINE -> 1.0
                TextAlignmentY.MAX -> 2.0
            } * 0.5

            add(MeshTextComponent(text, font, alignX, alignY), alignY, px, py, 0.2)
            add(SDFTextComponent(text, font, alignX, alignY), alignY, px, py, 0.1)
            add(TextureTextComponent(text, font, alignX, alignY), alignY, px, py, 0.0)
        }
    }

    for (z in listOf(4.0, 2.0, 0.0)) {
        for (x in listOf(0.0, 5.0, 10.0)) {
            showDebugLine(
                DebugLine(
                    Vector3d(x, -5.0, z), Vector3d(x, 5.0, z),
                    0x77ff77 or black, 1e3f
                )
            )
        }
    }

    testSceneWithUI("Text in 3d", scene) {
        (it.editControls.settings as DraggingControlSettings).renderMode = RenderMode.SHOW_AABB
    }
}