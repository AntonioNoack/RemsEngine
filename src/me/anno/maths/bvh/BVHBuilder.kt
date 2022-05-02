package me.anno.maths.bvh

import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.raycast.RayHit
import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBf
import org.joml.Vector3f
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// todo visualize a bvh structure in-engine
// todo top layer acceleration structure, for now we only have the bottom layer acceleration structure
/**
 * creates a bounding volume hierarchy for triangle meshes
 * */
abstract class BVHBuilder(
    val bounds: AABBf
) {

    // https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp

    var index = 0

    abstract fun print(depth: Int = 0)

    abstract fun countNodes(): Int

    abstract fun maxDepth(): Int

    abstract fun forEach(run: (BVHBuilder) -> Unit)

    fun Vector3f.dirIsNeg() = (x < 0f).toInt() + (y < 0f).toInt(2) + (z < 0f).toInt(4)

    fun intersect(pos: Vector3f, dir: Vector3f, hit: RayHit) {
        val invDir = JomlPools.vec3f.create().set(1f).div(dir)
        val dirIsNeg = dir.dirIsNeg()
        intersect(pos, dir, invDir, dirIsNeg, hit)
        JomlPools.vec3f.sub(1)
    }

    abstract fun intersect(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit)

    private fun max(a: Float, b: Float, c: Float): Float {
        return max(max(a, b), c)
    }

    private fun min(a: Float, b: Float, c: Float): Float {
        return min(min(a, b), c)
    }

    fun intersectBounds(pos: Vector3f, invDir: Vector3f, dirIsNeg: Int, maxDistance: Float): Boolean {
        val b = bounds
        val xNeg = dirIsNeg.and(1) != 0
        val yNeg = dirIsNeg.and(2) != 0
        val zNeg = dirIsNeg.and(4) != 0
        val cx = if (xNeg) b.maxX else b.minX
        val cy = if (yNeg) b.maxY else b.minY
        val cz = if (zNeg) b.maxZ else b.minZ
        val fx = if (xNeg) b.minX else b.maxX
        val fy = if (yNeg) b.minY else b.maxY
        val fz = if (zNeg) b.minZ else b.maxZ
        val tMin = max((cx - pos.x) * invDir.x, (cy - pos.y) * invDir.y, (cz - pos.z) * invDir.z)
        val tMax = min((fx - pos.x) * invDir.x, (fy - pos.y) * invDir.y, (fz - pos.z) * invDir.z)
        return max(tMin, 0f) < min(tMax, maxDistance)
    }

    abstract fun findCompactPositions(): FloatArray?

    companion object {

        val good = AtomicInteger(0)
        val bad = AtomicInteger(0)

        // todo build whole scene into TLAS+BLAS and then render it correctly
        // todo how do we manage multiple meshes? connected buffer is probably the best...

        fun build(mesh: Mesh, splitMethod: SplitMethod, maxNodeSize: Int, withIndirections: Boolean): BVHBuilder? {
            val positions = mesh.positions ?: return null
            val indices = mesh.indices ?: IntArray(positions.size / 3) { it }
            val numTriangles = indices.size / 3
            val dstPos = if (withIndirections) null else FloatArray(numTriangles * 9)
            val root = recursiveBuild(positions, indices, 0, numTriangles, maxNodeSize, splitMethod, dstPos)
            if (dstPos != null) {
                var i3 = 0
                for (i in 0 until numTriangles * 3) {
                    var j3 = indices[i] * 3
                    dstPos[i3++] = positions[j3++]
                    dstPos[i3++] = positions[j3++]
                    dstPos[i3++] = positions[j3]
                }
            }
            return root
        }

        private fun recursiveBuild(
            positions: FloatArray,
            indices: IntArray,
            start: Int, end: Int, // triangle indices
            maxNodeSize: Int,
            splitMethod: SplitMethod,
            dstPos: FloatArray?,
        ): BLASNode {

            val bounds = AABBf()
            for (i in start * 3 until end * 3) {
                val ci = indices[i] * 3
                bounds.union(positions[ci], positions[ci + 1], positions[ci + 2])
            }

            val count = end - start
            if (count <= maxNodeSize) {

                // create leaf
                return if (dstPos == null)
                    BLASLeaf(start, count, bounds, positions, indices)
                else
                    BLASLeaf(start, count, bounds, dstPos, null)

            } else {

                // bounds of center of primitives for efficient split dimension
                val centroidBoundsX3 = AABBf()
                for (triIndex in start until end) {
                    val pointIndex = triIndex * 3
                    var ai = indices[pointIndex] * 3
                    var bi = indices[pointIndex + 1] * 3
                    var ci = indices[pointIndex + 2] * 3
                    centroidBoundsX3.union(
                        positions[ai++] + positions[bi++] + positions[ci++],
                        positions[ai++] + positions[bi++] + positions[ci++],
                        positions[ai] + positions[bi] + positions[ci]
                    )
                }

                // split dimension
                val dim = centroidBoundsX3.maxDim()
                // println("centroid ${centroidBounds.deltaX()}, ${centroidBounds.deltaY()}, ${centroidBounds.deltaZ()} -> $dim")

                // partition primitives into two sets & build children
                var mid = (start + end) / 2
                if (centroidBoundsX3.getMax(dim) == centroidBoundsX3.getMin(dim)) {
                    // creating a leaf node here would be illegal, because maxNodesPerPoint would be violated
                    // nodes must be split randomly -> just skip all splitting computations
                } else {
                    // partition based on split method
                    // for the very start, we'll only implement the simplest methods
                    when (splitMethod) {
                        SplitMethod.MIDDLE -> {
                            val midF = (centroidBoundsX3.getMin(dim) + centroidBoundsX3.getMax(dim)) * 0.5f
                            mid = partition(positions, indices, start, end) { a, b, c ->
                                a[dim] + b[dim] + c[dim] < midF
                            }
                            if (mid == start || mid >= end - 1) {// middle didn't work -> use more elaborate scheme
                                median(positions, indices, start, end) { a0, b0, c0, a1, b1, c1 ->
                                    a0[dim] + b0[dim] + c0[dim] < a1[dim] + b1[dim] + c1[dim]
                                }
                            }
                        }
                        SplitMethod.MEDIAN -> {
                            // if (start == 0 && end == indices.size / 3) debug(positions, indices, start, end)
                            median(positions, indices, start, end) { a0, b0, c0, a1, b1, c1 ->
                                a0[dim] + b0[dim] + c0[dim] < a1[dim] + b1[dim] + c1[dim]
                            }
                            //debug(positions, indices, start, mid)
                            //debug(positions, indices, mid, end)
                        }
                        SplitMethod.SURFACE_AREA_HEURISTIC -> TODO()
                        SplitMethod.HIERARCHICAL_LINEAR -> TODO()
                    }
                }
                val n0 = recursiveBuild(positions, indices, start, mid, maxNodeSize, splitMethod, dstPos)
                val n1 = recursiveBuild(positions, indices, mid, end, maxNodeSize, splitMethod, dstPos)

                return BLASBranch(dim, n0, n1, bounds)
            }

        }

        /*var fileId = 0
        fun debug(pos: FloatArray, indices: IntArray, start: Int, end: Int) {
            val file = desktop.getChild("bvh/${fileId++}.obj")
            val builder = StringBuilder()
            fun point(a3: Int) {
                builder.append("v ").append(pos[a3]).append(' ')
                    .append(pos[a3 + 1]).append(' ')
                    .append(pos[a3 + 2]).append('\n')
            }
            builder.append("o x$fileId\n")
            for (i in start until end) {
                val i3 = i * 3
                point(indices[i3] * 3)
                point(indices[i3 + 1] * 3)
                point(indices[i3 + 2] * 3)
            }
            fun face(i: Int) {
                builder.append("f ").append(i).append(' ')
                    .append(i + 1).append(' ')
                    .append(i + 2).append('\n')
            }
            for (i in 0 until end - start) {
                face(i * 3 + 1)
            }
            file.writeText(builder.toString())
        }*/

        fun median(
            positions: FloatArray,
            indices: IntArray,
            start: Int,
            end: Int,
            condition: (a0: Vector3f, b0: Vector3f, c0: Vector3f, a1: Vector3f, b1: Vector3f, c1: Vector3f) -> Boolean
        ) {
            // not optimal performance, but at least it will 100% work
            val count = end - start
            val solution = Array(count) { start + it }
            solution.sortWith { a, b ->
                when {
                    a == b -> 0
                    comp(positions, indices, a, b, condition) -> +1
                    else -> -1
                }
            }
            val c3 = count * 3
            val s3 = start * 3
            val indexBackup = IntArray(c3)
            System.arraycopy(indices, s3, indexBackup, 0, indexBackup.size)
            var dst3 = s3
            for (i in 0 until count) {
                val m = solution[i]
                // move triangle from i to m
                var src3 = m * 3 - s3
                indices[dst3++] = indexBackup[src3++]
                indices[dst3++] = indexBackup[src3++]
                indices[dst3++] = indexBackup[src3]
            }
        }

        private fun comp(
            positions: FloatArray, indices: IntArray, i: Int, j: Int,
            condition: (a0: Vector3f, b0: Vector3f, c0: Vector3f, a1: Vector3f, b1: Vector3f, c1: Vector3f) -> Boolean
        ): Boolean {
            val a0 = JomlPools.vec3f.create()
            val b0 = JomlPools.vec3f.create()
            val c0 = JomlPools.vec3f.create()
            val a1 = JomlPools.vec3f.create()
            val b1 = JomlPools.vec3f.create()
            val c1 = JomlPools.vec3f.create()
            val i3 = i * 3
            a0.set(positions, indices[i3] * 3)
            b0.set(positions, indices[i3 + 1] * 3)
            c0.set(positions, indices[i3 + 2] * 3)
            val j3 = j * 3
            a1.set(positions, indices[j3] * 3)
            b1.set(positions, indices[j3 + 1] * 3)
            c1.set(positions, indices[j3 + 2] * 3)
            val r = condition(a0, b0, c0, a1, b1, c1)
            JomlPools.vec3f.sub(6)
            return r
        }

        private fun partition(
            positions: FloatArray, indices: IntArray, start: Int, end: Int,
            condition: (Vector3f, Vector3f, Vector3f) -> Boolean
        ): Int {

            var i = start
            var j = (end - 1)

            while (i < j) {
                // while front is fine, progress front
                while (i < j && cond(positions, indices, i, condition)) i++
                // while back is fine, progress back
                while (i < j && !cond(positions, indices, j, condition)) j--
                // if nothing works, swap i and j
                if (i < j) swap(indices, i, j)
            }

            return i

        }

        private fun cond(
            positions: FloatArray, indices: IntArray, i: Int, condition: (Vector3f, Vector3f, Vector3f) -> Boolean
        ): Boolean {
            val a = JomlPools.vec3f.create()
            val b = JomlPools.vec3f.create()
            val c = JomlPools.vec3f.create()
            val i3 = i * 3
            a.set(positions, indices[i3] * 3)
            b.set(positions, indices[i3 + 1] * 3)
            c.set(positions, indices[i3 + 2] * 3)
            val r = condition(a, b, c)
            JomlPools.vec3f.sub(3)
            return r
        }

        private fun Vector3f.set(positions: FloatArray, ai: Int) {
            set(positions[ai], positions[ai + 1], positions[ai + 2])
        }

        private fun swap(indices: IntArray, i: Int, j: Int) {
            var i3 = i * 3
            var j3 = j * 3
            for (k in 0 until 3) {
                val t = indices[i3]
                indices[i3] = indices[j3]
                indices[j3] = t
                i3++
                j3++
            }
        }

        private fun AABBf.maxDim(): Int {
            val dx = deltaX()
            val dy = deltaY()
            val dz = deltaZ()
            return when {
                dx >= max(dy, dz) -> 0
                dy >= dz -> 1
                else -> 2
            }
        }

        fun createTexture(name: String, numElements: Int, pixelsPerElement: Int): Texture2D {
            val requiredPixels = numElements * pixelsPerElement
            val textureWidth = Maths.align(sqrt(requiredPixels.toFloat()).toInt(), pixelsPerElement)
            val textureHeight = Maths.ceilDiv(requiredPixels, textureWidth)
            return Texture2D(name, textureWidth, textureHeight, 1)
        }

        fun createTriangleTexture(bvh: BVHBuilder): Texture2D {
            GFX.checkIsGFXThread()
            val positions = bvh.findCompactPositions()!! // positions without index
            val pixelsPerTriangle = 3 // 9 floats -> 3 pixels with RGB or RGBA are needed
            // RGB is not supported by compute shaders (why ever...), so use RGBA
            val numTriangles = positions.size / 9
            val texture = createTexture("triangles", numTriangles, pixelsPerTriangle)
            val data = FloatArray(texture.w * texture.h * 4)
            // write triangle into memory
            var j = 0
            var k = 0
            for (i in 0 until numTriangles * 3) {
                data[j++] = positions[k++]
                data[j++] = positions[k++]
                data[j++] = positions[k++]
                data[j++] = 0f // padding
            }
            texture.createRGBA(data, false)
            return texture
        }

        fun createNodeTexture(bvh: BVHBuilder): Texture2D {
            GFX.checkIsGFXThread()
            // root node
            // aabb = 6x fp32
            // child0 can directly follow
            // child1 needs offset; 1x int32
            // leaf node
            // aabb = 6x fp32
            // start, length = 2x int32
            // for both types just use 8x4 = 32 bytes
            // we will find a place for markers about the type :)
            val pixelsPerNode = 32 / 4
            val numNodes = bvh.countNodes()
            val texture = createTexture("nodes", numNodes, pixelsPerNode)
            val data = FloatArray(texture.w * texture.h * 4)
            var i = 0
            // assign indices to all nodes
            bvh.forEach {
                it.index = i++
            }
            i = 0
            bvh.forEach {

                val v0: Int
                val v1: Int
                when (it) {
                    is BLASBranch -> {
                        v0 = it.n1.index - it.index // next node
                        v1 = 0  // not a leaf
                    }
                    is BLASLeaf -> {
                        v0 = it.start
                        v1 = it.length
                    }
                    else -> throw RuntimeException()
                }

                val bounds = it.bounds
                data[i++] = bounds.minX
                data[i++] = bounds.minY
                data[i++] = bounds.minZ
                data[i++] = Float.fromBits(v0)

                data[i++] = bounds.maxX
                data[i++] = bounds.maxY
                data[i++] = bounds.maxZ
                data[i++] = Float.fromBits(v1)

            }
            texture.createRGBA(data, false)
            return texture
        }

    }

}