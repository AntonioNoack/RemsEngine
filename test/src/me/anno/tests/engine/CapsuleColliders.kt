package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val scene = Entity()
    for (i in 0 until 3) {
        val collider = CapsuleCollider()
        collider.axis = i
        scene.add(
            Entity("Capsule $i", collider)
                .setPosition((i - 1) * 2.0, 0.0, 0.0)
        )
    }
    testSceneWithUI("Capsule Colliders", scene)
}