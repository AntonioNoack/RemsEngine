package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.ecs.components.mesh.utils.IndexRemover.removeIndices
import me.anno.gpu.buffer.DrawMode
import me.anno.maths.Maths
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Triangles
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
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
        val length = nx * nx + ny * ny + nz * nz
        return length in 0.7f..1.1f
    }

    // calculate = pure arithmetics
    // compute = calculations with rules
    @JvmStatic
    private fun calculateFlatNormal(
        positions: FloatArray,
        i0: Int, i1: Int, i2: Int,
        a: Vector3f, b: Vector3f, c: Vector3f
    ): Vector3f {
        a.set(positions[i0], positions[i0 + 1], positions[i0 + 2])
        b.set(positions[i1], positions[i1 + 1], positions[i1 + 2])
        c.set(positions[i2], positions[i2 + 1], positions[i2 + 2])
        b.sub(a)
        c.sub(a)
        val r = b.cross(c)
        val l = r.length()
        return if (l > 0f) r.div(l) else r.set(0f)
        // todo sometimes one is correct, and sometimes the opposite.. why?
        // c-b: Shape.smoothCube.front
        // b-c: Shape.tetrahedron.front, mesh from nav mesh
    }

    @JvmStatic
    private fun computeNormalsIndexed(mesh: Mesh, positions: FloatArray, normals: FloatArray) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        val weights = IntArray(positions.size / 3)
        for (j in weights.indices) {
            val i = j * 3
            if (abs(normals[i]) + abs(normals[i + 1]) + abs(normals[i + 2]) > 0.001) {
                // fine -> we don't need weights
                weights[j] = -1
            } else {
                // clear it, because we will sum our normal there
                normals[i + 0] = 0f
                normals[i + 1] = 0f
                normals[i + 2] = 0f
            }
        }
        mesh.forEachTriangleIndex { i0, i1, i2 ->
            if ((i0 in weights.indices && weights[i0] >= 0) ||
                (i1 in weights.indices && weights[i1] >= 0) ||
                (i2 in weights.indices && weights[i2] >= 0)
            ) {
                // we need this point
                val normal = calculateFlatNormal(positions, i0 * 3, i1 * 3, i2 * 3, a, b, c)
                if (weights[i0] >= 0) addWeightAndNormal(weights, i0, normals, normal)
                if (weights[i1] >= 0) addWeightAndNormal(weights, i1, normals, normal)
                if (weights[i2] >= 0) addWeightAndNormal(weights, i2, normals, normal)
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
            if (weight > 1) {
                val i = j * 3
                // dividing by the weight count is no enough, since the normal needs to be normalized,
                // and avg(normals) will not have length 1, if there are different input normals
                val weightInv = 1f / Maths.max(0.1f, Maths.length(normals[i + 0], normals[i + 1], normals[i + 2]))
                normals[i + 0] *= weightInv
                normals[i + 1] *= weightInv
                normals[i + 2] *= weightInv
            }
        }
        JomlPools.vec3f.sub(3)
    }

    @JvmStatic
    private fun computeNormalsNonIndexed(positions: FloatArray, normals: FloatArray) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        // just go through the vertices;
        // mode to calculate smooth shading by clustering points?
        forLoopSafely(min(positions.size, normals.size), 9) { i ->
            // check whether the normal update is needed
            val needsUpdate = !isNormalValid(normals, i) ||
                    !isNormalValid(normals, i + 3) ||
                    !isNormalValid(normals, i + 6)
            if (needsUpdate) {
                // flat shading
                val normal = calculateFlatNormal(positions, i, i + 3, i + 6, a, b, c)
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
    private fun addWeightAndNormal(weights: IntArray, i0: Int, normals: FloatArray, normal: Vector3f) {
        weights[i0]++
        val j = i0 * 3
        normals[j + 0] += normal.x
        normals[j + 1] += normal.y
        normals[j + 2] += normal.z
    }

    /**
     * @param mesh the mesh, where the normals shall be recalculated; indices should be null, but it might work anyway
     * @param maxAllowedAngle maximum allowed angle for smoothing, in radians, typically 15°-45°
     * @param largeLength all triangles, which have a larger perimeter than largeLength, will be ignored
     * @param normalScale internal parameter for a deviation; shall be ~MinVertexDistance/3, or if unknown, ~MeshScale/100; must not be too small
     * */
    @JvmStatic
    fun calculateSmoothNormals(mesh: Mesh, maxAllowedAngle: Float, largeLength: Float, normalScale: Float) {
        val llSq = largeLength * largeLength
        val maxD = Maths.length(cos(maxAllowedAngle) - 1f, sin(maxAllowedAngle))
        calculateSmoothNormals(mesh, normalScale) { a, b, c ->
            if (a.distanceSquared(b) + b.distanceSquared(c) + c.distanceSquared(a) < llSq) maxD
            else normalScale
        }
    }

    /**
     * @param mesh the mesh, where the normals shall be recalculated; indices should be null, but it might work anyway
     * @param normalScale internal parameter for a deviation; shall be ~MinVertexDistance/3, or if unknown, ~MeshScale/100; must not be too small
     * @param getTriangleSmoothness how smooth the triangle shall be; 0 = flat shaded, 1 = fully smooth (no matter the angle up to 180°), 2 = fully smooth, any angle
     * */
    @JvmStatic
    fun calculateSmoothNormals(
        mesh: Mesh, normalScale: Float,
        getTriangleSmoothness: (Vector3f, Vector3f, Vector3f) -> Float
    ) {
        // merge points with same normals
        // extend by normal, and bucket size relative to that size
        val map = NormalHelperTree<Vector3f>()
        val normal = Vector3f()
        val min = Vector3f()
        val max = Vector3f()
        mesh.forEachTriangle { a, b, c ->
            Triangles.subCross(a, b, c, normal)
            val maxC = getTriangleSmoothness(a, b, c)
            if (maxC > 0f) {
                val maxD = maxC * normalScale
                normal.safeNormalize(normalScale)
                add(map, min, max, a, normal, maxD)
                add(map, min, max, b, normal, maxD)
                add(map, min, max, c, normal, maxD)
            }
            false
        }
        val positions = mesh.positions!!
        val normals = mesh.normals.resize(positions.size)
        mesh.normals = normals
        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        val tmp = Vector3f()
        for (i in 0 until positions.size - 8) {
            a.set(positions, i)
            b.set(positions, i + 3)
            c.set(positions, i + 6)
            Triangles.subCross(a, b, c, normal)
            val maxC = getTriangleSmoothness(a, b, c)
            if (maxC > 0f) {
                val maxD = maxC * normalScale
                normal.safeNormalize(normalScale)
                // query normal at each point
                get(map, min, max, a.add(normal), maxD, tmp).get(normals, i)
                get(map, min, max, b.add(normal), maxD, tmp).get(normals, i + 3)
                get(map, min, max, c.add(normal), maxD, tmp).get(normals, i + 6)
            } else {
                // disable smoothing on large triangles
                normal.normalize()
                normal.get(normals, i)
                normal.get(normals, i + 3)
                normal.get(normals, i + 6)
            }
        }
    }

    @JvmStatic
    fun add(
        map: NormalHelperTree<Vector3f>,
        min: Vector3f, max: Vector3f,
        a: Vector3f, normal: Vector3f, maxD: Float
    ) {
        // todo smoothing isn't working correctly on 50747 yet
        a.add(normal)
        val maxD2 = maxD * maxD
        val maxDV2 = 1e-5f * (a.lengthSquared() + 1f)
        val maxDV1 = sqrt(maxDV2)
        min.set(a).sub(maxDV1, maxDV1, maxDV1)
        max.set(a).add(maxDV1, maxDV1, maxDV1)
        if (!map.query(min, max) { k ->
                // identical
                val found = k.first.distanceSquared(a) <= maxDV2 &&
                        Maths.sq(k.second.dot(normal)) > 0.99f * (normal.lengthSquared() * k.second.lengthSquared())
                if (found) k.second.add(normal)
                found
            }) {
            // not found -> add to map
            map.add(Pair(Vector3f(a), Vector3f(normal)))
            min.set(a).sub(maxD, maxD, maxD)
            max.set(a).add(maxD, maxD, maxD)
            // and add to all neighbors
            map.query(min, max) { k ->
                val foundIt = k.first.distanceSquared(a) <= maxD2
                if (foundIt) k.second.add(normal)
                false
            }
        }
    }

    @JvmStatic
    fun get(
        map: NormalHelperTree<Vector3f>,
        min: Vector3f, max: Vector3f,
        a: Vector3f, maxD: Float,
        dst: Vector3f
    ): Vector3f {
        min.set(a).sub(maxD, maxD, maxD)
        max.set(a).add(maxD, maxD, maxD)
        val maxD2 = maxD * maxD
        map.query(min, max) { k ->
            val foundIt = k.first.distanceSquared(a) <= maxD2
            if (foundIt) dst.set(k.second)
            foundIt
        }
        return dst.normalize()
    }

    @JvmStatic
    fun Mesh.makeFlatShaded(calculateNormals: Boolean = true) {
        if (indices != null) removeIndices()
        if (calculateNormals) calculateNormals(false)
    }
}