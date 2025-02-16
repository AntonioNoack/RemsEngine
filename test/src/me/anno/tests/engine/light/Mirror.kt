package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import me.anno.utils.OS.documents
import kotlin.math.PI

fun main() {
    // todo bug: reflection disappears on shallow angles
    OfficialExtensions.initForTests()
    val scene = Entity()
    scene.add(Entity("Mirror").apply {
        add(MeshComponent(DefaultAssets.plane, Material().apply {
            metallicMinMax.set(1f)
            roughnessMinMax.set(0.1f) // changes the used mip level
        }))
        add(
            Entity("Mirror Mesh")
                .add(PlanarReflection())
                .setPosition(0.0, -0.01, 0.0)
                .setRotation(-PIf / 2, 0f, 0f)
        )
    })
    scene.add(Entity("Monkey").apply {
        add(MeshComponent(documents.getChild("monkey.obj")))
        position = position.set(0.0, 0.3, 0.0)
        scale = scale.set(0.1)
    })
    testSceneWithUI("Planar Reflections", scene) {
        // doesn't have ssao yet
        it.renderView.renderMode = RenderMode.FORWARD
    }
}