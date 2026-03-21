package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * dim and bright objects
 * RenderMode with auto-exposure
 * */
fun main() {
    val scene = Entity("Scene")
    Entity("Dark Cube", scene)
        .add(MeshComponent(flatCube, DefaultAssets.steelMaterial))
        .setPosition(-1.3, 0.0, 0.0)
    Entity("Bright Cube", scene)
        .add(MeshComponent(flatCube, DefaultAssets.emissiveMaterial))
        .setPosition(1.3, 0.0, 0.0)

    testSceneWithUI("AutoExposureTest", scene)
}