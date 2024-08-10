package me.anno.tests.engine.effect

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.visual.render.effects.HeightExpFogNode
import me.anno.tests.utils.TestWorld
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.joml.Vector3f

@Suppress("unused")
class HeightExpFogSettings(val node: HeightExpFogNode) : Component() {

    var distance: Float
        get() = node.getFloatInput(1)
        set(value) {
            node.setInput(1, value)
        }

    var strength: Float
        get() = node.getFloatInput(2)
        set(value) {
            node.setInput(2, value)
        }

    var sharpness: Float
        get() = node.getFloatInput(3)
        set(value) {
            node.setInput(3, value)
        }

    var fogLevel: Float
        get() = node.getFloatInput(4)
        set(value) {
            node.setInput(4, value)
        }

    @Type("Color3HDR")
    var color: Vector3f
        get() = node.getInput(5) as Vector3f
        set(value) {
            node.setInput(5, value)
        }
}

fun main() {
    val mode = RenderMode.FOG_TEST
    val node = mode.renderGraph!!.nodes.firstInstance2(HeightExpFogNode::class)
    val settings = HeightExpFogSettings(node)
    val scene = Entity("Scene")
    scene.add(TestWorld().createTriangleMesh(0, 0, 0, 256, 32, 512))
    scene.add(settings)
    EditorState.select(settings) // select this for testing
    testSceneWithUI("HeightFog", scene) {
        it.renderView.renderMode = mode
    }
}