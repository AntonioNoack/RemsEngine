package me.anno.tests.collider

import me.anno.ecs.components.collider.Axis
import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.MaxAreaCircle
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.length
import me.anno.maths.Maths.posMod
import me.anno.sdf.shapes.SDFCone
import me.anno.sdf.shapes.SDFPyramid
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

fun main() {
    val collider = ConeCollider()
    collider.axis = Axis.Y
    testCollider(collider, generateConeMesh(collider).ref) {
        it.add(SDFPyramid())
    }
}

fun generateConeMesh(collider: ConeCollider): Mesh {

    val ay = collider.axis.id
    val ax = posMod(ay + 2, 3)
    val az = posMod(ay + 1, 3)
    val mesh = Mesh()
    val n = 16
    val r = collider.radius
    val h = collider.height

    val v = 7
    val numVertices = n * (v + 1) + 1
    val positions = FloatArray(numVertices * 3)
    val normals = FloatArray(numVertices * 3)

    var k = 0
    val invNormal = 1f / length(r, h)
    val nr = invNormal * r
    val nh = invNormal * h
    for (j in -1 until v) {
        // power to create more rings at the tip, where normals get more problematic
        // exponent = 2 seems ideal from my testing
        val rx = ((v - max(j, 0)).toFloat() / v).pow(2)
        val rv = r * rx
        val end = h * (0.5f - rx)
        for (i in 0 until n) {
            val angle = i * TAUf / n
            val c = cos(angle)
            val s = sin(angle)
            positions[k + ax] = c * rv
            positions[k + ay] = end
            positions[k + az] = s * rv
            if (j == -1) {
                normals[k + ay] = -1f
            } else {
                normals[k + ax] = c * nh
                normals[k + ay] = nr
                normals[k + az] = s * nh
            }
            k += 3
        }
    }

    // top
    positions[k + ay] = h * 0.5f
    normals[k + ay] = 1f

    val numBottomTriangles = n - 2
    val numRingTriangles = n + (v - 1) * n * 2
    val indices = IntArray((numRingTriangles + numBottomTriangles) * 3)

    k = 0
    val t0 = v * n
    val t1 = (v + 1) * n
    for (i in 0 until n) { // side triangles for top cone
        indices[k++] = t0 + posMod(i + 1, n)
        indices[k++] = t0 + i
        indices[k++] = t1
    }

    for (j in 1 until v) {
        val t0 = j * n
        val t1 = (j + 1) * n
        for (i0 in 0 until n) { // side quads
            val i1 = posMod(i0 + 1, n)
            indices[k++] = t0 + i1
            indices[k++] = t0 + i0
            indices[k++] = t1 + i0

            indices[k++] = t1 + i0
            indices[k++] = t1 + i1
            indices[k++] = t0 + i1
        }
    }

    // create bottom circle
    MaxAreaCircle.createCircleIndices(n, 0, indices, k, true)

    mesh.positions = positions
    mesh.indices = indices
    mesh.normals = normals
    // mesh.calculateNormals(false)
    return mesh
}