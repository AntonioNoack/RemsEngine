package me.anno.gpu.buffer

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Packing.pack64
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.callbacks.I3Z
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Booleans.hasFlag
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
            edgeToTriangle.put(pack64(ai, bi), i)
            edgeToTriangle.put(pack64(bi, ci), i)
            edgeToTriangle.put(pack64(ci, ai), i)
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

    /**
     * Removes all triangles, where the predicate returns false;
     * only supported for indexed meshes at the moment;
     * */
    fun Mesh.filterTriangles(filter: I3Z) {
        val indices = indices
        if (indices != null) {
            if (indices.size < 3) return
            val newIndices = when (drawMode) {
                DrawMode.TRIANGLES -> filterTrianglesIndexedSimple(indices, filter)
                DrawMode.TRIANGLE_STRIP -> filterStripTriangles(indices, filter)
                else -> return /* no triangles -> nothing to filter here */
            }
            this.indices = newIndices.toIntArray(true)
            invalidateGeometry()
        } else {
            // todo implement... we need to respect all properties:
            //  positions, uvs, colors, ...
        }
    }

    private fun filterTrianglesIndexedSimple(indices: IntArray, filter: I3Z): IntArrayList {
        var readIndex = 0
        var numTriangles = indices.size / 3
        while (readIndex < numTriangles) {

            val ri3 = readIndex * 3
            val ai = indices[ri3]
            val bi = indices[ri3 + 1]
            val ci = indices[ri3 + 2]
            if (!filter.call(ai, bi, ci)) {
                // copy last triangle here
                val li3 = (numTriangles - 1) * 3
                indices[ri3] = indices[li3]
                indices[ri3 + 1] = indices[li3 + 1]
                indices[ri3 + 2] = indices[li3 + 2]
                numTriangles--
            } else {
                readIndex++
            }
        }

        return IntArrayList(indices, numTriangles * 3)
    }

    // todo a small number of triangles seems to be incorrectly flipped... why???
    private fun filterStripTriangles(indices: IntArray, filter: I3Z): IntArrayList {
        val dst = IntArrayList(indices.size)
        var ai = indices[0]
        var bi = indices[1]
        for (i in 2 until indices.size) {
            val ci = indices[i]
            if (!isTriangleDegenerate(ai, bi, ci)) {
                if (i.hasFlag(1)) {
                    if (filter.call(ai, ci, bi)) {
                        dst.add(ai, ci, bi)
                    }
                } else {
                    if (filter.call(ai, bi, ci)) {
                        dst.add(ai, bi, ci)
                    }
                }
            }

            // continue one further & flip ai and bi
            ai = bi
            bi = ci
        }
        // convert list back to strip triangles
        return trianglesToTriangleStrip(dst)
    }

    fun isTriangleDegenerate(a: Int, b: Int, c: Int): Boolean {
        return a == b || b == c || a == c
    }
}