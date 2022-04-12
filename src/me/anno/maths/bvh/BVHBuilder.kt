package me.anno.maths.bvh

import me.anno.ecs.components.mesh.Mesh
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import org.joml.AABBf
import org.joml.Vector3f
import kotlin.math.max


// todo visualize a bvh structure
object BVHBuilder {

    class Leaf(val start: Int, val length: Int, val bounds: AABBf)
    class Node(val dim: Int, val n0: Any, val n1: Any)

    enum class SplitMethod {
        MIDDLE,
        MEDIAN,
        SURFACE_AREA_HEURISTIC,
        HIERARCHICAL_LINEAR // https://research.nvidia.com/sites/default/files/pubs/2011-08_Simpler-and-Faster/main.pdf
    }

    // https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp

    fun build(mesh: Mesh, splitMethod: SplitMethod): Unit? {

        val positions = mesh.positions ?: return null
        val indices = mesh.indices ?: IntArray(positions.size / 3) { it }

        recursiveBuild(positions, indices, 0, indices.size, splitMethod, IntArrayList(256))

        return Unit

    }

    fun recursiveBuild(
        positions: FloatArray,
        indices: IntArray,
        start: Int, end: Int, // triangle indices
        splitMethod: SplitMethod,
        dstList: IntArrayList
    ): Any {

        val bounds = AABBf()
        for (i in start * 3 until end * 3) {
            val ci = indices[i] * 3
            bounds.union(positions[ci], positions[ci + 1], positions[ci + 2])
        }

        val count = end - start
        if (count == 1) {// todo we could use a minimum node size here

            // create leaf
            dstList.add(start)
            return Leaf(dstList.size - 1, 1, bounds)

        } else {

            // bounds of center of primitives for efficient split dimension
            val centroidBounds = AABBf()
            for (triIndex in start until end) {
                val pointIndex = triIndex * 3
                val ai = indices[pointIndex]
                val bi = indices[pointIndex + 1]
                val ci = indices[pointIndex + 2]
                centroidBounds.union(
                    positions[ai] + positions[bi] + positions[ci],
                    positions[ai + 1] + positions[bi + 1] + positions[ci + 1],
                    positions[ai + 2] + positions[bi + 2] + positions[ci + 2]
                )
            }

            // maybe not required, if only dim is needed
            centroidBounds.scale(1f / 3f)

            // split dimension
            val dim = centroidBounds.maxDim()

            // partition primitives into two sets & build children
            var mid = (start + end) / 2
            if (centroidBounds.getMax(dim) == centroidBounds.getMin(dim)) {
                // all triangles are on a single point
                val start1 = dstList.size
                for (i in start until end) {
                    dstList.add(i)
                }
                return Leaf(start1, count, bounds)
            } else {
                // partition based on split method
                // for the very start, we'll only implement the simplest methods
                when (splitMethod) {
                    SplitMethod.MIDDLE -> {
                        // *3 so we can avoid a multiplication
                        val midF = centroidBounds.getMin(dim) + centroidBounds.getMax(dim) * 3f
                        mid = partition(positions, indices, start, end) { a, b, c ->
                            a[dim] + b[dim] + c[dim] < midF
                        }
                        if (mid == start || mid == end) {// middle didn't work -> use more elaborate scheme
                            median(positions, indices, start, mid, end) { a0, b0, c0, a1, b1, c1 ->
                                a0[dim] + b0[dim] + c0[dim] < a1[dim] + b1[dim] + c1[dim]
                            }
                        }
                    }
                    SplitMethod.MEDIAN -> {
                        median(positions, indices, start, mid, end) { a0, b0, c0, a1, b1, c1 ->
                            a0[dim] + b0[dim] + c0[dim] < a1[dim] + b1[dim] + c1[dim]
                        }
                    }
                    SplitMethod.SURFACE_AREA_HEURISTIC -> TODO()
                    SplitMethod.HIERARCHICAL_LINEAR -> TODO()
                }
                return Node(
                    dim,
                    recursiveBuild(positions, indices, start, mid, splitMethod, dstList),
                    recursiveBuild(positions, indices, mid, end, splitMethod, dstList)
                )
            }
        }
    }

    fun median(
        positions: FloatArray, indices: IntArray, start: Int, mid: Int, end: Int,
        condition: (a0: Vector3f, b0: Vector3f, c0: Vector3f, a1: Vector3f, b1: Vector3f, c1: Vector3f) -> Boolean
    ) {
        // not optimal performance, but at least it will 100% work
        val solution = Array(end - start) { start + it }
        solution.sortWith { a, b ->
            when {
                a == b -> 0
                comp(positions, indices, a, b, condition) -> +1
                else -> -1
            }
        }
        for (i in start until end) {
            indices[i] = solution[i - start]
        }
    }

    fun comp(
        positions: FloatArray,
        indices: IntArray,
        i: Int,
        j: Int,
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

    fun partition(
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
            if (i < j) swap(indices, i, j)
        }

        return i

    }

    fun cond(
        positions: FloatArray,
        indices: IntArray,
        i: Int,
        condition: (Vector3f, Vector3f, Vector3f) -> Boolean
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

    fun Vector3f.set(positions: FloatArray, ai: Int) {
        set(positions[ai], positions[ai + 1], positions[ai + 2])
    }

    fun swap(indices: IntArray, i: Int, j: Int) {
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

    fun AABBf.scale(f: Float) {
        minX *= f
        minY *= f
        minZ *= f
        maxX *= f
        maxY *= f
        maxZ *= f
    }

    fun AABBf.maxDim(): Int {
        val dx = deltaX()
        val dy = deltaY()
        val dz = deltaZ()
        return when {
            dx >= max(dy, dz) -> 0
            dy >= dz -> 1
            else -> 2
        }
    }

}