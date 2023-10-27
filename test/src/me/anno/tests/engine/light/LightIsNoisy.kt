package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.PlaneModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // todo light can be really noisy (screen-space)... why??
    // todo instanced rendering for lights still has the perspective-driver-bug
    val light = PointLight()
    light.color.set(1000f)
    light.shadowMapCascades = 1
    val plane = PlaneModel.createPlane(2, 2)
    val scene = Entity("Scene")
    scene.add(Entity(MeshComponent(plane)))
    scene.add(
        Entity(light)
            .setPosition(0.0, 0.03, 0.0)
            .setScale(1.0, 8.0, 1.0)
    )
    testSceneWithUI("Noisy Light", scene)
}