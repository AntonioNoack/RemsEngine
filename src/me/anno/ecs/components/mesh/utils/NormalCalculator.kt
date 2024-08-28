package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.gpu.buffer.DrawMode
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Triangles
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// todo something seems to be incorrect... some blender meshes have broken normals
object NormalCalculator {

    private val LOGGER = LogManager.getLogger(NormalCalculator::class)

    fun needsNormalsComputation(normals: FloatArray, stride: Int): Boolean {
        for (j in 0 until normals.size / stride) {
            if (!isNormalValid(normals, j * stride)) return true
        }
        return false
    }

    fun isNormalValid(normals: FloatArray, offset: Int): Boolean {
        val nx = normals[offset]
        val ny = normals[offset + 1]
        val nz = normals[offset + 2]
        val length = nx * nx + ny * ny + nz * nz
        return length in 0.7f..1.1f
    }

    // calculate = pure arithmetics
    // compute = calculations with rules
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
            if ((i0 < weights.size && weights[i0] >= 0) ||
                (i1 < weights.size && weights[i1] >= 0) ||
                (i2 < weights.size && weights[i2] >= 0)
            ) {
                // we need this point
                val normal = calculateFlatNormal(positions, i0 * 3, i1 * 3, i2 * 3, a, b, c)
                if (weights[i0] >= 0) addWeightAndNormal(weights, i0, normals, normal)
                if (weights[i1] >= 0) addWeightAndNormal(weights, i1, normals, normal)
                if (weights[i2] >= 0) addWeightAndNormal(weights, i2, normals, normal)
            }
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

    private fun computeNormalsNonIndexed(positions: FloatArray, normals: FloatArray) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        // just go through the vertices;
        // mode to calculate smooth shading by clustering points?
        val size = Maths.min(positions.size, normals.size) - 8
        for (i in 0 until size step 9) {
            // check whether the normal update is needed
            val needsUpdate = !isNormalValid(normals, i) ||
                    !isNormalValid(normals, i + 3) ||
                    !isNormalValid(normals, i + 6)
            if (needsUpdate) {
                // flat shading
                val normal = calculateFlatNormal(positions, i, i + 3, i + 6, a, b, c)
                for (offset in i until i + 9 step 3) {
                    normals[offset + 0] = normal.x
                    normals[offset + 1] = normal.y
                    normals[offset + 2] = normal.z
                }
            }
        }
        JomlPools.vec3f.sub(3)
    }

    private fun computeNormalsNonIndexedStrip(positions: FloatArray, normals: FloatArray) {
        // todo normal is avg from all 3 triangles containing it :)
        return computeNormalsNonIndexed(positions, normals)
    }

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

    private fun addWeightAndNormal(weights: IntArray, i0: Int, normals: FloatArray, normal: Vector3f) {
        weights[i0]++
        val j = i0 * 3
        normals[j + 0] += normal.x
        normals[j + 1] += normal.y
        normals[j + 2] += normal.z
    }

    fun generateIndices(
        positions: FloatArray,
        uvs: FloatArray?,
        color0: IntArray?,
        materialIndices: IntArray?,
        boneIndices: ByteArray?,
        boneWeights: FloatArray?
    ): IntArray {

        // todo we probably should be able to specify a merging radius, so close points get merged
        // for that however, we need other acceleration structures like oct-trees to accelerate the queries

        // what defines a unique point:
        // position, uvs, material index, bone indices and bone weights
        // in the future, we should maybe support all colors...
        class Point(
            var x: Float, var y: Float, var z: Float,
            var u: Float, var v: Float,
            var color0: Int,
            var materialIndex: Int,
            var b0: Byte, var b1: Byte, var b2: Byte, var b3: Byte,
            var w0: Float, var w1: Float, var w2: Float, var w3: Float,
        ) {

            constructor(x: Float, y: Float, z: Float) :
                    this(x, y, z, 0f, 0f, 0, 0, 0, 0, 0, 0, 0f, 0f, 0f, 0f)

            // I would use a data class, if they were mutable...
            private var _hashCode = 0
            override fun hashCode(): Int {
                if (_hashCode != 0) return _hashCode
                var hashCode = x.hashCode() * 31
                hashCode = hashCode * 31 + y.hashCode()
                hashCode = hashCode * 31 + z.hashCode()
                hashCode = hashCode * 31 + u.hashCode()
                hashCode = hashCode * 31 + v.hashCode()
                hashCode = hashCode * 31 + this.color0.hashCode()
                hashCode = hashCode * 31 + materialIndex.hashCode()
                hashCode = hashCode * 31 + b0.hashCode()
                hashCode = hashCode * 31 + b1.hashCode()
                hashCode = hashCode * 31 + b2.hashCode()
                hashCode = hashCode * 31 + b3.hashCode()
                hashCode = hashCode * 31 + w0.hashCode()
                hashCode = hashCode * 31 + w1.hashCode()
                hashCode = hashCode * 31 + w2.hashCode()
                hashCode = hashCode * 31 + w3.hashCode()
                this._hashCode = hashCode
                return hashCode
            }

            override fun equals(other: Any?): Boolean {
                if (other !is Point) return false
                return x == other.x && y == other.y && z == other.z &&
                        other.u == u && other.v == v &&
                        other.color0 == this.color0 &&
                        other.materialIndex == materialIndex &&
                        other.b0 == b0 && other.b1 == b1 && other.b2 == b2 && other.b3 == b3 &&
                        other.w0 == w0 && other.w1 == w1 && other.w2 == w2 && other.w3 == w3
            }
        }

        // generate all points
        val points = createArrayList(positions.size / 3) {
            val i3 = it * 3
            Point(positions[i3], positions[i3 + 1], positions[i3 + 2])
        }

        if (uvs != null) {
            for (i in 0 until Maths.min(uvs.size shr 1, points.size)) {
                val i2 = i + i
                val p = points[i]
                p.u = uvs[i2]
                p.v = uvs[i2 + 1]
            }
        }

        if (color0 != null) {
            for (i in 0 until Maths.min(color0.size, points.size)) {
                points[i].materialIndex = color0[i]
            }
        }

        if (materialIndices != null) {
            for (i in 0 until Maths.min(materialIndices.size, points.size)) {
                points[i].materialIndex = materialIndices[i]
            }
        }

        if (boneWeights != null && boneIndices != null) {
            for (i in 0 until Maths.min(Maths.min(boneWeights.size, boneIndices.size) shr 2, points.size)) {
                val i4 = i shl 2
                val p = points[i]
                p.b0 = boneIndices[i4]
                p.b1 = boneIndices[i4 + 1]
                p.b2 = boneIndices[i4 + 2]
                p.b3 = boneIndices[i4 + 3]
                p.w0 = boneWeights[i4]
                p.w1 = boneWeights[i4 + 1]
                p.w2 = boneWeights[i4 + 2]
                p.w3 = boneWeights[i4 + 3]
            }
        }

        val uniquePoints = HashMap<Point, Int>()
        for (i in points.lastIndex downTo 0) {
            uniquePoints[points[i]] = i
        }

        LOGGER.debug("Merged {} into {} points", points.size, uniquePoints.size)

        return IntArray(points.size) {
            uniquePoints[points[it]]!!
        }
    }

    /**
     * @param mesh the mesh, where the normals shall be recalculated; indices should be null, but it might work anyway
     * @param maxAllowedAngle maximum allowed angle for smoothing, in radians, typically 15°-45°
     * @param largeLength all triangles, which have a larger perimeter than largeLength, will be ignored
     * @param normalScale internal parameter for a deviation; shall be ~MinVertexDistance/3, or if unknown, ~MeshScale/100; must not be too small
     * */
    fun calculateSmoothNormals(mesh: Mesh, maxAllowedAngle: Float, largeLength: Float, normalScale: Float) {
        mesh.getBounds()
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
    fun calculateSmoothNormals(
        mesh: Mesh,
        normalScale: Float,
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
                normal.normalize(normalScale)
                add(map, min, max, a, normal, maxD)
                add(map, min, max, b, normal, maxD)
                add(map, min, max, c, normal, maxD)
            }
        }
        val positions = mesh.positions!!
        val normals = mesh.normals.resize(positions.size)
        mesh.normals = normals
        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        var i = 0
        val limit = positions.size - 8
        val tmp = Vector3f()
        while (i < limit) {
            a.set(positions[i++], positions[i++], positions[i++])
            b.set(positions[i++], positions[i++], positions[i++])
            c.set(positions[i++], positions[i++], positions[i++])
            Triangles.subCross(a, b, c, normal)
            val maxC = getTriangleSmoothness(a, b, c)
            if (maxC > 0f) {
                val maxD = maxC * normalScale
                normal.normalize(normalScale)
                // query normal at each point
                get(map, min, max, a.add(normal), maxD, tmp).get(normals, i - 9)
                get(map, min, max, b.add(normal), maxD, tmp).get(normals, i - 6)
                get(map, min, max, c.add(normal), maxD, tmp).get(normals, i - 3)
            } else {
                // disable smoothing on large triangles
                normal.normalize()
                normal.get(normals, i - 9)
                normal.get(normals, i - 6)
                normal.get(normals, i - 3)
            }
        }
    }

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

    fun Mesh.makeFlatShaded(calculateNormals: Boolean = true) {
        val indices = indices
        val positions = positions ?: return
        val colors = color0
        if (indices == null) {
            if (calculateNormals) calculateNormals(false)
        } else {
            val newPositions = FloatArray(indices.size * 3)
            val newColors = if (colors != null) IntArray(indices.size) else null
            for (i in indices.indices) {
                val i3 = i * 3
                val j = indices[i]
                val j3 = j * 3
                newPositions[i3] = positions[j3]
                newPositions[i3 + 1] = positions[j3 + 1]
                newPositions[i3 + 2] = positions[j3 + 2]
                if (colors != null) newColors!![i] = colors[j]
            }
            this.positions = newPositions
            this.normals = normals.resize(newPositions.size)
            this.color0 = newColors
            this.indices = null
            if (calculateNormals) calculateNormals(false)
        }
    }

}