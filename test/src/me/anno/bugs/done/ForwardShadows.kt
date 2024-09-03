package me.anno.bugs.done

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.DefaultAssets
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.types.Floats.toRadians

fun main() {
    // fixed: shadow was missing
    // fixed: the sphere is wayyy to glossy for roughness=1
    // fixed: stuff too bright
    // todo can we get forward and deferred to exact parity?
    DefaultAssets.init()
    val scene = Entity("Scene")
    Entity("Floor", scene)
        .add(MeshComponent(plane))
    Entity("Sphere", scene)
        .setPosition(0.0, 0.5, 0.0)
        .setScale(0.5)
        .add(MeshComponent(IcosahedronModel.createIcosphere(2)))
    Entity("Sun", scene)
        .setRotation((-30.0).toRadians(), 0.0, 0.0)
        .add(DirectionalLight().apply { shadowMapCascades = 1 })
    testSceneWithUI("Forward Shadows", scene) {
        it.renderView.renderMode = RenderMode.FORWARD
    }
}