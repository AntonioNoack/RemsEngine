package me.anno.gpu.buffer

import me.anno.maths.Packing.pack64
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.structures.arrays.IntArrayList
import speiger.primitivecollections.LongToIntHashMap

object TriangleIndices {
    /**
     * Converts a triangle list into a triangle strip list
     * */
    fun trianglesToTriangleStrip(indices: IntArrayList): IntArrayList {
        return trianglesToTriangleStrip(indices.values, indices.size)
    }

    /**
     * Converts a triangle list into a triangle strip list
     * (for more efficient GPU rendering)
     * */
    fun trianglesToTriangleStrip(indices: IntArray, indicesSize: Int = indices.size): IntArrayList {
        // create a lookup from (oriented) edges to triangles
        val edgeToTriangle = LongToIntHashMap(-1, indicesSize)
        forLoopSafely(indicesSize, 3) { i ->
            val ai = indices[i + 1]
            val bi = indices[i]
            val ci = indices[i + 2]
            check(edgeToTriangle.put(pack64(ai, bi), i) == -1)
            check(edgeToTriangle.put(pack64(bi, ci), i) == -1)
            check(edgeToTriangle.put(pack64(ci, ai), i) == -1)
        }

        val result = IntArrayList(indices.size)

        var flip = false
        while (edgeToTriangle.isNotEmpty()) {
            val edge = edgeToTriangle.firstKey(-1)
            val i = edgeToTriangle[edge]

            var ai = indices[i + 1]
            var bi = indices[i]
            var ci = indices[i + 2]
            removeTriangle(edgeToTriangle, ai, bi, ci)

            // find the preferred side...
            if (!edgeToTriangle.containsKey(pack64(ci, bi))) {
                val ai0 = ai // rotate to next side
                ai = bi
                bi = ci
                ci = ai0

                if (!edgeToTriangle.containsKey(pack64(ci, bi))) {
                    val ai0 = ai // rotate to next side
                    ai = bi
                    bi = ci
                    ci = ai0
                }
            }

            // terminate triangles by adding degenerate triangles
            if (result.isNotEmpty()) {
                result.add(result.last())
                result.add(ai)
                // two triangles -> flip stays the same
                // if flipped, fix it by adding another degenerate triangle:
                if (!flip) {
                    result.add(ai)
                    flip = true
                }
            }

            result.add(ai)
            result.add(bi)
            result.add(ci)
            flip = !flip // odd number -> flip

            // (ci,bi) is the best possible edge, at least for the next triangle...
            while (true) {
                val i = edgeToTriangle.remove(
                    if (flip) pack64(bi, ci)
                    else pack64(ci, bi)
                )
                if (i < 0) break

                // load the next triangle in the strip
                val ai2 = indices[i + 1]
                val bi2 = indices[i]
                val ci2 = indices[i + 2]
                removeTriangle(edgeToTriangle, ai2, bi2, ci2)

                // find which vertex is not shared
                val notShared = when {
                    ai2 != bi && ai2 != ci -> ai2
                    bi2 != bi && bi2 != ci -> bi2
                    else -> ci2
                }

                // and then add that one to the list
                result.add(notShared)

                // and then update our last edge...
                bi = ci
                ci = notShared
                flip = !flip
            }
        }
        return result
    }

    /**
     * remove our triangle, so we can terminate
     * */
    private fun removeTriangle(edgeToTriangle: LongToIntHashMap, ai: Int, bi: Int, ci: Int) {
        edgeToTriangle.remove(pack64(ai, bi))
        edgeToTriangle.remove(pack64(bi, ci))
        edgeToTriangle.remove(pack64(ci, ai))
    }
}