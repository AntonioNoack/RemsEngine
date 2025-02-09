package me.anno.tests.engine.light

import me.anno.ecs.Entity
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import kotlin.math.sqrt

fun main() {
    // light can be really noisy (screen-space)... why??
    //  -> because we were using fp16, and it was at its limit;
    //  we could work around that by conditionally smoothing, but that may be more expensive than just using fp32
    // todo instanced rendering for lights still has the perspective-driver-bug
    //  and I correctly set the "flat" attribute, so I guess there is nothing, I can do :(
    val scene = Entity("Scene")
    Entity("Curved Floor", scene)
        .add(MeshComponent(topOfSphere(10f, 100, 5000f)))
    Entity("Light", scene)
        .add(PointLight().apply { color.set(1000f) })
        .setPosition(0.0, 0.03, 0.0)
        .setScale(1.0, 8.0, 1.0)
    testSceneWithUI("Noisy Light", scene)
}

fun topOfSphere(size: Float, steps: Int, radius: Float): Mesh {
    val mesh = RectangleTerrainModel.generateRegularQuadHeightMesh(
        steps, steps, false, size / steps, Mesh(),
        true
    )
    val positions = mesh.positions!!
    val normals = mesh.normals!!
    for (i in positions.indices step 3) {
        val x = positions[i]
        val z = positions[i + 2]
        val y = sqrt(radius * radius - (x * x + z * z))
        positions[i + 1] = y - radius
        normals[i] = x / radius
        normals[i + 1] = y / radius
        normals[i + 2] = z / radius
    }
    mesh.calculateNormals(true)
    mesh.invalidateGeometry()
    return mesh
}