package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.ecs.components.mesh.utils.IndexRemover.removeIndices
import me.anno.gpu.buffer.DrawMode
import me.anno.maths.Maths
import me.anno.mesh.MeshUtils.numPoints
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Triangles.subCross
import org.joml.Vector3f
import org.joml.Vector3f.Companion.lengthSquared
import kotlin.math.min
import kotlin.math.sqrt

// todo some blender meshes have broken normals
object NormalCalculator {

    @JvmStatic
    fun needsNormalsComputation(normals: FloatArray, stride: Int): Boolean {
        for (j in 0 until normals.size / stride) {
            if (!isNormalValid(normals, j * stride)) return true
        }
        return false
    }

    @JvmStatic
    fun isNormalValid(normals: FloatArray, offset: Int): Boolean {
        val nx = normals[offset]
        val ny = normals[offset + 1]
        val nz = normals[offset + 2]
        val length = lengthSquared(nx, ny, nz)
        return length in 0.7f..1.1f
    }

    // calculate = pure arithmetics
    // compute = calculations with rules
    @JvmStatic
    private fun calculateFlatNormal(
        positions: FloatArray,
        i0: Int, i1: Int, i2: Int,
        a: Vector3f, b: Vector3f, c: Vector3f,
        normal: Vector3f
    ) {
        a.set(positions, i0)
        b.set(positions, i1)
        c.set(positions, i2)
        subCross(a, b, c, normal)
    }

    @JvmStatic
    private fun computeNormalsIndexed(mesh: Mesh, positions: FloatArray, normals: FloatArray) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        val normal = JomlPools.vec3f.create()
        val weights = FloatArray(positions.size / 3)
        for (j in weights.indices) {
            val i = j * 3
            val lenSq = lengthSquared(normals[i], normals[i + 1], normals[i + 2])
            if (lenSq > 1e-20f && lenSq.isFinite()) {
                val factor = 1f / sqrt(lenSq)
                normals[i] *= factor
                normals[i + 1] *= factor
                normals[i + 2] *= factor
                // fine -> we don't need weights
                weights[j] = -1f
            } else {
                // clear it, because we will sum our normal there, and these values might be NaN
                normals[i + 0] = 0f
                normals[i + 1] = 0f
                normals[i + 2] = 0f
            }
        }
        mesh.forEachTriangleIndex { i0, i1, i2 ->
            if ((i0 in weights.indices && weights[i0] >= 0f) ||
                (i1 in weights.indices && weights[i1] >= 0f) ||
                (i2 in weights.indices && weights[i2] >= 0f)
            ) {
                // we need this point
                calculateFlatNormal(positions, i0 * 3, i1 * 3, i2 * 3, a, b, c, normal)
                val weight = normal.length()
                if (weight > 0f && weight.isFinite()) {
                    if (weights[i0] >= 0) addWeightAndNormal(weights, i0, normals, normal)
                    if (weights[i1] >= 0) addWeightAndNormal(weights, i1, normals, normal)
                    if (weights[i2] >= 0) addWeightAndNormal(weights, i2, normals, normal)
                }
            }
            false
        }
        // apply all the normals, smooth shading
        for (j in weights.indices) {
            val weight = weights[j]
            // < 0: the normal is already done
            // = 0: no faces were found
            // = 1: we don't need to further normalize it, as its weight is already 1
            // > 1: we need to normalize it
            if (weight > 0f) {
                val i = j * 3
                // dividing by the weight count is no enough, since the normal needs to be normalized,
                // and avg(normals) will not have length 1, if there are different input normals
                val weightInv = 1f / Maths.max(1e-38f, Maths.length(normals[i + 0], normals[i + 1], normals[i + 2]))
                normals[i + 0] *= weightInv
                normals[i + 1] *= weightInv
                normals[i + 2] *= weightInv
            }
        }
        JomlPools.vec3f.sub(4)
    }

    @JvmStatic
    private fun computeNormalsNonIndexed(positions: FloatArray, normals: FloatArray) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        val normal = JomlPools.vec3f.create()
        // just go through the vertices;
        // mode to calculate smooth shading by clustering points?
        forLoopSafely(min(positions.size, normals.size), 9) { i ->
            // check whether the normal update is needed
            val needsUpdate = !isNormalValid(normals, i) ||
                    !isNormalValid(normals, i + 3) ||
                    !isNormalValid(normals, i + 6)
            if (needsUpdate) {
                // flat shading
                calculateFlatNormal(positions, i, i + 3, i + 6, a, b, c, normal)
                normal.safeNormalize()

                val nx = normal.x
                val ny = normal.y
                val nz = normal.z

                normals[i] = nx
                normals[i + 1] = ny
                normals[i + 2] = nz

                normals[i + 3] = nx
                normals[i + 4] = ny
                normals[i + 5] = nz

                normals[i + 6] = nx
                normals[i + 7] = ny
                normals[i + 8] = nz
            }
        }
        JomlPools.vec3f.sub(3)
    }

    @JvmStatic
    private fun computeNormalsNonIndexedStrip(positions: FloatArray, normals: FloatArray) {
        // todo normal is avg from all 3 triangles containing it :)
        return computeNormalsNonIndexed(positions, normals)
    }

    @JvmStatic
    fun checkNormals(mesh: Mesh, positions: FloatArray, normals: FloatArray, indices: IntArray?, drawMode: DrawMode) {
        // first an allocation free check
        when (drawMode) {
            DrawMode.TRIANGLES -> {
                if (needsNormalsComputation(normals, 3)) {
                    if (indices == null) {
                        computeNormalsNonIndexed(positions, normals)
                    } else {
                        computeNormalsIndexed(mesh, positions, normals)
                    }
                }
            }
            DrawMode.TRIANGLE_STRIP -> {
                if (needsNormalsComputation(normals, 3)) {
                    if (indices == null) {
                        computeNormalsNonIndexedStrip(positions, normals)
                    } else {
                        computeNormalsIndexed(mesh, positions, normals)
                    }
                }
            }
            else -> {
                // normals cannot be computed
            }
        }
    }

    @JvmStatic
    private fun addWeightAndNormal(weights: FloatArray, i0: Int, normals: FloatArray, normal: Vector3f) {
        weights[i0]++
        val j = i0 * 3
        normals[j + 0] += normal.x
        normals[j + 1] += normal.y
        normals[j + 2] += normal.z
    }

    /**
     * @param mesh the mesh, where the normals shall be recalculated; indices should be null, but it might work anyway
     * */
    @JvmStatic
    fun calculateSmoothNormals(
        mesh: Mesh, maxCosineAngle: Float,
        minVertexDistance: Float
    ) {

        // merge points with same normals
        // extend by normal, and bucket size relative to that size
        val map = NormalHelperTree(mesh.numPoints / 3, mesh.getBounds(), minVertexDistance)

        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        val normal = Vector3f()

        mesh.forEachTriangle(a, b, c) { a, b, c ->
            subCross(a, b, c, normal).safeNormalize()
            map.put(a, normal)
            map.put(b, normal)
            map.put(c, normal)
            false
        }

        val positions = mesh.positions!!
        val normals = mesh.normals.resize(positions.size)
        mesh.normals = normals

        // query all values
        normals.fill(0f)

        val tmp = Vector3f()
        mesh.forEachTriangleIndex { ai, bi, ci ->
            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            c.set(positions, ci * 3)
            subCross(a, b, c, normal).safeNormalize()
            get(map, a, normal, maxCosineAngle, tmp)
                .addInto(normals, ai * 3)
            get(map, b, normal, maxCosineAngle, tmp)
                .addInto(normals, bi * 3)
            get(map, c, normal, maxCosineAngle, tmp)
                .addInto(normals, ci * 3)
            false
        }

        if (mesh.indices != null) {
            // normalize all values
            forLoopSafely(positions.size, 3) { i ->
                normal.set(normals, i)
                    .safeNormalize()
                    .get(normals, i)
            }
        }
    }

    private fun Vector3f.addInto(v: FloatArray, i: Int) {
        v[i] += x
        v[i + 1] += y
        v[i + 2] += z
    }

    @JvmStatic
    private fun get(
        map: NormalHelperTree, position: Vector3f, normal: Vector3f,
        maxCosineAngle: Float, dst: Vector3f,
    ): Vector3f {

        dst.set(0f)

        val gridIndexBaseCorner = map.gridIndexBaseCorner(position)

        // loop of 2³ neighbors to avoid rounding errors separating vertices
        // putting this in the getter instead of the setter is 2x faster (120ms vs 58ms)
        var numValues = 0
        for (i in 0 until 8) {
            val gridIndexI = map.gridIndexIthCorner(gridIndexBaseCorner, i)
            val normals = map.entries[gridIndexI] ?: continue

            forLoopSafely(normals.size, 3) { i ->
                val nx = normals[i]
                val ny = normals[i + 1]
                val nz = normals[i + 2]
                if (normal.dot(nx, ny, nz) >= maxCosineAngle) {
                    dst.add(nx, ny, nz)
                    numValues++
                }
            }
        }

        if (numValues > 1) dst.safeNormalize()
        return dst
    }

    @JvmStatic
    fun Mesh.makeFlatShaded(calculateNormals: Boolean = true) {
        if (indices != null) removeIndices()
        if (calculateNormals) calculateNormals(false)
    }
}