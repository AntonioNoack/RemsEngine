package me.anno.tests.collider

import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.posMod
import me.anno.utils.assertions.assertEquals
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    // todo why is it not rotating??
    val collider = ConeCollider()
    collider.axis = 2
    testCollider(collider, generateConeMesh(collider).ref)
}

fun generateConeMesh(collider: ConeCollider): Mesh {
    // todo make normals beautiful somehow...
    val ay = collider.axis
    val ax = posMod(ay + 2, 3)
    val az = posMod(ay + 1, 3)
    val mesh = Mesh()
    val n = 16
    val r = collider.radius.toFloat()
    val h = collider.height.toFloat() * 0.5f
    val positions = FloatArray(n * 3 * 3)
    for (i in 0 until 2 * n) {
        val angle = i * TAUf / n
        positions[i * 3 + ax] = cos(angle) * r
        positions[i * 3 + ay] = -h
        positions[i * 3 + az] = sin(angle) * r
    }
    for (i in 2 * n until 3 * n) {
        positions[i * 3 + ay] = +h
    }
    val indices = IntArray(n * 3 + (n - 2) * 3)
    var j = 0
    for (i in 0 until n) {
        indices[j++] = posMod(i + 1, n)
        indices[j++] = i
        indices[j++] = n * 2 + i
    }
    for (i in 2 until n) {
        indices[j++] = n + i - 1
        indices[j++] = n + i
        indices[j++] = n
    }
    mesh.positions = positions
    mesh.indices = indices
    mesh.calculateNormals(false)
    return mesh
}