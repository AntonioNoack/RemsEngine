package me.anno.maths.bvh

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.raycast.RayHit
import me.anno.gpu.pipeline.M4x3Delta.set4x3delta
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.utils.Clock
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.partition1
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.avgZ
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import me.anno.utils.types.AABBs.transformSet
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// todo visualize a bvh structure in-engine
// todo for sun-shadow checks, we can skip everything once we found something
/**
 * creates a bounding volume hierarchy for triangle meshes
 * */
abstract class BVHBuilder(val bounds: AABBf) {

    // https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
    var nodeId = 0

    abstract fun print(depth: Int = 0)

    abstract fun countNodes(): Int

    abstract fun maxDepth(): Int

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
        return max(tMin, 0f) <= min(tMax, maxDistance)
    }

    companion object {

        // private val LOGGER = LogManager.getLogger(BVHBuilder::class)

        // done build whole scene into TLAS+BLAS and then render it correctly
        // done how do we manage multiple meshes? connected buffer is probably the best...

        // todo can we somehow use an array of pointers in OpenGL, so we can use many different texture sizes??? would really help...
        // we could reduce the number of materials, and draw the materials sequentially with separate TLASes...

        val blasCache = HashMap<Mesh, BLASNode?>()

        fun buildTLAS(
            scene: PipelineStage, // filled with meshes
            cameraPosition: Vector3d, worldScale: Double, splitMethod: SplitMethod, maxNodeSize: Int
        ): TLASNode {
            val clock = Clock()
            val objects = ArrayList<TLASLeaf>(scene.size)
            // add non-instanced objects
            val dr = scene.drawRequests
            fun add(mesh: Mesh, blas: BLASNode, transform: Transform) {
                val drawMatrix = transform.getDrawMatrix()
                val localToWorld = Matrix4x3f().set4x3delta(drawMatrix, cameraPosition, worldScale)
                val worldToLocal = Matrix4x3f()
                localToWorld.invert(worldToLocal)
                val centroid = Vector3f()
                val localBounds = mesh.aabb
                centroid.set(localBounds.avgX(), localBounds.avgY(), localBounds.avgZ())
                localToWorld.transformPosition(centroid)
                val globalBounds = AABBf()
                localBounds.transformSet(localToWorld, globalBounds)
                objects.add(TLASLeaf(centroid, localToWorld, worldToLocal, blas, globalBounds))
            }
            for (index in dr.indices) {
                val dri = dr[index]
                // to do theoretically, we'd need to respect material override as well,
                // but idk how to do materials yet...
                val mesh = dri.mesh
                val blas = blasCache.getOrPut(mesh) {
                    buildBLAS(mesh, splitMethod, maxNodeSize)
                } ?: continue
                val entity = dri.entity
                val transform = entity.transform
                add(mesh, blas, transform)
            }
            // add all instanced objects
            scene.instancedMeshes1.forEach { mesh, matWithId, stack ->
                val blas = blasCache.getOrPut(mesh) {
                    buildBLAS(mesh, splitMethod, maxNodeSize)
                }
                if (blas != null) {
                    for (i in 0 until stack.size) {
                        val transform = stack.transforms[i]!!
                        add(mesh, blas, transform)
                    }
                }
            }
            scene.instancedMeshes2.forEach { mesh, material, stack ->
                val blas = blasCache.getOrPut(mesh) {
                    buildBLAS(mesh, splitMethod, maxNodeSize)
                }
                if (blas != null) {
                    for (i in 0 until stack.size) {
                        val transform = stack.transforms[i]!!
                        add(mesh, blas, transform)
                    }
                }
            }
            clock.stop("Creating BLASes")
            val tlas = recursiveBuildTLAS(objects, 0, objects.size, splitMethod)
            clock.stop("Creating TLAS")
            return tlas
        }

        fun buildBLAS(mesh: Mesh, splitMethod: SplitMethod, maxNodeSize: Int): BLASNode? {
            val positions = mesh.positions ?: return null
            val indices = mesh.indices ?: IntArray(positions.size / 3) { it }
            val numTriangles = indices.size / 3
            val dstPos = FloatArray(numTriangles * 9)
            val root = recursiveBuildBLAS(positions, indices, 0, numTriangles, maxNodeSize, splitMethod, dstPos)
            var i3 = 0
            for (i in 0 until numTriangles * 3) {
                var j3 = indices[i] * 3
                dstPos[i3++] = positions[j3++]
                dstPos[i3++] = positions[j3++]
                dstPos[i3++] = positions[j3]
            }
            if (dstPos.size != i3) throw IllegalStateException()
            return root
        }

        private fun recursiveBuildTLAS(
            objects: ArrayList<TLASLeaf>, start: Int, end: Int, // triangle indices
            splitMethod: SplitMethod
        ): TLASNode {
            val count = end - start
            if (count <= 1) {
                // leaf was already created by parent buildTLAS()
                return objects[start]
            } else {

                // bounds of center of primitives for efficient split dimension
                val centroidBounds = AABBf()
                for (index in start until end) {
                    centroidBounds.union(objects[index].centroid)
                }

                // split dimension
                val dim = centroidBounds.maxDim()

                // partition primitives into two sets & build children
                var mid = (start + end) / 2
                if (centroidBounds.getMax(dim) == centroidBounds.getMin(dim)) {
                    // creating a leaf node here would be illegal, because maxNodesPerPoint would be violated
                    // nodes must be split randomly -> just skip all splitting computations
                } else {
                    // partition based on split method
                    // for the very start, we'll only implement the simplest methods
                    when (splitMethod) {
                        SplitMethod.MIDDLE -> {
                            val midF = (centroidBounds.getMin(dim) + centroidBounds.getMax(dim)) * 0.5f
                            mid = objects.partition1(start, end) { t ->
                                t.centroid[dim] < midF
                            }
                            if (mid == start || mid >= end - 1) {// middle didn't work -> use more elaborate scheme
                                // mid = (start + end) / 2
                                mid = objects.median(start, end) { t0, t1 ->
                                    t0.centroid[dim].compareTo(t1.centroid[dim])
                                }
                            }
                        }
                        SplitMethod.MEDIAN -> {
                            mid = objects.median(start, end) { t0, t1 ->
                                t0.centroid[dim].compareTo(t1.centroid[dim])
                            }
                        }
                        SplitMethod.SURFACE_AREA_HEURISTIC -> throw NotImplementedError()
                        SplitMethod.HIERARCHICAL_LINEAR -> throw NotImplementedError()
                    }
                }

                val n0 = recursiveBuildTLAS(objects, start, mid, splitMethod)
                val n1 = recursiveBuildTLAS(objects, mid, end, splitMethod)

                val bounds = AABBf(n0.bounds)
                bounds.union(n1.bounds)

                return TLASBranch(dim, n0, n1, bounds)
            }

        }

        private fun recursiveBuildBLAS(
            positions: FloatArray,
            indices: IntArray,
            start: Int, end: Int, // triangle indices
            maxNodeSize: Int,
            splitMethod: SplitMethod,
            newPositions: FloatArray,
        ): BLASNode {

            val count = end - start
            if (end - start <= maxNodeSize) { // create leaf
                val bounds = AABBf()
                for (i in start * 3 until end * 3) {
                    val ci = indices[i] * 3
                    bounds.union(positions[ci], positions[ci + 1], positions[ci + 2])
                }
                return BLASLeaf(start, count, newPositions, bounds)
            }

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
                            mid = (start + end) / 2
                            median(positions, indices, start, end) { a0, b0, c0, a1, b1, c1 ->
                                (a0[dim] + b0[dim] + c0[dim]).compareTo(a1[dim] + b1[dim] + c1[dim])
                            }
                        }
                    }
                    SplitMethod.MEDIAN -> {
                        // if (start == 0 && end == indices.size / 3) debug(positions, indices, start, end)
                        median(positions, indices, start, end) { a0, b0, c0, a1, b1, c1 ->
                            (a0[dim] + b0[dim] + c0[dim]).compareTo(a1[dim] + b1[dim] + c1[dim])
                        }
                        //debug(positions, indices, start, mid)
                        //debug(positions, indices, mid, end)
                    }
                    SplitMethod.SURFACE_AREA_HEURISTIC -> throw NotImplementedError()
                    SplitMethod.HIERARCHICAL_LINEAR -> throw NotImplementedError()
                }
            }

            val n0 = recursiveBuildBLAS(positions, indices, start, mid, maxNodeSize, splitMethod, newPositions)
            val n1 = recursiveBuildBLAS(positions, indices, mid, end, maxNodeSize, splitMethod, newPositions)

            val bounds = AABBf(n0.bounds)
            bounds.union(n1.bounds)

            return BLASBranch(dim, n0, n1, bounds)
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

        fun <V> ArrayList<V>.median(
            start: Int, end: Int,
            condition: (t0: V, t1: V) -> Int
        ): Int {
            // not optimal performance, but at least it will 100% work
            subList(start, end).sortWith(condition)
            return (start + end) ushr 1
            // new, on avg O(n) way, based on nth_element:
            // ... has the same performance ...
            /*val random = Random() // to do only create a single random instance per tlas
            // we could reduce this +/- 1 accuracy if there are e.g. max 16 nodes/element
            val avg = (start + end) / 2
            val mid = median2(start, end, avg, random, condition)
            return clamp(partition1(start, end) {
                condition(it, mid) < 0
            }, start + 1, end - 2)*/
        }

        /*fun <V> ArrayList<V>.median2(
            start: Int,
            end: Int,
            avg: Int,
            random: Random,
            condition: (t0: V, t1: V) -> Int
        ): V {
            val randomElement = this[start + random.nextInt(end - start)]
            val mid = partition1(start, end) { a ->
                condition(a, randomElement) > 0
            }
            return if (mid < avg) {
                // partition mid ... end
                median2(mid + 1, end, avg, random, condition)
            } else if (mid > avg) {
                // partition start .. mid
                median2(start, mid, avg, random, condition)
            } else randomElement// else done :)
        }*/

        fun median(
            positions: FloatArray,
            indices: IntArray,
            start: Int,
            end: Int,
            condition: (a0: Vector3f, b0: Vector3f, c0: Vector3f, a1: Vector3f, b1: Vector3f, c1: Vector3f) -> Int
        ) {
            // not optimal performance, but at least it will 100% work
            val count = end - start
            val solution = Array(count) { start + it }
            /*val sol2 = ArrayList(solution.toList())
            sol2.median2(0, count, count / 2, Random()) { a, b ->
                comp(positions, indices, a, b, condition)
            }
            for (i in solution.indices) {
                solution[i] = sol2[i]
            }*/
            solution.sortWith { a, b ->
                comp(positions, indices, a, b, condition)
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
            positions: FloatArray,
            indices: IntArray,
            i: Int,
            j: Int,
            condition: (a0: Vector3f, b0: Vector3f, c0: Vector3f, a1: Vector3f, b1: Vector3f, c1: Vector3f) -> Int
        ): Int {
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
            positions: FloatArray,
            indices: IntArray,
            start: Int,
            end: Int,
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
                if (i < j) {
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

    }

}