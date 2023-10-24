package me.anno.tests.engine.text

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.ui.base.Font
import me.anno.ui.base.constraints.AxisAlignment

/**
 * Discusses different ways to draw text in 3d
 *
 * TextTextureComponent is much cheaper to calculate than SDFTextureComponent, but also a bit lower quality.
 * TextMeshComponent has the highest quality, and has medium effort to calculate. The downside is triangles, which may become expensive,
 * if there is tons of text.
 * */
fun main() {

    val scene = Entity("Scene")
    fun place(component: Component, pos: Double) {
        Entity(scene)
            .setPosition(0.0, pos, 0.0)
            .add(component)
    }

    val font = Font("Verdana", 40f)
    place(TextTextureComponent("Texture Text g", font, AxisAlignment.MIN), 0.0)
    place(SDFTextureComponent("SDF Text g", font, AxisAlignment.MAX), 0.0)
    place(TextMeshComponent("Mesh Text g", font, AxisAlignment.CENTER), -2.0)

    testSceneWithUI("Text in 3d", scene)
}