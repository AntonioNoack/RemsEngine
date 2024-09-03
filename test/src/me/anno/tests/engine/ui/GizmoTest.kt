package me.anno.tests.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

/**
 * test, that Gizmos are usable
 * */
fun main() {
    val scene = Entity("Scene")
        .add(MeshComponent(DefaultAssets.flatCube))
    Entity("Cube", scene)
        .add(MeshComponent(DefaultAssets.flatCube))
        .setPosition(2.5, 0.0, 0.0)
    testSceneWithUI("Gizmos", scene)
}