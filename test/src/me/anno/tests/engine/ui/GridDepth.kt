package me.anno.tests.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * allows to check whether the grid lines end exactly at the geometry
 * */
fun main() {
    val scene = Entity()
    // make the material dark, so the grid lines are easier to see
    val material = Material.diffuse(0x222222)
    Entity("Sphere", scene)
        .add(MeshComponent(DefaultAssets.icoSphere, material))
        .setPosition(-2.0, 0.0, 0.0)
    Entity("Cube", scene)
        .add(MeshComponent(DefaultAssets.flatCube, material))
        .setPosition(+2.0, -0.5, 0.0)
        .setRotation(-1.0, 0.0, 0.0)
    testSceneWithUI("GridDepth", scene)
}