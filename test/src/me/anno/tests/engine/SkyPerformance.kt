package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // why is taking SSAO soo long for nothing to do??? (0.2ms vs 0.06ms for SSR)
    //  because it has much more samples
    val scene = Entity()
    scene.add(MeshComponent(flatCube))
    val sky = Skybox()
    sky.cirrus = 0f
    sky.cumulus = 0f
    scene.add(sky)
    testSceneWithUI("SkyPerf", scene)
}