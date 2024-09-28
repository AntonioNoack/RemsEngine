package me.anno.ecs.components.mesh

import me.anno.gpu.buffer.DrawMode
import me.anno.utils.callbacks.F3Z
import me.anno.utils.callbacks.I1Z
import me.anno.utils.callbacks.I2Z
import me.anno.utils.callbacks.I3Z
import me.anno.utils.callbacks.I4Z
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

object MeshIterators {

    // for each point

    fun Mesh.forEachPointIndex(onlyFaces: Boolean, callback: I1Z) {
        val indices = indices
        if (onlyFaces && indices != null) {
            forEachPointIndex(indices, callback)
        } else {
            val positions = positions ?: return
            for (i in 0 until positions.size / 3) {
                if (callback.call(i)) break
            }
        }
    }

    fun Mesh.forEachPoint(onlyFaces: Boolean, callback: F3Z) {
        val positions = positions ?: return
        forEachPointIndex(onlyFaces) { i ->
            val ai = i * 3
            callback.call(positions[ai], positions[ai + 1], positions[ai + 2])
        }
    }

    fun HelperMesh.forEachPointIndex(callback: I1Z) {
        forEachPointIndex(indices, callback)
    }

    private fun forEachPointIndex(indices: IntArray, callback: I1Z) {
        for (i in indices.indices) {
            if (callback.call(indices[i])) break
        }
    }

    fun Mesh.forEachPoint(helperMesh: HelperMesh, callback: F3Z) {
        val positions = positions ?: return
        helperMesh.forEachPointIndex { i ->
            val ai = i * 3
            callback.call(positions[ai], positions[ai + 1], positions[ai + 2])
        }
    }

    // for each triangle

    fun Mesh.forEachTriangleIndex(callback: I3Z) {
        forEachTriangleIndexV2 { x, y, z, _ ->
            callback.call(x, y, z)
        }
    }

    /**
     * ai,bi,ci, faceIndex
     * */
    fun Mesh.forEachTriangleIndexV2(callback: I4Z) {
        val positions = positions ?: return
        val indices = indices
        if (indices == null) {
            when (drawMode) {
                DrawMode.TRIANGLES -> {
                    for (i in 0 until positions.size / 9) {
                        val i3 = i * 3
                        if (callback.call(i3, i3 + 1, i3 + 2, i)) break
                    }
                }
                DrawMode.TRIANGLE_STRIP -> {
                    val size = positions.size / 3
                    var done = false
                    for (i in 2 until size - 1 step 2) {
                        done = callback.call(i - 2, i - 1, i, i) ||
                                callback.call(i - 1, i + 1, i, i + 1)
                        if (done) break
                    }
                    if (!done && size.hasFlag(1)) {
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
                        if (callback.call(indices[i3], indices[i3 + 1], indices[i3 + 2], i)) break
                    }
                }
                DrawMode.TRIANGLE_STRIP -> {
                    var a = indices[0]
                    var b = indices[1]
                    for (i in 2 until indices.size) {
                        val c = indices[i]
                        if (a != b && b != c && c != a) {
                            val done = if (i.hasFlag(1)) {
                                callback.call(a, c, b, i)
                            } else {
                                callback.call(a, b, c, i)
                            }
                            if (done) break
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

    fun Mesh.forEachTriangle(callback: (a: Vector3f, b: Vector3f, c: Vector3f) -> Boolean) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        forEachTriangle(a, b, c, callback)
        JomlPools.vec3f.sub(3)
    }

    fun Mesh.forEachTriangle(
        a: Vector3f, b: Vector3f, c: Vector3f,
        callback: (a: Vector3f, b: Vector3f, c: Vector3f) -> Boolean
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
        callback: (a: Vector3d, b: Vector3d, c: Vector3d) -> Boolean
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

    fun Mesh.forEachLineIndex(callback: I2Z) {
        val positions = positions ?: return
        val indices = indices
        if (indices != null) {
            forEachLineIndex(indices, callback)
        } else {
            val numPoints = positions.size / 3
            when (drawMode) {
                DrawMode.TRIANGLES -> {
                    for (i3 in 0 until numPoints - 2 step 3) {
                        if (callback.call(i3, i3 + 1) ||
                            callback.call(i3 + 1, i3 + 2) ||
                            callback.call(i3 + 2, i3)
                        ) break
                    }
                }
                DrawMode.TRIANGLE_STRIP -> {
                    for (c in 2 until numPoints) {
                        if (callback.call(c - 2, c) ||
                            callback.call(c - 1, c)
                        ) break
                    }
                }
                DrawMode.LINES -> {
                    for (i in 0 until numPoints step 2) {
                        if (callback.call(i, i + 1)) break
                    }
                }
                DrawMode.LINE_STRIP -> {
                    for (i in 1 until numPoints) {
                        if (callback.call(i - 1, i)) break
                    }
                }
                else -> {
                    // no lines present
                }
            }
        }
    }

    fun Mesh.forEachLine(callback: (Vector3f, Vector3f) -> Boolean) {
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

    fun Mesh.forEachLineIndex(helperMesh: HelperMesh, callback: I2Z) {
        forEachLineIndex(helperMesh.indices, callback)
    }

    private fun Mesh.forEachLineIndex(indices: IntArray, callback: I2Z) {
        when (drawMode) {
            DrawMode.TRIANGLES -> {
                for (i in 0 until indices.size - 2 step 3) {
                    val a = indices[i]
                    val b = indices[i + 1]
                    val c = indices[i + 2]
                    if (callback.call(a, b) ||
                        callback.call(b, c) ||
                        callback.call(c, a)
                    ) break
                }
            }
            DrawMode.TRIANGLE_STRIP -> {
                var a = indices[0]
                var b = indices[1]
                for (i in 2 until indices.size) {
                    val c = indices[i]
                    if (a != b && a != c && b != c) {
                        if (callback.call(a, c) ||
                            callback.call(b, c)
                        ) break
                    }
                    a = b
                    b = c
                }
            }
            DrawMode.LINES -> {
                for (i in 0 until indices.size - 1 step 2) {
                    if (callback.call(indices[i], indices[i + 1])) break
                }
            }
            DrawMode.LINE_STRIP -> {
                var a = indices[0]
                for (i in 1 until indices.size) {
                    val b = indices[i]
                    if (a != b && callback.call(a, b)) {
                        break
                    }
                    a = b
                }
            }
            else -> {}
        }
    }
}