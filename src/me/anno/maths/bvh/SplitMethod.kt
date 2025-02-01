package me.anno.maths.bvh

import me.anno.maths.Maths
import me.anno.utils.search.Median
import me.anno.utils.search.Median.median
import me.anno.utils.search.Partition
import me.anno.utils.structures.lists.Lists.partition1
import me.anno.utils.structures.lists.Lists.swap
import org.joml.AABBf

enum class SplitMethod {
    MIDDLE {
        override fun partitionTriangles(
            positions: FloatArray, indices: IntArray,
            start: Int, end: Int, dim: Int, pivot: Float
        ): Int {
            var mid = Partition.partition(start, end, { i ->
                getValue(positions, indices, dim, i * 3) < pivot
            }, TriangleSwapper(indices))
            if (mid == start || mid >= end - 1) {// middle didn't work -> use more elaborate scheme
                mid = MEDIAN_APPROX.partitionTriangles(positions, indices, start, end, dim, pivot)
            }
            return mid
        }

        override fun partitionTLASLeaves(
            objects: ArrayList<TLASLeaf>, start: Int, end: Int,
            dim: Int, pivot: Float
        ): Int {
            var mid = objects.partition1(start, end) { t -> t.centroid[dim] < pivot }
            if (mid == start || mid >= end - 1) {// middle didn't work -> use more elaborate scheme
                mid = MEDIAN_APPROX.partitionTLASLeaves(objects, start, end, dim, pivot)
            }
            return mid
        }
    },

    @Deprecated("Median Approx is much faster")
    MEDIAN {

        override fun partitionTriangles(
            positions: FloatArray, indices: IntArray,
            start: Int, end: Int, dim: Int, pivot: Float
        ): Int = median(start, end, TriangleSwapper(indices), indices::get, Comparator.comparing {
            getValue(positions, indices, dim, it * 3)
        })

        override fun partitionTLASLeaves(
            objects: ArrayList<TLASLeaf>, start: Int, end: Int,
            dim: Int, pivot: Float
        ): Int {
            objects.median(start, end) { t0, t1 ->
                t0.centroid[dim].compareTo(t1.centroid[dim])
            }
            return mid(start, end)
        }
    },

    /**
     * doesn't sort, uses statistical median; O(n) instead of O(n log n) -> 8x faster for Sponza
     * */
    MEDIAN_APPROX {
        override fun partitionTriangles(
            positions: FloatArray, indices: IntArray,
            start: Int, end: Int, dim: Int, pivot: Float
        ): Int {
            return Median.medianApprox(start, end, pivot.toDouble(), {
                val idx = it * 3
                val ai = indices[idx] * 3 + dim
                val bi = indices[idx + 1] * 3 + dim
                val ci = indices[idx + 2] * 3 + dim
                (positions[ai] + positions[bi] + positions[ci]).toDouble()
            }, TriangleSwapper(indices), Maths.getRandom())
        }

        override fun partitionTLASLeaves(
            objects: ArrayList<TLASLeaf>, start: Int, end: Int,
            dim: Int, pivot: Float
        ): Int {
            return Median.medianApprox(start, end, pivot.toDouble(), { i: Int ->
                val inst = objects[i]
                inst.centroid[dim].toDouble()
            }, { i, j ->
                objects.swap(i, j)
            }, Maths.getRandom())
        }
    },

    SURFACE_AREA_HEURISTIC {
        override fun partitionTriangles(
            positions: FloatArray, indices: IntArray,
            start: Int, end: Int, dim: Int, pivot: Float
        ): Int = throw NotImplementedError()

        override fun partitionTLASLeaves(
            objects: ArrayList<TLASLeaf>, start: Int, end: Int,
            dim: Int, pivot: Float
        ): Int = throw NotImplementedError()
    },

    HIERARCHICAL_LINEAR {
        // https://research.nvidia.com/sites/default/files/pubs/2011-08_Simpler-and-Faster/main.pdf
        override fun partitionTriangles(
            positions: FloatArray, indices: IntArray,
            start: Int, end: Int, dim: Int, pivot: Float
        ): Int = throw NotImplementedError()

        override fun partitionTLASLeaves(
            objects: ArrayList<TLASLeaf>, start: Int, end: Int,
            dim: Int, pivot: Float
        ): Int = throw NotImplementedError()
    };

    abstract fun partitionTriangles(
        positions: FloatArray, indices: IntArray,
        start: Int, end: Int, dim: Int, pivot: Float
    ): Int

    abstract fun partitionTLASLeaves(
        objects: ArrayList<TLASLeaf>, start: Int, end: Int,
        dim: Int, pivot: Float
    ): Int

    companion object {

        fun getValue(positions: FloatArray, indices: IntArray, dim: Int, i3: Int): Float {
            val a0 = positions[indices[i3] * 3 + dim]
            val b0 = positions[indices[i3 + 1] * 3 + dim]
            val c0 = positions[indices[i3 + 2] * 3 + dim]
            return a0 + b0 + c0
        }

        fun mid(start: Int, end: Int): Int = (start + end).ushr(1)

        fun pivot0(centroidBounds: AABBf, dim: Int): Float {
            return (centroidBounds.getMin(dim) + centroidBounds.getMax(dim)) * 0.5f
        }
    }
}