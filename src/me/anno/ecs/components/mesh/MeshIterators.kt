package me.anno.ecs.components.mesh

import me.anno.gpu.buffer.DrawMode
import me.anno.utils.callbacks.F3U
import me.anno.utils.callbacks.I1U
import me.anno.utils.callbacks.I2U
import me.anno.utils.callbacks.I3U
import me.anno.utils.callbacks.I4U
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

object MeshIterators {

    // for each point

    fun Mesh.forEachPointIndex(onlyFaces: Boolean, callback: I1U) {
        val indices = indices
        if (onlyFaces && indices != null) {
            forEachPointIndex(indices, callback)
        } else {
            val positions = positions ?: return
            for (i in 0 until positions.size / 3) {
                callback.call(i)
            }
        }
    }

    fun Mesh.forEachPoint(onlyFaces: Boolean, callback: F3U) {
        val positions = positions ?: return
        forEachPointIndex(onlyFaces) { i ->
            val ai = i * 3
            callback.call(positions[ai], positions[ai + 1], positions[ai + 2])
        }
    }

    fun HelperMesh.forEachPointIndex(callback: I1U) {
        forEachPointIndex(indices, callback)
    }

    private fun forEachPointIndex(indices: IntArray, callback: I1U) {
        for (i in indices.indices) {
            callback.call(indices[i])
        }
    }

    fun Mesh.forEachPoint(helperMesh: HelperMesh, callback: F3U) {
        val positions = positions ?: return
        helperMesh.forEachPointIndex { i ->
            val ai = i * 3
            callback.call(positions[ai], positions[ai + 1], positions[ai + 2])
        }
    }

    // for each triangle

    fun Mesh.forEachTriangleIndex(callback: I3U) {
        forEachTriangleIndexV2 { x, y, z, _ ->
            callback.call(x, y, z)
        }
    }

    /**
     * ai,bi,ci, faceIndex
     * */
    fun Mesh.forEachTriangleIndexV2(callback: I4U) {
        val positions = positions ?: return
        val indices = indices
        if (indices == null) {
            when (drawMode) {
                DrawMode.TRIANGLES -> {
                    for (i in 0 until positions.size / 9) {
                        val i3 = i * 3
                        callback.call(i3, i3 + 1, i3 + 2, i)
                    }
                }
                DrawMode.TRIANGLE_STRIP -> {
                    val size = positions.size / 3
                    for (i in 2 until size - 1 step 2) {
                        callback.call(i - 2, i - 1, i, i)
                        callback.call(i - 1, i + 1, i, i + 1)
                    }
                    if (size.hasFlag(1)) {
                        val i = size - 2 // correct??, I think so :)
                        callback.call(i - 1, i + 1, i, i + 1)
                    }
                }
                else -> {
                    // no triangles are present
                }
            }
        } else {
            if (indices.size < 3) return
            when (drawMode) {
                DrawMode.TRIANGLES -> {
                    for (i in 0 until indices.size / 3) {
                        val i3 = i * 3
                        callback.call(indices[i3], indices[i3 + 1], indices[i3 + 2], i)
                    }
                }
                DrawMode.TRIANGLE_STRIP -> {
                    var a = indices[0]
                    var b = indices[1]
                    for (i in 2 until indices.size) {
                        val c = indices[i]
                        if (a != b && b != c && c != a) {
                            if (i.hasFlag(1)) {
                                callback.call(a, c, b, i)
                            } else {
                                callback.call(a, b, c, i)
                            }
                        }
                        a = b
                        b = c
                    }
                }
                else -> {
                    // no triangles are present
                }
            }
        }
    }

    fun Mesh.forEachTriangle(callback: (a: Vector3f, b: Vector3f, c: Vector3f) -> Unit) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        forEachTriangle(a, b, c, callback)
        JomlPools.vec3f.sub(3)
    }

    fun Mesh.forEachTriangle(
        a: Vector3f, b: Vector3f, c: Vector3f,
        callback: (a: Vector3f, b: Vector3f, c: Vector3f) -> Unit
    ) {
        val positions = positions ?: return
        forEachTriangleIndex { ai, bi, ci ->
            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            c.set(positions, ci * 3)
            callback(a, b, c)
        }
    }

    fun Mesh.forEachTriangle(
        a: Vector3d, b: Vector3d, c: Vector3d,
        callback: (a: Vector3d, b: Vector3d, c: Vector3d) -> Unit
    ) {
        val positions = positions ?: return
        forEachTriangleIndex { ai, bi, ci ->
            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            c.set(positions, ci * 3)
            callback(a, b, c)
        }
    }

    // count lines

    fun Mesh.countLines(): Int {
        val positions = positions
        val indices = indices
        return max(
            if (indices == null) {
                positions ?: return 0
                val numPoints = positions.size / 3
                when (drawMode) {
                    DrawMode.TRIANGLES -> numPoints
                    DrawMode.TRIANGLE_STRIP -> (numPoints - 2) * 2
                    DrawMode.LINES -> numPoints / 2
                    DrawMode.LINE_STRIP -> numPoints - 1
                    else -> 0
                }
            } else {
                when (drawMode) {
                    DrawMode.TRIANGLES -> indices.size
                    DrawMode.TRIANGLE_STRIP -> (indices.size - 2) * 2
                    DrawMode.LINES -> indices.size / 2
                    DrawMode.LINE_STRIP -> indices.size - 1
                    else -> 0
                }
            }, 0
        )
    }

    // for each line

    fun Mesh.forEachLineIndex(callback: I2U) {
        val positions = positions ?: return
        val indices = indices
        if (indices != null) {
            forEachLineIndex(indices, callback)
        } else {
            val numPoints = positions.size / 3
            when (drawMode) {
                DrawMode.TRIANGLES -> {
                    for (i3 in 0 until numPoints - 2 step 3) {
                        callback.call(i3, i3 + 1)
                        callback.call(i3 + 1, i3 + 2)
                        callback.call(i3 + 2, i3)
                    }
                }
                DrawMode.TRIANGLE_STRIP -> {
                    for (c in 2 until numPoints) {
                        callback.call(c - 2, c)
                        callback.call(c - 1, c)
                    }
                }
                DrawMode.LINES -> {
                    for (i in 0 until numPoints step 2) {
                        callback.call(i, i + 1)
                    }
                }
                DrawMode.LINE_STRIP -> {
                    for (i in 1 until numPoints) {
                        callback.call(i - 1, i)
                    }
                }
                else -> {
                    // no lines present
                }
            }
        }
    }

    fun Mesh.forEachLine(callback: (Vector3f, Vector3f) -> Unit) {
        val positions = positions ?: return
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        forEachLineIndex { ai, bi ->
            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            callback(a, b)
        }
        JomlPools.vec3f.sub(2)
    }

    fun Mesh.forEachLineIndex(helperMesh: HelperMesh, callback: I2U) {
        forEachLineIndex(helperMesh.indices, callback)
    }

    private fun Mesh.forEachLineIndex(indices: IntArray, callback: I2U) {
        when (drawMode) {
            DrawMode.TRIANGLES -> {
                for (i in 0 until indices.size - 2 step 3) {
                    val a = indices[i]
                    val b = indices[i + 1]
                    val c = indices[i + 2]
                    callback.call(a, b)
                    callback.call(b, c)
                    callback.call(c, a)
                }
            }
            DrawMode.TRIANGLE_STRIP -> {
                var a = indices[0]
                var b = indices[1]
                for (i in 2 until indices.size) {
                    val c = indices[i]
                    if (a != b && a != c && b != c) {
                        callback.call(a, c)
                        callback.call(b, c)
                    }
                    a = b
                    b = c
                }
            }
            DrawMode.LINES -> {
                for (i in 0 until indices.size - 1 step 2) {
                    callback.call(indices[i], indices[i + 1])
                }
            }
            DrawMode.LINE_STRIP -> {
                var a = indices[0]
                for (i in 1 until indices.size) {
                    val b = indices[i]
                    if (a != b) callback.call(a, b)
                    a = b
                }
            }
            else -> {}
        }
    }
}