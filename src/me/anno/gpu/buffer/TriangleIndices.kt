package me.anno.gpu.buffer

import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.Packing.pack64
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.assertions.assertGreaterThanEquals
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

    private fun Mesh.filterTrianglesIndexedSimple(indices: IntArray, filter: I3Z) {
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

        if (numTriangles * 3 < indices.size) {
            this.indices = indices.copyOf(numTriangles * 3)
            invalidateGeometry()
        }
    }

    /**
     * Removes all triangles, where the predicate returns false;
     * only supported for indexed meshes at the moment;
     *
     * For triangle-strips, vertices are ONLY removed if there is enough space.
     * */
    fun Mesh.optimizeFilterTriangles(filter: I3Z) {
        val indices = indices
        if (indices != null) {
            if (indices.size < 3) return
            when (drawMode) {
                DrawMode.TRIANGLES -> filterTrianglesIndexedSimple(indices, filter)
                DrawMode.TRIANGLE_STRIP -> {

                    var writeIndex = 0
                    var gap = 0
                    var first = true

                    var ai = indices[0]
                    var bi = indices[1]
                    for (t in 2 until indices.size) {

                        assertGreaterThanEquals(t, writeIndex)
                        val ci = indices[t]

                        val degenerate = isDegenerate(ai, bi, ci)
                        val keep = !degenerate && filter.call(ai, bi, ci)

                        if (keep) {
                            if (first) {
                                // start first strip
                                indices[writeIndex++] = ai
                                indices[writeIndex++] = bi
                                first = false
                            } else {
                                if (gap > 0) {
                                    // before we can "fix" the gap, we need to check whether
                                    //  there is enough space for our stitching operation
                                    // todo this is not correct yet: some backsides,
                                    //  that shouldn't be visible are in fact visible
                                    val needsFlip = (t + writeIndex + gap).hasFlag(1)
                                    val neededSpace = if (needsFlip) 5 else 4
                                    if (t + gap - writeIndex >= neededSpace) {
                                        writeIndex -= gap
                                        val last = indices[writeIndex - 1]
                                        // insert degenerate stitching
                                        indices[writeIndex++] = last
                                        if (needsFlip) {
                                            // flip winding
                                            indices[writeIndex++] = bi
                                            indices[writeIndex++] = bi
                                            indices[writeIndex++] = bi
                                            indices[writeIndex++] = ai
                                        } else {
                                            indices[writeIndex++] = ai
                                            indices[writeIndex++] = ai
                                            indices[writeIndex++] = bi
                                        }
                                    }
                                    gap = 0
                                }
                            }
                        } else gap++

                        // normal continuation
                        indices[writeIndex++] = ci

                        ai = bi
                        bi = ci
                    }

                    // undo adding the last <gap> indices
                    writeIndex -= gap

                    if (writeIndex < indices.size) {
                        this.indices = indices.copyOf(writeIndex)
                        invalidateGeometry()
                    }
                }
                else -> { /* no triangles -> nothing to filter here */
                }
            }
        } else {
            // todo implement... we need to respect all properties:
            //  positions, uvs, colors, ...
        }
    }

    /**
     * Removes all triangles, where the predicate returns false;
     * only supported for indexed meshes at the moment;
     * */
    fun Mesh.filterTriangles(filter: I3Z) {
        val indices = indices
        if (indices != null) {
            if (indices.size < 3) return
            when (drawMode) {
                DrawMode.TRIANGLES -> filterTrianglesIndexedSimple(indices, filter)
                DrawMode.TRIANGLE_STRIP -> {

                    // todo this is not quite correct yet

                    var gap = 0
                    var first = true

                    val dst = IntArrayList(indices.size)

                    var ai = indices[0]
                    var bi = indices[1]
                    for (t in 2 until indices.size) {

                        val ci = indices[t]

                        val degenerate = isDegenerate(ai, bi, ci)
                        val keep = !degenerate && filter.call(ai, bi, ci)

                        if (keep) {
                            if (first) {
                                // start first strip
                                dst.clear()
                                dst.add(ai)
                                dst.add(bi)
                                first = false
                            } else {
                                if (gap > 0) {
                                    // before we can "fix" the gap, we need to check whether
                                    //  there is enough space for our stitching operation
                                    dst.size -= gap
                                    val needsFlip = (t + dst.size).hasFlag(1)
                                    // insert degenerate stitching
                                    dst.add(dst.last())
                                    if (needsFlip) {
                                        dst.add(bi) // flip winding
                                        dst.add(bi, bi, ai)
                                    } else {
                                        dst.add(ai, bi, bi)
                                    }
                                }
                            }
                            gap = 0
                        } else gap++

                        // normal continuation
                        dst.add(ci)

                        ai = bi
                        bi = ci
                    }

                    // undo adding the last <gap> indices
                    dst.size -= gap

                    this.indices = dst.toIntArray()
                    invalidateGeometry()
                }
                else -> { /* no triangles -> nothing to filter here */
                }
            }
        } else {
            // todo implement... we need to respect all properties:
            //  positions, uvs, colors, ...
        }
    }

    private fun isDegenerate(a: Int, b: Int, c: Int): Boolean {
        return a == b || b == c || a == c
    }
}