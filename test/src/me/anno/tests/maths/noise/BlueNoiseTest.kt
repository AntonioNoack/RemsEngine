package me.anno.tests.maths.noise

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.BlueNoiseSampler2f
import me.anno.maths.noise.BlueNoiseSampler3f
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * test 2d and 3d blue noise
 * */
fun main() {
    val maxAttempts = 30
    val scene = Entity()
    val points3d = BlueNoiseSampler3f(Vector3f(1f), 0.1f, maxAttempts, 1234)
        .generatePoints()
    for (pt in points3d) {
        Entity(scene)
            .setPosition(Vector3d(pt).mul(100.0))
            .add(MeshComponent(DefaultAssets.icoSphere))
    }

    val points2d = BlueNoiseSampler2f(Vector2f(1f), 0.03f, maxAttempts, 1234)
        .generatePoints()
    for (pt in points2d) {
        Entity(scene)
            .setPosition(Vector3d(pt.x, pt.y, -1f).mul(100.0))
            .add(MeshComponent(DefaultAssets.icoSphere))
    }
    testSceneWithUI("BlueNoise", scene)
}