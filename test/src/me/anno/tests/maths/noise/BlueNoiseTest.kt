package me.anno.tests.maths.noise

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.BlueNoiseSampler.sampleBlueNoise3f
import me.anno.maths.noise.BlueNoiseSampler.sampleBlueNoise2f
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * test 2d and 3d blue noise
 * */
fun main() {
    val scene = Entity()
    val points3d = sampleBlueNoise3f(Vector3f(1f), 0.1f, 1234)
    for (pt in points3d) {
        Entity(scene)
            .setPosition(Vector3d(pt).mul(100.0))
            .add(MeshComponent(DefaultAssets.icoSphere))
    }

    val points2d = sampleBlueNoise2f(Vector2f(1f), 0.03f, 1234)
    for (pt in points2d) {
        Entity(scene)
            .setPosition(Vector3d(pt.x, pt.y, -1f).mul(100.0))
            .add(MeshComponent(DefaultAssets.icoSphere))
    }
    testSceneWithUI("BlueNoise", scene)
}