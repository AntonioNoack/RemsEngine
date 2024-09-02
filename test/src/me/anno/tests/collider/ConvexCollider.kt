package me.anno.tests.collider

import me.anno.ecs.components.collider.ConvexCollider
import me.anno.ecs.components.mesh.Mesh

fun main() {
    // todo generate some sample mesh
    // todo why is it not rotating???
    val mesh = Mesh()
    val collider = ConvexCollider()
    collider.points = floatArrayOf(
        -1f, 0f, 0f,
        +1f, 0f, 0f,
        0f, -1f, 0f,
        0f, +1f, 0f,
        0f, 0f, -1f,
        0f, 0f, +1f
    )
    mesh.positions = collider.points
    mesh.indices = intArrayOf(
        0, 4, 2, 0, 2, 5,
        0, 5, 3, 0, 3, 4,

        1, 2, 4, 1, 5, 2,
        1, 3, 5, 1, 4, 3,
    )
    mesh.calculateNormals(false)
    testCollider(collider, mesh.ref)
}